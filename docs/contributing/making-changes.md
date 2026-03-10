# Contributing: Making Changes

## Before you start

- **Check existing issues** — your bug or feature may already be tracked
- **Open an issue first** for significant changes — design discussion before code
- **Small, focused PRs** — one feature or fix per PR

## Branch naming

Work on feature branches, never directly on `main` or `master`:

```bash
git checkout -b feature/junit5-support
git checkout -b fix/multipart-content-type
git checkout -b docs/improve-url-builder-reference
```

## Code style

DocTester follows the standard Sun/Oracle Java code style (default IntelliJ/Eclipse formatting):

- **4 spaces per indent** (no tabs)
- **UTF-8** everywhere
- **No trailing whitespace**
- **Apache 2.0 license header** on all new source files:

```java
/**
 * Copyright (C) the DocTester contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
```

- **Javadoc** on all public API methods:

```java
/**
 * Renders a paragraph in the HTML documentation output.
 *
 * @param text plain text content; HTML characters are escaped automatically
 */
public void say(String text) {
```

- **Keep classes small** — single responsibility
- **Reformat only changed lines** — large reformats make code review difficult

## Java 25 idioms

DocTester is a Java 25 project with `--enable-preview`. Prefer modern idioms:

**Use records for DTOs:**
```java
// Good
record HttpResult(int status, String body) {}

// Avoid
class HttpResult {
    int status;
    String body;
    // getters, setters, equals, hashCode, toString...
}
```

**Use switch expressions:**
```java
// Good
String method = switch (request.method()) {
    case "GET"    -> "retrieving";
    case "POST"   -> "creating";
    case "PUT"    -> "updating";
    case "DELETE" -> "deleting";
    default       -> "calling";
};

// Avoid
String method;
if (request.method().equals("GET")) {
    method = "retrieving";
} else if (...
```

**Use text blocks for HTML templates:**
```java
// Good
String panel = """
    <div class="panel panel-primary">
        <div class="panel-heading">%s %s</div>
        <div class="panel-body">%s</div>
    </div>
    """.formatted(method, url, body);

// Avoid
String panel = "<div class=\"panel panel-primary\">" +
    "<div class=\"panel-heading\">" + method + " " + url + "</div>" + ...
```

**Use pattern matching:**
```java
// Good
if (payload instanceof String s && !s.isBlank()) {
    writeString(s);
}

// Avoid
if (payload instanceof String) {
    String s = (String) payload;
    if (!s.isEmpty()) {
        writeString(s);
    }
}
```

**Use `formatted()` instead of `String.format()`:**
```java
// Good
"<p>%s</p>".formatted(text)

// Avoid
String.format("<p>%s</p>", text)
```

## Testing requirements

**All changes must include tests.**

For a bug fix, write the test first:
1. Write a test that reproduces the bug (it should fail)
2. Fix the code
3. Verify the test passes

For a new feature:
1. Add a unit test to `doctester-core/src/test/`
2. If the feature affects end-to-end behavior, also add to the integration test

**Run the full test suite before submitting:**
```bash
mvnd clean verify
```

Both modules must pass.

## Adding a new `say*` method

Example: adding `sayCodeBlock(String code)`:

**1. Add to `RenderMachineCommands`:**
```java
/**
 * Renders a preformatted code block in the documentation output.
 *
 * @param code source code or command text
 */
void sayCodeBlock(String code);
```

**2. Implement in `RenderMachineImpl`:**
```java
@Override
public void sayCodeBlock(String code) {
    content.append("<pre><code>")
           .append(HtmlEscapers.htmlEscaper().escape(code))
           .append("</code></pre>\n");
}
```

**3. Delegate in `DocTester`:**
```java
/**
 * Renders a preformatted code block in the documentation output.
 *
 * @param code source code or command text
 */
public void sayCodeBlock(String code) {
    renderMachine.sayCodeBlock(code);
}
```

**4. Test in `DocTesterTest`:**
```java
@Test
public void sayCodeBlock_rendersPreformattedCode() {
    docTester.sayCodeBlock("curl -X GET /api/users");
    verify(renderMachine).sayCodeBlock("curl -X GET /api/users");
}
```

**5. Use in `ApiControllerDocTest` (integration test):**
```java
sayCodeBlock("curl -X GET http://localhost:8080/api/articles.json");
```

## Commit messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add sayCodeBlock() for preformatted code in documentation

fix: correctly escape HTML in sayRaw() parameter names

docs: add how-to guide for file uploads

refactor: replace if/else chain with switch expression in TestBrowserImpl

test: add integration test for XML response deserialization
```

Keep the subject line under 72 characters. Add a body if the change needs explanation.

## Pull request checklist

Before opening a PR:

- [ ] Tests added and passing (`mvnd clean verify`)
- [ ] Javadoc added to new public methods
- [ ] License header on new files
- [ ] `changelog.md` updated with a brief description
- [ ] No reformatting of unchanged code
- [ ] PR description explains what changed and why

## Changelog format

Add an entry at the top of `changelog.md`:

```markdown
## 1.1.13 (in progress)

- feat: add `sayCodeBlock()` for preformatted code output (#47)
- fix: correctly handle XML responses with BOM (#48)
```

Use issue numbers when there's a corresponding GitHub issue.
