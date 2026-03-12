"""Chicago-style TDD tests for `dtr diff` commands.

Tests real behavior with real collaborators -- no mocks, no fakes.
Uses typer CliRunner to invoke the real CLI app end-to-end,
and pytest tmp_path for real file I/O.
"""

from pathlib import Path

import pytest
from typer.testing import CliRunner

from dtr_cli.main import app
from dtr_cli.commands.diff import (
    _extract_text_from_html,
    _read_file_content,
    _collect_relative_files,
    _format_size,
)

runner = CliRunner()

# ---------------------------------------------------------------------------
# Sample content used across tests
# ---------------------------------------------------------------------------

MARKDOWN_V1 = """\
# API Documentation

This is version 1 of the API docs.

## Endpoints

- GET /api/users
- POST /api/users
"""

MARKDOWN_V2 = """\
# API Documentation

This is version 2 of the API docs.

## Endpoints

- GET /api/users
- POST /api/users
- DELETE /api/users/{id}
"""

HTML_V1 = """\
<!DOCTYPE html>
<html>
<head><title>Report</title></head>
<body>
  <h1>Report v1</h1>
  <p>Content of version one.</p>
  <script>alert('remove me')</script>
  <style>body { color: red; }</style>
</body>
</html>
"""

HTML_V2 = """\
<!DOCTYPE html>
<html>
<head><title>Report</title></head>
<body>
  <h1>Report v2</h1>
  <p>Content of version two.</p>
</body>
</html>
"""


# ===========================================================================
# Unit tests for helper functions
# ===========================================================================


class TestExtractTextFromHtml:
    """Test HTML text extraction strips tags and removes script/style."""

    def test_extracts_body_text(self):
        text = _extract_text_from_html(HTML_V1)
        assert "Report v1" in text
        assert "Content of version one." in text

    def test_removes_script_tags(self):
        text = _extract_text_from_html(HTML_V1)
        assert "alert" not in text

    def test_removes_style_tags(self):
        text = _extract_text_from_html(HTML_V1)
        assert "color: red" not in text

    def test_strips_html_markup(self):
        text = _extract_text_from_html(HTML_V1)
        assert "<h1>" not in text
        assert "<p>" not in text

    def test_empty_html_returns_empty_string(self):
        text = _extract_text_from_html("<html><body></body></html>")
        assert text == ""

    def test_plain_text_passthrough(self):
        # Non-HTML string is returned unchanged (no tags to strip)
        text = _extract_text_from_html("just plain text")
        assert "just plain text" in text


class TestReadFileContent:
    """Test file reading with whitespace and HTML extraction."""

    def test_reads_markdown_lines(self, tmp_path: Path):
        md = tmp_path / "doc.md"
        md.write_text(MARKDOWN_V1, encoding="utf-8")
        lines = _read_file_content(md)
        assert any("API Documentation" in line for line in lines)

    def test_reads_html_as_text(self, tmp_path: Path):
        html = tmp_path / "report.html"
        html.write_text(HTML_V1, encoding="utf-8")
        lines = _read_file_content(html)
        combined = "".join(lines)
        assert "Report v1" in combined
        assert "<h1>" not in combined

    def test_ignore_whitespace_strips_lines(self, tmp_path: Path):
        md = tmp_path / "doc.md"
        md.write_text("  hello  \n  world  \n", encoding="utf-8")
        lines = _read_file_content(md, ignore_whitespace=True)
        assert lines[0] == "hello\n"
        assert lines[1] == "world\n"

    def test_preserves_newlines_in_keepends(self, tmp_path: Path):
        md = tmp_path / "doc.md"
        md.write_text("line one\nline two\n", encoding="utf-8")
        lines = _read_file_content(md)
        assert all(line.endswith("\n") for line in lines if line)


class TestCollectRelativeFiles:
    """Test directory file collection returns relative paths."""

    def test_collects_all_files(self, tmp_path: Path):
        (tmp_path / "a.md").write_text("a", encoding="utf-8")
        (tmp_path / "b.html").write_text("b", encoding="utf-8")
        result = _collect_relative_files(tmp_path)
        assert "a.md" in result
        assert "b.html" in result

    def test_collects_nested_files(self, tmp_path: Path):
        sub = tmp_path / "subdir"
        sub.mkdir()
        (sub / "nested.md").write_text("n", encoding="utf-8")
        result = _collect_relative_files(tmp_path)
        # On Linux the separator is /
        assert any("nested.md" in k for k in result)

    def test_empty_directory_returns_empty_dict(self, tmp_path: Path):
        result = _collect_relative_files(tmp_path)
        assert result == {}

    def test_paths_are_absolute(self, tmp_path: Path):
        (tmp_path / "x.md").write_text("x", encoding="utf-8")
        result = _collect_relative_files(tmp_path)
        for abs_path in result.values():
            assert abs_path.is_absolute()


