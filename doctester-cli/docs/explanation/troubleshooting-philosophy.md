# Troubleshooting Philosophy

This document explains how to think about and approach troubleshooting DTR CLI problems. Rather than just listing fixes, it teaches you *how* to diagnose and resolve issues systematically.

## The Troubleshooting Mindset

### Principles

1. **Gather Information** — Understand what went wrong before attempting fixes
2. **Isolate Variables** — Change one thing at a time to identify the cause
3. **Reproduce Consistently** — Can you make it happen again? If so, you can debug it
4. **Read Error Messages** — They usually tell you exactly what's wrong
5. **Check Assumptions** — Things you think should work might not
6. **Test the Fix** — Verify the solution works, don't just hope

### The Debug Loop

```
1. Observe the problem
   ↓
2. Form a hypothesis
   ↓
3. Gather evidence
   ↓
4. Test the hypothesis
   ↓
5. Implement fix (if hypothesis confirmed)
   ↓
6. Verify fix works
   ↓
7. Document for future reference
```

## Error Message Analysis

### Reading Error Messages

Error messages from DTR follow a pattern:

```
Error: [Type] — [What went wrong] — [Location/Context]

Example:
Error: File Not Found — /home/user/missing.md does not exist — Line 1 of input
```

**Always read the error message carefully.** It usually tells you:
- What failed
- Why it failed
- Where it failed
- Suggestions for fixing it

### Common Error Patterns

**"No such file or directory"**
- Problem: Input file doesn't exist
- Action: Check file path, verify file exists
- Command: `ls -la /path/to/file.md`

**"Permission denied"**
- Problem: Insufficient access to file/directory
- Action: Check file permissions, change if needed
- Command: `chmod +r file.md` or `ls -la file.md`

**"Invalid syntax"**
- Problem: File format is malformed
- Action: Check syntax, validate file
- Tool: Use a Markdown validator or editor with syntax highlighting

**"timeout" or "connection refused"**
- Problem: Network or service issue
- Action: Check network, verify service is running
- Command: `ping host` or `curl url`

## Systematic Troubleshooting Approach

### Step 1: Reproduce the Problem

Can you make the problem happen consistently?

```bash
# Try the exact command that failed
dtr fmt html myfile.md -o output.html

# Did it fail again? Good, you can debug it.
# Does it work sometimes? It's intermittent — check resources, network, etc.
```

**Why:** If you can't reproduce it, you can't debug it.

### Step 2: Check the Error Code

DTR returns an exit code. Check what it means:

```bash
dtr fmt html myfile.md -o output.html
echo $?  # Print the exit code
# 3 = File Not Found
# 5 = Invalid Format
# 4 = Permission Denied
```

See [Exit Codes Reference](../reference/exit-codes.md) for complete list.

### Step 3: Isolate the Variable

**Is it the input file?**

```bash
# Try a different input file
dtr fmt html test-file.md -o output.html

# Try a simple test file
echo "# Test" > simple.md
dtr fmt html simple.md -o output.html

# Did the simple file work? Then the problem is in your original file.
```

**Is it the output location?**

```bash
# Try a different output directory
dtr fmt html myfile.md -o /tmp/output.html

# Try the current directory
dtr fmt html myfile.md -o output.html

# Did a different location work? Check permissions on the original location.
```

**Is it the command?**

```bash
# Try the simplest version of the command
dtr fmt html myfile.md

# Try with all options
dtr fmt html myfile.md -o output.html --template default

# Did the simple version work? The issue is with an option you added.
```

### Step 4: Check System Resources

Sometimes problems are environmental:

```bash
# Check disk space
df -h

# Check available memory
free -h

# Check CPU usage
top -b -n 1 | head -15

# Are you out of space or memory? Free up resources.
```

### Step 5: Check Configuration

If using a config file:

```bash
# Validate the configuration
dtr config validate

# Check for typos
cat ~/.doctester/config.yaml

# Try without configuration
dtr fmt html myfile.md -o output.html  # No -c option
```

### Step 6: Check Permissions

For file-related errors:

```bash
# Check file permissions
ls -la myfile.md

# Check directory permissions
ls -la ./output/

# Try with explicit permissions
chmod 644 myfile.md
mkdir -p output
chmod 755 output
dtr fmt html myfile.md -o output/result.html
```

### Step 7: Check Dependencies

DTR requires certain things to be installed:

```bash
# Check Python version
python3 --version  # Should be 3.12+

# Check DTR is installed
dtr --version

# Check dependencies
pip list | grep -i doctester
```

### Step 8: Enable Verbose Mode

Get more information about what's happening:

