"""Discover and run DTR documentation tests.

Provides the `dtr test` command group with sub-commands:
- list:   Scan Maven modules for *DocTest.java and *Test.java files
- run:    Run a specific test class via mvnd
- filter: List tests matching a glob or regex pattern

Usage:
    dtr test list                                         # list all tests
    dtr test list --module dtr-integration-test           # limit to one module
    dtr test run --module dtr-integration-test --class PhDThesisDocTest
    dtr test filter --pattern "*Api*"
    dtr test filter --pattern ".*Doc.*" --module dtr-core
"""

import fnmatch
import re
import subprocess
from pathlib import Path
from typing import Optional

import typer
from rich.console import Console
from rich.table import Table

console = Console()
app = typer.Typer(
    help="Discover and run DTR documentation tests",
    no_args_is_help=True,
)


# ---------------------------------------------------------------------------
# Core logic (pure functions, easy to unit-test)
# ---------------------------------------------------------------------------


def _is_doc_or_test_class(name: str) -> bool:
    """Return True if *name* matches *DocTest.java or *Test.java."""
    stem = name if not name.endswith(".java") else name[:-5]
    return stem.endswith("DocTest") or stem.endswith("Test")


def _package_from_path(java_file: Path, module_dir: Path) -> str:
    """Derive the Java package string from a source file path.

    E.g. <module>/src/test/java/com/example/FooTest.java -> com.example
    Returns "" for the default package.
    """
    test_root = module_dir / "src" / "test" / "java"
    try:
        relative = java_file.relative_to(test_root)
    except ValueError:
        return ""
    parts = relative.parts[:-1]  # drop the filename
    return ".".join(parts)


def collect_test_files(
    project_dir: Path,
    module_filter: Optional[str] = None,
) -> list[dict]:
    """Return a list of dicts describing discovered test files.

    Each dict has keys: module, package, class_name, path (absolute Path).
    Only *DocTest.java and *Test.java files under src/test/java/ are returned.
    """
    results: list[dict] = []

    if module_filter:
        candidate = project_dir / module_filter
        modules = [candidate] if (candidate / "pom.xml").exists() else []
        # Tolerate project_dir itself being a single-module project
        if not modules and (project_dir / "pom.xml").exists():
            modules = [project_dir]
    else:
        modules = []
        for child in sorted(project_dir.iterdir()):
            if child.is_dir() and (child / "pom.xml").exists():
                modules.append(child)
        # Single-module project: project_dir itself
        if not modules and (project_dir / "pom.xml").exists():
            modules = [project_dir]

    for module_path in modules:
        module_name = module_path.name
        test_root = module_path / "src" / "test" / "java"
        if not test_root.exists():
            continue
        for java_file in sorted(test_root.rglob("*.java")):
            if not _is_doc_or_test_class(java_file.name):
                continue
            results.append(
                {
                    "module": module_name,
                    "package": _package_from_path(java_file, module_path),
                    "class_name": java_file.stem,
                    "path": java_file,
                }
            )

    return results


def filter_test_entries(
    entries: list[dict],
    pattern: str,
) -> list[dict]:
    """Return entries whose class_name matches *pattern*.

    The pattern is first tried as a glob (fnmatch).  If it contains regex
    metacharacters not valid in glob syntax it is also tried as a compiled
    regex, and the union is returned.
    """
    matched: list[dict] = []
    regex: Optional[re.Pattern] = None
    try:
        regex = re.compile(pattern)
    except re.error:
        regex = None

    for entry in entries:
        name = entry["class_name"]
        if fnmatch.fnmatch(name, pattern):
            matched.append(entry)
            continue
        if regex is not None and regex.search(name):
            matched.append(entry)

    return matched


def build_test_run_command(
    module: str,
    class_name: str,
    mvnd_executable: str = "mvnd",
) -> list[str]:
    """Construct the mvnd command to run a single test class."""
    return [
        mvnd_executable,
        "test",
        "-pl",
        module,
        f"-Dtest={class_name}",
        "--no-transfer-progress",
    ]


# ---------------------------------------------------------------------------
# Sub-commands
# ---------------------------------------------------------------------------


@app.command("list")
def list_tests(
    module: Optional[str] = typer.Option(
        None,
        "--module",
        "-m",
        help="Limit scan to this Maven module",
    ),
    project_dir: Path = typer.Option(
        None,
        "--project-dir",
        "-C",
        help="Maven project root directory (default: current directory)",
    ),
) -> None:
    """Scan Maven modules for *DocTest.java and *Test.java files.

    Displays results in a table with columns: Module, Package, Class, Path.

    Examples:

        List all tests across all modules:
        $ dtr test list

        List tests in a specific module:
        $ dtr test list --module dtr-integration-test

        List tests in a custom project directory:
        $ dtr test list --project-dir /path/to/my-project
    """
    resolved_dir = (project_dir or Path.cwd()).resolve()

    if not (resolved_dir / "pom.xml").exists():
        console.print(
            f"[red]No pom.xml found in {resolved_dir}. "
            "Are you inside a Maven project?[/red]"
        )
        raise typer.Exit(code=1)

    entries = collect_test_files(resolved_dir, module_filter=module)

    if not entries:
        msg = "No test files found"
        if module:
            msg += f" in module '{module}'"
        console.print(f"[yellow]{msg}.[/yellow]")
        raise typer.Exit(code=0)

    table = Table(title="DTR Test Classes", show_lines=False)
    table.add_column("Module", style="cyan", no_wrap=True)
    table.add_column("Package", style="dim")
    table.add_column("Class", style="bold green")
    table.add_column("Path", style="dim", overflow="fold")

    for entry in entries:
        table.add_row(
            entry["module"],
            entry["package"] or "(default)",
            entry["class_name"],
            str(entry["path"]),
        )

    console.print(table)
    console.print(f"[dim]{len(entries)} test class(es) found.[/dim]")


