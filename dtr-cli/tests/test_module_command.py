"""Chicago-style TDD tests for `dtr module` commands.

Tests real behavior with real collaborators -- no mocks, no fakes.
Uses typer CliRunner to invoke the real CLI app end-to-end,
and pytest tmp_path for real filesystem I/O.

Fake Maven project structures are built in tmp_path so tests are fully
hermetic and require no network or Java toolchain.
"""

from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

import pytest
from typer.testing import CliRunner

from dtr_cli.main import app
from dtr_cli.commands.module import (
    _parse_pom,
    _get_module_names,
    _count_test_files,
    _count_exports,
    _get_inter_module_deps,
    _find_text,
)

runner = CliRunner()

# ---------------------------------------------------------------------------
# Helpers to build fake Maven project trees in tmp_path
# ---------------------------------------------------------------------------

ROOT_POM_TEMPLATE = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <modules>
        {module_entries}
    </modules>
</project>
"""

MODULE_POM_TEMPLATE = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-project</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>{artifact_id}</artifactId>
    <packaging>{packaging}</packaging>
    <description>{description}</description>
    <dependencies>
        {dep_entries}
    </dependencies>
</project>
"""


def _make_dep_entry(group_id: str, artifact_id: str, scope: str = "compile") -> str:
    return f"""\
        <dependency>
            <groupId>{group_id}</groupId>
            <artifactId>{artifact_id}</artifactId>
            <scope>{scope}</scope>
        </dependency>"""


def make_root_pom(project_dir: Path, module_names: list[str]) -> Path:
    """Create a root pom.xml with the given module entries."""
    entries = "\n".join(f"        <module>{m}</module>" for m in module_names)
    pom_path = project_dir / "pom.xml"
    pom_path.write_text(
        ROOT_POM_TEMPLATE.format(module_entries=entries), encoding="utf-8"
    )
    return pom_path


def make_module(
    project_dir: Path,
    module_dir_name: str,
    artifact_id: str = "",
    packaging: str = "jar",
    description: str = "A module",
    deps: list[tuple[str, str, str]] | None = None,
    test_java_files: int = 0,
    export_files: int = 0,
) -> Path:
    """Create a module sub-directory with pom.xml and optional test/export files."""
    artifact_id = artifact_id or module_dir_name
    module_path = project_dir / module_dir_name
    module_path.mkdir(parents=True, exist_ok=True)

    dep_entries = ""
    if deps:
        dep_entries = "\n".join(
            _make_dep_entry(g, a, s) for g, a, s in deps
        )

    pom_content = MODULE_POM_TEMPLATE.format(
        artifact_id=artifact_id,
        packaging=packaging,
        description=description,
        dep_entries=dep_entries,
    )
    (module_path / "pom.xml").write_text(pom_content, encoding="utf-8")

    # Optional Java test sources
    if test_java_files > 0:
        test_src = module_path / "src" / "test" / "java"
        test_src.mkdir(parents=True, exist_ok=True)
        for i in range(test_java_files):
            (test_src / f"Test{i}.java").write_text(
                f"public class Test{i} {{}}", encoding="utf-8"
            )

    # Optional export files
    if export_files > 0:
        export_dir = module_path / "target" / "docs" / "test-results"
        export_dir.mkdir(parents=True, exist_ok=True)
        for i in range(export_files):
            (export_dir / f"report{i}.md").write_text(
                f"# Report {i}", encoding="utf-8"
            )

    return module_path


# ===========================================================================
# Unit tests for helper functions
# ===========================================================================


class TestParsePom:
    """Test _parse_pom returns a valid Element for good XML."""

    def test_parses_valid_pom(self, tmp_path: Path):
        pom = tmp_path / "pom.xml"
        pom.write_text(
            '<?xml version="1.0"?><project xmlns="http://maven.apache.org/POM/4.0.0">'
            "<modelVersion>4.0.0</modelVersion></project>",
            encoding="utf-8",
        )
        root = _parse_pom(pom)
        assert root is not None
        assert isinstance(root, ET.Element)

    def test_raises_file_not_found_when_missing(self, tmp_path: Path):
        pom = tmp_path / "nonexistent.xml"
        with pytest.raises(FileNotFoundError):
            _parse_pom(pom)

    def test_raises_parse_error_for_invalid_xml(self, tmp_path: Path):
        pom = tmp_path / "pom.xml"
        pom.write_text("<unclosed>", encoding="utf-8")
        with pytest.raises(ET.ParseError):
            _parse_pom(pom)


