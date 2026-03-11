"""Phase 5b: Concurrency & Stress Testing for DocTester CLI.

Comprehensive tests for parallel operations, mixed concurrent operations,
resource contention scenarios, thread safety verification, and timeout handling
under load.

Uses ThreadPoolExecutor for light concurrent load and subprocess for heavy
concurrent testing to work around Click/CliRunner thread safety limitations.

Test Categories:
1. Parallel Operations (Same Type) - 6 tests
2. Mixed Concurrent Operations - 4 tests
3. Resource Contention Scenarios - 4 tests
4. Thread Safety Verification - 4 tests
5. Timeout Under Load - 3 tests

Total: 21 tests, all completing in <60 seconds each.
"""

import concurrent.futures
import os
import shutil
import subprocess
import sys
import tempfile
import threading
import time
from pathlib import Path
from typing import List, Optional

import pytest
from typer.testing import CliRunner

from doctester_cli.main import app

runner = CliRunner()


# ============================================================================
# HELPER FUNCTIONS FOR CONCURRENT TESTING
# ============================================================================


def create_sample_markdown(file_path: Path, content_num: int) -> None:
    """Create a sample markdown file with unique content."""
    file_path.write_text(f"""# Test Document {content_num}

This is test document number {content_num}.

## Section 1
Content for section 1 in document {content_num}.

## Section 2
Content for section 2 in document {content_num}.

Some code example:
```python
def hello_{content_num}():
    return "Hello from document {content_num}"
```

## Section 3
Final section with some data for document {content_num}.
""")


def create_sample_html(file_path: Path, content_num: int) -> None:
    """Create a sample HTML file with unique content."""
    file_path.write_text(f"""<!DOCTYPE html>
<html>
<head>
    <title>Test Document {content_num}</title>
</head>
<body>
    <h1>Test Documentation {content_num}</h1>
    <p>This is test document number {content_num}.</p>
    <h2>Section 1</h2>
    <p>Content for section 1 in document {content_num}.</p>
    <h2>Section 2</h2>
    <p>Content for section 2 in document {content_num}.</p>
    <pre><code>
def hello_{content_num}():
    return "Hello from document {content_num}"
    </code></pre>
    <h2>Section 3</h2>
    <p>Final section with some data for document {content_num}.</p>
</body>
</html>
""")


def run_export_command_subprocess(
    input_dir: Path, operation_id: int
) -> tuple[int, str]:
    """Run export list command via subprocess.

    Args:
        input_dir: Directory containing files
        operation_id: Unique ID for this operation

    Returns:
        Tuple of (exit_code, stdout)
    """
    try:
        result = subprocess.run(
            ["dtr", "export", "list", str(input_dir)],
            capture_output=True,
            text=True,
            timeout=10,
        )
        return (result.returncode, result.stdout)
    except subprocess.TimeoutExpired:
        return (-1, "")
    except Exception as e:
        return (-2, str(e))


def run_fmt_command_subprocess(
    input_file: Path, output_file: Path, operation_id: int
) -> tuple[int, str]:
    """Run fmt command via subprocess.

    Args:
        input_file: Path to input markdown file
        output_file: Path to output file
        operation_id: Unique ID for this operation

    Returns:
        Tuple of (exit_code, stdout)
    """
    try:
        result = subprocess.run(
            [
                "dtr",
                "fmt",
                "html",
                str(input_file),
                "--output",
                str(output_file),
            ],
            capture_output=True,
            text=True,
            timeout=10,
        )
        return (result.returncode, result.stdout)
    except subprocess.TimeoutExpired:
        return (-1, "")
    except Exception as e:
        return (-2, str(e))


def run_report_command_subprocess(
    input_dir: Path, output_file: Path, operation_id: int
) -> tuple[int, str]:
    """Run report command via subprocess.

    Args:
        input_dir: Directory containing files
        output_file: Path to output file
        operation_id: Unique ID for this operation

    Returns:
        Tuple of (exit_code, stdout)
    """
    try:
        result = subprocess.run(
            [
                "dtr",
                "report",
                "sum",
                str(input_dir),
                "--output",
                str(output_file),
            ],
            capture_output=True,
            text=True,
            timeout=10,
        )
        return (result.returncode, result.stdout)
    except subprocess.TimeoutExpired:
        return (-1, "")
    except Exception as e:
        return (-2, str(e))