class TestFormatSize:
    """Test human-readable size formatting."""

    def test_formats_bytes(self):
        assert "B" in _format_size(512)

    def test_formats_kilobytes(self):
        assert "KB" in _format_size(2048)

    def test_formats_megabytes(self):
        assert "MB" in _format_size(2 * 1024 * 1024)

    def test_negative_size(self):
        result = _format_size(-512)
        assert "B" in result


# ===========================================================================
# Integration tests: `dtr diff dirs`
# ===========================================================================


class TestDiffDirs:
    """Test `dtr diff dirs` with real temp directories."""

    def _make_dir(self, base: Path, name: str, files: dict[str, str]) -> Path:
        d = base / name
        d.mkdir()
        for fname, content in files.items():
            (d / fname).write_text(content, encoding="utf-8")
        return d

    def test_detects_added_file(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"common.md": "hello"})
        new = self._make_dir(tmp_path, "new", {"common.md": "hello", "new.md": "world"})

        result = runner.invoke(app, ["diff", "dirs", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        assert "ADDED" in result.stdout
        assert "new.md" in result.stdout

    def test_detects_removed_file(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"common.md": "hello", "gone.md": "bye"})
        new = self._make_dir(tmp_path, "new", {"common.md": "hello"})

        result = runner.invoke(app, ["diff", "dirs", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        assert "REMOVED" in result.stdout
        assert "gone.md" in result.stdout

    def test_detects_modified_file(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"doc.md": MARKDOWN_V1})
        new = self._make_dir(tmp_path, "new", {"doc.md": MARKDOWN_V2})

        result = runner.invoke(app, ["diff", "dirs", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        assert "MODIFIED" in result.stdout
        assert "doc.md" in result.stdout

    def test_no_diff_when_identical(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"doc.md": MARKDOWN_V1})
        new = self._make_dir(tmp_path, "new", {"doc.md": MARKDOWN_V1})

        result = runner.invoke(app, ["diff", "dirs", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        assert "No differences" in result.stdout

    def test_summary_line_counts(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"a.md": "a", "b.md": "b"})
        new = self._make_dir(tmp_path, "new", {"a.md": "changed", "c.md": "c"})

        result = runner.invoke(app, ["diff", "dirs", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        # Summary must show added, removed, modified
        assert "added" in result.stdout.lower()
        assert "removed" in result.stdout.lower()
        assert "modified" in result.stdout.lower()

    def test_output_option_saves_to_file(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"doc.md": MARKDOWN_V1})
        new = self._make_dir(tmp_path, "new", {"doc.md": MARKDOWN_V2})
        out = tmp_path / "changes.diff"

        result = runner.invoke(app, ["diff", "dirs", str(old), str(new), "-o", str(out)])

        assert result.exit_code == 0, result.stdout
        assert out.exists(), "Output file was not created"
        content = out.read_text(encoding="utf-8")
        assert len(content) > 0

    def test_context_option_limits_lines(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"doc.md": MARKDOWN_V1})
        new = self._make_dir(tmp_path, "new", {"doc.md": MARKDOWN_V2})

        result_default = runner.invoke(app, ["diff", "dirs", str(old), str(new)])
        result_zero = runner.invoke(app, ["diff", "dirs", str(old), str(new), "--context", "0"])

        # With 0 context lines the output should be shorter
        assert result_zero.exit_code == 0
        assert len(result_zero.stdout) <= len(result_default.stdout)

    def test_ignore_whitespace_flag(self, tmp_path: Path):
        # --ignore-whitespace strips leading/trailing whitespace per line,
        # so "  hello  \n" and "hello\n" become identical after stripping.
        old = self._make_dir(tmp_path, "old", {"doc.md": "  hello  \n"})
        new = self._make_dir(tmp_path, "new", {"doc.md": "hello\n"})

        result = runner.invoke(
            app, ["diff", "dirs", str(old), str(new), "--ignore-whitespace"]
        )

        assert result.exit_code == 0, result.stdout
        assert "No differences" in result.stdout

    def test_fails_on_missing_dir_a(self, tmp_path: Path):
        old = tmp_path / "nonexistent"
        new = self._make_dir(tmp_path, "new", {"doc.md": "x"})

        result = runner.invoke(app, ["diff", "dirs", str(old), str(new)])

        assert result.exit_code != 0

    def test_fails_on_missing_dir_b(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"doc.md": "x"})
        new = tmp_path / "nonexistent"

        result = runner.invoke(app, ["diff", "dirs", str(old), str(new)])

        assert result.exit_code != 0


# ===========================================================================
# Integration tests: `dtr diff files`
# ===========================================================================


class TestDiffFiles:
    """Test `dtr diff files` with real file content."""

    def test_shows_diff_for_changed_markdown(self, tmp_path: Path):
        old = tmp_path / "v1.md"
        new = tmp_path / "v2.md"
        old.write_text(MARKDOWN_V1, encoding="utf-8")
        new.write_text(MARKDOWN_V2, encoding="utf-8")

        result = runner.invoke(app, ["diff", "files", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        # The added line should appear in diff output
        assert "DELETE" in result.stdout or "DELETE /api" in result.stdout or "delete" in result.stdout.lower() or "DELETE" in result.stdout or "+DELETE" in result.stdout or "users/{id}" in result.stdout

    def test_no_diff_for_identical_files(self, tmp_path: Path):
        f1 = tmp_path / "a.md"
        f2 = tmp_path / "b.md"
        f1.write_text(MARKDOWN_V1, encoding="utf-8")
        f2.write_text(MARKDOWN_V1, encoding="utf-8")

        result = runner.invoke(app, ["diff", "files", str(f1), str(f2)])

        assert result.exit_code == 0, result.stdout
        assert "No differences" in result.stdout

    def test_html_diff_extracts_text_not_tags(self, tmp_path: Path):
        old = tmp_path / "v1.html"
        new = tmp_path / "v2.html"
        old.write_text(HTML_V1, encoding="utf-8")
        new.write_text(HTML_V2, encoding="utf-8")

        result = runner.invoke(app, ["diff", "files", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        # The diff should show text changes, not HTML tag noise
        assert "DOCTYPE" not in result.stdout or "<html>" not in result.stdout

    def test_html_diff_shows_text_content_change(self, tmp_path: Path):
        old = tmp_path / "v1.html"
        new = tmp_path / "v2.html"
        old.write_text(HTML_V1, encoding="utf-8")
        new.write_text(HTML_V2, encoding="utf-8")

        result = runner.invoke(app, ["diff", "files", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        # version one and two text changes should be visible
        assert "v1" in result.stdout or "v2" in result.stdout

    def test_summary_shows_line_counts(self, tmp_path: Path):
        old = tmp_path / "v1.md"
        new = tmp_path / "v2.md"
        old.write_text(MARKDOWN_V1, encoding="utf-8")
        new.write_text(MARKDOWN_V2, encoding="utf-8")

        result = runner.invoke(app, ["diff", "files", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        assert "lines" in result.stdout.lower()

    def test_output_option_saves_diff_to_file(self, tmp_path: Path):
        old = tmp_path / "v1.md"
        new = tmp_path / "v2.md"
        old.write_text(MARKDOWN_V1, encoding="utf-8")
        new.write_text(MARKDOWN_V2, encoding="utf-8")
        out = tmp_path / "out.diff"

        result = runner.invoke(app, ["diff", "files", str(old), str(new), "-o", str(out)])

        assert result.exit_code == 0, result.stdout
        assert out.exists(), "Output diff file was not created"
        diff_content = out.read_text(encoding="utf-8")
        assert len(diff_content) > 0

    def test_ignore_whitespace_treats_spaces_as_equal(self, tmp_path: Path):
        old = tmp_path / "a.md"
        new = tmp_path / "b.md"
        old.write_text("same line\n", encoding="utf-8")
        new.write_text("  same line  \n", encoding="utf-8")

        result = runner.invoke(app, ["diff", "files", str(old), str(new), "--ignore-whitespace"])

        assert result.exit_code == 0, result.stdout
        assert "No differences" in result.stdout

    def test_context_option_respected(self, tmp_path: Path):
        # Write a file with many lines and change one in the middle
        lines_v1 = [f"line {i}\n" for i in range(20)]
        lines_v2 = lines_v1.copy()
        lines_v2[10] = "changed line\n"
        old = tmp_path / "a.md"
        new = tmp_path / "b.md"
        old.write_text("".join(lines_v1), encoding="utf-8")
        new.write_text("".join(lines_v2), encoding="utf-8")

        result_c1 = runner.invoke(app, ["diff", "files", str(old), str(new), "--context", "1"])
        result_c5 = runner.invoke(app, ["diff", "files", str(old), str(new), "--context", "5"])

        assert result_c1.exit_code == 0
        assert result_c5.exit_code == 0
        # More context = more output lines
        assert len(result_c5.stdout) >= len(result_c1.stdout)

    def test_fails_on_missing_file_a(self, tmp_path: Path):
        missing = tmp_path / "nonexistent.md"
        existing = tmp_path / "exists.md"
        existing.write_text("x", encoding="utf-8")

        result = runner.invoke(app, ["diff", "files", str(missing), str(existing)])

        assert result.exit_code != 0

    def test_fails_on_missing_file_b(self, tmp_path: Path):
        existing = tmp_path / "exists.md"
        existing.write_text("x", encoding="utf-8")
        missing = tmp_path / "nonexistent.md"

        result = runner.invoke(app, ["diff", "files", str(existing), str(missing)])

        assert result.exit_code != 0


# ===========================================================================
# Integration tests: `dtr diff summary`
# ===========================================================================


class TestDiffSummary:
    """Test `dtr diff summary` with real temp directories."""

    def _make_dir(self, base: Path, name: str, files: dict[str, str]) -> Path:
        d = base / name
        d.mkdir()
        for fname, content in files.items():
            (d / fname).write_text(content, encoding="utf-8")
        return d

    def test_reports_added_removed_modified_counts(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {
            "keep.md": "unchanged",
            "modify.md": "original",
            "remove.md": "gone",
        })
        new = self._make_dir(tmp_path, "new", {
            "keep.md": "unchanged",
            "modify.md": "changed",
            "added.md": "new file",
        })

        result = runner.invoke(app, ["diff", "summary", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        assert "Added" in result.stdout or "added" in result.stdout.lower()
        assert "Removed" in result.stdout or "removed" in result.stdout.lower()
        assert "Modified" in result.stdout or "modified" in result.stdout.lower()

    def test_shows_individual_file_names(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"gone.md": "x"})
        new = self._make_dir(tmp_path, "new", {"new.md": "y"})

        result = runner.invoke(app, ["diff", "summary", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        assert "gone.md" in result.stdout
        assert "new.md" in result.stdout

    def test_shows_size_information(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"a.md": "content"})
        new = self._make_dir(tmp_path, "new", {"a.md": "more content here"})

        result = runner.invoke(app, ["diff", "summary", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        # Should show total size info
        assert "size" in result.stdout.lower() or "B" in result.stdout

    def test_output_option_saves_summary_to_file(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"a.md": "old"})
        new = self._make_dir(tmp_path, "new", {"a.md": "new"})
        out = tmp_path / "summary.txt"

        result = runner.invoke(app, ["diff", "summary", str(old), str(new), "-o", str(out)])

        assert result.exit_code == 0, result.stdout
        assert out.exists(), "Summary output file was not created"
        content = out.read_text(encoding="utf-8")
        assert "DTR Diff Summary" in content

    def test_identical_dirs_show_zero_changes(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"a.md": "same"})
        new = self._make_dir(tmp_path, "new", {"a.md": "same"})

        result = runner.invoke(app, ["diff", "summary", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        assert "0" in result.stdout

    def test_fails_on_missing_dir_a(self, tmp_path: Path):
        old = tmp_path / "nonexistent"
        new = self._make_dir(tmp_path, "new", {"doc.md": "x"})

        result = runner.invoke(app, ["diff", "summary", str(old), str(new)])

        assert result.exit_code != 0

    def test_fails_on_missing_dir_b(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"doc.md": "x"})
        new = tmp_path / "nonexistent"

        result = runner.invoke(app, ["diff", "summary", str(old), str(new)])

        assert result.exit_code != 0

    def test_shows_reference_and_comparison_paths(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old_exports", {"doc.md": "x"})
        new = self._make_dir(tmp_path, "new_exports", {"doc.md": "x"})

        result = runner.invoke(app, ["diff", "summary", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        # Summary should mention both directories
        assert "old_exports" in result.stdout or str(old) in result.stdout
        assert "new_exports" in result.stdout or str(new) in result.stdout

    def test_unchanged_count_reported(self, tmp_path: Path):
        old = self._make_dir(tmp_path, "old", {"a.md": "same", "b.md": "change"})
        new = self._make_dir(tmp_path, "new", {"a.md": "same", "b.md": "different"})

        result = runner.invoke(app, ["diff", "summary", str(old), str(new)])

        assert result.exit_code == 0, result.stdout
        assert "Unchanged" in result.stdout or "unchanged" in result.stdout.lower()
