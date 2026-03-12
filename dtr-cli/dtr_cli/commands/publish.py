"""Maven Central publishing commands.

Provides commands for:
- dtr publish check — Pre-flight validation
- dtr publish deploy — Deploy to OSSRH staging
- dtr publish release — Release to Maven Central
- dtr publish status — Check Maven Central availability
"""

from pathlib import Path
from typing import Optional

import typer
from rich.console import Console
from rich.table import Table

from dtr_cli.cli_errors import (
    CLIError,
    MavenCentralNotFoundError,
    PublishValidationError,
)
from dtr_cli.managers.maven_publish_manager import MavenPublishManager
from dtr_cli.model import (
    PublishCheckConfig,
    PublishDeployConfig,
    PublishReleaseConfig,
    PublishStatusConfig,
)

console = Console()
app = typer.Typer(help="Publish DocTester to Maven Central")


@app.command()
def check(
    project_dir: Path = typer.Option(
        Path.cwd(),
        "--project-dir",
        "-C",
        help="Project root directory",
    ),
    ossrh_user: Optional[str] = typer.Option(
        None,
        "--ossrh-user",
        envvar="OSSRH_USERNAME",
        help="Sonatype OSSRH username (env: OSSRH_USERNAME)",
    ),
    ossrh_token: Optional[str] = typer.Option(
        None,
        "--ossrh-token",
        envvar="OSSRH_PASSWORD",
        help="Sonatype OSSRH token (env: OSSRH_PASSWORD)",
    ),
    gpg_key: Optional[str] = typer.Option(
        None,
        "--gpg-key",
        help="GPG key ID to use for signing",
    ),
    verbose: bool = typer.Option(
        False,
        "--verbose",
        "-v",
        help="Detailed output",
    ),
) -> None:
    """Validate environment before publishing.

    Checks for:
    - Valid pom.xml with required Maven Central metadata
    - Correct groupId (io.github.seanchatmangpt)
    - Non-SNAPSHOT version
    - Distribution management configured
    - GPG key available
    - OSSRH credentials configured

    \b
    Examples:
        dtr publish check
        dtr publish check --project-dir /path/to/project
        dtr publish check --gpg-key ABCD1234
    """
    try:
        manager = MavenPublishManager(project_dir)
        config = PublishCheckConfig(
            project_dir=project_dir,
            ossrh_user=ossrh_user,
            ossrh_token=ossrh_token,
            gpg_key=gpg_key,
            verbose=verbose,
        )

        checks = manager.validate_environment(config)

        # Display results
        console.print("[bold cyan]Pre-flight Validation[/bold cyan]\n")

        for check in checks:
            icon = "✅" if check.passed else "❌"
            status = "[green]PASS[/green]" if check.passed else "[red]FAIL[/red]"
            console.print(f"{icon} {check.name}: {status}")
            if check.message:
                console.print(f"  {check.message}")
            if check.hint and not check.passed:
                console.print(f"  [cyan]💡 {check.hint}[/cyan]")

        # Summary
        passed = sum(1 for c in checks if c.passed)
        total = len(checks)
        console.print(f"\n[cyan]Result: {passed}/{total} checks passed[/cyan]")

        # Exit with error if any check failed
        if passed < total:
            raise typer.Exit(code=1)

    except CLIError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except Exception as e:
        console.print(f"[red]✗[/red] Validation failed: {e}")
        raise typer.Exit(1)


@app.command()
def deploy(
    project_dir: Path = typer.Option(
        Path.cwd(),
        "--project-dir",
        "-C",
        help="Project root directory",
    ),
    ossrh_user: Optional[str] = typer.Option(
        None,
        "--ossrh-user",
        envvar="OSSRH_USERNAME",
        help="Sonatype OSSRH username",
    ),
    ossrh_token: Optional[str] = typer.Option(
        None,
        "--ossrh-token",
        envvar="OSSRH_PASSWORD",
        help="Sonatype OSSRH token",
    ),
    gpg_key: Optional[str] = typer.Option(
        None,
        "--gpg-key",
        help="GPG key ID for signing",
    ),
    gpg_passphrase: Optional[str] = typer.Option(
        None,
        "--gpg-passphrase",
        envvar="GPG_PASSPHRASE",
        prompt=False,
        hide_input=True,
        help="GPG passphrase (will prompt if not set)",
    ),
    skip_tests: bool = typer.Option(
        False,
        "--skip-tests",
        help="Skip tests during build",
    ),
    dry_run: bool = typer.Option(
        False,
        "--dry-run",
        help="Show what would be deployed without actually deploying",
    ),
    auto_release: bool = typer.Option(
        False,
        "--auto-release",
        help="Automatically release after successful deploy",
    ),
) -> None:
    """Deploy to OSSRH staging repository.

    This command:
    1. Validates your environment (pom.xml, credentials, GPG key)
    2. Runs Maven clean deploy to OSSRH staging
    3. Returns staging repository ID for verification

    Next step: Review the staging repository, then run:
      dtr publish release <REPO_ID>

    \b
    Examples:
        dtr publish deploy
        dtr publish deploy --gpg-key ABCD1234
        dtr publish deploy --auto-release
        dtr publish deploy --dry-run  # Show what would happen
    """
    try:
        manager = MavenPublishManager(project_dir)

        # If GPG passphrase not provided, prompt for it
        if not gpg_passphrase and gpg_key:
            gpg_passphrase = typer.prompt(
                "GPG Passphrase", hide_input=True
            )

        config = PublishDeployConfig(
            project_dir=project_dir,
            ossrh_user=ossrh_user,
            ossrh_token=ossrh_token,
            gpg_key=gpg_key,
            gpg_passphrase=gpg_passphrase,
            skip_tests=skip_tests,
            dry_run=dry_run,
            auto_release=auto_release,
        )

        if dry_run:
            console.print("[yellow][DRY RUN][/yellow] Would deploy to OSSRH staging")
            console.print("Run without --dry-run to actually deploy")
            raise typer.Exit(0)

        with console.status("[cyan]Deploying to OSSRH staging repository...[/cyan]"):
            staging_repo_id = manager.deploy_to_staging(config)

        console.print("[green]✓[/green] Successfully deployed to OSSRH staging!")
        console.print(f"[cyan]Staging Repository ID: {staging_repo_id}[/cyan]")
        console.print(
            f"\n[yellow]Next steps:[/yellow]\n"
            f"1. Review the staging repository\n"
            f"2. Run: dtr publish release {staging_repo_id}\n"
            f"\n[cyan]Nexus UI: https://s01.oss.sonatype.org/#stagingRepositories[/cyan]"
        )

    except PublishValidationError as e:
        console.print(f"[red]✗[/red] Validation failed:")
        console.print(f"{e.format_message()}")
        raise typer.Exit(1)
    except CLIError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except Exception as e:
        console.print(f"[red]✗[/red] Deploy failed: {e}")
        raise typer.Exit(1)


