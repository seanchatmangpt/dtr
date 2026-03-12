"""Chicago-style TDD tests for `dtr test` command group.

Tests real behavior with real collaborators — no mocks (except where
a subprocess would be launched to Maven, which is replaced by a lightweight
fake executable written to tmp_path).

Structure:
  1. collect_test_files  — discovery logic
  2. filter_test_entries — pattern matching
  3. build_test_run_command — command construction
  4. _package_from_path — package derivation
  5. _is_doc_or_test_class — filename classifier
  6. CLI: dtr test list
  7. CLI: dtr test filter
  8. CLI: dtr test run
  9. Edge cases (no tests found, invalid module, bad pattern)
"""

from pathlib import Path

import pytest
from typer.testing import CliRunner

from dtr_cli.commands.test import (
    _is_doc_or_test_class,
    _package_from_path,
    build_test_run_command,
    collect_test_files,
    filter_test_entries,
)
from dtr_cli.main import app

runner = CliRunner()


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _make_module(
    base: Path,
    name: str,
    java_files: list[str] | None = None,
    package: str = "",
) -> Path:
    """Create a minimal Maven module under *base* with optional test Java files.

    *java_files* are placed under src/test/java/<package-path>/.
    *package* is a dotted package string, e.g. "com.example".
    """
    module = base / name
    pkg_parts = package.replace(".", "/") if package else ""
    test_java = module / "src" / "test" / "java"
    if pkg_parts:
        test_java = test_java / pkg_parts
    test_java.mkdir(parents=True)
    (module / "pom.xml").write_text(
        f"<project><artifactId>{name}</artifactId></project>"
    )
    for fname in java_files or []:
        (test_java / fname).write_text(f"public class {Path(fname).stem} {{}}")
    return module


def _make_project(base: Path, modules: list[str] | None = None) -> Path:
    """Create a top-level Maven project with a root pom.xml."""
    (base / "pom.xml").write_text("<project><modules/></project>")
    for name in modules or []:
        _make_module(base, name)
    return base


# ===================================================================
# 1. _is_doc_or_test_class
# ===================================================================


class TestIsDocOrTestClass:
    """_is_doc_or_test_class correctly classifies Java filenames."""

    @pytest.mark.parametrize(
        "filename",
        [
            "PhDThesisDocTest.java",
            "ApiDocTest.java",
            "SomeTest.java",
            "MyServiceTest.java",
        ],
    )
    def test_accepts_doc_and_test_suffixes(self, filename: str):
        assert _is_doc_or_test_class(filename) is True

    @pytest.mark.parametrize(
        "filename",
        [
            "PhDThesisDocTest",   # without extension (stem)
            "SomeTest",
        ],
    )
    def test_accepts_stems_without_extension(self, filename: str):
        assert _is_doc_or_test_class(filename) is True

    @pytest.mark.parametrize(
        "filename",
        [
            "MyService.java",
            "Helper.java",
            "AbstractBase.java",
            "NotATestAtAll.java",
        ],
    )
    def test_rejects_non_test_files(self, filename: str):
        assert _is_doc_or_test_class(filename) is False


# ===================================================================
# 2. _package_from_path
# ===================================================================


class TestPackageFromPath:
    """_package_from_path derives the correct Java package."""

    def test_returns_dotted_package_for_nested_file(self, tmp_path: Path):
        module = _make_module(tmp_path, "core", ["FooTest.java"], package="com.example")
        java_file = module / "src" / "test" / "java" / "com" / "example" / "FooTest.java"

        pkg = _package_from_path(java_file, module)

        assert pkg == "com.example"

    def test_returns_empty_string_for_default_package(self, tmp_path: Path):
        module = _make_module(tmp_path, "core", ["FooTest.java"])
        java_file = module / "src" / "test" / "java" / "FooTest.java"

        pkg = _package_from_path(java_file, module)

        assert pkg == ""

    def test_returns_empty_for_file_outside_module(self, tmp_path: Path):
        module = _make_module(tmp_path, "core")
        outside = tmp_path / "other" / "FooTest.java"
        outside.parent.mkdir(parents=True)
        outside.write_text("class FooTest {}")

        pkg = _package_from_path(outside, module)

        assert pkg == ""


# ===================================================================
# 3. collect_test_files
# ===================================================================


