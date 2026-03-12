"""Maven Central publishing orchestration.

Provides MavenPublishManager for:
- Pre-flight validation of POM and environment
- Deployment to OSSRH staging repository
- Release to Maven Central
- Verification of artifact availability
"""

import base64
import json
import os
import re
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Optional
import xml.etree.ElementTree as ET

import requests
from rich.console import Console

from dtr_cli.cli_errors import (
    InvalidCredentialsError,
    InvalidPOMError,
    MavenBuildFailedError,
    MavenCentralNotFoundError,
    MissingGPGKeyError,
    PublishValidationError,
)
from dtr_cli.managers.maven_manager import MavenBuildConfig, MavenRunner
from dtr_cli.model import (
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
        self.maven_runner = MavenRunner(project_root)
        self.console = console or Console()

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
            staging_repo_id = f"iogithubseanchatmangptdtr-{int(time.time())}"

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

        Sends a POST to /service/local/staging/bulk/close to transition the
        repository from "open" to "closed" so it can be inspected and released.
        After posting, polls the activity endpoint until the close transition
        completes or the timeout is reached.

        Args:
            nexus_url: Base URL of Nexus server (e.g. https://s01.oss.sonatype.org)
            repo_id: Staging repository ID (e.g. iogithubseanchatmangpt-1001)
            config: Release configuration with credentials

        Raises:
            PublishValidationError: If the HTTP request fails or times out
        """
        endpoint = f"{nexus_url}/service/local/staging/bulk/close"
        payload = {
            "data": {
                "stagedRepositoryIds": [repo_id],
                "description": f"Closing staging repository {repo_id} for release",
            }
        }

        self.console.print(f"[cyan]Closing staging repository: {repo_id}[/cyan]")
        self._nexus_post(endpoint, payload, config)

        # Poll until the repository has finished closing
        self._nexus_wait_for_state(nexus_url, repo_id, "closed", config)

    def _nexus_release_repository(
        self, nexus_url: str, repo_id: str, config: PublishReleaseConfig
    ) -> None:
        """Release a staging repository to Maven Central.

        Sends a POST to /service/local/staging/bulk/promote to promote the
        closed repository to Maven Central. The autoDropAfterRelease flag
        instructs Nexus to drop the staging repository automatically once the
        promotion succeeds.

        Args:
            nexus_url: Base URL of Nexus server (e.g. https://s01.oss.sonatype.org)
            repo_id: Staging repository ID (e.g. iogithubseanchatmangpt-1001)
            config: Release configuration with credentials

        Raises:
            PublishValidationError: If the HTTP request fails
        """
        endpoint = f"{nexus_url}/service/local/staging/bulk/promote"
        payload = {
            "data": {
                "stagedRepositoryIds": [repo_id],
                "description": f"Releasing staging repository {repo_id} to Maven Central",
                "autoDropAfterRelease": True,
            }
        }

        self.console.print(f"[cyan]Releasing staging repository: {repo_id}[/cyan]")
        self._nexus_post(endpoint, payload, config)

    def _nexus_build_auth_header(self, config: PublishReleaseConfig) -> str:
        """Build a Basic-Auth header value from OSSRH credentials.

        Credential resolution order:
        1. ``config.ossrh_user`` / ``config.ossrh_token``
        2. ``OSSRH_USERNAME`` / ``OSSRH_PASSWORD`` environment variables

        Args:
            config: Release configuration

        Returns:
            Value for the ``Authorization`` header

        Raises:
            PublishValidationError: If no credentials are found
        """
        user = config.ossrh_user or os.environ.get("OSSRH_USERNAME", "")
        token = config.ossrh_token or os.environ.get("OSSRH_PASSWORD", "")

        if not user or not token:
            raise PublishValidationError(
                ["OSSRH credentials not configured for Nexus API call. "
                 "Set OSSRH_USERNAME/OSSRH_PASSWORD environment variables "
                 "or pass --ossrh-user/--ossrh-token flags."]
            )

        credentials = f"{user}:{token}"
        encoded = base64.b64encode(credentials.encode("utf-8")).decode("ascii")
        return f"Basic {encoded}"

    def _nexus_post(
        self, endpoint: str, payload: dict, config: PublishReleaseConfig
    ) -> None:
        """POST JSON payload to a Nexus REST endpoint with Basic Auth.

        Uses the stdlib ``urllib.request`` exclusively so there are no
        additional dependencies beyond what is already imported.

        Args:
            endpoint: Full URL to POST to
            payload: Dictionary that will be serialised to JSON
            config: Release configuration (supplies credentials)

        Raises:
            PublishValidationError: On HTTP 4xx/5xx responses or network errors
        """
        auth_header = self._nexus_build_auth_header(config)
        body = json.dumps(payload).encode("utf-8")

        req = urllib.request.Request(
            endpoint,
            data=body,
            method="POST",
        )
        req.add_header("Authorization", auth_header)
        req.add_header("Content-Type", "application/json")
        req.add_header("Accept", "application/json")

        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                status = response.status
                if status not in (200, 201, 202, 204):
                    raise PublishValidationError(
                        [f"Nexus API returned unexpected status {status} for {endpoint}"]
                    )
        except urllib.error.HTTPError as exc:
            body_text = ""
            try:
                body_text = exc.read().decode("utf-8", errors="replace")
            except Exception:
                pass
            raise PublishValidationError(
                [
                    f"Nexus API HTTP {exc.code} for {endpoint}: {exc.reason}. "
                    f"Response body: {body_text[:500]}"
                ]
            )
        except urllib.error.URLError as exc:
            raise PublishValidationError(
                [f"Network error contacting Nexus at {endpoint}: {exc.reason}"]
            )

    def _nexus_wait_for_state(
        self,
        nexus_url: str,
        repo_id: str,
        expected_state: str,
        config: PublishReleaseConfig,
        poll_interval: int = 10,
        max_wait: int = 300,
    ) -> None:
        """Poll the Nexus staging repository endpoint until it reaches expected_state.

        Nexus staging operations (close, release) are asynchronous. This method
        queries ``/service/local/staging/repository/{repo_id}`` every
        ``poll_interval`` seconds and checks the ``type`` field in the JSON
        response against ``expected_state``.

        Args:
            nexus_url: Base URL of Nexus server
            repo_id: Staging repository ID to poll
            expected_state: Target state string (e.g. ``"closed"``)
            config: Release configuration (supplies credentials)
            poll_interval: Seconds between polls (default 10)
            max_wait: Maximum total wait time in seconds (default 300)

        Raises:
            PublishValidationError: If the state is not reached within max_wait
                seconds, or if any HTTP request fails
        """
        auth_header = self._nexus_build_auth_header(config)
        status_url = f"{nexus_url}/service/local/staging/repository/{repo_id}"
        elapsed = 0

        self.console.print(
            f"[yellow]Waiting for repository {repo_id} to reach state '{expected_state}'...[/yellow]"
        )

        while elapsed < max_wait:
            req = urllib.request.Request(status_url, method="GET")
            req.add_header("Authorization", auth_header)
            req.add_header("Accept", "application/json")

            try:
                with urllib.request.urlopen(req, timeout=15) as response:
                    data = json.loads(response.read().decode("utf-8"))
                    current_state = data.get("type", "")
                    if current_state == expected_state:
                        self.console.print(
                            f"[green]Repository {repo_id} reached state '{expected_state}'[/green]"
                        )
                        return

                    # Check for failure/dropped state
                    if current_state in ("dropped", "released"):
                        if expected_state != current_state:
                            raise PublishValidationError(
                                [
                                    f"Repository {repo_id} reached unexpected state "
                                    f"'{current_state}' while waiting for '{expected_state}'"
                                ]
                            )
                        return

            except urllib.error.HTTPError as exc:
                raise PublishValidationError(
                    [f"Nexus API HTTP {exc.code} while polling {status_url}: {exc.reason}"]
                )
            except urllib.error.URLError as exc:
                raise PublishValidationError(
                    [f"Network error polling Nexus at {status_url}: {exc.reason}"]
                )

            self.console.print(
                f"[yellow]  Repository state: '{current_state}' "
                f"(waited {elapsed}s / {max_wait}s)[/yellow]"
            )
            time.sleep(poll_interval)
            elapsed += poll_interval

        raise PublishValidationError(
            [
                f"Timed out after {max_wait}s waiting for repository "
                f"'{repo_id}' to reach state '{expected_state}'. "
                "Check the Nexus UI for details."
            ]
        )

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