@app.command()
def release(
    repo_id: str = typer.Argument(
        ...,
        help="Staging repository ID from dtr publish deploy",
    ),
    project_dir: Path = typer.Option(
        Path.cwd(),
        "--project-dir",
        "-C",
        help="Project root directory",
    ),
    ossrh_user: Optional[str] = typer.Option(
        None,
        "--ossrh-user",
        envvar="OSSRH_USERNAME",
        help="Sonatype OSSRH username",
    ),
    ossrh_token: Optional[str] = typer.Option(
        None,
        "--ossrh-token",
        envvar="OSSRH_PASSWORD",
        help="Sonatype OSSRH token",
    ),
    wait: bool = typer.Option(
        False,
        "--wait",
        help="Wait for Maven Central sync (15-30 minutes)",
    ),
    timeout: int = typer.Option(
        1800,
        "--timeout",
        "-t",
        help="Timeout in seconds when waiting for Maven Central",
    ),
) -> None:
    """Release staging repository to Maven Central.

    This command:
    1. Closes the staging repository (prepares for release)
    2. Releases to Maven Central
    3. Optionally waits for artifact to appear on Maven Central

    \b
    Examples:
        dtr publish release iogithubseanchatmangptdoctester-1234567890
        dtr publish release iogithubseanchatmangptdoctester-1234567890 --wait
        dtr publish release iogithubseanchatmangptdoctester-1234567890 --timeout 3600
    """
    try:
        manager = MavenPublishManager(project_dir)
        config = PublishReleaseConfig(
            ossrh_user=ossrh_user,
            ossrh_token=ossrh_token,
            wait=wait,
            timeout=timeout,
        )

        with console.status("[cyan]Releasing to Maven Central...[/cyan]"):
            artifact_url = manager.release_from_staging(repo_id, config)

        console.print("[green]✓[/green] Successfully released to Maven Central!")
        console.print(f"[cyan]Artifact URL: {artifact_url}[/cyan]")

        if wait:
            console.print(
                "\n[green]✓[/green] Artifact is now available on Maven Central!"
            )
        else:
            console.print(
                "\n[yellow]Note:[/yellow] Maven Central sync takes 15-30 minutes"
            )
            console.print("Run 'dtr publish status --wait' to check availability")

    except CLIError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except Exception as e:
        console.print(f"[red]✗[/red] Release failed: {e}")
        raise typer.Exit(1)


@app.command()
def status(
    project_dir: Path = typer.Option(
        Path.cwd(),
        "--project-dir",
        "-C",
        help="Project root directory (to extract version)",
    ),
    version: Optional[str] = typer.Option(
        None,
        "--version",
        "-v",
        help="Specific version to check (auto-detected from pom.xml)",
    ),
    wait: bool = typer.Option(
        False,
        "--wait",
        help="Poll Maven Central until artifact is available",
    ),
    timeout: int = typer.Option(
        1800,
        "--timeout",
        "-t",
        help="Timeout in seconds when polling Maven Central",
    ),
) -> None:
    """Check Maven Central availability.

    Queries Maven Central REST API to verify that your artifact
    is available and has all required files (JAR, sources, javadoc, signatures).

    \b
    Examples:
        dtr publish status
        dtr publish status --version 2.5.0
        dtr publish status --wait --timeout 3600
    """
    try:
        manager = MavenPublishManager(project_dir)
        config = PublishStatusConfig(
            version=version,
            wait=wait,
            timeout=timeout,
        )

        status_msg = (
            "[cyan]Checking Maven Central[/cyan]"
            if not wait
            else "[cyan]Waiting for Maven Central sync...[/cyan]"
        )
        with console.status(status_msg):
            result = manager.check_maven_central_status(config)

        # Display results
        console.print("\n[bold cyan]Maven Central Status[/bold cyan]")

        table = Table(show_header=False, box=None)
        table.add_row("[green]✓ Available", result["artifact"])
        table.add_row("[cyan]URL[/cyan]", result["url"])

        console.print(table)
        console.print(
            f"\n[cyan]Search: https://search.maven.org/artifact/{result['artifact'].replace(':', '/')}/jar[/cyan]"
        )

    except MavenCentralNotFoundError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        if not wait:
            console.print("[cyan]💡 Use --wait to poll Maven Central (may take 15-30 min)[/cyan]")
        raise typer.Exit(1)
    except CLIError as e:
        console.print(f"[red]✗[/red] {e.format_message()}")
        raise typer.Exit(1)
    except Exception as e:
        console.print(f"[red]✗[/red] Status check failed: {e}")
        raise typer.Exit(1)
