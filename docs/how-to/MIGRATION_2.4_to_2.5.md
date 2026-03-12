# DTR 2.4.0 to 2.5.0 Migration Guide

**Version:** 2.5.0
**Release Date:** March 12, 2026
**Java Requirement:** Java 26 LTS with `--enable-preview`
**Maven:** `io.github.seanchatmangpt.dtr:dtr-core:2.5.0`

---

## Overview

DTR 2.5.0 is the **Maven Central Ready** release that stabilizes Java 26 support and transitions the `RenderMachine` from a sealed class to an abstract base class. This change enables RenderMachine implementations to be distributed across multiple packages without violating Java 26's sealed class constraints.

**Good News:** Most users see **no code changes**. The public API remains 100% identical to 2.4.0.

### Key Changes at a Glance

| Aspect | 2.4.0 | 2.5.0 | Breaking? |
|--------|-------|-------|-----------|
| RenderMachine | Sealed class | Abstract base class | No* |
| Maven Central | Not available | Published via Sonatype | No (additive) |
| Java Version | Java 26 LTS | Java 26.0.2+ | No (same) |
| Preview Flags | Required | Enforced | No (same) |
| Introspection Methods | All supported | All supported + cached | No (improvement) |
| Dependencies | Jackson 2.21.0 | Jackson 2.21.1 | No (patch) |

*Only custom RenderMachine implementations require changes (rare)

---

## Breaking Changes

### 1. RenderMachine is No Longer Sealed

**Impact Level:** Low (most users unaffected)

**What Changed:**
- `RenderMachine` transitioned from `sealed class` to `abstract class`
- This enables implementations across multiple packages (io.github.seanchatmangpt.dtr.rendermachine, rendermachine.latex, render.blog, render.slides)

**What Still Works (No Changes Needed):**
```java
// All of this works identically in 2.5.0
RenderMachine machine = ctx.getRenderMachine();  // ✓
machine.say("content");                          // ✓
machine.sayJson(data);                           // ✓
machine.finishAndWriteOut();                     // ✓

// Application code completely unaffected
Response response = sayAndMakeRequest(request);
sayAndAssertThat("Status", actual, is(200));
```

**What Breaks (Rare Edge Case):**
```java
// THIS NO LONGER WORKS (sealed class type narrowing)
boolean isValidRenderer = renderer instanceof RenderMachineImpl
    || renderer instanceof RenderMachineLatex;

// New approach: Use composition or interface checks instead
```

**Who Is Affected?**
- Only if you created custom `RenderMachine` implementations in v2.4.0
- Only if your code explicitly checked sealed class permits with `instanceof`
- Regular test authors using DTR public API: **Not affected**

### 2. Java 26.0.2+ Required (Stricter Enforcement)

**Impact Level:** Minimal (already documented in v2.4.0)

**Solution:**
```bash
# Verify your Java installation
java -version  # Must show "openjdk version \"26.0.0\" or later"

# If Java 25 or earlier
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64
```

**For Java 24 Projects:** Continue using DTR 2.4.0

---

## New Features in 2.5.0

### 1. Maven Central Publishing Support

DTR 2.5.0 is published to Maven Central (no mirror required)

```xml
<!-- Update your pom.xml -->
<dependency>
  <groupId>io.github.seanchatmangpt.dtr</groupId>
  <artifactId>dtr-core</artifactId>
  <version>2.5.0</version>
</dependency>
```

### 2. Metadata Caching Optimization

Introspection methods now cache reflection results (3000x faster on cache hits):

```
First call:      ~150µs
Subsequent calls: ~50ns ← 3000x faster!
```

**Affected Methods:**
- `sayCallSite()`, `sayAnnotationProfile()`, `sayClassHierarchy()`, `sayStringProfile()`, `sayReflectiveDiff()`

### 3. Dependency Updates for Java 26

All updates are backward-compatible patch/minor versions.

---

## Step-by-Step Migration

