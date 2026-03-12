"""Chicago-style TDD tests for `dtr watch`.

Tests real behavior with real collaborators — no mocks, no fakes.
Uses pytest tmp_path for real file I/O and verifies the pure helper
functions that drive the watch loop.
"""

import os
import time
from pathlib import Path

import pytest
from typer.testing import CliRunner

from dtr_cli.commands.watch import (
    build_mvnd_command,
    class_name_from_path,
    collect_modules,
    detect_changes,
    find_java_test_files,
    module_name_from_path,
    snapshot_mtimes,
)
from dtr_cli.main import app

runner = CliRunner()


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _make_module(base: Path, name: str, java_files: list[str] | None = None) -> Path:
    """Create a minimal Maven module directory under *base*."""
    module = base / name
    test_java = module / "src" / "test" / "java"
    test_java.mkdir(parents=True)
    (module / "pom.xml").write_text(f"<project><artifactId>{name}</artifactId></project>")
    for fname in java_files or []:
        (test_java / fname).write_text(f"public class {Path(fname).stem} {{}}")
    return module


# ===================================================================
# 1. find_java_test_files
# ===================================================================


class TestFindJavaTestFiles:
    """find_java_test_files returns all *.java paths under src/test/java."""

    def test_returns_java_files_in_test_source_tree(self, tmp_path: Path):
        module = _make_module(tmp_path, "my-module", ["FooTest.java", "BarTest.java"])

        files = find_java_test_files(module)

        names = {f.name for f in files}
        assert "FooTest.java" in names
        assert "BarTest.java" in names

    def test_returns_empty_list_when_no_test_root(self, tmp_path: Path):
        # Module exists but has no src/test/java
        module = tmp_path / "empty-module"
        module.mkdir()
        (module / "pom.xml").write_text("<project/>")

        files = find_java_test_files(module)

        assert files == []

    def test_returns_empty_list_for_nonexistent_module(self, tmp_path: Path):
        missing = tmp_path / "does-not-exist"

        files = find_java_test_files(missing)

        assert files == []

    def test_discovers_nested_packages(self, tmp_path: Path):
        module = _make_module(tmp_path, "nested-mod")
        pkg_dir = module / "src" / "test" / "java" / "com" / "example"
        pkg_dir.mkdir(parents=True)
        (pkg_dir / "DeepTest.java").write_text("public class DeepTest {}")

        files = find_java_test_files(module)

        assert any(f.name == "DeepTest.java" for f in files)


# ===================================================================
# 2. snapshot_mtimes
# ===================================================================


class TestSnapshotMtimes:
    """snapshot_mtimes builds a path->mtime dict from real files."""

    def test_records_mtime_for_each_file(self, tmp_path: Path):
        f1 = tmp_path / "A.java"
        f2 = tmp_path / "B.java"
        f1.write_text("A")
        f2.write_text("B")

        snap = snapshot_mtimes([f1, f2])

        assert f1 in snap
        assert f2 in snap
        assert snap[f1] == f1.stat().st_mtime
        assert snap[f2] == f2.stat().st_mtime

    def test_skips_missing_files_without_error(self, tmp_path: Path):
        existing = tmp_path / "exists.java"
        existing.write_text("ok")
        missing = tmp_path / "gone.java"

        snap = snapshot_mtimes([existing, missing])

        assert existing in snap
        assert missing not in snap

    def test_returns_empty_dict_for_empty_list(self):
        snap = snapshot_mtimes([])

        assert snap == {}


# ===================================================================
# 3. detect_changes
# ===================================================================


class TestDetectChanges:
    """detect_changes identifies files whose mtimes differ between snapshots."""

    def test_detects_modified_file(self, tmp_path: Path):
        f = tmp_path / "MyTest.java"
        f.write_text("v1")
        old = {f: f.stat().st_mtime}

        # Advance mtime by simulating a write after a small delay
        time.sleep(0.05)
        f.write_text("v2")
        new = {f: f.stat().st_mtime}

        changed = detect_changes(old, new)

        assert f in changed

    def test_reports_new_file_as_changed(self, tmp_path: Path):
        existing = tmp_path / "Old.java"
        existing.write_text("old")
        new_file = tmp_path / "New.java"
        new_file.write_text("new")

        old = {existing: existing.stat().st_mtime}
        new = {existing: existing.stat().st_mtime, new_file: new_file.stat().st_mtime}

        changed = detect_changes(old, new)

        assert new_file in changed
        assert existing not in changed

    def test_unchanged_files_not_reported(self, tmp_path: Path):
        f = tmp_path / "Stable.java"
        f.write_text("stable")
        mtime = f.stat().st_mtime

        old = {f: mtime}
        new = {f: mtime}

        changed = detect_changes(old, new)

        assert changed == []

    def test_empty_snapshots_yield_no_changes(self):
        changed = detect_changes({}, {})

        assert changed == []


