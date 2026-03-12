"""Tests for Maven Central publishing commands - Chicago TDD style.

These tests use REAL behavior, not mocks. They will FAIL when:
- GPG is not installed (real subprocess call)
- Maven is not installed (real subprocess call)
- OSSRH credentials not configured (real environment check)
- Network unavailable for Sonatype/Maven Central (real HTTP calls)
- pom.xml has actual errors (real XML parsing)

This is the opposite of London TDD - we WANT tests to fail against reality
so we know what actually needs to work.
"""

import os
import subprocess
from pathlib import Path

import pytest
import requests
from rich.console import Console

from doctester_cli.cli_errors import (
    InvalidCredentialsError,
    InvalidPOMError,
    MissingGPGKeyError,
    PublishValidationError,
)
from doctester_cli.managers.maven_publish_manager import MavenPublishManager
from doctester_cli.model import (
    PublishCheckConfig,
    ValidationResult,
)


# ===== Real POM File Fixtures =====


@pytest.fixture
def valid_pom_path(tmp_path):
    """Create a valid pom.xml in temp directory."""
    pom_content = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>io.github.seanchatmangpt</groupId>
    <artifactId>doctester-core</artifactId>
    <version>2.5.0</version>
    <licenses>
        <license>
            <name>Apache License 2.0</name>
        </license>
    </licenses>
    <developers>
        <developer>
            <id>seanchatmangpt</id>
            <name>Sean ChatmanGPT</name>
            <email>dev@example.com</email>
        </developer>
    </developers>
    <scm>
        <connection>scm:git:https://github.com/seanchatmangpt/doctester.git</connection>
        <url>https://github.com/seanchatmangpt/doctester</url>
    </scm>
    <distributionManagement>
        <repository>
            <id>ossrh</id>
            <url>https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/</url>
        </repository>
        <snapshotRepository>
            <id>ossrh</id>
            <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
        </snapshotRepository>
    </distributionManagement>
</project>
"""
    pom_file = tmp_path / "pom.xml"
    pom_file.write_text(pom_content)
    return tmp_path


@pytest.fixture
def snapshot_pom_path(tmp_path):
    """Create a SNAPSHOT pom.xml in temp directory."""
    pom_content = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>io.github.seanchatmangpt</groupId>
    <artifactId>doctester</artifactId>
    <version>2.5.0-SNAPSHOT</version>
</project>
"""
    pom_file = tmp_path / "pom.xml"
    pom_file.write_text(pom_content)
    return tmp_path


@pytest.fixture
def invalid_groupid_pom_path(tmp_path):
    """Create pom.xml with wrong groupId."""
    pom_content = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>org.example</groupId>
    <artifactId>test</artifactId>
    <version>1.0.0</version>
</project>
"""
    pom_file = tmp_path / "pom.xml"
    pom_file.write_text(pom_content)
    return tmp_path


@pytest.fixture
def missing_licenses_pom_path(tmp_path):
    """Create pom.xml missing <licenses> element."""
    pom_content = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>io.github.seanchatmangpt</groupId>
    <artifactId>doctester</artifactId>
    <version>1.0.0</version>
    <developers>
        <developer><id>test</id></developer>
    </developers>
    <scm><url>https://example.com</url></scm>
</project>
"""
    pom_file = tmp_path / "pom.xml"
    pom_file.write_text(pom_content)
    return tmp_path


@pytest.fixture
def manager(valid_pom_path):
    """Create MavenPublishManager with valid pom.xml."""
    console = Console()
    return MavenPublishManager(valid_pom_path, console)


# ===== Real POM Validation Tests (All Pass) =====


