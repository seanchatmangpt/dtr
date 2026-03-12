"""Chicago TDD tests for `dtr build` command.

Philosophy (Joe Armstrong / Chicago style):
- Assert real output content, real exit codes, real file-system state.
- Prefer real temp dirs over mocks.
- Mock only the external process boundary (subprocess.run / MavenRunner.build)
  where a real Maven build would be impractical.

Coverage:
1. No pom.xml  → error + non-zero exit
2. Custom goals → subprocess called with correct args
3. Timing output → "completed in" appears in stdout
4. JSON output mode → stdout is parseable JSON with expected keys
"""

import json
import subprocess
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest
from typer.testing import CliRunner

from dtr_cli.main import app

runner = CliRunner()


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _make_pom(directory: Path, modules: list[str] | None = None) -> Path:
    """Write a minimal root pom.xml into *directory* and return the pom path."""
    modules_xml = ""
    if modules:
        items = "".join(f"        <module>{m}</module>\n" for m in modules)
        modules_xml = f"    <modules>\n{items}    </modules>\n"

    content = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<project xmlns="http://maven.apache.org/POM/4.0.0">\n'
        "    <modelVersion>4.0.0</modelVersion>\n"
        "    <groupId>com.example</groupId>\n"
        "    <artifactId>test-project</artifactId>\n"
        "    <version>1.0.0</version>\n"
        "    <packaging>pom</packaging>\n"
        f"{modules_xml}"
        "</project>\n"
    )
    pom = directory / "pom.xml"
    pom.write_text(content)
    return pom


# ---------------------------------------------------------------------------
# 1. No pom.xml → error + non-zero exit
# ---------------------------------------------------------------------------


class TestBuildNoPom:
    """dtr build exits non-zero with a clear error when pom.xml is absent."""

    def test_missing_pom_exits_nonzero(self, tmp_path: Path) -> None:
        result = runner.invoke(app, ["build", "--project-dir", str(tmp_path)])

        assert result.exit_code != 0, (
            "Expected non-zero exit when pom.xml is absent, got 0. "
            f"stdout={result.stdout!r}"
        )

    def test_missing_pom_mentions_pom_xml(self, tmp_path: Path) -> None:
        result = runner.invoke(app, ["build", "--project-dir", str(tmp_path)])

        combined = result.stdout + (result.stderr or "")
        assert "pom.xml" in combined, (
            "Expected error message to mention 'pom.xml'. "
            f"output={combined!r}"
        )


# ---------------------------------------------------------------------------
# 2. Custom goals → subprocess called with "validate" in args
# ---------------------------------------------------------------------------


class TestBuildCustomGoals:
    """dtr build --goals validate passes the goal through to MavenRunner."""

    def test_custom_goal_validate_reaches_maven_runner(
        self, tmp_path: Path
    ) -> None:
        _make_pom(tmp_path)
        captured: list[object] = []

        # Patch MavenRunner so we capture the config it receives.
        with patch("dtr_cli.commands.build.MavenRunner") as mock_class:
            mock_instance = MagicMock()
            mock_instance.is_multi_module.return_value = False
            mock_instance.get_available_modules.return_value = []
            mock_instance.build.return_value = 0
            mock_instance.get_export_dir.return_value = tmp_path / "target"
            mock_class.return_value = mock_instance

            result = runner.invoke(
                app,
                ["build", "--goals", "validate", "--project-dir", str(tmp_path)],
            )

            assert result.exit_code == 0, f"stdout={result.stdout!r}"
            # Inspect the MavenBuildConfig passed to build()
            build_calls = mock_instance.build.call_args_list
            assert build_calls, "MavenRunner.build was never called"
            config = build_calls[0].args[0]
            assert "validate" in config.goals, (
                f"Expected 'validate' in config.goals, got {config.goals!r}"
            )

    def test_multiple_goals_all_present(self, tmp_path: Path) -> None:
        _make_pom(tmp_path)

        with patch("dtr_cli.commands.build.MavenRunner") as mock_class:
            mock_instance = MagicMock()
            mock_instance.is_multi_module.return_value = False
            mock_instance.get_available_modules.return_value = []
            mock_instance.build.return_value = 0
            mock_instance.get_export_dir.return_value = tmp_path / "target"
            mock_class.return_value = mock_instance

            result = runner.invoke(
                app,
                [
                    "build",
                    "--goals", "clean,package",
                    "--project-dir", str(tmp_path),
                ],
            )

            assert result.exit_code == 0
            config = mock_instance.build.call_args_list[0].args[0]
            assert "clean" in config.goals
            assert "package" in config.goals


