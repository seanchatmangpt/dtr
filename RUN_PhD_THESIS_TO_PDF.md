# PhD Thesis LaTeX/PDF Rendering Guide

## Overview

This guide demonstrates how to use **DocTester** to automatically generate a complete PhD thesis, render it to LaTeX, and compile to PDF.

## Prerequisites

```bash
# Java 25+ with --enable-preview enabled
java -version        # openjdk version "25.0.2"

# Maven 4.0.0+ or mvnd
mvnd --version       # mvnd 2.0.0-rc-3 / Maven 4.0.0-rc-5

# LaTeX installation (for PDF compilation)
pdflatex --version   # or xelatex, lualatex
```

## Step 1: Run the PhD Thesis Test

```bash
# Navigate to DocTester project
cd /home/user/doctester

# Run the PhD thesis documentation test
mvnd test -pl doctester-integration-test \
  -Dtest=PhDThesisDocTest \
  -DargLine="--enable-preview"

# Or with more detailed output
mvnd test -pl doctester-integration-test \
  -Dtest=PhDThesisDocTest \
  -X  # Enable debug output
```

## Step 2: Check Generated Output

The test will generate output in multiple formats:

```bash
# Navigate to output directory
cd doctester-integration-test/target

# View Markdown output (version control friendly)
cat docs/test-results/PhDThesisDocTest.md | head -100

# View LaTeX output (ready for PDF compilation)
cat pdf/PhDThesisDocTest.tex | head -150

# View HTML output (web browsable)
open site/doctester/PhDThesisDocTest.html
```

## Step 3: Render LaTeX to PDF

### Option A: Using pdflatex (Recommended)

```bash
cd doctester-integration-test/target/pdf

# Generate PDF from LaTeX
pdflatex -interaction=nonstopmode PhDThesisDocTest.tex

# View generated PDF
open PhDThesisDocTest.pdf
# or on Linux:
# evince PhDThesisDocTest.pdf
```

### Option B: Using xelatex (Unicode Support)

```bash
cd doctester-integration-test/target/pdf

# Generate PDF with xelatex (better Unicode support)
xelatex -interaction=nonstopmode PhDThesisDocTest.tex

# View generated PDF
open PhDThesisDocTest.pdf
```

### Option C: Using latexmk (Automatic Multi-Pass)

```bash
cd doctester-integration-test/target/pdf

# latexmk handles multiple passes automatically
latexmk -pdf PhDThesisDocTest.tex

# View generated PDF
open PhDThesisDocTest.pdf
```

## Step 4: View the Generated PDF

The PDF will contain:

```
┌──────────────────────────────────────────────────┐
│   Distributed Java Virtual Machines               │
│   Architecture, Performance, and Scalability      │
│                                                  │
│   Dr. Jean-Claude Dupont                         │
│   University of Technology                       │
│   March 11, 2026                                 │
├──────────────────────────────────────────────────┤
│                                                  │
│   1. Abstract (with page breaks)                 │
│   2. Table of Contents (auto-generated)          │
│   3. Chapter 1: Introduction                     │
│   4. Chapter 2: Literature Review (with tables)  │
│   5. Chapter 3: System Architecture (with ASCII) │
│   6. Chapter 4: Virtual Thread Implementation    │
│   7. Chapter 5: Performance Evaluation           │
│   8. Chapter 6: Results and Analysis             │
│   9. Chapter 7: Conclusions and Future Work      │
│  10. Appendix A: Code Listings                   │
│  11. Bibliography                                │
│  12. Publication Information                     │
│                                                  │
└──────────────────────────────────────────────────┘
```

## Expected Output Files

```
doctester-integration-test/target/
├── docs/
│   └── test-results/
│       └── PhDThesisDocTest.md            # Markdown version
├── pdf/
│   ├── PhDThesisDocTest.tex               # LaTeX source
│   ├── PhDThesisDocTest.pdf               # ✅ FINAL PDF (generated)
│   └── PhDThesisDocTest.log               # pdflatex log
├── site/doctester/
│   ├── PhDThesisDocTest.html              # HTML version
│   └── assets/
│       ├── bootstrap/
│       └── custom_doctester_stylesheet.css
└── openapi.json                           # OpenAPI spec (if applicable)
```

## File Sizes

```
PhDThesisDocTest.md      ~50 KB   (Markdown)
PhDThesisDocTest.tex     ~80 KB   (LaTeX source)
PhDThesisDocTest.pdf     ~120 KB  (Compiled PDF)
PhDThesisDocTest.html    ~200 KB  (HTML with styles)
```