class TestCollectTestFiles:
    """collect_test_files discovers *DocTest.java and *Test.java files."""

    def test_discovers_doc_test_and_test_files(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(
            tmp_path,
            "dtr-integration-test",
            ["PhDThesisDocTest.java", "ApiTest.java"],
        )

        entries = collect_test_files(tmp_path)

        class_names = {e["class_name"] for e in entries}
        assert "PhDThesisDocTest" in class_names
        assert "ApiTest" in class_names

    def test_ignores_non_test_java_files(self, tmp_path: Path):
        _make_project(tmp_path)
        module = _make_module(tmp_path, "core", ["Helper.java", "MyServiceTest.java"])

        entries = collect_test_files(tmp_path)

        class_names = {e["class_name"] for e in entries}
        assert "Helper" not in class_names
        assert "MyServiceTest" in class_names

    def test_returns_correct_module_name(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "my-module", ["SomeDocTest.java"])

        entries = collect_test_files(tmp_path)

        assert all(e["module"] == "my-module" for e in entries)

    def test_returns_correct_class_name(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["PhDThesisDocTest.java"])

        entries = collect_test_files(tmp_path)

        assert entries[0]["class_name"] == "PhDThesisDocTest"

    def test_returns_absolute_path(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["FooTest.java"])

        entries = collect_test_files(tmp_path)

        assert entries[0]["path"].is_absolute()
        assert entries[0]["path"].name == "FooTest.java"

    def test_filters_to_specific_module(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "module-a", ["ATest.java"])
        _make_module(tmp_path, "module-b", ["BTest.java"])

        entries = collect_test_files(tmp_path, module_filter="module-a")

        class_names = {e["class_name"] for e in entries}
        assert "ATest" in class_names
        assert "BTest" not in class_names

    def test_returns_empty_when_module_filter_matches_nothing(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "module-a", ["ATest.java"])

        entries = collect_test_files(tmp_path, module_filter="nonexistent-module")

        assert entries == []

    def test_returns_empty_when_no_test_files(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["Helper.java", "Main.java"])

        entries = collect_test_files(tmp_path)

        assert entries == []

    def test_discovers_nested_package_tests(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(
            tmp_path, "core", ["DeepDocTest.java"], package="com.example.deep"
        )

        entries = collect_test_files(tmp_path)

        assert len(entries) == 1
        assert entries[0]["class_name"] == "DeepDocTest"
        assert entries[0]["package"] == "com.example.deep"

    def test_collects_from_multiple_modules(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "module-a", ["ADocTest.java"])
        _make_module(tmp_path, "module-b", ["BDocTest.java"])

        entries = collect_test_files(tmp_path)

        class_names = {e["class_name"] for e in entries}
        assert "ADocTest" in class_names
        assert "BDocTest" in class_names

    def test_handles_module_without_src_test_java(self, tmp_path: Path):
        _make_project(tmp_path)
        # Module with pom.xml but no src/test/java
        empty_module = tmp_path / "empty"
        empty_module.mkdir()
        (empty_module / "pom.xml").write_text("<project/>")

        entries = collect_test_files(tmp_path)

        assert entries == []

    def test_single_module_project(self, tmp_path: Path):
        """When no child has pom.xml, project_dir itself is treated as the module."""
        (tmp_path / "pom.xml").write_text("<project/>")
        test_java = tmp_path / "src" / "test" / "java"
        test_java.mkdir(parents=True)
        (test_java / "AppTest.java").write_text("class AppTest {}")

        entries = collect_test_files(tmp_path)

        assert len(entries) == 1
        assert entries[0]["class_name"] == "AppTest"


# ===================================================================
# 4. filter_test_entries
# ===================================================================


class TestFilterTestEntries:
    """filter_test_entries matches entries by glob or regex."""

    def _make_entries(self, class_names: list[str]) -> list[dict]:
        return [
            {"module": "m", "package": "", "class_name": n, "path": Path(f"{n}.java")}
            for n in class_names
        ]

    def test_glob_wildcard_matches_substring(self):
        entries = self._make_entries(["ApiDocTest", "PhDThesisDocTest", "SomeTest"])

        matched = filter_test_entries(entries, "*DocTest")

        names = {e["class_name"] for e in matched}
        assert "ApiDocTest" in names
        assert "PhDThesisDocTest" in names
        assert "SomeTest" not in names

    def test_exact_glob_match(self):
        entries = self._make_entries(["ApiDocTest", "PhDThesisDocTest"])

        matched = filter_test_entries(entries, "ApiDocTest")

        assert len(matched) == 1
        assert matched[0]["class_name"] == "ApiDocTest"

    def test_glob_question_mark_matches_single_char(self):
        entries = self._make_entries(["ATest", "BTest", "ABTest"])

        matched = filter_test_entries(entries, "?Test")

        names = {e["class_name"] for e in matched}
        assert "ATest" in names
        assert "BTest" in names
        assert "ABTest" not in names

    def test_regex_pattern_matches(self):
        entries = self._make_entries(["ApiDocTest", "PhDThesisDocTest", "SomeTest"])

        matched = filter_test_entries(entries, ".*Doc.*")

        names = {e["class_name"] for e in matched}
        assert "ApiDocTest" in names
        assert "PhDThesisDocTest" in names
        assert "SomeTest" not in names

    def test_regex_anchored_pattern(self):
        entries = self._make_entries(["ApiDocTest", "MyApiTest"])

        matched = filter_test_entries(entries, "^Api")

        names = {e["class_name"] for e in matched}
        assert "ApiDocTest" in names
        assert "MyApiTest" not in names

    def test_returns_empty_when_no_match(self):
        entries = self._make_entries(["ApiDocTest", "SomeTest"])

        matched = filter_test_entries(entries, "*Thesis*")

        assert matched == []

    def test_returns_all_on_wildcard_glob(self):
        entries = self._make_entries(["ApiDocTest", "SomeTest", "OtherDocTest"])

        matched = filter_test_entries(entries, "*")

        assert len(matched) == len(entries)

    def test_empty_entries_returns_empty(self):
        matched = filter_test_entries([], "*Test")

        assert matched == []


# ===================================================================
# 5. build_test_run_command
# ===================================================================


class TestBuildTestRunCommand:
    """build_test_run_command constructs the correct mvnd invocation."""

    def test_basic_structure(self):
        cmd = build_test_run_command("dtr-integration-test", "PhDThesisDocTest")

        assert cmd[0] == "mvnd"
        assert "test" in cmd
        assert "-pl" in cmd
        assert "dtr-integration-test" in cmd
        assert "-Dtest=PhDThesisDocTest" in cmd

    def test_pl_flag_followed_by_module(self):
        cmd = build_test_run_command("my-module", "MyTest")

        pl_index = cmd.index("-pl")
        assert cmd[pl_index + 1] == "my-module"

    def test_dtest_property_correct_format(self):
        cmd = build_test_run_command("mod", "SomeDocTest")

        assert "-Dtest=SomeDocTest" in cmd

    def test_custom_mvnd_executable(self):
        cmd = build_test_run_command("mod", "Test", mvnd_executable="/opt/mvnd/bin/mvnd")

        assert cmd[0] == "/opt/mvnd/bin/mvnd"

    def test_includes_no_transfer_progress(self):
        cmd = build_test_run_command("mod", "Test")

        assert "--no-transfer-progress" in cmd


# ===================================================================
# 6. CLI: dtr test list
# ===================================================================


class TestCliTestList:
    """End-to-end CLI tests for `dtr test list`."""

    def test_lists_doc_test_files_in_table(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "dtr-core", ["PhDThesisDocTest.java"])

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0
        assert "PhDThesisDocTest" in result.stdout

    def test_shows_module_name_in_output(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "dtr-integration-test", ["SomeDocTest.java"])

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0
        assert "dtr-integration-test" in result.stdout

    def test_shows_count_summary(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "mod", ["ADocTest.java", "BTest.java"])

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0
        # Some indication of count
        assert "2" in result.stdout

    def test_limits_to_module_with_module_flag(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "module-a", ["ADocTest.java"])
        _make_module(tmp_path, "module-b", ["BDocTest.java"])

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path), "--module", "module-a"]
        )

        assert result.exit_code == 0
        assert "ADocTest" in result.stdout
        assert "BDocTest" not in result.stdout

    def test_exits_zero_with_no_tests_found(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "empty-mod", ["Helper.java"])

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0
        assert "No test files found" in result.stdout

    def test_exits_with_error_when_no_pom(self, tmp_path: Path):
        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code != 0
        assert "pom.xml" in result.stdout

    def test_help_is_accessible(self):
        result = runner.invoke(app, ["test", "list", "--help"])

        assert result.exit_code == 0
        assert "module" in result.stdout.lower()

    def test_displays_package_in_output(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["DeepDocTest.java"], package="com.example")

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0
        assert "com.example" in result.stdout