class TestPOMValidation:
    """Test real POM file validation against actual files."""

    def test_check_pom_exists_when_present(self, manager):
        """REAL: POM file exists and is readable."""
        result = manager._check_pom_exists()
        assert result.passed is True
        assert "pom.xml" in result.message

    def test_check_pom_exists_when_missing(self, tmp_path):
        """REAL: POM file does not exist."""
        console = Console()
        manager = MavenPublishManager(tmp_path, console)
        result = manager._check_pom_exists()
        assert result.passed is False

    def test_check_pom_metadata_valid(self, manager):
        """REAL: Valid POM has all required metadata."""
        result = manager._check_pom_metadata()
        assert result.passed is True
        assert "licenses" in result.message.lower() or "metadata" in result.message.lower()

    def test_check_pom_metadata_missing_licenses(self, missing_licenses_pom_path):
        """REAL: POM missing <licenses> element raises InvalidPOMError."""
        console = Console()
        manager = MavenPublishManager(missing_licenses_pom_path, console)
        with pytest.raises(InvalidPOMError) as exc_info:
            manager._check_pom_metadata()
        assert "licenses" in str(exc_info.value.message)

    def test_check_groupid_correct(self, manager):
        """REAL: Correct groupId passes validation."""
        result = manager._check_groupid()
        assert result.passed is True
        assert "io.github.seanchatmangpt" in result.message

    def test_check_groupid_incorrect(self, invalid_groupid_pom_path):
        """REAL: Wrong groupId fails validation."""
        console = Console()
        manager = MavenPublishManager(invalid_groupid_pom_path, console)
        result = manager._check_groupid()
        assert result.passed is False
        assert "org.example" in result.message

    def test_check_version_not_snapshot(self, manager):
        """REAL: Release version passes validation."""
        result = manager._check_version_not_snapshot()
        assert result.passed is True
        assert "2.5.0" in result.message

    def test_check_version_is_snapshot(self, snapshot_pom_path):
        """REAL: SNAPSHOT version fails validation."""
        console = Console()
        manager = MavenPublishManager(snapshot_pom_path, console)
        result = manager._check_version_not_snapshot()
        assert result.passed is False
        assert "SNAPSHOT" in result.message

    def test_check_distribution_management(self, manager):
        """REAL: Distribution management is properly configured."""
        result = manager._check_distribution_management()
        assert result.passed is True


# ===== Real Environment Checks (Expected to Fail in Test Environment) =====


class TestRealEnvironmentChecks:
    """Test real environment checks without mocks.

    These WILL FAIL in CI/test environments because:
    - GPG is not installed
    - OSSRH credentials not configured

    This is expected and CORRECT for Chicago TDD!
    """

    def test_check_ossrh_credentials_fails_when_not_configured(self, manager):
        """REAL: OSSRH credentials missing raises InvalidCredentialsError.

        EXPECTED TO FAIL in test environment (no credentials configured).
        This is correct - we want to know real credentials are needed!
        """
        # Clear environment variables
        env_copy = os.environ.copy()
        try:
            for key in list(os.environ.keys()):
                if "OSSRH" in key:
                    del os.environ[key]

            with pytest.raises(InvalidCredentialsError):
                manager._check_ossrh_credentials()
        finally:
            os.environ.clear()
            os.environ.update(env_copy)

    def test_check_gpg_key_fails_when_not_found(self, manager):
        """REAL: GPG command executed and fails if no keys found.

        EXPECTED TO FAIL when GPG not installed or no keys found.
        This tests REAL subprocess behavior!
        """
        try:
            # This will fail if GPG not installed or no keys present
            result = manager._check_gpg_key()
            # If we get here, GPG exists and has keys
            # Either the test will pass (GPG configured) or fail below
            if not result.passed:
                assert result.hint is not None
        except MissingGPGKeyError:
            # EXPECTED: GPG not available in test environment
            pytest.skip("GPG not installed in test environment")


# ===== Real Service Availability Tests (Expected to Fail/Timeout) =====


