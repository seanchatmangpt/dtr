# How to Troubleshoot Common Issues

## Problem
Something is broken. Your command failed, files won't export, or the output looks wrong. You need a systematic way to diagnose and fix DTR CLI issues.

## Solution Overview

This guide covers the most common DTR CLI errors, their causes, and solutions. Use the error table below to jump to your specific issue, or follow the diagnostic steps to identify the problem.

## Quick Diagnostic Checklist

Before diving into specific errors, verify the basics:

```bash
# 1. Check DTR is installed
dtr --version
# Expected: dtr 2.5.0

# 2. Verify Java is available
java -version
# Expected: Java 25+

# 3. Test basic command
dtr --help
# Should show command list

# 4. Verify file exists
ls -la guide.md
# Should show file details

# 5. Check output directory is writable
touch output/.write-test && rm output/.write-test
# Should complete without error
```

## Common Errors and Solutions

### File-Related Errors

#### "File not found" or "No such file or directory"
```
Error: guide.md: No such file or directory
```

**Causes:**
- File path is incorrect
- File was deleted or moved
- Relative path doesn't match current directory

**Solutions:**
```bash
# Use absolute path instead of relative
dtr export /home/user/docs/guide.md --format html

# Verify file exists
ls -la guide.md

# Use pwd to check current directory
pwd

# Find file if unsure of location
find . -name "guide.md" -type f
```

#### "Permission denied"
```
Error: guide.md: Permission denied
```

**Causes:**
- File is not readable by your user
- Output directory is not writable
- Temporary directory full or unavailable

**Solutions:**
```bash
# Check file permissions
ls -la guide.md
# Should show: -rw-r--r--

# Fix if needed (make readable)
chmod 644 guide.md

# Check output directory permissions
ls -la output/
# Should show: drwxr-xr-x

# Make writable if needed
mkdir -p output && chmod 755 output

# Check disk space
df -h
# Ensure at least 1GB free space
```

### Format and Conversion Errors

#### "Invalid format"
```
Error: Unknown format: 'pdf'
Valid formats: markdown, html, latex, slides, blog, openapi
```

**Cause:** Typo or unsupported format specified

**Solution:**
```bash
# Use correct format name
dtr export guide.md --format latex  # NOT 'pdf'

# View all valid formats
dtr formats --list

# Or use --help to see examples
dtr export --help
```

#### "Conversion failed" (generic error)
```
Error: Conversion failed for guide.md (exit code 1)
Use --debug for more information
```

**Cause:** Various causes; need to enable debugging

**Solution:**
```bash
# Enable debug output to see actual error
dtr export guide.md --format html --debug

# Or use verbose mode
dtr export guide.md --format html -v

# Save debug output to file for review
dtr export guide.md --format html --debug 2>&1 | tee debug.log
```

#### "LaTeX compilation failed"
```
Error: LaTeX compilation failed. pdflatex not found.
```

**Cause:** TeX distribution not installed

**Solution:**
```bash
# Linux (Debian/Ubuntu)
sudo apt-get install texlive-xetex texlive-fonts-recommended

# macOS
brew install mactex

# Or use alternative compiler (if installed)
dtr export guide.md --format latex --latex-compiler xelatex

# Verify installation
which pdflatex
pdflatex --version
```

#### "Invalid markdown syntax"
```
Error: Markdown parsing failed at line 42
```

**Cause:** Syntax error in the markdown file

**Solution:**
```bash
# Validate markdown before converting
dtr validate guide.md

# Output will show line numbers with errors
# Common issues:
# - Missing closing ``` fence
# - Unmatched brackets
# - Invalid URL syntax

# View the problem area
sed -n '40,45p' guide.md

# Fix syntax, then retry
dtr export guide.md --format html
```

### Batch Export Errors

#### "Pattern matches no files"
```
Error: Pattern "**/*.md" matched 0 files
```

**Cause:** Glob pattern doesn't match any files or is incorrect

**Solution:**
```bash
# Test pattern with ls first
ls "**/*.md"

