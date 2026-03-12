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


# ===================================================================
# 11. Chicago TDD: type column, pre-run validation, timing
# ===================================================================


class TestTypeDetection:
    """_detect_test_type classifies test classes correctly."""

    def test_doc_test_suffix_returns_doctest(self):
        from dtr_cli.commands.test import _detect_test_type
        assert _detect_test_type("PhDThesisDocTest", "dtr-integration-test") == "DocTest"

    def test_regular_test_suffix_returns_unit(self):
        from dtr_cli.commands.test import _detect_test_type
        assert _detect_test_type("UtilityTest", "dtr-core") == "Unit"

    def test_bench_module_returns_bench(self):
        from dtr_cli.commands.test import _detect_test_type
        assert _detect_test_type("ThroughputTest", "dtr-benchmarks") == "Bench"

    def test_doc_test_takes_priority_over_bench_module(self):
        from dtr_cli.commands.test import _detect_test_type
        # DocTest suffix beats bench module name
        assert _detect_test_type("ApiDocTest", "benchmarks") == "DocTest"


class TestListEmptyProjectReturnsGracefully:
    """dtr test list with a pom.xml but no Java files returns empty table gracefully."""

    def test_list_with_no_java_files_exits_zero(self, tmp_path: Path):
        # Real temp dir with only a pom.xml — no src/test/java at all.
        (tmp_path / "pom.xml").write_text("<project><modules/></project>")

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0, (
            "Expected exit 0 for empty project, got "
            f"{result.exit_code}. stdout={result.stdout!r}"
        )

    def test_list_with_no_java_files_reports_not_found(self, tmp_path: Path):
        (tmp_path / "pom.xml").write_text("<project><modules/></project>")

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert "No test files found" in result.stdout, (
            f"Expected 'No test files found' message. stdout={result.stdout!r}"
        )

    def test_list_with_non_test_java_only_exits_zero(self, tmp_path: Path):
        """Helper.java is not a test — list should report empty gracefully."""
        module = tmp_path / "core"
        module.mkdir()
        (module / "pom.xml").write_text("<project/>")
        (tmp_path / "pom.xml").write_text("<project><modules/></project>")
        src = module / "src" / "test" / "java"
        src.mkdir(parents=True)
        (src / "Helper.java").write_text("public class Helper {}")

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0
        assert "No test files found" in result.stdout


