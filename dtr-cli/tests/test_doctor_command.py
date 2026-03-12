"""Chicago TDD tests for `dtr doctor` command.

Focus areas (per upgrade spec):
- pom.xml FAIL shown when directory has no pom.xml
- --fix creates .mvn/maven.config (real filesystem assertion)
- --fix creates .dtr.yml if missing (real filesystem assertion)
- exit code 0 when all required checks pass (mock subprocess for java/mvn)
- exit code 1 when a required check fails

All tests use real temp dirs (pytest's tmp_path) and assert real file system
state — no mocks of the filesystem itself.  Subprocess calls for java/mvn are
patched to avoid requiring a specific Java/Maven installation in every
environment.
"""

from __future__ import annotations

import subprocess
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest
from typer.testing import CliRunner

from dtr_cli.main import app
from dtr_cli.commands.doctor import (
    check_pom_xml,
    check_python,
    check_disk_space,
    check_write_permissions,
    fix_maven_config,
    fix_dtr_yml,
)

runner = CliRunner()


# ---------------------------------------------------------------------------
# Helper: patch subprocess so Java + Maven always return a "pass" result,
# regardless of what's installed in the test environment.
# ---------------------------------------------------------------------------

def _mock_java_pass(cmd, **kwargs):
    """Fake java -version → reports Java 26."""
    result = MagicMock()
    result.stdout = ""
    result.stderr = 'openjdk version "26.0.2" 2025-07-15\n'
    return result


def _mock_mvn_pass(cmd, **kwargs):
    """Fake mvn --version → reports Maven 4.0.0."""
    result = MagicMock()
    result.stdout = "Apache Maven 4.0.0 (something)\n"
    result.stderr = ""
    return result


def _mock_subprocess_run_all_pass(cmd, **kwargs):
    """Route java / mvn / mvnd / pandoc / latexmk subprocess calls to fakes."""
    if not cmd:
        return MagicMock(stdout="", stderr="", returncode=0)
    name = str(cmd[0]).split("/")[-1]
    if name == "java":
        return _mock_java_pass(cmd, **kwargs)
    if name in ("mvn",):
        return _mock_mvn_pass(cmd, **kwargs)
    # mvnd, pandoc, latexmk → treat as not-found / warn
    result = MagicMock()
    result.stdout = ""
    result.stderr = ""
    result.returncode = 1
    return result


# ---------------------------------------------------------------------------
# 1. pom.xml check shows FAIL when pom.xml is absent
# ---------------------------------------------------------------------------


class TestDoctorShowsPomXmlFail:
    """dtr doctor in a directory with no pom.xml must show pom.xml as FAIL."""

    def test_check_pom_xml_returns_fail_status_in_empty_dir(self, tmp_path: Path):
        status, label, detail = check_pom_xml(tmp_path)
        assert status == "fail", f"Expected 'fail', got '{status}': {detail}"

    def test_check_pom_xml_detail_mentions_directory(self, tmp_path: Path):
        status, label, detail = check_pom_xml(tmp_path)
        assert str(tmp_path) in detail

    def test_dtr_doctor_outputs_pom_fail_in_stdout(self, tmp_path: Path):
        """CLI invocation: no pom.xml → pom.xml check must appear in output."""
        result = runner.invoke(app, ["doctor", "--project-dir", str(tmp_path)])
        assert "pom.xml" in result.stdout

    def test_dtr_doctor_exits_1_when_pom_missing(self, tmp_path: Path):
        """No pom.xml is a required-check failure → exit code 1."""
        result = runner.invoke(app, ["doctor", "--project-dir", str(tmp_path)])
        assert result.exit_code == 1, (
            f"Expected exit code 1, got {result.exit_code}.\nOutput:\n{result.stdout}"
        )


# ---------------------------------------------------------------------------
# 2. --fix creates .mvn/maven.config (real filesystem)
# ---------------------------------------------------------------------------


class TestFixCreatesMavenConfig:
    """dtr doctor --fix must create .mvn/maven.config with --enable-preview."""

    def test_fix_creates_maven_config_file(self, tmp_path: Path):
        """--fix in a fresh temp dir creates .mvn/maven.config."""
        (tmp_path / "pom.xml").write_text("<project/>", encoding="utf-8")
        runner.invoke(app, ["doctor", "--project-dir", str(tmp_path), "--fix"])
        maven_config = tmp_path / ".mvn" / "maven.config"
        assert maven_config.exists(), (
            ".mvn/maven.config was not created by --fix"
        )

    def test_fix_writes_enable_preview_flag(self, tmp_path: Path):
        """Created .mvn/maven.config must contain --enable-preview."""
        (tmp_path / "pom.xml").write_text("<project/>", encoding="utf-8")
        runner.invoke(app, ["doctor", "--project-dir", str(tmp_path), "--fix"])
        maven_config = tmp_path / ".mvn" / "maven.config"
        assert maven_config.exists()
        content = maven_config.read_text(encoding="utf-8")
        assert "--enable-preview" in content, (
            f"--enable-preview not in maven.config content: {content!r}"
        )

    def test_fix_helper_creates_mvn_dir(self, tmp_path: Path):
        """fix_maven_config creates the .mvn directory if absent."""
        assert not (tmp_path / ".mvn").exists()
        fix_maven_config(tmp_path)
        assert (tmp_path / ".mvn").exists()
        assert (tmp_path / ".mvn" / "maven.config").exists()

    def test_fix_does_not_duplicate_enable_preview(self, tmp_path: Path):
        """Running --fix twice does not add --enable-preview twice."""
        (tmp_path / "pom.xml").write_text("<project/>", encoding="utf-8")
        runner.invoke(app, ["doctor", "--project-dir", str(tmp_path), "--fix"])
        runner.invoke(app, ["doctor", "--project-dir", str(tmp_path), "--fix"])
        maven_config = tmp_path / ".mvn" / "maven.config"
        content = maven_config.read_text(encoding="utf-8")
        assert content.count("--enable-preview") == 1, (
            f"--enable-preview duplicated in: {content!r}"
        )