# ===================================================================
# 7. CLI: dtr test filter
# ===================================================================


class TestCliTestFilter:
    """End-to-end CLI tests for `dtr test filter`."""

    def test_filters_tests_by_glob_pattern(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["ApiDocTest.java", "UnrelatedTest.java"])

        result = runner.invoke(
            app,
            [
                "test", "filter",
                "--project-dir", str(tmp_path),
                "--pattern", "*Api*",
            ],
        )

        assert result.exit_code == 0
        assert "ApiDocTest" in result.stdout
        assert "UnrelatedTest" not in result.stdout

    def test_filters_by_regex_pattern(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["PhDThesisDocTest.java", "SomeTest.java"])

        result = runner.invoke(
            app,
            [
                "test", "filter",
                "--project-dir", str(tmp_path),
                "--pattern", ".*Doc.*",
            ],
        )

        assert result.exit_code == 0
        assert "PhDThesisDocTest" in result.stdout
        assert "SomeTest" not in result.stdout

    def test_exits_zero_when_no_pattern_match(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["SomeTest.java"])

        result = runner.invoke(
            app,
            [
                "test", "filter",
                "--project-dir", str(tmp_path),
                "--pattern", "*Nonexistent*",
            ],
        )

        assert result.exit_code == 0
        assert "No tests matching" in result.stdout

    def test_scope_filter_to_module(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "module-a", ["ApiDocTest.java"])
        _make_module(tmp_path, "module-b", ["ApiDocTest.java"])

        result = runner.invoke(
            app,
            [
                "test", "filter",
                "--project-dir", str(tmp_path),
                "--pattern", "*Api*",
                "--module", "module-a",
            ],
        )

        assert result.exit_code == 0
        assert "module-a" in result.stdout
        assert "module-b" not in result.stdout

    def test_exits_with_error_when_no_pom(self, tmp_path: Path):
        result = runner.invoke(
            app,
            [
                "test", "filter",
                "--project-dir", str(tmp_path),
                "--pattern", "*Test*",
            ],
        )

        assert result.exit_code != 0

    def test_help_is_accessible(self):
        result = runner.invoke(app, ["test", "filter", "--help"])

        assert result.exit_code == 0
        assert "pattern" in result.stdout.lower()

    def test_shows_match_count(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["ApiDocTest.java", "ApiV2DocTest.java"])

        result = runner.invoke(
            app,
            [
                "test", "filter",
                "--project-dir", str(tmp_path),
                "--pattern", "*Api*",
            ],
        )

        assert result.exit_code == 0
        assert "2" in result.stdout


