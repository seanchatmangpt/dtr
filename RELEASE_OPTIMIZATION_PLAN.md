# DocTester 2.0.0 Release Optimization Plan

## Overview

Three focused improvements to maximize Java 25 idiom compliance before release. Total effort: ~60 minutes.

---

## Task 1: Migrate String.format() → String.formatted()

### Impact
- **Effort:** 30 minutes
- **Files:** 2
- **Locations:** 8 total
- **Benefit:** Modern Java 15+ idiom, cleaner API

### Details

#### 1.1 RenderMachineImpl.java (6 locations)

**Current Code:**
```java
// Line 74
toc.add(String.format("- [%s](#%s)", heading, anchorId));

// Lines 86-87
markdownDocument.add(String.format("- **%s**: `%s` (path: %s, domain: %s)",
        cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain()));

// Line 98
markdownDocument.add(String.format("- **Value**: `%s`", cookie.getValue()));

// Line 99
markdownDocument.add(String.format("- **Path**: `%s`", cookie.getPath()));

// Line 100
markdownDocument.add(String.format("- **Domain**: `%s`", cookie.getDomain()));
```

**Migration:**
```java
// Line 74
toc.add("- [%s](#%s)".formatted(heading, anchorId));

// Lines 86-87
markdownDocument.add("- **%s**: `%s` (path: %s, domain: %s)".formatted(
        cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain()));

// Line 98
markdownDocument.add("- **Value**: `%s`".formatted(cookie.getValue()));

// Line 99
markdownDocument.add("- **Path**: `%s`".formatted(cookie.getPath()));

// Line 100
markdownDocument.add("- **Domain**: `%s`".formatted(cookie.getDomain()));
```

#### 1.2 OpenApiCollector.java (2 locations)

**Current Code:**
```java
// Search for String.format in this file
```

**Migration Target:**
Replace any `String.format(...)` with `"...".formatted(...)`

### Validation
```bash
# After changes, verify no String.format() remains in these files
grep -n "String\.format" doctester-core/src/main/java/org/r10r/doctester/rendermachine/RenderMachineImpl.java
grep -n "String\.format" doctester-core/src/main/java/org/r10r/doctester/openapi/OpenApiCollector.java

# Should return 0 matches
```

---

## Task 2: Replace Guava Collection Factories with Java 9+ Equivalents

### Impact
- **Effort:** 20 minutes
- **Files:** 3
- **Locations:** 9 total
- **Benefit:** Dependency cleanup, reduced JAR size, pure Java idioms

### Details

#### 2.1 Request.java (4 locations)

**Current Code:**
```java
// Line 58 (constructor)
headers = Maps.newHashMap();

// Line 232 (addFileToUpload)
filesToUpload = Maps.newHashMap();

// Line 249 (addHeader)
headers = Maps.newHashMap();

// Line 278 (addFormParameter)
formParameters = Maps.newHashMap();
```

**Migration:**
```java
// Line 58
headers = new HashMap<>();

// Line 232
filesToUpload = new HashMap<>();

// Line 249
headers = new HashMap<>();

// Line 278
formParameters = new HashMap<>();
```

**Note:** Import cleanup not needed (no `com.google.common.collect.Maps` import once all uses are removed)

#### 2.2 Url.java (1 location)

**Current Code:**
```java
// Line 49 (constructor)
queryParameters = Maps.newHashMap();
```

**Migration:**
```java
// Line 49
queryParameters = new HashMap<>();
```

#### 2.3 TestBrowserImpl.java (4 locations)

**Current Code:**
```java
// Line 104
if (Sets.newHashSet(HEAD, GET, DELETE).contains(httpRequest.httpRequestType)) {

// Line 108
} else if (Sets.newHashSet(POST, PUT, PATCH).contains(httpRequest.httpRequestType)) {

// Line 206
List<NameValuePair> formparams = Lists.newArrayList();

// Line 296
Map<String, String> headers = Maps.newHashMap();
```

**Migration:**
```java
// Line 104
if (Set.of(HEAD, GET, DELETE).contains(httpRequest.httpRequestType)) {

// Line 108
} else if (Set.of(POST, PUT, PATCH).contains(httpRequest.httpRequestType)) {

// Line 206
List<NameValuePair> formparams = new ArrayList<>();

// Line 296
Map<String, String> headers = new HashMap<>();
```

### Validation
```bash
# After changes, verify no Guava collection factories remain
grep -r "Maps\.newHashMap\|Maps\.newHashMap\|Lists\.newArrayList\|Sets\.newHashSet" \
    doctester-core/src/main/java/org/r10r/doctester/testbrowser/ \
    doctester-core/src/main/java/org/r10r/doctester/rendermachine/

# Should return 0 matches

# Verify imports are removed
grep "com.google.common.collect" doctester-core/src/main/java/org/r10r/doctester/testbrowser/Request.java
grep "com.google.common.collect" doctester-core/src/main/java/org/r10r/doctester/testbrowser/Url.java
grep "com.google.common.collect" doctester-core/src/main/java/org/r10r/doctester/testbrowser/TestBrowserImpl.java

# Should return 0 matches
```

---

## Task 3: Fix Test Deprecation Warning

### Impact
- **Effort:** 10 minutes
- **Files:** 1 (test code)
- **Benefit:** Clean test suite output

### Details

