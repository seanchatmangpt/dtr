"""Chicago-style TDD tests for format converters.

Tests real behavior with real file I/O -- no mocks, no fakes.
Every converter is exercised against actual temp files and verified
by inspecting the output content.
"""

import json
import subprocess
from pathlib import Path

import pytest

from dtr_cli.cli_errors import ConversionError, LatexCompilationError
from dtr_cli.converters.base_converter import BaseConverter
from dtr_cli.converters.html_converter import HtmlConverter
from dtr_cli.converters.json_converter import JsonConverter
from dtr_cli.converters.latex_converter import LatexConverter
from dtr_cli.converters.markdown_converter import MarkdownConverter
from dtr_cli.converters.pdf_converter import PdfConverter
from dtr_cli.model import (
    CompilerStrategy,
    ConversionConfig,
    ConversionResult,
    LatexExportConfig,
    LatexTemplate,
    PdfExportConfig,
)


# ---------------------------------------------------------------------------
# Helpers / fixtures
# ---------------------------------------------------------------------------

SAMPLE_HTML = """\
<html>
<head><title>Test Doc</title></head>
<body>
<h1>Introduction</h1>
<p>This is the first paragraph.</p>
<h2>Details</h2>
<p>More content here.</p>
<pre><code>print("hello")</code></pre>
<ul>
<li>Item one</li>
<li>Item two</li>
</ul>
<ol>
<li>First</li>
<li>Second</li>
</ol>
<table>
<tr><th>Name</th><th>Value</th></tr>
<tr><td>alpha</td><td>1</td></tr>
<tr><td>beta</td><td>2</td></tr>
</table>
</body>
</html>
"""

SAMPLE_MARKDOWN = """\
# My Document

This is a paragraph with **bold** and *italic* text.

## Code Example

```python
def hello():
    print("world")
```

## Table

| Col A | Col B |
|-------|-------|
| 1     | 2     |

## List

- apple
- banana
- cherry
"""

MINIMAL_HTML = "<html><body><h1>Hello</h1><p>World</p></body></html>"


@pytest.fixture
def html_file(tmp_path: Path) -> Path:
    """Create a sample HTML file for conversion."""
    f = tmp_path / "sample.html"
    f.write_text(SAMPLE_HTML, encoding="utf-8")
    return f


@pytest.fixture
def md_file(tmp_path: Path) -> Path:
    """Create a sample Markdown file for conversion."""
    f = tmp_path / "sample.md"
    f.write_text(SAMPLE_MARKDOWN, encoding="utf-8")
    return f


@pytest.fixture
def output_dir(tmp_path: Path) -> Path:
    """Create an output directory."""
    d = tmp_path / "output"
    d.mkdir()
    return d


def _make_config(
    input_path: Path,
    output_path: Path,
    *,
    recursive: bool = False,
    force: bool = False,
    pretty: bool = True,
    template: str | None = None,
) -> ConversionConfig:
    return ConversionConfig(
        input_path=input_path,
        output_path=output_path,
        recursive=recursive,
        force=force,
        pretty=pretty,
        template=template,
    )


# ---------------------------------------------------------------------------
# BaseConverter abstract interface
# ---------------------------------------------------------------------------


