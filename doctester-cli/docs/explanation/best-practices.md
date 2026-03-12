# Best Practices

This guide shares proven patterns and conventions for using DTR CLI effectively. Follow these practices for maintainable, professional documentation.

## Documentation Organization

### Directory Structure

Organize your documentation in a consistent, hierarchical structure:

```
docs/
├── README.md                    # Overview and navigation
├── getting-started/
│   ├── installation.md
│   ├── first-example.md
│   └── common-tasks.md
├── guides/
│   ├── user-guide.md
│   ├── developer-guide.md
│   └── admin-guide.md
├── api/
│   ├── endpoints.md
│   ├── authentication.md
│   └── examples.md
├── tutorials/
│   ├── tutorial-1.md
│   ├── tutorial-2.md
│   └── tutorial-3.md
└── reference/
    ├── cli-reference.md
    ├── config-reference.md
    └── glossary.md
```

**Benefits:**
- Easy to navigate for users
- Clear separation of concerns
- Scales well as documentation grows

### Naming Conventions

**File names:**
- Use lowercase with hyphens: `getting-started.md`, `api-guide.md`
- Avoid spaces and special characters
- Use numbers for sequenced docs: `01-intro.md`, `02-setup.md`

**Folder names:**
- Use lowercase with hyphens: `getting-started/`, `api-docs/`
- Keep names short but descriptive

**Examples:**
```
✓ api-authentication.md
✗ API Authentication.md
✗ ApiAuthentication.md

✓ 01-getting-started.md
✗ getting-started-1.md
✗ 1-getting-started.md
```

## Writing Documentation

### Audience-First Approach

Know who you're writing for:

| Audience | Approach | Examples |
|----------|----------|----------|
| **Beginners** | Start simple, explain jargon, provide examples | "What is a format?" in Getting Started |
| **Professionals** | Assume knowledge, focus on "how", show options | CLI reference with all flags |
| **Contributors** | Explain "why", architecture, design patterns | Performance Guide, Design Decisions |

**Best practice:** Write for the least technical reader, then add advanced notes.

### Structure: Markdown Best Practices

```markdown
# Main Title (H1)

One-paragraph summary of what this document covers.

## Section (H2)

Explain the concept. Use **bold** for emphasis, `code` for inline code.

### Subsection (H3)

More specific detail.

**Code example:**
\`\`\`bash
dtr fmt html input.md -o output.html
\`\`\`

**Explanation:** Describe what the code does and why.

### Another Subsection

Continue with more detail.
```

**Guidelines:**
- Use H1 once per file for the title
- Use H2 for major sections
- Use H3 for subsections
- Keep line length reasonable (80 chars for code, 120 for prose)
- Use blank lines between sections

### Code Examples

**Good code examples:**

```bash
# ✓ Command with explanation
dtr fmt html input.md -o output.html
# Converts input.md to output.html
```

```bash
# ✓ Multiple commands with context
dtr batch --input '*.md' --output ./html/
# Converts all .md files in current directory to ./html/
```

```markdown
# ✗ Bad: No explanation
dtr fmt html input.md -o output.html
```

**Code block format:**

````markdown
\`\`\`bash
dtr fmt html input.md -o output.html
\`\`\`

\`\`\`python
import sys
print("Hello, World!")
\`\`\`

\`\`\`json
{
  "name": "example",
  "value": 42
}
\`\`\`
````

Always specify the language for syntax highlighting.

### Links and References

**Good internal links:**

```markdown
See [Getting Started](../tutorials/01-getting-started.md) for installation.

Learn more in [How-To: Formats](../how-to/formats.md).

