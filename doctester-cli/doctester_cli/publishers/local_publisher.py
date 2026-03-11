"""Publish to local directory."""

import shutil
from pathlib import Path

from doctester_cli.model import PublishConfig, PublishResult
from doctester_cli.publishers.base_publisher import BasePublisher


class LocalPublisher(BasePublisher):
    """Publish exports to a local directory."""

    def publish(self, config: PublishConfig) -> PublishResult:
        """Publish to local directory."""
        if not config.target_path:
            raise ValueError("Target directory required (--target)")

        target = Path(config.target_path)
        target.mkdir(parents=True, exist_ok=True)

        files_copied = 0

        # Copy all files
        for file_path in config.export_path.rglob("*"):
            if not file_path.is_file():
                continue

            rel_path = file_path.relative_to(config.export_path)
            dest = target / rel_path
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(file_path, dest)
            files_copied += 1

        return PublishResult(
            platform="local",
            url=str(target),
            files_count=files_copied,
        )