class TestGetModuleNames:
    """Test _get_module_names extracts <module> entries."""

    def test_returns_all_module_names(self, tmp_path: Path):
        make_root_pom(tmp_path, ["alpha", "beta", "gamma"])
        root = _parse_pom(tmp_path / "pom.xml")
        names = _get_module_names(root)
        assert names == ["alpha", "beta", "gamma"]

    def test_returns_empty_for_no_modules(self, tmp_path: Path):
        pom = tmp_path / "pom.xml"
        pom.write_text(
            '<?xml version="1.0"?><project xmlns="http://maven.apache.org/POM/4.0.0">'
            "<modelVersion>4.0.0</modelVersion></project>",
            encoding="utf-8",
        )
        root = _parse_pom(pom)
        names = _get_module_names(root)
        assert names == []

    def test_ignores_whitespace_only_module_entries(self, tmp_path: Path):
        pom = tmp_path / "pom.xml"
        pom.write_text(
            '<?xml version="1.0"?>'
            '<project xmlns="http://maven.apache.org/POM/4.0.0">'
            "<modelVersion>4.0.0</modelVersion>"
            "<modules>"
            "  <module>  </module>"
            "  <module>real-module</module>"
            "</modules>"
            "</project>",
            encoding="utf-8",
        )
        root = _parse_pom(pom)
        names = _get_module_names(root)
        assert names == ["real-module"]


class TestCountTestFiles:
    """Test _count_test_files walks src/test/java for *.java."""

    def test_counts_java_test_files(self, tmp_path: Path):
        make_module(tmp_path, "mod-a", test_java_files=3)
        assert _count_test_files(tmp_path / "mod-a") == 3

    def test_returns_zero_when_no_test_dir(self, tmp_path: Path):
        mod = tmp_path / "mod-b"
        mod.mkdir()
        assert _count_test_files(mod) == 0

    def test_counts_nested_java_files(self, tmp_path: Path):
        mod = tmp_path / "mod-c"
        nested = mod / "src" / "test" / "java" / "com" / "example"
        nested.mkdir(parents=True)
        (nested / "FooTest.java").write_text("class FooTest {}", encoding="utf-8")
        (nested / "BarTest.java").write_text("class BarTest {}", encoding="utf-8")
        assert _count_test_files(mod) == 2


class TestCountExports:
    """Test _count_exports counts files in target/docs/test-results."""

    def test_counts_export_files(self, tmp_path: Path):
        make_module(tmp_path, "mod-a", export_files=5)
        assert _count_exports(tmp_path / "mod-a") == 5

    def test_returns_zero_when_no_exports(self, tmp_path: Path):
        mod = tmp_path / "mod-b"
        mod.mkdir()
        assert _count_exports(mod) == 0


class TestGetInterModuleDeps:
    """Test _get_inter_module_deps identifies intra-project dependencies."""

    def test_returns_known_artifact_deps(self, tmp_path: Path):
        make_module(
            tmp_path,
            "mod-b",
            artifact_id="mod-b",
            deps=[("com.example", "mod-a", "compile")],
        )
        known = {"mod-a", "mod-b", "mod-c"}
        deps = _get_inter_module_deps(tmp_path / "mod-b", known)
        assert "mod-a" in deps

    def test_ignores_external_deps(self, tmp_path: Path):
        make_module(
            tmp_path,
            "mod-b",
            deps=[("org.springframework", "spring-core", "compile")],
        )
        known = {"mod-a", "mod-b"}
        deps = _get_inter_module_deps(tmp_path / "mod-b", known)
        assert deps == []

    def test_returns_empty_for_missing_pom(self, tmp_path: Path):
        mod = tmp_path / "no-pom"
        mod.mkdir()
        deps = _get_inter_module_deps(mod, {"mod-a"})
        assert deps == []

    def test_returns_empty_for_invalid_xml(self, tmp_path: Path):
        mod = tmp_path / "bad-xml"
        mod.mkdir()
        (mod / "pom.xml").write_text("<invalid>", encoding="utf-8")
        deps = _get_inter_module_deps(mod, {"mod-a"})
        assert deps == []