# Or use find
find . -name "*.md" -type f

# Check current directory
pwd

# Try simpler pattern
dtr export "*.md" --format html --output-dir output/

# Enable dry-run to preview what would match
dtr export "src/docs/*.md" --format html --dry-run
```

#### "Out of memory during parallel export"
```
Error: Java heap space
```

**Cause:** Too many parallel workers for available RAM

**Solution:**
```bash
# Reduce parallel workers
dtr export src/docs/ --format html --parallel 5

# Or use single-threaded mode
dtr export src/docs/ --format html --parallel 1

# Increase memory allocation
export JAVA_OPTS="-Xmx2g"
dtr export src/docs/ --format html --parallel auto
```

#### "Some files failed in batch"
```
3 files failed out of 50
```

**Cause:** Individual file conversion errors in batch

**Solution:**
```bash
# Get detailed error report
dtr export src/docs/ --continue-on-error --error-report errors.txt

# View error report
cat errors.txt

# Check which specific files failed
grep "ERROR" errors.txt | cut -d: -f1

# Fix problematic files individually
dtr validate src/docs/problematic-file.md

# Then retry just the failed files
dtr export src/docs/problematic-file.md --format html
```

### Platform Integration Errors

#### "Plugin not found"
```
[ERROR] Failed to execute goal org.r10r:dtr-maven-plugin
Error: Could not find artifact org.r10r:dtr-maven-plugin:2.5.0
```

**Cause:** Maven plugin not installed or wrong version

**Solution:**
```bash
# Check Maven can download plugin
mvn help:describe -Dplugin=org.r10r:dtr-maven-plugin:2.5.0

# Force update of Maven cache
mvn -U clean install

# Verify plugin version exists
mvn org.r10r:dtr-maven-plugin:2.5.0:help

# Use correct groupId/artifactId (check pom.xml)
# <groupId>org.r10r</groupId>
# <artifactId>dtr-maven-plugin</artifactId>
# <version>2.5.0</version>
```

#### "Authentication failed" (Blog Publishing)
```
Error: Authentication failed for Dev.to API
```

**Cause:** API key invalid, expired, or misconfigured

**Solution:**
```bash
# Verify API key is in config
cat ~/.config/doctester/platforms.yaml | grep devto

# Regenerate API key on platform
# Dev.to: https://dev.to/settings/account → API Keys

# Test API key manually
curl -H "api-key: YOUR_KEY" https://dev.dev/api/articles

# Use environment variable instead
export DTR_DEVTO_API_KEY="your_new_key"
dtr publish guide.md --platform devto --dry-run

# Check permissions on config file
ls -la ~/.config/doctester/platforms.yaml
# Should be: -rw------- (only readable by you)
```

#### "Post not appearing after publish"
```
Publish successful, but post not visible on platform
```

**Cause:** Post in draft mode or scheduled for future

**Solution:**
```bash
# Check post was published live
dtr queue list

# Post in DRAFT status should be manually published
# Visit platform's editor to publish draft

# Or republish with --live flag
dtr publish guide.md --platform devto --live

# Check if scheduled
grep "scheduled_for:" guide.md

# For scheduled posts, visit platform to verify time
```

### Template and Style Errors

#### "Template not found"
```
Error: Template 'acm-conference' not found
```

**Cause:** Template name is incorrect or not installed

**Solution:**
```bash
# List available templates
dtr templates --list

# Output shows:
# LaTeX Templates:
#   - acm-conference
#   - arxiv
#   - ieee
#   - nature
#   - us-patent

# Use correct template name (all lowercase)
dtr export guide.md --format latex --template acm-conference

# Check for typos
# Common mistake: 'acm_conference' (underscore) → use hyphen
```

#### "Syntax highlighting not applied"
```
Code blocks show without colors in output
```

**Cause:** Language identifier missing or theme not loaded

**Solution:**
```markdown
# Wrong (no language identifier)
\`\`\`
def hello():
    print("Hello")
\`\`\`

# Correct (language identifier specified)
\`\`\`python
def hello():
    print("Hello")
\`\`\`
```

