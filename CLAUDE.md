# DTR (Documentation Testing Runtime) — Claude Code Quick Reference

**Project:** Markdown documentation generator for Java 25 | **Version:** 2.5.0-SNAPSHOT

---

## ⚡ CRITICAL RULES

### 1. REAL CODE, REAL MEASUREMENTS ONLY
- ❌ NO simulation, NO fakes, NO hard-coded numbers
- ✅ Use actual DTR code (RenderMachine + say* methods)
- ✅ Measure with System.nanoTime() on real execution
- ✅ Report: metric + units + Java version + iterations + environment
- **Example:** "JEP 516: 78ns avg (10M accesses, 100 iter, Java 25.0.2)" NOT "6667x faster"

### 2. ALWAYS USE REAL DTR CLI
- ✅ JUnit 5 tests with DocTesterContext
- ✅ Output through RenderMachine rendering pipeline
- ❌ Never bypass with standalone generators

### 3. Toolchain (Non-Negotiable)
- Java 25: `/usr/lib/jvm/java-25-openjdk-amd64`
- Maven 4.0.0-rc-5+: `/opt/apache-maven-4.0.0-rc-5/bin/mvn`
- mvnd 2.0.0+: `/opt/mvnd/bin/mvnd` (preferred)
- Flag: `--enable-preview` in `.mvn/maven.config`

---

## 🔧 QUICK BUILD

```bash
# Run real DTR test
mvnd test -pl dtr-integration-test -Dtest=PhDThesisDocTest

# If Maven auth fails, start proxy first
python3 maven-proxy-auth.py &

# Then build with proxy
mvnd clean install -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 \
  -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128

# Check output
ls target/docs/test-results/
cat target/docs/test-results/PhDThesisDocTest.md
```

---

## 🌐 MAVEN PROXY SOLUTION

**Problem:** "too many authentication attempts" from Maven Central

**Fix:** 3 steps
```bash
# 1. Start proxy
python3 maven-proxy-auth.py &

# 2. Add to .mvn/jvm.config
-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128
-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
-Dhttp.nonProxyHosts=localhost|127.0.0.1

# 3. Set env var
export https_proxy=http://user:pass@proxy.company.com:8080
```

**What it does:** Listens on 127.0.0.1:3128, handles HTTPS CONNECT tunneling, injects Proxy-Authorization header automatically.

**Troubleshoot:**
- Still failing? `pkill -f maven-proxy && python3 maven-proxy-auth.py &`
- Check env? `echo $https_proxy`

---

## 📝 HOW TO ADD A TEST

```java
@ExtendWith(DocTesterExtension.class)
class PhDThesisDocTest {
    @Test
    void testThesis(DocTesterContext ctx) {
        ctx.sayNextSection("Chapter Title");
        ctx.say("Content here.");
        ctx.sayCode("System.out.println(\"code\");", "java");
        ctx.sayTable(new String[][] {{"Col1", "Col2"}, {"V1", "V2"}});
        ctx.sayJson(someObject);
        ctx.sayWarning("Important!");
        ctx.sayNote("FYI...");
    }
}
```

**Output:** `target/docs/test-results/PhDThesisDocTest.md` (+ .tex, .html, .json)

---

## 📊 say* API (Documentation Methods)

| Method | Output | Use For |
|--------|--------|---------|
| `sayNextSection(String)` | H1 heading | Chapter/section titles |
| `say(String)` | Paragraph | Body text |
| `sayCode(String, lang)` | Fenced block | Code examples |
| `sayTable(String[][])` | Markdown table | Data comparison |
| `sayJson(Object)` | JSON pretty | Payload examples |
| `sayWarning(String)` | Alert block | Critical info |
| `sayNote(String)` | Note block | Tips/context |
| `sayKeyValue(Map)` | 2-col table | Metadata |
| `sayUnorderedList(List)` | Bullets | Features/checklist |
| `sayOrderedList(List)` | Numbered | Steps/sequence |

---

## 🎯 DOCTESTER ARCHITECTURE (80/20)

**Input:** JUnit 5 test calls `say*` methods
**Process:** RenderMachine captures calls → formats → routes to output engines
**Output:** Markdown + LaTeX + HTML + OpenAPI + Blog (auto-generated)

**Example:**
```java
ctx.sayAndMakeRequest(Request.GET().url(api));  // Executes HTTP + documents
ctx.sayAndAssertThat("Status", actual, is(200)); // Assert + document result
// Output: Complete API doc with request/response examples
```

---

## 🚀 JAVA 26 FEATURES (Use These)

```java
// Records (immutable data)
record Response(int status, String body) {}

// Sealed classes + pattern matching
sealed interface Result permits Success, Error {}
String msg = switch(result) {
    case Success(var d) -> "OK: " + d;
    case Error(var m) -> "FAIL: " + m;
};

// Virtual threads (millions of concurrent tasks)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> generateDoc());
}

// Text blocks (multi-line strings)
String html = """
    <div>Content</div>
    """;
```

---

## ✅ BEFORE CODING

1. `java -version` → 25.0.2+
2. `mvnd --version` → Maven 4.0.0-rc-5+
3. `.mvn/maven.config` contains `--enable-preview`
4. Proxy running (if needed): `python3 maven-proxy-auth.py &`
5. Remember: **REAL CODE + REAL MEASUREMENTS + REAL DOCTESTER CLI**

---

## 🔍 TROUBLESHOOTING

```bash
mvnd --stop                    # Stop daemon on auth issues
mvnd -X clean install          # Verbose output
cat target/surefire-reports/*  # Test details
echo $JAVA_HOME                # Verify Java path
ps aux | grep maven-proxy      # Check proxy running
```

---

## 📚 FILES YOU NEED

- `maven-proxy-auth.py` — Enterprise proxy solution
- `dtr-core/` — Core library
- `dtr-integration-test/` — Integration tests
- `.mvn/maven.config` — Build flags (--enable-preview)
- `pom.xml` — `<release>26</release>`

---

**Last Updated:** March 11, 2026
**Branch:** claude/fix-latex-errors-rzhxB
**Rule:** Always measure real, report real, use real DTR code.