class TestBaseConverter:
    """Tests for the abstract BaseConverter contract."""

    def test_cannot_instantiate_directly(self) -> None:
        """BaseConverter is abstract and must not be instantiated."""
        with pytest.raises(TypeError):
            BaseConverter()  # type: ignore

    def test_get_input_files_single_file(self, html_file: Path, output_dir: Path) -> None:
        """get_input_files returns a single-element list for a file path."""
        converter = HtmlConverter()  # concrete subclass to test inherited method
        config = _make_config(html_file, output_dir)
        files = converter.get_input_files(config)
        assert files == [html_file]

    def test_get_input_files_directory(self, tmp_path: Path) -> None:
        """get_input_files returns all files in a directory (non-recursive)."""
        src = tmp_path / "src"
        src.mkdir()
        (src / "a.html").write_text("<p>a</p>")
        (src / "b.html").write_text("<p>b</p>")
        sub = src / "sub"
        sub.mkdir()
        (sub / "c.html").write_text("<p>c</p>")

        converter = HtmlConverter()
        config = _make_config(src, tmp_path / "out", recursive=False)
        files = converter.get_input_files(config)
        names = {f.name for f in files}
        assert "a.html" in names
        assert "b.html" in names
        # sub directory itself may appear, but c.html inside sub should not
        assert "c.html" not in names

    def test_get_input_files_recursive(self, tmp_path: Path) -> None:
        """get_input_files with recursive=True traverses subdirectories."""
        src = tmp_path / "src"
        src.mkdir()
        (src / "a.html").write_text("<p>a</p>")
        sub = src / "sub"
        sub.mkdir()
        (sub / "b.html").write_text("<p>b</p>")

        converter = HtmlConverter()
        config = _make_config(src, tmp_path / "out", recursive=True)
        files = converter.get_input_files(config)
        names = {f.name for f in files}
        assert "a.html" in names
        assert "b.html" in names

    def test_should_overwrite_nonexistent_file(self, tmp_path: Path) -> None:
        """should_overwrite returns True if output does not exist."""
        converter = HtmlConverter()
        assert converter.should_overwrite(tmp_path / "nope.md", force=False) is True

    def test_should_overwrite_existing_no_force(self, tmp_path: Path) -> None:
        """should_overwrite returns False for existing file when force=False."""
        existing = tmp_path / "existing.md"
        existing.write_text("old")
        converter = HtmlConverter()
        assert converter.should_overwrite(existing, force=False) is False

    def test_should_overwrite_existing_with_force(self, tmp_path: Path) -> None:
        """should_overwrite returns True for existing file when force=True."""
        existing = tmp_path / "existing.md"
        existing.write_text("old")
        converter = HtmlConverter()
        assert converter.should_overwrite(existing, force=True) is True


# ---------------------------------------------------------------------------
# ConversionResult / ConversionConfig model basics
# ---------------------------------------------------------------------------


class TestModels:
    """Verify model dataclass defaults and field access."""

    def test_conversion_result_defaults(self) -> None:
        result = ConversionResult(files_processed=3)
        assert result.files_processed == 3
        assert result.files_failed == 0
        assert result.warnings == []
        assert result.errors == []

    def test_conversion_result_with_warnings(self) -> None:
        result = ConversionResult(files_processed=1, warnings=["w1", "w2"])
        assert len(result.warnings) == 2

    def test_conversion_config_defaults(self, tmp_path: Path) -> None:
        config = ConversionConfig(input_path=tmp_path, output_path=tmp_path)
        assert config.recursive is False
        assert config.force is False
        assert config.pretty is True
        assert config.template is None


# ---------------------------------------------------------------------------
# HtmlConverter -- HTML to Markdown
# ---------------------------------------------------------------------------


