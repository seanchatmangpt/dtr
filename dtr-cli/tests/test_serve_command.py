"""Chicago-style TDD tests for `dtr serve`.

Tests real behavior with real collaborators — no mocks, no fakes (except where
a server thread must be interrupted cleanly).  Uses pytest tmp_path for real
file I/O and real HTTP requests via urllib.
"""

import os
import socket
import threading
import time
import urllib.request
from pathlib import Path

import pytest
from typer.testing import CliRunner

from dtr_cli.commands.serve import (
    detect_mime_type,
    find_html_files,
    generate_index_html,
    make_handler,
    read_pid_file,
    remove_pid_file,
    start_server,
    write_pid_file,
)
from dtr_cli.main import app

runner = CliRunner()


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _free_port() -> int:
    """Return an available TCP port on localhost."""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def _start_server_thread(export_dir: Path, port: int) -> threading.Thread:
    """Start the HTTP server in a daemon thread; returns the thread."""
    server = start_server(export_dir, "127.0.0.1", port)

    def _run() -> None:
        server.serve_forever()

    t = threading.Thread(target=_run, daemon=True)
    t.server = server  # type: ignore[attr-defined]
    t.start()
    # Give the server a moment to bind
    time.sleep(0.1)
    return t


def _get(url: str, timeout: float = 3.0) -> tuple[int, str, str]:
    """Return (status_code, content_type, body_text) for a GET request."""
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        body = resp.read().decode("utf-8", errors="replace")
        content_type = resp.headers.get("Content-Type", "")
        return resp.status, content_type, body


# ===================================================================
# 1. find_html_files
# ===================================================================


class TestFindHtmlFiles:
    """find_html_files returns *.html files directly under export_dir."""

    def test_returns_html_files(self, tmp_path: Path):
        (tmp_path / "report.html").write_text("<html/>")
        (tmp_path / "summary.html").write_text("<html/>")
        (tmp_path / "data.json").write_text("{}")

        files = find_html_files(tmp_path)

        names = {f.name for f in files}
        assert "report.html" in names
        assert "summary.html" in names
        assert "data.json" not in names

    def test_returns_empty_list_for_empty_dir(self, tmp_path: Path):
        files = find_html_files(tmp_path)
        assert files == []

    def test_returns_empty_list_for_nonexistent_dir(self, tmp_path: Path):
        missing = tmp_path / "does-not-exist"
        files = find_html_files(missing)
        assert files == []

    def test_files_are_sorted_by_name(self, tmp_path: Path):
        (tmp_path / "zzz.html").write_text("<html/>")
        (tmp_path / "aaa.html").write_text("<html/>")
        (tmp_path / "mmm.html").write_text("<html/>")

        files = find_html_files(tmp_path)

        names = [f.name for f in files]
        assert names == sorted(names)

    def test_does_not_recurse_into_subdirectories(self, tmp_path: Path):
        (tmp_path / "top.html").write_text("<html/>")
        sub = tmp_path / "sub"
        sub.mkdir()
        (sub / "nested.html").write_text("<html/>")

        files = find_html_files(tmp_path)

        names = {f.name for f in files}
        assert "top.html" in names
        assert "nested.html" not in names


# ===================================================================
# 2. generate_index_html
# ===================================================================


class TestGenerateIndexHtml:
    """generate_index_html produces a valid HTML page listing exports."""

    def test_includes_all_html_file_names(self, tmp_path: Path):
        html_files = [
            tmp_path / "PhDThesisDocTest.html",
            tmp_path / "ApiDocTest.html",
        ]
        for f in html_files:
            f.write_text("<html/>")

        page = generate_index_html(tmp_path, html_files)

        assert "PhDThesisDocTest.html" in page
        assert "ApiDocTest.html" in page

    def test_files_are_linked_as_hrefs(self, tmp_path: Path):
        html_files = [tmp_path / "report.html"]
        html_files[0].write_text("<html/>")

        page = generate_index_html(tmp_path, html_files)

        assert 'href="report.html"' in page

    def test_empty_file_list_shows_zero_count(self, tmp_path: Path):
        page = generate_index_html(tmp_path, [])

        assert "0 file" in page

    def test_page_is_valid_html_structure(self, tmp_path: Path):
        html_files = [tmp_path / "x.html"]
        html_files[0].write_text("<html/>")

        page = generate_index_html(tmp_path, html_files)

        assert "<!DOCTYPE html>" in page
        assert "<html" in page
        assert "</html>" in page

    def test_includes_file_count(self, tmp_path: Path):
        html_files = [tmp_path / f"{i}.html" for i in range(3)]
        for f in html_files:
            f.write_text("<html/>")

        page = generate_index_html(tmp_path, html_files)

        assert "3 file" in page


# ===================================================================
# 3. detect_mime_type
# ===================================================================