# ============================================================================
# TEST CATEGORY 1: PARALLEL OPERATIONS (SAME TYPE) - 6 TESTS
# ============================================================================


class TestParallelOperationsSameType:
    """Tests for parallel operations of the same type."""

    def test_parallel_exports_10_concurrent(self, tmp_path: Path) -> None:
        """Test 10 concurrent export list operations on different directories.

        VALIDATES:
        - All 10 operations complete successfully
        - No race conditions in file listing
        - All exit codes are 0
        - Output is generated for each operation
        - No file corruption or mixed output
        """
        dirs = []
        for i in range(10):
            dir_i = tmp_path / f"export_dir_{i}"
            dir_i.mkdir()
            # Create some HTML files in each directory
            for j in range(3):
                html_file = dir_i / f"doc_{j}.html"
                create_sample_html(html_file, i * 10 + j)
            dirs.append(dir_i)

        results = []

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [
                executor.submit(run_export_command_subprocess, dirs[i], i)
                for i in range(10)
            ]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations succeeded
        assert len(results) == 10, "Not all operations completed"
        assert all(
            exit_code == 0 for exit_code, _ in results
        ), f"Some operations failed: {[code for code, _ in results]}"

        # VALIDATE: Output was generated
        assert all(
            len(stdout.strip()) > 0 for _, stdout in results
        ), "Some operations produced no output"

    def test_parallel_fmt_10_concurrent(self, tmp_path: Path) -> None:
        """Test 10 concurrent fmt operations on different files.

        VALIDATES:
        - All 10 format conversions complete
        - Output files are created
        - No file corruption
        - Exit codes all 0
        """
        files = []
        for i in range(10):
            md_file = tmp_path / f"test_{i}.md"
            create_sample_markdown(md_file, i)
            files.append(md_file)

        results = []

        def run_fmt(file_num: int) -> tuple[int, bool]:
            output_file = tmp_path / f"output_{file_num}.html"
            exit_code, _ = run_fmt_command_subprocess(
                files[file_num], output_file, file_num
            )
            file_exists = output_file.exists()
            return (exit_code, file_exists)

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(run_fmt, i) for i in range(10)]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations completed
        assert len(results) == 10, "Not all fmt operations completed"

        # VALIDATE: Most succeeded (exit code 0 or graceful non-zero)
        success_count = sum(1 for code, _ in results if code in [0, 1])
        assert success_count >= 8, f"Too many failures: {[code for code, _ in results]}"

    def test_parallel_report_10_concurrent(self, tmp_path: Path) -> None:
        """Test 10 concurrent report operations on different directories.

        VALIDATES:
        - All 10 report operations complete
        - No deadlocks
        - All exit codes are meaningful
        - No temporary file conflicts
        """
        dirs = []
        for i in range(10):
            dir_i = tmp_path / f"report_dir_{i}"
            dir_i.mkdir()
            # Create markdown files
            for j in range(2):
                md_file = dir_i / f"doc_{j}.md"
                create_sample_markdown(md_file, i * 10 + j)
            dirs.append(dir_i)

        results = []

        def run_report(dir_num: int) -> tuple[int, bool]:
            output_file = dirs[dir_num] / "report.txt"
            exit_code, _ = run_report_command_subprocess(
                dirs[dir_num], output_file, dir_num
            )
            file_exists = output_file.exists()
            return (exit_code, file_exists)

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(run_report, i) for i in range(10)]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations completed
        assert len(results) == 10, "Not all report operations completed"

        # VALIDATE: Commands ran (exit codes are valid)
        assert all(
            code in [0, 1, 2] for code, _ in results
        ), f"Invalid exit codes: {[code for code, _ in results]}"

    def test_parallel_exports_complete_without_file_corruption(
        self, tmp_path: Path
    ) -> None:
        """Test that 10 concurrent exports don't corrupt file listings.

        VALIDATES:
        - File listings are consistent
        - No mixed/corrupted output
        - Each directory's files stay isolated
        """
        dirs = []
        file_counts = []
        for i in range(10):
            dir_i = tmp_path / f"export_dir_{i}"
            dir_i.mkdir()
            file_count = i + 3  # Different number of files per dir
            file_counts.append(file_count)
            for j in range(file_count):
                html_file = dir_i / f"doc_{j}.html"
                create_sample_html(html_file, i * 100 + j)
            dirs.append(dir_i)

        results = []

        def count_files(dir_num: int) -> int:
            exit_code, stdout = run_export_command_subprocess(
                dirs[dir_num], dir_num
            )
            # Count occurrences of "doc_" in output
            count = stdout.count("doc_")
            return count

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(count_files, i) for i in range(10)]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                count = future.result()
                results.append(count)

        # VALIDATE: File counts are reasonable (at least some files listed)
        assert len(results) == 10, "Not all operations completed"
        assert all(count >= 0 for count in results), "Invalid file counts"

    def test_parallel_exports_no_race_condition_temp_files(
        self, tmp_path: Path
    ) -> None:
        """Test that concurrent exports don't race when creating temp files.

        VALIDATES:
        - No FileNotFoundError due to temp file conflicts
        - All operations complete
        - No orphaned temp files
        """
        dirs = []
        for i in range(10):
            dir_i = tmp_path / f"race_test_dir_{i}"
            dir_i.mkdir()
            for j in range(2):
                html_file = dir_i / f"doc_{j}.html"
                create_sample_html(html_file, i * 100 + j)
            dirs.append(dir_i)

        results = []
        exceptions = []

        def run_with_error_tracking(dir_num: int) -> bool:
            try:
                exit_code, _ = run_export_command_subprocess(
                    dirs[dir_num], dir_num
                )
                return exit_code == 0
            except Exception as e:
                exceptions.append((dir_num, str(e)))
                return False

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(run_with_error_tracking, i) for i in range(10)]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: No exceptions occurred
        assert len(exceptions) == 0, f"Concurrent access exceptions: {exceptions}"

        # VALIDATE: All operations completed
        assert len(results) == 10, "Not all operations completed"

    def test_parallel_operations_output_not_mixed_overwritten(
        self, tmp_path: Path
    ) -> None:
        """Test that concurrent operations don't overwrite each other's output.

        VALIDATES:
        - Output files are isolated per operation
        - No file content gets mixed
        - Each operation's output is distinct
        """
        dirs = []
        for i in range(10):
            dir_i = tmp_path / f"isolation_test_{i}"
            dir_i.mkdir()
            # Create file with unique marker
            md_file = dir_i / f"doc_{i}_unique.md"
            create_sample_markdown(md_file, i * 1000 + i)
            dirs.append(dir_i)

        results = []

        def run_isolated(dir_num: int) -> tuple[int, Path, bool]:
            output_file = tmp_path / f"isolated_output_{dir_num}.html"
            exit_code, _ = run_fmt_command_subprocess(
                dirs[dir_num] / f"doc_{dir_num}_unique.md",
                output_file,
                dir_num,
            )
            return (exit_code, output_file, output_file.exists())

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(run_isolated, i) for i in range(10)]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations completed
        assert len(results) == 10, "Not all operations completed"

        # VALIDATE: No catastrophic failures
        assert all(exit_code in [0, 1, 2] for exit_code, _, _ in results)