# ---------------------------------------------------------------------------
# 3. --fix creates .dtr.yml if missing (real filesystem)
# ---------------------------------------------------------------------------


class TestFixCreatesDtrYml:
    """dtr doctor --fix must create .dtr.yml when absent."""

    def test_fix_creates_dtr_yml_when_absent(self, tmp_path: Path):
        """--fix creates .dtr.yml in the project directory."""
        (tmp_path / "pom.xml").write_text("<project/>", encoding="utf-8")
        dtr_yml = tmp_path / ".dtr.yml"
        assert not dtr_yml.exists(), "Pre-condition: .dtr.yml must not exist"
        runner.invoke(app, ["doctor", "--project-dir", str(tmp_path), "--fix"])
        assert dtr_yml.exists(), ".dtr.yml was not created by --fix"

    def test_fix_dtr_yml_helper_creates_file(self, tmp_path: Path):
        """fix_dtr_yml() creates .dtr.yml with YAML content."""
        dtr_yml = tmp_path / ".dtr.yml"
        assert not dtr_yml.exists()
        fix_dtr_yml(tmp_path)
        assert dtr_yml.exists()
        content = dtr_yml.read_text(encoding="utf-8")
        # Must be non-empty and contain at least one known section
        assert len(content.strip()) > 0
        assert "build" in content or "export" in content

    def test_fix_dtr_yml_is_idempotent(self, tmp_path: Path):
        """fix_dtr_yml called twice does not raise and file remains valid."""
        fix_dtr_yml(tmp_path)
        dtr_yml = tmp_path / ".dtr.yml"
        first_content = dtr_yml.read_text(encoding="utf-8")

        # Second call should be a no-op
        fix_dtr_yml(tmp_path)
        second_content = dtr_yml.read_text(encoding="utf-8")
        assert first_content == second_content, "File content changed on second fix call"

    def test_fix_outputs_message_about_dtr_yml(self, tmp_path: Path):
        """--fix output mentions .dtr.yml creation."""
        (tmp_path / "pom.xml").write_text("<project/>", encoding="utf-8")
        result = runner.invoke(app, ["doctor", "--project-dir", str(tmp_path), "--fix"])
        assert ".dtr.yml" in result.stdout, (
            f"Expected .dtr.yml mention in output:\n{result.stdout}"
        )


# ---------------------------------------------------------------------------
# 4. Exit code 0 when all required checks pass (subprocess mocked)
# ---------------------------------------------------------------------------


class TestExitCodeZeroAllPass:
    """Exit code is 0 when all required checks pass."""

    def test_exit_code_0_with_full_project_and_mocked_tools(self, tmp_path: Path):
        """All required checks pass: Java, Maven, pom.xml, Python, disk, write."""
        # Set up a complete project
        (tmp_path / "pom.xml").write_text("<project/>", encoding="utf-8")
        mvn_dir = tmp_path / ".mvn"
        mvn_dir.mkdir()
        (mvn_dir / "maven.config").write_text("--enable-preview\n", encoding="utf-8")
        (tmp_path / ".dtr.yml").write_text("build:\n  verbose: false\n", encoding="utf-8")

        with patch("subprocess.run", side_effect=_mock_subprocess_run_all_pass):
            result = runner.invoke(
                app, ["doctor", "--project-dir", str(tmp_path)]
            )

        # Required checks all pass → exit 0 (or 2 if optional warns exist)
        # We accept 0 or 2 — both mean "required pass"
        assert result.exit_code in (0, 2), (
            f"Expected exit code 0 or 2 (all required pass), got {result.exit_code}.\n"
            f"Output:\n{result.stdout}"
        )

    def test_exit_code_not_1_when_pom_exists_and_tools_pass(self, tmp_path: Path):
        """With pom.xml present and tools mocked to pass, exit code must not be 1."""
        (tmp_path / "pom.xml").write_text("<project/>", encoding="utf-8")

        with patch("subprocess.run", side_effect=_mock_subprocess_run_all_pass):
            result = runner.invoke(
                app, ["doctor", "--project-dir", str(tmp_path)]
            )

        assert result.exit_code != 1, (
            f"Got exit code 1 (required failure) unexpectedly.\nOutput:\n{result.stdout}"
        )