class TestHtmlConverter:
    """Chicago-style tests for HTML-to-Markdown conversion."""

    def test_basic_conversion(self, html_file: Path, output_dir: Path) -> None:
        """Convert a single HTML file and verify output exists."""
        converter = HtmlConverter()
        config = _make_config(html_file, output_dir)
        result = converter.convert(config)

        assert result.files_processed == 1
        out = output_dir / "sample.md"
        assert out.exists()
        content = out.read_text(encoding="utf-8")
        assert len(content) > 0

    def test_headings_converted(self, html_file: Path, output_dir: Path) -> None:
        """HTML headings become Markdown headings."""
        converter = HtmlConverter()
        result = converter.convert(_make_config(html_file, output_dir))
        content = (output_dir / "sample.md").read_text()
        assert "# Introduction" in content or "## Introduction" in content
        assert "## Details" in content or "### Details" in content

    def test_paragraph_text_preserved(self, html_file: Path, output_dir: Path) -> None:
        """Paragraph text appears in the Markdown output."""
        HtmlConverter().convert(_make_config(html_file, output_dir))
        content = (output_dir / "sample.md").read_text()
        assert "This is the first paragraph." in content
        assert "More content here." in content

    def test_code_block_converted(self, html_file: Path, output_dir: Path) -> None:
        """<pre><code> becomes a fenced code block."""
        HtmlConverter().convert(_make_config(html_file, output_dir))
        content = (output_dir / "sample.md").read_text()
        assert "```" in content
        assert 'print("hello")' in content

    def test_unordered_list_converted(self, html_file: Path, output_dir: Path) -> None:
        """<ul> items become bullet points."""
        HtmlConverter().convert(_make_config(html_file, output_dir))
        content = (output_dir / "sample.md").read_text()
        assert "- Item one" in content
        assert "- Item two" in content

    def test_ordered_list_converted(self, html_file: Path, output_dir: Path) -> None:
        """<ol> items become numbered list."""
        HtmlConverter().convert(_make_config(html_file, output_dir))
        content = (output_dir / "sample.md").read_text()
        assert "1. First" in content
        assert "1. Second" in content

    def test_table_converted(self, html_file: Path, output_dir: Path) -> None:
        """HTML table becomes a Markdown table."""
        HtmlConverter().convert(_make_config(html_file, output_dir))
        content = (output_dir / "sample.md").read_text()
        assert "Name" in content
        assert "Value" in content
        assert "alpha" in content
        assert "|" in content

    def test_title_extracted(self, html_file: Path, output_dir: Path) -> None:
        """<title> becomes a top-level heading."""
        HtmlConverter().convert(_make_config(html_file, output_dir))
        content = (output_dir / "sample.md").read_text()
        assert "# Test Doc" in content

    def test_empty_html_file(self, tmp_path: Path) -> None:
        """Converting an empty HTML file produces output without errors."""
        empty = tmp_path / "empty.html"
        empty.write_text("", encoding="utf-8")
        out = tmp_path / "out"
        out.mkdir()
        result = HtmlConverter().convert(_make_config(empty, out))
        assert result.files_processed == 1
        assert (out / "empty.md").exists()

    def test_skip_existing_without_force(self, html_file: Path, output_dir: Path) -> None:
        """Existing output file is skipped when force=False."""
        existing = output_dir / "sample.md"
        existing.write_text("original content")
        config = _make_config(html_file, output_dir, force=False)
        result = HtmlConverter().convert(config)

        assert result.files_processed == 0
        assert len(result.warnings) == 1
        assert "Skipped" in result.warnings[0]
        # Content should be unchanged
        assert existing.read_text() == "original content"

    def test_overwrite_existing_with_force(self, html_file: Path, output_dir: Path) -> None:
        """Existing output file is overwritten when force=True."""
        existing = output_dir / "sample.md"
        existing.write_text("original content")
        config = _make_config(html_file, output_dir, force=True)
        result = HtmlConverter().convert(config)

        assert result.files_processed == 1
        assert existing.read_text() != "original content"

    def test_directory_with_multiple_html_files(self, tmp_path: Path) -> None:
        """Convert a directory containing multiple HTML files."""
        src = tmp_path / "src"
        src.mkdir()
        (src / "one.html").write_text(MINIMAL_HTML)
        (src / "two.html").write_text(MINIMAL_HTML)
        (src / "readme.txt").write_text("not html")  # should be ignored
        out = tmp_path / "out"
        out.mkdir()

        result = HtmlConverter().convert(_make_config(src, out))
        assert result.files_processed == 2
        assert (out / "one.md").exists()
        assert (out / "two.md").exists()

    def test_non_html_files_ignored(self, tmp_path: Path) -> None:
        """Non-HTML files in a directory are silently skipped."""
        src = tmp_path / "src"
        src.mkdir()
        (src / "data.json").write_text('{"a":1}')
        (src / "notes.txt").write_text("text")
        out = tmp_path / "out"
        out.mkdir()

        result = HtmlConverter().convert(_make_config(src, out))
        assert result.files_processed == 0

    def test_htm_extension_accepted(self, tmp_path: Path) -> None:
        """.htm files are processed alongside .html."""
        src = tmp_path / "src"
        src.mkdir()
        (src / "page.htm").write_text(MINIMAL_HTML)
        out = tmp_path / "out"
        out.mkdir()

        result = HtmlConverter().convert(_make_config(src, out))
        assert result.files_processed == 1
        assert (out / "page.md").exists()

    def test_html_with_no_body(self, tmp_path: Path) -> None:
        """HTML without a body tag still converts gracefully."""
        f = tmp_path / "nobody.html"
        f.write_text("<h1>Title</h1><p>Content</p>")
        out = tmp_path / "out"
        out.mkdir()
        result = HtmlConverter().convert(_make_config(f, out))
        assert result.files_processed == 1
        content = (out / "nobody.md").read_text()
        assert "Title" in content

    def test_recursive_html_conversion(self, tmp_path: Path) -> None:
        """Recursive mode picks up HTML files in subdirectories."""
        src = tmp_path / "src"
        sub = src / "sub"
        sub.mkdir(parents=True)
        (src / "top.html").write_text(MINIMAL_HTML)
        (sub / "nested.html").write_text(MINIMAL_HTML)
        out = tmp_path / "out"
        out.mkdir()

        result = HtmlConverter().convert(_make_config(src, out, recursive=True))
        assert result.files_processed == 2


