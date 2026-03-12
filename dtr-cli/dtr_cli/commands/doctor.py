"""Environment diagnostics command.

Provides `dtr doctor` to validate the development environment and report issues.

Usage:
    dtr doctor                          # Check environment in current directory
    dtr doctor --project-dir /path      # Check a specific project directory
    dtr doctor --fix                    # Attempt auto-fixes for missing config
"""

from __future__ import annotations

import re
import shutil
import subprocess
from pathlib import Path
from typing import Optional

import typer
from rich.console import Console
from rich.table import Table

console = Console()

# Status symbols
PASS = "[bold green]PASS[/bold green]"
FAIL = "[bold red]FAIL[/bold red]"
WARN = "[bold yellow]WARN[/bold yellow]"
INFO = "[bold cyan]INFO[/bold cyan]"


# ---------------------------------------------------------------------------
# Individual check functions (testable units, no side effects)
# ---------------------------------------------------------------------------


def check_java_version() -> tuple[str, str, str]:
    """Check Java version.

    Returns:
        (status, label, detail) where status is one of "pass", "fail", "warn"
    """
    java_path = shutil.which("java")
    if java_path is None:
        return ("fail", "Java", "Not found in PATH (requires Java 25+)")

    try:
        result = subprocess.run(
            ["java", "-version"],
            capture_output=True,
            text=True,
            timeout=10,
        )
        # Java prints version to stderr
        output = result.stderr or result.stdout
        match = re.search(r'version "?(\d+)(?:\.(\d+))?(?:\.(\d+))?', output)
        if match is None:
            return ("warn", "Java", f"Could not parse version from: {output.strip()!r}")

        major = int(match.group(1))
        version_str = match.group(0).replace("version ", "").strip('"')

        if major < 25:
            return ("fail", "Java", f"Version {version_str} — requires 25+")
        if major < 26:
            return ("warn", "Java", f"Version {version_str} — Java 26 recommended (JEP 516 features)")
        return ("pass", "Java", f"Version {version_str}")
    except subprocess.TimeoutExpired:
        return ("fail", "Java", "Timed out checking java version")
    except OSError as e:
        return ("fail", "Java", f"Error running java: {e}")


def check_maven_version() -> tuple[str, str, str]:
    """Check Maven version (mvn).

    Returns:
        (status, label, detail)
    """
    mvn_path = shutil.which("mvn")
    if mvn_path is None:
        return ("fail", "Maven (mvn)", "Not found in PATH (requires 4.0+)")

    try:
        result = subprocess.run(
            ["mvn", "--version"],
            capture_output=True,
            text=True,
            timeout=15,
        )
        output = result.stdout or result.stderr
        match = re.search(r"Apache Maven (\d+)\.(\d+)\.(\d+)", output)
        if match is None:
            return ("warn", "Maven (mvn)", f"Could not parse version from output")

        major = int(match.group(1))
        version_str = f"{match.group(1)}.{match.group(2)}.{match.group(3)}"

        if major < 4:
            return ("fail", "Maven (mvn)", f"Version {version_str} — requires 4.0+")
        return ("pass", "Maven (mvn)", f"Version {version_str}")
    except subprocess.TimeoutExpired:
        return ("fail", "Maven (mvn)", "Timed out checking mvn version")
    except OSError as e:
        return ("fail", "Maven (mvn)", f"Error running mvn: {e}")


def check_mvnd_version() -> tuple[str, str, str]:
    """Check Maven Daemon (mvnd) availability and version.

    Returns:
        (status, label, detail)
    """
    mvnd_path = shutil.which("mvnd")
    if mvnd_path is None:
        return ("warn", "mvnd", "Not found in PATH — install mvnd 2.0+ for faster builds")

    try:
        result = subprocess.run(
            ["mvnd", "--version"],
            capture_output=True,
            text=True,
            timeout=15,
        )
        output = result.stdout or result.stderr
        # mvnd version output varies; try to find a version number
        match = re.search(r"mvnd\s+(\d+\.\d+\.\d+)", output) or re.search(
            r"(\d+\.\d+\.\d+)", output
        )
        if match is None:
            return ("warn", "mvnd", f"Found at {mvnd_path} but could not parse version")

        version_str = match.group(1)
        major = int(version_str.split(".")[0])

        if major < 2:
            return ("warn", "mvnd", f"Version {version_str} — mvnd 2.0+ recommended")
        return ("pass", "mvnd", f"Version {version_str} at {mvnd_path}")
    except subprocess.TimeoutExpired:
        return ("warn", "mvnd", "Timed out checking mvnd version")
    except OSError as e:
        return ("warn", "mvnd", f"Error running mvnd: {e}")


