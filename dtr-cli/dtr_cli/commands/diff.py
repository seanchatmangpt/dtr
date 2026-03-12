"""Compare DTR export directories and files to show documentation changes."""

import difflib
from pathlib import Path
from typing import Optional

import typer
from rich.console import Console
from rich.text import Text

console = Console()
app = typer.Typer(help="Compare export directories and files")


def _extract_text_from_html(content: str) -> str:
    """Extract plain text from HTML content using BeautifulSoup."""
    try:
        from bs4 import BeautifulSoup

        soup = BeautifulSoup(content, "html.parser")
        # Remove script and style elements
        for tag in soup(["script", "style"]):
            tag.decompose()
        lines = []
        for line in soup.get_text(separator="\n").splitlines():
            stripped = line.strip()
            if stripped:
                lines.append(stripped)
        return "\n".join(lines)
    except ImportError:
        return content


def _read_file_content(path: Path, ignore_whitespace: bool = False) -> list[str]:
    """Read file content, extracting text from HTML if needed."""
    raw = path.read_text(encoding="utf-8", errors="replace")
    if path.suffix.lower() in (".html", ".htm"):
        raw = _extract_text_from_html(raw)
    lines = raw.splitlines(keepends=True)
    if ignore_whitespace:
        lines = [line.strip() + "\n" for line in lines]
    return lines


def _format_size(size_bytes: int) -> str:
    """Format bytes into a human-readable string."""
    for unit in ("B", "KB", "MB", "GB"):
        if abs(size_bytes) < 1024.0:
            return f"{size_bytes:.1f} {unit}"
        size_bytes /= 1024.0  # type: ignore[assignment]
    return f"{size_bytes:.1f} TB"


def _collect_relative_files(directory: Path) -> dict[str, Path]:
    """Return a mapping of relative-path-string -> absolute Path for all files."""
    result: dict[str, Path] = {}
    for f in directory.rglob("*"):
        if f.is_file():
            rel = str(f.relative_to(directory))
            result[rel] = f
    return result


def _render_unified_diff(
    diff_lines: list[str],
    output_path: Optional[Path],
    use_color: bool = True,
) -> None:
    """Print unified diff lines to console (colored) and optionally save to file."""
    plain_lines: list[str] = []

    for line in diff_lines:
        plain_lines.append(line)
        if not use_color:
            continue
        if line.startswith("+") and not line.startswith("+++"):
            text = Text(line.rstrip("\n"), style="green")
            console.print(text)
        elif line.startswith("-") and not line.startswith("---"):
            text = Text(line.rstrip("\n"), style="red")
            console.print(text)
        elif line.startswith("@@"):
            text = Text(line.rstrip("\n"), style="cyan")
            console.print(text)
        else:
            console.print(line.rstrip("\n"))

    if output_path is not None:
        output_path.write_text("".join(plain_lines), encoding="utf-8")
        console.print(f"\n[cyan]Diff saved to:[/cyan] {output_path}")


