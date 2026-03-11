"""Publish DocTester exports to various platforms."""

from pathlib import Path
from typing import Optional
import typer
from rich.console import Console
from rich.progress import Progress

from doctester_cli.publishers.github_publisher import GithubPublisher
from doctester_cli.publishers.s3_publisher import S3Publisher
from doctester_cli.publishers.gcs_publisher import GcsPublisher
from doctester_cli.publishers.local_publisher import LocalPublisher
from doctester_cli.model import PublishConfig

console = Console()
app = typer.Typer(help="Publish exports to platforms")


@app.command()
def github_pages(
    export_dir: Path = typer.Argument(
        ...,
        help="Export directory to publish",
        exists=True,
    ),
    repo: str = typer.Option(
        ...,
        "--repo",
        "-r",
        help="GitHub repo (owner/repo)",
    ),
    branch: str = typer.Option(
        "gh-pages",
        "--branch",
        "-b",
        help="Target branch (default: gh-pages)",
    ),
    token: Optional[str] = typer.Option(
        None,
        "--token",
        "-t",
        help="GitHub token (or use GITHUB_TOKEN env var)",
    ),
) -> None:
    """
    Publish documentation to GitHub Pages.

    Pushes generated documentation to a GitHub Pages branch.

    \b
    Examples:
        doctester publish github-pages target/site/doctester --repo myorg/myrepo
        doctester publish github-pages target/site/doctester \\
            --repo myorg/myrepo --branch docs --token ghp_xxx
    """
    publisher = GithubPublisher()
    config = PublishConfig(
        export_path=export_dir,
        platform="github",
        target="pages",
        repo=repo,
        branch=branch,
        token=token,
    )

    with Progress() as progress:
        task = progress.add_task("[cyan]Publishing to GitHub Pages...", total=None)
        try:
            result = publisher.publish(config)
            progress.update(task, completed=True)
            console.print(f"[green]✓[/green] Published to {result.url}")
        except Exception as e:
            console.print(f"[red]✗[/red] Publishing failed: {e}")
            raise typer.Exit(1)


@app.command()
def s3(
    export_dir: Path = typer.Argument(
        ...,
        help="Export directory to publish",
        exists=True,
    ),
    bucket: str = typer.Option(
        ...,
        "--bucket",
        "-b",
        help="S3 bucket name",
    ),
    prefix: str = typer.Option(
        "docs/",
        "--prefix",
        "-p",
        help="S3 prefix/path (default: docs/)",
    ),
    region: str = typer.Option(
        "us-east-1",
        "--region",
        help="AWS region",
    ),
    public: bool = typer.Option(
        False,
        "--public",
        help="Make objects publicly readable",
    ),
) -> None:
    """
    Publish documentation to AWS S3.

    Uploads all exports to an S3 bucket with optional public access.

    \b
    Examples:
        doctester publish s3 target/site/doctester --bucket my-docs
        doctester publish s3 target/site/doctester \\
            --bucket my-docs --prefix api-docs/ --public
    """
    publisher = S3Publisher()
    config = PublishConfig(
        export_path=export_dir,
        platform="s3",
        bucket=bucket,
        prefix=prefix,
        region=region,
        public=public,
    )

    with Progress() as progress:
        task = progress.add_task("[cyan]Uploading to S3...", total=None)
        try:
            result = publisher.publish(config)
            progress.update(task, completed=True)
            url = f"s3://{bucket}/{prefix}"
            console.print(f"[green]✓[/green] Published to {url}")
            console.print(f"  Files uploaded: {result.files_count}")
            if public:
                console.print(f"  Access: [bold]public[/bold]")
        except Exception as e:
            console.print(f"[red]✗[/red] Publishing failed: {e}")
            raise typer.Exit(1)


@app.command()
def gcs(
    export_dir: Path = typer.Argument(
        ...,
        help="Export directory to publish",
        exists=True,
    ),
    bucket: str = typer.Option(
        ...,
        "--bucket",
        "-b",
        help="GCS bucket name",
    ),
    prefix: str = typer.Option(
        "docs/",
        "--prefix",
        "-p",
        help="GCS prefix/path (default: docs/)",
    ),
    project: Optional[str] = typer.Option(
        None,
        "--project",
        help="GCP project ID",
    ),
) -> None:
    """
    Publish documentation to Google Cloud Storage.

    Uploads exports to a GCS bucket with proper configuration.

    \b
    Examples:
        doctester publish gcs target/site/doctester --bucket my-docs
        doctester publish gcs target/site/doctester \\
            --bucket my-docs --project my-gcp-project --prefix api-docs/
    """
    publisher = GcsPublisher()
    config = PublishConfig(
        export_path=export_dir,
        platform="gcs",
        bucket=bucket,
        prefix=prefix,
        project=project,
    )

    with Progress() as progress:
        task = progress.add_task("[cyan]Uploading to GCS...", total=None)
        try:
            result = publisher.publish(config)
            progress.update(task, completed=True)
            console.print(f"[green]✓[/green] Published to gs://{bucket}/{prefix}")
            console.print(f"  Files uploaded: {result.files_count}")
        except Exception as e:
            console.print(f"[red]✗[/red] Publishing failed: {e}")
            raise typer.Exit(1)


@app.command()
def local(
    export_dir: Path = typer.Argument(
        ...,
        help="Export directory to publish",
        exists=True,
    ),
    target_dir: Path = typer.Option(
        ...,
        "--target",
        "-t",
        help="Target directory for publishing",
    ),
) -> None:
    """
    Publish documentation to a local directory.

    Copies exports to another directory on the filesystem (useful for CI/CD).

    \b
    Examples:
        doctester publish local target/site/doctester --target ./docs
        doctester publish local target/site/doctester --target /var/www/html
    """
    publisher = LocalPublisher()
    config = PublishConfig(
        export_path=export_dir,
        platform="local",
        target_path=target_dir,
    )

    with Progress() as progress:
        task = progress.add_task("[cyan]Publishing to local directory...", total=None)
        try:
            result = publisher.publish(config)
            progress.update(task, completed=True)
            console.print(f"[green]✓[/green] Published to {target_dir}")
            console.print(f"  Files copied: {result.files_count}")
        except Exception as e:
            console.print(f"[red]✗[/red] Publishing failed: {e}")
            raise typer.Exit(1)


@app.command()
def list_platforms() -> None:
    """List supported publishing platforms."""
    console.print("[bold cyan]Supported Publishing Platforms[/bold cyan]")
    console.print("""
[bold]GitHub Pages[/bold]
  doctester publish github-pages <dir> --repo owner/repo
  - Deploys to gh-pages branch
  - Requires GITHUB_TOKEN

[bold]AWS S3[/bold]
  doctester publish s3 <dir> --bucket my-bucket
  - Static website hosting
  - Optional public access
  - Requires AWS credentials

[bold]Google Cloud Storage[/bold]
  doctester publish gcs <dir> --bucket my-bucket
  - Cloud-hosted documentation
  - Fine-grained access control
  - Requires GCP authentication

[bold]Local Directory[/bold]
  doctester publish local <dir> --target /path/to/docs
  - Copy to another directory
  - Useful for CI/CD pipelines
  - No credentials required
""")