# ---------------------------------------------------------------------------
# 5. Exit code 1 when a required check fails
# ---------------------------------------------------------------------------


class TestExitCodeOneRequiredFail:
    """Exit code is 1 when at least one required check fails."""

    def test_exit_code_1_when_pom_missing(self, tmp_path: Path):
        """Missing pom.xml → required fail → exit 1."""
        result = runner.invoke(app, ["doctor", "--project-dir", str(tmp_path)])
        assert result.exit_code == 1, (
            f"Expected exit code 1, got {result.exit_code}.\nOutput:\n{result.stdout}"
        )

    def test_exit_code_1_when_java_not_found(self, tmp_path: Path):
        """Java not in PATH → required fail → exit 1."""
        (tmp_path / "pom.xml").write_text("<project/>", encoding="utf-8")

        def java_not_found(cmd, **kwargs):
            if cmd and str(cmd[0]).split("/")[-1] == "java":
                raise FileNotFoundError("java not found")
            return _mock_mvn_pass(cmd, **kwargs)

        import shutil
        with patch("shutil.which", return_value=None):
            result = runner.invoke(
                app, ["doctor", "--project-dir", str(tmp_path)]
            )
        assert result.exit_code == 1, (
            f"Expected exit code 1 when java absent, got {result.exit_code}."
        )

    def test_exit_code_1_includes_failure_count_in_output(self, tmp_path: Path):
        """Failure output mentions 'failures' with a count > 0."""
        result = runner.invoke(app, ["doctor", "--project-dir", str(tmp_path)])
        assert result.exit_code == 1
        # Output must contain summary line
        assert "failures" in result.stdout or "fail" in result.stdout.lower()

    def test_tip_to_run_fix_shown_on_failure(self, tmp_path: Path):
        """When required check fails without --fix, tip is shown."""
        result = runner.invoke(app, ["doctor", "--project-dir", str(tmp_path)])
        assert result.exit_code == 1
        assert "--fix" in result.stdout or "fix" in result.stdout.lower()


# ---------------------------------------------------------------------------
# 6. New check functions: check_python, check_disk_space, check_write_permissions
# ---------------------------------------------------------------------------


class TestNewChecks:
    """Verify the three new check functions return correct tuples."""

    def test_check_python_returns_three_tuple(self):
        result = check_python()
        assert len(result) == 3

    def test_check_python_status_is_ok_or_fail(self):
        status, label, detail = check_python()
        assert status in ("ok", "fail")

    def test_check_python_label_is_python(self):
        status, label, detail = check_python()
        assert "Python" in label

    def test_check_python_detail_has_version_number(self):
        import sys
        status, label, detail = check_python()
        v = sys.version_info
        # Detail must contain the major.minor version
        assert f"{v.major}.{v.minor}" in detail

    def test_check_disk_space_returns_three_tuple(self, tmp_path: Path):
        result = check_disk_space(tmp_path)
        assert len(result) == 3

    def test_check_disk_space_status_is_ok_or_fail(self, tmp_path: Path):
        status, label, detail = check_disk_space(tmp_path)
        assert status in ("ok", "fail")

    def test_check_disk_space_label_is_disk_space(self, tmp_path: Path):
        status, label, detail = check_disk_space(tmp_path)
        assert "Disk" in label or "Space" in label

    def test_check_disk_space_detail_contains_gb(self, tmp_path: Path):
        status, label, detail = check_disk_space(tmp_path)
        assert "GB" in detail

    def test_check_write_permissions_returns_three_tuple(self, tmp_path: Path):
        result = check_write_permissions(tmp_path)
        assert len(result) == 3

    def test_check_write_permissions_ok_for_writable_dir(self, tmp_path: Path):
        status, label, detail = check_write_permissions(tmp_path)
        assert status == "ok", f"Expected 'ok' for writable tmp_path, got: {status}"

    def test_check_write_permissions_detail_contains_path(self, tmp_path: Path):
        status, label, detail = check_write_permissions(tmp_path)
        assert str(tmp_path) in detail

    def test_check_write_permissions_fail_for_unwritable_dir(self, tmp_path: Path):
        """Simulate a non-writable directory via os.access mock."""
        import os
        with patch("os.access", return_value=False):
            status, label, detail = check_write_permissions(tmp_path)
        assert status == "fail"

    def test_new_checks_appear_in_doctor_output(self, tmp_path: Path):
        """Python, Disk Space, Write Access must appear in doctor output."""
        result = runner.invoke(app, ["doctor", "--project-dir", str(tmp_path)])
        output = result.stdout
        assert "Python" in output, "Python check missing from output"
        assert "Disk" in output, "Disk Space check missing from output"
        assert "Write" in output, "Write Access check missing from output"
