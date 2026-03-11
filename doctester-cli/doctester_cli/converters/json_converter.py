"""Convert exports to/from JSON format."""

from pathlib import Path
import json
from bs4 import BeautifulSoup

from doctester_cli.model import ConversionConfig, ConversionResult
from doctester_cli.converters.base_converter import BaseConverter


class JsonConverter(BaseConverter):
    """Convert exports to JSON format."""

    def convert_from_html(self, config: ConversionConfig) -> ConversionResult:
        """Convert HTML files to JSON format."""
        files = self.get_input_files(config)
        html_files = [f for f in files if f.suffix in [".html", ".htm"]]

        processed = 0
        warnings = []

        for html_file in html_files:
            try:
                json_data = self._html_to_json_data(html_file)
                output_file = config.output_path / f"{html_file.stem}.json"

                if not self.should_overwrite(output_file, config.force):
                    warnings.append(f"Skipped {html_file.name} (file exists)")
                    continue

                if config.pretty:
                    output_file.write_text(
                        json.dumps(json_data, indent=2, ensure_ascii=False),
                        encoding="utf-8",
                    )
                else:
                    output_file.write_text(
                        json.dumps(json_data, ensure_ascii=False),
                        encoding="utf-8",
                    )

                processed += 1
            except Exception as e:
                warnings.append(f"Failed to convert {html_file.name}: {str(e)}")

        return ConversionResult(files_processed=processed, warnings=warnings)

    def _html_to_json_data(self, html_file: Path) -> dict:
        """Convert HTML file to structured JSON data."""
        html_content = html_file.read_text(encoding="utf-8")
        soup = BeautifulSoup(html_content, "html.parser")

        data = {
            "file": html_file.name,
            "title": "",
            "sections": [],
            "metadata": {},
        }

        # Extract title
        title = soup.find("title")
        if title:
            data["title"] = title.string

        # Extract content structure
        body = soup.find("body") or soup
        current_section = None

        for element in body.find_all(recursive=False):
            if element.name == "h1":
                if current_section:
                    data["sections"].append(current_section)
                current_section = {
                    "title": element.get_text(),
                    "content": [],
                    "subsections": [],
                }
            elif element.name in ["h2", "h3"]:
                content = {"type": "heading", "level": element.name, "text": element.get_text()}
                if current_section:
                    current_section["content"].append(content)
            elif element.name == "p":
                content = {"type": "paragraph", "text": element.get_text()}
                if current_section:
                    current_section["content"].append(content)
            elif element.name == "pre":
                code_elem = element.find("code")
                code_text = code_elem.get_text() if code_elem else element.get_text()
                content = {"type": "code", "text": code_text}
                if current_section:
                    current_section["content"].append(content)
            elif element.name == "table":
                content = {
                    "type": "table",
                    "data": self._extract_table_data(element),
                }
                if current_section:
                    current_section["content"].append(content)

        if current_section:
            data["sections"].append(current_section)

        return data

    def _extract_table_data(self, table_elem) -> dict:
        """Extract table data as JSON."""
        rows = table_elem.find_all("tr")
        if not rows:
            return {"headers": [], "rows": []}

        headers = []
        header_cells = rows[0].find_all(["th", "td"])
        headers = [cell.get_text().strip() for cell in header_cells]

        data_rows = []
        for row in rows[1:]:
            cells = row.find_all("td")
            if cells:
                row_data = [cell.get_text().strip() for cell in cells]
                data_rows.append(row_data)

        return {"headers": headers, "rows": data_rows}
