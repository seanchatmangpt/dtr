"""Chicago-style TDD tests for `dtr template` command group.

Tests real behavior with real collaborators — no mocks, no fakes.
Uses typer CliRunner to invoke the real CLI app end-to-end and
pytest tmp_path for real file I/O.
"""

from pathlib import Path

import pytest
from typer.testing import CliRunner

from dtr_cli.main import app
from dtr_cli.commands.template import TEMPLATES, _class_name_to_title

runner = CliRunner()

# ---------------------------------------------------------------------------
# 1. dtr template list — show all built-in templates
# ---------------------------------------------------------------------------


class TestTemplateList:
    """Test `dtr template list` displays the full template catalogue."""

    def test_exits_successfully(self):
        result = runner.invoke(app, ["template", "list"])
        assert result.exit_code == 0, f"stdout: {result.stdout}"

    def test_shows_all_five_built_in_templates(self):
        result = runner.invoke(app, ["template", "list"])
        assert result.exit_code == 0
        for name in ("basic", "api", "benchmark", "tutorial", "changelog"):
            assert name in result.stdout, f"Template '{name}' not found in output"

    def test_shows_template_categories(self):
        result = runner.invoke(app, ["template", "list"])
        assert result.exit_code == 0
        for category in ("general", "api", "performance", "docs"):
            assert category in result.stdout

    def test_shows_template_descriptions(self):
        result = runner.invoke(app, ["template", "list"])
        assert result.exit_code == 0
        # Spot-check a few description fragments
        assert "simple" in result.stdout.lower() or "documentation" in result.stdout.lower()
        assert "REST" in result.stdout or "api" in result.stdout.lower()
        assert "benchmark" in result.stdout.lower() or "performance" in result.stdout.lower()

    def test_includes_usage_hint(self):
        result = runner.invoke(app, ["template", "list"])
        assert result.exit_code == 0
        assert "template show" in result.stdout or "template apply" in result.stdout


# ---------------------------------------------------------------------------
# 2. dtr template show TEMPLATE_NAME — display source with highlighting
# ---------------------------------------------------------------------------


class TestTemplateShow:
    """Test `dtr template show` displays Java source for a given template."""

    def test_shows_basic_template_source(self):
        result = runner.invoke(app, ["template", "show", "basic"])
        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "DocTesterContext" in result.stdout
        assert "DocTesterExtension" in result.stdout
        assert "@Test" in result.stdout

    def test_shows_api_template_source(self):
        result = runner.invoke(app, ["template", "show", "api"])
        assert result.exit_code == 0
        assert "sayAndMakeRequest" in result.stdout or "Request.GET" in result.stdout
        assert "sayTable" in result.stdout

    def test_shows_benchmark_template_source(self):
        result = runner.invoke(app, ["template", "show", "benchmark"])
        assert result.exit_code == 0
        assert "nanoTime" in result.stdout or "MEASURE_ITERATIONS" in result.stdout

    def test_shows_tutorial_template_source(self):
        result = runner.invoke(app, ["template", "show", "tutorial"])
        assert result.exit_code == 0
        assert "sayOrderedList" in result.stdout or "sayUnorderedList" in result.stdout

    def test_shows_changelog_template_source(self):
        result = runner.invoke(app, ["template", "show", "changelog"])
        assert result.exit_code == 0
        assert "v2.0.0" in result.stdout or "v1.0.0" in result.stdout

    def test_show_unknown_template_exits_nonzero(self):
        result = runner.invoke(app, ["template", "show", "nonexistent"])
        assert result.exit_code != 0

    def test_show_unknown_template_reports_error(self):
        result = runner.invoke(app, ["template", "show", "nonexistent"])
        assert "Unknown template" in result.stdout or "nonexistent" in result.stdout

    def test_show_unknown_template_lists_available_names(self):
        result = runner.invoke(app, ["template", "show", "ghost"])
        # Should hint at what's available
        assert "basic" in result.stdout


# ---------------------------------------------------------------------------
# 3. dtr template apply — generate Java file from template
# ---------------------------------------------------------------------------


