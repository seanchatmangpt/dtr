# DocTester CLI Documentation

Welcome to the comprehensive documentation for DocTester CLI — a powerful tool for converting and publishing documentation in multiple formats.

## 📚 Four Ways to Learn

Choose your path based on your needs:

### 🎓 **Tutorials** — Learning-Oriented
*"Show me step-by-step how to do this"*

Start here if you're new to DocTester CLI. Tutorials guide you through practical tasks with real examples:

- [Getting Started](./tutorials/01-getting-started.md) — Install and run your first conversion
- [Creating Multi-Format Docs](./tutorials/02-export-formats.md) — HTML, Markdown, JSON outputs
- [Maven Integration](./tutorials/03-maven-integration.md) — Integrate with Maven builds
- [Publishing to Blogs](./tutorials/04-blogging-workflow.md) — Share on Dev.to, Medium, etc.

**Time commitment:** 30 minutes to get productive

### 📖 **How-To Guides** — Problem-Oriented
*"I want to solve a specific problem"*

Answer your "how do I...?" questions with focused, practical solutions:

- [Export in Different Formats](./how-to/formats.md) — HTML, PDF, LaTeX, Markdown, JSON
- [Add Rich Content](./how-to/rich-content.md) — Images, tables, code, math, diagrams
- [Maven Integration](./how-to/maven-integration.md) — Continuous documentation generation
- [Batch Export](./how-to/batch-export.md) — Convert multiple files efficiently
- [Publishing Workflows](./how-to/blog-publishing.md) — Share on GitHub, blogs, and platforms
- [Troubleshooting](./how-to/troubleshooting.md) — Fix common issues and errors

**Use when:** You have a specific task or problem to solve

### 📋 **Reference** — Information-Oriented
*"What are all the options for X?"*

Complete, structured reference material for lookup and discovery:

- [CLI Commands](./reference/cli-commands.md) — All dtr commands with complete options
- [Environment Variables](./reference/environment-variables.md) — Configuration via environment
- [Configuration Files](./reference/configuration.md) — YAML config format and options
- [Exit Codes](./reference/exit-codes.md) — What each exit code means
- [Glossary](./reference/glossary.md) — Definitions and terminology

**Use when:** You need to look something up or understand all available options

### 💡 **Explanation** — Understanding-Oriented
*"Why does it work this way?"*

Understand the concepts, design decisions, and best practices behind DocTester:

- [Architecture](./explanation/architecture.md) — System design, components, and data flow
- [Design Decisions](./explanation/design-decisions.md) — Why certain choices were made
- [Performance Guide](./explanation/performance-guide.md) — Optimization, caching, and limits
- [Best Practices](./explanation/best-practices.md) — Patterns, conventions, and workflows
- [Troubleshooting Philosophy](./explanation/troubleshooting-philosophy.md) — How to debug effectively

**Use when:** You want to understand design philosophy and best practices

---

## 🚀 Quick Start

Get up and running in under 5 minutes:

```bash
# Install DocTester CLI
cd doctester/doctester-cli
python3 -m venv venv
source venv/bin/activate  # or: venv\Scripts\activate on Windows
pip install -e .

# Your first conversion
dtr fmt html myguide.md -o myguide.html

# View the result
open myguide.html  # macOS
xdg-open myguide.html  # Linux
start myguide.html  # Windows
```

**Next step:** [→ Getting Started Tutorial](./tutorials/01-getting-started.md)

---

## 📊 Documentation Map

```
docs/
├── index.md                    (you are here)
│
├── tutorials/                  [Learning-oriented: Step-by-step guides]
│   ├── 01-getting-started.md       (Install and first conversion)
│   ├── 02-export-formats.md        (Multiple output formats)
│   ├── 03-maven-integration.md     (Maven build integration)
│   └── 04-blogging-workflow.md     (Publishing workflow)
│
├── how-to/                     [Problem-oriented: How to solve X]
│   ├── formats.md                  (Format conversion reference)
│   ├── rich-content.md             (Images, tables, code, math)
│   ├── maven-integration.md        (CI/CD integration)
│   ├── batch-export.md             (Bulk conversions)
│   ├── blog-publishing.md          (Platform publishing)
│   └── troubleshooting.md          (Debugging and fixes)
│
├── reference/                  [Information-oriented: Complete reference]
│   ├── cli-commands.md             (All commands and options)
│   ├── environment-variables.md    (Env variable configuration)
│   ├── configuration.md            (YAML config format)
│   ├── exit-codes.md               (Exit code meanings)
│   └── glossary.md                 (Terminology)
│
└── explanation/                [Understanding-oriented: Why and how]
    ├── architecture.md             (System design)
    ├── design-decisions.md         (Rationale)
    ├── performance-guide.md        (Optimization)
    ├── best-practices.md           (Conventions)
    └── troubleshooting-philosophy.md (Debug methodology)
```

