"""Validate and generate schemas for DTR exported HTML/JSON/Markdown files."""

import json
import re
from pathlib import Path
from typing import Optional

import typer
from rich.console import Console
from rich.table import Table

console = Console()
app = typer.Typer(help="Validate and generate schemas for DTR exports")

# ---------------------------------------------------------------------------
# Format detection
# ---------------------------------------------------------------------------

_FORMAT_SUFFIXES: dict[str, str] = {
    ".html": "html",
    ".htm": "html",
    ".json": "json",
    ".md": "md",
    ".markdown": "md",
}


def _detect_format(path: Path) -> Optional[str]:
    """Infer format from file extension. Returns None when unrecognised."""
    return _FORMAT_SUFFIXES.get(path.suffix.lower())


# ---------------------------------------------------------------------------
# Validation helpers
# ---------------------------------------------------------------------------


def _validate_html(content: str, strict: bool) -> tuple[list[str], list[str]]:
    """Return (errors, warnings) for a DTR HTML file.

    Required elements: <title>, <body>, at least one heading (h1-h6).
    Warnings (become errors in strict mode): empty title, no h1.
    """
    from bs4 import BeautifulSoup

    soup = BeautifulSoup(content, "html.parser")
    errors: list[str] = []
    warnings: list[str] = []

    if not soup.find("title"):
        errors.append("missing <title> element")
    else:
        title_text = soup.find("title").get_text(strip=True)
        if not title_text:
            warnings.append("<title> element is empty")

    if not soup.find("body"):
        errors.append("missing <body> element")

    headings = soup.find_all(re.compile(r"^h[1-6]$"))
    if not headings:
        errors.append("no heading elements (h1-h6) found")
    else:
        h1_tags = soup.find_all("h1")
        if not h1_tags:
            warnings.append("no <h1> element found (expected at least one top-level heading)")

    return errors, warnings


def _validate_json(content: str, strict: bool) -> tuple[list[str], list[str]]:
    """Return (errors, warnings) for a DTR JSON file.

    Required keys: sections, metadata.
    Warnings (become errors in strict mode): empty sections list, missing
    metadata sub-keys (title, generated_at).
    """
    errors: list[str] = []
    warnings: list[str] = []

    try:
        data = json.loads(content)
    except json.JSONDecodeError as exc:
        errors.append(f"invalid JSON: {exc}")
        return errors, warnings

    if not isinstance(data, dict):
        errors.append("top-level value must be a JSON object")
        return errors, warnings

    if "sections" not in data:
        errors.append("missing required key: 'sections'")
    else:
        if not isinstance(data["sections"], list):
            errors.append("'sections' must be a JSON array")
        elif len(data["sections"]) == 0:
            warnings.append("'sections' array is empty")

    if "metadata" not in data:
        errors.append("missing required key: 'metadata'")
    else:
        meta = data["metadata"]
        if not isinstance(meta, dict):
            errors.append("'metadata' must be a JSON object")
        else:
            for sub_key in ("title", "generated_at"):
                if sub_key not in meta:
                    warnings.append(f"metadata missing recommended key: '{sub_key}'")

    return errors, warnings


def _validate_markdown(content: str, strict: bool) -> tuple[list[str], list[str]]:
    """Return (errors, warnings) for a DTR Markdown file.

    Checks: at least one heading, heading hierarchy (no skipped levels from
    the first heading down — a warning unless strict).
    """
    errors: list[str] = []
    warnings: list[str] = []

    heading_pattern = re.compile(r"^(#{1,6})\s+(.+)$", re.MULTILINE)
    matches = heading_pattern.findall(content)

    if not matches:
        errors.append("no Markdown headings found (expected at least one # heading)")
        return errors, warnings

    levels = [len(hashes) for hashes, _ in matches]

    # Must start with h1
    if levels[0] != 1:
        warnings.append(
            f"document does not start with an H1 heading (found H{levels[0]})"
        )

    # Check for skipped levels (e.g. H1 -> H3)
    for i in range(1, len(levels)):
        prev, curr = levels[i - 1], levels[i]
        if curr > prev + 1:
            warnings.append(
                f"heading hierarchy skip: H{prev} followed by H{curr} "
                f"(heading #{i + 1}: '{matches[i][1]}')"
            )

    return errors, warnings


# ---------------------------------------------------------------------------
# Schema generation helpers
# ---------------------------------------------------------------------------


