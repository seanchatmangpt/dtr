# Glossary

A reference of terms and concepts used throughout DTR CLI documentation.

## A

### API Key
A unique identifier used for authentication when interacting with web services. DTR can publish to services using API keys.

### Automation
The process of running DTR commands automatically, typically through scripts, CI/CD pipelines, or scheduled jobs. See [Maven Integration](../tutorials/03-maven-integration.md).

## B

### Batch Export
Converting multiple documentation files in a single command. See [How-To: Batch Export](../how-to/batch-export.md).

### Blog Export
Publishing documentation directly to blogging platforms like Medium, Dev.to, or Hashnode. See [How-To: Blog Publishing](../how-to/blog-publishing.md).

### Build Integration
Integration of DTR with build tools like Maven to generate documentation automatically. See [Maven Integration](../tutorials/03-maven-integration.md).

## C

### CLI
Command-Line Interface. The text-based interface for interacting with DTR. Accessed via the `dtr` command.

### Configuration
Settings that control how DTR behaves, specified via YAML files or environment variables. See [Reference: Configuration](./configuration.md).

### Conversion
The process of transforming a document from one format (e.g., Markdown) to another format (e.g., HTML, PDF). The core function of DTR CLI.

## D

### Diataxis
A framework for organizing documentation into four types: Tutorials, How-To Guides, Reference, and Explanation. This documentation follows Diataxis principles.

### dtr
The command-line interface for DTR. Used to run conversions and publish documentation.

## E

### Environment Variables
Named values that control DTR behavior, set in the shell or OS configuration. Examples: `DTR_OUTPUT_FORMAT`, `DTR_GITHUB_TOKEN`. See [Reference: Environment Variables](./environment-variables.md).

### Exit Code
A numeric value returned by a command indicating success (0) or the type of error (1-10). See [Reference: Exit Codes](./exit-codes.md).

### Export
The process of saving a document in a specific format. DTR "exports" to HTML, PDF, Markdown, etc.

## F

### Format
A file type or standard for documents. Common formats: HTML, Markdown (MD), PDF, JSON, LaTeX, DOCX.

### Frontend
The user-facing interface of DocTester; the commands, options, and output users interact with directly.

## G

### GitHub Pages
A service for hosting static websites directly from GitHub repositories. DTR can publish to GitHub Pages. See [How-To: Blog Publishing](../how-to/blog-publishing.md).

### Glossary
A list of terms and definitions. This document.

## H

### HTML
HyperText Markup Language. The standard format for web pages. One of DocTester's primary output formats.

### How-To Guide
Documentation that solves specific, practical problems. Part of Diataxis structure.

## I

### Input
The source file or data provided to DTR for processing. Usually a Markdown file.

### Installation
The process of setting up DTR CLI on your system. See [Tutorial 1: Getting Started](../tutorials/01-getting-started.md).

## J

### JSON
JavaScript Object Notation. A lightweight data format. DTR can convert to/from JSON.

## K

### Keyword
A word used to search documentation or describe content.

## L

### LaTeX
A document preparation system commonly used for academic papers and technical documents. DTR can export to LaTeX/PDF.

## M

### Markdown
A lightweight markup language for creating formatted text. The default input format for DTR. Files end in `.md`.

### Maven
A build automation tool for Java projects. DTR integrates with Maven. See [Tutorial 3: Maven Integration](../tutorials/03-maven-integration.md).

### Multi-format
Capable of handling multiple input and output formats. DTR is a multi-format documentation tool.

## O

### Output
The file or result produced by DTR after conversion. Examples: `output.html`, `output.pdf`.

### Overwrite
To replace an existing file with new content. DTR will overwrite output files by default.

## P

### PDF
Portable Document Format. A common format for documents. DTR can convert to PDF via LaTeX.

### Publishing
The process of uploading or deploying documentation to a service (GitHub, S3, blog platform, etc.). See [How-To: Blog Publishing](../how-to/blog-publishing.md).

### Python
The programming language used to build DTR. Requires Python 3.12+.

## Q

### Query Parameter
A parameter passed in the command line, like `--help` or `--output`. See [Reference: CLI Commands](./cli-commands.md).

## R

### Reference
Documentation that lists commands, options, and configurations. Part of Diataxis structure.

### Rich Content
Advanced formatting like images, tables, code blocks, and mathematical equations. See [How-To: Rich Content](../how-to/rich-content.md).

## S

### Schema
A structured definition of how data is organized. Used for validating configuration files and output formats.

### Template
A predefined structure or format used for output. DTR uses templates for HTML, PDF, and other formats.

### Tool
A general reference to DTR CLI itself.

### Tutorial
Step-by-step instructional documentation. Part of Diataxis structure. See [Tutorials](../tutorials/).

## V

### Virtual Environment
An isolated Python environment that separates dependencies. Recommended for installing DTR. See [Getting Started](../tutorials/01-getting-started.md).

## W

### Workflow
A series of steps or commands used to accomplish a goal. Example: "documentation workflow" means the process of writing, converting, and publishing docs.

## X

### XML
Extensible Markup Language. A structured data format. DTR can handle some XML conversions.

## Y

### YAML
YAML Ain't Markup Language. A human-readable data format used for configuration files. See [Reference: Configuration](./configuration.md).

---

## Related Resources

- [CLI Commands Reference](./cli-commands.md) — All available commands
- [Configuration Reference](./configuration.md) — Configuration options
- [Getting Started Tutorial](../tutorials/01-getting-started.md) — Quick start guide

*Last updated: 2026-03-11*
