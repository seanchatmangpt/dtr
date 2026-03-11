"""Publish to AWS S3."""

import boto3
from pathlib import Path

from doctester_cli.model import PublishConfig, PublishResult
from doctester_cli.publishers.base_publisher import BasePublisher


class S3Publisher(BasePublisher):
    """Publish exports to AWS S3."""

    def publish(self, config: PublishConfig) -> PublishResult:
        """Publish to S3."""
        if not config.bucket:
            raise ValueError("S3 bucket required (--bucket)")

        # Create S3 client
        s3 = boto3.client("s3", region_name=config.region)

        files_uploaded = 0

        # Upload all files
        for file_path in config.export_path.rglob("*"):
            if not file_path.is_file():
                continue

            # Calculate S3 key
            rel_path = file_path.relative_to(config.export_path)
            key = f"{config.prefix.rstrip('/')}/{rel_path}".lstrip("/")

            # Upload file
            extra_args = {}
            if config.public:
                extra_args["ACL"] = "public-read"

            s3.upload_file(str(file_path), config.bucket, key, ExtraArgs=extra_args)
            files_uploaded += 1

        # Build URL
        url = f"s3://{config.bucket}/{config.prefix}"

        return PublishResult(
            platform="s3",
            url=url,
            files_count=files_uploaded,
        )