# ---------------------------------------------------------------------------
# JsonConverter -- HTML to JSON
# ---------------------------------------------------------------------------


class TestJsonConverter:
    """Chicago-style tests for HTML-to-JSON conversion."""

    def test_basic_conversion(self, html_file: Path, output_dir: Path) -> None:
        """Convert HTML to JSON and verify valid JSON output."""
        converter = JsonConverter()
        result = converter.convert(_make_config(html_file, output_dir))
        assert result.files_processed == 1

        out = output_dir / "sample.json"
        assert out.exists()
        data = json.loads(out.read_text(encoding="utf-8"))
        assert isinstance(data, dict)

    def test_json_has_required_keys(self, html_file: Path, output_dir: Path) -> None:
        """Output JSON contains file, title, sections, metadata keys."""
        JsonConverter().convert(_make_config(html_file, output_dir))
        data = json.loads((output_dir / "sample.json").read_text())
        assert "file" in data
        assert "title" in data
        assert "sections" in data
        assert "metadata" in data

    def test_title_extracted(self, html_file: Path, output_dir: Path) -> None:
        """JSON title matches the HTML <title>."""
        JsonConverter().convert(_make_config(html_file, output_dir))
        data = json.loads((output_dir / "sample.json").read_text())
        assert data["title"] == "Test Doc"

    def test_sections_populated(self, html_file: Path, output_dir: Path) -> None:
        """Sections list is non-empty when HTML has h1 elements."""
        JsonConverter().convert(_make_config(html_file, output_dir))
        data = json.loads((output_dir / "sample.json").read_text())
        assert len(data["sections"]) >= 1
        section = data["sections"][0]
        assert "title" in section
        assert "content" in section

    def test_section_content_types(self, html_file: Path, output_dir: Path) -> None:
        """Section content items have a 'type' field."""
        JsonConverter().convert(_make_config(html_file, output_dir))
        data = json.loads((output_dir / "sample.json").read_text())
        for section in data["sections"]:
            for item in section["content"]:
                assert "type" in item
                assert item["type"] in ("heading", "paragraph", "code", "table")

    def test_table_data_structure(self, html_file: Path, output_dir: Path) -> None:
        """Table content contains headers and rows."""
        JsonConverter().convert(_make_config(html_file, output_dir))
        data = json.loads((output_dir / "sample.json").read_text())
        table_items = [
            item
            for section in data["sections"]
            for item in section["content"]
            if item["type"] == "table"
        ]
        assert len(table_items) >= 1
        table_data = table_items[0]["data"]
        assert "headers" in table_data
        assert "rows" in table_data
        assert table_data["headers"] == ["Name", "Value"]
        assert ["alpha", "1"] in table_data["rows"]

    def test_code_block_in_json(self, html_file: Path, output_dir: Path) -> None:
        """Code blocks appear as type=code in JSON."""
        JsonConverter().convert(_make_config(html_file, output_dir))
        data = json.loads((output_dir / "sample.json").read_text())
        code_items = [
            item
            for section in data["sections"]
            for item in section["content"]
            if item["type"] == "code"
        ]
        assert len(code_items) >= 1
        assert 'print("hello")' in code_items[0]["text"]

    def test_pretty_json_output(self, html_file: Path, output_dir: Path) -> None:
        """With pretty=True (default), JSON is indented."""
        JsonConverter().convert(_make_config(html_file, output_dir, pretty=True))
        raw = (output_dir / "sample.json").read_text()
        # Indented JSON has newlines and leading spaces
        assert "\n" in raw
        assert "  " in raw

    def test_compact_json_output(self, html_file: Path, output_dir: Path) -> None:
        """With pretty=False, JSON is compact (no extra whitespace)."""
        JsonConverter().convert(_make_config(html_file, output_dir, pretty=False))
        raw = (output_dir / "sample.json").read_text()
        # Compact JSON has no leading indentation
        assert "\n  " not in raw

    def test_skip_existing_without_force(self, html_file: Path, output_dir: Path) -> None:
        """Existing JSON file is skipped without force."""
        existing = output_dir / "sample.json"
        existing.write_text('{"old": true}')
        result = JsonConverter().convert(_make_config(html_file, output_dir, force=False))
        assert result.files_processed == 0
        assert len(result.warnings) == 1
        assert json.loads(existing.read_text()) == {"old": True}

    def test_overwrite_existing_with_force(self, html_file: Path, output_dir: Path) -> None:
        """Existing JSON file is overwritten with force."""
        existing = output_dir / "sample.json"
        existing.write_text('{"old": true}')
        result = JsonConverter().convert(_make_config(html_file, output_dir, force=True))
        assert result.files_processed == 1
        data = json.loads(existing.read_text())
        assert "old" not in data

    def test_empty_html_produces_valid_json(self, tmp_path: Path) -> None:
        """Empty HTML file still produces valid JSON."""
        f = tmp_path / "empty.html"
        f.write_text("", encoding="utf-8")
        out = tmp_path / "out"
        out.mkdir()
        JsonConverter().convert(_make_config(f, out))
        data = json.loads((out / "empty.json").read_text())
        assert isinstance(data, dict)
        assert data["title"] == ""

    def test_file_name_in_json(self, html_file: Path, output_dir: Path) -> None:
        """JSON 'file' field matches the input filename."""
        JsonConverter().convert(_make_config(html_file, output_dir))
        data = json.loads((output_dir / "sample.json").read_text())
        assert data["file"] == "sample.html"

    def test_multiple_html_files(self, tmp_path: Path) -> None:
        """Convert multiple HTML files from a directory."""
        src = tmp_path / "src"
        src.mkdir()
        (src / "a.html").write_text(MINIMAL_HTML)
        (src / "b.html").write_text(MINIMAL_HTML)
        out = tmp_path / "out"
        out.mkdir()
        result = JsonConverter().convert(_make_config(src, out))
        assert result.files_processed == 2
        assert (out / "a.json").exists()
        assert (out / "b.json").exists()


