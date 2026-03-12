"""Main entry point for the DTR CLI.

Provides comprehensive commands to manage DTR documentation exports:
- build: Orchestrate Maven builds
- fmt: Convert between formats (HTML -> Markdown, JSON, etc.)
- export: Manage export directories (list, save, clean, check, latex, pdf)
- report: Generate reports and dashboards (sum, cov, log)
- push: Publish to various platforms (gh, s3, gcs, local)
- publish: Publish to Maven Central (check, deploy, release, status)
"""

import logging

import typer
from rich.console import Console

from dtr_cli import __version__
from dtr_cli.commands import fmt, export, push, report, build, publish, init

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

console = Console()
app = typer.Typer(
    help="DTR CLI — Manage DTR documentation exports and publishing.",
    no_args_is_help=True,
    rich_markup_mode="rich",
)

# Add sub-command groups
app.add_typer(build.app, name="build", help="Orchestrate Maven builds")
app.add_typer(fmt.app, name="fmt", help="Convert exports between formats")
app.add_typer(export.app, name="export", help="Manage export directories")
app.add_typer(report.app, name="report", help="Generate reports from exports")
app.add_typer(push.app, name="push", help="Publish exports to platforms")
app.add_typer(publish.app, name="publish", help="Publish to Maven Central")
app.add_typer(init.app, name="init", help="Scaffold new DTR documentation tests")


@app.command()
def version() -> None:
    """Display the CLI version."""
    console.print(f"[cyan]DTR CLI[/cyan] v{__version__}")


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
        console.print("[bold cyan]DTR CLI Configuration[/bold cyan]")
        console.print("Use [code]dtr config --help[/code] for configuration options.")
    else:
        console.print("Use --show to display current configuration.")


def main() -> None:
    """Entry point for the CLI."""
    app()


if __name__ == "__main__":
    main()
