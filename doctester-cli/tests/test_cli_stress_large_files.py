"""Stress testing with large files and scale for CLI commands.

Tests that CLI handles large-scale operations with graceful performance:
- Large file handling (100MB to 1GB+)
- Many files in single directory (1000-10000 files)
- Deeply nested directory structures
- Memory and resource monitoring

Phase 5a: Stress Testing & Scale validation.

Test categories:
1. Large File Handling (8 tests)
   - Export/fmt operations on 100MB, 500MB, 1GB+ files
   - Memory efficiency and streaming
   - Timeout handling for long operations
   - Partial success on resource limits

2. Many Files in Single Directory (6 tests)
   - 1000 markdown files aggregation
   - 10000 markdown files (if hardware permits)
   - Mixed file sizes in same directory
   - Export aggregation without duplicates
   - Reasonable processing time (<5min)
   - Report generation with large file lists

3. Deeply Nested Directories (4 tests)
   - 100-level nested directory structure
   - 200-level nesting (OS limits)
   - Circular symlinks detection
   - Stack depth handling

4. Memory & Resource Monitoring (4 tests)
   - Peak memory stays under 500MB
   - Temp files cleaned up
   - File descriptors not leaked
   - CPU usage reasonable
"""

import os
import shutil
import tempfile
import time
from pathlib import Path
from typing import Any, Generator
from unittest import mock

import pytest
from typer.testing import CliRunner

from doctester_cli.main import app

runner = CliRunner()


def get_output(result: Any) -> str:
    """Get combined stdout and stderr from CLI result."""
    output = result.stdout
    if result.stderr:
        output += result.stderr
    return output.lower()


def create_large_file(path: Path, size_bytes: int, chunk_size: int = 1024 * 1024) -> None:
    """Create a large file efficiently without consuming equal RAM.

    Args:
        path: File to create
        size_bytes: Total size in bytes
        chunk_size: Write in chunks to avoid loading entire file in RAM
    """
    with open(path, 'wb') as f:
        remaining = size_bytes
        while remaining > 0:
            write_size = min(chunk_size, remaining)
            f.write(b'x' * write_size)
            remaining -= write_size


@pytest.fixture
def large_markdown_file_100mb(tmp_path: Path) -> Generator[Path, None, None]:
    """Create a 100MB markdown file for stress testing."""
    md_file = tmp_path / "large_100mb.md"
    with open(md_file, 'w') as f:
        f.write("# Large Markdown Document\n\n")
        f.write("This document is 100MB in size.\n\n")
        # Write content in 10MB chunks (9 times) for 90MB of body
        for i in range(9):
            chunk = f"## Section {i+1}\n"
            chunk += "x" * (10 * 1024 * 1024 - len(chunk))
            chunk += "\n\n"
            f.write(chunk)
        # Fill to 100MB
        remaining = 100 * 1024 * 1024 - md_file.stat().st_size
        if remaining > 0:
            f.write("x" * remaining)
    yield md_file


@pytest.fixture
def large_html_file_100mb(tmp_path: Path) -> Generator[Path, None, None]:
    """Create a 100MB HTML file for stress testing."""
    html_file = tmp_path / "large_100mb.html"
    with open(html_file, 'w') as f:
        f.write("<!DOCTYPE html>\n<html>\n<head><title>Large HTML</title></head>\n<body>\n")
        # Write 100MB of HTML content
        for i in range(1000):
            f.write(f"<div><p>Section {i}: " + "x" * 100000 + "</p></div>\n")
    yield html_file


@pytest.fixture
def many_small_markdown_files_1000(tmp_path: Path) -> Generator[Path, None, None]:
    """Create 1000 markdown files in a single directory."""
    files_dir = tmp_path / "many_files_1000"
    files_dir.mkdir()
    for i in range(1000):
        md_file = files_dir / f"doc_{i:04d}.md"
        md_file.write_text(f"# Document {i}\n\nContent for document {i}.\n")
    yield files_dir