class TestListShowsTypeColumn:
    """dtr test list shows the Type column with correct values."""

    def test_doc_test_java_shows_doctest_type(self, tmp_path: Path):
        # Real filesystem: create a DocTest.java under src/test/java
        module = tmp_path / "dtr-integration-test"
        module.mkdir()
        (module / "pom.xml").write_text("<project/>")
        (tmp_path / "pom.xml").write_text("<project><modules/></project>")
        src = module / "src" / "test" / "java"
        src.mkdir(parents=True)
        (src / "PhDThesisDocTest.java").write_text(
            "public class PhDThesisDocTest {}"
        )

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0, f"stdout={result.stdout!r}"
        assert "PhDThesisDocTest" in result.stdout
        # The Type column must show "DocTest"
        assert "DocTest" in result.stdout, (
            "Expected 'DocTest' type in table output. "
            f"stdout={result.stdout!r}"
        )

    def test_regular_test_java_shows_unit_type(self, tmp_path: Path):
        module = tmp_path / "dtr-core"
        module.mkdir()
        (module / "pom.xml").write_text("<project/>")
        (tmp_path / "pom.xml").write_text("<project><modules/></project>")
        src = module / "src" / "test" / "java"
        src.mkdir(parents=True)
        (src / "UtilityTest.java").write_text("public class UtilityTest {}")

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0
        assert "Unit" in result.stdout, (
            "Expected 'Unit' type in table output. "
            f"stdout={result.stdout!r}"
        )

    def test_bench_module_shows_bench_type(self, tmp_path: Path):
        module = tmp_path / "dtr-benchmarks"
        module.mkdir()
        (module / "pom.xml").write_text("<project/>")
        (tmp_path / "pom.xml").write_text("<project><modules/></project>")
        src = module / "src" / "test" / "java"
        src.mkdir(parents=True)
        (src / "ThroughputTest.java").write_text("public class ThroughputTest {}")

        result = runner.invoke(
            app, ["test", "list", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code == 0
        assert "Bench" in result.stdout, (
            "Expected 'Bench' type in table output. "
            f"stdout={result.stdout!r}"
        )


class TestRunNonExistentClassValidation:
    """dtr test run exits 1 with a helpful error when the class is not found."""

    def test_nonexistent_class_exits_nonzero(self, tmp_path: Path):
        # Real Maven project with no test sources.
        _make_project(tmp_path)
        _make_module(tmp_path, "dtr-core", [])  # no Java files

        result = runner.invoke(
            app,
            [
                "test", "run",
                "--project-dir", str(tmp_path),
                "--module", "dtr-core",
                "--class", "NonExistentTest",
            ],
        )

        assert result.exit_code != 0, (
            "Expected non-zero exit when class not found. "
            f"stdout={result.stdout!r}"
        )

    def test_nonexistent_class_error_mentions_class_name(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "dtr-core", [])

        result = runner.invoke(
            app,
            [
                "test", "run",
                "--project-dir", str(tmp_path),
                "--module", "dtr-core",
                "--class", "NonExistentTest",
            ],
        )

        combined = result.stdout + (result.stderr or "")
        assert "NonExistentTest" in combined, (
            "Expected class name in error output. "
            f"output={combined!r}"
        )

    def test_nonexistent_class_hints_available_tests(self, tmp_path: Path):
        """When other test classes exist, the error lists them as hints."""
        _make_project(tmp_path)
        _make_module(tmp_path, "dtr-core", ["RealDocTest.java"])

        result = runner.invoke(
            app,
            [
                "test", "run",
                "--project-dir", str(tmp_path),
                "--module", "dtr-core",
                "--class", "GhostTest",
            ],
        )

        assert result.exit_code != 0
        combined = result.stdout + (result.stderr or "")
        # The hint should include the existing class
        assert "RealDocTest" in combined, (
            "Expected available test class 'RealDocTest' in hint output. "
            f"output={combined!r}"
        )

    def test_valid_class_bypasses_validation(
        self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
    ):
        """When test class file exists, validation passes and Maven is invoked."""
        _make_project(tmp_path)
        _make_module(tmp_path, "dtr-core", ["RealDocTest.java"])

        # Provide a fake mvnd that succeeds
        bin_dir = tmp_path / "bin"
        bin_dir.mkdir()
        script = bin_dir / "mvnd"
        script.write_text(
            f'#!/bin/sh\necho "$@" > "{bin_dir}/args.txt"\nexit 0\n'
        )
        script.chmod(0o755)
        monkeypatch.setenv("PATH", str(bin_dir))

        result = runner.invoke(
            app,
            [
                "test", "run",
                "--project-dir", str(tmp_path),
                "--module", "dtr-core",
                "--class", "RealDocTest",
            ],
        )

        # Should pass validation and invoke Maven (exit 0 from fake mvnd)
        assert result.exit_code == 0, f"stdout={result.stdout!r}"


class TestFindTestFile:
    """_find_test_file locates Java source files under src/test/java."""

    def test_finds_existing_test_file(self, tmp_path: Path):
        from dtr_cli.commands.test import _find_test_file

        module = tmp_path / "core"
        src = module / "src" / "test" / "java"
        src.mkdir(parents=True)
        (src / "FooTest.java").write_text("public class FooTest {}")

        found = _find_test_file("FooTest", tmp_path)

        assert found is not None
        assert found.name == "FooTest.java"

    def test_returns_none_for_missing_class(self, tmp_path: Path):
        from dtr_cli.commands.test import _find_test_file

        (tmp_path / "pom.xml").write_text("<project/>")

        found = _find_test_file("DoesNotExist", tmp_path)

        assert found is None

    def test_ignores_files_outside_src_test_java(self, tmp_path: Path):
        from dtr_cli.commands.test import _find_test_file

        # Place Java file in src/main/java — should NOT be found
        main_src = tmp_path / "src" / "main" / "java"
        main_src.mkdir(parents=True)
        (main_src / "FooTest.java").write_text("public class FooTest {}")

        found = _find_test_file("FooTest", tmp_path)

        assert found is None


class TestCollectTestFilesIncludesType:
    """collect_test_files now includes a 'type' key in each entry."""

    def test_entries_have_type_key(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["ApiDocTest.java"])

        entries = collect_test_files(tmp_path)

        assert entries, "Expected at least one entry"
        for entry in entries:
            assert "type" in entry, f"Missing 'type' key in entry: {entry}"

    def test_doc_test_entry_has_doctest_type(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["ApiDocTest.java"])

        entries = collect_test_files(tmp_path)

        doc_tests = [e for e in entries if e["class_name"] == "ApiDocTest"]
        assert doc_tests
        assert doc_tests[0]["type"] == "DocTest"

    def test_unit_test_entry_has_unit_type(self, tmp_path: Path):
        _make_project(tmp_path)
        _make_module(tmp_path, "core", ["UtilTest.java"])

        entries = collect_test_files(tmp_path)

        unit_tests = [e for e in entries if e["class_name"] == "UtilTest"]
        assert unit_tests
        assert unit_tests[0]["type"] == "Unit"