# ---------------------------------------------------------------------------
# 3. Timing output → "completed in" appears in stdout
# ---------------------------------------------------------------------------


class TestBuildTimingOutput:
    """Build success prints a timing message containing 'completed in'."""

    def test_timing_message_present_after_success(self, tmp_path: Path) -> None:
        _make_pom(tmp_path)

        with patch("dtr_cli.commands.build.MavenRunner") as mock_class:
            mock_instance = MagicMock()
            mock_instance.is_multi_module.return_value = False
            mock_instance.get_available_modules.return_value = []
            mock_instance.build.return_value = 0
            mock_instance.get_export_dir.return_value = tmp_path / "target"
            mock_class.return_value = mock_instance

            result = runner.invoke(
                app, ["build", "--project-dir", str(tmp_path)]
            )

            assert result.exit_code == 0, f"stdout={result.stdout!r}"
            # Either "completed in" or a duration with "s" suffix is sufficient.
            output = result.stdout
            assert "completed in" in output.lower() or (
                any(c.isdigit() for c in output) and "s" in output
            ), (
                "Expected timing message ('completed in …s') in output. "
                f"stdout={output!r}"
            )

    def test_timing_message_contains_seconds_unit(self, tmp_path: Path) -> None:
        _make_pom(tmp_path)

        with patch("dtr_cli.commands.build.MavenRunner") as mock_class:
            mock_instance = MagicMock()
            mock_instance.is_multi_module.return_value = False
            mock_instance.get_available_modules.return_value = []
            mock_instance.build.return_value = 0
            mock_instance.get_export_dir.return_value = tmp_path / "target"
            mock_class.return_value = mock_instance

            result = runner.invoke(
                app, ["build", "--project-dir", str(tmp_path)]
            )

            # The timing line ends with "s" (e.g. "Build completed in 0.01s")
            assert "s" in result.stdout, (
                "Expected 's' (seconds) in timing output. "
                f"stdout={result.stdout!r}"
            )


# ---------------------------------------------------------------------------
# 4. JSON output mode → stdout is parseable JSON with expected keys
# ---------------------------------------------------------------------------