@app.command("run")
def run_test(
    class_name: str = typer.Option(
        ...,
        "--class",
        "-c",
        help="Test class name to run (e.g. PhDThesisDocTest)",
    ),
    module: str = typer.Option(
        ...,
        "--module",
        "-m",
        help="Maven module that owns the test class",
    ),
    verbose: bool = typer.Option(
        False,
        "--verbose",
        "-v",
        help="Stream full Maven output",
    ),
    project_dir: Path = typer.Option(
        None,
        "--project-dir",
        "-C",
        help="Maven project root directory (default: current directory)",
    ),
) -> None:
    """Run a specific test class via mvnd.

    Executes:  mvnd test -pl <module> -Dtest=<ClassName> --no-transfer-progress

    Examples:

        Run a doc test in the integration-test module:
        $ dtr test run --module dtr-integration-test --class PhDThesisDocTest

        Run with full Maven output:
        $ dtr test run --module dtr-core --class SomeTest --verbose

        Run from a custom project root:
        $ dtr test run --module mod --class Foo --project-dir /path/to/proj
    """
    resolved_dir = (project_dir or Path.cwd()).resolve()

    if not (resolved_dir / "pom.xml").exists():
        console.print(
            f"[red]No pom.xml found in {resolved_dir}. "
            "Are you inside a Maven project?[/red]"
        )
        raise typer.Exit(code=1)

    cmd = build_test_run_command(module, class_name)
    console.print(f"[bold cyan]$ {' '.join(cmd)}[/bold cyan]")

    try:
        kwargs: dict = {"cwd": resolved_dir, "text": True}
        if not verbose:
            kwargs["stdout"] = subprocess.PIPE
            kwargs["stderr"] = subprocess.STDOUT

        proc = subprocess.run(cmd, **kwargs)

        if not verbose and proc.stdout:
            console.print(proc.stdout)

        if proc.returncode == 0:
            console.print(f"[bold green]Test PASSED[/bold green] — {class_name}")
        else:
            console.print(
                f"[bold red]Test FAILED[/bold red] (exit {proc.returncode}) — {class_name}"
            )
            raise typer.Exit(code=proc.returncode)

    except FileNotFoundError:
        # mvnd not on PATH — fall back to mvn
        fallback = ["mvn"] + cmd[1:]
        console.print("[yellow]mvnd not found, retrying with mvn...[/yellow]")
        console.print(f"[bold cyan]$ {' '.join(fallback)}[/bold cyan]")
        try:
            fallback_kwargs: dict = {"cwd": resolved_dir, "text": True}
            if not verbose:
                fallback_kwargs["stdout"] = subprocess.PIPE
                fallback_kwargs["stderr"] = subprocess.STDOUT

            proc2 = subprocess.run(fallback, **fallback_kwargs)

            if not verbose and proc2.stdout:
                console.print(proc2.stdout)

            if proc2.returncode == 0:
                console.print(f"[bold green]Test PASSED[/bold green] — {class_name}")
            else:
                console.print(
                    f"[bold red]Test FAILED[/bold red] (exit {proc2.returncode}) — {class_name}"
                )
                raise typer.Exit(code=proc2.returncode)
        except FileNotFoundError:
            console.print("[red]Neither mvnd nor mvn found on PATH.[/red]")
            raise typer.Exit(code=127)


@app.command("filter")
def filter_tests(
    pattern: str = typer.Option(
        ...,
        "--pattern",
        "-p",
        help="Glob or regex pattern to match against class names (e.g. '*Api*')",
    ),
    module: Optional[str] = typer.Option(
        None,
        "--module",
        "-m",
        help="Limit scan to this Maven module",
    ),
    project_dir: Path = typer.Option(
        None,
        "--project-dir",
        "-C",
        help="Maven project root directory (default: current directory)",
    ),
) -> None:
    """List test classes whose name matches a glob or regex pattern.

    The pattern is matched against the simple class name (not the FQCN).
    Glob syntax is tried first; if the pattern is a valid regex it is also
    applied and results are merged.

    Examples:

        Find all API-related doc tests:
        $ dtr test filter --pattern "*Api*"

        Find tests matching a regex:
        $ dtr test filter --pattern ".*Doc.*"

        Scope to a single module:
        $ dtr test filter --pattern "*Thesis*" --module dtr-integration-test
    """
    resolved_dir = (project_dir or Path.cwd()).resolve()

    if not (resolved_dir / "pom.xml").exists():
        console.print(
            f"[red]No pom.xml found in {resolved_dir}. "
            "Are you inside a Maven project?[/red]"
        )
        raise typer.Exit(code=1)

    all_entries = collect_test_files(resolved_dir, module_filter=module)
    matched = filter_test_entries(all_entries, pattern)

    if not matched:
        console.print(
            f"[yellow]No tests matching '{pattern}'"
            + (f" in module '{module}'" if module else "")
            + ".[/yellow]"
        )
        raise typer.Exit(code=0)

    table = Table(title=f"Tests matching '{pattern}'", show_lines=False)
    table.add_column("Module", style="cyan", no_wrap=True)
    table.add_column("Package", style="dim")
    table.add_column("Class", style="bold green")
    table.add_column("Path", style="dim", overflow="fold")

    for entry in matched:
        table.add_row(
            entry["module"],
            entry["package"] or "(default)",
            entry["class_name"],
            str(entry["path"]),
        )

    console.print(table)
    console.print(f"[dim]{len(matched)} match(es) for pattern '{pattern}'.[/dim]")