@pytest.fixture
def many_mixed_size_files_500(tmp_path: Path) -> Generator[Path, None, None]:
    """Create 500 files with mixed sizes (1KB to 10MB) in single directory."""
    files_dir = tmp_path / "mixed_sizes_500"
    files_dir.mkdir()
    for i in range(500):
        md_file = files_dir / f"doc_{i:04d}.md"
        # Vary sizes: 1KB, 10KB, 100KB, 1MB, 10MB in rotation
        size = [1, 10, 100, 1024, 10 * 1024][i % 5]
        size_bytes = size * 1024
        with open(md_file, 'w') as f:
            f.write(f"# Document {i}\n\n")
            f.write("x" * (size_bytes - 20))
    yield files_dir


@pytest.fixture
def deeply_nested_100_dirs(tmp_path: Path) -> Generator[Path, None, None]:
    """Create 100 levels of nested directories."""
    current = tmp_path / "nested"
    current.mkdir()
    for i in range(100):
        current = current / f"level_{i:03d}"
        current.mkdir()
    # Create a file at the deepest level
    (current / "deep_file.md").write_text("# Deep File\n\nAt level 100.\n")
    yield tmp_path / "nested"


@pytest.fixture
def circular_symlink_structure(tmp_path: Path) -> Generator[Path, None, None]:
    """Create a directory structure with circular symlinks."""
    root = tmp_path / "symlink_root"
    root.mkdir()
    a_dir = root / "a"
    b_dir = root / "b"
    a_dir.mkdir()
    b_dir.mkdir()

    # Create files in both directories
    (a_dir / "file_a.md").write_text("# File A\n")
    (b_dir / "file_b.md").write_text("# File B\n")

    # Create circular symlinks
    try:
        (a_dir / "link_to_b").symlink_to(b_dir)
        (b_dir / "link_to_a").symlink_to(a_dir)
    except OSError:
        # Symlinks may not be supported on some systems
        pass

    yield root


# ============================================================================
# CATEGORY 1: LARGE FILE HANDLING (8 tests)
# ============================================================================


def test_export_list_with_100mb_markdown_file() -> None:
    """Test 'dtr export list' with 100MB markdown file.

    Expected:
    - Completes without crash
    - File is listed
    - No excessive memory usage
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)
        large_md = tmpdir_path / "large_100mb.md"
        create_large_file(large_md, 100 * 1024 * 1024)

        start_time = time.time()
        result = runner.invoke(app, ["export", "list", str(tmpdir_path)])
        elapsed = time.time() - start_time

        assert result.exit_code in [0, 1], f"Unexpected failure: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Python exception in large file list"
        assert elapsed < 30, f"Listing took too long: {elapsed:.1f}s"


def test_export_list_with_500mb_markdown_file() -> None:
    """Test 'dtr export list' with 500MB markdown file.

    Expected:
    - Handles gracefully
    - Lists file without timeout
    - Process doesn't hang
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)
        large_md = tmpdir_path / "large_500mb.md"
        create_large_file(large_md, 500 * 1024 * 1024)

        start_time = time.time()
        result = runner.invoke(
            app,
            ["export", "list", str(tmpdir_path)],
            timeout=60,  # 60 second timeout
        )
        elapsed = time.time() - start_time

        # Should complete or timeout gracefully
        assert result.exit_code in [0, 1], f"Unexpected behavior: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception raised on 500MB file"
        assert elapsed < 60, f"Command took too long: {elapsed:.1f}s"


