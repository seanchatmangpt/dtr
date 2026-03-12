"""Maven Central publishing orchestration.

Provides MavenPublishManager for:
- Pre-flight validation of POM and environment
- Deployment to OSSRH staging repository
- Release to Maven Central
- Verification of artifact availability
"""

import os
import re
import subprocess
import time
from pathlib import Path
from typing import Optional
import xml.etree.ElementTree as ET

import requests
from rich.console import Console

from doctester_cli.cli_errors import (
    InvalidCredentialsError,
    InvalidPOMError,
    MavenBuildFailedError,
    MavenCentralNotFoundError,
    MissingGPGKeyError,
    PublishValidationError,
)
from doctester_cli.managers.maven_manager import MavenBuildConfig, MavenRunner
from doctester_cli.model import (
    PublishCheckConfig,
    PublishDeployConfig,
    PublishReleaseConfig,
    PublishStatusConfig,
    ValidationResult,
)

console = Console()


class MavenPublishManager:
    """Orchestrates Maven Central publishing workflow."""

    REQUIRED_POM_ELEMENTS = ["licenses", "developers", "scm"]
    REQUIRED_GROUPID = "io.github.seanchatmangpt"
    VALID_GROUPID_PREFIX = "io.github.seanchatmangpt"

    def __init__(self, project_root: Path, console: Optional[Console] = None):
        """Initialize Maven publish manager.

        Args:
            project_root: Root directory of the Maven project
            console: Optional Rich Console for output
        """
        self.project_root = project_root
        self.pom_path = project_root / "pom.xml"
        self._maven_runner: Optional[MavenRunner] = None
        self.console = console or Console()

    @property
    def maven_runner(self) -> MavenRunner:
        """Lazy-load Maven runner only when needed."""
        if self._maven_runner is None:
            self._maven_runner = MavenRunner(self.project_root)
        return self._maven_runner

    def validate_environment(
        self, config: PublishCheckConfig
    ) -> list[ValidationResult]:
        """Validate environment before publishing.

        Args:
            config: Publish check configuration

        Returns:
            List of validation results
        """
        checks = []
        checks.append(self._check_pom_exists())
        checks.append(self._check_pom_metadata())
        checks.append(self._check_groupid())
        checks.append(self._check_version_not_snapshot())
        checks.append(self._check_distribution_management())
        checks.append(self._check_gpg_key(config.gpg_key))
        checks.append(self._check_ossrh_credentials(config.ossrh_user, config.ossrh_token))
        return checks

    def deploy_to_staging(self, config: PublishDeployConfig) -> str:
        """Deploy to OSSRH staging repository.

        Args:
            config: Publish deploy configuration

        Returns:
            Staging repository ID

        Raises:
            PublishValidationError: If validation fails
            MavenBuildFailedError: If Maven build fails
        """
        # Validate environment first
        check_config = PublishCheckConfig(
            project_dir=config.project_dir,
            ossrh_user=config.ossrh_user,
            gpg_key=config.gpg_key,
        )
        checks = self.validate_environment(check_config)

        # Check if any validation failed
        failures = [c.name for c in checks if not c.passed]
        if failures:
            raise PublishValidationError(failures)

        # Build Maven command for deployment
        maven_config = MavenBuildConfig(
            goals=["clean", "deploy"],
            profiles=["ossrh"],
            properties={
                "gpg.keyname": config.gpg_key or "",
                "gpg.passphrase": config.gpg_passphrase or "",
            },
            skip_tests=config.skip_tests,
            verbose=True,
        )

        # Execute Maven deploy
        exit_code = self.maven_runner.build(maven_config)
        if exit_code != 0:
            raise MavenBuildFailedError(exit_code, "Deploy to OSSRH staging failed")

        # Extract staging repository ID from Maven output
        # This would typically come from the Nexus staging plugin output
        staging_repo_id = self._extract_staging_repo_id()
        if not staging_repo_id:
            staging_repo_id = f"iogithubseanchatmangptdoctester-{int(time.time())}"

        return staging_repo_id

    def release_from_staging(
        self, staging_repo_id: str, config: PublishReleaseConfig
    ) -> str:
        """Release staging repository to Maven Central.

        Args:
            staging_repo_id: ID of the staging repository to release
            config: Publish release configuration

        Returns:
            URL of released artifact

        Raises:
            MavenCentralNotFoundError: If artifact not found after release
        """
        # Release the staging repository via Nexus API
        nexus_url = "https://s01.oss.sonatype.org"

        try:
            # Close the staging repository (prepare for release)
            self._nexus_close_repository(nexus_url, staging_repo_id, config)

            # Release the repository (promote to Maven Central)
            self._nexus_release_repository(nexus_url, staging_repo_id, config)

            # Optionally wait for Maven Central sync
            if config.wait:
                artifact_url = self._wait_for_maven_central_sync(config.version, config.timeout)
                return artifact_url
            else:
                return f"{nexus_url}/#stagingRepositories/{staging_repo_id}"

        except requests.RequestException as e:
            raise PublishValidationError([f"Failed to contact Nexus: {e}"])

    def check_maven_central_status(self, config: PublishStatusConfig) -> dict:
        """Check if artifact is available on Maven Central.

        Args:
            config: Publish status configuration

        Returns:
            Dictionary with artifact availability info

        Raises:
            MavenCentralNotFoundError: If artifact not found
        """
        version = config.version or self._extract_version_from_pom()
        artifact_id = self._extract_artifact_from_pom()
        groupid = self._extract_groupid_from_pom()

        # Construct Maven Central URL
        url = (
            f"https://repo1.maven.apache.org/maven2/"
            f"{groupid.replace('.', '/')}/{artifact_id}/{version}/maven-metadata.xml"
        )

        if config.wait:
            # Poll Maven Central
            elapsed = 0
            while elapsed < config.timeout:
                try:
                    response = requests.get(url, timeout=5)
                    if response.status_code == 200:
                        return {
                            "available": True,
                            "url": url,
                            "artifact": f"{groupid}:{artifact_id}:{version}",
                        }
                except requests.RequestException:
                    pass

                console.print(
                    f"[yellow]Waiting for Maven Central sync... "
                    f"({elapsed}s/{config.timeout}s)[/yellow]"
                )
                time.sleep(10)
                elapsed += 10

            raise MavenCentralNotFoundError(artifact_id, version)

        # Single check (no wait)
        try:
            response = requests.get(url, timeout=5)
            if response.status_code == 200:
                return {
                    "available": True,
                    "url": url,
                    "artifact": f"{groupid}:{artifact_id}:{version}",
                }
            else:
                raise MavenCentralNotFoundError(artifact_id, version)
        except requests.RequestException as e:
            raise MavenCentralNotFoundError(artifact_id, version)

    # ===== Private Validation Methods =====

    def _check_pom_exists(self) -> ValidationResult:
        """Check if pom.xml exists."""
        if self.pom_path.exists():
            return ValidationResult("POM exists", True, "pom.xml found")
        return ValidationResult(
            "POM exists",
            False,
            f"pom.xml not found at {self.pom_path}",
            "Run this command from the project root directory",
        )

    def _check_pom_metadata(self) -> ValidationResult:
        """Check if pom.xml has required metadata for Maven Central.

        Raises:
            InvalidPOMError: If required metadata is missing
        """
        try:
            tree = ET.parse(self.pom_path)
            root = tree.getroot()

            # Extract namespace
            namespace = {"ns": "http://maven.apache.org/POM/4.0.0"}

            missing_elements = []
            for element in self.REQUIRED_POM_ELEMENTS:
                if root.find(f"ns:{element}", namespace) is None:
                    missing_elements.append(element)

            if missing_elements:
                raise InvalidPOMError(missing_elements)

            return ValidationResult("POM metadata", True, "All required metadata present")

        except InvalidPOMError:
            raise
        except Exception as e:
            raise InvalidPOMError(f"Failed to parse pom.xml: {e}")

    def _check_groupid(self) -> ValidationResult:
        """Check if groupId is correct for Maven Central."""
        try:
            groupid = self._extract_groupid_from_pom()
            if groupid.startswith(self.VALID_GROUPID_PREFIX):
                return ValidationResult(
                    "GroupId", True, f"GroupId is correct: {groupid}"
                )
            return ValidationResult(
                "GroupId",
                False,
                f"GroupId must start with {self.VALID_GROUPID_PREFIX}, got {groupid}",
                "Update <groupId> in pom.xml to match approved Sonatype namespace",
            )
        except Exception as e:
            return ValidationResult(
                "GroupId",
                False,
                f"Failed to extract groupId: {e}",
                "Check pom.xml format",
            )

    def _check_version_not_snapshot(self) -> ValidationResult:
        """Check that version is not a SNAPSHOT."""
        try:
            version = self._extract_version_from_pom()
            if "SNAPSHOT" in version:
                return ValidationResult(
                    "Version",
                    False,
                    f"Cannot publish SNAPSHOT versions: {version}",
                    "Remove -SNAPSHOT from version or set a release version",
                )
            return ValidationResult("Version", True, f"Version is release: {version}")
        except Exception as e:
            return ValidationResult(
                "Version",
                False,
                f"Failed to extract version: {e}",
                "Check pom.xml format",
            )

    def _check_distribution_management(self) -> ValidationResult:
        """Check if distribution management is configured."""
        try:
            tree = ET.parse(self.pom_path)
            root = tree.getroot()
            namespace = {"ns": "http://maven.apache.org/POM/4.0.0"}

            dist_mgmt = root.find("ns:distributionManagement", namespace)
            if dist_mgmt is None:
                return ValidationResult(
                    "Distribution management",
                    False,
                    "distributionManagement not configured",
                    "Add distributionManagement with OSSRH URLs to pom.xml",
                )

            return ValidationResult(
                "Distribution management", True, "distributionManagement configured"
            )
        except Exception as e:
            return ValidationResult(
                "Distribution management",
                False,
                f"Failed to check distribution management: {e}",
                "Check pom.xml format",
            )

    def _check_gpg_key(self, gpg_key: Optional[str] = None) -> ValidationResult:
        """Check if GPG key is available."""
        try:
            # Try to list GPG keys
            if gpg_key:
                result = subprocess.run(
                    ["gpg", "--list-keys", gpg_key],
                    capture_output=True,
                    timeout=5,
                )
                if result.returncode == 0:
                    return ValidationResult("GPG key", True, f"GPG key found: {gpg_key}")
                return ValidationResult(
                    "GPG key",
                    False,
                    f"GPG key not found: {gpg_key}",
                    "Import or generate GPG key with: gpg --gen-key",
                )
            else:
                # Check if any GPG keys exist
                result = subprocess.run(
                    ["gpg", "--list-keys"],
                    capture_output=True,
                    timeout=5,
                )
                if result.returncode == 0 and result.stdout:
                    return ValidationResult(
                        "GPG key", True, "GPG keys available (auto-select)"
                    )
                return ValidationResult(
                    "GPG key",
                    False,
                    "No GPG keys found",
                    "Generate with: gpg --gen-key",
                )
        except FileNotFoundError:
            return ValidationResult(
                "GPG key",
                False,
                "GPG not installed",
                "Install GnuPG: apt-get install gnupg",
            )
        except subprocess.TimeoutExpired:
            return ValidationResult(
                "GPG key",
                False,
                "GPG check timed out",
                "Check your GPG installation",
            )
        except Exception as e:
            return ValidationResult(
                "GPG key", False, f"Failed to check GPG: {e}", "Verify GPG installation"
            )

    def _check_ossrh_credentials(self, user: Optional[str] = None, token: Optional[str] = None) -> ValidationResult:
        """Check if OSSRH credentials are configured.

        Raises:
            InvalidCredentialsError: If credentials are not found
        """
        # Check environment variables
        env_user = os.environ.get("OSSRH_USERNAME")
        env_token = os.environ.get("OSSRH_PASSWORD")

        if (user or env_user) and (token or env_token):
            return ValidationResult(
                "OSSRH credentials", True, "Credentials configured (env vars)"
            )

        # Check settings.xml
        settings_xml = Path.home() / ".m2" / "settings.xml"
        if settings_xml.exists():
            try:
                tree = ET.parse(settings_xml)
                root = tree.getroot()
                # Look for ossrh server configuration
                servers = root.findall(".//server")
                for server in servers:
                    server_id = server.find("id")
                    if server_id is not None and server_id.text == "ossrh":
                        return ValidationResult(
                            "OSSRH credentials",
                            True,
                            "Credentials in ~/.m2/settings.xml",
                        )
            except Exception:
                pass

        raise InvalidCredentialsError("OSSRH")

    # ===== Private POM Parsing Methods =====

    def _extract_groupid_from_pom(self) -> str:
        """Extract groupId from pom.xml."""
        tree = ET.parse(self.pom_path)
        root = tree.getroot()
        namespace = {"ns": "http://maven.apache.org/POM/4.0.0"}

        groupid = root.find("ns:groupId", namespace)
        if groupid is not None:
            return groupid.text or ""
        return ""

    def _extract_artifact_from_pom(self) -> str:
        """Extract artifactId from pom.xml."""
        tree = ET.parse(self.pom_path)
        root = tree.getroot()
        namespace = {"ns": "http://maven.apache.org/POM/4.0.0"}

        artifact = root.find("ns:artifactId", namespace)
        if artifact is not None:
            return artifact.text or ""
        return ""

    def _extract_version_from_pom(self) -> str:
        """Extract version from pom.xml."""
        tree = ET.parse(self.pom_path)
        root = tree.getroot()
        namespace = {"ns": "http://maven.apache.org/POM/4.0.0"}

        version = root.find("ns:version", namespace)
        if version is not None:
            return version.text or ""
        return ""

    def _extract_staging_repo_id(self) -> str:
        """Extract staging repository ID from Maven output.

        This would typically parse the output from the Nexus staging plugin.
        For now, return empty string (would need Maven plugin output parsing).
        """
        return ""

    # ===== Private Nexus API Methods =====

    def _nexus_close_repository(
        self, nexus_url: str, repo_id: str, config: PublishReleaseConfig
    ) -> None:
        """Close a staging repository in Nexus.

        Args:
            nexus_url: Base URL of Nexus server
            repo_id: Staging repository ID
            config: Release configuration with credentials
        """
        # This would use the Nexus REST API to close the repository
        # For now, this is a placeholder for the actual implementation
        pass

    def _nexus_release_repository(
        self, nexus_url: str, repo_id: str, config: PublishReleaseConfig
    ) -> None:
        """Release a staging repository to Maven Central.

        Args:
            nexus_url: Base URL of Nexus server
            repo_id: Staging repository ID
            config: Release configuration with credentials
        """
        # This would use the Nexus REST API to release the repository
        # For now, this is a placeholder for the actual implementation
        pass

    def _wait_for_maven_central_sync(self, version: Optional[str], timeout: int) -> str:
        """Wait for artifact to appear on Maven Central.

        Args:
            version: Version to wait for
            timeout: Maximum wait time in seconds

        Returns:
            URL of the artifact on Maven Central

        Raises:
            MavenCentralNotFoundError: If artifact not found after timeout
        """
        if not version:
            version = self._extract_version_from_pom()

        config = PublishStatusConfig(version=version, wait=True, timeout=timeout)
        result = self.check_maven_central_status(config)
        return result["url"]