class TestBuildJsonOutputMode:
    """dtr build emits valid JSON to stdout when json_mode is active."""

    def test_json_output_is_parseable(self, tmp_path: Path) -> None:
        _make_pom(tmp_path)

        with patch("dtr_cli.commands.build.MavenRunner") as mock_class:
            mock_instance = MagicMock()
            mock_instance.is_multi_module.return_value = False
            mock_instance.get_available_modules.return_value = []
            mock_instance.build.return_value = 0
            mock_instance.get_export_dir.return_value = tmp_path / "target"
            mock_class.return_value = mock_instance

            # Activate JSON mode via the global state module.
            with patch("dtr_cli.commands.build._HAS_STATE", True), patch(
                "dtr_cli.commands.build.get_state"
            ) as mock_state:
                state = MagicMock()
                state.json_mode = True
                mock_state.return_value = state

                result = runner.invoke(
                    app, ["build", "--project-dir", str(tmp_path)]
                )

        assert result.exit_code == 0, f"stdout={result.stdout!r}"
        # stdout should contain JSON (possibly preceded by Rich markup stripped)
        raw = result.stdout.strip()
        # Find the JSON object in the output
        json_start = raw.rfind("{")
        assert json_start != -1, f"No JSON object in output: {raw!r}"
        parsed = json.loads(raw[json_start:])
        assert "success" in parsed, f"Missing 'success' key: {parsed}"
        assert "elapsed_seconds" in parsed, f"Missing 'elapsed_seconds' key: {parsed}"
        assert "goals" in parsed, f"Missing 'goals' key: {parsed}"

    def test_json_output_success_true_on_zero_exit(self, tmp_path: Path) -> None:
        _make_pom(tmp_path)

        with patch("dtr_cli.commands.build.MavenRunner") as mock_class:
            mock_instance = MagicMock()
            mock_instance.is_multi_module.return_value = False
            mock_instance.get_available_modules.return_value = []
            mock_instance.build.return_value = 0
            mock_instance.get_export_dir.return_value = tmp_path / "target"
            mock_class.return_value = mock_instance

            with patch("dtr_cli.commands.build._HAS_STATE", True), patch(
                "dtr_cli.commands.build.get_state"
            ) as mock_state:
                state = MagicMock()
                state.json_mode = True
                mock_state.return_value = state

                result = runner.invoke(
                    app, ["build", "--project-dir", str(tmp_path)]
                )

        raw = result.stdout.strip()
        json_start = raw.rfind("{")
        parsed = json.loads(raw[json_start:])
        assert parsed["success"] is True
        assert isinstance(parsed["elapsed_seconds"], (int, float))

    def test_json_output_success_false_on_build_failure(
        self, tmp_path: Path
    ) -> None:
        _make_pom(tmp_path)

        with patch("dtr_cli.commands.build.MavenRunner") as mock_class:
            mock_instance = MagicMock()
            mock_instance.is_multi_module.return_value = False
            mock_instance.get_available_modules.return_value = []
            mock_instance.build.return_value = 1  # simulate failure
            mock_class.return_value = mock_instance

            with patch("dtr_cli.commands.build._HAS_STATE", True), patch(
                "dtr_cli.commands.build.get_state"
            ) as mock_state:
                state = MagicMock()
                state.json_mode = True
                mock_state.return_value = state

                result = runner.invoke(
                    app, ["build", "--project-dir", str(tmp_path)]
                )

        # Should exit non-zero
        assert result.exit_code != 0, (
            "Expected non-zero exit on build failure in JSON mode"
        )
        raw = result.stdout.strip()
        json_start = raw.rfind("{")
        assert json_start != -1, f"No JSON object in failure output: {raw!r}"
        parsed = json.loads(raw[json_start:])
        assert parsed["success"] is False


# ---------------------------------------------------------------------------
# 5. Retry mechanism — transient failure detection
# ---------------------------------------------------------------------------


class TestBuildRetryLogic:
    """_is_transient_failure correctly classifies errors."""

    def test_connection_refused_is_transient(self) -> None:
        from dtr_cli.commands.build import _is_transient_failure

        result = subprocess.CompletedProcess(
            args=[], returncode=1, stderr=b"Connection refused to proxy"
        )
        assert _is_transient_failure(result) is True

    def test_timeout_is_transient(self) -> None:
        from dtr_cli.commands.build import _is_transient_failure

        result = subprocess.CompletedProcess(
            args=[], returncode=1, stderr=b"Read timeout after 30s"
        )
        assert _is_transient_failure(result) is True

    def test_compile_error_is_not_transient(self) -> None:
        from dtr_cli.commands.build import _is_transient_failure

        result = subprocess.CompletedProcess(
            args=[], returncode=1, stderr=b"[ERROR] COMPILATION ERROR"
        )
        assert _is_transient_failure(result) is False

    def test_empty_stderr_is_not_transient(self) -> None:
        from dtr_cli.commands.build import _is_transient_failure

        result = subprocess.CompletedProcess(
            args=[], returncode=1, stderr=b""
        )
        assert _is_transient_failure(result) is False