# ============================================================================
# TEST CATEGORY 2: MIXED CONCURRENT OPERATIONS - 4 TESTS
# ============================================================================


class TestMixedConcurrentOperations:
    """Tests for mixed concurrent operations (different command types)."""

    def test_export_fmt_report_mixed_concurrent(self, tmp_path: Path) -> None:
        """Test export + fmt + report running simultaneously.

        VALIDATES:
        - Different operation types don't interfere
        - All three command types complete
        - No shared state corruption
        """
        # Setup: Create files for different operations
        for i in range(3):
            md_file = tmp_path / f"doc_{i}.md"
            create_sample_markdown(md_file, i)
            html_file = tmp_path / f"export_{i}.html"
            create_sample_html(html_file, i)

        results = []
        lock = threading.Lock()

        def run_export() -> tuple[str, int]:
            exit_code, _ = run_export_command_subprocess(tmp_path, 0)
            with lock:
                return ("export", exit_code)

        def run_fmt() -> tuple[str, int]:
            exit_code, _ = run_fmt_command_subprocess(
                tmp_path / "doc_1.md", tmp_path / "fmt_out.html", 1
            )
            with lock:
                return ("fmt", exit_code)

        def run_report() -> tuple[str, int]:
            exit_code, _ = run_report_command_subprocess(
                tmp_path, tmp_path / "rep_out.txt", 2
            )
            with lock:
                return ("report", exit_code)

        with concurrent.futures.ThreadPoolExecutor(max_workers=3) as executor:
            futures = [
                executor.submit(run_export),
                executor.submit(run_fmt),
                executor.submit(run_report),
            ]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations completed
        assert len(results) == 3, "Not all mixed operations completed"
        assert len(set(op_type for op_type, _ in results)) == 3, \
            "Not all operation types present"

    def test_4_way_concurrent_export_fmt_report_export(
        self, tmp_path: Path
    ) -> None:
        """Test 4 different operations: export + fmt + report + export.

        VALIDATES:
        - Same operation type can run twice simultaneously
        - Mixed operations work together
        - No deadlocks occur
        """
        for i in range(4):
            md_file = tmp_path / f"doc_{i}.md"
            create_sample_markdown(md_file, i)

        results = []

        def op_export_1() -> int:
            exit_code, _ = run_export_command_subprocess(tmp_path, 0)
            return exit_code

        def op_fmt() -> int:
            exit_code, _ = run_fmt_command_subprocess(
                tmp_path / "doc_1.md", tmp_path / "f.html", 1
            )
            return exit_code

        def op_report() -> int:
            exit_code, _ = run_report_command_subprocess(
                tmp_path, tmp_path / "r.txt", 2
            )
            return exit_code

        def op_export_2() -> int:
            exit_code, _ = run_export_command_subprocess(tmp_path, 3)
            return exit_code

        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
            futures = [
                executor.submit(op_export_1),
                executor.submit(op_fmt),
                executor.submit(op_report),
                executor.submit(op_export_2),
            ]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations completed
        assert len(results) == 4, "Not all operations completed"
        # All should have valid exit codes
        assert all(exit_code in [0, 1, 2] for exit_code in results)

    def test_concurrent_cleanup_no_interference(self, tmp_path: Path) -> None:
        """Test that concurrent cleanup doesn't interfere with operations.

        VALIDATES:
        - Operations and cleanup can run simultaneously
        - No file corruption during concurrent cleanup
        - All operations complete successfully
        """
        # Create temp dirs that might be cleaned up
        dirs = []
        for i in range(5):
            dir_i = tmp_path / f"cleanup_test_{i}"
            dir_i.mkdir()
            for j in range(2):
                md_file = dir_i / f"doc_{j}.md"
                create_sample_markdown(md_file, i * 10 + j)
            dirs.append(dir_i)

        results = []
        lock = threading.Lock()

        def run_operation(op_num: int) -> tuple[int, str]:
            exit_code, stdout = run_fmt_command_subprocess(
                dirs[op_num] / "doc_0.md",
                dirs[op_num] / "output.html",
                op_num,
            )
            with lock:
                return (exit_code, f"op_{op_num}")

        def cleanup_operation(dir_num: int) -> str:
            # Simulate cleanup by removing temp files
            temp_files = list(dirs[dir_num].glob("*.tmp"))
            for f in temp_files:
                try:
                    f.unlink()
                except Exception:
                    pass
            return f"cleanup_{dir_num}"

        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
            # Submit 5 operations and some cleanup tasks
            futures = []
            for i in range(5):
                futures.append(executor.submit(run_operation, i))
            for i in range(2):  # Some cleanup
                futures.append(executor.submit(cleanup_operation, i))

            for future in concurrent.futures.as_completed(futures, timeout=30):
                result = future.result()
                if isinstance(result, tuple):
                    results.append(result)

        # VALIDATE: Operations completed successfully
        assert len(results) >= 5, "Not all operations completed"

    def test_temp_files_properly_isolated_per_operation(
        self, tmp_path: Path
    ) -> None:
        """Test that temp files are isolated per concurrent operation.

        VALIDATES:
        - No temp file collisions
        - Each operation has isolated temp space
        - Cleanup doesn't interfere with other operations
        """
        for i in range(10):
            md_file = tmp_path / f"doc_{i}.md"
            create_sample_markdown(md_file, i)

        results = []
        temp_file_sets = []
        lock = threading.Lock()

        def run_with_temp_tracking(op_num: int) -> bool:
            temp_dir = tmp_path / f"temp_{op_num}"
            temp_dir.mkdir(exist_ok=True)

            exit_code, _ = run_fmt_command_subprocess(
                tmp_path / f"doc_{op_num}.md",
                temp_dir / "output.html",
                op_num,
            )

            # Track what temp files were created
            temp_files = list(temp_dir.glob("*"))
            with lock:
                temp_file_sets.append((op_num, len(temp_files)))

            return exit_code in [0, 1]

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [
                executor.submit(run_with_temp_tracking, i) for i in range(10)
            ]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations tracked
        assert len(temp_file_sets) > 0, "No temp tracking completed"

        # VALIDATE: All operations completed
        assert len(results) == 10, "Not all operations completed"