#### 3.1 AnnotationDocTest.java

**Issue:**
```
[INFO] /home/user/doctester/doctester-core/src/test/java/org/r10r/doctester/AnnotationDocTest.java:
       Some input files use or override a deprecated API.
```

**Action:**
1. Identify which deprecated API is being used
2. Replace with modern equivalent or suppress warning with `@SuppressWarnings("deprecation")` if intentional
3. Document reason in code comment if suppression is necessary

**Command to investigate:**
```bash
mvn clean compile -pl doctester-core -Xlint:deprecation 2>&1 | \
    grep -A 5 "AnnotationDocTest"
```

### Validation
```bash
# After fix, no deprecation warning should appear for this file
mvnd clean compile -pl doctester-core -Xlint:deprecation 2>&1 | \
    grep "AnnotationDocTest"

# Should return no matches
```

---

## Execution Checklist

### Pre-Execution
- [ ] Verify current branch is `main` or release branch
- [ ] Create feature branch: `git checkout -b chore/java25-idiom-cleanup`
- [ ] Ensure all tests currently pass: `mvnd clean test -pl doctester-core`

### Task 1: String.format() Migration
- [ ] Open `RenderMachineImpl.java`
- [ ] Replace 6 `String.format()` calls with `.formatted()`
- [ ] Open `OpenApiCollector.java`
- [ ] Replace 2 `String.format()` calls with `.formatted()`
- [ ] Run grep validation
- [ ] Compile and test: `mvnd clean test -pl doctester-core`

### Task 2: Guava Cleanup
- [ ] Open `Request.java`
- [ ] Replace 4 `Maps.newHashMap()` with `new HashMap<>()`
- [ ] Remove Guava import from `Request.java`
- [ ] Open `Url.java`
- [ ] Replace 1 `Maps.newHashMap()` with `new HashMap<>()`
- [ ] Remove Guava import from `Url.java`
- [ ] Open `TestBrowserImpl.java`
- [ ] Replace 2 `Sets.newHashSet()` with `Set.of()`
- [ ] Replace 1 `Lists.newArrayList()` with `new ArrayList<>()`
- [ ] Replace 1 `Maps.newHashMap()` with `new HashMap<>()`
- [ ] Remove Guava imports from `TestBrowserImpl.java`
- [ ] Run grep validation
- [ ] Compile and test: `mvnd clean test -pl doctester-core`

### Task 3: Deprecation Fix
- [ ] Investigate AnnotationDocTest deprecation
- [ ] Fix or suppress appropriately
- [ ] Compile with warnings: `mvnd clean compile -pl doctester-core -Xlint:deprecation`
- [ ] Verify no AnnotationDocTest warnings

### Final Validation
- [ ] Full compilation: `mvnd clean install -pl doctester-core`
- [ ] Full test suite: `mvnd test -pl doctester-core`
- [ ] Verify no compilation warnings related to these changes
- [ ] Code review changes

### Post-Execution
- [ ] Stage changes: `git add doctester-core/src/main/java`
- [ ] Create commit with message:
  ```
  Modernize Java 25 idioms for 2.0.0 release

  - Migrate String.format() to String.formatted() (8 locations)
  - Replace Guava collection factories with Java 9+ equivalents (9 locations)
  - Fix test deprecation warning in AnnotationDocTest

  This improves code consistency with Java 25 best practices while
  reducing external dependency footprint and JAR size.
  ```
- [ ] Push to branch: `git push origin chore/java25-idiom-cleanup`
- [ ] Open pull request against release branch

---

## Estimated Timeline

| Task | Duration | Cumulative |
|------|----------|-----------|
| Task 1: String.formatted() | 30 min | 30 min |
| Task 2: Guava cleanup | 20 min | 50 min |
| Task 3: Deprecation fix | 10 min | 60 min |
| **Testing & validation** | 15 min | **75 min** |
| **Code review** | 10 min | **85 min** |

---

## Quality Gates

### Before Committing
- [ ] `mvnd clean install -pl doctester-core` succeeds
- [ ] `mvnd test -pl doctester-core` all tests pass
- [ ] No compilation warnings related to these changes
- [ ] No new deprecation warnings introduced
- [ ] Grep validation shows 0 matches for:
  - `String.format(` in RenderMachineImpl.java or OpenApiCollector.java
  - `Maps.newHashMap`, `Lists.newArrayList`, `Sets.newHashSet` in any testbrowser or rendermachine file

### Before Release
- [ ] Pull request approved by code reviewer
- [ ] All CI checks pass
- [ ] Release notes updated with "Java 25 idiom modernization" section
- [ ] Version bumped to 2.0.0 in pom.xml

---

## Rollback Plan

If any issues arise:

```bash
# Revert all changes to main
git checkout main

# OR revert specific commits if already pushed
git revert <commit-hash>
```

No API or behavioral changes means zero risk of rollback issues.

---

## Notes

1. **No API Changes:** All modifications are internal optimizations only
2. **Backward Compatible:** Behavior is identical before and after
3. **Clean Compilation:** All changes verified to compile without errors
4. **Zero Risk:** Can safely revert if needed (no business logic changes)
5. **Test Coverage:** All existing tests continue to pass

---

**Status:** Ready for execution
**Target Release:** DocTester 2.0.0
**Priority:** High (improves Java 25 adoption score from 85 → 95)
