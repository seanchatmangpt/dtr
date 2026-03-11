"""Manage DocTester export directories."""

from pathlib import Path
from typing import Optional
import typer
from rich.console import Console
from rich.table import Table

from doctester_cli.managers.directory_manager import DirectoryManager
from doctester_cli.model import ManageConfig
from doctester_cli.cli_errors import (
    FileNotFoundError_,
    DirectoryExpectedError,
    InvalidFormatError,
    InvalidArgumentError,
    CLIError,
)

console = Console()
app = typer.Typer(help="Manage export directories")

# Valid archive formats
VALID_ARCHIVE_FORMATS = ["tar.gz", "zip"]


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


@app.command()
def list(
    export_dir: Path = typer.Argument(
        ...,
        help="Export directory to list",
        callback=lambda x: validate_export_dir(x),
    ),
    detailed: bool = typer.Option(
        False,
        "--detailed",
        "-d",
        help="Show detailed information",
    ),
) -> None:
    """
    List all exported documentation files.

    Shows all test classes, methods, and generated HTML files.

    \b
    Examples:
        dtr export list target/site/doctester
        dtr export list target/site/doctester -d
    """
    manager = DirectoryManager()
    config = ManageConfig(
        export_path=export_dir,
        detailed=detailed,
    )

    try:
        exports = manager.list_exports(config)

        if not exports:
            console.print("[yellow]No exports found.[/yellow]")
            return

        table = Table(title="DocTester Exports")
        table.add_column("Test Class", style="cyan")
        table.add_column("File Size", justify="right", style="green")
        table.add_column("Modified", style="yellow")

        for export in exports:
            table.add_row(
                export.name,
                format_size(export.size),
                export.modified,
            )

        console.print(table)
        console.print(f"\n[blue]Total:[/blue] {len(exports)} file(s)")
    except CLIError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except Exception as e:
        console.print(f"[red]✗[/red] Failed to list exports: {e}")
        raise typer.Exit(1)


def validate_archive_format(format: str) -> str:
    """Validate archive format is supported."""
    if format not in VALID_ARCHIVE_FORMATS:
        raise typer.BadParameter(
            f"Invalid format: {format}\nValid formats are: {', '.join(VALID_ARCHIVE_FORMATS)}"
        )
    return format


@app.command()
def save(
    export_dir: Path = typer.Argument(
        ...,
        help="Export directory to archive",
        callback=lambda x: validate_export_dir(x),
    ),
    output_file: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Archive file (default: doctester_export.tar.gz)",
    ),
    format: str = typer.Option(
        "tar.gz",
        "--format",
        "-f",
        help="Archive format (tar.gz, zip)",
        callback=lambda x: validate_archive_format(x),
    ),
) -> None:
    """
    Archive exported documentation for backup or sharing.

    Creates a compressed archive of all export files.

    \b
    Examples:
        dtr export save target/site/doctester
        dtr export save target/site/doctester -o exports_backup.zip -f zip
    """
    if output_file is None:
        output_file = Path(f"doctester_export.{format}")

    manager = DirectoryManager()
    config = ManageConfig(
        export_path=export_dir,
        archive_path=output_file,
        archive_format=format,
    )

    try:
        result = manager.archive_exports(config)
        size = format_size(output_file.stat().st_size)
        console.print(f"[green]✓[/green] Archive created: {output_file} ({size})")
    except CLIError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except Exception as e:
        console.print(f"[red]✗[/red] Archive failed: {e}")
        raise typer.Exit(1)


def validate_keep_count(value: int) -> int:
    """Validate keep count is positive."""
    if value < 1:
        raise typer.BadParameter(
            f"Keep count must be at least 1, got {value}\nExpected: a positive integer"
        )
    return value


@app.command()
def clean(
    export_dir: Path = typer.Argument(
        ...,
        help="Export directory to clean",
        callback=lambda x: validate_export_dir(x),
    ),
    keep_latest: int = typer.Option(
        5,
        "--keep",
        "-k",
        help="Number of latest exports to keep",
        callback=lambda x: validate_keep_count(x),
    ),
    dry_run: bool = typer.Option(
        True,
        "--dry-run",
        help="Preview cleanup without deleting",
    ),
) -> None:
    """
    Clean up old exports, keeping the latest versions.

    Removes old test documentation files while preserving recent ones.

    \b
    Examples:
        dtr export clean target/site/doctester
        dtr export clean target/site/doctester --keep 3 --no-dry-run
    """
    manager = DirectoryManager()
    config = ManageConfig(
        export_path=export_dir,
        keep_latest=keep_latest,
        dry_run=dry_run,
    )

    try:
        result = manager.cleanup_exports(config)

        if dry_run:
            console.print("[yellow]DRY RUN[/yellow] - Files would be removed:")
        else:
            console.print("[cyan]Removed files:[/cyan]")

        for removed_file in result.removed_files:
            console.print(f"  - {removed_file}")

        if dry_run:
            console.print("\nRun with [code]--no-dry-run[/code] to actually delete.")
        else:
            console.print(f"\n[green]✓[/green] Cleaned up {len(result.removed_files)} file(s)")
    except CLIError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except Exception as e:
        console.print(f"[red]✗[/red] Cleanup failed: {e}")
        raise typer.Exit(1)


@app.command()
def check(
    export_dir: Path = typer.Argument(
        ...,
        help="Export directory to validate",
        callback=lambda x: validate_export_dir(x),
    ),
) -> None:
    """
    Validate the integrity of exported documentation.

    Checks for broken links, missing assets, and valid HTML structure.

    \b
    Examples:
        dtr export check target/site/doctester
    """
    manager = DirectoryManager()
    config = ManageConfig(export_path=export_dir)

    try:
        result = manager.validate_exports(config)

        console.print("[bold cyan]Validation Results[/bold cyan]")
        console.print(f"Files checked: {result.stats['files_checked']}")
        console.print(f"Valid files: {result.stats['valid_files']}")
        console.print(f"Issues found: {result.stats['issues_found']}")

        if result.issues:
            console.print("\n[yellow]Issues:[/yellow]")
            for issue in result.issues[:10]:
                console.print(f"  • {issue}")

        if result.stats['issues_found'] == 0:
            console.print("\n[green]✓[/green] All exports are valid.")
        else:
            raise typer.Exit(1)
    except CLIError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except Exception as e:
        console.print(f"[red]✗[/red] Validation failed: {e}")
        raise typer.Exit(1)


def format_size(bytes: int) -> str:
    """Format bytes to human-readable size."""
    for unit in ["B", "KB", "MB", "GB"]:
        if bytes < 1024.0:
            return f"{bytes:.1f} {unit}"
        bytes /= 1024.0
    return f"{bytes:.1f} TB"
