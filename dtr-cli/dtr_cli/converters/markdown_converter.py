"""Convert Markdown to HTML and other formats."""

from pathlib import Path
import markdown
from jinja2 import Template

from dtr_cli.model import ConversionConfig, ConversionResult
from dtr_cli.converters.base_converter import BaseConverter


class MarkdownConverter(BaseConverter):
    """Convert Markdown documentation to HTML."""

    TEMPLATES = {
        "default": """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>{{ title }}</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; max-width: 900px; margin: 0 auto; padding: 20px; color: #333; }
        h1, h2, h3 { border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
        code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-family: monospace; }
        pre { background: #f6f6f6; padding: 12px; border-left: 4px solid #0066cc; overflow-x: auto; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        table th, table td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        table th { background: #f5f5f5; font-weight: bold; }
        blockquote { border-left: 4px solid #ddd; margin: 0; padding-left: 16px; color: #666; }
        a { color: #0066cc; text-decoration: none; }
        a:hover { text-decoration: underline; }
    </style>
</head>
<body>
    {{ content }}
</body>
</html>""",
        "minimal": """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>{{ title }}</title>
</head>
<body>
    {{ content }}
</body>
</html>""",
        "github": """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>{{ title }}</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/github-markdown-css@5.1.0/github-markdown.min.css">
    <style>
        .markdown-body { box-sizing: border-box; min-width: 200px; max-width: 980px; margin: 0 auto; padding: 45px; }
        @media (max-width: 767px) { .markdown-body { padding: 15px; } }
    </style>
</head>
<body class="markdown-body">
    {{ content }}
</body>
</html>""",
    }

    def convert(self, config: ConversionConfig) -> ConversionResult:
        """Convert Markdown files to HTML (default conversion)."""
        return self.convert_to_html(config)

    def convert_to_html(self, config: ConversionConfig) -> ConversionResult:
        """Convert Markdown files to HTML."""
        files = self.get_input_files(config)
        md_files = [f for f in files if f.suffix in [".md", ".markdown"]]

        processed = 0
        warnings = []
        template_name = config.template or "default"

        for md_file in md_files:
            try:
                html = self._markdown_to_html(md_file, template_name)
                output_file = config.output_path / f"{md_file.stem}.html"

                if not self.should_overwrite(output_file, config.force):
                    warnings.append(f"Skipped {md_file.name} (file exists)")
                    continue

                output_file.write_text(html, encoding="utf-8")
                processed += 1
            except Exception as e:
                warnings.append(f"Failed to convert {md_file.name}: {str(e)}")

        return ConversionResult(files_processed=processed, warnings=warnings)

    def _markdown_to_html(self, md_file: Path, template: str) -> str:
        """Convert a single Markdown file to HTML."""
        content = md_file.read_text(encoding="utf-8")

        # Convert Markdown to HTML
        html_content = markdown.markdown(
            content,
            extensions=[
                "extra",
                "codehilite",
                "toc",
                "tables",
            ],
        )

        # Get title from first H1 or filename
        title = md_file.stem.replace("-", " ").replace("_", " ").title()

        # Apply template
        template_str = self.TEMPLATES.get(template, self.TEMPLATES["default"])
        tmpl = Template(template_str)

        return tmpl.render(title=title, content=html_content)
