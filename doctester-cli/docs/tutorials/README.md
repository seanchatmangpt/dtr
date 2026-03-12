# DTR CLI Tutorials

Welcome to the DTR CLI tutorials! These guides teach you how to use DTR CLI from the ground up.

## Learning Path

Follow these tutorials in order to master DTR CLI:

### 1. [Getting Started with DTR CLI](01-getting-started.md)
**Duration:** 5-10 minutes | **Difficulty:** Beginner

Learn to:
- Install DTR CLI
- Convert your first Markdown file to HTML
- Verify the installation

**Start here if:** You're new to DTR CLI or command-line tools.

### 2. [Creating Multi-Format Documentation](02-export-formats.md)
**Duration:** 10-15 minutes | **Difficulty:** Beginner

Learn to:
- Export to HTML (for web)
- Export to Markdown (for GitHub)
- Export to JSON (for integrations)
- Batch convert multiple files

**Start here if:** You understand basic commands and want to explore output formats.

### 3. [Integrating DTR CLI with Maven Projects](03-maven-integration.md)
**Duration:** 15-20 minutes | **Difficulty:** Intermediate

Learn to:
- Use DTR CLI with Maven projects
- Generate reports from test documentation
- Automate documentation with CI/CD
- Publish to GitHub Pages from Maven

**Start here if:** You're working with Java/Maven projects and want to automate docs.

### 4. [Publishing to Dev.to, Medium, and Other Platforms](04-blogging-workflow.md)
**Duration:** 15-20 minutes | **Difficulty:** Intermediate

Learn to:
- Export to blogging platforms (Dev.to, Medium, Hashnode, LinkedIn, Substack)
- Publish to multiple platforms at once
- Automate blogging with GitHub Actions
- Manage cross-posted content

**Start here if:** You want to reach broader audiences and syndicate your content.

## Quick Navigation

| Tutorial | Topic | Skills You'll Learn |
| --- | --- | --- |
| 01 | Getting Started | Installation, basic conversion, verification |
| 02 | Format Conversion | HTML, Markdown, JSON output formats |
| 03 | Maven Integration | Automation, CI/CD, GitHub Pages deployment |
| 04 | Blog Publishing | Multi-platform publishing, content syndication |

## Prerequisites

For all tutorials, you need:
- **Python 3.12+** installed
- A **terminal** or command prompt
- A **text editor** (VS Code, Sublime, Vim, etc.)

For Tutorial 3 (Maven Integration), also need:
- Java 25+ installed
- Maven 4.0.0+ installed

## Recommended Setup

Before starting Tutorial 1, prepare your environment:

```bash
# Check Python version
python3 --version  # Should show 3.12+

# Clone DTR repo
git clone https://github.com/seanchatmangpt/doctester.git
cd doctester/dtr-cli

# Create virtual environment
python3.12 -m venv venv
source venv/bin/activate  # or: venv\Scripts\activate on Windows

# Install
uv sync  # or: pip install -e .
```

Then you're ready to begin!

## Estimated Time

- **Complete all tutorials:** 45-60 minutes
- **Just basics (01-02):** 15-25 minutes
- **Just Java integration (03):** 15-20 minutes
- **Just blogging (04):** 15-20 minutes

## What You'll Be Able to Do

After completing these tutorials, you'll be able to:

✅ Install and verify DTR CLI
✅ Convert documentation to HTML, Markdown, and JSON
✅ Export in bulk using batch commands
✅ Generate reports from documentation
✅ Integrate with Maven builds
✅ Automate documentation generation
✅ Deploy to GitHub Pages
✅ Publish to Dev.to, Medium, Hashnode, LinkedIn, and Substack
✅ Syndicate content across platforms
✅ Set up CI/CD pipelines for documentation

## Next Steps After Tutorials

Once you've completed the tutorials, explore the How-To Guides:

- **[How-To Guides](../how-to/)** — Deep dives into specific tasks
- **[Reference Documentation](../reference/)** — Complete command reference
- **[FAQ](../faq.md)** — Frequently asked questions
- **[Troubleshooting](../troubleshooting.md)** — Solutions to common problems

## Get Help

- **Read:** Check the [main README](../../README.md)
- **Ask:** Open an [issue on GitHub](https://github.com/seanchatmangpt/doctester/issues)
- **Contribute:** See [CONTRIBUTING.md](../../CONTRIBUTING.md)

---

**Ready to start?** Begin with [Tutorial 1: Getting Started](01-getting-started.md) 🚀