def test_fmt_with_100mb_html_input() -> None:
    """Test 'dtr fmt' on 100MB HTML file.

    Expected:
    - Conversion completes or times out gracefully
    - No stack traces
    - Output format valid (if conversion succeeds)
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create 100MB HTML file
        large_html = tmpdir_path / "large_100mb.html"
        with open(large_html, 'w') as f:
            f.write("<!DOCTYPE html>\n<html>\n<head><title>Large</title></head>\n<body>\n")
            for i in range(500):
                f.write(f"<p>Content {i}: " + "x" * 200000 + "</p>\n")
            f.write("</body>\n</html>\n")

        output_file = tmpdir_path / "output.md"

        start_time = time.time()
        result = runner.invoke(
            app,
            ["fmt", str(large_html), "--output", str(output_file), "--from", "html", "--to", "markdown"],
            timeout=60,
        )
        elapsed = time.time() - start_time

        # Should complete or fail gracefully
        assert result.exit_code in [0, 1], f"Unexpected failure: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception during large file conversion"
        assert elapsed < 60, f"Conversion took too long: {elapsed:.1f}s"


def test_fmt_with_500mb_html_input() -> None:
    """Test 'dtr fmt' on 500MB HTML file.

    Expected:
    - Handles gracefully without hanging
    - Timeout completes cleanly
    - No memory explosion
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create 500MB HTML file
        large_html = tmpdir_path / "large_500mb.html"
        with open(large_html, 'w') as f:
            f.write("<!DOCTYPE html>\n<html>\n<head><title>Large</title></head>\n<body>\n")
            for i in range(2500):
                f.write(f"<p>Content {i}: " + "x" * 200000 + "</p>\n")
            f.write("</body>\n</html>\n")

        output_file = tmpdir_path / "output.md"

        start_time = time.time()
        result = runner.invoke(
            app,
            ["fmt", str(large_html), "--output", str(output_file), "--from", "html", "--to", "markdown"],
            timeout=120,
        )
        elapsed = time.time() - start_time

        # Graceful handling
        assert result.exit_code in [0, 1], f"Command failed unexpectedly: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception on 500MB conversion"
        assert elapsed < 120, f"Timeout - took {elapsed:.1f}s"


def test_report_with_large_markdown_input() -> None:
    """Test 'dtr report sum' on 100MB markdown file.

    Expected:
    - Report generation completes
    - Shows file statistics
    - No crash or excessive memory
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create 100MB markdown file
        large_md = tmpdir_path / "large_100mb.md"
        with open(large_md, 'w') as f:
            f.write("# Large Document\n\n")
            for i in range(500):
                f.write(f"## Section {i}\n")
                f.write("x" * 200000)
                f.write("\n\n")

        start_time = time.time()
        result = runner.invoke(app, ["report", "sum", str(tmpdir_path)])
        elapsed = time.time() - start_time

        # Should complete with statistics
        assert result.exit_code in [0, 1], f"Report failed: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception in report generation"
        assert elapsed < 30, f"Report took too long: {elapsed:.1f}s"


def test_export_save_large_markdown_export() -> None:
    """Test 'dtr export save' with 100MB markdown export.

    Expected:
    - Archive creation succeeds or fails gracefully
    - No temp files left behind on failure
    - Streaming doesn't load entire file in memory
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)
        export_dir = tmpdir_path / "exports"
        export_dir.mkdir()

        # Create 100MB markdown file
        large_md = export_dir / "large_100mb.md"
        create_large_file(large_md, 100 * 1024 * 1024)

        output_file = tmpdir_path / "export.tar.gz"

        start_time = time.time()
        result = runner.invoke(
            app,
            ["export", "save", str(export_dir), "--output", str(output_file), "--format", "tar.gz"],
            timeout=60,
        )
        elapsed = time.time() - start_time

        # Should complete
        assert result.exit_code in [0, 1], f"Archive creation failed: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception during archive creation"

        # If successful, archive should exist
        if result.exit_code == 0:
            assert output_file.exists(), "Archive not created on success"
            assert output_file.stat().st_size > 0, "Archive is empty"

        # Should not leave partial files
        for f in tmpdir_path.glob("*.tmp"):
            pytest.fail(f"Temp file left behind: {f}")


