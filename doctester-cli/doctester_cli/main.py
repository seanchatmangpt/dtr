"""Main entry point for the DocTester CLI.

Provides comprehensive commands to manage DocTester documentation exports:
- Convert between formats (HTML → Markdown, JSON, etc.)
- Generate reports and dashboards
- Manage export directories
- Publish to various platforms (GitHub, S3, GCS, etc.)
"""

import logging

import typer
from rich.console import Console

from doctester_cli import __version__
from doctester_cli.commands import convert, manage, publish, report

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

console = Console()
app = typer.Typer(
    help="DocTester CLI — Manage DocTester documentation exports and publishing.",
    no_args_is_help=True,
    rich_markup_mode="rich",
)

# Add sub-command groups
app.add_typer(convert.app, name="convert", help="Convert exports between formats")
app.add_typer(report.app, name="report", help="Generate reports from exports")
app.add_typer(manage.app, name="manage", help="Manage export directories")
app.add_typer(publish.app, name="publish", help="Publish exports to platforms")


@app.command()
def version() -> None:
    """Display the CLI version."""
    console.print(f"[cyan]DocTester CLI[/cyan] v{__version__}")


@app.command()
def config(
    show: bool = typer.Option(
        False,
        "--show",
        help="Show current configuration",
    ),
) -> None:
    """Manage CLI configuration."""
    if show:
        console.print("[bold cyan]DocTester CLI Configuration[/bold cyan]")
        console.print("Use [code]dtr config --help[/code] for configuration options.")
    else:
        console.print("Use --show to display current configuration.")


def main() -> None:
    """Entry point for the CLI."""
    app()


if __name__ == "__main__":
    main()
