# DTR 2.0.0 — Breaking Changes Summary

**Release Date:** 2026-03-10
**Previous Version:** 1.1.12
**Upgrade Path:** [Complete Migration Guide](MIGRATION-1.x-TO-2.0.0.md)

---

## Overview

DTR 2.0.0 contains **three major breaking changes** that fundamentally alter how documentation is generated and distributed. These changes modernize the framework for contemporary Java ecosystems.

---

## 1. Output Format: Bootstrap HTML → Markdown

### What Changed

**Version 1.x:** Generates Bootstrap 3-styled HTML pages with embedded CSS/JS

```
target/site/doctester/
├── index.html                    # Bootstrap navbar + TOC
├── UserApiDocTest.html           # Styled HTML page
├── bootstrap/
│   ├── css/bootstrap.min.css
│   └── js/bootstrap.min.js
├── jquery/
│   └── jquery-1.9.0.min.js
└── custom_doctester_stylesheet.css
```

**Version 2.0.0:** Generates pure Markdown (portable, no assets)

```
docs/test/
├── README.md                     # Index (Markdown)
├── UserApiDocTest.md             # Test docs (Markdown)
└── (OpenAPI specs if generated)
```

### Why?

| Factor | HTML | Markdown |
|--------|------|----------|
| **Version Control** | Binary-like diffs | Clean text diffs |
| **Portability** | Requires CSS/JS assets | Self-contained |
| **GitHub Support** | Doesn't render in repo browser | Auto-renders in GitHub |
| **Static Generators** | Manual conversion needed | Native support |
| **Dependencies** | Bootstrap 3, jQuery 1.9 | None |
| **Customization** | CSS-based | Theme-based (MkDocs, Docusaurus) |

### Impact Assessment

**Who is affected:**
- ✓ Everyone (all projects generate documentation)

**What breaks:**
- ❌ CI/CD scripts looking for `target/site/doctester/`
- ❌ Documentation server configurations expecting `.html` files
- ❌ Bookmarks to generated HTML pages
- ❌ Custom CSS in `custom_doctester_stylesheet.css`

**Mitigation:**
1. Update all references from `target/site/doctester/` to `docs/test/`
2. Update CI/CD to deploy Markdown instead of HTML
3. Use a static site generator (MkDocs, Docusaurus, Jekyll) to render HTML if needed
4. If you had custom CSS, configure your site generator's theme instead

### Code Changes Required

**CI/CD Pipeline (GitHub Actions):**

```yaml
# Version 1.x
- name: Deploy Docs
  run: |
    if [ -d "target/site/doctester" ]; then
      cp -r target/site/doctester ./gh-pages
    fi

# Version 2.0.0
- name: Deploy Docs
  run: |
    if [ -d "docs/test" ]; then
      cp -r docs/test ./docs
    fi
```

**Documentation Build (pom.xml):**

```xml
<!-- Version 1.x -->
<configuration>
  <docDir>${project.basedir}/target/site/doctester</docDir>
</configuration>

<!-- Version 2.0.0 -->
<configuration>
  <docDir>${project.basedir}/../docs/test</docDir>
</configuration>
```

---

## 2. Java Version: 1.8+ → 25 (LTS Only)

### What Changed

**Version 1.x:** Supports Java 8, 11, 17, 21

```bash
$ java -version
openjdk version "11.0.20"  ✓ Works
openjdk version "17.0.1"   ✓ Works
openjdk version "21.0.1"   ✓ Works
```

**Version 2.0.0:** Requires **exactly Java 25 (or later 25 LTS releases)**

```bash
$ java -version
openjdk version "25.0.0"   ✓ Works
openjdk version "21.0.1"   ✗ FAILS — too old
openjdk version "24.0.1"   ✗ FAILS — not LTS
```

### Why?

- **Stable Preview Features:** Java 25 stabilizes pattern matching, records, and sealed classes
- **Virtual Threads:** Enables efficient concurrent testing
- **Modern Idioms:** Leverages Java platform improvements
- **LTS Guarantee:** Java 25 will receive 8+ years of support

### Impact Assessment

**Who is affected:**
- ✓ Everyone (all builds require Java 25)

**What breaks:**
- ❌ CI runners using Java 21 or earlier
- ❌ Developer machines without Java 25 installed
- ❌ IDEs not configured for Java 25
- ❌ Maven enforcer rules checking for Java < 25

**Mitigation:**
1. Install Java 25 (or wait for Java 33 LTS in 2026 if Java 25 is no longer current)
2. Set `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`
3. Update CI/CD to use Java 25 runners
4. Update IDE project settings

### Code Changes Required

**pom.xml (Compiler):**

```xml
<!-- Version 1.x -->
<properties>
  <maven.compiler.source>11</maven.compiler.source>
  <maven.compiler.target>11</maven.compiler.target>
</properties>

<!-- Version 2.0.0 -->
<properties>
  <maven.compiler.release>25</maven.compiler.release>
</properties>

<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <release>25</release>
    <compilerArgs>
      <arg>--enable-preview</arg>
    </compilerArgs>
  </configuration>
</plugin>
```