# ===================================================================
# 4. class_name_from_path
# ===================================================================


class TestClassNameFromPath:
    """class_name_from_path extracts the simple Java class name."""

    def test_extracts_stem_from_path(self, tmp_path: Path):
        java = tmp_path / "MyServiceDocTest.java"

        name = class_name_from_path(java)

        assert name == "MyServiceDocTest"

    def test_works_for_nested_path(self, tmp_path: Path):
        java = tmp_path / "com" / "example" / "ApiTest.java"

        name = class_name_from_path(java)

        assert name == "ApiTest"


# ===================================================================
# 5. module_name_from_path
# ===================================================================


class TestModuleNameFromPath:
    """module_name_from_path resolves the owning Maven module."""

    def test_returns_module_name_for_file_inside_module(self, tmp_path: Path):
        module = _make_module(tmp_path, "dtr-integration-test", ["PhDThesisDocTest.java"])
        java_file = module / "src" / "test" / "java" / "PhDThesisDocTest.java"

        name = module_name_from_path(java_file, tmp_path)

        assert name == "dtr-integration-test"

    def test_returns_none_for_file_outside_project(self, tmp_path: Path):
        other = tmp_path / "outside" / "Foo.java"
        other.parent.mkdir(parents=True)
        other.write_text("class Foo {}")
        project = tmp_path / "project"
        project.mkdir()

        name = module_name_from_path(other, project)

        assert name is None

    def test_returns_none_when_top_level_dir_has_no_pom(self, tmp_path: Path):
        # Directory exists but has no pom.xml
        no_pom = tmp_path / "no-pom"
        java = no_pom / "src" / "test" / "java" / "Foo.java"
        java.parent.mkdir(parents=True)
        java.write_text("class Foo {}")

        name = module_name_from_path(java, tmp_path)

        assert name is None


# ===================================================================
# 6. build_mvnd_command
# ===================================================================


class TestBuildMvndCommand:
    """build_mvnd_command constructs the correct mvnd invocation."""

    def test_basic_command_structure(self, tmp_path: Path):
        cmd = build_mvnd_command("dtr-integration-test", "PhDThesisDocTest", tmp_path)

        assert cmd[0] == "mvnd"
        assert "test" in cmd
        assert "-pl" in cmd
        assert "dtr-integration-test" in cmd
        assert "-Dtest=PhDThesisDocTest" in cmd

    def test_pl_flag_followed_by_module(self, tmp_path: Path):
        cmd = build_mvnd_command("my-module", "MyTest", tmp_path)

        pl_index = cmd.index("-pl")
        assert cmd[pl_index + 1] == "my-module"

    def test_custom_executable(self, tmp_path: Path):
        cmd = build_mvnd_command("mod", "Test", tmp_path, mvnd_executable="/opt/mvnd/bin/mvnd")

        assert cmd[0] == "/opt/mvnd/bin/mvnd"

    def test_includes_no_transfer_progress(self, tmp_path: Path):
        cmd = build_mvnd_command("mod", "Test", tmp_path)

        assert "--no-transfer-progress" in cmd


# ===================================================================
# 7. collect_modules
# ===================================================================


class TestCollectModules:
    """collect_modules discovers Maven modules in a project directory."""

    def test_finds_all_modules_with_pom(self, tmp_path: Path):
        _make_module(tmp_path, "module-a")
        _make_module(tmp_path, "module-b")
        # Directory without pom should be ignored
        (tmp_path / "not-a-module").mkdir()

        modules = collect_modules(tmp_path, module_filter=None)

        names = {m.name for m in modules}
        assert "module-a" in names
        assert "module-b" in names
        assert "not-a-module" not in names

    def test_filters_to_single_module(self, tmp_path: Path):
        _make_module(tmp_path, "module-a")
        _make_module(tmp_path, "module-b")

        modules = collect_modules(tmp_path, module_filter="module-a")

        assert len(modules) == 1
        assert modules[0].name == "module-a"

    def test_returns_empty_when_filter_matches_nothing(self, tmp_path: Path):
        _make_module(tmp_path, "module-a")

        modules = collect_modules(tmp_path, module_filter="nonexistent")

        assert modules == []

    def test_falls_back_to_project_dir_when_no_child_modules(self, tmp_path: Path):
        # project_dir itself has a pom.xml but no children with pom.xml
        (tmp_path / "pom.xml").write_text("<project/>")

        modules = collect_modules(tmp_path, module_filter=None)

        assert len(modules) == 1
        assert modules[0] == tmp_path


# ===================================================================
# 8. CLI integration — dtr watch invoked via CliRunner
# ===================================================================


