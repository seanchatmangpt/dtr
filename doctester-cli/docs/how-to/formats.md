# How to Export in Different Formats

## Problem
You've written documentation or run tests with DocTester, and now you need to export to multiple output formats like PDF, slides, or blog posts. Each platform has different requirements and styling needs.

## Solution Overview

DocTester supports 6 primary output formats. Choose based on your audience and distribution method:

| Format | Use Case | Output File | Best For |
|--------|----------|-------------|----------|
| **Markdown** | Version control, portability | `.md` | GitHub, GitLab, static sites |
| **HTML** | Web viewing with styling | `.html` | Websites, Netlify, Vercel |
| **LaTeX/PDF** | Print & academic publishing | `.pdf` | Papers, reports, archives |
| **Slides** | Presentations & talks | `.html` (Reveal.js) | Conferences, team meetings |
| **Blog** | Blogging platforms | JSON queue | Dev.to, Medium, Hashnode |
| **OpenAPI** | API documentation | `.json` / `.yaml` | Swagger UI, API portals |

## Step-by-Step

### 1. Export to Markdown (Default)
```bash
dtr export guide.md
# Output: guide.md (in same directory)
```

Markdown is the universal format — portable, Git-friendly, and readable everywhere.

### 2. Export to HTML
```bash
dtr export guide.md output.html --format html

# With custom CSS theme
dtr export guide.md output.html --format html --template bootstrap

# With syntax highlighting
dtr export guide.md output.html --format html --highlight-theme atom-dark
```

HTML includes embedded CSS, so output is a single, self-contained file.

### 3. Export to PDF (Academic Templates)
```bash
# Default template (PDF)
dtr export guide.md output.pdf --format latex

# ACM conference format
dtr export guide.md output.pdf --format latex --template acm-conference

# arXiv preprint style
dtr export guide.md output.pdf --format latex --template arxiv

# IEEE journal format
dtr export guide.md output.pdf --format latex --template ieee

# Nature magazine style
dtr export guide.md output.pdf --format latex --template nature

# US Patent format
dtr export guide.md output.pdf --format latex --template us-patent
```

LaTeX templates require a TeX distribution (texlive/miktex). Compilation happens automatically.

### 4. Export to HTML5 Slides
```bash
dtr export guide.md slides.html --format slides

# With speaker notes
dtr export guide.md slides.html --format slides --speaker-notes

# Custom transition style
dtr export guide.md slides.html --format slides --transition fade
```

Output is a standalone `.html` file using Reveal.js. Open in any browser.

### 5. Combine Formats (Single Export)
```bash
# Generate all formats at once
dtr export guide.md \
  --format markdown,html,latex,slides \
  --output-dir output/

# With named templates
dtr export guide.md \
  --format latex,html \
  --latex-template acm-conference \
  --html-template bootstrap \
  --output-dir docs/
```

Output goes to `output/` directory with format-specific subdirectories.

### 6. Export to Blog Platform
```bash
# Export for Dev.to
dtr export guide.md --format blog --platform devto

# Export for Medium
dtr export guide.md --format blog --platform medium

# Export for Hashnode
dtr export guide.md --format blog --platform hashnode
```

See [How to Publish to Blogging Platforms](blog-publishing.md) for authentication details.

### 7. Batch Export Directory
```bash
dtr export src/docs/ --format html --output-dir output/html/
# Converts all .md files in src/docs/ to HTML
```

## Format-Specific Settings

### PDF Compiler Selection
```bash
# Force specific LaTeX compiler
dtr export guide.md output.pdf \
  --format latex \
  --latex-compiler pdflatex      # default, fastest
dtr export guide.md output.pdf --latex-compiler xelatex  # Unicode support
dtr export guide.md output.pdf --latex-compiler lualatex # modern, extensible
```

### Syntax Highlighting Themes
```bash
# Available for HTML & PDF
--highlight-theme atom-dark
--highlight-theme monokai
--highlight-theme github
--highlight-theme solarized-light
```

### HTML Template Themes
```bash
--template bootstrap       # Bootstrap 5
--template tailwind       # Tailwind CSS
--template minimal        # Minimal CSS
--template github         # GitHub markdown style
```

## Choosing the Right Format

**Use Markdown if:**
- Document lives in version control
- Portability is critical
- You want easy Git diffs

**Use HTML if:**
- Sharing via website
- Interactive content needed
- Custom styling required

**Use PDF if:**
- Document will be printed
- Academic/formal publication
- Long-term archival

**Use Slides if:**
- Presenting to an audience
- Building a presentation deck
- Live demo environment

**Use Blog if:**
- Publishing to online platforms
- Reaching developer community
- Cross-posting to multiple sites

## Troubleshooting

**"pdflatex not found"** → Install TeX: `sudo apt install texlive-xetex` (Linux) or download MacTeX (macOS)

**"Output file already exists"** → Use `--overwrite` flag to replace

**"Template not found"** → Run `dtr templates --list` to see available templates

## Next Steps
- [Add Images and Tables](rich-content.md)
- [Integrate with Maven](maven-integration.md)
- [Publish to Blogs](blog-publishing.md)
