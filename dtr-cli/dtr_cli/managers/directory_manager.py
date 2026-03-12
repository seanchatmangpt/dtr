"""Manage export directories."""

import shutil
import tarfile
import zipfile
from pathlib import Path
from bs4 import BeautifulSoup

from dtr_cli.model import ManageConfig, ManageResult, ExportInfo


class DirectoryManager:
    """Manage DocTester export directories."""

    def list_exports(self, config: ManageConfig) -> list[ExportInfo]:
        """List all exports in a directory."""
        if not config.export_path.exists():
            return []

        exports = []
        for html_file in config.export_path.glob("*.html"):
            if html_file.name == "index.html":
                continue

            stat = html_file.stat()
            from datetime import datetime

            modified = datetime.fromtimestamp(stat.st_mtime).isoformat()

            exports.append(
                ExportInfo(
                    name=html_file.stem,
                    path=html_file,
                    size=stat.st_size,
                    modified=modified,
                )
            )

        return sorted(exports, key=lambda e: e.modified, reverse=True)

    def archive_exports(self, config: ManageConfig) -> ManageResult:
        """Archive exports directory."""
        if not config.export_path.exists():
            raise FileNotFoundError(f"Export directory not found: {config.export_path}")

        if config.archive_format == "zip":
            self._create_zip_archive(config.export_path, config.archive_path)
        else:  # tar.gz
            self._create_tar_archive(config.export_path, config.archive_path)

        return ManageResult(stats={"archived_path": str(config.archive_path)})

    def cleanup_exports(self, config: ManageConfig) -> ManageResult:
        """Clean up old exports, keeping latest versions."""
        if not config.export_path.exists():
            raise FileNotFoundError(f"Export directory not found: {config.export_path}")

        exports = self.list_exports(config)
        removed = []

        # Keep the latest N exports, remove older ones
        for export in exports[config.keep_latest :]:
            if not config.dry_run:
                export.path.unlink()
            removed.append(export.name)

        return ManageResult(removed_files=removed)

    def validate_exports(self, config: ManageConfig) -> ManageResult:
        """Validate export integrity."""
        if not config.export_path.exists():
            raise FileNotFoundError(f"Export directory not found: {config.export_path}")

        issues = []
        files_checked = 0
        valid_files = 0

        for html_file in config.export_path.glob("*.html"):
            files_checked += 1

            try:
                content = html_file.read_text(encoding="utf-8")
                soup = BeautifulSoup(content, "html.parser")

                # Check for basic HTML structure
                if not soup.find("html"):
                    issues.append(f"Missing HTML tag in {html_file.name}")
                    continue

                # Check for broken links
                for link in soup.find_all("a"):
                    href = link.get("href")
                    if href and not href.startswith(("http", "#", "/")):
                        target = config.export_path / href
                        if not target.exists():
                            issues.append(f"Broken link in {html_file.name}: {href}")

                valid_files += 1
            except Exception as e:
                issues.append(f"Error parsing {html_file.name}: {str(e)}")

        return ManageResult(
            stats={
                "files_checked": files_checked,
                "valid_files": valid_files,
                "issues_found": len(issues),
            },
            issues=issues,
        )

    @staticmethod
    def _create_zip_archive(source_dir: Path, archive_path: Path) -> None:
        """Create ZIP archive."""
        with zipfile.ZipFile(archive_path, "w", zipfile.ZIP_DEFLATED) as zf:
            for file in source_dir.rglob("*"):
                if file.is_file():
                    arcname = file.relative_to(source_dir.parent)
                    zf.write(file, arcname)

    @staticmethod
    def _create_tar_archive(source_dir: Path, archive_path: Path) -> None:
        """Create TAR.GZ archive."""
        with tarfile.open(archive_path, "w:gz") as tf:
            tf.add(source_dir, arcname=source_dir.name)