**pom.xml (Surefire):**

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <argLine>--enable-preview</argLine>
  </configuration>
</plugin>
```

**CI/CD (GitHub Actions):**

```yaml
# Version 1.x
- name: Set up JDK 11
  uses: actions/setup-java@v3
  with:
    java-version: 11

# Version 2.0.0
- name: Set up JDK 25
  uses: actions/setup-java@v3
  with:
    java-version: 25
```

---

## 3. Output Directory: `target/site/doctester/` → `docs/test/`

### What Changed

**Version 1.x:** Docs written to `target/site/doctester/`

```
project/
└── target/site/doctester/
    ├── index.html
    ├── UserApiDocTest.html
    ├── bootstrap/
    └── jquery/
```

**Version 2.0.0:** Docs written to `docs/test/` (repository root)

```
project/
└── docs/test/
    ├── README.md
    ├── UserApiDocTest.md
    └── (OpenAPI specs if generated)
```

### Why?

- `docs/test/` is located at repository root (not buried in `target/`)
- Docs are generated once and committed/versioned with the repository
- Aligns with GitHub documentation structure (`docs/` folder)
- Shorter path, easier to reference
- Markdown files work natively on GitHub without special processing
- Compatible with static site generators (MkDocs, Docusaurus, Jekyll)

### Impact Assessment

**Who is affected:**
- ✓ Anyone with documentation build pipelines
- ✓ Anyone with custom paths referencing the old location

**What breaks:**
- ❌ Scripts assuming `target/site/doctester/` exists
- ❌ CI/CD copying from `target/site/doctester/`
- ❌ Paths in `pom.xml`, `build.gradle`, or Ant scripts
- ❌ Hard-coded references in shell scripts
- ❌ Maven site plugin integrations (if any)

**Mitigation:**
1. Update all path references globally (grep, IDE search & replace)
2. Update CI/CD deploy scripts to use `docs/test/`
3. Update `.gitignore` patterns (docs/ is now committed with test runs)

### Code Changes Required

**Global Search & Replace:**

```bash
# Find all references
grep -r "target/site/doctester" .

# Replace (in most files)
find . -type f \( -name "*.sh" -o -name "*.yml" -o -name "*.yaml" -o -name "pom.xml" \) \
  -exec sed -i 's|target/site/doctester|docs/test|g' {} \;
```

**Maven POM files:**

```xml
<!-- If you have custom paths -->
<!-- Version 1.x -->
<property>
  <name>doctester.output.dir</name>
  <value>${project.basedir}/target/site/doctester</value>
</property>

<!-- Version 2.0.0 -->
<property>
  <name>doctester.output.dir</name>
  <value>${project.basedir}/../docs/test</value>
</property>
```

**CI/CD Scripts:**

```bash
#!/bin/bash

# Version 1.x
DOCS_DIR="target/site/doctester"
INDEX_FILE="target/site/doctester/index.html"

# Version 2.0.0
DOCS_DIR="docs/test"
INDEX_FILE="docs/test/README.md"

# Copy documentation
if [ -d "$DOCS_DIR" ]; then
  cp -r "$DOCS_DIR" ./published-docs
fi
```

**.gitignore (if needed):**

```bash
# Version 1.x
/target/site/doctester/

# Version 2.0.0 — Note: docs/test is typically committed to the repository
# Only exclude if you want to prevent test-generated docs from being versioned
# /docs/test/
```

---

## Non-Breaking Changes (Mostly Backward Compatible)

### Request/Response API

The core API is **unchanged**:

```java
// All of these still work exactly the same
Response response = sayAndMakeRequest(Request.GET().url(...));
response.payloadAs(MyDto.class);
sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));
```

Methods like `payloadAsJson()`, `payloadXmlAs()` are deprecated but still functional. Use the newer `payloadAs()` for cleaner code.

### Annotations (Optional Enhancement)

New annotations are **completely optional**:

```java
// Version 1.x style still works
@Test
public void testFoo() {
    sayNextSection("My Section");
    say("Some description");
    // ...
}

