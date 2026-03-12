"""Maven build orchestration command.

Provides `dtr build` to orchestrate Maven builds and validate exports.
Replaces manual `mvnd clean verify` calls with a single CLI command.

Usage:
    dtr build                           # Default: clean verify
    dtr build --goals test              # Custom goals: test
    dtr build --profiles docs-html      # Activate profile: docs-html
    dtr build --properties key=value    # Pass property: -Dkey=value
    dtr build --modules doctester-core  # Build specific module
    dtr build --verbose                 # Show full Maven output
    dtr build --export                  # Run format conversion after build
"""

from pathlib import Path
from typing import Optional
import typer
from rich.console import Console

from dtr_cli.managers.maven_manager import (
    MavenRunner,
    MavenBuildConfig,
)
from dtr_cli.managers.latex_manager import LatexManager
from dtr_cli.cli_errors import (
    CLIError,
    PomNotFoundError,
    MavenNotFoundError,
    MavenBuildFailedError,
    LatexCompilationError,
    NoLatexCompilerError,
)
from dtr_cli.model import CompilerStrategy

console = Console()
app = typer.Typer(
    help="Orchestrate Maven builds and validate exports",
    invoke_without_command=True,
    no_args_is_help=False,
)


def validate_pom_exists(path: Path) -> Path:
    """Validate that pom.xml exists in the given directory."""
    pom = path / "pom.xml"
    if not pom.exists():
        raise typer.BadParameter(
            f"pom.xml not found in {path}\n"
            "Make sure you're in the root of a Maven project"
        )
    return path


@app.callback(invoke_without_command=True)
def build_command(
    goals: Optional[str] = typer.Option(
        None,
        "--goals",
        "-g",
        help="Maven goals to execute (comma-separated)",
    ),
    profiles: Optional[str] = typer.Option(
        None,
        "--profiles",
        "-P",
        help="Maven profiles to activate (comma-separated)",
    ),
    properties: Optional[str] = typer.Option(
        None,
        "--properties",
        "-D",
        help="Maven properties (format: key1=val1,key2=val2)",
    ),
    modules: Optional[str] = typer.Option(
        None,
        "--modules",
        "-pl",
        help="Specific modules to build (comma-separated)",
    ),
    export: bool = typer.Option(
        False,
        "--export",
        help="Run format conversion after successful build",
    ),
    verbose: bool = typer.Option(
        False,
        "--verbose",
        "-v",
        help="Show full Maven output",
    ),
    timeout: int = typer.Option(
        600,
        "--timeout",
        help="Build timeout in seconds",
    ),
    project_dir: Path = typer.Option(
        Path.cwd(),
        "--project-dir",
        "-C",
        help="Maven project root directory",
        callback=lambda x: validate_pom_exists(x),
    ),
) -> None:
    """Execute Maven build (default: clean verify).

    This command orchestrates Maven builds, replacing manual invocations like
    `mvnd clean verify` with a unified CLI interface.

    Examples:

        Build with defaults (clean verify):
        $ dtr build

        Custom goals:
        $ dtr build --goals test

        Activate profile and run export conversion:
        $ dtr build --profiles docs-html --export

        Build specific modules:
        $ dtr build --modules doctester-core,doctester-integration-test

        Pass properties:
        $ dtr build --properties key1=val1,key2=val2

        Show full output:
        $ dtr build --verbose
    """
    try:
        # Initialize Maven runner
        maven = MavenRunner(project_dir)

        console.print(f"[cyan]📦 Maven root: {project_dir}[/cyan]")

        # Show available modules if multi-module project
        if maven.is_multi_module():
            module_list = ", ".join(maven.get_available_modules())
            console.print(f"[cyan]📚 Available modules: {module_list}[/cyan]")

        # Parse options
        goals_list = (
            [g.strip() for g in goals.split(",")] if goals else None
        )
        profiles_list = (
            [p.strip() for p in profiles.split(",")] if profiles else None
        )
        modules_list = (
            [m.strip() for m in modules.split(",")] if modules else None
        )

        # Parse properties
        properties_dict = {}
        if properties:
            for prop in properties.split(","):
                if "=" in prop:
                    key, value = prop.split("=", 1)
                    properties_dict[key.strip()] = value.strip()

        # Build configuration
        config = MavenBuildConfig(
            goals=goals_list,
            profiles=profiles_list,
            properties=properties_dict,
            modules=modules_list,
            verbose=verbose,
            timeout=timeout,
        )

        # Execute build
        console.print("[cyan]🔨 Starting Maven build...[/cyan]")
        exit_code = maven.build(config)

        if exit_code != 0:
            raise MavenBuildFailedError(
                exit_code,
                "Check the output above for details",
            )

        # Validate exports exist
        export_dir = maven.get_export_dir()
        if export_dir.exists() and list(export_dir.glob("*.html")):
            console.print(
                f"[green]✅ Exports generated in {export_dir}[/green]"
            )
        else:
            console.print(
                "[yellow]⚠️  No exports found. "
                "Did your tests generate DTR output?[/yellow]"
            )

        # Run format conversion if requested
        if export:
            console.print(
                "[cyan]📝 Running LaTeX/PDF compilation...[/cyan]"
            )

            # Find generated .tex files from Maven output
            latex_dir = export_dir.parent / "latex"
            if latex_dir.exists():
                tex_files = list(latex_dir.glob("*.tex"))

                if tex_files:
                    console.print(
                        f"[cyan]Found {len(tex_files)} LaTeX file(s). Compiling to PDF...[/cyan]"
                    )
                    latex_manager = LatexManager()
                    compiled_count = 0

                    for tex_file in tex_files:
                        try:
                            pdf_output = tex_file.parent / f"{tex_file.stem}.pdf"
                            latex_manager.compile_pdf(
                                tex_file,
                                pdf_output,
                                compiler=CompilerStrategy.AUTO,
                            )
                            console.print(
                                f"[green]✓[/green] Generated PDF: {pdf_output.name}"
                            )
                            compiled_count += 1
                        except (LatexCompilationError, NoLatexCompilerError) as e:
                            console.print(
                                f"[yellow]⚠️  PDF compilation skipped: {tex_file.name} - {e.message}[/yellow]"
                            )
                            continue

                    if compiled_count > 0:
                        console.print(
                            f"[green]✅ Compiled {compiled_count} PDF(s)[/green]"
                        )
                else:
                    console.print(
                        "[yellow]ℹ️  No LaTeX files found. "
                        "Run build with -Ddoctester.output=latex to generate LaTeX.[/yellow]"
                    )
            else:
                console.print(
                    "[yellow]ℹ️  No LaTeX files found. "
                    "Run build with -Ddoctester.output=latex to generate LaTeX.[/yellow]"
                )

    except FileNotFoundError as e:
        raise typer.BadParameter(str(e))
    except (PomNotFoundError, MavenNotFoundError) as e:
        console.print(f"[red]{e.format_message()}[/red]")
        raise typer.Exit(code=1)
    except MavenBuildFailedError as e:
        console.print(f"[red]{e.format_message()}[/red]")
        raise typer.Exit(code=1)
    except RuntimeError as e:
        console.print(f"[red]❌ {e}[/red]")
        raise typer.Exit(code=2)
    except Exception as e:
        console.print(f"[red]❌ Unexpected error: {e}[/red]")
        if verbose:
            raise
        raise typer.Exit(code=2)
