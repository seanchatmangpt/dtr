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

import logging
from pathlib import Path
from typing import Optional

import typer
from rich.console import Console

from dtr_cli import __version__
from dtr_cli.commands import fmt, export, push, report, build, publish, init, watch, doctor, diff, module, serve, schema, template
from dtr_cli.commands import test as test_cmd
from dtr_cli import config as config_module

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


@app.command()
def version() -> None:
    """Display the CLI version."""
    console.print(f"[cyan]DTR CLI[/cyan] v{__version__}")


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
        source = cfg.source()
        location = str(source) if source else "(defaults — no .dtr.yml found)"
        console.print(f"[bold cyan]DTR CLI Configuration[/bold cyan]  source: {location}")
        import yaml
        console.print(yaml.dump(cfg.to_dict(), default_flow_style=False, sort_keys=True))
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
