"""Main entry point for the DTR CLI.

Provides comprehensive commands to manage DTR documentation exports:
- build: Orchestrate Maven builds
- fmt: Convert between formats (HTML -> Markdown, JSON, etc.)
- export: Manage export directories (list, save, clean, check, latex, pdf)
- report: Generate reports and dashboards (sum, cov, log)
- push: Publish to various platforms (gh, s3, gcs, local)
- publish: Publish to Maven Central (check, deploy, release, status)
- watch: Watch Java test files and auto-trigger Maven builds on change
- diff: Compare export directories and files to show documentation changes
- test: Discover and run DTR documentation tests (list, run, filter)
- serve: Start a local HTTP server to preview documentation exports
- template: Browse and apply built-in DTR test templates
"""

import json
import logging
import subprocess
import sys
from pathlib import Path
from typing import Optional

import typer
from rich.console import Console
from rich.panel import Panel

from dtr_cli import __version__
from dtr_cli.cli_errors import ConfigurationError
from dtr_cli.commands import (
    fmt,
    export,
    push,
    report,
    build,
    publish,
    init,
    watch,
    doctor,
    diff,
    module,
    serve,
    schema,
    template,
)
from dtr_cli.commands import test as test_cmd
from dtr_cli import config as config_module
from dtr_cli import state

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

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
app.add_typer(watch.app, name="watch", help="Watch Java test files and auto-trigger builds")
app.add_typer(diff.app, name="diff", help="Compare export directories and files")
app.add_typer(module.app, name="module", help="Inspect Maven module structure")
app.add_typer(test_cmd.app, name="test", help="Discover and run DTR documentation tests")
app.add_typer(serve.app, name="serve", help="Start a local HTTP server to preview documentation exports")
app.add_typer(schema.app, name="schema", help="Validate and generate schemas for DTR exports")
app.add_typer(template.app, name="template", help="Browse and apply built-in DTR test templates")

app.command(name="doctor", help="Validate the development environment and report issues")(
    doctor.doctor_command
)


def _probe(cmd: list[str], timeout: int = 5) -> str:
    """Run a tool and return its first output line, or empty string on failure."""
    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        output = (result.stdout or result.stderr or "").strip()
        return output.splitlines()[0] if output else ""
    except (FileNotFoundError, subprocess.TimeoutExpired, OSError):
        return ""


def _java_version() -> str:
    """Return short Java version string, e.g. 'Java 26'."""
    raw = _probe(["java", "-version"])
    # java -version outputs to stderr: 'openjdk version "26.0.2" ...'
    if not raw:
        raw = _probe(["java", "--version"])
    if not raw:
        return "Java (not found)"
    # Extract quoted version number
    import re
    match = re.search(r'"([^"]+)"', raw)
    if match:
        ver = match.group(1).split(".")[0]
        return f"Java {ver}"
    return raw[:30]


def _maven_version() -> str:
    """Return short Maven version string, e.g. 'Maven 4.0.0'."""
    raw = _probe(["mvn", "--version"])
    if not raw:
        raw = _probe(["/opt/apache-maven-4.0.0-rc-5/bin/mvn", "--version"])
    if not raw:
        return "Maven (not found)"
    # "Apache Maven 4.0.0-rc-5 ..."
    import re
    match = re.search(r"Maven\s+([\d.\w-]+)", raw)
    if match:
        return f"Maven {match.group(1)}"
    return raw[:30]


def _mvnd_version() -> str:
    """Return short mvnd version string, e.g. 'mvnd 2.0.0'."""
    raw = _probe(["/opt/mvnd/bin/mvnd", "--version"])
    if not raw:
        raw = _probe(["mvnd", "--version"])
    if not raw:
        return ""
    import re
    match = re.search(r"mvnd\s+([\d.]+)", raw, re.IGNORECASE)
    if match:
        return f"mvnd {match.group(1)}"
    return raw[:30]


def _env_matrix() -> dict[str, str]:
    """Collect full environment matrix: Python, Java, Maven, mvnd."""
    python_ver = f"Python {sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}"
    java_ver = _java_version()
    maven_ver = _maven_version()
    mvnd_ver = _mvnd_version()
    return {
        "version": __version__,
        "python": python_ver,
        "java": java_ver,
        "maven": maven_ver,
        "mvnd": mvnd_ver,
    }


@app.callback()
def global_callback(
    ctx: typer.Context,
    json_mode: bool = typer.Option(
        False,
        "--json/--no-json",
        help="Output as JSON (suppresses banner and color)",
        is_eager=False,
    ),
    quiet: bool = typer.Option(
        False,
        "--quiet",
        "-q",
        help="Suppress banner and informational output",
    ),
    verbose: bool = typer.Option(
        False,
        "--verbose",
        "-v",
        help="Enable verbose diagnostic output",
    ),
    no_color: bool = typer.Option(
        False,
        "--no-color",
        help="Disable color output",
        envvar="NO_COLOR",
    ),
) -> None:
    """DTR CLI — Documentation Testing Runtime."""
    try:
        state.configure(
            json_mode=json_mode,
            quiet=quiet,
            verbose=verbose,
            no_color=no_color,
        )

        s = state.get_state()

        # Show startup banner unless quiet or json mode
        if not quiet and not json_mode and ctx.invoked_subcommand is not None:
            java_ver = _java_version()
            maven_ver = _maven_version()
            banner_text = (
                f"[bold cyan]DTR[/bold cyan]  Documentation Testing Runtime\n"
                f"[dim]v{__version__}[/dim]  |  [dim]{java_ver}[/dim]  |  [dim]{maven_ver}[/dim]"
            )
            s.console.print(
                Panel(banner_text, expand=False, border_style="cyan"),
                highlight=False,
            )
    except Exception as exc:
        err_console = Console(stderr=True)
        err_console.print(
            f"[red]DTR startup error:[/red] {exc}\n"
            "Try [bold]dtr doctor[/bold] to diagnose environment issues."
        )
        raise typer.Exit(code=1)