# ---------------------------------------------------------------------------
# MarkdownConverter -- Markdown to HTML
# ---------------------------------------------------------------------------


class TestMarkdownConverter:
    """Chicago-style tests for Markdown-to-HTML conversion."""

    def test_basic_conversion(self, md_file: Path, output_dir: Path) -> None:
        """Convert Markdown to HTML and verify output exists."""
        result = MarkdownConverter().convert(_make_config(md_file, output_dir))
        assert result.files_processed == 1
        out = output_dir / "sample.html"
        assert out.exists()
        content = out.read_text(encoding="utf-8")
        assert "<html>" in content.lower() or "<!doctype" in content.lower()

    def test_default_template_applied(self, md_file: Path, output_dir: Path) -> None:
        """Default template includes styled body and meta viewport."""
        MarkdownConverter().convert(_make_config(md_file, output_dir))
        content = (output_dir / "sample.html").read_text()
        assert "viewport" in content
        assert "<style>" in content

    def test_minimal_template(self, md_file: Path, output_dir: Path) -> None:
        """Minimal template has no style block."""
        MarkdownConverter().convert(
            _make_config(md_file, output_dir, template="minimal")
        )
        content = (output_dir / "sample.html").read_text()
        assert "<style>" not in content
        assert "<title>" in content

    def test_github_template(self, md_file: Path, output_dir: Path) -> None:
        """GitHub template uses markdown-body class."""
        MarkdownConverter().convert(
            _make_config(md_file, output_dir, template="github")
        )
        content = (output_dir / "sample.html").read_text()
        assert "markdown-body" in content
        assert "github-markdown" in content

    def test_unknown_template_falls_back_to_default(
        self, md_file: Path, output_dir: Path
    ) -> None:
        """Unknown template name falls back to the default template."""
        MarkdownConverter().convert(
            _make_config(md_file, output_dir, template="nonexistent")
        )
        content = (output_dir / "sample.html").read_text()
        # Should use default which has viewport meta
        assert "viewport" in content

    def test_heading_converted_to_html(self, md_file: Path, output_dir: Path) -> None:
        """Markdown headings become HTML heading tags."""
        MarkdownConverter().convert(_make_config(md_file, output_dir))
        content = (output_dir / "sample.html").read_text()
        assert "<h1" in content
        assert "My Document" in content

    def test_bold_and_italic(self, md_file: Path, output_dir: Path) -> None:
        """Bold and italic Markdown renders as <strong> and <em>."""
        MarkdownConverter().convert(_make_config(md_file, output_dir))
        content = (output_dir / "sample.html").read_text()
        assert "<strong>" in content or "<b>" in content
        assert "<em>" in content or "<i>" in content

    def test_code_block_in_html(self, md_file: Path, output_dir: Path) -> None:
        """Fenced code block appears in HTML output."""
        MarkdownConverter().convert(_make_config(md_file, output_dir))
        content = (output_dir / "sample.html").read_text()
        assert "hello" in content
        assert "<code" in content or "<pre" in content

    def test_table_in_html(self, md_file: Path, output_dir: Path) -> None:
        """Markdown table renders as an HTML table."""
        MarkdownConverter().convert(_make_config(md_file, output_dir))
        content = (output_dir / "sample.html").read_text()
        assert "<table" in content
        assert "Col A" in content

    def test_list_in_html(self, md_file: Path, output_dir: Path) -> None:
        """Markdown unordered list becomes <ul>."""
        MarkdownConverter().convert(_make_config(md_file, output_dir))
        content = (output_dir / "sample.html").read_text()
        assert "<ul>" in content or "<li>" in content
        assert "apple" in content

    def test_title_from_filename(self, md_file: Path, output_dir: Path) -> None:
        """Title in HTML is derived from the Markdown filename."""
        MarkdownConverter().convert(_make_config(md_file, output_dir))
        content = (output_dir / "sample.html").read_text()
        assert "<title>" in content
        assert "Sample" in content

    def test_title_from_dashed_filename(self, tmp_path: Path) -> None:
        """Dashes and underscores in filename become spaces in title."""
        f = tmp_path / "my-great_doc.md"
        f.write_text("# Hello")
        out = tmp_path / "out"
        out.mkdir()
        MarkdownConverter().convert(_make_config(f, out))
        content = (out / "my-great_doc.html").read_text()
        assert "My Great Doc" in content

    def test_empty_markdown_file(self, tmp_path: Path) -> None:
        """Empty Markdown file produces a valid (if minimal) HTML page."""
        f = tmp_path / "empty.md"
        f.write_text("", encoding="utf-8")
        out = tmp_path / "out"
        out.mkdir()
        result = MarkdownConverter().convert(_make_config(f, out))
        assert result.files_processed == 1
        content = (out / "empty.html").read_text()
        assert "</html>" in content

    def test_skip_existing_without_force(self, md_file: Path, output_dir: Path) -> None:
        """Existing HTML file is not overwritten without force."""
        existing = output_dir / "sample.html"
        existing.write_text("<p>old</p>")
        result = MarkdownConverter().convert(_make_config(md_file, output_dir, force=False))
        assert result.files_processed == 0
        assert existing.read_text() == "<p>old</p>"

    def test_overwrite_with_force(self, md_file: Path, output_dir: Path) -> None:
        """Existing HTML file is overwritten when force=True."""
        existing = output_dir / "sample.html"
        existing.write_text("<p>old</p>")
        result = MarkdownConverter().convert(_make_config(md_file, output_dir, force=True))
        assert result.files_processed == 1
        assert existing.read_text() != "<p>old</p>"

    def test_multiple_markdown_files(self, tmp_path: Path) -> None:
        """Convert multiple Markdown files from a directory."""
        src = tmp_path / "src"
        src.mkdir()
        (src / "one.md").write_text("# One")
        (src / "two.md").write_text("# Two")
        (src / "skip.txt").write_text("not markdown")
        out = tmp_path / "out"
        out.mkdir()

        result = MarkdownConverter().convert(_make_config(src, out))
        assert result.files_processed == 2
        assert (out / "one.html").exists()
        assert (out / "two.html").exists()

    def test_markdown_extension_accepted(self, tmp_path: Path) -> None:
        """.markdown extension is recognized."""
        f = tmp_path / "doc.markdown"
        f.write_text("# Test")
        out = tmp_path / "out"
        out.mkdir()
        result = MarkdownConverter().convert(_make_config(f, out))
        assert result.files_processed == 1
        assert (out / "doc.html").exists()