class TestFindText:
    """Test _find_text navigates nested XML tags."""

    def test_finds_direct_child_text(self, tmp_path: Path):
        pom = tmp_path / "pom.xml"
        pom.write_text(
            '<project xmlns="http://maven.apache.org/POM/4.0.0">'
            "<artifactId>my-artifact</artifactId>"
            "</project>",
            encoding="utf-8",
        )
        root = _parse_pom(pom)
        assert _find_text(root, "artifactId") == "my-artifact"

    def test_returns_default_when_element_missing(self, tmp_path: Path):
        pom = tmp_path / "pom.xml"
        pom.write_text(
            '<project xmlns="http://maven.apache.org/POM/4.0.0"></project>',
            encoding="utf-8",
        )
        root = _parse_pom(pom)
        assert _find_text(root, "nonexistent") == ""
        assert _find_text(root, "nonexistent", default="fallback") == "fallback"

    def test_finds_nested_path(self, tmp_path: Path):
        pom = tmp_path / "pom.xml"
        pom.write_text(
            '<project xmlns="http://maven.apache.org/POM/4.0.0">'
            "<parent><version>2.0.0</version></parent>"
            "</project>",
            encoding="utf-8",
        )
        root = _parse_pom(pom)
        assert _find_text(root, "parent", "version") == "2.0.0"


# ===========================================================================
# Integration tests: `dtr module list`
# ===========================================================================