@app.command()
def version() -> None:
    """Display the CLI version and full environment matrix."""
    try:
        matrix = _env_matrix()
        s = state.get_state()

        if s.json_mode:
            s.out.print(json.dumps(matrix, indent=2))
            return

        s.out.print(f"[bold cyan]DTR CLI[/bold cyan] v{matrix['version']}")
        s.out.print(f"  {matrix['python']}")
        s.out.print(f"  {matrix['java']}")
        s.out.print(f"  {matrix['maven']}")
        if matrix.get("mvnd"):
            s.out.print(f"  {matrix['mvnd']}")
    except Exception as exc:
        console = Console(stderr=True)
        console.print(f"[red]Error:[/red] {exc}")
        raise typer.Exit(code=1)


@app.command()
def config(
    show: bool = typer.Option(
        False,
        "--show",
        help="Display the current resolved configuration",
    ),
    init: bool = typer.Option(
        False,
        "--init",
        help="Create a default .dtr.yml in the current directory",
    ),
    get: Optional[str] = typer.Option(
        None,
        "--get",
        metavar="KEY",
        help="Get a config value using dot notation (e.g. build.verbose)",
    ),
    set_kv: Optional[str] = typer.Option(
        None,
        "--set",
        metavar="KEY=VALUE",
        help="Set a config value using dot notation (e.g. build.verbose=true)",
    ),
) -> None:
    """Manage CLI configuration (.dtr.yml).

    DTR looks for .dtr.yml starting from the current directory and walking
    up to the project root (where pom.xml lives).  Falls back to built-in
    defaults when no file is found.
    """
    s = state.get_state()
    console = s.out

    # --init: create a fresh .dtr.yml
    if init:
        try:
            created = config_module.init_config(Path.cwd())
            console.print(f"[green]Created[/green] {created}")
        except FileExistsError as exc:
            console.print(f"[yellow]Already exists:[/yellow] {exc}")
            raise typer.Exit(code=1)
        return

    # Load resolved config for the remaining sub-commands
    try:
        cfg = config_module.load_config(Path.cwd())
    except ValueError as exc:
        console.print(f"[red]Config error:[/red] {exc}")
        raise typer.Exit(code=1)

    # --get KEY
    if get:
        try:
            value = config_module.get_value(cfg, get)
            console.print(value)
        except KeyError as exc:
            console.print(f"[red]Unknown key:[/red] {exc}")
            raise typer.Exit(code=1)
        return

    # --set KEY=VALUE
    if set_kv:
        if "=" not in set_kv:
            console.print(
                "[red]--set expects KEY=VALUE format, e.g. build.verbose=true[/red]"
            )
            raise typer.Exit(code=1)
        key, _, value = set_kv.partition("=")
        try:
            config_module.set_value(cfg, key.strip(), value.strip())
        except (KeyError, ValueError) as exc:
            console.print(f"[red]Error:[/red] {exc}")
            raise typer.Exit(code=1)

        # Persist: write to existing file, or create in cwd
        dest = cfg.source() or (Path.cwd() / config_module.CONFIG_FILENAME)
        config_module.save_config(cfg, dest)
        console.print(f"[green]Updated[/green] {key.strip()} = {value.strip()} ({dest})")
        return

    # --show (or no flag)
    if show:
        from rich.table import Table as RichTable
        source = cfg.source()
        location = str(source) if source else "(defaults — no .dtr.yml found)"
        console.print(f"[bold cyan]DTR CLI Configuration[/bold cyan]  source: {location}")
        console.print()

        defaults = config_module.DtrConfig.defaults()
        table = RichTable(show_header=True, header_style="bold cyan", box=None, padding=(0, 1))
        table.add_column("Section", style="bold", no_wrap=True)
        table.add_column("Key", no_wrap=True)
        table.add_column("Value")
        table.add_column("Default", style="dim")

        for section_name in ("build", "export", "publish", "report"):
            section = getattr(cfg, section_name)
            default_section = getattr(defaults, section_name)
            first_row = True
            for field_name in section.model_fields:
                current_val = getattr(section, field_name)
                default_val = getattr(default_section, field_name)
                section_label = section_name if first_row else ""
                first_row = False
                # Highlight changed values
                val_str = str(current_val)
                default_str = str(default_val)
                if current_val != default_val:
                    val_str = f"[bold yellow]{val_str}[/bold yellow]"
                table.add_row(section_label, field_name, val_str, default_str)

        console.print(table)
    else:
        console.print(
            "Use [bold]--show[/bold] to display configuration, "
            "[bold]--init[/bold] to create .dtr.yml, "
            "[bold]--get KEY[/bold] / [bold]--set KEY=VALUE[/bold] to read/write values."
        )


def main() -> None:
    """Entry point for the CLI."""
    app()


if __name__ == "__main__":
    main()