@app.command()
def dirs(
    dir_a: Path = typer.Argument(..., help="Reference (old) export directory"),
    dir_b: Path = typer.Argument(..., help="Comparison (new) export directory"),
    context: int = typer.Option(3, "--context", "-c", help="Lines of context around changes"),
    ignore_whitespace: bool = typer.Option(
        False, "--ignore-whitespace", "-w", help="Ignore whitespace differences"
    ),
    output: Optional[Path] = typer.Option(
        None, "--output", "-o", help="Save diff output to this file"
    ),
) -> None:
    """
    Compare two export directories showing added, removed, and modified files.

    \b
    Examples:
        dtr diff dirs old_exports/ new_exports/
        dtr diff dirs old_exports/ new_exports/ --context 5
        dtr diff dirs old_exports/ new_exports/ --ignore-whitespace -o changes.diff
    """
    if not dir_a.exists() or not dir_a.is_dir():
        console.print(f"[red]✗[/red] Not a directory: {dir_a}")
        raise typer.Exit(1)
    if not dir_b.exists() or not dir_b.is_dir():
        console.print(f"[red]✗[/red] Not a directory: {dir_b}")
        raise typer.Exit(1)

    files_a = _collect_relative_files(dir_a)
    files_b = _collect_relative_files(dir_b)

    keys_a = set(files_a)
    keys_b = set(files_b)

    added = sorted(keys_b - keys_a)
    removed = sorted(keys_a - keys_b)
    common = sorted(keys_a & keys_b)

    all_diff_lines: list[str] = []

    # Report added files
    for name in added:
        console.print(f"[green]+ ADDED    {name}[/green]")
        lines_b = _read_file_content(files_b[name], ignore_whitespace)
        diff = list(
            difflib.unified_diff(
                [],
                lines_b,
                fromfile=f"a/{name}",
                tofile=f"b/{name}",
                n=context,
            )
        )
        all_diff_lines.extend(diff)

    # Report removed files
    for name in removed:
        console.print(f"[red]- REMOVED  {name}[/red]")
        lines_a = _read_file_content(files_a[name], ignore_whitespace)
        diff = list(
            difflib.unified_diff(
                lines_a,
                [],
                fromfile=f"a/{name}",
                tofile=f"b/{name}",
                n=context,
            )
        )
        all_diff_lines.extend(diff)

    # Report modified files
    modified_count = 0
    for name in common:
        lines_a = _read_file_content(files_a[name], ignore_whitespace)
        lines_b = _read_file_content(files_b[name], ignore_whitespace)
        diff = list(
            difflib.unified_diff(
                lines_a,
                lines_b,
                fromfile=f"a/{name}",
                tofile=f"b/{name}",
                n=context,
            )
        )
        if diff:
            console.print(f"[yellow]~ MODIFIED {name}[/yellow]")
            _render_unified_diff(diff, output_path=None)
            all_diff_lines.extend(diff)
            modified_count += 1

    total_changes = len(added) + len(removed) + modified_count
    console.print(
        f"\n[bold]Summary:[/bold] "
        f"[green]{len(added)} added[/green], "
        f"[red]{len(removed)} removed[/red], "
        f"[yellow]{modified_count} modified[/yellow]"
    )

    if total_changes == 0:
        console.print("[green]No differences found.[/green]")

    if output is not None and all_diff_lines:
        output.write_text("".join(all_diff_lines), encoding="utf-8")
        console.print(f"[cyan]Diff saved to:[/cyan] {output}")


@app.command()
def files(
    file_a: Path = typer.Argument(..., help="Reference (old) file"),
    file_b: Path = typer.Argument(..., help="Comparison (new) file"),
    context: int = typer.Option(3, "--context", "-c", help="Lines of context around changes"),
    ignore_whitespace: bool = typer.Option(
        False, "--ignore-whitespace", "-w", help="Ignore whitespace differences"
    ),
    output: Optional[Path] = typer.Option(
        None, "--output", "-o", help="Save diff output to this file"
    ),
) -> None:
    """
    Compare two specific export files (HTML or Markdown), showing content diff.

    For HTML files, text content is extracted before diffing so tag changes
    do not dominate the output.

    \b
    Examples:
        dtr diff files old/report.md new/report.md
        dtr diff files old/report.html new/report.html --context 10
        dtr diff files old/report.md new/report.md -o report.diff
    """
    if not file_a.exists() or not file_a.is_file():
        console.print(f"[red]✗[/red] Not a file: {file_a}")
        raise typer.Exit(1)
    if not file_b.exists() or not file_b.is_file():
        console.print(f"[red]✗[/red] Not a file: {file_b}")
        raise typer.Exit(1)

    lines_a = _read_file_content(file_a, ignore_whitespace)
    lines_b = _read_file_content(file_b, ignore_whitespace)

    diff = list(
        difflib.unified_diff(
            lines_a,
            lines_b,
            fromfile=str(file_a),
            tofile=str(file_b),
            n=context,
        )
    )

    if not diff:
        console.print("[green]No differences found.[/green]")
        return

    _render_unified_diff(diff, output_path=output)

    added_lines = sum(1 for l in diff if l.startswith("+") and not l.startswith("+++"))
    removed_lines = sum(1 for l in diff if l.startswith("-") and not l.startswith("---"))
    console.print(
        f"\n[bold]Summary:[/bold] "
        f"[green]+{added_lines} lines[/green], "
        f"[red]-{removed_lines} lines[/red]"
    )