```bash
# Run with verbose output
dtr -v fmt html myfile.md -o output.html

# Or with debug mode (if available)
dtr -vv fmt html myfile.md -o output.html

# Log output to file for review
dtr fmt html myfile.md -o output.html 2>&1 | tee debug.log
```

## Common Problems and Solutions

### Problem: "dtr: command not found"

**Hypothesis:** DTR is not installed or not on PATH

**Debug:**
```bash
which dtr
pip list | grep doctester
echo $PATH
```

**Solutions:**
- Install: `pip install -e ./dtr-cli/`
- Activate venv: `source venv/bin/activate`
- Add to PATH: Export in `.bashrc` or `.zshrc`

### Problem: "Invalid markdown syntax"

**Hypothesis:** Your Markdown file has syntax errors

**Debug:**
```bash
# Check the file
cat myfile.md

# Use a Markdown validator
dtr validate myfile.md

# Check specific issues
grep -n "^#" myfile.md  # Check headings
```

**Solutions:**
- Fix syntax errors (use a Markdown editor with validation)
- Check for unmatched backticks, brackets, etc.
- Validate with `dtr validate myfile.md`

### Problem: "Out of memory"

**Hypothesis:** DTR needs more RAM or file is too large

**Debug:**
```bash
# Check file size
ls -lh myfile.md

# Check memory usage
free -h
top
```

**Solutions:**
- Reduce threads: `dtr batch --threads 1`
- Limit memory: `dtr batch --max-memory 512M`
- Split large file into smaller parts
- Increase available RAM or close other apps

### Problem: "Network timeout"

**Hypothesis:** Network is slow or service is unreachable

**Debug:**
```bash
# Check internet connection
ping github.com

# Check if service is up
curl https://github.com

# Check DNS resolution
nslookup github.com
```

**Solutions:**
- Increase timeout: `dtr publish --timeout 30`
- Check internet connection
- Verify service is online
- Try again later if service is down

## Asking for Help

If you're stuck, gather this information before asking for help:

1. **The command you ran:**
   ```bash
   dtr fmt html input.md -o output.html
   ```

2. **The error message (complete):**
   ```
   Error: File Not Found — /home/user/input.md does not exist
   Exit code: 3
   ```

3. **What you've tried:**
   - Checked file exists: `ls -la /home/user/input.md` — file not found
   - Tried different path: `/tmp/input.md` — same error
   - Reinstalled: `pip install -e .` — still fails

4. **Your environment:**
   - OS: macOS Ventura / Linux Ubuntu 22.04 / Windows 11
   - Python: `python3 --version` → Python 3.12.0
   - DocTester: `dtr --version` → dtr-cli, version 0.1.0

5. **Reproduction steps:**
   - `echo "# Test" > test.md`
   - `dtr fmt html test.md -o output.html`
   - Result: Error...

## Documentation and Resources

### When to Check Documentation

- **Getting Started:** First time using DocTester
- **How-To Guides:** "How do I...?" questions
- **Reference:** Looking up options, exit codes, commands
- **Troubleshooting Guide:** Common issues and solutions
- **Glossary:** Definition of unfamiliar terms

### When to Report an Issue

Report a bug if:
- Error message doesn't make sense
- Command fails with no clear reason
- Behavior is unexpected or incorrect
- Other users might have the same problem

**Report at:** https://github.com/seanchatmangpt/doctester/issues

## Learning from Problems

### Keep a Debug Log

Record what went wrong and how you fixed it:

```markdown
## Issue: "Permission denied" on output.html

**Date:** 2026-03-11
**Command:** dtr fmt html input.md -o output.html
**Error:** Permission denied: output.html
**Root Cause:** Output directory was read-only
**Fix:** `chmod +w ./` to make directory writable
**Prevention:** Check permissions before running batch conversions
```

This helps you solve similar problems faster next time.

### Improve Error Messages

If an error message is confusing, suggest an improvement:
- Create an issue on GitHub
- Describe what was confusing
- Suggest clearer wording
- Help improve DTR for everyone

## Prevention is Better Than Cure

### Best Practices to Avoid Problems

1. **Validate before running:** `dtr validate myfile.md`
2. **Test with simple examples:** Use `echo "# Test" > test.md` first
3. **Check permissions early:** `ls -la input.md` before processing
4. **Use absolute paths:** `/home/user/docs/` instead of `docs/`
5. **Keep resources available:** Don't run during heavy system load
6. **Review error messages:** Take time to understand what went wrong
7. **Test fixes thoroughly:** Verify the solution actually works

---

## See Also

- [Exit Codes Reference](../reference/exit-codes.md) — Exit code meanings
- [How-To: Troubleshooting](../how-to/troubleshooting.md) — Common problems and solutions
- [Getting Started](../tutorials/01-getting-started.md) — Basic setup

*Last updated: 2026-03-11*
