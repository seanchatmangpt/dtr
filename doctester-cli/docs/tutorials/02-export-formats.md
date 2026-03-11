# Creating Multi-Format Documentation

Now that you can convert files, let's explore the power of DocTester CLI's format support. You'll learn how to export the same documentation to HTML, Markdown, JSON, and more.

## Overview: Supported Formats

DocTester CLI supports three main conversion formats:

| Format | Best For | File Extension |
| --- | --- | --- |
| **HTML** | Web publishing, sharing, professional docs | `.html` |
| **Markdown** | GitHub wikis, version control, documentation repos | `.md` |
| **JSON** | API documentation, machine-readable specs, integrations | `.json` |

Each format is optimized for its use case.

## Export to HTML

HTML is perfect for publishing documentation on websites or sharing via email.

### Basic HTML Export

Create a file called `api-guide.md`:

```markdown
# API Guide

## Authentication

Use Bearer tokens for API authentication:

\`\`\`bash
curl -H "Authorization: Bearer YOUR_TOKEN" https://api.example.com/v1/users
\`\`\`

## Pagination

Results are paginated by default. Use `page` and `limit` parameters:

\`\`\`bash
curl "https://api.example.com/v1/users?page=2&limit=10"
\`\`\`

## Response Format

All responses are in JSON:

\`\`\`json
{
  "data": [
    {"id": 1, "name": "Alice"},
    {"id": 2, "name": "Bob"}
  ],
  "page": 1,
  "total": 100
}
\`\`\`
```

Export to HTML:

```bash
dtr fmt html api-guide.md -o api-guide.html
```

Open in your browser:

```bash
open api-guide.html   # macOS
```

The HTML includes syntax highlighting for code blocks and a clean, professional layout.

### Customize HTML Export

Add options to control the output:

```bash
# Force overwrite if file exists
dtr fmt html api-guide.md -o api-guide.html --force

# Show what will happen without applying
dtr fmt html api-guide.md -o api-guide.html --dry-run
```

## Export to Markdown

Markdown is ideal for documentation repositories and GitHub wikis. It stays human-readable in version control.

### Convert HTML to Markdown

If you have HTML documentation, convert it back to Markdown:

```bash
dtr fmt md api-guide.html -o api-guide-clean.md
```

This is useful when you have legacy HTML docs and want to migrate them to Markdown for easier maintenance.

### Batch Convert Multiple Files

Export all HTML files in a directory to Markdown:

```bash
# Create a test directory
mkdir -p html_docs
cp api-guide.html html_docs/

# Convert all files recursively
dtr fmt md html_docs -o markdown_docs -r
```

The `-r` flag processes directories recursively. All HTML files are converted to Markdown in `markdown_docs/`.

## Export to JSON

JSON is powerful for machine-readable documentation, API specs, and integrations.

### Convert to JSON

```bash
dtr fmt json api-guide.md -o api-guide.json
```

Open `api-guide.json` in your editor. It contains:

```json
{
  "title": "API Guide",
  "sections": [
    {
      "heading": "Authentication",
      "paragraphs": ["Use Bearer tokens for API authentication:"],
      "code_blocks": [
        {
          "language": "bash",
          "content": "curl -H \"Authorization: Bearer YOUR_TOKEN\" ..."
        }
      ]
    }
  ]
}
```

JSON exports are perfect for:
- **Integrations** — Feed documentation into search engines or CMS systems
- **Automated Processing** — Parse with scripts or tools
- **API Documentation** — Generate OpenAPI/Swagger specs
- **Analytics** — Track documentation coverage and structure

### Use JSON with Scripts

Process JSON documentation programmatically (Python example):

```python
import json

with open("api-guide.json") as f:
    doc = json.load(f)

# Count sections
print(f"Total sections: {len(doc['sections'])}")

# Extract all code examples
for section in doc['sections']:
    for code in section.get('code_blocks', []):
        print(f"Language: {code['language']}")
        print(f"Code: {code['content'][:50]}...")
```

## Workflow: Convert and Organize

Here's a real-world workflow converting documentation for different audiences:

```bash
# 1. Start with Markdown (your source)
cat > tutorial.md << 'EOF'
# Getting Started with Our API

## Prerequisites

- Python 3.8+
- API token

## Installation

\`\`\`bash
pip install our-sdk
\`\`\`
EOF

# 2. Generate HTML for the website
dtr fmt html tutorial.md -o docs/html/tutorial.html

# 3. Generate Markdown for GitHub
dtr fmt md tutorial.md -o docs/github/tutorial.md

# 4. Generate JSON for API documentation portal
dtr fmt json tutorial.md -o docs/api/tutorial.json

# 5. All three formats are now ready!
echo "✓ HTML ready for web"
echo "✓ Markdown ready for GitHub"
echo "✓ JSON ready for integrations"
```

## Format Conversion Chart

Here's what conversions are supported:

```
Markdown → HTML ✓
Markdown → JSON ✓
HTML → Markdown ✓
HTML → JSON ✓
```

Want to go the other direction? Use the appropriate command:

```bash
# Markdown → HTML
dtr fmt html myfile.md -o output.html

# HTML → Markdown
dtr fmt md myfile.html -o output.md

# Any → JSON
dtr fmt json myfile.md -o output.json
dtr fmt json myfile.html -o output.json
```

## Common Options

All format commands support these options:

| Option | Purpose |
| --- | --- |
| `-o, --output` | Output file or directory |
| `-r, --recursive` | Process directories recursively |
| `-f, --force` | Overwrite existing files without asking |
| `--dry-run` | Show what will happen without applying |

## Troubleshooting

**"File not found: api-guide.md"**
- Double-check the file path
- Use absolute paths if relative paths don't work: `dtr fmt html /full/path/to/file.md`

**"Unsupported format"**
- Check supported formats: `dtr fmt --help`
- Verify the source format is correct

**"Output file already exists"**
- Use `--force` to overwrite: `dtr fmt html file.md -o output.html --force`
- Or specify a different output filename

## Next Steps

You've learned:
- ✅ Export to HTML for web publishing
- ✅ Export to Markdown for GitHub
- ✅ Export to JSON for integrations
- ✅ Batch convert multiple files

Ready to learn more?

- **[Generating Reports](../how-to/reports.md)** — Create summaries and changelogs
- **[Publishing Your Docs](../how-to/publishing.md)** — Push to GitHub Pages or the cloud
- **[Directory Management](../how-to/directory-management.md)** — List, archive, and validate exports

---

**Keep converting!** 📄
