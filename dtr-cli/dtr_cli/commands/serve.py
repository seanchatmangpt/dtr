"""Serve DTR documentation exports via a local HTTP server.

Provides `dtr serve start EXPORT_DIR` to spin up a local HTTP server
so that generated HTML exports can be previewed in a browser.

Usage:
    dtr serve start ./target/docs/test-results
    dtr serve start ./docs --port 9090 --open
    dtr serve start ./docs --host 0.0.0.0 --port 8000
    dtr serve stop
"""

import mimetypes
import os
import threading
import webbrowser
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from typing import Optional
from urllib.parse import unquote

import typer
from rich.console import Console
from rich.panel import Panel

console = Console()
app = typer.Typer(help="Start a local HTTP server to preview documentation exports")

# ---------------------------------------------------------------------------
# Pure helper functions (easy to unit-test)
# ---------------------------------------------------------------------------

_PID_FILE = Path(os.path.expanduser("~/.dtr_serve.pid"))


def find_html_files(export_dir: Path) -> list[Path]:
    """Return all *.html files directly under *export_dir*, sorted by name."""
    if not export_dir.is_dir():
        return []
    return sorted(export_dir.glob("*.html"))


def generate_index_html(export_dir: Path, html_files: list[Path]) -> str:
    """Generate an index HTML page listing all HTML exports in *export_dir*."""
    items = ""
    for f in html_files:
        name = f.name
        items += f'    <li><a href="{name}">{name}</a></li>\n'

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DTR Documentation Index</title>
  <style>
    body {{ font-family: system-ui, sans-serif; max-width: 800px; margin: 2rem auto; padding: 0 1rem; }}
    h1 {{ color: #1a73e8; border-bottom: 2px solid #1a73e8; padding-bottom: 0.5rem; }}
    ul {{ list-style: none; padding: 0; }}
    li {{ margin: 0.5rem 0; }}
    a {{ color: #1a73e8; text-decoration: none; font-size: 1.1rem; }}
    a:hover {{ text-decoration: underline; }}
    .count {{ color: #666; font-size: 0.9rem; margin-top: 1rem; }}
  </style>
</head>
<body>
  <h1>DTR Documentation Exports</h1>
  <ul>
{items}  </ul>
  <p class="count">{len(html_files)} file(s) found in {export_dir}</p>
</body>
</html>"""


def detect_mime_type(path: Path) -> str:
    """Return the MIME type for *path*, defaulting to application/octet-stream."""
    mime, _ = mimetypes.guess_type(str(path))
    if mime:
        return mime
    # Explicit fallbacks for common doc types
    ext = path.suffix.lower()
    fallbacks = {
        ".html": "text/html",
        ".htm": "text/html",
        ".css": "text/css",
        ".js": "application/javascript",
        ".json": "application/json",
        ".md": "text/markdown",
        ".pdf": "application/pdf",
        ".tex": "text/x-tex",
        ".png": "image/png",
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".svg": "image/svg+xml",
    }
    return fallbacks.get(ext, "application/octet-stream")


# ---------------------------------------------------------------------------
# HTTP request handler
# ---------------------------------------------------------------------------


def make_handler(export_dir: Path):
    """Factory that returns an HTTPRequestHandler bound to *export_dir*."""

    class DTRHandler(BaseHTTPRequestHandler):
        _export_dir = export_dir

        def log_message(self, fmt: str, *args) -> None:  # type: ignore[override]
            # Route HTTP access log through Rich
            console.print(
                f"[dim]{self.address_string()}[/dim] "
                f"[cyan]{fmt % args}[/cyan]"
            )

        def do_GET(self) -> None:  # noqa: N802
            # Decode and strip leading slash from path
            raw_path = unquote(self.path.lstrip("/").split("?")[0])

            # Root → serve index.html or auto-generate one
            if not raw_path or raw_path == "/":
                index = self._export_dir / "index.html"
                if index.exists():
                    self._serve_file(index)
                else:
                    html_files = find_html_files(self._export_dir)
                    body = generate_index_html(self._export_dir, html_files)
                    self._send_response(200, "text/html", body.encode())
                return

            target = self._export_dir / raw_path
            # Security: prevent path traversal
            try:
                target.resolve().relative_to(self._export_dir.resolve())
            except ValueError:
                self._send_response(403, "text/plain", b"403 Forbidden")
                return

            if target.is_file():
                self._serve_file(target)
            elif target.is_dir():
                # Redirect to trailing slash and serve directory index
                index = target / "index.html"
                if index.exists():
                    self._serve_file(index)
                else:
                    html_files = sorted(target.glob("*.html"))
                    body = generate_index_html(target, html_files)
                    self._send_response(200, "text/html", body.encode())
            else:
                self._send_response(404, "text/plain", b"404 Not Found")

        def _serve_file(self, path: Path) -> None:
            mime = detect_mime_type(path)
            data = path.read_bytes()
            self._send_response(200, mime, data)

        def _send_response(
            self, code: int, content_type: str, body: bytes
        ) -> None:
            self.send_response(code)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

    return DTRHandler


# ---------------------------------------------------------------------------
# Server lifecycle helpers
# ---------------------------------------------------------------------------


def start_server(export_dir: Path, host: str, port: int) -> HTTPServer:
    """Create and return an HTTPServer (not yet started in a thread)."""
    handler = make_handler(export_dir)
    server = HTTPServer((host, port), handler)
    return server


def write_pid_file(pid: int, pid_file: Path = _PID_FILE) -> None:
    """Write the server PID to *pid_file*."""
    pid_file.write_text(str(pid))


def read_pid_file(pid_file: Path = _PID_FILE) -> Optional[int]:
    """Read PID from *pid_file*, returning None if it does not exist."""
    if not pid_file.exists():
        return None
    try:
        return int(pid_file.read_text().strip())
    except (ValueError, OSError):
        return None


def remove_pid_file(pid_file: Path = _PID_FILE) -> None:
    """Remove the PID file if it exists."""
    try:
        pid_file.unlink()
    except FileNotFoundError:
        pass


# ---------------------------------------------------------------------------
# Typer commands
# ---------------------------------------------------------------------------


@app.command(name="start")
def start(
    export_dir: Path = typer.Argument(
        ...,
        help="Directory containing HTML export files to serve",
        exists=False,  # We validate manually for a better error message
    ),
    port: int = typer.Option(
        8080,
        "--port",
        "-p",
        help="TCP port to listen on",
    ),
    host: str = typer.Option(
        "localhost",
        "--host",
        help="Hostname or IP address to bind to",
    ),
    open_browser: bool = typer.Option(
        False,
        "--open",
        "-o",
        help="Auto-open the browser after the server starts",
    ),
) -> None:
    """Start a local HTTP server to preview documentation exports.

    EXPORT_DIR should contain the HTML files generated by DTR (typically
    target/docs/test-results/).  If no index.html is present an auto-generated
    index listing all HTML files is served at the root URL.

    Press Ctrl+C to stop the server.

    Examples:

        Preview the default output directory:
        $ dtr serve start target/docs/test-results

        Custom port with auto-open:
        $ dtr serve start ./docs --port 9090 --open

        Bind to all interfaces:
        $ dtr serve start ./docs --host 0.0.0.0
    """
    export_dir = export_dir.resolve()

    if not export_dir.exists():
        console.print(
            f"[red]Export directory not found:[/red] {export_dir}\n"
            "Run [bold]dtr build[/bold] first to generate exports."
        )
        raise typer.Exit(code=1)

    if not export_dir.is_dir():
        console.print(f"[red]{export_dir} is not a directory.[/red]")
        raise typer.Exit(code=1)

    html_files = find_html_files(export_dir)
    url = f"http://{host}:{port}"

    # Rich startup banner
    file_list = "\n".join(f"  • {f.name}" for f in html_files[:10])
    if len(html_files) > 10:
        file_list += f"\n  … and {len(html_files) - 10} more"
    if not html_files:
        file_list = "  (no HTML files found — directory listing will be empty)"

    console.print(
        Panel(
            f"[bold green]DTR Documentation Server[/bold green]\n\n"
            f"[dim]Export directory:[/dim] {export_dir}\n"
            f"[dim]Listening on:[/dim]    [bold cyan]{url}[/bold cyan]\n"
            f"[dim]HTML exports:[/dim]\n{file_list}\n\n"
            f"[dim]Press Ctrl+C to stop[/dim]",
            title="[bold]dtr serve start[/bold]",
            border_style="cyan",
        )
    )

    server = start_server(export_dir, host, port)
    write_pid_file(os.getpid())

    if open_browser:
        # Open browser after a short delay so the server is ready
        def _open() -> None:
            import time
            time.sleep(0.5)
            webbrowser.open(url)

        threading.Thread(target=_open, daemon=True).start()

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        console.print("\n[cyan]Server stopped.[/cyan]")
    finally:
        server.server_close()
        remove_pid_file()


@app.command(name="stop")
def stop() -> None:
    """Stop the DTR documentation server.

    Because `dtr serve start` runs in the foreground, the normal way to stop
    it is to press Ctrl+C in the terminal where it is running.

    If the server was started in the background, this command will attempt to
    find and kill it using the stored PID file.
    """
    pid = read_pid_file()
    if pid is None:
        console.print(
            "[yellow]No running DTR server detected.[/yellow]\n"
            "If the server is running in the foreground, press [bold]Ctrl+C[/bold] "
            "in that terminal to stop it."
        )
        return

    console.print(f"[dim]Found DTR server PID:[/dim] {pid}")
    try:
        os.kill(pid, 15)  # SIGTERM
        remove_pid_file()
        console.print(f"[green]Sent SIGTERM to process {pid}.[/green]")
    except ProcessLookupError:
        console.print(
            f"[yellow]Process {pid} is not running.[/yellow] "
            "Cleaning up stale PID file."
        )
        remove_pid_file()
    except PermissionError:
        console.print(
            f"[red]Permission denied:[/red] cannot send signal to PID {pid}."
        )
        raise typer.Exit(code=1)