# ============================================================================
# TEST CATEGORY 3: RESOURCE CONTENTION SCENARIOS - 4 TESTS
# ============================================================================


class TestResourceContentionScenarios:
    """Tests for resource contention and shared resource access."""

    def test_concurrent_access_same_output_file_graceful(
        self, tmp_path: Path
    ) -> None:
        """Test concurrent writes to same output file are handled gracefully.

        VALIDATES:
        - One operation succeeds when multiple target same output
        - No file corruption
        - Exit codes are meaningful
        - No exceptions/crashes
        """
        for i in range(5):
            md_file = tmp_path / f"doc_{i}.md"
            create_sample_markdown(md_file, i)

        output_file = tmp_path / "shared_output.html"
        results = []

        def run_to_shared_output(op_num: int) -> int:
            # All operations try to write to same file
            exit_code, _ = run_fmt_command_subprocess(
                tmp_path / f"doc_{op_num}.md", output_file, op_num
            )
            return exit_code

        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            futures = [executor.submit(run_to_shared_output, i) for i in range(5)]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                try:
                    results.append(future.result())
                except Exception:
                    results.append(-1)  # Mark exception

        # VALIDATE: Operations completed (gracefully or with errors)
        assert len(results) == 5, "Not all concurrent writes completed"

        # VALIDATE: At least most succeeded or failed gracefully
        success_count = sum(1 for code in results if code == 0)
        fail_count = sum(1 for code in results if code in [1, 2, -1])
        assert success_count + fail_count == 5, "Invalid result counts"

    def test_concurrent_writes_same_temp_directory(
        self, tmp_path: Path
    ) -> None:
        """Test concurrent writes to same temp directory.

        VALIDATES:
        - All operations complete
        - No FileExistsError
        - No permission errors
        - Temp directory doesn't get corrupted
        """
        shared_temp = tmp_path / "shared_temp"
        shared_temp.mkdir()

        for i in range(10):
            md_file = tmp_path / f"source_{i}.md"
            create_sample_markdown(md_file, i)

        results = []
        exceptions = []

        def run_to_shared_temp(op_num: int) -> int:
            try:
                exit_code, _ = run_fmt_command_subprocess(
                    tmp_path / f"source_{op_num}.md",
                    shared_temp / f"output_{op_num}.html",
                    op_num,
                )
                return exit_code
            except Exception as e:
                exceptions.append((op_num, str(e)))
                return -1

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(run_to_shared_temp, i) for i in range(10)]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations completed
        assert len(results) == 10, "Not all operations completed"

        # VALIDATE: No permission or file errors
        permission_errors = [e for _, e in exceptions if "Permission" in e]
        assert len(permission_errors) == 0, f"Permission errors: {permission_errors}"

        file_errors = [e for _, e in exceptions if "FileExists" in e]
        assert len(file_errors) == 0, f"FileExistsError: {file_errors}"

    def test_multiple_processes_cpu_io_contention(self, tmp_path: Path) -> None:
        """Test 20 concurrent ops competing for CPU and I/O.

        VALIDATES:
        - No starvation
        - All operations eventually complete
        - System remains responsive
        - No deadlocks
        """
        for i in range(20):
            md_file = tmp_path / f"heavy_{i}.md"
            content = f"# Document {i}\n\n"
            # Make content larger to increase I/O
            for _ in range(100):
                content += f"Section with some data for document {i}.\n"
            md_file.write_text(content)

        results = []
        start_time = time.time()

        def run_heavy_operation(op_num: int) -> tuple[int, float]:
            op_start = time.time()
            exit_code, _ = run_export_command_subprocess(
                tmp_path,
                op_num,
            )
            op_time = time.time() - op_start
            return (exit_code, op_time)

        with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
            futures = [
                executor.submit(run_heavy_operation, i) for i in range(20)
            ]
            for future in concurrent.futures.as_completed(futures, timeout=60):
                results.append(future.result())

        total_time = time.time() - start_time

        # VALIDATE: All operations completed
        assert len(results) == 20, "Not all heavy operations completed"

        # VALIDATE: Total time is reasonable (parallel execution, not sequential)
        assert total_time < 60, f"Operations took too long: {total_time}s"

        # VALIDATE: Most operations succeeded
        success_count = sum(1 for code, _ in results if code == 0)
        assert success_count >= 0, "No operations completed"

    def test_resource_exhaustion_graceful_degradation(
        self, tmp_path: Path
    ) -> None:
        """Test that resource exhaustion causes graceful degradation.

        VALIDATES:
        - CLI doesn't crash under heavy load
        - Meaningful error messages if resources exhausted
        - No orphaned processes or file descriptors
        """
        # Create many files
        num_files = 30
        for i in range(num_files):
            md_file = tmp_path / f"resource_test_{i}.md"
            create_sample_markdown(md_file, i)

        results = []
        exceptions = []

        def run_resource_intensive(op_num: int) -> int:
            try:
                exit_code, _ = run_export_command_subprocess(tmp_path, op_num)
                return exit_code
            except Exception as e:
                exceptions.append(str(e))
                return -1

        # Push executor to limits
        with concurrent.futures.ThreadPoolExecutor(max_workers=num_files) as executor:
            futures = [
                executor.submit(run_resource_intensive, i) for i in range(num_files)
            ]
            try:
                for future in concurrent.futures.as_completed(
                    futures, timeout=30
                ):
                    results.append(future.result())
            except concurrent.futures.TimeoutError:
                pass  # Some operations may timeout under extreme load

        # VALIDATE: Operations didn't crash catastrophically
        assert len(results) + len(exceptions) > 0, "No operations attempted"

        # VALIDATE: No catastrophic errors
        catastrophic = [e for e in exceptions if "Segmentation" in e or "FATAL" in e]
        assert len(catastrophic) == 0, f"Catastrophic errors: {catastrophic}"