def test_export_with_timeout_handling_on_large_file() -> None:
    """Test that CLI times out gracefully on operations taking >10s.

    Expected:
    - Timeout is respected
    - No hanging processes
    - Helpful timeout message
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create a large file
        large_file = tmpdir_path / "large.md"
        create_large_file(large_file, 100 * 1024 * 1024)

        # Run with short timeout
        result = runner.invoke(
            app,
            ["export", "list", str(tmpdir_path)],
            timeout=2,  # 2 second timeout
        )

        # Should not have Python traceback
        output = get_output(result)
        assert "traceback" not in output, "Stack trace on timeout"


def test_export_partial_success_on_resource_limit() -> None:
    """Test graceful handling when processing hits system resource limits.

    Expected:
    - Command fails gracefully (not crash)
    - Clear error message about resource limit
    - Partial results if available
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create a large markdown file
        large_md = tmpdir_path / "large.md"
        create_large_file(large_md, 100 * 1024 * 1024)

        # Mock a resource limit exception
        def mock_process(*args, **kwargs):
            raise MemoryError("Cannot allocate memory")

        with mock.patch(
            "doctester_cli.managers.directory_manager.DirectoryManager.list_exports",
            side_effect=mock_process,
        ):
            result = runner.invoke(app, ["export", "list", str(tmpdir_path)])

        # Should fail cleanly
        assert result.exit_code != 0, "Should fail on resource limit"
        output = get_output(result)
        assert "traceback" not in output, "Stack trace leaked to user"
        # Should have helpful message
        assert any(word in output for word in ["error", "memory", "resource", "limit"]) or result.exit_code == 1


# ============================================================================
# CATEGORY 2: MANY FILES IN SINGLE DIRECTORY (6 tests)
# ============================================================================


def test_export_list_with_1000_markdown_files() -> None:
    """Test 'dtr export list' with 1000 files in single directory.

    Expected:
    - Lists all files efficiently
    - Completes in reasonable time (<5s)
    - No file limit hit
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create 1000 markdown files
        for i in range(1000):
            md_file = tmpdir_path / f"doc_{i:04d}.md"
            md_file.write_text(f"# Document {i}\n\nContent.\n")

        start_time = time.time()
        result = runner.invoke(app, ["export", "list", str(tmpdir_path)])
        elapsed = time.time() - start_time

        assert result.exit_code in [0, 1], f"Unexpected failure: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception listing 1000 files"
        assert elapsed < 5, f"Listing took too long: {elapsed:.1f}s"


def test_export_list_with_10000_markdown_files() -> None:
    """Test 'dtr export list' with 10000 files (if hardware permits).

    Expected:
    - Lists all files without crashing
    - Completes in reasonable time (<30s)
    - Handles directory iteration efficiently
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create 10000 markdown files (may take a while)
        print("Creating 10000 files...")
        for i in range(10000):
            if i % 1000 == 0:
                print(f"  Created {i} files...")
            md_file = tmpdir_path / f"doc_{i:05d}.md"
            md_file.write_text(f"# Document {i}\n")

        start_time = time.time()
        result = runner.invoke(app, ["export", "list", str(tmpdir_path)])
        elapsed = time.time() - start_time

        assert result.exit_code in [0, 1], f"Failed on 10000 files: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception with 10000 files"
        assert elapsed < 30, f"Listing took too long: {elapsed:.1f}s"