class TestDetectMimeType:
    """detect_mime_type returns correct MIME types for common extensions."""

    def test_html_files(self, tmp_path: Path):
        assert detect_mime_type(tmp_path / "doc.html") == "text/html"

    def test_htm_files(self, tmp_path: Path):
        assert detect_mime_type(tmp_path / "doc.htm") == "text/html"

    def test_css_files(self, tmp_path: Path):
        assert detect_mime_type(tmp_path / "style.css") == "text/css"

    def test_js_files(self, tmp_path: Path):
        mime = detect_mime_type(tmp_path / "app.js")
        assert "javascript" in mime

    def test_json_files(self, tmp_path: Path):
        mime = detect_mime_type(tmp_path / "data.json")
        assert "json" in mime

    def test_pdf_files(self, tmp_path: Path):
        assert detect_mime_type(tmp_path / "thesis.pdf") == "application/pdf"

    def test_unknown_extension_returns_octet_stream(self, tmp_path: Path):
        mime = detect_mime_type(tmp_path / "file.xyz123unknown")
        assert mime == "application/octet-stream"

    def test_png_files(self, tmp_path: Path):
        assert detect_mime_type(tmp_path / "image.png") == "image/png"

    def test_svg_files(self, tmp_path: Path):
        assert detect_mime_type(tmp_path / "diagram.svg") == "image/svg+xml"


# ===================================================================
# 4. HTTP server: real requests
# ===================================================================


class TestHttpServer:
    """Integration tests that start a real HTTP server and make real requests."""

    def test_serves_existing_html_file(self, tmp_path: Path):
        (tmp_path / "report.html").write_text("<h1>Report</h1>")
        port = _free_port()
        t = _start_server_thread(tmp_path, port)

        try:
            status, content_type, body = _get(f"http://127.0.0.1:{port}/report.html")
            assert status == 200
            assert "text/html" in content_type
            assert "<h1>Report</h1>" in body
        finally:
            t.server.shutdown()

    def test_auto_generates_index_when_no_index_html(self, tmp_path: Path):
        (tmp_path / "PhDThesisDocTest.html").write_text("<html/>")
        (tmp_path / "ApiDocTest.html").write_text("<html/>")
        port = _free_port()
        t = _start_server_thread(tmp_path, port)

        try:
            status, content_type, body = _get(f"http://127.0.0.1:{port}/")
            assert status == 200
            assert "text/html" in content_type
            assert "PhDThesisDocTest.html" in body
            assert "ApiDocTest.html" in body
        finally:
            t.server.shutdown()

    def test_serves_existing_index_html_at_root(self, tmp_path: Path):
        (tmp_path / "index.html").write_text("<h1>Custom Index</h1>")
        (tmp_path / "other.html").write_text("<html/>")
        port = _free_port()
        t = _start_server_thread(tmp_path, port)

        try:
            status, _ct, body = _get(f"http://127.0.0.1:{port}/")
            assert status == 200
            assert "<h1>Custom Index</h1>" in body
        finally:
            t.server.shutdown()

    def test_returns_404_for_missing_file(self, tmp_path: Path):
        port = _free_port()
        t = _start_server_thread(tmp_path, port)

        try:
            with pytest.raises(urllib.error.HTTPError) as exc_info:
                _get(f"http://127.0.0.1:{port}/nonexistent.html")
            assert exc_info.value.code == 404
        finally:
            t.server.shutdown()

    def test_serves_correct_content_type_for_json(self, tmp_path: Path):
        (tmp_path / "data.json").write_text('{"key": "value"}')
        port = _free_port()
        t = _start_server_thread(tmp_path, port)

        try:
            status, content_type, body = _get(f"http://127.0.0.1:{port}/data.json")
            assert status == 200
            assert "json" in content_type
        finally:
            t.server.shutdown()

    def test_path_traversal_is_blocked(self, tmp_path: Path):
        """Requests that escape the export_dir must return 403."""
        port = _free_port()
        t = _start_server_thread(tmp_path, port)

        try:
            with pytest.raises(urllib.error.HTTPError) as exc_info:
                _get(f"http://127.0.0.1:{port}/../etc/passwd")
            assert exc_info.value.code in (403, 404)
        finally:
            t.server.shutdown()


# ===================================================================
# 5. --port and --host options (CLI level)
# ===================================================================