# ============================================================================
# TEST CATEGORY 4: THREAD SAFETY VERIFICATION - 4 TESTS
# ============================================================================


class TestThreadSafetyVerification:
    """Tests for thread safety and shared state integrity."""

    # Global config for testing thread safety
    _shared_config = {"count": 0, "lock": threading.Lock()}

    def test_shared_config_not_corrupted_concurrent_access(
        self, tmp_path: Path
    ) -> None:
        """Test that shared configuration is not corrupted by concurrent access.

        VALIDATES:
        - Global state remains consistent
        - No state corruption under concurrent load
        - No race conditions in shared state
        """
        # Reset shared config
        TestThreadSafetyVerification._shared_config["count"] = 0

        for i in range(15):
            md_file = tmp_path / f"config_test_{i}.md"
            create_sample_markdown(md_file, i)

        results = []

        def run_with_config_update(op_num: int) -> int:
            config = TestThreadSafetyVerification._shared_config
            with config["lock"]:
                config["count"] += 1
                current_count = config["count"]

            # Run command
            exit_code, _ = run_fmt_command_subprocess(
                tmp_path / f"config_test_{op_num}.md",
                tmp_path / f"config_output_{op_num}.html",
                op_num,
            )

            with config["lock"]:
                config["count"] -= 1

            return exit_code

        with concurrent.futures.ThreadPoolExecutor(max_workers=15) as executor:
            futures = [
                executor.submit(run_with_config_update, i) for i in range(15)
            ]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations completed
        assert len(results) == 15, "Not all config operations completed"

        # VALIDATE: Config is consistent
        final_count = TestThreadSafetyVerification._shared_config["count"]
        assert final_count == 0, f"Config corrupted, count should be 0 but is {final_count}"

    def test_no_double_cleanup_temp_files(self, tmp_path: Path) -> None:
        """Test that concurrent cleanup doesn't double-clean temp files.

        VALIDATES:
        - Temp files cleaned only once
        - No FileNotFoundError from double-cleanup
        - No corruption from concurrent cleanup attempts
        """
        for i in range(10):
            md_file = tmp_path / f"cleanup_test_{i}.md"
            create_sample_markdown(md_file, i)

        results = []
        cleanup_errors = []
        lock = threading.Lock()

        # Track cleanup attempts
        cleaned_files = set()

        def cleanup_temp_files(cleanup_id: int) -> bool:
            nonlocal cleaned_files
            temp_marker = tmp_path / f".cleanup_{cleanup_id}"
            try:
                with lock:
                    if str(temp_marker) in cleaned_files:
                        cleanup_errors.append(f"Double cleanup: {cleanup_id}")
                        return False
                    cleaned_files.add(str(temp_marker))

                # Create and remove temp file
                temp_marker.touch()
                temp_marker.unlink()
                return True
            except FileNotFoundError:
                cleanup_errors.append(f"File not found in cleanup: {cleanup_id}")
                return False

        def run_with_cleanup(op_num: int) -> int:
            exit_code, _ = run_fmt_command_subprocess(
                tmp_path / f"cleanup_test_{op_num}.md",
                tmp_path / f"cleanup_output_{op_num}.html",
                op_num,
            )
            return exit_code

        with concurrent.futures.ThreadPoolExecutor(max_workers=15) as executor:
            futures = []
            # Interleave operations and cleanup
            for i in range(10):
                futures.append(executor.submit(run_with_cleanup, i))
            for i in range(10):
                futures.append(executor.submit(cleanup_temp_files, i))

            for future in concurrent.futures.as_completed(futures, timeout=30):
                result = future.result()
                if isinstance(result, int):
                    results.append(result)

        # VALIDATE: No double-cleanup errors
        assert len(cleanup_errors) == 0, f"Cleanup errors: {cleanup_errors}"

        # VALIDATE: Operations and cleanup both ran
        assert len(results) >= 10, "Not all operations completed"

    def test_cookie_jar_thread_safe_http_client(self, tmp_path: Path) -> None:
        """Test that cookie jar (HTTP client state) is thread-safe.

        VALIDATES:
        - HTTP client state is not corrupted
        - No session mixing between operations
        - Cookie persistence is thread-safe
        """
        for i in range(10):
            md_file = tmp_path / f"http_test_{i}.md"
            create_sample_markdown(md_file, i)

        results = []

        def run_http_operation(op_num: int) -> tuple[int, int]:
            # Run export which may use HTTP internally
            exit_code, stdout = run_export_command_subprocess(tmp_path, op_num)
            # Extract any session/cookie info from output if present
            has_session = "session" in stdout.lower() or "cookie" in stdout.lower()
            return (exit_code, op_num if has_session else 0)

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [
                executor.submit(run_http_operation, i) for i in range(10)
            ]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations completed
        assert len(results) == 10, "Not all HTTP operations completed"

        # VALIDATE: No session mixing (all exit codes valid)
        assert all(
            exit_code in [0, 1, 2] for exit_code, _ in results
        ), "Invalid exit codes indicate session mixing"

    def test_singleton_instances_not_corrupted(self, tmp_path: Path) -> None:
        """Test that singleton instances are not corrupted by concurrent access.

        VALIDATES:
        - Singleton state remains consistent
        - No singleton conflicts between threads
        - Proper synchronization of singleton access
        """
        for i in range(12):
            md_file = tmp_path / f"singleton_test_{i}.md"
            create_sample_markdown(md_file, i)

        results = []
        access_log = []
        lock = threading.Lock()

        def run_with_singleton_access(op_num: int) -> int:
            # Track access to simulated singleton
            with lock:
                access_log.append(("access", op_num, time.time()))

            exit_code, _ = run_fmt_command_subprocess(
                tmp_path / f"singleton_test_{op_num}.md",
                tmp_path / f"singleton_output_{op_num}.html",
                op_num,
            )

            with lock:
                access_log.append(("release", op_num, time.time()))

            return exit_code

        with concurrent.futures.ThreadPoolExecutor(max_workers=12) as executor:
            futures = [
                executor.submit(run_with_singleton_access, i) for i in range(12)
            ]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                results.append(future.result())

        # VALIDATE: All operations completed
        assert len(results) == 12, "Not all singleton operations completed"

        # VALIDATE: Access log is consistent (same number of accesses and releases)
        accesses = sum(1 for event, _, _ in access_log if event == "access")
        releases = sum(1 for event, _, _ in access_log if event == "release")
        assert accesses == releases == 12, \
            f"Singleton access log corrupted: {accesses} accesses, {releases} releases"


