"""Maven module inspection commands.

Provides `dtr module` sub-commands to inspect the Maven multi-module project
structure without running a build.

Usage:
    dtr module list                        # List all modules from root pom.xml
    dtr module info MODULE_NAME            # Detailed info for a specific module
    dtr module tree                        # Show inter-module dependency tree
"""

from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Optional

import typer
from rich.console import Console
from rich.table import Table
from rich.tree import Tree

console = Console()
app = typer.Typer(help="Inspect Maven module structure")

# Maven POM namespace
_NS = "http://maven.apache.org/POM/4.0.0"
_NS_MAP = {"m": _NS}


# ---------------------------------------------------------------------------
# XML helpers
# ---------------------------------------------------------------------------


def _parse_pom(pom_path: Path) -> ET.Element:
    """Parse a pom.xml file and return its root element.

    Raises:
        FileNotFoundError: if pom_path does not exist.
        ET.ParseError:     if the XML is malformed.
    """
    if not pom_path.exists():
        raise FileNotFoundError(f"pom.xml not found: {pom_path}")
    return ET.parse(str(pom_path)).getroot()


def _tag(local: str) -> str:
    """Return a Clark-notation tag name for the Maven namespace."""
    return f"{{{_NS}}}{local}"


def _find_text(root: ET.Element, *path_parts: str, default: str = "") -> str:
    """Walk a sequence of tag names from *root* and return the text, or default."""
    node = root
    for part in path_parts:
        node = node.find(_tag(part))  # type: ignore[assignment]
        if node is None:
            return default
    return (node.text or "").strip() if node is not None else default


def _find_root_pom(project_dir: Path) -> Path:
    """Return the root pom.xml path, raising typer.BadParameter if absent."""
    pom = project_dir / "pom.xml"
    if not pom.exists():
        raise typer.BadParameter(
            f"pom.xml not found in {project_dir}\n"
            "Make sure you're in the root of a Maven project."
        )
    return pom


# ---------------------------------------------------------------------------
# Module discovery helpers
# ---------------------------------------------------------------------------


def _get_module_names(root_pom: ET.Element) -> list[str]:
    """Return the list of <module> entries declared in a pom.xml root element."""
    modules_el = root_pom.find(_tag("modules"))
    if modules_el is None:
        return []
    return [
        (el.text or "").strip()
        for el in modules_el.findall(_tag("module"))
        if (el.text or "").strip()
    ]


def _count_test_files(module_path: Path) -> int:
    """Count Java test source files inside src/test/java, recursively."""
    test_dir = module_path / "src" / "test" / "java"
    if not test_dir.is_dir():
        return 0
    return sum(1 for _ in test_dir.rglob("*.java"))


def _count_exports(module_path: Path) -> int:
    """Count exported documentation files in target/docs/test-results (*.md, *.html)."""
    export_dir = module_path / "target" / "docs" / "test-results"
    if not export_dir.is_dir():
        return 0
    return sum(1 for _ in export_dir.rglob("*") if _.is_file())


def _get_inter_module_deps(
    module_path: Path, known_artifact_ids: set[str]
) -> list[str]:
    """Return dependency artifactIds that are also known Maven modules."""
    pom_path = module_path / "pom.xml"
    if not pom_path.exists():
        return []
    try:
        root = _parse_pom(pom_path)
    except ET.ParseError:
        return []

    deps: list[str] = []
    dependencies_el = root.find(_tag("dependencies"))
    if dependencies_el is None:
        return []
    for dep in dependencies_el.findall(_tag("dependency")):
        artifact_id = _find_text(dep, "artifactId")
        if artifact_id and artifact_id in known_artifact_ids:
            deps.append(artifact_id)
    return deps


# ---------------------------------------------------------------------------
# `dtr module list`
# ---------------------------------------------------------------------------


