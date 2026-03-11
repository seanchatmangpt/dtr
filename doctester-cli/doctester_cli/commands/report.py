"""Generate reports from DocTester exports."""

from pathlib import Path
from typing import Optional
import typer
from rich.console import Console
from rich.progress import Progress

from doctester_cli.reporters.html_reporter import HtmlReporter
from doctester_cli.reporters.markdown_reporter import MarkdownReporter
from doctester_cli.reporters.summary_reporter import SummaryReporter
from doctester_cli.model import ReportConfig

console = Console()
app = typer.Typer(help="Generate reports from exports")


@app.command()
def summary(
    export_dir: Path = typer.Argument(
        ...,
        help="DocTester export directory (target/site/doctester)",
        exists=True,
    ),
    output_file: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output file (default: summary.md)",
    ),
    format: str = typer.Option(
        "markdown",
        "--format",
        "-f",
        help="Output format (markdown, html, json)",
    ),
) -> None:
    """
    Generate a summary report from test exports.

    Creates an overview of all test classes, methods, and assertions.

    \b
    Examples:
        doctester report summary target/site/doctester
        doctester report summary target/site/doctester -o report.html -f html
    """
    if output_file is None:
        output_file = Path(f"summary.{get_extension(format)}")

    reporter = SummaryReporter()
    config = ReportConfig(
        export_path=export_dir,
        output_path=output_file,
        format=format,
    )

    with Progress() as progress:
        task = progress.add_task("[cyan]Generating summary report...", total=None)
        try:
            result = reporter.generate(config)
            progress.update(task, completed=True)
            console.print(f"[green]✓[/green] Report generated: {output_file}")
            console.print(f"  Tests: {result.stats.get('test_count', 0)}")
            console.print(f"  Assertions: {result.stats.get('assertion_count', 0)}")
        except Exception as e:
            console.print(f"[red]✗[/red] Report generation failed: {e}")
            raise typer.Exit(1)


@app.command()
def coverage(
    export_dir: Path = typer.Argument(
        ...,
        help="DocTester export directory",
        exists=True,
    ),
    output_file: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output file (default: coverage.html)",
    ),
) -> None:
    """
    Generate an endpoint coverage report.

    Shows which API endpoints are tested and which are missing documentation.

    \b
    Examples:
        doctester report coverage target/site/doctester
        doctester report coverage target/site/doctester -o coverage.html
    """
    if output_file is None:
        output_file = Path("coverage.html")

    reporter = HtmlReporter()
    config = ReportConfig(
        export_path=export_dir,
        output_path=output_file,
        report_type="coverage",
    )

    with Progress() as progress:
        task = progress.add_task("[cyan]Analyzing endpoint coverage...", total=None)
        try:
            result = reporter.generate(config)
            progress.update(task, completed=True)
            console.print(f"[green]✓[/green] Coverage report: {output_file}")
        except Exception as e:
            console.print(f"[red]✗[/red] Report generation failed: {e}")
            raise typer.Exit(1)


@app.command()
def changelog(
    export_dir: Path = typer.Argument(
        ...,
        help="DocTester export directory",
        exists=True,
    ),
    output_file: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output file (default: changelog.md)",
    ),
    since: Optional[str] = typer.Option(
        None,
        "--since",
        help="Include changes since version (e.g., v1.0.0)",
    ),
) -> None:
    """
    Generate a changelog from test modifications.

    Tracks endpoint changes, new tests, and documentation updates.

    \b
    Examples:
        doctester report changelog target/site/doctester
        doctester report changelog target/site/doctester --since v1.0.0
    """
    if output_file is None:
        output_file = Path("changelog.md")

    reporter = MarkdownReporter()
    config = ReportConfig(
        export_path=export_dir,
        output_path=output_file,
        report_type="changelog",
        since=since,
    )

    with Progress() as progress:
        task = progress.add_task("[cyan]Generating changelog...", total=None)
        try:
            result = reporter.generate(config)
            progress.update(task, completed=True)
            console.print(f"[green]✓[/green] Changelog: {output_file}")
        except Exception as e:
            console.print(f"[red]✗[/red] Report generation failed: {e}")
            raise typer.Exit(1)


def get_extension(format: str) -> str:
    """Get file extension for format."""
    return {"markdown": "md", "html": "html", "json": "json"}.get(format, "txt")
