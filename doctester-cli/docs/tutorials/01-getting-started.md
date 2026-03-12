# Getting Started with DTR CLI

Welcome! DTR CLI helps you convert and publish documentation in multiple formats. Whether you're converting Markdown to HTML, managing documentation exports, or publishing to platforms like GitHub Pages, this guide will get you started in 5 minutes.

## What is DTR CLI?

DTR CLI is a powerful tool that:
- **Converts** documentation between formats (HTML, Markdown, JSON)
- **Generates** summary reports and changelogs
- **Publishes** documentation to GitHub Pages, AWS S3, Google Cloud Storage, or local folders
- **Manages** and archives your documentation exports

It's built with Python 3.12+ and designed to be fast, reliable, and easy to use.

## Before You Start

You'll need:
- **Python 3.12+** installed ([download](https://www.python.org/downloads/))
- **A terminal** or command prompt
- **A text editor** (VS Code, Sublime, Vim, etc.)
- **5-10 minutes** of your time
- A markdown file (or create one as we go)

Check your Python version:
```bash
python3 --version
```

You should see `Python 3.12.x` or higher. If not, install Python 3.12+.

## Installation

### Step 1: Clone the Repository

```bash
git clone https://github.com/seanchatmangpt/doctester.git
cd doctester/dtr-cli
```

### Step 2: Create a Virtual Environment

A virtual environment keeps your project dependencies isolated. It's a best practice.

```bash
# macOS / Linux
python3.12 -m venv venv
source venv/bin/activate

# Windows
python -m venv venv
venv\Scripts\activate
```

You'll see `(venv)` at the start of your terminal prompt. Good!

### Step 3: Install DTR CLI

Using `uv` (recommended, faster):
```bash
uv sync
```

Or using `pip`:
```bash
pip install -e .
```

### Step 4: Verify Installation

```bash
dtr --version
```

You should see: `dtr-cli, version 0.1.0`

Great! The CLI is ready to use.

## Your First Conversion

Let's convert a Markdown file to HTML.

### Create a Sample Markdown File

Create a file called `myguide.md`:

```markdown
# My First Guide

This is a paragraph explaining something important.

## Getting Started

DTR CLI is easy to use. Follow these steps:

1. Install Python 3.12
2. Clone the repository
3. Create a virtual environment
4. Run commands!

## Key Features

- **Fast** — Written in Python 3.12
- **Flexible** — Multiple output formats
- **Powerful** — Publish anywhere

## What's Next?

Check out the [How-To Guides](../how-to/formats.md) for advanced topics.
```

### Convert to HTML

```bash
dtr fmt html myguide.md -o myguide.html
```

This creates a file called `myguide.html`. Open it in your browser:

```bash
# macOS
open myguide.html

# Linux
xdg-open myguide.html

# Windows
start myguide.html
```

**Success!** You've converted your first file. The HTML is styled, readable, and ready to share.

## Try Another Format

Convert the same file to JSON:

```bash
dtr fmt json myguide.md -o myguide.json
```

Open `myguide.json` in your editor. You'll see a structured representation of your document.

## Common Commands

Here are the commands you'll use most often:

| Command | What It Does |
| --- | --- |
| `dtr fmt html <file>` | Convert Markdown → HTML |
| `dtr fmt md <file>` | Convert HTML → Markdown |
| `dtr fmt json <file>` | Convert HTML → JSON |
| `dtr --help` | Show all available commands |
| `dtr fmt --help` | Show format conversion options |

## Next Steps

You've learned:
- ✅ Install DTR CLI
- ✅ Convert Markdown to HTML
- ✅ Convert files to other formats

Ready to level up? Learn more:

- **[How to Use Different Formats](../how-to/formats.md)** — Explore HTML, Markdown, and JSON outputs
- **[Advanced Conversions](../how-to/rich-content.md)** — Add images, tables, and code blocks
- **[Publishing Your Docs](../how-to/publishing.md)** — Share on GitHub Pages or the cloud

## Troubleshooting

**"dtr: command not found"**
- Make sure your virtual environment is activated: `source venv/bin/activate`
- Reinstall: `pip install -e .`

**"Python 3.12 not found"**
- Install Python 3.12 from [python.org](https://www.python.org)
- Use `python3.12` explicitly: `python3.12 -m venv venv`

**"Permission denied"**
- On macOS/Linux, you may need to make the script executable: `chmod +x venv/bin/activate`

Need help? Check the [main README](../../README.md) or open an [issue on GitHub](https://github.com/seanchatmangpt/doctester/issues).

---

**Happy documenting!** 🎉
