# Exit Codes Reference

DTR CLI uses standard exit codes to indicate success or failure. This reference documents all possible exit codes and what they mean.

## Exit Code Summary

| Code | Meaning | Description |
|------|---------|-------------|
| `0` | Success | Command completed successfully |
| `1` | General Error | Unspecified error occurred |
| `2` | Invalid Arguments | Command-line arguments are invalid |
| `3` | File Not Found | Input file does not exist |
| `4` | Permission Denied | Insufficient permissions to read/write file |
| `5` | Invalid Format | Input format not supported or malformed |
| `6` | Output Error | Cannot write to output file or directory |
| `7` | Configuration Error | Configuration file is invalid or missing required fields |
| `8` | Authentication Error | Failed to authenticate with publishing service |
| `9` | Network Error | Network connection failed or timeout |
| `10` | Template Error | Template not found or invalid |

## Detailed Exit Code Reference

### 0 — Success

The command completed successfully. Output files have been created and are ready to use.

```bash
dtr fmt html input.md -o output.html
# Exit code: 0 (file created successfully)
```

### 1 — General Error

An unexpected error occurred. Check the error message for more details. This can happen for various reasons not covered by specific exit codes.

```bash
dtr fmt html corrupted.md -o output.html
# Exit code: 1 (unexpected error during processing)
```

### 2 — Invalid Arguments

The command-line arguments are invalid. This includes missing required arguments, unknown commands or options, and conflicting arguments.

```bash
dtr fmt unknown-format input.md
# Exit code: 2 (format is unknown)

dtr fmt html  # Missing input file
# Exit code: 2 (missing required argument)
```

**How to fix:**
- Run `dtr --help` to see available commands
- Run `dtr <command> --help` for command-specific options
- Check argument syntax in [CLI Commands Reference](./cli-commands.md)

### 3 — File Not Found

The input file does not exist or cannot be accessed.

```bash
dtr fmt html nonexistent.md
# Exit code: 3 (file does not exist)
```

**How to fix:**
- Verify the file path is correct
- Check that the file exists: `ls -la input.md`
- Use absolute paths for clarity

### 4 — Permission Denied

Insufficient permissions to read the input file or write to the output location.

```bash
dtr fmt html input.md -o /root/output.html
# Exit code: 4 (cannot write to directory)
```

**How to fix:**
- Check file permissions: `ls -la input.md`
- Make file readable: `chmod +r input.md`
- Check output directory permissions

### 5 — Invalid Format

The input file format is not supported or the file is malformed.

```bash
dtr fmt html malformed.md
# Exit code: 5 (invalid markdown syntax)
```

**How to fix:**
- Verify the file format is supported
- Check markdown syntax
- Convert the file to a supported format first

### 6 — Output Error

Cannot write to the output file or directory.

```bash
dtr fmt html input.md -o /readonly/output.html
# Exit code: 6 (directory is read-only)
```

**How to fix:**
- Check available disk space: `df -h`
- Verify output directory exists and is writable
- Use a different output location

### 7 — Configuration Error

The configuration file is invalid or missing required fields.

```bash
dtr -c config.yaml fmt html input.md
# Exit code: 7 (config.yaml is invalid)
```

**How to fix:**
- Validate YAML syntax
- Check that required fields are present
- See [Configuration Reference](./configuration.md) for details

### 8 — Authentication Error

Failed to authenticate with a publishing service.

```bash
dtr publish github input.md --token invalid-token
# Exit code: 8 (authentication failed)
```

**How to fix:**
- Verify API token or credentials are correct
- Check token has not expired
- Set environment variables correctly

### 9 — Network Error

Network connection failed, timeout, or service is unreachable.

```bash
dtr publish github input.md
# Exit code: 9 (cannot reach github.com)
```

**How to fix:**
- Check internet connection
- Verify service is online
- Increase timeout if needed

### 10 — Template Error

Template file not found or is invalid.

```bash
dtr fmt html input.md --template missing.html
# Exit code: 10 (template not found)
```

**How to fix:**
- Verify template file exists
- Check template path is correct
- Use a built-in template instead

## Using Exit Codes in Scripts

Exit codes are useful for automation:

```bash
#!/bin/bash

dtr fmt html input.md -o output.html

if [ $? -eq 0 ]; then
    echo "✓ Conversion successful"
else
    echo "✗ Conversion failed"
    exit 1
fi
```

---

## See Also

- [CLI Commands Reference](./cli-commands.md)
- [Troubleshooting Guide](../how-to/troubleshooting.md)

*Last updated: 2026-03-11*