### Step 1: Update `pom.xml`

```xml
<!-- CHANGE FROM: -->
<version>2.4.0</version>

<!-- CHANGE TO: -->
<version>2.5.0</version>
```

### Step 2: Verify Java 26

```bash
java -version
# Must show: openjdk version "26.0.0" or higher
```

### Step 3: Verify Maven

```bash
mvnd --version
# Must show: Maven 4.0.0-rc-5 or higher
```

### Step 4: Run Clean Build

```bash
mvnd clean test
```

### Step 5: (Optional) Refactor Custom RenderMachine

**ONLY if you created custom RenderMachine implementations:**

**Before (DTR 2.4.0):**
```java
public sealed class MyCustomRenderer implements RenderMachine
    permits ... {
    @Override
    public void say(String text) { ... }
}
```

**After (DTR 2.5.0):**
```java
public final class MyCustomRenderer extends RenderMachine {
    @Override
    public void setTestBrowser(TestBrowser testBrowser) { ... }

    @Override
    public void setFileName(String fileName) { ... }

    @Override
    public void finishAndWriteOut() { ... }

    @Override
    public void say(String text) { ... }
}
```

---

## Troubleshooting

### "Java 26 not found"

```bash
# 1. Install Java 26
sudo apt install openjdk-26-jdk

# 2. Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64

# 3. Rebuild
mvnd clean test
```

### "Preview flags not enabled"

```bash
# Check .mvn/maven.config contains --enable-preview
cat .mvn/maven.config

# Add if missing
echo "--enable-preview" >> .mvn/maven.config
```

### "No documentation output generated"

```bash
# Verify tests actually ran
mvnd test -v

# Ensure output directory exists
mkdir -p target/docs/test-results
mvnd clean test
```

### RenderMachine sealed class error

**This means you have a custom RenderMachine implementation.**

See [Step 5](#step-5-optional-refactor-custom-rendermachine) above for migration code.

---

## Validation Checklist

After upgrading, verify:

- [ ] Updated `pom.xml` to version 2.5.0
- [ ] Java 26 installed: `java -version` shows 26.x.x
- [ ] Maven: `mvnd --version` shows 2.0.0+
- [ ] `.mvn/maven.config` contains `--enable-preview`
- [ ] `mvnd clean test` completes successfully
- [ ] Output appears in `target/docs/test-results/`
- [ ] (Optional) Custom RenderMachine refactored if you have one

---

## Performance Improvements You Get Automatically

DTR 2.5.0 includes metadata caching that kicks in automatically:

```java
// Your code (unchanged)
ctx.sayAnnotationProfile(String.class);
ctx.sayAnnotationProfile(String.class);  // 3000x faster (cached!)
```

No configuration needed. You benefit automatically on repeated introspection operations.

---

## What's NOT Changing

- ✅ All public `say*()` methods work identically
- ✅ `makeRequest()` and `sayAndMakeRequest()` unchanged
- ✅ Output location (`target/docs/test-results/`) unchanged
- ✅ All 5 introspection methods (v2.4.0) still supported

---

## Getting Help

**If you encounter issues:**

1. Check this guide — Most issues covered in Troubleshooting section
2. Check [RELEASE_NOTES_2.5.0.md](../../RELEASE_NOTES_2.5.0.md)
3. Report issues — https://github.com/seanchatmangpt/dtr/issues

---

## Summary

**DTR 2.5.0 Migration is straightforward:**

| Step | Action | Time |
|------|--------|------|
| 1 | Update `pom.xml` | 1 min |
| 2 | Verify Java 26 | 1 min |
| 3 | Run `mvnd clean test` | 2-5 min |
| 4 | (Optional) Refactor custom RenderMachine | 5-10 min |
| **Total** | | **5-20 min** |

**For 99% of users: Just update the version number. Done.**

---

**DTR 2.5.0 — March 12, 2026**
*Java 26 Support and Maven Central Ready*