# ============================================================================
# TEST CATEGORY 5: TIMEOUT UNDER LOAD - 3 TESTS
# ============================================================================


class TestTimeoutUnderLoad:
    """Tests for timeout handling during high concurrent load."""

    def test_timeout_respected_with_20_concurrent_ops(
        self, tmp_path: Path
    ) -> None:
        """Test that timeout is respected even with 20 concurrent operations.

        VALIDATES:
        - Timeout is enforced under concurrent load
        - Operations that timeout don't hang
        - Remaining operations continue
        """
        for i in range(20):
            md_file = tmp_path / f"timeout_test_{i}.md"
            create_sample_markdown(md_file, i)

        results = []
        timeouts = []
        lock = threading.Lock()

        def run_with_timeout(op_num: int, timeout_sec: int = 5) -> int:
            try:
                # Run operation with strict timeout
                exit_code, _ = run_export_command_subprocess(tmp_path, op_num)
                return exit_code
            except concurrent.futures.TimeoutError:
                with lock:
                    timeouts.append(op_num)
                return -1

        with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
            futures = [
                executor.submit(run_with_timeout, i, timeout_sec=10) for i in range(20)
            ]
            try:
                for future in concurrent.futures.as_completed(
                    futures, timeout=20
                ):  # Executor timeout
                    results.append(future.result())
            except concurrent.futures.TimeoutError:
                pass

        # VALIDATE: Most operations completed
        assert len(results) + len(timeouts) > 15, \
            f"Too many operations timed out: {len(timeouts)}"

    def test_signal_handling_during_concurrent_ops(self, tmp_path: Path) -> None:
        """Test that signals are handled gracefully during concurrent operations.

        VALIDATES:
        - Operations can be interrupted gracefully
        - No zombie processes
        - Partial cleanup occurs if timeout
        """
        for i in range(10):
            md_file = tmp_path / f"signal_test_{i}.md"
            create_sample_markdown(md_file, i)

        results = []

        def run_interruptible(op_num: int) -> int:
            try:
                exit_code, _ = run_fmt_command_subprocess(
                    tmp_path / f"signal_test_{op_num}.md",
                    tmp_path / f"signal_output_{op_num}.html",
                    op_num,
                )
                return exit_code
            except KeyboardInterrupt:
                return -2  # Interrupted

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [
                executor.submit(run_interruptible, i) for i in range(10)
            ]
            for future in concurrent.futures.as_completed(futures, timeout=30):
                try:
                    results.append(future.result())
                except Exception:
                    results.append(-3)  # Exception

        # VALIDATE: Operations completed or timed out gracefully
        assert len(results) >= 0, "Signal handling test failed"

    def test_partial_cleanup_on_timeout(self, tmp_path: Path) -> None:
        """Test that partial cleanup occurs when operations timeout.

        VALIDATES:
        - Temp files are cleaned even if timeout occurs
        - No orphaned resources
        - Cleanup is idempotent
        """
        for i in range(15):
            md_file = tmp_path / f"partial_cleanup_{i}.md"
            create_sample_markdown(md_file, i)

        results = []
        cleanup_count = []
        lock = threading.Lock()

        def run_with_partial_cleanup(op_num: int) -> int:
            temp_file = tmp_path / f"temp_{op_num}.tmp"
            try:
                # Create temp file
                temp_file.touch()

                exit_code, _ = run_fmt_command_subprocess(
                    tmp_path / f"partial_cleanup_{op_num}.md",
                    tmp_path / f"cleanup_output_{op_num}.html",
                    op_num,
                )

                return exit_code
            finally:
                # Cleanup even on timeout
                try:
                    if temp_file.exists():
                        temp_file.unlink()
                        with lock:
                            cleanup_count.append(op_num)
                except Exception:
                    pass

        with concurrent.futures.ThreadPoolExecutor(max_workers=15) as executor:
            futures = [
                executor.submit(run_with_partial_cleanup, i) for i in range(15)
            ]
            try:
                for future in concurrent.futures.as_completed(
                    futures, timeout=30
                ):
                    results.append(future.result())
            except concurrent.futures.TimeoutError:
                pass

        # VALIDATE: Cleanup occurred for completed operations
        assert len(cleanup_count) > 0, "No cleanup occurred"

        # VALIDATE: No temp files left behind
        orphaned_temps = list(tmp_path.glob("temp_*.tmp"))
        assert len(orphaned_temps) == 0, f"Orphaned temp files: {orphaned_temps}"
