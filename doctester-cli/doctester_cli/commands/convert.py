"""Convert DocTester exports between formats."""

from pathlib import Path
from typing import Optional, Literal
import typer
from rich.console import Console
from rich.progress import Progress

from doctester_cli.converters.html_converter import HtmlConverter
from doctester_cli.converters.json_converter import JsonConverter
from doctester_cli.converters.markdown_converter import MarkdownConverter
from doctester_cli.model import ConversionConfig

console = Console()
app = typer.Typer(help="Convert exports between formats")


@app.command()
def html_to_markdown(
    input_file: Path = typer.Argument(
        ...,
        help="Input HTML file or directory",
        exists=True,
    ),
    output_dir: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output directory (default: current directory)",
    ),
    recursive: bool = typer.Option(
        False,
        "--recursive",
        "-r",
        help="Process directories recursively",
    ),
    force: bool = typer.Option(
        False,
        "--force",
        "-f",
        help="Overwrite existing files",
    ),
) -> None:
    """
    Convert HTML documentation to Markdown format.

    \b
    Examples:
        doctester convert html-to-markdown test_output.html
        doctester convert html-to-markdown ./docs -o ./markdown_docs -r
    """
    if output_dir is None:
        output_dir = Path.cwd()

    output_dir.mkdir(parents=True, exist_ok=True)

    converter = HtmlConverter()
    config = ConversionConfig(
        input_path=input_file,
        output_path=output_dir,
        recursive=recursive,
        force=force,
    )

    with Progress() as progress:
        task = progress.add_task("[cyan]Converting HTML to Markdown...", total=None)
        try:
            result = converter.convert_to_markdown(config)
            progress.update(task, completed=True)
            console.print(f"[green]✓[/green] Converted {result.files_processed} file(s)")
            if result.warnings:
                console.print(f"[yellow]⚠[/yellow] {len(result.warnings)} warning(s)")
                for warning in result.warnings[:5]:
                    console.print(f"  - {warning}")
        except Exception as e:
            console.print(f"[red]✗[/red] Conversion failed: {e}")
            raise typer.Exit(1)


@app.command()
def html_to_json(
    input_file: Path = typer.Argument(
        ...,
        help="Input HTML file or directory",
        exists=True,
    ),
    output_dir: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output directory (default: current directory)",
    ),
    pretty: bool = typer.Option(
        True,
        "--pretty",
        help="Pretty-print JSON output",
    ),
    recursive: bool = typer.Option(
        False,
        "--recursive",
        "-r",
        help="Process directories recursively",
    ),
) -> None:
    """
    Convert HTML documentation to JSON format.

    \b
    Examples:
        doctester convert html-to-json test_output.html
        doctester convert html-to-json ./docs -o ./json_export -r
    """
    if output_dir is None:
        output_dir = Path.cwd()

    output_dir.mkdir(parents=True, exist_ok=True)

    converter = JsonConverter()
    config = ConversionConfig(
        input_path=input_file,
        output_path=output_dir,
        recursive=recursive,
        pretty=pretty,
    )

    with Progress() as progress:
        task = progress.add_task("[cyan]Converting HTML to JSON...", total=None)
        try:
            result = converter.convert_from_html(config)
            progress.update(task, completed=True)
            console.print(f"[green]✓[/green] Converted {result.files_processed} file(s)")
        except Exception as e:
            console.print(f"[red]✗[/red] Conversion failed: {e}")
            raise typer.Exit(1)


@app.command()
def markdown_to_html(
    input_file: Path = typer.Argument(
        ...,
        help="Input Markdown file or directory",
        exists=True,
    ),
    output_dir: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output directory (default: current directory)",
    ),
    template: Optional[str] = typer.Option(
        "default",
        "--template",
        "-t",
        help="HTML template (default, minimal, github, custom)",
    ),
    recursive: bool = typer.Option(
        False,
        "--recursive",
        "-r",
        help="Process directories recursively",
    ),
) -> None:
    """
    Convert Markdown documentation to HTML format.

    \b
    Examples:
        doctester convert markdown-to-html docs.md
        doctester convert markdown-to-html ./markdown_docs -o ./html_docs -t github -r
    """
    if output_dir is None:
        output_dir = Path.cwd()

    output_dir.mkdir(parents=True, exist_ok=True)

    converter = MarkdownConverter()
    config = ConversionConfig(
        input_path=input_file,
        output_path=output_dir,
        recursive=recursive,
        template=template,
    )

    with Progress() as progress:
        task = progress.add_task("[cyan]Converting Markdown to HTML...", total=None)
        try:
            result = converter.convert_to_html(config)
            progress.update(task, completed=True)
            console.print(f"[green]✓[/green] Converted {result.files_processed} file(s)")
        except Exception as e:
            console.print(f"[red]✗[/red] Conversion failed: {e}")
            raise typer.Exit(1)


@app.command()
def list_formats() -> None:
    """List supported conversion formats."""
    console.print("[bold cyan]Supported Formats[/bold cyan]")
    console.print("""
[bold]Input Formats:[/bold]
  • HTML        - DocTester generated HTML documentation
  • Markdown    - Standard Markdown files
  • JSON        - Structured JSON exports

[bold]Output Formats:[/bold]
  • HTML        - Bootstrap-styled HTML (multiple templates)
  • Markdown    - GitHub-flavored Markdown
  • JSON        - Pretty-printed or compact JSON
  • PDF         - (coming soon)

[bold]Supported Conversions:[/bold]
  • HTML → Markdown
  • HTML → JSON
  • Markdown → HTML
  • JSON → HTML
  • JSON → Markdown
""")