# ---------------------------------------------------------------------------
# LatexConverter -- Markdown/HTML to LaTeX (requires pandoc)
# ---------------------------------------------------------------------------


def _pandoc_available() -> bool:
    """Check whether pandoc is installed."""
    try:
        subprocess.run(
            ["pandoc", "--version"], capture_output=True, check=True, timeout=5
        )
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False


@pytest.mark.skipif(not _pandoc_available(), reason="pandoc not installed")
class TestLatexConverter:
    """Chicago-style tests for LaTeX conversion (pandoc required)."""

    def test_markdown_to_latex(self, md_file: Path, tmp_path: Path) -> None:
        """Convert Markdown to LaTeX and verify .tex output."""
        out = tmp_path / "output.tex"
        config = LatexExportConfig(input_path=md_file, output_path=out)
        result = LatexConverter().convert(config)
        assert result.files_processed == 1
        assert out.exists()
        content = out.read_text()
        assert "\\begin{document}" in content or "\\documentclass" in content

    def test_html_to_latex(self, html_file: Path, tmp_path: Path) -> None:
        """Convert HTML to LaTeX."""
        out = tmp_path / "output.tex"
        config = LatexExportConfig(input_path=html_file, output_path=out)
        result = LatexConverter().convert(config)
        assert result.files_processed == 1
        assert out.exists()
        content = out.read_text()
        assert "\\begin{document}" in content or "\\documentclass" in content

    def test_latex_passthrough(self, tmp_path: Path) -> None:
        """A .tex input file is copied to the output location."""
        tex = tmp_path / "input.tex"
        tex.write_text("\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}")
        out = tmp_path / "out" / "copy.tex"
        config = LatexExportConfig(input_path=tex, output_path=out)
        result = LatexConverter().convert(config)
        assert result.files_processed == 1
        assert out.exists()
        assert "Hello" in out.read_text()

    def test_missing_input_raises(self, tmp_path: Path) -> None:
        """ConversionError raised for missing input file."""
        config = LatexExportConfig(input_path=tmp_path / "nope.md")
        with pytest.raises(ConversionError):
            LatexConverter().convert(config)

    def test_unsupported_format_raises(self, tmp_path: Path) -> None:
        """ConversionError raised for unsupported input extensions."""
        f = tmp_path / "data.csv"
        f.write_text("a,b,c")
        config = LatexExportConfig(input_path=f)
        with pytest.raises(ConversionError):
            LatexConverter().convert(config)

    def test_skip_existing_without_force(self, md_file: Path, tmp_path: Path) -> None:
        """Existing .tex file is not overwritten without force."""
        out = tmp_path / "output.tex"
        out.write_text("old content")
        config = LatexExportConfig(input_path=md_file, output_path=out, force=False)
        result = LatexConverter().convert(config)
        assert result.files_processed == 0
        assert out.read_text() == "old content"

    def test_overwrite_with_force(self, md_file: Path, tmp_path: Path) -> None:
        """Existing .tex file is overwritten with force=True."""
        out = tmp_path / "output.tex"
        out.write_text("old content")
        config = LatexExportConfig(input_path=md_file, output_path=out, force=True)
        result = LatexConverter().convert(config)
        assert result.files_processed == 1
        assert out.read_text() != "old content"

    def test_default_output_path(self, md_file: Path) -> None:
        """When output_path is None, .tex file is created next to input."""
        config = LatexExportConfig(input_path=md_file, output_path=None)
        result = LatexConverter().convert(config)
        assert result.files_processed == 1
        expected = md_file.parent / f"{md_file.stem}.tex"
        assert expected.exists()