Check the [Glossary](../reference/glossary.md) for definitions.
```

**Use relative paths:**
- Easier to move documentation
- Works offline and in version control
- More portable than absolute URLs

**External links:**

```markdown
See the [Python documentation](https://docs.python.org/3/)

Read about [Markdown syntax](https://spec.commonmark.org/)
```

### Tables

**Good tables:**

| Feature | Description | When to Use |
|---------|-------------|-------------|
| Markdown | Fast, portable | Default for all docs |
| HTML | Rich, web-ready | Web publishing |
| PDF | Print-ready | Reports |

**Bad tables:** Too many columns, tiny font, hard to read

Keep tables simple and scannable.

## Configuration Management

### Configuration Files

Use YAML for configuration:

```yaml
# Good: descriptive, organized, validated
output:
  format: html
  directory: ./docs/html/
  theme: default

processing:
  parallel: true
  threads: 4
  cache: true
```

Keep configuration files:
- Well-commented
- Type-safe (validate against schema)
- Version-controlled
- Separate from source code

### Environment Variables

Use environment variables for secrets and deployment-specific settings:

```bash
# Good: sensitive data in environment
export DTR_GITHUB_TOKEN="ghp_xxxxxx"
export DTR_S3_BUCKET="my-docs-bucket"
export DTR_AWS_REGION="us-east-1"

dtr publish github docs.md
```

Never hardcode secrets in configuration files or documentation.

## Version Control

### What to Commit

**Commit:**
- Source Markdown files
- Configuration files (non-secrets)
- Images and assets
- Build scripts

**Don't commit:**
- Generated output (HTML, PDF)
- Cache files
- Node modules or virtual environments
- API tokens or secrets

### .gitignore

```
# Generated output
docs/html/
docs/pdf/
docs/output/

# DTR cache
.doctester/
~/.doctester/

# Virtual environment
venv/
.venv/

# OS files
.DS_Store
Thumbs.db

# IDE
.vscode/
.idea/
*.swp
```

### Commit Messages

```
# Good: descriptive, action-oriented
docs: add CLI reference guide
docs: update getting started with Python 3.12 requirements
docs: fix broken link in API guide

# Bad: vague or not descriptive
update docs
docs update
fixed stuff
```

## Publishing Workflows

### Local Development

```bash
# Edit locally
vim docs/getting-started.md

# Convert to HTML for review
dtr fmt html docs/getting-started.md -o /tmp/preview.html

# Review in browser
open /tmp/preview.html

# Commit and push
git add docs/getting-started.md
git commit -m "docs: improve getting started guide"
git push
```

### CI/CD Pipeline

```yaml
name: Documentation Build

on: [push, pull_request]

jobs:
  build-docs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up Python
        uses: actions/setup-python@v2
        with:
          python-version: '3.12'
      
      - name: Install DocTester
        run: pip install -e ./dtr-cli
      
      - name: Generate Documentation
        run: dtr batch --input 'docs/**/*.md' --output './build/docs/'
      
      - name: Deploy to GitHub Pages
        uses: actions/upload-artifact@v2
        with:
          name: documentation
          path: ./build/docs/
```

### Manual Publishing

```bash
# Build all documentation
dtr batch --input 'docs/**/*.md' --output './dist/'

# Review output
ls -la dist/

# Publish to GitHub Pages
git checkout gh-pages
cp -r dist/* .
git add .
git commit -m "docs: rebuild documentation"
git push

# Return to main branch
git checkout main
```

## Quality Assurance

### Documentation Review Checklist

Before publishing, verify:

- [ ] All links are valid (relative paths, no 404s)
- [ ] Code examples are correct and runnable
- [ ] Grammar and spelling checked
- [ ] Consistent terminology (use glossary)
- [ ] Table of contents is accurate
- [ ] Cross-references updated
- [ ] Version numbers correct
- [ ] Examples use current API/CLI
- [ ] Outdated info removed
- [ ] Style consistent with rest of docs

### Testing Documentation

```bash
# Validate Markdown syntax
dtr validate docs/**/*.md

# Check for broken links
dtr lint --check-links docs/

# Spell check
dtr lint --spell-check docs/

# Convert to all formats to catch errors
dtr fmt html docs/index.md
dtr fmt pdf docs/index.md
dtr fmt json docs/index.md
```

### Keeping Docs Current

- Review documentation quarterly
- Update examples when APIs change
- Link to version-specific docs
- Maintain changelog
- Archive old versions

## Accessibility

### Markdown for Accessibility

**Good:**

```markdown
# Heading (not **bold heading**)

Use proper heading hierarchy (H1, H2, H3...)

- Use lists for related items
- Not just paragraphs

| Header | Column |
|--------|--------|
| Row    | Data   |

[Link text describes destination](url)

\`\`\`alt text for complex content\`\`\`
```

**Bad:**

```markdown
**Heading** (bold is not semantic)

Use **ALL BOLD** for emphasis

Important:
- List items not using - bullets

[Click here](url) (link text unclear)
```

### Images and Diagrams

- Always include alt text
- Use high contrast
- Provide text descriptions
- Avoid images as primary content

```markdown
![Database schema diagram](./assets/schema.png)

The diagram shows three tables: Users, Articles, and Comments, 
related by foreign keys. See [Schema Description](./schema.md) 
for detailed information.
```

## Performance Optimization

### Documentation Size

- Aim for 2,000-3,000 words per page
- Split large guides into multiple pages
- Use tables instead of prose for comparisons
- Keep code examples focused and short

### Asset Optimization

```bash
# Compress images before publishing
convert image.png -quality 85 image-compressed.png

# Size limits
dtr batch --input 'docs/*.md' --optimize --max-size 5M
```

## Team Collaboration

### Documentation Standards

Create a `DOCUMENTATION.md` in your project:

```markdown
# Documentation Standards

## File Organization
- Use structure described in [Best Practices](docs/explanation/best-practices.md)

## Writing Style
- Second person ("you should", not "one should")
- Active voice ("The API returns..." not "The response is returned...")
- Present tense
- Inclusive language

## Examples
- All code examples must be tested
- Include expected output
- Keep examples short and focused

## Review Process
- All documentation changes require review
- Use the checklist in best-practices.md
- Request feedback from non-experts
```

---

## See Also

- [Architecture](./architecture.md) — System design
- [Design Decisions](./design-decisions.md) — Why certain choices
- [Diataxis Framework](https://diataxis.fr/) — Documentation structure

*Last updated: 2026-03-11*