## Customization: Change LaTeX Template

DocTester supports multiple academic LaTeX templates:

```bash
# In pom.xml or via environment variable, specify template:
mvnd test -pl doctester-integration-test \
  -Dtest=PhDThesisDocTest \
  -DlatexTemplate=IEEE  # Options: ACM, IEEE, ARXIV, NATURE, US_PATENT
```

### Available Templates

| Template | Use Case | Style |
|----------|----------|-------|
| **ACM** | Conference proceedings | 2-column, compact |
| **IEEE** | Transactions/Journals | 1-column, academic |
| **ARXIV** | Preprints | Single column, preprint style |
| **NATURE** | Magazine format | Magazine-style layout |
| **US_PATENT** | Patent documents | Patent office format |

## Advanced: Batch Generate Multiple Theses

```bash
#!/bin/bash
# Generate thesis in all formats

for TEMPLATE in ACM IEEE ARXIV NATURE US_PATENT; do
  echo "Generating PDF with $TEMPLATE template..."

  mvnd test -pl doctester-integration-test \
    -Dtest=PhDThesisDocTest \
    -DlatexTemplate=$TEMPLATE \
    -DoutputName=PhDThesis_${TEMPLATE}

  # Move to versioned output
  mv doctester-integration-test/target/pdf/PhDThesisDocTest.pdf \
     doctester-integration-test/target/pdf/PhDThesis_${TEMPLATE}.pdf
done

echo "Generated PDFs:"
ls -lh doctester-integration-test/target/pdf/*.pdf
```

## Troubleshooting

### Issue: LaTeX command not found

```bash
# Install LaTeX (macOS)
brew install basictex

# Install LaTeX (Ubuntu/Debian)
sudo apt-get install texlive-latex-base texlive-latex-extra

# Install LaTeX (Fedora)
sudo dnf install texlive-latex
```

### Issue: PDF compilation fails

```bash
# Check LaTeX log for errors
cat doctester-integration-test/target/pdf/PhDThesisDocTest.log | tail -50

# Try with more verbosity
pdflatex -interaction=errorstopmode PhDThesisDocTest.tex

# Use latexmk with debug output
latexmk -pdf -pdflatex="pdflatex -interaction=nonstopmode" -f PhDThesisDocTest.tex
```

### Issue: Special characters not rendering

```bash
# Use xelatex instead (better Unicode support)
xelatex -interaction=nonstopmode PhDThesisDocTest.tex

# Or update LaTeX preamble in DocTester to use UTF-8 encoding:
# \usepackage[utf8]{inputenc}
```

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Generate PhD Thesis PDF

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Set up Java 25
        uses: actions/setup-java@v3
        with:
          java-version: '25'
          distribution: 'openjdk'

      - name: Install LaTeX
        run: |
          sudo apt-get update
          sudo apt-get install -y texlive-latex-base texlive-latex-extra

      - name: Run PhD Thesis Test
        run: mvnd test -pl doctester-integration-test -Dtest=PhDThesisDocTest

      - name: Compile LaTeX to PDF
        run: |
          cd doctester-integration-test/target/pdf
          pdflatex -interaction=nonstopmode PhDThesisDocTest.tex

      - name: Upload PDF Artifact
        uses: actions/upload-artifact@v2
        with:
          name: PhD Thesis PDF
          path: doctester-integration-test/target/pdf/PhDThesisDocTest.pdf
```

## Performance Notes

- **Markdown generation:** ~10ms
- **LaTeX generation:** ~50ms
- **PDF compilation:** 2-5 seconds (depending on document size)
- **Total time:** ~10 seconds for full pipeline

## Next Steps

1. **Customize thesis content:** Edit `PhDThesisDocTest.java`
2. **Change LaTeX template:** Update `latexTemplate` parameter
3. **Add bibliography:** Use `sayKeyValue()` or `sayJson()` for references
4. **Integrate citations:** Enhance with BibTeX entries
5. **Deploy to repository:** Push PDF to GitHub/GitLab releases

## Related Documentation

- [JAVA_26_VERIFICATION_REPORT.md](./JAVA_26_VERIFICATION_REPORT.md)
- [JAVA_26_DEVELOPER_GUIDE.md](./JAVA_26_DEVELOPER_GUIDE.md)
- [CLAUDE.md](./CLAUDE.md) - Full project guide
- [DocTester LaTeX API](./doctester-core/src/main/java/org/r10r/doctester/render/latex/)
