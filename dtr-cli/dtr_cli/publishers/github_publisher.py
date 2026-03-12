"""Publish to GitHub Pages."""

import os
import subprocess
from pathlib import Path

from dtr_cli.model import PublishConfig, PublishResult
from dtr_cli.publishers.base_publisher import BasePublisher


class GithubPublisher(BasePublisher):
    """Publish exports to GitHub Pages."""

    def publish(self, config: PublishConfig) -> PublishResult:
        """Publish to GitHub Pages."""
        if not config.repo:
            raise ValueError("GitHub repo required (--repo owner/repo)")

        # Get token from config or environment
        token = config.token or os.getenv("GITHUB_TOKEN")
        if not token:
            raise ValueError("GitHub token required (--token or GITHUB_TOKEN env var)")

        # Build remote URL with token
        owner, repo = config.repo.split("/")
        remote_url = f"https://{token}@github.com/{owner}/{repo}.git"

        # Clone or fetch repository
        temp_dir = Path("/tmp/doctester_github")
        if temp_dir.exists():
            subprocess.run(
                ["git", "-C", str(temp_dir), "fetch", "origin"],
                check=True,
                capture_output=True,
            )
        else:
            subprocess.run(
                ["git", "clone", "--depth", "1", "-b", config.branch, remote_url, str(temp_dir)],
                check=True,
                capture_output=True,
            )

        # Copy files
        target_dir = temp_dir
        for item in config.export_path.rglob("*"):
            if item.is_file():
                rel_path = item.relative_to(config.export_path)
                dest = target_dir / rel_path
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_bytes(item.read_bytes())

        # Commit and push
        subprocess.run(
            ["git", "-C", str(temp_dir), "add", "."],
            check=True,
            capture_output=True,
        )

        result = subprocess.run(
            [
                "git",
                "-C",
                str(temp_dir),
                "commit",
                "-m",
                "Update documentation",
            ],
            capture_output=True,
        )

        if result.returncode == 0:
            subprocess.run(
                ["git", "-C", str(temp_dir), "push", "origin", config.branch],
                check=True,
                capture_output=True,
            )

        # Cleanup
        import shutil

        shutil.rmtree(temp_dir, ignore_errors=True)

        url = f"https://{owner}.github.io/{repo}"

        return PublishResult(
            platform="github",
            url=url,
            files_count=len(list(config.export_path.rglob("*"))),
        )
