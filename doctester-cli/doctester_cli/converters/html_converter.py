"""Convert HTML exports to other formats."""

from pathlib import Path
import re
from bs4 import BeautifulSoup

from doctester_cli.model import ConversionConfig, ConversionResult
from doctester_cli.converters.base_converter import BaseConverter


class HtmlConverter(BaseConverter):
    """Convert HTML documentation to other formats."""

    def convert_to_markdown(self, config: ConversionConfig) -> ConversionResult:
        """Convert HTML files to Markdown format."""
        files = self.get_input_files(config)
        html_files = [f for f in files if f.suffix in [".html", ".htm"]]

        processed = 0
        warnings = []

        for html_file in html_files:
            try:
                markdown = self._html_to_markdown(html_file)
                output_file = config.output_path / f"{html_file.stem}.md"

                if not self.should_overwrite(output_file, config.force):
                    warnings.append(f"Skipped {html_file.name} (file exists)")
                    continue

                output_file.write_text(markdown, encoding="utf-8")
                processed += 1
            except Exception as e:
                warnings.append(f"Failed to convert {html_file.name}: {str(e)}")

        return ConversionResult(files_processed=processed, warnings=warnings)

    def _html_to_markdown(self, html_file: Path) -> str:
        """Convert a single HTML file to Markdown."""
        html_content = html_file.read_text(encoding="utf-8")
        soup = BeautifulSoup(html_content, "html.parser")

        markdown_parts = []

        # Extract title
        title = soup.find("title")
        if title:
            markdown_parts.append(f"# {title.string}\n")

        # Extract main content
        body = soup.find("body") or soup
        for element in body.find_all(recursive=False):
            if element.name in ["h1", "h2", "h3", "h4", "h5", "h6"]:
                level = int(element.name[1])
                markdown_parts.append(f"{'#' * level} {element.get_text()}\n")
            elif element.name == "p":
                markdown_parts.append(f"{element.get_text()}\n")
            elif element.name == "pre":
                code = element.find("code")
                if code:
                    markdown_parts.append(f"```\n{code.get_text()}\n```\n")
                else:
                    markdown_parts.append(f"```\n{element.get_text()}\n```\n")
            elif element.name in ["ul", "ol"]:
                markdown_parts.append(self._convert_list(element))
            elif element.name == "table":
                markdown_parts.append(self._convert_table(element))

        return "\n".join(markdown_parts)

    def _convert_list(self, list_elem) -> str:
        """Convert HTML list to Markdown."""
        items = list_elem.find_all("li", recursive=False)
        prefix = "- " if list_elem.name == "ul" else "1. "
        return "\n".join(f"{prefix}{item.get_text()}" for item in items) + "\n"

    def _convert_table(self, table_elem) -> str:
        """Convert HTML table to Markdown."""
        lines = []
        rows = table_elem.find_all("tr")

        if not rows:
            return ""

        # Header row
        header_cells = rows[0].find_all(["th", "td"])
        header = " | ".join(cell.get_text().strip() for cell in header_cells)
        lines.append(f"| {header} |")
        lines.append("|" + "|".join("-" * len(h) for h in header_cells) + "|")

        # Data rows
        for row in rows[1:]:
            cells = row.find_all("td")
            if cells:
                data = " | ".join(cell.get_text().strip() for cell in cells)
                lines.append(f"| {data} |")

        return "\n".join(lines) + "\n"