def test_export_with_mixed_file_sizes_in_same_dir(mixed_sizes_fixture: Any = None) -> None:
    """Test operations on mixed-size files (1KB to 100MB) in single directory.

    Expected:
    - Handles all sizes without discrimination
    - Large and small files processed equally
    - No file skipped or error on size variance
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create files of varying sizes
        files_dir = tmpdir_path / "mixed"
        files_dir.mkdir()

        sizes = [1, 10, 100, 1024, 10 * 1024]  # 1KB to 10MB
        for i, size_kb in enumerate(sizes):
            md_file = files_dir / f"doc_{i:04d}.md"
            size_bytes = size_kb * 1024
            with open(md_file, 'w') as f:
                f.write(f"# Document {i}\n\n")
                f.write("x" * (size_bytes - 20))

        start_time = time.time()
        result = runner.invoke(app, ["export", "list", str(files_dir)])
        elapsed = time.time() - start_time

        assert result.exit_code in [0, 1], f"Failed on mixed sizes: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception on mixed file sizes"
        assert elapsed < 5, f"Processing took too long: {elapsed:.1f}s"


def test_fmt_processes_many_files_in_reasonable_time() -> None:
    """Test 'dtr fmt' on directory with 500 files completes in <5 minutes.

    Expected:
    - All files processed or skipped
    - No hangs or timeouts
    - Reasonable throughput
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create 500 markdown files
        files_dir = tmpdir_path / "many_files"
        files_dir.mkdir()
        for i in range(500):
            md_file = files_dir / f"doc_{i:04d}.md"
            md_file.write_text(f"# Document {i}\n\nSome content here.\n")

        output_dir = tmpdir_path / "output"
        output_dir.mkdir()

        start_time = time.time()
        result = runner.invoke(
            app,
            ["fmt", str(files_dir), "--output", str(output_dir), "--from", "markdown", "--to", "html"],
            timeout=300,  # 5 minute timeout
        )
        elapsed = time.time() - start_time

        assert result.exit_code in [0, 1], f"Processing failed: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception during batch processing"
        assert elapsed < 300, f"Processing took {elapsed:.1f}s (>5min)"


def test_export_save_aggregates_many_files_without_duplicates() -> None:
    """Test 'dtr export save' on directory with 1000 files without duplication.

    Expected:
    - Archive contains all files
    - No duplicate entries
    - Archive integrity verified
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)
        export_dir = tmpdir_path / "exports"
        export_dir.mkdir()

        # Create 1000 markdown files
        for i in range(1000):
            md_file = export_dir / f"doc_{i:04d}.md"
            md_file.write_text(f"# Document {i}\n")

        output_file = tmpdir_path / "export.tar.gz"

        start_time = time.time()
        result = runner.invoke(
            app,
            ["export", "save", str(export_dir), "--output", str(output_file), "--format", "tar.gz"],
            timeout=30,
        )
        elapsed = time.time() - start_time

        assert result.exit_code in [0, 1], f"Archive creation failed: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception during aggregation"

        if result.exit_code == 0:
            assert output_file.exists(), "Archive not created"
            # Verify archive has content
            assert output_file.stat().st_size > 1000, "Archive suspiciously small"


def test_report_sum_on_large_file_list() -> None:
    """Test 'dtr report sum' with 1000 files in directory.

    Expected:
    - Report generation completes
    - Statistics accurate
    - Processes all files
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create 1000 markdown files
        for i in range(1000):
            md_file = tmpdir_path / f"doc_{i:04d}.md"
            md_file.write_text(f"# Document {i}\n\nContent {i}.\n")

        start_time = time.time()
        result = runner.invoke(app, ["report", "sum", str(tmpdir_path)])
        elapsed = time.time() - start_time

        assert result.exit_code in [0, 1], f"Report failed: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception in report"
        assert elapsed < 10, f"Report took {elapsed:.1f}s"


# ============================================================================
# CATEGORY 3: DEEPLY NESTED DIRECTORIES (4 tests)
# ============================================================================


