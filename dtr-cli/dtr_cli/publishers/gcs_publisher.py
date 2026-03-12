"""Publish to Google Cloud Storage."""

from google.cloud import storage
from pathlib import Path

from dtr_cli.model import PublishConfig, PublishResult
from dtr_cli.publishers.base_publisher import BasePublisher


class GcsPublisher(BasePublisher):
    """Publish exports to Google Cloud Storage."""

    def publish(self, config: PublishConfig) -> PublishResult:
        """Publish to GCS."""
        if not config.bucket:
            raise ValueError("GCS bucket required (--bucket)")

        # Create GCS client
        client = storage.Client(project=config.project)
        bucket = client.bucket(config.bucket)

        files_uploaded = 0

        # Upload all files
        for file_path in config.export_path.rglob("*"):
            if not file_path.is_file():
                continue

            # Calculate GCS blob path
            rel_path = file_path.relative_to(config.export_path)
            blob_path = f"{config.prefix.rstrip('/')}/{rel_path}".lstrip("/")

            # Upload file
            blob = bucket.blob(blob_path)
            blob.upload_from_filename(str(file_path))
            files_uploaded += 1

        url = f"gs://{config.bucket}/{config.prefix}"

        return PublishResult(
            platform="gcs",
            url=url,
            files_count=files_uploaded,
        )
