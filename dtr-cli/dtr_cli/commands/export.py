"""Manage DTR export directories."""

from pathlib import Path
from typing import Optional
import typer
from rich.console import Console
from rich.table import Table

from dtr_cli.managers.directory_manager import DirectoryManager
from dtr_cli.managers.latex_manager import LatexManager
from dtr_cli.model import (
    ManageConfig,
    LatexTemplate,
    CompilerStrategy,
)
from dtr_cli.cli_errors import (
    FileNotFoundError_,
    DirectoryExpectedError,
    InvalidFormatError,
    InvalidArgumentError,
    LatexTemplateMissingError,
    LatexCompilationError,
    NoLatexCompilerError,
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

        table = Table(title="DTR Exports")
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


@app.command()
def latex(
    input_file: Path = typer.Argument(
        ...,
        help="Input file (Markdown, HTML, or LaTeX)",
    ),
    output_file: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output .tex file (default: input.stem.tex)",
    ),
    template: str = typer.Option(
        "arxiv",
        "--template",
        "-t",
        help="LaTeX template: arxiv, patent, ieee, acm, nature",
    ),
    force: bool = typer.Option(
        False,
        "--force",
        "-f",
        help="Overwrite existing files",
    ),
) -> None:
    """
    Export documentation to LaTeX format.

    Converts Markdown or HTML files to LaTeX format with academic templates.
    Can also validate and copy existing LaTeX files.

    \b
    Examples:
        dtr export latex docs/guide.md --template arxiv
        dtr export latex docs/guide.html --template ieee -o guide.tex
        dtr export latex generated.tex --force
    """
    try:
        # Validate template
        try:
            template_enum = LatexTemplate(template)
        except ValueError:
            valid = [t.value for t in LatexTemplate]
            raise LatexTemplateMissingError(template, valid)

        # Create manager and convert
        manager = LatexManager()
        result = manager.generate_latex(
            input_file, output_file, template_enum, force
        )

        console.print(
            f"[green]✓[/green] LaTeX generated: {output_file or f'{input_file.stem}.tex'}"
        )
        if result.warnings:
            for warning in result.warnings:
                console.print(f"[yellow]⚠[/yellow] {warning}")

    except LatexTemplateMissingError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except CLIError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except FileNotFoundError:
        console.print(
            f"[red]✗[/red] Input file not found: {input_file}\n"
            "[cyan]Make sure the file exists and is readable"
        )
        raise typer.Exit(1)
    except Exception as e:
        console.print(f"[red]✗[/red] LaTeX export failed: {e}")
        raise typer.Exit(1)


@app.command()
def pdf(
    input_file: Path = typer.Argument(
        ...,
        help="Input file (.tex, Markdown, or HTML)",
    ),
    output_file: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output PDF file (default: input.stem.pdf)",
    ),
    template: str = typer.Option(
        "arxiv",
        "--template",
        "-t",
        help="LaTeX template: arxiv, patent, ieee, acm, nature",
    ),
    compiler: str = typer.Option(
        "auto",
        "--compiler",
        "-c",
        help="Compiler: auto, latexmk, pdflatex, xelatex, pandoc",
    ),
    keep_tex: bool = typer.Option(
        False,
        "--keep-tex",
        help="Keep intermediate LaTeX file",
    ),
    timeout: int = typer.Option(
        300,
        "--timeout",
        help="Compilation timeout in seconds",
    ),
    force: bool = typer.Option(
        False,
        "--force",
        "-f",
        help="Overwrite existing files",
    ),
) -> None:
    """
    Compile LaTeX to PDF with automatic compiler selection.

    Compiles LaTeX files to PDF using a fallback compiler chain:
    latexmk (recommended) → pdflatex → xelatex → pandoc

    If input is Markdown or HTML, converts to LaTeX first.

    \b
    Examples:
        dtr export pdf docs/guide.tex
        dtr export pdf docs/guide.tex --compiler pdflatex
        dtr export pdf docs/guide.md --template ieee
        dtr export pdf guide.tex --compiler auto --keep-tex
    """
    try:
        # Validate template
        try:
            template_enum = LatexTemplate(template)
        except ValueError:
            valid = [t.value for t in LatexTemplate]
            raise LatexTemplateMissingError(template, valid)

        # Validate compiler strategy
        try:
            compiler_enum = CompilerStrategy(compiler)
        except ValueError:
            valid = [c.value for c in CompilerStrategy]
            raise typer.BadParameter(
                f"Invalid compiler: {compiler}\nValid options: {', '.join(valid)}"
            )

        # Create manager and compile
        manager = LatexManager()

        # If input is not .tex, convert first
        if input_file.suffix.lower() != ".tex":
            console.print(f"[cyan]Converting {input_file.suffix} to LaTeX...[/cyan]")
            tex_file = input_file.parent / f"{input_file.stem}.tex"
            manager.generate_latex(input_file, tex_file, template_enum, force)
        else:
            tex_file = input_file

        # Compile to PDF
        console.print(f"[cyan]Compiling LaTeX to PDF...{' ' if compiler == 'auto' else f' ({compiler})'}[/cyan]")
        result = manager.compile_pdf(
            tex_file,
            output_file,
            template_enum,
            compiler_enum,
            keep_tex,
            timeout,
            force,
        )

        console.print(
            f"[green]✓[/green] PDF generated: {output_file or f'{tex_file.stem}.pdf'}"
        )
        if result.warnings:
            for warning in result.warnings:
                console.print(f"[yellow]⚠[/yellow] {warning}")

    except NoLatexCompilerError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(2)
    except LatexCompilationError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        if e.details:
            console.print(f"\n[yellow]Error details:[/yellow]\n{e.details}")
        raise typer.Exit(2)
    except LatexTemplateMissingError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except CLIError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except FileNotFoundError:
        console.print(
            f"[red]✗[/red] Input file not found: {input_file}\n"
            "[cyan]Make sure the file exists and is readable"
        )
        raise typer.Exit(1)
    except Exception as e:
        console.print(f"[red]✗[/red] PDF compilation failed: {e}")
        raise typer.Exit(1)


def format_size(bytes: int) -> str:
    """Format bytes to human-readable size."""
    for unit in ["B", "KB", "MB", "GB"]:
        if bytes < 1024.0:
            return f"{bytes:.1f} {unit}"
        bytes /= 1024.0
    return f"{bytes:.1f} TB"
