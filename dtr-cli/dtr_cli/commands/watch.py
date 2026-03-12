"""Watch Java test source files and auto-trigger Maven builds on change.

Provides `dtr watch` to monitor `src/test/java/**/*.java` files for modifications
and automatically invoke `mvnd test -pl <module> -Dtest=<ChangedTestClass>`.

Usage:
    dtr watch                              # Watch all modules in cwd
    dtr watch --module dtr-integration-test  # Watch a specific module
    dtr watch --interval 1                 # Poll every 1 second
    dtr watch --project-dir /path/to/proj  # Custom project root
"""

import subprocess
import time
from pathlib import Path
from typing import Optional

import typer
from rich.console import Console
from rich.panel import Panel

console = Console()
app = typer.Typer(
    help="Watch Java test files and auto-trigger Maven builds",
    invoke_without_command=True,
    no_args_is_help=False,
)


# ---------------------------------------------------------------------------
# Core logic (pure functions, easy to unit-test)
# ---------------------------------------------------------------------------


def find_java_test_files(module_dir: Path) -> list[Path]:
    """Return all *.java files under <module>/src/test/java."""
    test_root = module_dir / "src" / "test" / "java"
    if not test_root.exists():
        return []
    return list(test_root.rglob("*.java"))


def snapshot_mtimes(files: list[Path]) -> dict[Path, float]:
    """Return a mapping of file path -> mtime for all existing files."""
    result: dict[Path, float] = {}
    for f in files:
        try:
            result[f] = f.stat().st_mtime
        except FileNotFoundError:
            pass
    return result


def detect_changes(
    old_snapshot: dict[Path, float],
    new_snapshot: dict[Path, float],
) -> list[Path]:
    """Return paths whose mtime changed or that are new since the last snapshot."""
    changed: list[Path] = []
    for path, mtime in new_snapshot.items():
        if old_snapshot.get(path) != mtime:
            changed.append(path)
    return changed


def class_name_from_path(java_file: Path) -> str:
    """Extract the simple class name (no extension) from a Java source path."""
    return java_file.stem


def module_name_from_path(java_file: Path, project_dir: Path) -> Optional[str]:
    """Return the Maven module directory name that owns *java_file*.

    Walks up from the file until it finds a directory that is a direct child of
    *project_dir* and contains a pom.xml.  Returns None when the file does not
    belong to any known module.
    """
    try:
        relative = java_file.relative_to(project_dir)
    except ValueError:
        return None

    # The first component of the relative path is the module directory name.
    parts = relative.parts
    if not parts:
        return None
    candidate = project_dir / parts[0]
    if (candidate / "pom.xml").exists():
        return parts[0]
    return None


def build_mvnd_command(
    module: str,
    test_class: str,
    project_dir: Path,
    mvnd_executable: str = "mvnd",
) -> list[str]:
    """Construct the mvnd command to run a single test class in a module."""
    return [
        mvnd_executable,
        "test",
        "-pl",
        module,
        f"-Dtest={test_class}",
        "--no-transfer-progress",
    ]


def run_build(cmd: list[str], project_dir: Path) -> int:
    """Run the build command and stream its output to the console.

    Returns the process exit code.
    """
    console.print(f"[bold cyan]$ {' '.join(cmd)}[/bold cyan]")
    try:
        proc = subprocess.run(
            cmd,
            cwd=project_dir,
            text=True,
        )
        return proc.returncode
    except FileNotFoundError:
        # mvnd / mvn not on PATH — fall back to mvn
        fallback = ["mvn"] + cmd[1:]
        console.print(
            f"[yellow]mvnd not found, retrying with mvn...[/yellow]"
        )
        try:
            proc = subprocess.run(fallback, cwd=project_dir, text=True)
            return proc.returncode
        except FileNotFoundError:
            console.print("[red]Neither mvnd nor mvn found on PATH.[/red]")
            return 127


def collect_modules(project_dir: Path, module_filter: Optional[str]) -> list[Path]:
    """Return a list of module root directories to watch.

    When *module_filter* is given, only that module is returned (if it exists
    and has a pom.xml).  Otherwise every direct child directory that contains a
    pom.xml is returned.
    """
    if module_filter:
        candidate = project_dir / module_filter
        if (candidate / "pom.xml").exists():
            return [candidate]
        # Also accept the case where project_dir itself is the module
        if (project_dir / "pom.xml").exists():
            return [project_dir]
        return []

    modules: list[Path] = []
    for child in sorted(project_dir.iterdir()):
        if child.is_dir() and (child / "pom.xml").exists():
            modules.append(child)
    # If no child modules found, treat project_dir itself as the module
    if not modules and (project_dir / "pom.xml").exists():
        modules.append(project_dir)
    return modules