// Version 2.0.0 style (recommended but optional)
@Test
@DocSection("My Section")
@DocDescription("Some description")
public void testFoo() {
    // ...
}
```

Both produce identical documentation. Mix and match in the same test class if desired.

---

## Migration Checklist

Quick reference for what needs to be updated:

### 1. Development Environment
- [ ] Java 25 installed
- [ ] `JAVA_HOME` set to Java 25
- [ ] IDE configured for Java 25
- [ ] IDE recognizes `--enable-preview` flag

### 2. Project Configuration (pom.xml)
- [ ] Update compiler plugin with `<release>25</release>`
- [ ] Add `--enable-preview` to compiler args
- [ ] Add `--enable-preview` to surefire argLine
- [ ] Update DTR dependency to 2.0.0
- [ ] Update Maven Enforcer rules if present

### 3. Paths and Scripts
- [ ] Update all `target/site/doctester` → `target/docs` references
- [ ] Update CI/CD deploy paths
- [ ] Update `.gitignore` if it excludes old path
- [ ] Update documentation build scripts
- [ ] Update Ant/Gradle build files if applicable

### 4. CI/CD Configuration
- [ ] GitHub Actions: Use Java 25 runner
- [ ] GitLab CI: Update `image` to Java 25
- [ ] Jenkins: Update JDK tool to Java 25
- [ ] Update any documentation publish steps
- [ ] Update any email/Slack notifications with new path

### 5. Documentation Pipeline
- [ ] Set up static site generator if rendering HTML
- [ ] Update documentation server config
- [ ] Test Markdown rendering locally
- [ ] Configure GitHub Pages / hosting for new path
- [ ] Update links in README and other docs

### 6. Testing
- [ ] Compile locally with `mvnd clean compile`
- [ ] Run tests with `mvnd clean test`
- [ ] Verify `target/docs/` contains `.md` files
- [ ] Verify CI/CD pipeline passes
- [ ] Verify documentation renders correctly

---

## FAQ

### Q: Do I have to upgrade to 2.0.0?

**A:** No, version 1.1.12 continues to work with Java 8–21 and generates HTML. However:
- 1.x will receive no further updates
- Security issues may not be patched
- Java 21 goes out of support in September 2026
- Version 2.0.0 includes significant improvements (Markdown, WebSocket, OpenAPI)

**Recommendation:** Upgrade within 6 months to stay on a supported version.

### Q: Can I generate HTML instead of Markdown?

**A:** Not from DTR directly. However, you can easily render Markdown to HTML using:
- **MkDocs** — `mkdocs build` → static site in `site/`
- **Docusaurus** — `npm run build` → static site in `build/`
- **Pandoc** — `pandoc *.md -o index.html`
- **Jekyll** — GitHub Pages automatically renders Markdown

This is actually **better** because you control the look and feel, and Markdown is always the source of truth.

### Q: What about my custom CSS?

**A:** Custom DTR CSS (`custom_doctester_stylesheet.css`) is no longer used because:
1. Markdown is not styled by DocTester
2. Styling happens in your static site generator (theme)

**Migrate by:**
- Exporting styles from your old CSS
- Applying them to your site generator theme (MkDocs, Docusaurus, Jekyll)
- Using `--theme` flags or CSS override files

### Q: Will DTR support Java 21 or 17?

**A:** No. Version 2.0.0 requires Java 25 LTS.

When Java 33 is released in 2026, DTR will support Java 33 LTS. Version 2.0.0 will still require 25 until end-of-life (2032).

### Q: How do I migrate a multi-module project?

**A:** Each module gets its own `target/docs/`:

```
my-api/
├── api-core/
│   └── target/docs/              # Generated here
├── api-auth/
│   └── target/docs/              # Generated here
└── api-web/
    └── target/docs/              # Generated here
```

Use a static site generator to aggregate:

```yaml
# mkdocs.yml
nav:
  - Home: index.md
  - Core API:
    - Overview: '!include ./api-core/target/docs'
  - Auth Service:
    - Overview: '!include ./api-auth/target/docs'
```

### Q: What if I'm using a custom RenderMachine?

**A:** If you extended `RenderMachine` in 1.x, you'll need to update:

**Version 1.x interface:**
```java
interface RenderMachine {
    void sayNextSection(String title);
    void say(String text);
    void sayRaw(String html);
    // ... HTML-focused methods
}
```

**Version 2.0.0 interface:**
```java
interface RenderMachine {
    void sayNextSection(String title);
    void say(String text);
    void sayRaw(String markdown);  // Now Markdown, not HTML
    // ... Markdown-focused methods
}
```

Contact the team if you have a custom `RenderMachine` implementation.

---

## Getting Help

- **Migration Guide:** [MIGRATION-1.x-TO-2.0.0.md](MIGRATION-1.x-TO-2.0.0.md)
- **Updated README:** [README-2.0.0.md](README-2.0.0.md)
- **Full Documentation:** [docs/](docs/)
- **GitHub Issues:** [Report problems](https://github.com/r10r-org/doctester/issues)
- **GitHub Discussions:** [Ask questions](https://github.com/r10r-org/doctester/discussions)

---

## Timeline

| Date | Event |
|------|-------|
| 2026-03-10 | DTR 2.0.0 released |
| 2026-03-31 | Last critical bug fixes for 1.x |
| 2026-06-30 | End of life for 1.x (no further support) |
| 2026-09-14 | Java 21 end-of-life (Java 25 becomes baseline) |
| 2032-09-01 | Java 25 end-of-life (new LTS required) |

**Action Items:**
- Plan upgrade in Q2 2026
- Complete by Q3 2026
- Leverage Markdown improvements in your workflow

---

## Conclusion

These breaking changes represent a significant modernization of DTR:
- **Markdown** is portable and version-control friendly
- **Java 25** unlocks modern language features
- **New output path** aligns with Maven standards

The upgrade is a one-time effort that positions you for years of maintainable, living API documentation.

**Questions?** See [MIGRATION-1.x-TO-2.0.0.md](MIGRATION-1.x-TO-2.0.0.md) for detailed steps.

Happy upgrading! 🚀