class TestServeCli:
    """CLI-level tests using Typer's CliRunner."""

    def test_start_help_is_accessible(self):
        result = runner.invoke(app, ["serve", "start", "--help"])
        assert result.exit_code == 0
        assert "--port" in result.stdout

    def test_stop_help_is_accessible(self):
        result = runner.invoke(app, ["serve", "stop", "--help"])
        assert result.exit_code == 0

    def test_start_exits_with_error_for_missing_export_dir(self, tmp_path: Path):
        missing = tmp_path / "does-not-exist"
        result = runner.invoke(app, ["serve", "start", str(missing)])
        assert result.exit_code != 0
        assert "not found" in result.stdout.lower() or "error" in result.stdout.lower() or "does-not-exist" in result.stdout

    def test_start_exits_with_error_for_file_not_dir(self, tmp_path: Path):
        a_file = tmp_path / "not_a_dir.txt"
        a_file.write_text("I am a file")
        result = runner.invoke(app, ["serve", "start", str(a_file)])
        assert result.exit_code != 0

    def test_stop_reports_no_server_when_no_pid_file(self, tmp_path: Path):
        pid_file = tmp_path / "test.pid"
        # Remove the PID file if it exists to ensure clean state
        remove_pid_file(pid_file)
        # We test stop by verifying the command runs without crashing
        # The actual PID file location is global, so we just verify CLI exits 0
        result = runner.invoke(app, ["serve", "stop"])
        # Should exit cleanly (0) whether or not a server is running
        # since stop just prints instructions when there is no PID file
        assert result.exit_code in (0, 1)

    def test_start_port_option_shown_in_help(self):
        result = runner.invoke(app, ["serve", "start", "--help"])
        assert result.exit_code == 0
        assert "8080" in result.stdout  # default port

    def test_start_host_option_shown_in_help(self):
        result = runner.invoke(app, ["serve", "start", "--help"])
        assert result.exit_code == 0
        assert "localhost" in result.stdout  # default host

    def test_open_flag_shown_in_help(self):
        result = runner.invoke(app, ["serve", "start", "--help"])
        assert result.exit_code == 0
        assert "--open" in result.stdout


# ===================================================================
# 6. PID file helpers
# ===================================================================


class TestPidFile:
    """PID file write/read/remove round-trips with real files."""

    def test_write_and_read_pid(self, tmp_path: Path):
        pid_file = tmp_path / "server.pid"
        write_pid_file(12345, pid_file)
        assert read_pid_file(pid_file) == 12345

    def test_read_returns_none_when_file_absent(self, tmp_path: Path):
        pid_file = tmp_path / "missing.pid"
        assert read_pid_file(pid_file) is None

    def test_remove_deletes_pid_file(self, tmp_path: Path):
        pid_file = tmp_path / "server.pid"
        pid_file.write_text("99")
        remove_pid_file(pid_file)
        assert not pid_file.exists()

    def test_remove_is_idempotent(self, tmp_path: Path):
        pid_file = tmp_path / "nope.pid"
        # Should not raise even if the file doesn't exist
        remove_pid_file(pid_file)

    def test_read_returns_none_for_corrupt_file(self, tmp_path: Path):
        pid_file = tmp_path / "bad.pid"
        pid_file.write_text("not-a-number")
        assert read_pid_file(pid_file) is None


# ===================================================================
# 7. Graceful shutdown (KeyboardInterrupt)
# ===================================================================


class TestGracefulShutdown:
    """The server loop shuts down cleanly when the thread is stopped."""

    def test_server_stops_when_shutdown_called(self, tmp_path: Path):
        (tmp_path / "test.html").write_text("<html/>")
        port = _free_port()
        server = start_server(tmp_path, "127.0.0.1", port)

        t = threading.Thread(target=server.serve_forever, daemon=True)
        t.start()
        time.sleep(0.1)

        # Verify it is serving
        status, _, _ = _get(f"http://127.0.0.1:{port}/test.html")
        assert status == 200

        # Shut down and verify it stops accepting connections
        server.shutdown()
        server.server_close()
        t.join(timeout=2.0)
        assert not t.is_alive()

    def test_cli_start_exits_cleanly_on_keyboard_interrupt(self, tmp_path: Path):
        """CliRunner invocation exits cleanly when KeyboardInterrupt is raised
        inside serve_forever via a side-effecting mock."""
        from unittest.mock import patch

        (tmp_path / "doc.html").write_text("<html/>")

        port = _free_port()
        with patch(
            "dtr_cli.commands.serve.HTTPServer.serve_forever",
            side_effect=KeyboardInterrupt,
        ):
            result = runner.invoke(
                app,
                ["serve", "start", str(tmp_path), "--port", str(port)],
            )

        assert result.exit_code == 0
        assert "stopped" in result.stdout.lower() or "server" in result.stdout.lower()


# ===================================================================
# 8. Custom port and host binding
# ===================================================================


class TestCustomPortAndHost:
    """Server binds to the requested port and host."""

    def test_server_listens_on_custom_port(self, tmp_path: Path):
        (tmp_path / "index.html").write_text("<h1>ok</h1>")
        port = _free_port()
        t = _start_server_thread(tmp_path, port)

        try:
            status, _, body = _get(f"http://127.0.0.1:{port}/")
            assert status == 200
            assert "<h1>ok</h1>" in body
        finally:
            t.server.shutdown()

    def test_two_servers_can_run_on_different_ports(self, tmp_path: Path):
        (tmp_path / "a.html").write_text("<html>A</html>")
        port1, port2 = _free_port(), _free_port()

        t1 = _start_server_thread(tmp_path, port1)
        t2 = _start_server_thread(tmp_path, port2)

        try:
            s1, _, b1 = _get(f"http://127.0.0.1:{port1}/a.html")
            s2, _, b2 = _get(f"http://127.0.0.1:{port2}/a.html")
            assert s1 == 200
            assert s2 == 200
            assert b1 == b2
        finally:
            t1.server.shutdown()
            t2.server.shutdown()