class TestTemplateApply:
    """Test `dtr template apply` creates Java files with correct content."""

    def test_apply_basic_creates_file(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "basic",
            "--class-name", "MyFeatureDocTest",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code == 0, f"stdout: {result.stdout}"

        expected = (
            tmp_path
            / "dtr-integration-test"
            / "src" / "test" / "java"
            / "controllers"
            / "MyFeatureDocTest.java"
        )
        assert expected.exists(), f"Expected file not created: {expected}"

    def test_apply_writes_correct_package(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "basic",
            "--class-name", "PkgDocTest",
            "--package", "com.example.docs",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code == 0

        java_file = (
            tmp_path
            / "dtr-integration-test"
            / "src" / "test" / "java"
            / "com" / "example" / "docs"
            / "PkgDocTest.java"
        )
        assert java_file.exists()
        content = java_file.read_text(encoding="utf-8")
        assert "package com.example.docs;" in content

    def test_apply_writes_correct_class_name(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "basic",
            "--class-name", "OrderServiceDocTest",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code == 0

        java_file = (
            tmp_path
            / "dtr-integration-test"
            / "src" / "test" / "java"
            / "controllers"
            / "OrderServiceDocTest.java"
        )
        content = java_file.read_text(encoding="utf-8")
        assert "class OrderServiceDocTest" in content

    def test_apply_substitutes_title(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "basic",
            "--class-name", "PaymentServiceDocTest",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code == 0

        java_file = (
            tmp_path
            / "dtr-integration-test"
            / "src" / "test" / "java"
            / "controllers"
            / "PaymentServiceDocTest.java"
        )
        content = java_file.read_text(encoding="utf-8")
        # Title derived from class name: "Payment Service"
        assert "Payment Service" in content

    def test_apply_respects_custom_module(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "api",
            "--class-name", "PetsApiDocTest",
            "--module", "petstore-service",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code == 0

        java_file = (
            tmp_path
            / "petstore-service"
            / "src" / "test" / "java"
            / "controllers"
            / "PetsApiDocTest.java"
        )
        assert java_file.exists()

    def test_apply_api_template_contains_request_code(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "api",
            "--class-name", "UsersApiDocTest",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code == 0

        java_file = (
            tmp_path
            / "dtr-integration-test"
            / "src" / "test" / "java"
            / "controllers"
            / "UsersApiDocTest.java"
        )
        content = java_file.read_text(encoding="utf-8")
        assert "Request.GET" in content or "sayAndMakeRequest" in content

    def test_apply_benchmark_template_contains_nanotime(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "benchmark",
            "--class-name", "HashMapBenchmarkDocTest",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code == 0

        java_file = (
            tmp_path
            / "dtr-integration-test"
            / "src" / "test" / "java"
            / "controllers"
            / "HashMapBenchmarkDocTest.java"
        )
        content = java_file.read_text(encoding="utf-8")
        assert "nanoTime" in content or "MEASURE_ITERATIONS" in content

    def test_apply_prints_file_path_on_success(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "basic",
            "--class-name", "FooDocTest",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code == 0
        assert "Created" in result.stdout

    def test_apply_creates_nested_package_directories(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "tutorial",
            "--class-name", "GettingStartedDocTest",
            "--package", "io.example.docs.tutorials",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code == 0

        java_file = (
            tmp_path
            / "dtr-integration-test"
            / "src" / "test" / "java"
            / "io" / "example" / "docs" / "tutorials"
            / "GettingStartedDocTest.java"
        )
        assert java_file.exists()


# ---------------------------------------------------------------------------
# 4. dtr template apply --force — overwrite behaviour
# ---------------------------------------------------------------------------


class TestTemplateApplyForce:
    """Test --force flag allows overwriting an existing file."""

    def _apply(self, tmp_path: Path, extra_args: list[str] | None = None) -> object:
        args = [
            "template", "apply", "basic",
            "--class-name", "OverwriteDocTest",
            "--project-dir", str(tmp_path),
        ]
        if extra_args:
            args.extend(extra_args)
        return runner.invoke(app, args)

    def test_without_force_fails_if_file_exists(self, tmp_path: Path):
        # Create the file first
        self._apply(tmp_path)
        # Second apply without --force must fail
        result = self._apply(tmp_path)
        assert result.exit_code != 0

    def test_without_force_reports_already_exists(self, tmp_path: Path):
        self._apply(tmp_path)
        result = self._apply(tmp_path)
        assert "already exists" in result.stdout.lower() or "exists" in result.stdout.lower()

    def test_without_force_suggests_force_flag(self, tmp_path: Path):
        self._apply(tmp_path)
        result = self._apply(tmp_path)
        assert "--force" in result.stdout

    def test_with_force_overwrites_existing_file(self, tmp_path: Path):
        # First apply
        self._apply(tmp_path)
        java_file = (
            tmp_path
            / "dtr-integration-test"
            / "src" / "test" / "java"
            / "controllers"
            / "OverwriteDocTest.java"
        )
        original_mtime = java_file.stat().st_mtime

        # Second apply with --force
        result = self._apply(tmp_path, extra_args=["--force"])
        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert java_file.exists()

    def test_with_force_short_flag_works(self, tmp_path: Path):
        self._apply(tmp_path)
        result = self._apply(tmp_path, extra_args=["-f"])
        assert result.exit_code == 0


# ---------------------------------------------------------------------------
# 5. Error cases
# ---------------------------------------------------------------------------


class TestTemplateErrorCases:
    """Test error handling for unknown templates and invalid inputs."""

    def test_apply_unknown_template_exits_nonzero(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "unknown-template",
            "--class-name", "SomeDocTest",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code != 0

    def test_apply_unknown_template_reports_name(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "doesnotexist",
            "--class-name", "SomeDocTest",
            "--project-dir", str(tmp_path),
        ])
        assert "doesnotexist" in result.stdout

    def test_apply_unknown_template_lists_valid_names(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "nope",
            "--class-name", "SomeDocTest",
            "--project-dir", str(tmp_path),
        ])
        assert "basic" in result.stdout

    def test_apply_missing_class_name_exits_nonzero(self, tmp_path: Path):
        result = runner.invoke(app, [
            "template", "apply", "basic",
            "--project-dir", str(tmp_path),
        ])
        assert result.exit_code != 0

    def test_template_no_args_shows_help(self):
        result = runner.invoke(app, ["template"])
        # no_args_is_help=True means it shows help, not necessarily a non-zero exit
        assert "list" in result.stdout or "show" in result.stdout or "apply" in result.stdout


# ---------------------------------------------------------------------------
# 6. TEMPLATES registry unit tests
# ---------------------------------------------------------------------------


class TestTemplatesRegistry:
    """Unit tests for the TEMPLATES constant and helper functions."""

    def test_registry_has_exactly_five_templates(self):
        assert len(TEMPLATES) == 5

    def test_registry_contains_required_keys(self):
        for name in ("basic", "api", "benchmark", "tutorial", "changelog"):
            assert name in TEMPLATES

    def test_each_template_has_description(self):
        for name, meta in TEMPLATES.items():
            assert "description" in meta, f"Template '{name}' missing 'description'"
            assert meta["description"], f"Template '{name}' has empty description"

    def test_each_template_has_category(self):
        for name, meta in TEMPLATES.items():
            assert "category" in meta, f"Template '{name}' missing 'category'"
            assert meta["category"], f"Template '{name}' has empty category"

    def test_each_template_has_source(self):
        for name, meta in TEMPLATES.items():
            assert "source" in meta, f"Template '{name}' missing 'source'"
            assert meta["source"].strip(), f"Template '{name}' has empty source"

    def test_each_template_source_contains_placeholders(self):
        required_placeholders = ("{package}", "{class_name}", "{title}")
        for name, meta in TEMPLATES.items():
            for placeholder in required_placeholders:
                assert placeholder in meta["source"], (
                    f"Template '{name}' missing placeholder {placeholder!r}"
                )

    def test_each_template_source_has_doctester_import(self):
        for name, meta in TEMPLATES.items():
            assert "DocTesterContext" in meta["source"], (
                f"Template '{name}' missing DocTesterContext import"
            )

    def test_class_name_to_title_strips_doctest_suffix(self):
        assert _class_name_to_title("MyFeatureDocTest") == "My Feature"

    def test_class_name_to_title_strips_test_suffix(self):
        assert _class_name_to_title("SecurityTest") == "Security"

    def test_class_name_to_title_splits_camel_case(self):
        assert _class_name_to_title("OrderServiceDocTest") == "Order Service"

    def test_class_name_to_title_handles_multiple_words(self):
        assert _class_name_to_title("UserRegistrationFlowDocTest") == "User Registration Flow"