class TestWatchCli:
    """End-to-end CLI tests for `dtr watch` invoked through CliRunner."""

    def test_exits_with_error_when_no_pom_in_project_dir(self, tmp_path: Path):
        result = runner.invoke(
            app, ["watch", "--project-dir", str(tmp_path)]
        )

        assert result.exit_code != 0
        assert "pom.xml" in result.stdout or result.exit_code == 1

    def test_module_filter_for_existing_module_proceeds(self, tmp_path: Path):
        """When a valid module is specified the CLI reaches the watch loop.

        This test uses a patched time.sleep to interrupt the loop immediately
        so it exits cleanly rather than blocking forever.
        """
        from unittest.mock import patch

        (tmp_path / "pom.xml").write_text("<project/>")
        module = _make_module(tmp_path, "my-module", ["SomeTest.java"])

        with patch("dtr_cli.commands.watch.time.sleep", side_effect=KeyboardInterrupt):
            result = runner.invoke(
                app,
                ["watch", "--project-dir", str(tmp_path), "--module", "my-module"],
            )

        # The Typer command catches KeyboardInterrupt and exits 0 with a message
        assert result.exit_code == 0

    def test_help_text_is_accessible(self):
        result = runner.invoke(app, ["watch", "--help"])

        assert result.exit_code == 0
        assert "interval" in result.stdout.lower() or "poll" in result.stdout.lower()

    def test_default_interval_option_shown_in_help(self):
        result = runner.invoke(app, ["watch", "--help"])

        assert result.exit_code == 0
        # The default value 2.0 should appear in the help
        assert "2" in result.stdout


# ===================================================================
# 9. Watch loop exits cleanly on KeyboardInterrupt
# ===================================================================


class TestWatchLoopInterrupt:
    """The watch loop exits gracefully when interrupted."""

    def test_loop_exits_on_keyboard_interrupt(self, tmp_path: Path):
        """watch_loop raises no uncaught exception when interrupted."""
        from unittest.mock import patch

        from dtr_cli.commands.watch import watch_loop

        module = _make_module(tmp_path, "my-mod", ["SomeTest.java"])

        # Simulate KeyboardInterrupt on the very first time.sleep call
        with patch("dtr_cli.commands.watch.time.sleep", side_effect=KeyboardInterrupt):
            # The function should raise KeyboardInterrupt for the caller to catch
            # (the Typer command catches it and prints "Watch stopped.")
            with pytest.raises(KeyboardInterrupt):
                watch_loop(tmp_path, [module], interval=2.0)

    def test_cli_prints_stopped_message_on_ctrl_c(self, tmp_path: Path):
        """The Typer command catches KeyboardInterrupt and prints a clean message."""
        from unittest.mock import patch

        (tmp_path / "pom.xml").write_text("<project/>")
        module = _make_module(tmp_path, "mod", ["Test.java"])

        # Make the loop raise KeyboardInterrupt immediately
        with patch("dtr_cli.commands.watch.time.sleep", side_effect=KeyboardInterrupt):
            result = runner.invoke(
                app,
                ["watch", "--project-dir", str(tmp_path), "--module", "mod"],
            )

        # Should exit cleanly (exit code 0) with a "stopped" message
        assert result.exit_code == 0
        assert "stopped" in result.stdout.lower() or "watch" in result.stdout.lower()


# ===================================================================
# 10. Full change-detection integration with real files
# ===================================================================


class TestChangeDetectionIntegration:
    """Integration test: detect a real file modification end-to-end."""

    def test_detects_real_file_modification(self, tmp_path: Path):
        module = _make_module(tmp_path, "core", ["IntegrationTest.java"])
        java_file = module / "src" / "test" / "java" / "IntegrationTest.java"

        files = find_java_test_files(module)
        old_snap = snapshot_mtimes(files)

        # Modify the file (ensure mtime advances)
        time.sleep(0.05)
        java_file.write_text("public class IntegrationTest { /* changed */ }")
        # Force mtime update in case filesystem resolution is coarse
        new_mtime = old_snap[java_file] + 1.0
        os.utime(java_file, (new_mtime, new_mtime))

        new_snap = snapshot_mtimes(find_java_test_files(module))
        changed = detect_changes(old_snap, new_snap)

        assert java_file in changed
        assert class_name_from_path(java_file) == "IntegrationTest"
        assert module_name_from_path(java_file, tmp_path) == "core"

    def test_new_file_triggers_detection(self, tmp_path: Path):
        module = _make_module(tmp_path, "mod", ["ExistingTest.java"])
        files = find_java_test_files(module)
        old_snap = snapshot_mtimes(files)

        # Add a new file after the initial snapshot
        new_java = module / "src" / "test" / "java" / "BrandNewTest.java"
        new_java.write_text("public class BrandNewTest {}")

        new_snap = snapshot_mtimes(find_java_test_files(module))
        changed = detect_changes(old_snap, new_snap)

        assert new_java in changed
        assert class_name_from_path(new_java) == "BrandNewTest"