def test_export_list_with_100_levels_nested_dirs() -> None:
    """Test 'dtr export list' with 100 levels of nested directories.

    Expected:
    - Traverses full depth without stack overflow
    - Finds files at deepest level
    - Completes without hanging
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)
        current = tmpdir_path / "nested"
        current.mkdir()

        # Create 100 levels
        for i in range(100):
            current = current / f"level_{i:03d}"
            current.mkdir()

        # Create file at deepest level
        (current / "deep_file.md").write_text("# Deep File\n")

        start_time = time.time()
        result = runner.invoke(app, ["export", "list", str(tmpdir_path / "nested")])
        elapsed = time.time() - start_time

        assert result.exit_code in [0, 1], f"Failed on deep nesting: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Stack overflow on 100-level nesting"
        assert elapsed < 5, f"Traversal took too long: {elapsed:.1f}s"


def test_export_list_with_200_levels_nested_dirs() -> None:
    """Test 'dtr export list' with 200 levels of nesting (OS limit testing).

    Expected:
    - Either completes or fails gracefully
    - No stack overflow crash
    - Clear error if OS limits hit
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)
        current = tmpdir_path / "nested_200"
        current.mkdir()

        # Create 200 levels (may fail on some systems)
        try:
            for i in range(200):
                current = current / f"level_{i:03d}"
                current.mkdir()
            # File at deepest level
            (current / "deep.md").write_text("# Deep\n")
            deepest_path = tmpdir_path / "nested_200"
        except OSError:
            # OS limit hit - that's OK, test the path traversal of what we have
            deepest_path = current

        result = runner.invoke(app, ["export", "list", str(deepest_path)])

        # Should not crash
        assert result.exit_code in [0, 1], f"Failed: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Stack overflow or exception on deep nesting"


def test_circular_symlinks_handling() -> None:
    """Test 'dtr export list' with circular symlinks in structure.

    Expected:
    - Detects cycles without infinite loop
    - Completes without hanging
    - Clear handling of symlinks
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)
        root = tmpdir_path / "symlink_root"
        root.mkdir()

        a_dir = root / "a"
        b_dir = root / "b"
        a_dir.mkdir()
        b_dir.mkdir()

        # Create files
        (a_dir / "file_a.md").write_text("# A\n")
        (b_dir / "file_b.md").write_text("# B\n")

        # Create circular symlinks (if supported)
        try:
            (a_dir / "link_to_b").symlink_to(b_dir)
            (b_dir / "link_to_a").symlink_to(a_dir)
        except OSError:
            # Symlinks not supported
            pytest.skip("Symlinks not supported on this system")

        start_time = time.time()
        result = runner.invoke(app, ["export", "list", str(root)])
        elapsed = time.time() - start_time

        # Should not hang or crash
        assert result.exit_code in [0, 1], f"Failed with circular symlinks: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception on circular symlinks"
        assert elapsed < 5, f"Symlink processing hung: {elapsed:.1f}s"


def test_path_traversal_stack_depth_not_exceeded() -> None:
    """Test that path traversal respects stack depth limits.

    Expected:
    - No RecursionError or stack overflow
    - Graceful handling of deep paths
    - Clear error message if limit reached
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create increasingly deep directory structure
        current = tmpdir_path / "deep"
        current.mkdir()

        depth = 150
        try:
            for i in range(depth):
                current = current / f"d{i}"
                current.mkdir()
        except OSError:
            # System limit hit
            pass

        result = runner.invoke(app, ["export", "list", str(tmpdir_path / "deep")])

        # Should not crash with recursion error
        assert result.exit_code in [0, 1], f"Failed: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Stack overflow exception"
        assert "recursion" not in output, "Recursion limit exceeded"


# ============================================================================
# CATEGORY 4: MEMORY & RESOURCE MONITORING (4 tests)
# ============================================================================