def _analyse_html_structure(path: Path) -> dict:
    """Extract structural metadata from a single HTML export file."""
    from bs4 import BeautifulSoup

    content = path.read_text(encoding="utf-8", errors="replace")
    soup = BeautifulSoup(content, "html.parser")

    title_tag = soup.find("title")
    title = title_tag.get_text(strip=True) if title_tag else ""

    headings: list[dict] = []
    for tag in soup.find_all(re.compile(r"^h[1-6]$")):
        headings.append({"level": int(tag.name[1]), "text": tag.get_text(strip=True)})

    code_blocks = len(soup.find_all("code"))
    tables = len(soup.find_all("table"))

    return {
        "title": title,
        "headings": headings,
        "code_block_count": code_blocks,
        "table_count": tables,
    }


def _analyse_json_structure(path: Path) -> dict:
    """Extract structural metadata from a single JSON export file."""
    try:
        data = json.loads(path.read_text(encoding="utf-8", errors="replace"))
    except json.JSONDecodeError:
        return {"parse_error": True}

    if not isinstance(data, dict):
        return {"not_object": True}

    sections = data.get("sections", [])
    section_count = len(sections) if isinstance(sections, list) else 0

    section_types: list[str] = []
    if isinstance(sections, list):
        for section in sections:
            if isinstance(section, dict) and "type" in section:
                t = section["type"]
                if t not in section_types:
                    section_types.append(t)

    metadata_keys = list(data.get("metadata", {}).keys()) if "metadata" in data else []

    return {
        "top_level_keys": list(data.keys()),
        "section_count": section_count,
        "section_types": section_types,
        "metadata_keys": metadata_keys,
    }


def _analyse_markdown_structure(path: Path) -> dict:
    """Extract structural metadata from a single Markdown export file."""
    content = path.read_text(encoding="utf-8", errors="replace")
    heading_pattern = re.compile(r"^(#{1,6})\s+(.+)$", re.MULTILINE)
    matches = heading_pattern.findall(content)

    headings = [{"level": len(h), "text": t.strip()} for h, t in matches]
    code_fence_pattern = re.compile(r"^```", re.MULTILINE)
    code_block_count = len(code_fence_pattern.findall(content)) // 2

    return {
        "heading_count": len(headings),
        "headings": headings,
        "code_block_count": code_block_count,
    }


# ---------------------------------------------------------------------------
# `dtr schema validate`
# ---------------------------------------------------------------------------


@app.command()
def validate(
    file_or_dir: Path = typer.Argument(
        ...,
        help="File or directory of DTR exports to validate",
    ),
    format: Optional[str] = typer.Option(
        None,
        "--format",
        "-f",
        help="Force format: html, json, md, auto (default: auto-detect from extension)",
    ),
    strict: bool = typer.Option(
        False,
        "--strict",
        help="Treat warnings as errors — exit non-zero when any warning is found",
    ),
) -> None:
    """
    Validate exported HTML/JSON/Markdown files against expected DTR output structure.

    Checks HTML for required elements (title, body, headings), JSON for expected
    keys (sections, metadata), and Markdown for proper heading hierarchy.
    Reports pass/fail per file with Rich output.

    \b
    Examples:
        dtr schema validate target/docs/report.html
        dtr schema validate target/docs/ --format html
        dtr schema validate target/docs/data.json --strict
        dtr schema validate target/docs/guide.md
    """
    if format is not None and format not in ("html", "json", "md", "auto"):
        console.print(
            "[red]✗[/red] Invalid --format value. Choose from: html, json, md, auto"
        )
        raise typer.Exit(1)

    # Collect target files
    if file_or_dir.is_dir():
        candidates = [p for p in file_or_dir.rglob("*") if p.is_file()]
    elif file_or_dir.is_file():
        candidates = [file_or_dir]
    else:
        console.print(f"[red]✗[/red] Path not found: {file_or_dir}")
        raise typer.Exit(1)

    if not candidates:
        console.print("[yellow]No files found to validate.[/yellow]")
        return

    table = Table(title="DTR Schema Validation")
    table.add_column("File", style="cyan")
    table.add_column("Format", style="blue")
    table.add_column("Result", justify="center")
    table.add_column("Details")

    total_errors = 0
    total_warnings = 0
    files_checked = 0

    for path in sorted(candidates):
        # Resolve effective format
        if format is None or format == "auto":
            eff_format = _detect_format(path)
        else:
            eff_format = format

        if eff_format is None:
            table.add_row(
                str(path.name),
                "unknown",
                "[dim]SKIP[/dim]",
                "unrecognised extension",
            )
            continue

        files_checked += 1
        content = path.read_text(encoding="utf-8", errors="replace")

        if eff_format == "html":
            errors, warnings = _validate_html(content, strict)
        elif eff_format == "json":
            errors, warnings = _validate_json(content, strict)
        else:  # md
            errors, warnings = _validate_markdown(content, strict)

        effective_errors = errors + (warnings if strict else [])
        effective_warnings = [] if strict else warnings

        total_errors += len(effective_errors)
        total_warnings += len(effective_warnings)

        if effective_errors:
            result_cell = "[red]FAIL[/red]"
            details = "; ".join(effective_errors[:3])
            if len(effective_errors) > 3:
                details += f" (+{len(effective_errors) - 3} more)"
        elif effective_warnings:
            result_cell = "[yellow]WARN[/yellow]"
            details = "; ".join(effective_warnings[:3])
        else:
            result_cell = "[green]PASS[/green]"
            details = ""

        table.add_row(str(path.name), eff_format, result_cell, details)

    console.print(table)
    console.print(
        f"\n[bold]Summary:[/bold] {files_checked} file(s) checked, "
        f"[red]{total_errors} error(s)[/red], "
        f"[yellow]{total_warnings} warning(s)[/yellow]"
    )

    if total_errors > 0:
        raise typer.Exit(1)