# ---------------------------------------------------------------------------
# PdfConverter -- LaTeX to PDF (requires a LaTeX compiler)
# ---------------------------------------------------------------------------


def _any_latex_compiler_available() -> bool:
    """Check whether any LaTeX compiler is available."""
    for cmd in ["latexmk", "pdflatex", "xelatex", "pandoc"]:
        try:
            subprocess.run(
                [cmd, "--version"], capture_output=True, check=True, timeout=5
            )
            return True
        except (subprocess.CalledProcessError, FileNotFoundError):
            continue
    return False


@pytest.mark.skipif(
    not _any_latex_compiler_available(), reason="no LaTeX compiler installed"
)
class TestPdfConverter:
    """Chicago-style tests for PDF compilation."""

    @pytest.fixture
    def simple_tex(self, tmp_path: Path) -> Path:
        """Create a minimal compilable LaTeX file."""
        tex = tmp_path / "doc.tex"
        tex.write_text(
            "\\documentclass{article}\n"
            "\\begin{document}\n"
            "Hello World\n"
            "\\end{document}\n"
        )
        return tex

    def test_basic_compilation(self, simple_tex: Path, tmp_path: Path) -> None:
        """Compile a simple LaTeX file to PDF."""
        out = tmp_path / "doc.pdf"
        config = PdfExportConfig(
            input_path=simple_tex,
            output_path=out,
            compiler_strategy=CompilerStrategy.AUTO,
            compilation_timeout=60,
        )
        result = PdfConverter().convert(config)
        assert result.files_processed == 1
        assert out.exists()
        assert out.stat().st_size > 0

    def test_missing_input_raises(self, tmp_path: Path) -> None:
        """LatexCompilationError raised for missing input file."""
        config = PdfExportConfig(input_path=tmp_path / "nope.tex")
        with pytest.raises(LatexCompilationError):
            PdfConverter().convert(config)

    def test_non_tex_input_raises(self, tmp_path: Path) -> None:
        """LatexCompilationError raised for non-.tex input."""
        f = tmp_path / "doc.md"
        f.write_text("# hello")
        config = PdfExportConfig(input_path=f)
        with pytest.raises(LatexCompilationError):
            PdfConverter().convert(config)

    def test_skip_existing_without_force(self, simple_tex: Path, tmp_path: Path) -> None:
        """Existing PDF is not overwritten without force."""
        out = tmp_path / "doc.pdf"
        out.write_text("fake pdf")
        config = PdfExportConfig(
            input_path=simple_tex, output_path=out, force=False
        )
        result = PdfConverter().convert(config)
        assert result.files_processed == 0
        assert out.read_text() == "fake pdf"

    def test_default_output_path(self, simple_tex: Path) -> None:
        """When output_path is None, PDF is created next to input."""
        config = PdfExportConfig(
            input_path=simple_tex,
            output_path=None,
            compiler_strategy=CompilerStrategy.AUTO,
            compilation_timeout=60,
        )
        result = PdfConverter().convert(config)
        assert result.files_processed == 1
        expected = simple_tex.parent / f"{simple_tex.stem}.pdf"
        assert expected.exists()

    def test_compiler_available_check(self) -> None:
        """_compiler_available returns bool for known strategies."""
        converter = PdfConverter()
        # At least one must be True for this test class to run
        results = [
            converter._compiler_available(s)
            for s in [
                CompilerStrategy.LATEXMK,
                CompilerStrategy.PDFLATEX,
                CompilerStrategy.XELATEX,
                CompilerStrategy.PANDOC,
            ]
        ]
        assert any(results)

    def test_compiler_available_returns_false_for_auto(self) -> None:
        """AUTO is not a real compiler and should return False."""
        converter = PdfConverter()
        assert converter._compiler_available(CompilerStrategy.AUTO) is False