---

## 🎯 Choose Your Path

Not sure where to start? Use this table:

| Your Situation | Start Here |
|---|---|
| I'm brand new to DocTester | [Tutorial 1: Getting Started](./tutorials/01-getting-started.md) |
| I want to export to PDF | [How-To: Formats](./how-to/formats.md) |
| I need a complete command list | [Reference: CLI Commands](./reference/cli-commands.md) |
| I want to integrate with my build | [Tutorial 3: Maven Integration](./tutorials/03-maven-integration.md) |
| I'm getting an error | [How-To: Troubleshooting](./how-to/troubleshooting.md) |
| Why is this designed this way? | [Explanation: Architecture](./explanation/architecture.md) |
| I want to publish documentation | [Tutorial 4: Blogging Workflow](./tutorials/04-blogging-workflow.md) |
| I need to understand options for X | [Reference: CLI Commands](./reference/cli-commands.md) |

---

## 🔑 Key Features

DocTester CLI provides:

- **Multi-format conversion** — Convert between HTML, Markdown, JSON, LaTeX, and more
- **Rich formatting** — Support for images, tables, code blocks, math, and diagrams
- **Publishing ready** — Export to GitHub Pages, S3, GCS, or local storage
- **Batch operations** — Convert multiple files in one command
- **Maven integration** — Generate docs automatically during builds
- **Blog export** — Publish directly to Dev.to, Medium, Hashnode, and more
- **Configuration** — YAML-based or environment variable configuration
- **Performance** — Fast, parallel processing with caching

---

## ❓ FAQ

**Q: What versions of Python are supported?**
A: Python 3.12 and higher. See [Getting Started](./tutorials/01-getting-started.md).

**Q: Can I use DocTester with Maven?**
A: Yes! See [Tutorial 3: Maven Integration](./tutorials/03-maven-integration.md) for setup.

**Q: How do I convert multiple files at once?**
A: Use batch mode. See [How-To: Batch Export](./how-to/batch-export.md).

**Q: Can I publish to GitHub Pages?**
A: Yes. See [How-To: Blog Publishing](./how-to/blog-publishing.md).

**Q: How do I configure DocTester?**
A: Use environment variables or a YAML config file. See [Reference: Configuration](./reference/configuration.md).

**Q: What if something goes wrong?**
A: Check [How-To: Troubleshooting](./how-to/troubleshooting.md).

---

## 🔗 Related Resources

- **GitHub Repository:** https://github.com/seanchatmangpt/doctester
- **Issues & Bug Reports:** https://github.com/seanchatmangpt/doctester/issues
- **Discussions & Questions:** https://github.com/seanchatmangpt/doctester/discussions
- **Project README:** [Main README](../README.md)

---

## 📝 Documentation Quality

This documentation follows the [Diataxis](https://diataxis.fr/) framework:

- **Tutorials** teach through practical steps
- **How-to guides** solve specific problems
- **Reference** provides complete, organized information
- **Explanation** explores understanding and concepts

Each section serves a different user need. Start where you are; learn what you need.

---

## 🤝 Contributing

Found a typo? Have a better example? Want to improve the docs?

1. [Fork the repository](https://github.com/seanchatmangpt/doctester/fork)
2. Make your changes
3. Submit a pull request

See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

---

## 📄 License

DocTester CLI is licensed under the Apache 2.0 License. See [LICENSE](../LICENSE) for details.

---

*Last updated: 2026-03-11*
*Documentation version: 2.5.0*