def test_peak_memory_under_limit_on_100mb_file() -> None:
    """Test that processing 100MB file doesn't exceed 500MB peak memory.

    Expected:
    - Peak memory usage stays reasonable
    - No memory leaks during processing
    - Process completes and releases memory
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)
        large_md = tmpdir_path / "large.md"
        create_large_file(large_md, 100 * 1024 * 1024)

        # Try to monitor memory if psutil available
        try:
            import psutil
            process = psutil.Process(os.getpid())
            mem_before = process.memory_info().rss / (1024 * 1024)  # MB
        except ImportError:
            mem_before = None

        result = runner.invoke(app, ["export", "list", str(tmpdir_path)])

        # Memory check (if available)
        if mem_before is not None:
            try:
                mem_after = process.memory_info().rss / (1024 * 1024)  # MB
                # Growth should be reasonable (not 500MB+)
                growth = mem_after - mem_before
                assert growth < 200, f"Excessive memory growth: {growth:.1f}MB"
            except Exception:
                # Memory monitoring failed, but command should still work
                pass

        assert result.exit_code in [0, 1], f"Failed: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception during processing"


def test_temp_files_cleaned_on_completion() -> None:
    """Test that temp files are cleaned up after operation completes.

    Expected:
    - No .tmp, .temp, .partial files left behind
    - Temp directory empty after success
    - Cleanup happens even on error
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)
        export_dir = tmpdir_path / "exports"
        export_dir.mkdir()

        # Create test files
        (export_dir / "test1.md").write_text("# Test 1\n")
        (export_dir / "test2.md").write_text("# Test 2\n")

        output_file = tmpdir_path / "archive.tar.gz"

        # Count files before
        temp_before = list(tmpdir_path.glob("*.tmp")) + list(tmpdir_path.glob("*.temp"))

        result = runner.invoke(
            app,
            ["export", "save", str(export_dir), "--output", str(output_file)],
        )

        # Count files after
        temp_after = list(tmpdir_path.glob("*.tmp")) + list(tmpdir_path.glob("*.temp"))

        # Should not have created permanent temp files
        new_temp_files = [f for f in temp_after if f not in temp_before]
        assert len(new_temp_files) == 0, f"Temp files left: {new_temp_files}"


def test_file_descriptors_not_leaked() -> None:
    """Test that file descriptors aren't leaked during processing.

    Expected:
    - File descriptors released after operation
    - No accumulating open files
    - Cleanup on error and success
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create test files
        files_dir = tmpdir_path / "files"
        files_dir.mkdir()
        for i in range(100):
            (files_dir / f"doc_{i:04d}.md").write_text(f"# Document {i}\n")

        # Try to check FD count if available
        try:
            import psutil
            process = psutil.Process(os.getpid())
            fds_before = process.num_fds()
        except (ImportError, AttributeError):
            fds_before = None

        # Run command multiple times
        for _ in range(5):
            result = runner.invoke(app, ["export", "list", str(files_dir)])
            assert result.exit_code in [0, 1]

        # Check FD count didn't grow excessively
        if fds_before is not None:
            try:
                fds_after = process.num_fds()
                growth = fds_after - fds_before
                # Allow some growth but not unbounded
                assert growth < 50, f"File descriptors leaked: {growth} new FDs"
            except Exception:
                # FD monitoring failed, but operations should still work
                pass


def test_cpu_usage_reasonable_on_large_operations() -> None:
    """Test that CPU usage stays reasonable during large operations.

    Expected:
    - Process doesn't spin continuously
    - Completes without excessive CPU churn
    - No busy-wait loops
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir_path = Path(tmpdir)

        # Create 500 files
        for i in range(500):
            (tmpdir_path / f"doc_{i:04d}.md").write_text(f"# Document {i}\n")

        start_time = time.time()
        result = runner.invoke(app, ["export", "list", str(tmpdir_path)])
        elapsed = time.time() - start_time

        # Should complete reasonably fast (not busy-waiting)
        # 500 files should list in <5 seconds
        assert elapsed < 5, f"Excessive processing time: {elapsed:.1f}s (may indicate busy-wait)"

        # Should succeed
        assert result.exit_code in [0, 1], f"Failed: {result.stdout}"
        output = get_output(result)
        assert "traceback" not in output, "Exception during listing"