def check_pom_xml(project_dir: Path) -> tuple[str, str, str]:
    """Check that pom.xml exists in project_dir.

    Returns:
        (status, label, detail)
    """
    pom = project_dir / "pom.xml"
    if pom.exists():
        return ("pass", "pom.xml", f"Found at {pom}")
    return ("fail", "pom.xml", f"Not found in {project_dir} — not a Maven project root")


def check_maven_config(project_dir: Path) -> tuple[str, str, str]:
    """Check that .mvn/maven.config exists and contains --enable-preview.

    Returns:
        (status, label, detail)
    """
    maven_config = project_dir / ".mvn" / "maven.config"
    if not maven_config.exists():
        return (
            "fail",
            ".mvn/maven.config",
            f"Not found at {maven_config} — --enable-preview flag missing",
        )

    content = maven_config.read_text(encoding="utf-8")
    if "--enable-preview" in content:
        return ("pass", ".mvn/maven.config", "Found, contains --enable-preview")
    return (
        "warn",
        ".mvn/maven.config",
        "Found but --enable-preview flag is missing (required for Java 26 preview features)",
    )


def check_pandoc() -> tuple[str, str, str]:
    """Check pandoc availability (optional).

    Returns:
        (status, label, detail)
    """
    pandoc_path = shutil.which("pandoc")
    if pandoc_path is None:
        return ("warn", "pandoc", "Not found — optional, needed for some format conversions")

    try:
        result = subprocess.run(
            ["pandoc", "--version"],
            capture_output=True,
            text=True,
            timeout=10,
        )
        output = result.stdout or result.stderr
        match = re.search(r"pandoc\s+(\d+\.\d+[\.\d]*)", output)
        version_str = match.group(1) if match else "unknown version"
        return ("pass", "pandoc", f"Version {version_str} at {pandoc_path}")
    except (subprocess.TimeoutExpired, OSError):
        return ("warn", "pandoc", f"Found at {pandoc_path} but could not check version")


def check_latex() -> tuple[str, str, str]:
    """Check latexmk/pdflatex availability (optional).

    Returns:
        (status, label, detail)
    """
    latexmk_path = shutil.which("latexmk")
    pdflatex_path = shutil.which("pdflatex")

    if latexmk_path:
        try:
            result = subprocess.run(
                ["latexmk", "--version"],
                capture_output=True,
                text=True,
                timeout=10,
            )
            output = result.stdout or result.stderr
            match = re.search(r"Latexmk,\s+John Collins,\s+\d+[\s\S]*?(\d+\.\d+\w*)", output)
            if not match:
                match = re.search(r"(\d+\.\d+\w*)", output)
            version_str = match.group(1) if match else "unknown version"
            return ("pass", "latexmk/pdflatex", f"latexmk {version_str} at {latexmk_path}")
        except (subprocess.TimeoutExpired, OSError):
            return ("pass", "latexmk/pdflatex", f"latexmk found at {latexmk_path}")

    if pdflatex_path:
        return ("pass", "latexmk/pdflatex", f"pdflatex found at {pdflatex_path} (latexmk preferred)")

    return ("warn", "latexmk/pdflatex", "Not found — optional, needed for PDF generation")


def check_dtr_yml(project_dir: Path) -> tuple[str, str, str]:
    """Check for .dtr.yml config file (informational).

    Returns:
        (status, label, detail)
    """
    dtr_yml = project_dir / ".dtr.yml"
    if dtr_yml.exists():
        return ("info", ".dtr.yml", f"Found at {dtr_yml}")
    return ("info", ".dtr.yml", f"Not found at {dtr_yml} — optional project config")


def check_python_packages() -> tuple[str, str, str]:
    """Check Python package versions (typer, rich, etc.).

    Returns:
        (status, label, detail)
    """
    packages: dict[str, str] = {}
    missing: list[str] = []

    for pkg in ("typer", "rich", "httpx", "jinja2"):
        try:
            import importlib.metadata
            version = importlib.metadata.version(pkg)
            packages[pkg] = version
        except importlib.metadata.PackageNotFoundError:
            missing.append(pkg)

    if missing:
        return (
            "warn",
            "Python packages",
            f"Missing: {', '.join(missing)}. Installed: {', '.join(f'{k}=={v}' for k, v in packages.items())}",
        )
    parts = ", ".join(f"{k}=={v}" for k, v in packages.items())
    return ("pass", "Python packages", parts)