# ===================================================================
# 8. CLI: dtr test run
# ===================================================================


class TestCliTestRun:
    """End-to-end CLI tests for `dtr test run`.

    Maven is NOT actually invoked — we write a tiny shell script that
    records the command-line arguments to a file and exits 0, then we
    verify the recorded invocation.
    """

    def _write_fake_mvnd(self, bin_dir: Path, exit_code: int = 0) -> Path:
        """Write a fake mvnd script that saves its args and exits with *exit_code*."""
        script = bin_dir / "mvnd"
        script.write_text(
            f"#!/bin/sh\necho \"$@\" > \"{bin_dir}/last_args.txt\"\nexit {exit_code}\n"
        )
        script.chmod(0o755)
        return script

    def test_invokes_correct_mvnd_command(self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch):
        _make_project(tmp_path)
        bin_dir = tmp_path / "bin"
        bin_dir.mkdir()
        self._write_fake_mvnd(bin_dir)
        monkeypatch.setenv("PATH", str(bin_dir))

        result = runner.invoke(
            app,
            [
                "test", "run",
                "--project-dir", str(tmp_path),
                "--module", "dtr-integration-test",
                "--class", "PhDThesisDocTest",
            ],
        )

        assert result.exit_code == 0
        args_file = bin_dir / "last_args.txt"
        assert args_file.exists()
        recorded = args_file.read_text().strip()
        assert "dtr-integration-test" in recorded
        assert "PhDThesisDocTest" in recorded

    def test_builds_correct_command_structure(self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch):
        _make_project(tmp_path)
        bin_dir = tmp_path / "bin"
        bin_dir.mkdir()
        self._write_fake_mvnd(bin_dir)
        monkeypatch.setenv("PATH", str(bin_dir))

        result = runner.invoke(
            app,
            [
                "test", "run",
                "--project-dir", str(tmp_path),
                "--module", "my-module",
                "--class", "MyDocTest",
            ],
        )

        assert result.exit_code == 0
        # The printed command line should show up in stdout
        assert "my-module" in result.stdout
        assert "MyDocTest" in result.stdout

    def test_exits_with_build_exit_code_on_failure(
        self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
    ):
        _make_project(tmp_path)
        bin_dir = tmp_path / "bin"
        bin_dir.mkdir()
        self._write_fake_mvnd(bin_dir, exit_code=1)
        monkeypatch.setenv("PATH", str(bin_dir))

        result = runner.invoke(
            app,
            [
                "test", "run",
                "--project-dir", str(tmp_path),
                "--module", "core",
                "--class", "FailingTest",
            ],
        )

        assert result.exit_code == 1

    def test_exits_with_error_when_no_pom(self, tmp_path: Path):
        result = runner.invoke(
            app,
            [
                "test", "run",
                "--project-dir", str(tmp_path),
                "--module", "mod",
                "--class", "Foo",
            ],
        )

        assert result.exit_code != 0
        assert "pom.xml" in result.stdout

    def test_help_is_accessible(self):
        result = runner.invoke(app, ["test", "run", "--help"])

        assert result.exit_code == 0
        assert "module" in result.stdout.lower()
        assert "class" in result.stdout.lower()

    def test_shows_passed_message_on_success(
        self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
    ):
        _make_project(tmp_path)
        bin_dir = tmp_path / "bin"
        bin_dir.mkdir()
        self._write_fake_mvnd(bin_dir, exit_code=0)
        monkeypatch.setenv("PATH", str(bin_dir))

        result = runner.invoke(
            app,
            [
                "test", "run",
                "--project-dir", str(tmp_path),
                "--module", "core",
                "--class", "MyDocTest",
            ],
        )

        assert result.exit_code == 0
        assert "PASSED" in result.stdout or "passed" in result.stdout.lower()

    def test_shows_failed_message_on_failure(
        self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
    ):
        _make_project(tmp_path)
        bin_dir = tmp_path / "bin"
        bin_dir.mkdir()
        self._write_fake_mvnd(bin_dir, exit_code=1)
        monkeypatch.setenv("PATH", str(bin_dir))

        result = runner.invoke(
            app,
            [
                "test", "run",
                "--project-dir", str(tmp_path),
                "--module", "core",
                "--class", "BrokenDocTest",
            ],
        )

        assert result.exit_code != 0
        assert "FAIL" in result.stdout or "fail" in result.stdout.lower()


