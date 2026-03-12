"""Publish DocTester exports to various platforms."""

from pathlib import Path
from typing import Optional
import typer
from rich.console import Console
from rich.progress import Progress

from dtr_cli.publishers.github_publisher import GithubPublisher
from dtr_cli.publishers.s3_publisher import S3Publisher
from dtr_cli.publishers.gcs_publisher import GcsPublisher
from dtr_cli.publishers.local_publisher import LocalPublisher
from dtr_cli.model import PublishConfig

console = Console()
app = typer.Typer(help="Publish exports to platforms")


@app.command()
def gh(
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
        dtr push gh target/site/doctester --repo myorg/myrepo
        dtr push gh target/site/doctester \\
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
        dtr push s3 target/site/doctester --bucket my-docs
        dtr push s3 target/site/doctester \\
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
        dtr push gcs target/site/doctester --bucket my-docs
        dtr push gcs target/site/doctester \\
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
        dtr push local target/site/doctester --target ./docs
        dtr push local target/site/doctester --target /var/www/html
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