# ---------------------------------------------------------------------------
# Watch loop
# ---------------------------------------------------------------------------


def watch_loop(
    project_dir: Path,
    modules: list[Path],
    interval: float,
) -> None:
    """Poll for file changes in a loop until KeyboardInterrupt."""
    # Gather initial snapshot
    all_files: list[Path] = []
    for m in modules:
        all_files.extend(find_java_test_files(m))

    snapshot = snapshot_mtimes(all_files)

    module_names = [m.name for m in modules]
    console.print(
        Panel(
            f"[bold green]Watching modules:[/bold green] {', '.join(module_names)}\n"
            f"[dim]Files tracked: {len(all_files)} | Poll interval: {interval}s[/dim]\n"
            f"[dim]Press Ctrl+C to stop[/dim]",
            title="[bold]dtr watch[/bold]",
            border_style="cyan",
        )
    )

    while True:
        time.sleep(interval)

        # Re-scan (files may have been added)
        current_files: list[Path] = []
        for m in modules:
            current_files.extend(find_java_test_files(m))

        current_snapshot = snapshot_mtimes(current_files)
        changed = detect_changes(snapshot, current_snapshot)
        snapshot = current_snapshot

        if not changed:
            continue

        for java_file in changed:
            console.print(
                f"[yellow]Changed:[/yellow] {java_file.relative_to(project_dir)}"
            )
            test_class = class_name_from_path(java_file)
            mod_name = module_name_from_path(java_file, project_dir)

            if mod_name is None:
                # Fallback: use the first module that contains this file
                for m in modules:
                    try:
                        java_file.relative_to(m)
                        mod_name = m.name
                        break
                    except ValueError:
                        continue

            if mod_name is None:
                console.print(
                    f"[red]Could not determine module for {java_file}, skipping.[/red]"
                )
                continue

            cmd = build_mvnd_command(mod_name, test_class, project_dir)
            exit_code = run_build(cmd, project_dir)

            if exit_code == 0:
                console.print(
                    f"[bold green]Build SUCCESS[/bold green] — {test_class}"
                )
            else:
                console.print(
                    f"[bold red]Build FAILED[/bold red] (exit {exit_code}) — {test_class}"
                )


# ---------------------------------------------------------------------------
# Typer command
# ---------------------------------------------------------------------------


@app.callback(invoke_without_command=True)
def run(
    module: Optional[str] = typer.Option(
        None,
        "--module",
        "-m",
        help="Maven module to watch (default: all modules with pom.xml)",
    ),
    interval: float = typer.Option(
        2.0,
        "--interval",
        "-i",
        help="Poll interval in seconds",
    ),
    project_dir: Path = typer.Option(
        Path.cwd(),
        "--project-dir",
        "-C",
        help="Maven project root directory",
    ),
) -> None:
    """Watch Java test source files and auto-trigger Maven builds on change.

    Polls src/test/java/**/*.java files every INTERVAL seconds.  When a file's
    modification time changes, runs:

        mvnd test -pl <module> -Dtest=<ChangedTestClass>

    Examples:

        Watch all modules in the current project:
        $ dtr watch

        Watch a single module:
        $ dtr watch --module dtr-integration-test

        Faster polling:
        $ dtr watch --interval 1

        Custom project root:
        $ dtr watch --project-dir /path/to/my-dtr-project
    """
    project_dir = project_dir.resolve()

    if not (project_dir / "pom.xml").exists():
        console.print(
            f"[red]No pom.xml found in {project_dir}. "
            "Are you inside a Maven project?[/red]"
        )
        raise typer.Exit(code=1)

    modules = collect_modules(project_dir, module)
    if not modules:
        console.print(
            f"[red]No modules found"
            + (f" matching '{module}'" if module else "")
            + f" in {project_dir}.[/red]"
        )
        raise typer.Exit(code=1)

    try:
        watch_loop(project_dir, modules, interval)
    except KeyboardInterrupt:
        console.print("\n[cyan]Watch stopped.[/cyan]")