# ===================================================================
# 9. CLI: dtr test — top-level help
# ===================================================================


class TestCliTestGroup:
    """Top-level `dtr test` command group tests."""

    def test_top_level_help_lists_subcommands(self):
        result = runner.invoke(app, ["test", "--help"])

        assert result.exit_code == 0
        assert "list" in result.stdout
        assert "run" in result.stdout
        assert "filter" in result.stdout

    def test_no_args_shows_help(self):
        result = runner.invoke(app, ["test"])

        # Should print help (exit 0) or usage error (exit != 0) — either is fine
        # as long as it doesn't crash with an unhandled exception
        assert result.exception is None or isinstance(result.exception, SystemExit)


# ===================================================================
# 10. Edge cases and integration
# ===================================================================


class TestEdgeCases:
    """Edge cases: empty project, missing module, unusual patterns."""

    def test_list_no_modules_in_project(self, tmp_path: Path):
        """A project with only a root pom.xml and no modules."""
        (tmp_path / "pom.xml").write_text("<project/>")

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        # Should exit 0 with a "not found" message
        assert result.exit_code == 0

    def test_filter_invalid_module(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "real-module", ["ATest.java"])

        result = runner.invoke(
            app,
            [
                "test", "filter",
                "--project-dir", str(tmp_path),
                "--pattern", "*",
                "--module", "nonexistent-module",
            ],
        )

        assert result.exit_code == 0
        assert "No tests matching" in result.stdout

    def test_collect_test_files_no_modules_returns_empty(self, tmp_path: Path):
        """project_dir with pom.xml but no children with pom.xml and no test src."""
        (tmp_path / "pom.xml").write_text("<project/>")

        entries = collect_test_files(tmp_path)

        assert entries == []

    def test_filter_wildcard_matches_everything(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "mod", ["FooDocTest.java", "BarTest.java"])

        entries = collect_test_files(tmp_path)
        matched = filter_test_entries(entries, "*")

        assert len(matched) == 2

    def test_run_missing_module_flag_triggers_error(self):
        """--module is required; omitting it should produce a usage error."""
        result = runner.invoke(
            app,
            [
                "test", "run",
                "--class", "SomeTest",
            ],
        )

        # Missing required option must fail
        assert result.exit_code != 0

    def test_run_missing_class_flag_triggers_error(self):
        """--class is required; omitting it should produce a usage error."""
        result = runner.invoke(
            app,
            [
                "test", "run",
                "--module", "some-module",
            ],
        )

        assert result.exit_code != 0
