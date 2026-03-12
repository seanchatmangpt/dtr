"""Chicago-style TDD tests for managers module.

Tests real behavior with real collaborators -- no mocks.
Uses real temp directories, real file I/O, and real pom.xml files.
"""

import tarfile
import time
import zipfile
from pathlib import Path

import pytest

from dtr_cli.managers.directory_manager import DirectoryManager
from dtr_cli.managers.latex_manager import LatexManager
from dtr_cli.managers.maven_manager import MavenBuildConfig, MavenRunner
from dtr_cli.model import ManageConfig, ManageResult, ExportInfo


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _create_html_file(directory: Path, name: str, content: str | None = None) -> Path:
    """Create an HTML file in the given directory."""
    html = content or f"<html><body><h1>{name}</h1></body></html>"
    path = directory / f"{name}.html"
    path.write_text(html, encoding="utf-8")
    return path


def _create_valid_pom(directory: Path, modules: list[str] | None = None) -> Path:
    """Create a real pom.xml in the given directory."""
    modules_xml = ""
    if modules:
        module_entries = "\n".join(f"        <module>{m}</module>" for m in modules)
        modules_xml = f"""
    <modules>
{module_entries}
    </modules>"""

    pom_content = f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>{modules_xml}
</project>
"""
    pom_path = directory / "pom.xml"
    pom_path.write_text(pom_content, encoding="utf-8")
    return pom_path


# ===========================================================================
# DirectoryManager.list_exports
# ===========================================================================

class TestDirectoryManagerListExports:
    """Test listing exports from a real directory."""

    def test_list_exports_empty_directory(self, tmp_path: Path) -> None:
        """Empty directory returns empty list."""
        dm = DirectoryManager()
        config = ManageConfig(export_path=tmp_path)
        result = dm.list_exports(config)
        assert result == []

    def test_list_exports_nonexistent_directory(self, tmp_path: Path) -> None:
        """Non-existent directory returns empty list."""
        dm = DirectoryManager()
        config = ManageConfig(export_path=tmp_path / "does-not-exist")
        result = dm.list_exports(config)
        assert result == []

    def test_list_exports_returns_export_info_objects(self, tmp_path: Path) -> None:
        """Each entry is a proper ExportInfo with correct fields."""
        _create_html_file(tmp_path, "alpha")
        dm = DirectoryManager()
        config = ManageConfig(export_path=tmp_path)

        result = dm.list_exports(config)

        assert len(result) == 1
        info = result[0]
        assert isinstance(info, ExportInfo)
        assert info.name == "alpha"
        assert info.path == tmp_path / "alpha.html"
        assert info.size > 0
        assert info.modified  # non-empty ISO timestamp

    def test_list_exports_sorted_by_modification_time(self, tmp_path: Path) -> None:
        """Exports are sorted newest-first by modification time."""
        _create_html_file(tmp_path, "old")
        time.sleep(0.05)
        _create_html_file(tmp_path, "mid")
        time.sleep(0.05)
        _create_html_file(tmp_path, "new")

        dm = DirectoryManager()
        config = ManageConfig(export_path=tmp_path)
        result = dm.list_exports(config)

        names = [e.name for e in result]
        assert names == ["new", "mid", "old"]

    def test_list_exports_skips_index_html(self, tmp_path: Path) -> None:
        """index.html is excluded from the listing."""
        _create_html_file(tmp_path, "index")
        _create_html_file(tmp_path, "report")

        dm = DirectoryManager()
        config = ManageConfig(export_path=tmp_path)
        result = dm.list_exports(config)

        names = [e.name for e in result]
        assert "index" not in names
        assert "report" in names

    def test_list_exports_ignores_non_html_files(self, tmp_path: Path) -> None:
        """Only .html files are listed; others are ignored."""
        _create_html_file(tmp_path, "report")
        (tmp_path / "data.json").write_text("{}")
        (tmp_path / "notes.txt").write_text("hello")

        dm = DirectoryManager()
        config = ManageConfig(export_path=tmp_path)
        result = dm.list_exports(config)

        assert len(result) == 1
        assert result[0].name == "report"

    def test_list_exports_reports_correct_file_size(self, tmp_path: Path) -> None:
        """ExportInfo.size matches the actual file size on disk."""
        content = "<html><body>" + "x" * 500 + "</body></html>"
        path = _create_html_file(tmp_path, "sized", content)

        dm = DirectoryManager()
        config = ManageConfig(export_path=tmp_path)
        result = dm.list_exports(config)

        assert result[0].size == path.stat().st_size


# ===========================================================================
# DirectoryManager.archive_exports
# ===========================================================================

class TestDirectoryManagerArchiveExports:
    """Test archiving exports into tar.gz and zip."""

    def test_archive_tar_gz_creates_file(self, tmp_path: Path) -> None:
        """tar.gz archive is created and contains the expected files."""
        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        _create_html_file(export_dir, "doc1")
        _create_html_file(export_dir, "doc2")

        archive_path = tmp_path / "backup.tar.gz"
        config = ManageConfig(
            export_path=export_dir,
            archive_path=archive_path,
            archive_format="tar.gz",
        )

        dm = DirectoryManager()
        result = dm.archive_exports(config)

        assert archive_path.exists()
        assert isinstance(result, ManageResult)
        assert result.stats["archived_path"] == str(archive_path)

        with tarfile.open(archive_path, "r:gz") as tf:
            member_names = tf.getnames()
            assert any("doc1.html" in n for n in member_names)
            assert any("doc2.html" in n for n in member_names)

    def test_archive_zip_creates_file(self, tmp_path: Path) -> None:
        """zip archive is created and contains the expected files."""
        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        _create_html_file(export_dir, "report")

        archive_path = tmp_path / "backup.zip"
        config = ManageConfig(
            export_path=export_dir,
            archive_path=archive_path,
            archive_format="zip",
        )

        dm = DirectoryManager()
        result = dm.archive_exports(config)

        assert archive_path.exists()
        assert isinstance(result, ManageResult)

        with zipfile.ZipFile(archive_path, "r") as zf:
            names = zf.namelist()
            assert any("report.html" in n for n in names)

    def test_archive_nonexistent_directory_raises(self, tmp_path: Path) -> None:
        """Archiving a non-existent directory raises FileNotFoundError."""
        config = ManageConfig(
            export_path=tmp_path / "nope",
            archive_path=tmp_path / "archive.tar.gz",
        )
        dm = DirectoryManager()
        with pytest.raises(FileNotFoundError):
            dm.archive_exports(config)

    def test_archive_includes_nested_files(self, tmp_path: Path) -> None:
        """Archive includes files in subdirectories."""
        export_dir = tmp_path / "exports"
        sub = export_dir / "images"
        sub.mkdir(parents=True)
        _create_html_file(export_dir, "main")
        (sub / "logo.png").write_bytes(b"\x89PNG")

        archive_path = tmp_path / "full.tar.gz"
        config = ManageConfig(
            export_path=export_dir,
            archive_path=archive_path,
            archive_format="tar.gz",
        )

        dm = DirectoryManager()
        dm.archive_exports(config)

        with tarfile.open(archive_path, "r:gz") as tf:
            member_names = tf.getnames()
            assert any("logo.png" in n for n in member_names)
            assert any("main.html" in n for n in member_names)


# ===========================================================================
# DirectoryManager.cleanup_exports
# ===========================================================================

class TestDirectoryManagerCleanupExports:
    """Test cleaning up old exports."""

    def test_cleanup_dry_run_does_not_delete(self, tmp_path: Path) -> None:
        """Dry run reports files to remove but does not delete them."""
        for i in range(7):
            _create_html_file(tmp_path, f"doc{i:02d}")
            time.sleep(0.02)

        config = ManageConfig(export_path=tmp_path, keep_latest=5, dry_run=True)
        dm = DirectoryManager()
        result = dm.cleanup_exports(config)

        # Should identify 2 files for removal (7 - 5 = 2)
        assert len(result.removed_files) == 2

        # All 7 files still exist because dry_run=True
        html_files = list(tmp_path.glob("*.html"))
        assert len(html_files) == 7

    def test_cleanup_actual_deletion(self, tmp_path: Path) -> None:
        """With dry_run=False, old files are actually deleted."""
        for i in range(7):
            _create_html_file(tmp_path, f"doc{i:02d}")
            time.sleep(0.02)

        config = ManageConfig(export_path=tmp_path, keep_latest=5, dry_run=False)
        dm = DirectoryManager()
        result = dm.cleanup_exports(config)

        assert len(result.removed_files) == 2
        html_files = list(tmp_path.glob("*.html"))
        assert len(html_files) == 5

    def test_cleanup_keeps_newest_files(self, tmp_path: Path) -> None:
        """The newest N files survive cleanup; oldest are removed."""
        created_names = []
        for i in range(5):
            name = f"doc{i:02d}"
            _create_html_file(tmp_path, name)
            created_names.append(name)
            time.sleep(0.02)

        config = ManageConfig(export_path=tmp_path, keep_latest=3, dry_run=False)
        dm = DirectoryManager()
        result = dm.cleanup_exports(config)

        # The two oldest should be removed
        assert len(result.removed_files) == 2
        surviving = {p.stem for p in tmp_path.glob("*.html")}
        # doc02, doc03, doc04 should survive (newest 3)
        assert "doc02" in surviving
        assert "doc03" in surviving
        assert "doc04" in surviving

    def test_cleanup_fewer_than_keep_latest_removes_nothing(self, tmp_path: Path) -> None:
        """When file count <= keep_latest, nothing is removed."""
        _create_html_file(tmp_path, "only_one")

        config = ManageConfig(export_path=tmp_path, keep_latest=5, dry_run=False)
        dm = DirectoryManager()
        result = dm.cleanup_exports(config)

        assert result.removed_files == []
        assert (tmp_path / "only_one.html").exists()

    def test_cleanup_nonexistent_directory_raises(self, tmp_path: Path) -> None:
        """Cleanup on non-existent directory raises FileNotFoundError."""
        config = ManageConfig(export_path=tmp_path / "nope", keep_latest=5)
        dm = DirectoryManager()
        with pytest.raises(FileNotFoundError):
            dm.cleanup_exports(config)


# ===========================================================================
# DirectoryManager.validate_exports
# ===========================================================================

class TestDirectoryManagerValidateExports:
    """Test validation of HTML export integrity."""

    def test_validate_valid_html(self, tmp_path: Path) -> None:
        """Well-formed HTML passes validation."""
        _create_html_file(tmp_path, "good", "<html><body><p>OK</p></body></html>")

        config = ManageConfig(export_path=tmp_path)
        dm = DirectoryManager()
        result = dm.validate_exports(config)

        assert result.stats["files_checked"] == 1
        assert result.stats["valid_files"] == 1
        assert result.stats["issues_found"] == 0
        assert result.issues == []

    def test_validate_missing_html_tag(self, tmp_path: Path) -> None:
        """HTML without <html> tag is flagged."""
        (tmp_path / "bad.html").write_text(
            "<body><p>No html tag</p></body>", encoding="utf-8"
        )

        config = ManageConfig(export_path=tmp_path)
        dm = DirectoryManager()
        result = dm.validate_exports(config)

        assert result.stats["issues_found"] == 1
        assert any("Missing HTML tag" in issue for issue in result.issues)
        assert result.stats["valid_files"] == 0

    def test_validate_broken_local_link(self, tmp_path: Path) -> None:
        """Broken relative link (file does not exist) is flagged."""
        html = '<html><body><a href="missing.html">Link</a></body></html>'
        _create_html_file(tmp_path, "linker", html)

        config = ManageConfig(export_path=tmp_path)
        dm = DirectoryManager()
        result = dm.validate_exports(config)

        assert result.stats["issues_found"] == 1
        assert any("Broken link" in i for i in result.issues)
        assert "missing.html" in result.issues[0]

    def test_validate_valid_local_link(self, tmp_path: Path) -> None:
        """Relative link to existing file passes validation."""
        _create_html_file(tmp_path, "target", "<html><body>Target</body></html>")
        html = '<html><body><a href="target.html">Link</a></body></html>'
        _create_html_file(tmp_path, "linker", html)

        config = ManageConfig(export_path=tmp_path)
        dm = DirectoryManager()
        result = dm.validate_exports(config)

        assert result.stats["issues_found"] == 0

    def test_validate_external_links_not_flagged(self, tmp_path: Path) -> None:
        """HTTP(S) links and anchors are not flagged as broken."""
        html = (
            '<html><body>'
            '<a href="https://example.com">External</a>'
            '<a href="http://example.com">HTTP</a>'
            '<a href="#section">Anchor</a>'
            '<a href="/absolute">Absolute</a>'
            '</body></html>'
        )
        _create_html_file(tmp_path, "links", html)

        config = ManageConfig(export_path=tmp_path)
        dm = DirectoryManager()
        result = dm.validate_exports(config)

        assert result.issues == []

    def test_validate_multiple_files_mixed(self, tmp_path: Path) -> None:
        """Validation across multiple files, some valid, some not."""
        _create_html_file(tmp_path, "good", "<html><body>OK</body></html>")
        (tmp_path / "bad.html").write_text("<p>No html tag</p>", encoding="utf-8")
        _create_html_file(
            tmp_path, "broken",
            '<html><body><a href="ghost.html">Dead</a></body></html>'
        )

        config = ManageConfig(export_path=tmp_path)
        dm = DirectoryManager()
        result = dm.validate_exports(config)

        assert result.stats["files_checked"] == 3
        # bad.html: missing html tag; broken.html: broken link
        assert result.stats["issues_found"] == 2
        # valid_files counts files that have an <html> tag (parsing succeeds),
        # even if they contain broken links -- "good" and "broken" both qualify.
        assert result.stats["valid_files"] == 2

    def test_validate_nonexistent_directory_raises(self, tmp_path: Path) -> None:
        """Validation on non-existent directory raises FileNotFoundError."""
        config = ManageConfig(export_path=tmp_path / "nope")
        dm = DirectoryManager()
        with pytest.raises(FileNotFoundError):
            dm.validate_exports(config)


# ===========================================================================
# MavenBuildConfig
# ===========================================================================

class TestMavenBuildConfig:
    """Test MavenBuildConfig dataclass and post_init defaults."""

    def test_default_goals(self) -> None:
        """Default goals are ['clean', 'verify']."""
        config = MavenBuildConfig()
        assert config.goals == ["clean", "verify"]

    def test_default_profiles_empty_list(self) -> None:
        """Profiles default to empty list after post_init."""
        config = MavenBuildConfig()
        assert config.profiles == []

    def test_default_properties_empty_dict(self) -> None:
        """Properties default to empty dict after post_init."""
        config = MavenBuildConfig()
        assert config.properties == {}

    def test_default_timeout(self) -> None:
        """Default timeout is 600 seconds."""
        config = MavenBuildConfig()
        assert config.timeout == 600

    def test_default_skip_tests_false(self) -> None:
        """skip_tests defaults to False."""
        config = MavenBuildConfig()
        assert config.skip_tests is False

    def test_default_verbose_false(self) -> None:
        """verbose defaults to False."""
        config = MavenBuildConfig()
        assert config.verbose is False

    def test_custom_goals_preserved(self) -> None:
        """Explicit goals are not overridden by post_init."""
        config = MavenBuildConfig(goals=["install"])
        assert config.goals == ["install"]

    def test_custom_profiles_preserved(self) -> None:
        """Explicit profiles are not overridden by post_init."""
        config = MavenBuildConfig(profiles=["fast"])
        assert config.profiles == ["fast"]

    def test_custom_properties_preserved(self) -> None:
        """Explicit properties are not overridden by post_init."""
        config = MavenBuildConfig(properties={"skipTests": "true"})
        assert config.properties == {"skipTests": "true"}

    def test_modules_default_none_stays_none(self) -> None:
        """modules=None is not changed by post_init (None means 'all')."""
        config = MavenBuildConfig()
        assert config.modules is None


# ===========================================================================
# MavenRunner.__init__
# ===========================================================================

class TestMavenRunnerInit:
    """Test MavenRunner initialization with real pom.xml files."""

    def test_no_pom_raises_file_not_found(self, tmp_path: Path) -> None:
        """MavenRunner raises FileNotFoundError when pom.xml is missing."""
        with pytest.raises(FileNotFoundError, match="pom.xml not found"):
            MavenRunner(project_root=tmp_path)

    def test_single_module_pom(self, tmp_path: Path) -> None:
        """Single-module pom.xml results in empty modules list."""
        _create_valid_pom(tmp_path)
        runner = MavenRunner(project_root=tmp_path)

        assert runner.modules == []
        assert runner.is_multi_module() is False

    def test_multi_module_pom(self, tmp_path: Path) -> None:
        """Multi-module pom.xml parses all module names."""
        modules = ["core", "api", "integration-test"]
        _create_valid_pom(tmp_path, modules=modules)
        runner = MavenRunner(project_root=tmp_path)

        assert runner.modules == modules
        assert runner.is_multi_module() is True

    def test_get_available_modules(self, tmp_path: Path) -> None:
        """get_available_modules returns the parsed modules."""
        modules = ["module-a", "module-b"]
        _create_valid_pom(tmp_path, modules=modules)
        runner = MavenRunner(project_root=tmp_path)

        assert runner.get_available_modules() == modules

    def test_get_export_dir(self, tmp_path: Path) -> None:
        """get_export_dir returns the expected path."""
        _create_valid_pom(tmp_path)
        runner = MavenRunner(project_root=tmp_path)

        expected = tmp_path / "target" / "site" / "dtr"
        assert runner.get_export_dir() == expected

    def test_pom_path_attribute(self, tmp_path: Path) -> None:
        """pom_path points to the actual pom.xml file."""
        _create_valid_pom(tmp_path)
        runner = MavenRunner(project_root=tmp_path)

        assert runner.pom_path == tmp_path / "pom.xml"
        assert runner.pom_path.exists()

    def test_malformed_pom_returns_empty_modules(self, tmp_path: Path) -> None:
        """Malformed pom.xml does not crash; returns empty modules."""
        pom_path = tmp_path / "pom.xml"
        pom_path.write_text("this is not xml at all", encoding="utf-8")
        runner = MavenRunner(project_root=tmp_path)

        assert runner.modules == []


# ===========================================================================
# LatexManager
# ===========================================================================

class TestLatexManager:
    """Test LatexManager wiring of LatexConverter and PdfConverter."""

    def test_init_wires_converters(self) -> None:
        """LatexManager.__init__ creates real LatexConverter and PdfConverter."""
        from dtr_cli.converters.latex_converter import LatexConverter
        from dtr_cli.converters.pdf_converter import PdfConverter

        manager = LatexManager()

        assert isinstance(manager.latex_converter, LatexConverter)
        assert isinstance(manager.pdf_converter, PdfConverter)

    def test_generate_latex_missing_input_raises(self, tmp_path: Path) -> None:
        """generate_latex raises when input file does not exist."""
        from dtr_cli.cli_errors import ConversionError

        manager = LatexManager()
        missing = tmp_path / "nonexistent.md"

        with pytest.raises(ConversionError):
            manager.generate_latex(missing)

    def test_generate_latex_with_real_file(self, tmp_path: Path) -> None:
        """generate_latex processes a real Markdown file if pandoc is available."""
        import shutil

        if shutil.which("pandoc") is None:
            pytest.skip("pandoc not installed")

        md_file = tmp_path / "sample.md"
        md_file.write_text("# Hello\n\nThis is a test.", encoding="utf-8")
        tex_file = tmp_path / "sample.tex"

        manager = LatexManager()
        result = manager.generate_latex(md_file, tex_file)

        assert result.files_processed >= 1
        assert tex_file.exists()

    def test_compile_pdf_missing_input_raises(self, tmp_path: Path) -> None:
        """compile_pdf raises when .tex file does not exist."""
        manager = LatexManager()
        missing = tmp_path / "nonexistent.tex"

        # PdfConverter should raise on missing input
        with pytest.raises(Exception):
            manager.compile_pdf(missing)
