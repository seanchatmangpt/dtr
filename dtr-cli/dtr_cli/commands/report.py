"""Generate reports from DocTester exports."""

from pathlib import Path
from typing import Optional
import typer
from rich.console import Console
from rich.progress import Progress

from dtr_cli.reporters.html_reporter import HtmlReporter
from dtr_cli.reporters.markdown_reporter import MarkdownReporter
from dtr_cli.reporters.summary_reporter import SummaryReporter
from dtr_cli.model import ReportConfig
from dtr_cli.cli_errors import (
    FileNotFoundError_,
    DirectoryExpectedError,
    InvalidFormatError,
    PermissionDeniedError,
)

console = Console()
app = typer.Typer(help="Generate reports from exports")

# Valid report formats
VALID_FORMATS = ["markdown", "html"]


def validate_export_dir(path: Path) -> Path:
    """Validate that export directory exists and is a directory."""
    if not path.exists():
        raise typer.BadParameter(
            f"Export directory not found: {path}\nCheck that the path exists and is accessible"
        )
    if not path.is_dir():
        raise typer.BadParameter(
            f"Expected a directory, got file: {path}\nCheck that you're pointing to a directory, not a file"
        )
    return path


def validate_output_path(path: Optional[Path]) -> Optional[Path]:
    """Validate that output file path is writable."""
    if path is None:
        return None

    # Check if parent directory exists
    parent = path.parent
    if not parent.exists():
        raise typer.BadParameter(
            f"Output directory does not exist: {parent}\nCheck that the parent directory exists and is accessible"
        )

    # Check if parent directory is writable
    if not parent.is_dir():
        raise typer.BadParameter(
            f"Expected a directory, got file: {parent}\nCheck that the parent path is a directory"
        )

    try:
        # Test write permissions
        if not parent.stat().st_mode & 0o200:
            raise typer.BadParameter(
                f"Permission denied: cannot write to {parent}\nCheck file permissions and try again"
            )
    except PermissionError:
        raise typer.BadParameter(
            f"Permission denied: cannot write to {parent}\nCheck file permissions and try again"
        )

    return path


def validate_report_format(format: str) -> str:
    """Validate report format is supported."""
    if format not in VALID_FORMATS:
        raise typer.BadParameter(
            f"Invalid format: {format}\nValid formats are: {', '.join(VALID_FORMATS)}"
        )
    return format


@app.command()
def sum(
    export_dir: Path = typer.Argument(
        ...,
        help="DocTester export directory (target/site/doctester)",
        callback=lambda x: validate_export_dir(x),
    ),
    output_file: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output file (default: summary.md)",
        callback=lambda x: validate_output_path(x),
    ),
    format: str = typer.Option(
        "markdown",
        "--format",
        "-f",
        help="Output format (markdown, html)",
        callback=lambda x: validate_report_format(x),
    ),
) -> None:
    """
    Generate a summary report from test exports.

    Creates an overview of all test classes, methods, and assertions.

    \b
    Examples:
        dtr report sum target/site/doctester
        dtr report sum target/site/doctester -o report.html -f html
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
def cov(
    export_dir: Path = typer.Argument(
        ...,
        help="DocTester export directory",
        callback=lambda x: validate_export_dir(x),
    ),
    output_file: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output file (default: coverage.html)",
        callback=lambda x: validate_output_path(x),
    ),
) -> None:
    """
    Generate an endpoint coverage report.

    Shows which API endpoints are tested and which are missing documentation.

    \b
    Examples:
        dtr report cov target/site/doctester
        dtr report cov target/site/doctester -o coverage.html
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
def log(
    export_dir: Path = typer.Argument(
        ...,
        help="DocTester export directory",
        callback=lambda x: validate_export_dir(x),
    ),
    output_file: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output file (default: changelog.md)",
        callback=lambda x: validate_output_path(x),
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
        dtr report log target/site/doctester
        dtr report log target/site/doctester --since v1.0.0
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