@app.command(name="list")
def list_modules(
    project_dir: Path = typer.Option(
        None,
        "--project-dir",
        "-C",
        help="Maven project root directory (default: current directory)",
    ),
) -> None:
    """List all Maven modules declared in the root pom.xml.

    Displays a Rich table with Module name, relative path, whether the module
    has test sources, and how many documentation exports are present.

    \b
    Examples:
        dtr module list
        dtr module list --project-dir /path/to/project
    """
    if project_dir is None:
        project_dir = Path.cwd()

    root_pom_path = _find_root_pom(project_dir)

    try:
        root = _parse_pom(root_pom_path)
    except ET.ParseError as exc:
        console.print(f"[red]Invalid XML in {root_pom_path}: {exc}[/red]")
        raise typer.Exit(code=1)

    module_names = _get_module_names(root)

    if not module_names:
        console.print(
            "[yellow]No <module> entries found in the root pom.xml.[/yellow]"
        )
        return

    table = Table(title="Maven Modules", show_header=True, header_style="bold cyan")
    table.add_column("Module", style="bold")
    table.add_column("Path")
    table.add_column("Has Tests", justify="center")
    table.add_column("Export Count", justify="right")

    for module_name in module_names:
        module_path = project_dir / module_name
        has_tests = _count_test_files(module_path) > 0
        export_count = _count_exports(module_path)

        table.add_row(
            module_name,
            str(module_path.relative_to(project_dir)) if module_path.exists() else module_name,
            "[green]yes[/green]" if has_tests else "[dim]no[/dim]",
            str(export_count) if export_count > 0 else "[dim]0[/dim]",
        )

    console.print(table)
    console.print(f"\n[dim]Root:[/dim] {project_dir}")


# ---------------------------------------------------------------------------
# `dtr module info MODULE_NAME`
# ---------------------------------------------------------------------------


@app.command(name="info")
def module_info(
    module_name: str = typer.Argument(..., help="Name of the module (directory name)"),
    project_dir: Path = typer.Option(
        None,
        "--project-dir",
        "-C",
        help="Maven project root directory (default: current directory)",
    ),
) -> None:
    """Show detailed information for a specific Maven module.

    Parses the module's pom.xml to display: artifactId, version, packaging,
    dependencies, test count, and export directory status.

    \b
    Examples:
        dtr module info dtr-core
        dtr module info dtr-integration-test --project-dir /path/to/project
    """
    if project_dir is None:
        project_dir = Path.cwd()

    module_path = project_dir / module_name
    pom_path = module_path / "pom.xml"

    if not module_path.exists():
        console.print(f"[red]Module directory not found: {module_path}[/red]")
        raise typer.Exit(code=1)

    if not pom_path.exists():
        console.print(f"[red]pom.xml not found in module: {pom_path}[/red]")
        raise typer.Exit(code=1)

    try:
        root = _parse_pom(pom_path)
    except ET.ParseError as exc:
        console.print(f"[red]Invalid XML in {pom_path}: {exc}[/red]")
        raise typer.Exit(code=1)

    # Extract basic fields — fall back to parent values when absent
    artifact_id = _find_text(root, "artifactId") or module_name
    version = _find_text(root, "version")
    if not version:
        version = _find_text(root, "parent", "version") or "(inherited)"
    packaging = _find_text(root, "packaging") or "jar"
    description = _find_text(root, "description") or "(no description)"

    # Dependencies
    deps: list[tuple[str, str, str]] = []
    dependencies_el = root.find(_tag("dependencies"))
    if dependencies_el is not None:
        for dep in dependencies_el.findall(_tag("dependency")):
            group_id = _find_text(dep, "groupId")
            dep_artifact = _find_text(dep, "artifactId")
            scope = _find_text(dep, "scope") or "compile"
            deps.append((group_id, dep_artifact, scope))

    test_count = _count_test_files(module_path)
    export_count = _count_exports(module_path)
    export_dir = module_path / "target" / "docs" / "test-results"

    console.print(f"\n[bold cyan]Module:[/bold cyan] {artifact_id}")
    console.print(f"  [dim]Version:[/dim]     {version}")
    console.print(f"  [dim]Packaging:[/dim]   {packaging}")
    console.print(f"  [dim]Description:[/dim] {description}")
    console.print(f"  [dim]Path:[/dim]        {module_path}")
    console.print()
    console.print(f"  [dim]Test files:[/dim]  {test_count}")
    console.print(
        f"  [dim]Export dir:[/dim]  "
        + (f"[green]{export_dir}[/green] ({export_count} files)" if export_dir.exists() else "[dim]not built yet[/dim]")
    )

    if deps:
        console.print()
        dep_table = Table(
            title="Dependencies",
            show_header=True,
            header_style="bold",
            show_lines=False,
        )
        dep_table.add_column("GroupId", style="dim")
        dep_table.add_column("ArtifactId")
        dep_table.add_column("Scope", justify="right", style="dim")

        for group_id, dep_artifact, scope in deps:
            dep_table.add_row(group_id, dep_artifact, scope)

        console.print(dep_table)
    else:
        console.print("\n  [dim]No dependencies declared.[/dim]")