# ---------------------------------------------------------------------------
# Cross-converter integration
# ---------------------------------------------------------------------------


class TestCrossConverterIntegration:
    """Round-trip and pipeline tests across converters."""

    def test_html_to_markdown_to_html_roundtrip(self, tmp_path: Path) -> None:
        """HTML -> Markdown -> HTML produces valid HTML output."""
        # Step 1: HTML to Markdown
        html_in = tmp_path / "input.html"
        html_in.write_text(MINIMAL_HTML)
        md_out = tmp_path / "md_out"
        md_out.mkdir()
        HtmlConverter().convert(_make_config(html_in, md_out))

        md_file = md_out / "input.md"
        assert md_file.exists()

        # Step 2: Markdown back to HTML
        html_out = tmp_path / "html_out"
        html_out.mkdir()
        result = MarkdownConverter().convert(_make_config(md_file, html_out))
        assert result.files_processed == 1

        final = (html_out / "input.html").read_text()
        assert "<html>" in final.lower() or "<!doctype" in final.lower()
        # Core content should survive the round-trip
        assert "Hello" in final
        assert "World" in final

    def test_html_to_json_and_markdown_same_source(self, tmp_path: Path) -> None:
        """Same HTML file can be converted to both JSON and Markdown."""
        html_in = tmp_path / "doc.html"
        html_in.write_text(SAMPLE_HTML)
        json_out = tmp_path / "json_out"
        json_out.mkdir()
        md_out = tmp_path / "md_out"
        md_out.mkdir()

        r1 = JsonConverter().convert(_make_config(html_in, json_out))
        r2 = HtmlConverter().convert(_make_config(html_in, md_out))

        assert r1.files_processed == 1
        assert r2.files_processed == 1

        data = json.loads((json_out / "doc.json").read_text())
        md_content = (md_out / "doc.md").read_text()

        # Both outputs should reference the same content
        assert data["title"] == "Test Doc"
        assert "Test Doc" in md_content

    def test_all_converters_are_base_converter_subclasses(self) -> None:
        """Every converter extends BaseConverter."""
        for cls in [HtmlConverter, JsonConverter, MarkdownConverter, LatexConverter, PdfConverter]:
            assert issubclass(cls, BaseConverter)
            instance = cls()
            assert hasattr(instance, "convert")
            assert hasattr(instance, "get_input_files")
            assert hasattr(instance, "should_overwrite")
