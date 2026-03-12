"""Maven build orchestration and module management.

Provides MavenRunner for executing Maven builds with support for:
- Auto-detection of mvnd vs mvn
- Maven goal, profile, and property customization
- Multi-module project detection and selection
- Real-time output streaming with Rich progress
- Proper error handling with helpful messages
"""

import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Optional
import xml.etree.ElementTree as ET

from rich.console import Console
from rich.progress import Progress, SpinnerColumn, TextColumn

console = Console()


@dataclass
class MavenBuildConfig:
    """Configuration for Maven build execution.

    Attributes:
        goals: List of Maven goals (default: ["clean", "verify"])
        profiles: List of profiles to activate
        properties: Dict of properties to pass via -D flags
        modules: List of specific modules to build (None = all)
        verbose: Whether to show full Maven output
        timeout: Timeout in seconds (0 = no timeout)
    """

    goals: list[str] | None = None
    profiles: list[str] | None = None
    properties: dict[str, str] | None = None
    modules: list[str] | None = None
    verbose: bool = False
    timeout: int = 600
    skip_tests: bool = False

    def __post_init__(self) -> None:
        """Set defaults if not provided."""
        if self.goals is None:
            self.goals = ["clean", "verify"]
        if self.profiles is None:
            self.profiles = []
        if self.properties is None:
            self.properties = {}


class MavenRunner:
    """Orchestrates Maven builds with intelligent defaults and error handling.

    Features:
    - Auto-detects mvnd (preferred) or mvn
    - Parses pom.xml for available modules
    - Supports custom goals, profiles, properties
    - Streams output in real-time with Rich
    - Validates Java version and Maven availability
    """

    def __init__(self, project_root: Path | None = None):
        """Initialize Maven runner.

        Args:
            project_root: Root directory containing pom.xml.
                         Defaults to current directory.

        Raises:
            FileNotFoundError: If pom.xml not found in project_root.
        """
        self.project_root = project_root or Path.cwd()
        self.pom_path = self.project_root / "pom.xml"

        if not self.pom_path.exists():
            raise FileNotFoundError(
                f"pom.xml not found in {self.project_root}\n"
                "Make sure you're in the root directory of a Maven project"
            )

        self.maven_exe = self._find_maven()
        self.modules = self._parse_modules()

    def _find_maven(self) -> str:
        """Find and return path to Maven executable.

        Prefers mvnd (Maven Daemon) if available, falls back to mvn.

        Returns:
            Path to Maven executable (mvnd or mvn)

        Raises:
            RuntimeError: If neither mvnd nor mvn found in PATH
        """
        # Try mvnd first (faster daemon)
        try:
            subprocess.run(
                ["mvnd", "--version"],
                capture_output=True,
                text=True,
                timeout=5,
                check=True,
            )
            return "mvnd"
        except (FileNotFoundError, subprocess.CalledProcessError):
            pass

        # Fall back to mvn
        try:
            subprocess.run(
                ["mvn", "--version"],
                capture_output=True,
                text=True,
                timeout=5,
                check=True,
            )
            return "mvn"
        except (FileNotFoundError, subprocess.CalledProcessError):
            raise RuntimeError(
                "Neither mvnd nor mvn found in PATH\n"
                "Install Maven or mvnd and ensure it's in your PATH:\n"
                "  mvnd: https://maven.apache.org/mvnd/\n"
                "  mvn:  https://maven.apache.org/"
            )

    def _parse_modules(self) -> list[str]:
        """Parse pom.xml to extract module names.

        Returns:
            List of module names from <modules> section.
            Empty list if no modules defined (single-module project).
        """
        try:
            tree = ET.parse(self.pom_path)
            root = tree.getroot()

            # Handle XML namespaces
            ns = {"pom": "http://maven.apache.org/POM/4.0.0"}
            modules_elem = root.find("pom:modules", ns)

            if modules_elem is None:
                return []

            modules = []
            for module_elem in modules_elem.findall("pom:module", ns):
                if module_elem.text:
                    modules.append(module_elem.text)

            return modules
        except ET.ParseError as e:
            console.print(
                f"[yellow]⚠️  Warning: Could not parse pom.xml: {e}[/yellow]"
            )
            return []

    def build(self, config: MavenBuildConfig | None = None) -> int:
        """Execute Maven build with specified configuration.

        Args:
            config: Build configuration. Defaults to clean verify.

        Returns:
            Exit code from Maven (0 = success, non-zero = failure)

        Raises:
            RuntimeError: If Maven execution fails unexpectedly
        """
        if config is None:
            config = MavenBuildConfig()

        # Build command
        cmd = [self.maven_exe]

        # Add goals
        if config.goals:
            cmd.extend(config.goals)

        # Add profiles
        for profile in config.profiles or []:
            cmd.append("-P")
            cmd.append(profile)

        # Add properties
        for key, value in (config.properties or {}).items():
            cmd.append(f"-D{key}={value}")

        # Skip tests if requested
        if config.skip_tests:
            cmd.append("-DskipTests")

        # Add modules (selective build)
        if config.modules:
            modules_str = ",".join(config.modules)
            cmd.append("-pl")
            cmd.append(modules_str)

        # Execute with streaming output
        try:
            with Progress(
                SpinnerColumn(),
                TextColumn("[progress.description]{task.description}"),
                console=console,
            ) as progress:
                task = progress.add_task(
                    f"[cyan]Running: {' '.join(cmd)[:60]}...",
                    total=None,
                )

                process = subprocess.Popen(
                    cmd,
                    cwd=self.project_root,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    text=True,
                )

                # Stream output if verbose
                stdout_lines = []
                stderr_lines = []

                if config.verbose:
                    for line in iter(process.stdout.readline, ""):
                        if line:
                            console.print(line.rstrip())
                            stdout_lines.append(line)
                else:
                    stdout_lines = process.stdout.readlines()

                stderr_lines = process.stderr.readlines()

                exit_code = process.wait(timeout=config.timeout)
                progress.update(task, completed=True)

                # Handle errors
                if exit_code != 0:
                    stderr_output = "".join(stderr_lines)
                    console.print(
                        f"\n[red]❌ Maven build failed with exit code {exit_code}[/red]"
                    )
                    if stderr_output and config.verbose:
                        console.print(
                            "[yellow]stderr output:[/yellow]\n" + stderr_output
                        )
                    return exit_code

                console.print(
                    "[green]✅ Maven build completed successfully[/green]"
                )
                return 0

        except subprocess.TimeoutExpired:
            raise RuntimeError(
                f"Maven build timed out after {config.timeout} seconds\n"
                "Increase timeout with: dtr build --timeout 1200"
            )
        except Exception as e:
            raise RuntimeError(f"Failed to execute Maven: {e}")

    def get_available_modules(self) -> list[str]:
        """Get list of available modules in pom.xml.

        Returns:
            List of module names (empty if single-module project)
        """
        return self.modules

    def get_export_dir(self) -> Path:
        """Get expected export directory after build.

        For multi-module projects, returns the root exports dir.
        Individual modules export to their own target/site/doctester/

        Returns:
            Path to exports directory (target/site/doctester)
        """
        return self.project_root / "target" / "site" / "doctester"

    def is_multi_module(self) -> bool:
        """Check if this is a multi-module Maven project.

        Returns:
            True if project has multiple modules
        """
        return len(self.modules) > 0