# ---------------------------------------------------------------------------
# `dtr module tree`
# ---------------------------------------------------------------------------


@app.command(name="tree")
def module_tree(
    project_dir: Path = typer.Option(
        None,
        "--project-dir",
        "-C",
        help="Maven project root directory (default: current directory)",
    ),
) -> None:
    """Show the inter-module dependency tree as a Rich tree visualization.

    Parses each module's pom.xml and identifies dependencies on sibling modules
    to build a dependency graph.

    \b
    Examples:
        dtr module tree
        dtr module tree --project-dir /path/to/project
    """
    if project_dir is None:
        project_dir = Path.cwd()

    root_pom_path = _find_root_pom(project_dir)

    try:
        root = _parse_pom(root_pom_path)
    except ET.ParseError as exc:
        console.print(f"[red]Invalid XML in {root_pom_path}: {exc}[/red]")
        raise typer.Exit(code=1)

    root_artifact = _find_text(root, "artifactId") or project_dir.name
    module_names = _get_module_names(root)

    if not module_names:
        console.print(
            "[yellow]No <module> entries found in the root pom.xml.[/yellow]"
        )
        return

    # Collect artifact IDs for each known module name so we can resolve deps
    module_artifact_map: dict[str, str] = {}  # module_dir_name -> artifactId
    for module_name in module_names:
        pom_path = project_dir / module_name / "pom.xml"
        artifact_id = module_name  # default
        if pom_path.exists():
            try:
                mod_root = _parse_pom(pom_path)
                artifact_id = _find_text(mod_root, "artifactId") or module_name
            except ET.ParseError:
                pass
        module_artifact_map[module_name] = artifact_id

    known_artifacts: set[str] = set(module_artifact_map.values())

    # Build inter-module dependency map: artifactId -> [dep artifactIds]
    dep_map: dict[str, list[str]] = {}
    for module_name, artifact_id in module_artifact_map.items():
        module_path = project_dir / module_name
        deps = _get_inter_module_deps(module_path, known_artifacts)
        dep_map[artifact_id] = deps

    # Build the Rich tree
    tree = Tree(
        f"[bold cyan]{root_artifact}[/bold cyan] [dim](root)[/dim]",
        guide_style="dim",
    )

    for module_name in module_names:
        artifact_id = module_artifact_map[module_name]
        inter_deps = dep_map.get(artifact_id, [])

        module_branch = tree.add(
            f"[green]{artifact_id}[/green]"
        )

        if inter_deps:
            for dep in sorted(inter_deps):
                module_branch.add(f"[yellow]{dep}[/yellow] [dim](depends on)[/dim]")
        else:
            module_branch.add("[dim]no intra-project dependencies[/dim]")

    console.print(tree)
    console.print(
        f"\n[dim]{len(module_names)} module(s) found in {root_pom_path}[/dim]"
    )