# ---------------------------------------------------------------------------
# Auto-fix helpers
# ---------------------------------------------------------------------------


def fix_maven_config(project_dir: Path) -> str:
    """Create .mvn/maven.config with --enable-preview if missing or incomplete.

    Returns a human-readable message describing what was done.
    """
    mvn_dir = project_dir / ".mvn"
    maven_config = mvn_dir / "maven.config"

    mvn_dir.mkdir(parents=True, exist_ok=True)

    if not maven_config.exists():
        maven_config.write_text("--enable-preview\n", encoding="utf-8")
        return f"Created {maven_config} with --enable-preview"

    content = maven_config.read_text(encoding="utf-8")
    if "--enable-preview" not in content:
        updated = content.rstrip("\n") + "\n--enable-preview\n"
        maven_config.write_text(updated, encoding="utf-8")
        return f"Added --enable-preview to {maven_config}"

    return f"{maven_config} already contains --enable-preview (no change)"


# ---------------------------------------------------------------------------
# Status helpers
# ---------------------------------------------------------------------------

STATUS_SYMBOL = {
    "pass": "[bold green]✓[/bold green]",
    "fail": "[bold red]✗[/bold red]",
    "warn": "[bold yellow]![/bold yellow]",
    "info": "[bold cyan]i[/bold cyan]",
}

STATUS_LABEL = {
    "pass": PASS,
    "fail": FAIL,
    "warn": WARN,
    "info": INFO,
}


# ---------------------------------------------------------------------------
# Command
# ---------------------------------------------------------------------------


def doctor_command(
    project_dir: Path = typer.Option(
        Path.cwd(),
        "--project-dir",
        "-C",
        help="Maven project root directory to check",
    ),
    fix: bool = typer.Option(
        False,
        "--fix",
        help="Attempt auto-fixes (e.g., create .mvn/maven.config with --enable-preview)",
    ),
) -> None:
    """Validate the development environment and report issues.

    Checks Java version, Maven toolchain, project structure, and optional tools.
    Exits with code 0 if all required checks pass, 1 if any required check fails.

    Examples:

        Check environment in current directory:
        $ dtr doctor

        Check a specific project:
        $ dtr doctor --project-dir /path/to/project

        Auto-fix missing configuration:
        $ dtr doctor --fix
    """
    console.print()
    console.print("[bold]DTR Environment Doctor[/bold]")
    console.print(f"Project directory: [cyan]{project_dir.resolve()}[/cyan]")
    console.print()

    # Collect results: (status, label, detail, required)
    results: list[tuple[str, str, str, bool]] = []

    # Required checks
    results.append((*check_java_version(), True))
    results.append((*check_maven_version(), True))
    results.append((*check_mvnd_version(), False))          # warn only, not required
    results.append((*check_pom_xml(project_dir), True))
    results.append((*check_maven_config(project_dir), True))

    # Optional checks (warn/info only, never fail)
    results.append((*check_pandoc(), False))
    results.append((*check_latex(), False))
    results.append((*check_dtr_yml(project_dir), False))
    results.append((*check_python_packages(), False))

    # Build output table
    table = Table(show_header=True, header_style="bold", box=None, padding=(0, 1))
    table.add_column("Status", width=6, no_wrap=True)
    table.add_column("Check", min_width=22)
    table.add_column("Detail")

    for status, label, detail, _required in results:
        symbol = STATUS_SYMBOL.get(status, "?")
        status_label = STATUS_LABEL.get(status, status.upper())
        table.add_row(symbol, label, detail)

    console.print(table)
    console.print()

    # Summary counts
    fail_count = sum(1 for s, _, _, req in results if s == "fail" and req)
    warn_count = sum(1 for s, _, _, _ in results if s == "warn")
    pass_count = sum(1 for s, _, _, _ in results if s == "pass")

    console.print(
        f"Results: "
        f"[green]{pass_count} passed[/green]  "
        f"[yellow]{warn_count} warnings[/yellow]  "
        f"[red]{fail_count} failures[/red]"
    )

    # Auto-fix logic
    if fix:
        console.print()
        console.print("[bold]Running auto-fixes...[/bold]")
        fix_msg = fix_maven_config(project_dir)
        console.print(f"  [cyan]{fix_msg}[/cyan]")
        console.print()
        console.print("[cyan]Re-run `dtr doctor` to verify fixes.[/cyan]")

    if fail_count > 0:
        console.print()
        if not fix:
            console.print("[dim]Tip: run `dtr doctor --fix` to attempt auto-fixes.[/dim]")
        raise typer.Exit(code=1)