```bash
# Verify highlight theme is valid
dtr templates --highlight-themes

# Use correct theme name
dtr export guide.md --format html --highlight-theme atom-dark
```

### Image and Media Errors

#### "Image not found" in output
```
Image references broken in exported HTML/PDF
```

**Cause:** Image path is relative and not found during export

**Solution:**
```bash
# Use absolute URLs instead of relative paths
# Wrong:
![Screenshot](images/screenshot.png)

# Correct:
![Screenshot](https://example.com/images/screenshot.png)

# Or place images in same directory as markdown
# Then use:
![Screenshot](./screenshot.png)

# Verify image exists
ls -la images/screenshot.png

# Test image is accessible
file images/screenshot.png
# Should show: PNG image data, ...
```

#### "Large images slow down export"
```
Export takes very long when processing many large images
```

**Cause:** Images are large or not optimized

**Solution:**
```bash
# Optimize images before export
for f in images/*.png; do
    pngquant --ext .png --force $f
done

# Or use ImageMagick to resize
mogrify -resize 1024x768 images/*.png

# Use format that compresses better
# PNG for screenshots, JPG for photos

# For batch export, skip images
dtr export guide.md --format html --skip-images

# Include minimal images
dtr export guide.md --format markdown  # Text only, fastest
```

## Advanced Debugging

### Enable Full Debug Output
```bash
# Maximum verbosity
dtr export guide.md --format html --debug -v

# Save everything to log file
dtr export guide.md --format html --debug 2>&1 | tee full-debug.log

# Analyze log for patterns
grep ERROR full-debug.log
grep WARN full-debug.log
```

### Check System Resources
```bash
# Available disk space
df -h

# Available memory
free -h

# CPU usage
top -b -n 1

# Java memory settings
echo $JAVA_OPTS

# Java version
java -version
```

### Test with Minimal File
```bash
# Create simple test file
cat > test.md << EOF
# Test Document

This is a test.
EOF

# Try export with test file
dtr export test.md --format html

# If test succeeds, original file has issues
# If test fails, system-wide problem
```

### Reset to Defaults
```bash
# Clear all caches
dtr cache --clear

# Reset configuration
rm -rf ~/.config/doctester/

# Reinstall CLI
dtr --version  # Check version
# (Reinstall via package manager if corrupted)
```

## Getting Help

If you're still stuck:

```bash
# 1. Check online documentation
dtr docs --open

# 2. View command help
dtr help export

# 3. Report bug with system info
dtr bug-report > system-info.txt

# 4. Check GitHub issues
# https://github.com/r10r/doctester/issues

# 5. Ask in community forums
# https://github.com/r10r/doctester/discussions
```

## Creating a Good Bug Report

When reporting an issue:

```bash
# Gather system information
dtr bug-report > bug-details.txt

# Run failing command with debug output
dtr export problem-file.md --format html --debug 2>&1 | tee error.log

# Include in bug report:
# 1. System info (bug-details.txt)
# 2. Command that failed (error.log)
# 3. Input file (problem-file.md) or sample
# 4. Expected vs actual output
```

## Common Workarounds

| Issue | Workaround |
|-------|-----------|
| TeX not installed | Use HTML instead: `--format html` |
| Memory issues | Use `--parallel 1` to disable threading |
| Slow exports | Use `--format markdown` (fastest) |
| Image problems | Remove images, use `--skip-images` |
| Markdown errors | Validate first: `dtr validate file.md` |
| Batch too large | Split into smaller batches |

## Next Steps
- [Export in Different Formats](formats.md)
- [Integrate with Maven](maven-integration.md)
- Visit [GitHub Issues](https://github.com/r10r/doctester/issues) for platform-specific bugs