# ---------------------------------------------------------------------------
# `dtr schema generate`
# ---------------------------------------------------------------------------


@app.command()
def generate(
    export_dir: Path = typer.Argument(
        ...,
        help="Export directory to analyse for schema generation",
    ),
    output: Optional[Path] = typer.Option(
        None,
        "--output",
        "-o",
        help="Output path for the generated schema file (default: <export_dir>/.dtr-schema.json)",
    ),
    pretty: bool = typer.Option(
        True,
        "--pretty/--no-pretty",
        help="Pretty-print the JSON output (default: true)",
    ),
) -> None:
    """
    Generate a JSON schema from existing DTR exports.

    Analyses HTML structure, extracts section patterns from JSON files, and
    documents heading hierarchies from Markdown files. Writes a
    .dtr-schema.json file describing the corpus.

    \b
    Examples:
        dtr schema generate target/docs/
        dtr schema generate target/docs/ --output my-schema.json
        dtr schema generate target/docs/ --no-pretty
    """
    if not export_dir.exists() or not export_dir.is_dir():
        console.print(f"[red]✗[/red] Export directory not found: {export_dir}")
        raise typer.Exit(1)

    html_files = list(export_dir.rglob("*.html")) + list(export_dir.rglob("*.htm"))
    json_files = list(export_dir.rglob("*.json"))
    md_files = list(export_dir.rglob("*.md")) + list(export_dir.rglob("*.markdown"))

    # Filter out any previously generated schema file
    json_files = [p for p in json_files if p.name != ".dtr-schema.json"]

    schema: dict = {
        "$schema": "https://dtr.org/schema/v1",
        "generated_from": str(export_dir),
        "file_counts": {
            "html": len(html_files),
            "json": len(json_files),
            "markdown": len(md_files),
        },
        "html_structure": [],
        "json_structure": [],
        "markdown_structure": [],
    }

    with console.status("[cyan]Analysing HTML files...[/cyan]"):
        for path in sorted(html_files):
            entry = _analyse_html_structure(path)
            entry["file"] = path.name
            schema["html_structure"].append(entry)

    with console.status("[cyan]Analysing JSON files...[/cyan]"):
        for path in sorted(json_files):
            entry = _analyse_json_structure(path)
            entry["file"] = path.name
            schema["json_structure"].append(entry)

    with console.status("[cyan]Analysing Markdown files...[/cyan]"):
        for path in sorted(md_files):
            entry = _analyse_markdown_structure(path)
            entry["file"] = path.name
            schema["markdown_structure"].append(entry)

    # Derive aggregate section type patterns from JSON exports
    all_section_types: list[str] = []
    for entry in schema["json_structure"]:
        all_section_types.extend(entry.get("section_types", []))
    schema["observed_section_types"] = sorted(set(all_section_types))

    # Resolve output path
    out_path = output if output is not None else (export_dir / ".dtr-schema.json")

    indent = 2 if pretty else None
    out_path.write_text(json.dumps(schema, indent=indent), encoding="utf-8")

    total = len(html_files) + len(json_files) + len(md_files)
    console.print(
        f"[green]✓[/green] Schema generated from {total} file(s): {out_path}"
    )
    console.print(
        f"  HTML: {len(html_files)}, JSON: {len(json_files)}, Markdown: {len(md_files)}"
    )