@app.command()
def summary(
    dir_a: Path = typer.Argument(..., help="Reference (old) export directory"),
    dir_b: Path = typer.Argument(..., help="Comparison (new) export directory"),
    output: Optional[Path] = typer.Option(
        None, "--output", "-o", help="Save summary to this file"
    ),
) -> None:
    """
    Show a high-level summary of changes between two export directories.

    Reports file counts, size changes, and which files changed.

    \b
    Examples:
        dtr diff summary old_exports/ new_exports/
        dtr diff summary old_exports/ new_exports/ -o summary.txt
    """
    if not dir_a.exists() or not dir_a.is_dir():
        console.print(f"[red]✗[/red] Not a directory: {dir_a}")
        raise typer.Exit(1)
    if not dir_b.exists() or not dir_b.is_dir():
        console.print(f"[red]✗[/red] Not a directory: {dir_b}")
        raise typer.Exit(1)

    files_a = _collect_relative_files(dir_a)
    files_b = _collect_relative_files(dir_b)

    keys_a = set(files_a)
    keys_b = set(files_b)

    added = sorted(keys_b - keys_a)
    removed = sorted(keys_a - keys_b)
    common = sorted(keys_a & keys_b)
    modified = [
        name
        for name in common
        if files_a[name].read_bytes() != files_b[name].read_bytes()
    ]
    unchanged = [name for name in common if name not in modified]

    size_a = sum(f.stat().st_size for f in files_a.values())
    size_b = sum(f.stat().st_size for f in files_b.values())
    size_delta = size_b - size_a

    lines: list[str] = [
        "DTR Diff Summary",
        "=" * 40,
        f"Reference dir : {dir_a}",
        f"Comparison dir: {dir_b}",
        "",
        f"Files in reference : {len(files_a)}",
        f"Files in comparison: {len(files_b)}",
        "",
        f"Added    : {len(added)}",
        f"Removed  : {len(removed)}",
        f"Modified : {len(modified)}",
        f"Unchanged: {len(unchanged)}",
        "",
        f"Total size (ref) : {_format_size(size_a)}",
        f"Total size (new) : {_format_size(size_b)}",
        f"Size delta       : {'+' if size_delta >= 0 else ''}{_format_size(size_delta)}",
    ]

    if added:
        lines += ["", "Added files:"] + [f"  + {f}" for f in added]
    if removed:
        lines += ["", "Removed files:"] + [f"  - {f}" for f in removed]
    if modified:
        lines += ["", "Modified files:"] + [f"  ~ {f}" for f in modified]

    report_text = "\n".join(lines)

    # Print to console with colour highlights
    for line in lines:
        if line.startswith("  +"):
            console.print(f"[green]{line}[/green]")
        elif line.startswith("  -"):
            console.print(f"[red]{line}[/red]")
        elif line.startswith("  ~"):
            console.print(f"[yellow]{line}[/yellow]")
        elif line.startswith("Added") or "added" in line.lower():
            console.print(f"[green]{line}[/green]")
        elif line.startswith("Removed") or "removed" in line.lower():
            console.print(f"[red]{line}[/red]")
        elif line.startswith("Modified") or "modified" in line.lower():
            console.print(f"[yellow]{line}[/yellow]")
        else:
            console.print(line)

    if output is not None:
        output.write_text(report_text + "\n", encoding="utf-8")
        console.print(f"\n[cyan]Summary saved to:[/cyan] {output}")