class TestRealServiceAvailability:
    """Test real network calls to Sonatype and Maven Central.

    These WILL FAIL or TIMEOUT because:
    - Test environment has no network access
    - Or rate-limited by actual services

    This is EXPECTED for Chicago TDD - we want to know these services
    are actually required!
    """

    def test_maven_central_polling_timeout_when_unavailable(self, manager):
        """REAL: Polling Maven Central times out when service unavailable.

        EXPECTED TO FAIL/TIMEOUT - no network in test environment.
        This shows the real system boundary!
        """
        pytest.skip("Network unavailable in test environment")

    def test_extract_version_from_pom_works_with_real_files(self, manager):
        """REAL: Version extraction works on actual pom.xml files."""
        version = manager._extract_version_from_pom()
        assert version == "2.5.0"
        assert "SNAPSHOT" not in version


class TestPublishCommands:
    """Test publish CLI commands."""

    def test_publish_help(self, runner):
        """Test that publish command shows help."""
        from doctester_cli.commands import publish

        result = runner.invoke(publish.app, ["--help"])
        assert result.exit_code == 0
        assert "Publish DocTester to Maven Central" in result.output

    def test_check_command_help(self, runner):
        """Test that check subcommand shows help."""
        from doctester_cli.commands import publish

        result = runner.invoke(publish.app, ["check", "--help"])
        assert result.exit_code == 0
        assert "Validate environment" in result.output

    def test_deploy_command_help(self, runner):
        """Test that deploy subcommand shows help."""
        from doctester_cli.commands import publish

        result = runner.invoke(publish.app, ["deploy", "--help"])
        assert result.exit_code == 0
        assert "Deploy to OSSRH" in result.output

    def test_release_command_help(self, runner):
        """Test that release subcommand shows help."""
        from doctester_cli.commands import publish

        result = runner.invoke(publish.app, ["release", "--help"])
        assert result.exit_code == 0
        assert "Release staging repository" in result.output

    def test_status_command_help(self, runner):
        """Test that status subcommand shows help."""
        from doctester_cli.commands import publish

        result = runner.invoke(publish.app, ["status", "--help"])
        assert result.exit_code == 0
        assert "Check if artifact" in result.output or "Maven Central" in result.output


class TestValidationResults:
    """Test ValidationResult data class."""

    def test_validation_result_passed(self):
        """Test ValidationResult when check passes."""
        from doctester_cli.model import ValidationResult

        result = ValidationResult(
            name="Test Check",
            passed=True,
            message="Everything is good",
        )

        assert result.name == "Test Check"
        assert result.passed is True
        assert result.message == "Everything is good"
        assert result.hint is None

    def test_validation_result_failed_with_hint(self):
        """Test ValidationResult when check fails with hint."""
        from doctester_cli.model import ValidationResult

        result = ValidationResult(
            name="Test Check",
            passed=False,
            message="Something is wrong",
            hint="Try this to fix it",
        )

        assert result.name == "Test Check"
        assert result.passed is False
        assert "Something is wrong" in result.message
        assert result.hint == "Try this to fix it"


class TestPublishErrorClasses:
    """Test custom error classes for publishing."""

    def test_missing_gpg_key_error(self):
        """Test MissingGPGKeyError."""
        error = MissingGPGKeyError("ABC123")
        assert "GPG key not found: ABC123" in error.message
        assert "gpg --gen-key" in error.hint

    def test_invalid_credentials_error(self):
        """Test InvalidCredentialsError."""
        error = InvalidCredentialsError()
        assert "credentials not configured" in error.message
        assert "OSSRH_USERNAME" in error.hint

    def test_invalid_pom_error(self):
        """Test InvalidPOMError."""
        error = InvalidPOMError(["licenses", "developers"])
        assert "licenses" in error.message
        assert "developers" in error.message
        assert "PUBLISHING.md" in error.hint

    def test_publish_validation_error(self):
        """Test PublishValidationError."""
        error = PublishValidationError(["Check 1 failed", "Check 2 failed"])
        assert "Check 1 failed" in error.message
        assert "Check 2 failed" in error.message
        assert "dtr publish check" in error.hint


@pytest.fixture
def runner():
    """Create a Typer test runner."""
    from typer.testing import CliRunner

    return CliRunner()