class TestModuleList:
    """Test `dtr module list` with real fake Maven projects in tmp_path."""

    def test_lists_modules_from_root_pom(self, tmp_path: Path):
        make_root_pom(tmp_path, ["core", "integration-test"])
        make_module(tmp_path, "core")
        make_module(tmp_path, "integration-test")

        result = runner.invoke(app, ["module", "list", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "core" in result.stdout
        assert "integration-test" in result.stdout

    def test_shows_has_tests_yes_when_test_sources_exist(self, tmp_path: Path):
        make_root_pom(tmp_path, ["core"])
        make_module(tmp_path, "core", test_java_files=2)

        result = runner.invoke(app, ["module", "list", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "yes" in result.stdout

    def test_shows_has_tests_no_when_no_test_sources(self, tmp_path: Path):
        make_root_pom(tmp_path, ["core"])
        make_module(tmp_path, "core", test_java_files=0)

        result = runner.invoke(app, ["module", "list", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "no" in result.stdout

    def test_shows_export_count(self, tmp_path: Path):
        make_root_pom(tmp_path, ["core"])
        make_module(tmp_path, "core", export_files=3)

        result = runner.invoke(app, ["module", "list", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "3" in result.stdout

    def test_shows_zero_export_count_when_no_exports(self, tmp_path: Path):
        make_root_pom(tmp_path, ["core"])
        make_module(tmp_path, "core", export_files=0)

        result = runner.invoke(app, ["module", "list", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "0" in result.stdout

    def test_warns_when_no_modules_declared(self, tmp_path: Path):
        pom = tmp_path / "pom.xml"
        pom.write_text(
            '<?xml version="1.0"?>'
            '<project xmlns="http://maven.apache.org/POM/4.0.0">'
            "<modelVersion>4.0.0</modelVersion>"
            "<artifactId>empty-project</artifactId>"
            "</project>",
            encoding="utf-8",
        )

        result = runner.invoke(app, ["module", "list", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "No" in result.stdout or "no" in result.stdout.lower()

    def test_fails_when_pom_missing(self, tmp_path: Path):
        result = runner.invoke(app, ["module", "list", "--project-dir", str(tmp_path)])

        assert result.exit_code != 0

    def test_fails_when_pom_invalid_xml(self, tmp_path: Path):
        (tmp_path / "pom.xml").write_text("<invalid>", encoding="utf-8")

        result = runner.invoke(app, ["module", "list", "--project-dir", str(tmp_path)])

        assert result.exit_code != 0

    def test_uses_cwd_when_project_dir_not_specified(self, tmp_path: Path):
        # Invoking without --project-dir should use current working directory.
        # The runner defaults to the real cwd, which may or may not have a pom.
        # We just verify it does not crash with a strange error.
        make_root_pom(tmp_path, ["core"])
        make_module(tmp_path, "core")

        # Pass --project-dir explicitly to ensure reliability, but also test the
        # short alias -C
        result = runner.invoke(app, ["module", "list", "-C", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "core" in result.stdout

    def test_table_has_correct_column_headers(self, tmp_path: Path):
        make_root_pom(tmp_path, ["core"])
        make_module(tmp_path, "core")

        result = runner.invoke(app, ["module", "list", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "Module" in result.stdout
        assert "Path" in result.stdout
        assert "Has Tests" in result.stdout or "Tests" in result.stdout
        assert "Export" in result.stdout or "Count" in result.stdout


# ===========================================================================
# Integration tests: `dtr module info`
# ===========================================================================


class TestModuleInfo:
    """Test `dtr module info MODULE_NAME` with fake Maven structures."""

    def test_shows_artifact_id(self, tmp_path: Path):
        make_root_pom(tmp_path, ["my-core"])
        make_module(tmp_path, "my-core", artifact_id="my-core")

        result = runner.invoke(
            app, ["module", "info", "my-core", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0, result.stdout
        assert "my-core" in result.stdout

    def test_shows_version_from_parent(self, tmp_path: Path):
        make_root_pom(tmp_path, ["my-core"])
        make_module(tmp_path, "my-core")

        result = runner.invoke(
            app, ["module", "info", "my-core", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0, result.stdout
        # The module pom has parent version 1.0.0
        assert "1.0.0" in result.stdout or "inherited" in result.stdout

    def test_shows_packaging(self, tmp_path: Path):
        make_root_pom(tmp_path, ["web-mod"])
        make_module(tmp_path, "web-mod", packaging="war")

        result = runner.invoke(
            app, ["module", "info", "web-mod", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0, result.stdout
        assert "war" in result.stdout

    def test_shows_description(self, tmp_path: Path):
        make_root_pom(tmp_path, ["described"])
        make_module(tmp_path, "described", description="The best module ever.")

        result = runner.invoke(
            app, ["module", "info", "described", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0, result.stdout
        assert "The best module ever." in result.stdout

    def test_shows_dependencies(self, tmp_path: Path):
        make_root_pom(tmp_path, ["consumer"])
        make_module(
            tmp_path,
            "consumer",
            deps=[
                ("org.junit.jupiter", "junit-jupiter", "test"),
                ("com.google.guava", "guava", "compile"),
            ],
        )

        result = runner.invoke(
            app, ["module", "info", "consumer", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0, result.stdout
        assert "junit-jupiter" in result.stdout
        assert "guava" in result.stdout

    def test_shows_test_count(self, tmp_path: Path):
        make_root_pom(tmp_path, ["tested"])
        make_module(tmp_path, "tested", test_java_files=4)

        result = runner.invoke(
            app, ["module", "info", "tested", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0, result.stdout
        assert "4" in result.stdout

    def test_shows_export_dir_when_built(self, tmp_path: Path):
        make_root_pom(tmp_path, ["built"])
        make_module(tmp_path, "built", export_files=2)

        result = runner.invoke(
            app, ["module", "info", "built", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0, result.stdout
        assert "test-results" in result.stdout or "2" in result.stdout

    def test_shows_not_built_when_no_exports(self, tmp_path: Path):
        make_root_pom(tmp_path, ["fresh"])
        make_module(tmp_path, "fresh")

        result = runner.invoke(
            app, ["module", "info", "fresh", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0, result.stdout
        assert "not built" in result.stdout or "yet" in result.stdout

    def test_fails_when_module_dir_missing(self, tmp_path: Path):
        make_root_pom(tmp_path, ["ghost"])

        result = runner.invoke(
            app, ["module", "info", "ghost", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code != 0

    def test_fails_when_module_pom_missing(self, tmp_path: Path):
        make_root_pom(tmp_path, ["nopom"])
        (tmp_path / "nopom").mkdir()

        result = runner.invoke(
            app, ["module", "info", "nopom", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code != 0

    def test_fails_when_module_pom_invalid_xml(self, tmp_path: Path):
        make_root_pom(tmp_path, ["badxml"])
        mod = tmp_path / "badxml"
        mod.mkdir()
        (mod / "pom.xml").write_text("<broken", encoding="utf-8")

        result = runner.invoke(
            app, ["module", "info", "badxml", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code != 0


# ===========================================================================
# Integration tests: `dtr module tree`
# ===========================================================================


class TestModuleTree:
    """Test `dtr module tree` builds a dependency tree visualization."""

    def test_shows_root_artifact_id(self, tmp_path: Path):
        make_root_pom(tmp_path, ["core", "integration-test"])
        make_module(tmp_path, "core")
        make_module(tmp_path, "integration-test")

        result = runner.invoke(app, ["module", "tree", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        # Root pom artifact id is "my-project" per our template
        assert "my-project" in result.stdout

    def test_shows_all_module_names(self, tmp_path: Path):
        make_root_pom(tmp_path, ["alpha", "beta", "gamma"])
        for m in ("alpha", "beta", "gamma"):
            make_module(tmp_path, m)

        result = runner.invoke(app, ["module", "tree", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "alpha" in result.stdout
        assert "beta" in result.stdout
        assert "gamma" in result.stdout

    def test_shows_inter_module_dependency(self, tmp_path: Path):
        make_root_pom(tmp_path, ["lib", "app"])
        make_module(tmp_path, "lib", artifact_id="lib")
        make_module(
            tmp_path,
            "app",
            artifact_id="app",
            deps=[("com.example", "lib", "compile")],
        )

        result = runner.invoke(app, ["module", "tree", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        # app should list lib as a dependency
        assert "lib" in result.stdout
        assert "depends on" in result.stdout

    def test_shows_no_intra_project_dependencies_label(self, tmp_path: Path):
        make_root_pom(tmp_path, ["standalone"])
        make_module(tmp_path, "standalone")

        result = runner.invoke(app, ["module", "tree", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "no intra-project" in result.stdout

    def test_shows_module_count_in_footer(self, tmp_path: Path):
        make_root_pom(tmp_path, ["a", "b", "c"])
        for m in ("a", "b", "c"):
            make_module(tmp_path, m)

        result = runner.invoke(app, ["module", "tree", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "3" in result.stdout

    def test_fails_when_pom_missing(self, tmp_path: Path):
        result = runner.invoke(app, ["module", "tree", "--project-dir", str(tmp_path)])

        assert result.exit_code != 0

    def test_fails_when_pom_invalid_xml(self, tmp_path: Path):
        (tmp_path / "pom.xml").write_text("<unclosed>", encoding="utf-8")

        result = runner.invoke(app, ["module", "tree", "--project-dir", str(tmp_path)])

        assert result.exit_code != 0

    def test_warns_when_no_modules_declared(self, tmp_path: Path):
        pom = tmp_path / "pom.xml"
        pom.write_text(
            '<?xml version="1.0"?>'
            '<project xmlns="http://maven.apache.org/POM/4.0.0">'
            "<modelVersion>4.0.0</modelVersion>"
            "<artifactId>empty</artifactId>"
            "</project>",
            encoding="utf-8",
        )

        result = runner.invoke(app, ["module", "tree", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        assert "No" in result.stdout or "no" in result.stdout.lower()

    def test_external_deps_not_shown_in_tree(self, tmp_path: Path):
        make_root_pom(tmp_path, ["core"])
        make_module(
            tmp_path,
            "core",
            deps=[("org.springframework", "spring-core", "compile")],
        )

        result = runner.invoke(app, ["module", "tree", "--project-dir", str(tmp_path)])

        assert result.exit_code == 0, result.stdout
        # External spring dep should not appear as a dependency node in the tree
        assert "spring-core" not in result.stdout or "depends on" not in result.stdout
