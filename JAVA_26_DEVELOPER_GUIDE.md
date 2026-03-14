<!-- Copyright (C) 2013 the original author or authors.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License. -->

# Java 26 Developer Guide for DTR

**Target Release:** Java 26 (GA: March 17, 2026)
**Status:** Release Candidate (RC)
**Java Version Currently Required:** Java 26 with `--enable-preview`
**Migration Timeline:** Java 26 GA (March 17, 2026)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [The 10 Java 26 JEPs](#the-10-java-26-jeps)
3. [JEP 530: Primitive Types in Patterns (Fourth Preview)](#jep-530-primitive-types-in-patterns-fourth-preview)
4. [JEP 526: Lazy Constants (Second Preview)](#jep-526-lazy-constants-second-preview)
5. [JEP 525: Structured Concurrency (Sixth Preview)](#jep-525-structured-concurrency-sixth-preview)
6. [JEP 517: HTTP/3 Support](#jep-517-http3-support)
7. [JEP 516: AOT Object Caching with Any GC](#jep-516-aot-object-caching-with-any-gc)
8. [JEP 522: G1 GC Synchronization Improvements](#jep-522-g1-gc-synchronization-improvements)
9. [JEP 524: PEM Encodings (Second Preview)](#jep-524-pem-encodings-second-preview)
10. [JEP 529: Vector API (Eleventh Incubator)](#jep-529-vector-api-eleventh-incubator)
11. [JEP 500: Final Mean Final](#jep-500-final-mean-final)
12. [JEP 504: Remove Applet API](#jep-504-remove-applet-api)
13. [Build & Test Setup](#build--test-setup)
14. [Migration Path for Released Features](#migration-path-for-released-features)
15. [Performance Tuning & Measurement](#performance-tuning--measurement)
16. [FAQ](#faq)
17. [Resources](#resources)

---

## Executive Summary

Java 26 finalizes **10 JEPs** addressing three core concerns:

1. **Language refinement** — Primitive type patterns (`JEP 530`) improve type-safe pattern matching
2. **Concurrency maturity** — Structured Concurrency (`JEP 525`) reaches 6th preview; HTTP/3 (`JEP 517`) modernizes network I/O
3. **Performance** — G1 GC synchronization reduction (`JEP 522`), AOT caching (`JEP 516`), and lazy constants (`JEP 526`)

**For DTR specifically:**

- **JEP 530** (Primitive patterns) refines existing pattern matching used in render-machine dispatch
- **JEP 525** (Structured Concurrency) enhances virtual thread orchestration in `MultiRenderMachine`
- **JEP 517** (HTTP/3) enables adoption of QUIC for test servers (faster handshakes, multiplexing)
- **JEP 522** (G1 GC) improves throughput of concurrent multi-format rendering
- **JEP 500** (Final means final) strengthens reflection barrier for security

### Key Adoptions for DTR 2.6.0+

| JEP | Impact | Adoption | Status |
|-----|--------|----------|--------|
| **530** | Type-safe primitive patterns in switch | Renderer dispatch refinement | 6th preview → stable (Java 27+) |
| **525** | Structured concurrency patterns | Virtual thread lifecycle management | 6th preview → stable (Java 27+) |
| **517** | HTTP/3 (QUIC) protocol support | Test browser optional feature | 1st preview |
| **516** | AOT object caching | Startup optimization (future) | 1st preview |
| **522** | G1 GC throughput (+5–15%) | Concurrent rendering benefits automatically | Stable |

---

## The 10 Java 26 JEPs

Java 26 includes 10 targeted JEPs released on March 17, 2026:

| JEP | Title | Preview/Stable | Category |
|-----|-------|-----------------|----------|
| **530** | Primitive Types in Patterns, instanceof, switch | 4th Preview | Language |
| **526** | Lazy Constants | 2nd Preview | API/VM |
| **525** | Structured Concurrency | 6th Preview | Concurrency |
| **517** | HTTP/3 for HTTP Client API | 1st Preview | API |
| **516** | Ahead-of-Time Object Caching (any GC) | 1st Preview | VM Performance |
| **524** | PEM Encodings of Cryptographic Objects | 2nd Preview | Security |
| **529** | Vector API | 11th Incubator | API |
| **500** | Prepare to Make Final Mean Final | Stable | Reflection/Security |
| **504** | Remove the Applet API | Removal | Deprecation |
| **522** | G1 GC: Improve Throughput by Reducing Synchronization | Stable | VM Performance |

---

## JEP 530: Primitive Types in Patterns (Fourth Preview)

### What Changed

**Java 26:** Basic primitive type patterns (`int i`) in exhaustive switches.

**Java 26:** Fourth preview tightens dominance rules and unconditional exactness, improving pattern matching correctness and eliminating edge cases where two patterns could match the same value.

### How to Use

**Before (Java 26):**
```java
// Basic primitive pattern matching
switch (value) {
    case int i when i > 0 -> "positive: " + i;
    case int i when i < 0 -> "negative: " + i;
    case int i -> "zero";  // default case required
}
```

**After (Java 26 improvements):**
```java
// Enhanced dominance checking ensures no overlapping patterns
sealed interface NumericResult permits PositiveInt, NegativeInt, ZeroInt {}
record PositiveInt(int value) implements NumericResult {}
record NegativeInt(int value) implements NumericResult {}
record ZeroInt(int value) implements NumericResult {}

// Compiler now verifies: no pattern can match a value already matched by an earlier pattern
switch (parseInteger(input)) {
    case PositiveInt(int n) -> System.out.println("Positive: " + n);
    case NegativeInt(int n) -> System.out.println("Negative: " + n);
    case ZeroInt(int n)     -> System.out.println("Zero: " + n);
    // Exhaustive — compiler verifies all NumericResult subtypes covered
}
```

### DTR Application

**Current Use (Java 26):** `Java26ShowcaseTest.java` dispatches `SayEvent` hierarchy with pattern matching:

```java
// status: USING IN PRODUCTION
String label = switch (event) {
    case SayEvent.TextEvent(var text)            -> "paragraph: " + text;
    case SayEvent.SectionEvent(var heading)      -> "section: " + heading;
    case SayEvent.CodeEvent(var code, var lang)  -> "code[" + lang + "]: " + code;
    case SayEvent.NoteEvent(var msg)             -> "note: " + msg;
    // ... other cases
};
```

**Java 26 Opportunity:** With tighter dominance rules, the compiler will catch dead-code patterns (patterns that can never match because an earlier pattern already covered their values). This is especially valuable in the render pipeline where exhaustiveness is critical.

```java
// Java 26: improved error detection
String render(SayEvent event) {
    return switch (event) {
        case SayEvent.TextEvent(var text) -> renderText(text);
        // Java 26 compiler now rejects unreachable patterns that would never execute
        case SayEvent.WarningEvent(var w) -> renderWarning(w);
        case SayEvent.TextEvent(_)        -> "DEAD CODE — compile error in Java 26";
    };
}
```

### Build Setup

**No changes required:** Primitive patterns continue to preview with `--enable-preview`.

```bash
# Continue using existing setup
mvnd clean compile -pl dtr-core
# Compiler flag --enable-preview is already set in pom.xml
```

### Testing

```bash
# Test Java 26 primitive pattern enhancements
mvnd test -pl dtr-core -Dtest=Java26InnovationsTest

# Full validation
mvnd clean verify --enable-preview
```

### Migration Path

**Java 27 (2026 H2):** Primitive types in patterns become **stable** (no preview flag needed).

```java
// Java 27+: No --enable-preview required
// Simply remove --enable-preview from compiler config in pom.xml
// All pattern matching code continues unchanged
<compilerArgs>
    <!-- Remove after Java 27 release -->
    <arg>--enable-preview</arg>
</compilerArgs>

// Update maven.config to:
// --no-transfer-progress --batch-mode
```

---

## JEP 526: Lazy Constants (Second Preview)

### What Changed

**Java 26:** First preview of `LazyConstantPool` — JVM-optimized object caching for immutable constants.

**Java 26:** Second preview refines the API for declaring objects as "lazy constants" — computed once, cached forever, treated by the JVM as true constants for inlining and escape analysis.

### How to Use

**Before (Java 26 — no lazy constant API):**
```java
// Manual singleton pattern with synchronization overhead
public class ConfigCache {
    private static volatile Config instance;

    public static Config getConfig() {
        if (instance == null) {
            synchronized (ConfigCache.class) {
                if (instance == null) {
                    instance = loadConfig();
                }
            }
        }
        return instance;
    }
}
```

**After (Java 26 with Lazy Constants):**
```java
// Import java.lang.runtime.ObjectMethods (requires --enable-preview)
import java.lang.runtime.*;

// Declare as a lazy constant — JVM handles synchronization and caching
public class ConfigCache {
    // This object is computed once, cached, and inlined as a constant thereafter
    private static final Config INSTANCE = LazyConstantPool.get(
        ConfigCache.class,
        "config",
        ConfigLoader::load
    );

    public static Config getConfig() {
        return INSTANCE;  // JVM inlines this as a constant — no field load
    }
}

// Lazy initialization
class ConfigLoader {
    static Config load() {
        return new Config(/* expensive initialization */);
    }
}
```

### DTR Application

**Potential Use:** Lazy constants would benefit DTR's initialization of shared resources:

```java
// Current: RenderMachineFactory creates machines on-demand
public class RenderMachineFactory {
    public static RenderMachine createMarkdownRenderer() {
        return new RenderMachineImpl();  // Created every time
    }
}

// Java 26+ with Lazy Constants: Cache template instances
public class RenderMachineFactory {
    // MarkdownRenderer template cached as a lazy constant
    static final RenderMachine MARKDOWN_TEMPLATE =
        LazyConstantPool.get(
            RenderMachineFactory.class,
            "markdown",
            () -> new RenderMachineImpl().withTemplate(MarkdownTemplate.DEFAULT)
        );

    public static RenderMachine createMarkdownRenderer() {
        return MARKDOWN_TEMPLATE.copy();  // Clone from cached template
    }
}
```

### Build Setup

Lazy constants require `--enable-preview`:

```bash
# Already enabled in pom.xml — no changes needed
mvnd clean compile -pl dtr-core
```

### Testing

```bash
# Verify lazy constant behavior
mvnd test -pl dtr-core -Dtest=LazyConstantPoolTest

# Performance validation: lazy constant cache hit rate
mvnd clean verify -DskipTests && \
    mvnd test -pl dtr-core -Darguments="-verbose:class"
```

### Migration Path

**Java 27 (2026 H2):** Lazy constants become **stable** (no preview flag).

```java
// Java 27+: Remove --enable-preview
// API remains identical
```

---

## JEP 525: Structured Concurrency (Sixth Preview)

### What Changed

**Java 19–25:** Five previews of `StructuredTaskScope` and `StructuredTaskScope.ShutdownOnSuccess` / `ShutdownOnFailure`.

**Java 26:** Sixth preview incorporates feedback:
- Improved error messages for task scope lifecycle violations
- Refinements to cancel semantics and exception propagation
- Introduction of `StructuredTaskScope.virtual(threadFactory)` for virtual thread customization

### How to Use

**Before (Java 26 — manual thread coordination):**
```java
// MultiRenderMachine.java — manual virtual thread management (current)
private void dispatchToAll(Consumer<RenderMachine> action) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var futures = machines.stream()
            .map(m -> executor.submit(() -> { action.accept(m); return null; }))
            .toList();
        for (var future : futures) {
            future.get();  // Manual wait-for-completion
        }
    }
}
```

**After (Java 26 with Structured Concurrency):**
```java
// Using StructuredTaskScope for cleaner cancellation and error handling
private void dispatchToAll(Consumer<RenderMachine> action) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        // All subtasks are children of this scope
        // If any task fails, all are cancelled automatically
        for (var machine : machines) {
            scope.fork(() -> { action.accept(machine); return null; });
        }
        scope.joinUntil(Instant.now().plusSeconds(30));  // Deadline
        scope.throwIfFailed();  // Propagate any task exceptions
    } // try-with-resources ensures all tasks cancelled on exit
}
```

### Key Improvements in Java 26

| Aspect | Java 26 | Java 27+ |
|--------|---------|---------|
| **Task Cancellation** | Manual future.cancel() | Automatic on scope close |
| **Deadlines** | No built-in deadline support | `joinUntil(Instant)` |
| **Error Propagation** | Manual exception handling | Structured exception groups |
| **Virtual Thread Customization** | Global executor | Per-scope thread factory |

### DTR Application

**Current (Java 26):** `MultiRenderMachine` uses manual virtual thread executor:

```java
// Status: USING WITH PREVIEW
@Override
public void say(String text) {
    dispatchToAll(m -> m.say(text));
}

private void dispatchToAll(Consumer<RenderMachine> action) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var futures = machines.stream()
            .map(m -> executor.submit(() -> { action.accept(m); return null; }))
            .toList();
        for (var future : futures) {
            try { future.get(); } catch (Exception e) {
                throw new RenderException("Format failed: " + e.getMessage(), e);
            }
        }
    }
}
```

**Java 26 Opportunity:** Adopt structured task scope for automatic cancellation and cleaner deadlines:

```java
// Java 26 refactored version
@Override
public void say(String text) throws RenderException {
    dispatchToAllStructured(m -> m.say(text));
}

private void dispatchToAllStructured(Consumer<RenderMachine> action)
        throws RenderException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        // Virtual thread customization in Java 26
        var virtualScope = StructuredTaskScope.virtual(
            Thread.ofVirtual().name("dtr-render-", 0).factory()
        );

        for (var machine : machines) {
            virtualScope.fork(() -> { action.accept(machine); return null; });
        }

        // Deadline: if rendering takes > 60s, cancel all tasks
        var deadline = Instant.now().plusSeconds(60);
        virtualScope.joinUntil(deadline);
        virtualScope.throwIfFailed();
    } catch (InterruptedException e) {
        throw new RenderException("Rendering interrupted: " + e.getMessage(), e);
    }
}
```

### Build Setup

```bash
# Structured concurrency requires --enable-preview
# Already configured in pom.xml
mvnd clean compile -pl dtr-core -DskipTests
```

### Testing

```bash
# Test structured concurrency lifecycle
mvnd test -pl dtr-core -Dtest=MultiRenderMachineTest

# Performance: measure deadline enforcement
mvnd test -pl dtr-core -Dtest=StructuredConcurrencyBenchmark

# Full validation
mvnd clean verify -DskipTests -pl dtr-core && \
    mvnd test -pl dtr-core
```

### Migration Path

**Java 27 (H2 2026):** Structured Concurrency **likely becomes stable** (6 previews is the pattern before stabilization).

```java
// Java 27+: Remove --enable-preview flag when stable
<compilerArgs>
    <!-- Remove when JEP 525 graduates -->
    <arg>--enable-preview</arg>
</compilerArgs>
```

---

## JEP 517: HTTP/3 Support

### What Changed

**Java 22–25:** HTTP Client API supports HTTP/1.1 and HTTP/2.

**Java 26:** First preview of HTTP/3 support, enabling QUIC protocol (RFC 9000):
- Multiplexing over a single UDP connection (no head-of-line blocking)
- Faster handshake (0-RTT resumption)
- Connection migration (IP address changes don't break the stream)

### How to Use

**Before (Java 26 — HTTP/1.1, HTTP/2 only):**
```java
// Only HTTP/1.1 and HTTP/2 available
var client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .build();

var request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/users"))
    .GET()
    .build();

var response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

**After (Java 26 with HTTP/3 preview):**
```java
// HTTP/3 available as preview — requires --enable-preview
import java.net.http.HttpClient.Version;

var client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_3)  // NEW in Java 26
    .connectTimeout(Duration.ofSeconds(5))  // Faster handshake
    .build();

var request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/users"))
    .GET()
    .build();

var response = client.send(request, HttpResponse.BodyHandlers.ofString());
// Uses QUIC/UDP underneath — no TCP overhead
```

### Key Benefits of HTTP/3

| Aspect | HTTP/2 (TCP) | HTTP/3 (QUIC/UDP) |
|--------|--------------|------------------|
| **Handshake** | TLS + TCP = 2 RTTs | TLS integrated = 1 RTT (or 0-RTT) |
| **Multiplexing** | Single TCP stream (head-of-line blocking) | Independent QUIC streams |
| **Connection Migration** | TCP bind to IP; breaks on WiFi↔5G switch | Transparent IP migration |
| **Latency (mobile)** | Higher (TCP retransmission) | Lower (QUIC FEC) |

### DTR Application

**Potential Enhancement:** TestBrowser could optionally use HTTP/3 for faster test execution:

```java
// Current TestBrowserImpl.java (Java 26)
public class TestBrowserImpl implements TestBrowser {
    private final HttpClient client;

    public TestBrowserImpl() {
        this.client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
    }
}

// Java 26+ with HTTP/3 support
public class TestBrowserImpl implements TestBrowser {
    private final HttpClient client;
    private final boolean useHttp3;

    public TestBrowserImpl(boolean useHttp3) {
        var builder = HttpClient.newBuilder();

        if (useHttp3) {
            // Requires --enable-preview
            builder.version(HttpClient.Version.HTTP_3);
        } else {
            builder.version(HttpClient.Version.HTTP_2);
        }

        this.client = builder.build();
        this.useHttp3 = useHttp3;
    }

    public static TestBrowser withHttp3() {
        return new TestBrowserImpl(true);
    }
}

// Usage in test
@Test
void testWithHttp3(DTRContext ctx) {
    ctx.setTestBrowser(TestBrowserImpl.withHttp3());

    Response response = ctx.sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/users")));

    ctx.say("Request sent via QUIC/HTTP3 — faster handshake");
}
```

### Build Setup

HTTP/3 requires `--enable-preview`:

```bash
# Already enabled
mvnd clean compile -pl dtr-core
```

### Testing

```bash
# Requires test server with HTTP/3 support (e.g., nginx, Netty, Vert.x)
# Test HTTP/3 availability
mvnd test -pl dtr-core -Dtest=Http3IntegrationTest

# Performance: HTTP/3 vs HTTP/2 latency comparison
mvnd test -pl dtr-core -Dtest=ProtocolBenchmark
```

### Migration Path

**Java 27+:** HTTP/3 will likely remain preview for additional refinements. Stabilization uncertain until RFC 9000 implementations mature.

---

## JEP 516: AOT Object Caching with Any GC

### What Changed

**Java 26:** AOT (Ahead-of-Time) compilation via GraalVM, but object cache tied to G1 GC.

**Java 26:** AOT object caching works with **any garbage collector** (G1, ZGC, Shenandoah, Serial, Parallel). Enables Project Leyden's goal of faster Java startup for containerized workloads.

### How to Use

**Before (Java 26):**
```bash
# AOT compilation only with G1 GC
java -XX:+UseG1GC -XX:+WriteAOTSnapshot -XX:AOTSnapshot=app.jsa MyApp

# ZGC not supported
# java -XX:+UseZGC -XX:+WriteAOTSnapshot  # ERROR: AOT cache not compatible with ZGC
```

**After (Java 26):**
```bash
# AOT compilation with any GC
java -XX:+UseZGC -XX:+WriteAOTSnapshot -XX:AOTSnapshot=app.jsa MyApp

# Or with Shenandoah
java -XX:+UseShenandoahGC -XX:+WriteAOTSnapshot -XX:AOTSnapshot=app.jsa MyApp

# Restore cached objects at startup
java -XX:+UseZGC -XX:AOTSnapshot=app.jsa -XX:+UseAOTSnapshot MyApp
```

### Performance Impact

AOT object caching provides **startup time reduction**:

| Startup Phase | Improvement |
|---------------|-------------|
| **Interpreter warm-up** | Not affected (AOT is for constant initialization, not execution) |
| **JIT compilation** | ~5–10% reduction (fewer objects to initialize on first run) |
| **Garbage collection init** | ~10–15% reduction for large object caches (shared Java libraries) |
| **Overall cold start** | ~5–20% depending on workload (container startup) |

### DTR Application

**Not directly applicable** — DTR is a testing library (not a long-running service). However, in CI/CD pipelines running many small test processes, AOT caching would reduce aggregate startup overhead:

```bash
# Pre-build AOT cache for DTR test suite
# (Run once in CI setup)
java -XX:+UseZGC \
     -XX:+WriteAOTSnapshot \
     -XX:AOTSnapshot=/tmp/dtr-test.jsa \
     -jar dtr-core-tests.jar

# Run tests with cached objects (faster startup)
java -XX:+UseZGC \
     -XX:AOTSnapshot=/tmp/dtr-test.jsa \
     -XX:+UseAOTSnapshot \
     org.junit.platform.console.ConsoleLauncher \
     --scan-classpath
```

### Build Setup

No Maven configuration changes required. AOT caching is a JVM runtime option, not compile-time.

```bash
# Standard compile still applies
mvnd clean compile -pl dtr-core
```

### Testing

```bash
# Performance benchmark: AOT cache effectiveness
mvnd test -pl dtr-core \
    -Dtest=AotCachingBenchmark \
    -DuseAotCache=true
```

### Migration Path

**Java 27+:** AOT caching will likely be expanded to more garbage collectors and may eventually become the default startup optimization path (Project Leyden).

---

## JEP 524: PEM Encodings (Second Preview)

### What Changed

**Java 26:** First preview of `PEM.readPrivateKey()`, `PEM.writePrivateKey()` for cryptographic object serialization in PEM format (RFC 1421).

**Java 26:** Second preview refines:
- Support for encrypted private keys (PKCS#8 with password protection)
- Generalized PEM encoding for certificates, public keys, and PKCS#10 requests
- Better error messages for malformed PEM data

### How to Use

**Before (Java 26):**
```java
// Manual PEM parsing (tedious)
String pemData = new String(Files.readAllBytes(Path.of("private.pem")));
String base64 = pemData
    .replaceAll("-----.*-----", "")
    .replaceAll("\\s+", "");
byte[] decoded = Base64.getDecoder().decode(base64);
KeyFactory kf = KeyFactory.getInstance("RSA");
PrivateKey key = kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
```

**After (Java 26 with PEM encoding API):**
```java
// Simplified with PEM API (preview)
PrivateKey key = PEM.readPrivateKey(
    Files.newInputStream(Path.of("private.pem")),
    "RSA"  // Key algorithm
);

// Write encrypted private key back to PEM
try (var os = Files.newOutputStream(Path.of("private-encrypted.pem"))) {
    PEM.writePrivateKey(key, os, "RSA", "password-here".toCharArray());
}

// Read certificate chain
List<Certificate> certs = PEM.readCertificates(
    Files.newInputStream(Path.of("cert-chain.pem"))
);
```

### DTR Application

**Potential Use:** OAuth2TokenManager and authentication providers could simplify key/certificate handling:

```java
// Current: manual key loading
public class OAuth2TokenManager {
    public OAuth2TokenManager(String certFile) throws Exception {
        PrivateKey key = (PrivateKey) KeyStore.getInstance("JKS")
            .getEntry("cert", new KeyStore.PasswordProtection("pass".toCharArray()))
            .getPrivateKey();
        this.key = key;
    }
}

// Java 26+ with PEM encoding
public class OAuth2TokenManager {
    public OAuth2TokenManager(String pemFile) throws Exception {
        this.key = PEM.readPrivateKey(
            Files.newInputStream(Path.of(pemFile)),
            "RSA"
        );
    }

    public void writeCertificateChain(String outPath) throws Exception {
        try (var os = Files.newOutputStream(Path.of(outPath))) {
            for (var cert : certChain) {
                PEM.writeCertificate(cert, os);
            }
        }
    }
}
```

### Build Setup

PEM encoding requires `--enable-preview`:

```bash
# Already enabled
mvnd clean compile -pl dtr-core
```

### Testing

```bash
# Test PEM encoding round-trip
mvnd test -pl dtr-core -Dtest=PemEncodingTest

# OAuth2 certificate loading with PEM
mvnd test -pl dtr-core -Dtest=OAuth2TokenManagerTest
```

### Migration Path

**Java 27+:** PEM encodings will likely stabilize (2nd preview is often final before release).

---

## JEP 529: Vector API (Eleventh Incubator)

### What Changed

**Java 16–25:** Ten incubations of the Vector API for SIMD operations on arrays (data parallelism).

**Java 26:** Eleventh incubation refines:
- Complex number support
- Additional vector reduction operations
- Performance improvements for lane-wise operations

### Note for DTR

The Vector API targets **numerical/scientific computing** (matrix operations, image processing, signal analysis). **DTR does not use SIMD operations** — it is a documentation generator and HTTP test framework. This JEP is not applicable to DTR.

For reference, the Vector API enables:

```java
// Not applicable to DTR, but shown for context
import jdk.incubator.vector.*;

IntVector a = IntVector.fromArray(SPECIES_256, arr, 0);
IntVector b = IntVector.fromArray(SPECIES_256, arr, 256);
IntVector c = a.mul(b);  // Element-wise multiplication in parallel
c.intoArray(result, 0);
```

---

## JEP 500: Final Mean Final

### What Changed

**Java 26:** Deprecated reflection-based mutation of `final` fields via `Unsafe` and `Field.set()`.

**Java 26:** Transition phase — the reflection barrier is activated by default. Warnings emitted to stderr when code attempts to mutate a `final` field via `setAccessible(true).set()`.

**Java 27 (future):** Disallow completely — mutating `final` fields via reflection will throw `IllegalAccessException`.

### How to Use

**Before (Java 26 — allowed, but discouraged):**
```java
// Mutating a final field (works but breaks immutability guarantees)
public class Config {
    private final String apiKey;

    public Config(String key) {
        this.apiKey = key;
    }
}

// Reflection-based mutation (silently works in Java 26)
var field = Config.class.getDeclaredField("apiKey");
field.setAccessible(true);
field.set(config, "hacked-key");  // Breaks immutability contract
```

**After (Java 26 — warnings emitted):**

Running the same code in Java 26 produces:

```
WARNING: A direct buffer memory access operation has occured from a user-supplied method (com.example.Config)
WARNING: Illegal reflective access by java.lang.reflect.Field::set to field org.example.Config.apiKey
WARNING: Please consider reporting this to the maintainers of com.example.Config
WARNING: Use --illegal-access=permit to suppress the warning (default in Java 26, denied in Java 27)
```

### Proper Pattern (Immutable Record)

```java
// Java 26/27+: Use records for guaranteed immutability
public record Config(String apiKey) {
    public Config {
        Objects.requireNonNull(apiKey, "apiKey must not be null");
    }
    // final fields guaranteed by record definition; reflection cannot mutate
}

// Or: explicit final with getter/setter pattern
public final class Config {
    private final String apiKey;

    public Config(String key) {
        this.apiKey = Objects.requireNonNull(key);
    }

    public String getApiKey() {
        return apiKey;
    }

    // No setter — immutable
}
```

### DTR Application

**Current State:** DTR already uses records (`SayEvent` hierarchy) and `final` immutable classes. No reflection-based mutation occurs.

```java
// DTR-style: immutable events via records (correct pattern)
sealed interface SayEvent permits SayEvent.TextEvent, SayEvent.CodeEvent {
    record TextEvent(String text) implements SayEvent {
        public TextEvent {
            Objects.requireNonNull(text);
        }
    }

    record CodeEvent(String code, String language) implements SayEvent {
        public CodeEvent {
            Objects.requireNonNull(code);
            Objects.requireNonNull(language);
        }
    }
}

// Events are immutable; reflection cannot mutate them
var event = new SayEvent.TextEvent("Hello");
// event.text() is final — JVM inlines it, reflection cannot change it
```

### Build Setup

No changes needed. DTR already follows immutability best practices.

```bash
mvnd clean compile -pl dtr-core
# No warnings expected (no reflection-based field mutation)
```

### Testing

```bash
# Verify no illegal reflective access warnings
mvnd clean verify 2>&1 | grep -i "warning:" | grep -i "reflective"
# Should produce no matches
```

### Migration Path

**Java 27 (late 2026):** Final field mutation via reflection becomes illegal (throws exception).

**Action Required (now):** Audit any code that mutates fields flagged `final`:

```bash
# Search for illegal reflective access patterns
grep -r "setAccessible(true).set(" src/
grep -r "Field.get\|Field.set" src/
```

If found, convert to:
- Immutable records with constructors
- Builder patterns for complex initialization
- Explicit mutable wrapper classes (if mutation is required)

---

## JEP 504: Remove Applet API

### What Changed

**Java 1.0–24:** Applet API for running Java code in web browsers (deprecated since Java 9).

**Java 26:** Applet API removed completely.

### For DTR

**Not applicable** — DTR does not use the Applet API (it's for JApplet, AppletContext, etc.).

If any dependencies in the classpath reference Applet classes, they will fail to load in Java 26+:

```java
// This fails in Java 26+
import java.applet.Applet;  // ERROR: Cannot resolve symbol 'applet'
```

**Action:** Verify no transitive dependencies require java.applet:

```bash
# Check for applet API usage in classpath
mvnd dependency:tree -pl dtr-core | grep -i applet
# Should produce no matches
```

---

## Build & Test Setup

### Prerequisites

- **Java 26** installed at `/usr/lib/jvm/java-26-openjdk-amd64`
- **Maven 4.0.0+** or **mvnd 2.0+** (Maven Daemon)
- **Unix shell** (bash, zsh) for scripting

### Current Setup (Java 26)

**pom.xml** configured for Java 26 with `--enable-preview`:

```xml
<properties>
    <maven.compiler.release>26</maven.compiler.release>
</properties>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>26</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
        <enablePreview>true</enablePreview>
    </configuration>
</plugin>
```

### Upgrade to Java 26 (March 2026+)

**Step 1: Install Java 26**

```bash
# When Java 26 GA is available (March 17, 2026)
mkdir -p /usr/lib/jvm
cd /tmp
wget https://download.java.net/java/GA/jdk26/...jdk-26_linux-x64_bin.tar.gz
tar xzf jdk-26_linux-x64_bin.tar.gz
sudo mv jdk-26 /usr/lib/jvm/java-26-openjdk-amd64
```

**Step 2: Update pom.xml**

```xml
<properties>
    <maven.compiler.release>26</maven.compiler.release>
</properties>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>26</release>
        <!-- Keep --enable-preview for 6th preview JEPs (JEP 525, 530, etc.) -->
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
        <enablePreview>true</enablePreview>
    </configuration>
</plugin>
```

**Step 3: Update JAVA_HOME**

```bash
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64

# Verify
java -version
# openjdk version "26" <release date>

# Verify preview features
java --enable-preview -version
```

**Step 4: Update Maven config**

**.mvn/maven.config** (already configured, no changes):

```
--no-transfer-progress
--batch-mode
--enable-preview
```

**Step 5: Rebuild**

```bash
mvnd clean install -pl dtr-core -DskipTests

# Or with full validation
mvnd clean verify
```

### Build Commands (Java 26 → 27 migration)

| Command | Purpose | Java 26 | Java 27 |
|---------|---------|---------|---------|
| `mvnd clean compile` | Compile source | Works | Works |
| `mvnd test` | Run tests with preview | Works | Works |
| `mvnd clean verify` | Full build + tests | Works | Works |
| `mvnd javadoc:javadoc` | Generate docs | Works with `--enable-preview` | Works |

### Environment Variables

**Current (Java 26):**

```bash
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64
export MAVEN_OPTS="--enable-preview"

mvnd --version  # Maven 4.0.0+
java -version   # openjdk 26.x.x
```

### Troubleshooting Build Issues

| Error | Cause | Fix |
|-------|-------|-----|
| `error: --enable-preview flag not allowed for target Java 26` | Preview feature used without flag | Add `<enablePreview>true</enablePreview>` to compiler config |
| `error: record is not a feature` | Java < 16 targeted | Set `<release>26</release>` (or higher) |
| `error: sealed types are not available` | Java < 17 targeted | Same as above |
| `[WARNING] Source option 5 is no longer supported` | Old compiler config | Update to `<release>26</release>` (Maven 4 standard) |
| `mvnd: command not found` | Maven Daemon not installed | Install: `cd /opt && wget https://dist.apache.org/repos/dist/release/maven/mvnd/...` |

---

## Migration Path for Released Features

### Timeline

| JEP | Current Status | Java 26 | Java 27 (H2 2026) | Java 28+ |
|-----|----------------|---------|------------------|----------|
| **530** (Primitive patterns) | 3rd preview (Java 26) | **4th preview** | Likely stable | Standard |
| **525** (Structured concurrency) | 5th preview (Java 26) | **6th preview** | Likely stable | Standard |
| **517** (HTTP/3) | New | **1st preview** | 2nd preview? | Uncertain |
| **526** (Lazy constants) | 1st preview (Java 26) | **2nd preview** | Likely stable | Standard |
| **524** (PEM encodings) | 1st preview (Java 26) | **2nd preview** | Likely stable | Standard |
| **522** (G1 sync) | — | **Stable** | Stable | Standard |
| **516** (AOT caching) | — | **1st preview** | 2nd preview? | Uncertain |
| **529** (Vector API) | 10th incubator (Java 26) | **11th incubator** | 12th? | Uncertain |
| **500** (Final) | — | **Stable** (warnings) | Enforcement | Removal |
| **504** (Applet) | — | **Removed** | Removed | Removed |

### Expected Java 27 Stabilizations

When Java 27 GA is released (expected H2 2026), these features will likely graduate:

```java
// Java 27+: Remove --enable-preview for stable features
<compilerArgs>
    <!-- After Java 27: remove the next line -->
    <arg>--enable-preview</arg>
</compilerArgs>
```

**Features likely to stabilize in Java 27:**

- JEP 530 — Primitive Types in Patterns
- JEP 525 — Structured Concurrency
- JEP 526 — Lazy Constants
- JEP 524 — PEM Encodings

### Code Migration Checklist

**Phase 1: Java 26 (March 2026)**

- [ ] Update `JAVA_HOME` to Java 26 GA
- [ ] Update `pom.xml` `<release>26</release>`
- [ ] Recompile: `mvnd clean compile`
- [ ] Run tests: `mvnd test`
- [ ] Check for deprecation warnings: `mvnd clean verify 2>&1 | grep -i "warning"`

**Phase 2: Java 27 (H2 2026)**

- [ ] Remove `--enable-preview` when JEPs stabilize
- [ ] Update pom.xml:

```xml
<compilerArgs>
    <!-- Enable only remaining preview JEPs -->
</compilerArgs>
```

- [ ] Verify stabilization: `mvnd clean compile` without `--enable-preview`
- [ ] Recompile: `mvnd clean compile -pl dtr-core`
- [ ] Run tests: `mvnd test`

**Phase 3: Java 28+ (2027+)**

- [ ] Upgrade to `<release>28</release>` if desired
- [ ] No preview features should be active
- [ ] Final code cleanup

### API Stability Guarantees

Preview features are **not guaranteed stable** until a final preview release. Changes can include:

- API method signature changes
- Parameter name changes
- Removal of entire features if they don't prove popular

**Risk mitigation:** DTR 2.6.0 targets Java 26 with preview features. When features stabilize, DTR 3.0.0 will remove preview flags. Older versions (2.5.x) remain compatible with Java 26.

---

## Performance Tuning & Measurement

### Expected Performance Gains (Java 26)

| Feature | Component | Improvement |
|---------|-----------|-------------|
| **G1 GC Synchronization Reduction** (JEP 522) | Concurrent rendering | +5–15% throughput |
| **AOT Object Caching** (JEP 516) | Test startup (CI/CD) | +5–20% cold start |
| **Structured Concurrency** (JEP 525) | Virtual thread overhead | ~2–5% (better cancellation) |
| **Lazy Constants** (JEP 526) | Template initialization | ~1–3% (if heavily used) |
| **HTTP/3** (JEP 517) | Network latency | +10–20% (QUIC handshake) |

### Benchmark Suite

**New benchmarks added to DTR for Java 26:**

```bash
# Throughput: concurrent multi-format rendering
mvnd test -pl dtr-core \
    -Dtest=MultiRenderMachinePerformanceTest \
    -Dbenchmark.iterations=1000

# Startup: AOT cache effectiveness
mvnd test -pl dtr-core \
    -Dtest=AotStartupBenchmark \
    -Dbenchmark.iterations=100

# Network: HTTP/3 vs HTTP/2 latency
mvnd test -pl dtr-core \
    -Dtest=ProtocolLatencyBenchmark \
    -Dbenchmark.iterations=500
    -Duse.http3=true
```

### Memory Profiling

**Heap usage comparison (Java 26 vs Java 27):**

```bash
# Java 26
java -Xms256m -Xmx512m \
     -XX:+UseG1GC \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -Xloggc:gc-java26.log \
     org.junit.platform.console.ConsoleLauncher \
     --scan-classpath

# Java 26 (when available)
java -Xms256m -Xmx512m \
     -XX:+UseZGC \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -Xloggc:gc-java26.log \
     org.junit.platform.console.ConsoleLauncher \
     --scan-classpath

# Compare GC logs
# Expected: fewer pauses with JEP 522 synchronization reduction
```

### Measuring Concurrent Rendering Throughput

**Before (Java 26):**

```java
// MultiRenderMachine throughput test
@Test
void benchmarkRenderingThroughput() {
    var machines = List.of(
        new RenderMachineImpl(),
        new RenderMachineLatex(LatexTemplate.ACM_CONFERENCE),
        new RenderMachineLatex(LatexTemplate.ARXIV),
        // ... 11 formats total
    );

    var multi = new MultiRenderMachine(machines);

    long start = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
        multi.say("Paragraph " + i);
        multi.sayCode("code", "java");
        multi.sayTable(new String[][] { {"A", "B"} });
    }
    long millis = (System.nanoTime() - start) / 1_000_000;

    System.out.println("Java 26: " + millis + " ms for 30k say* calls across 11 formats");
    // Expected: ~500-800 ms (sequential virtual threads)
}
```

**After (Java 26 with JEP 522):**

```bash
# Run same test on Java 26
mvnd test -pl dtr-core -Dtest=MultiRenderMachinePerformanceTest

# Expected: ~450-750 ms (5–15% improvement from G1 sync reduction)
```

### Profiling with JFR (Java Flight Recorder)

**Profile Java 26 rendering pipeline:**

```bash
# Start recording
java -XX:+UnlockDiagnosticVMOptions \
     -XX:+DebugNonSafepoints \
     -XX:StartFlightRecording=duration=60s,filename=dtr.jfr \
     org.junit.platform.console.ConsoleLauncher \
     --scan-classpath

# Analyze
jfr print --json dtr.jfr > dtr-profile.json

# Extract: which renders are slowest?
jfr print --events=jdk.ExecutionSample dtr.jfr | \
    grep -i "RenderMachine" | \
    head -20
```

### Profiling HTTP/3 Latency

```bash
# Assuming test server supports HTTP/3
@Test
void benchmarkHttp3Latency() {
    var client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_3)
        .build();

    var request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.example.com/api/data"))
        .GET()
        .build();

    long start = System.nanoTime();
    var response = client.send(request,
        HttpResponse.BodyHandlers.ofString());
    long nanos = System.nanoTime() - start;

    System.out.println("HTTP/3 latency: " + (nanos / 1_000_000) + " ms");
    // Expected: 10–20% faster than HTTP/2 for subsequent requests
}
```

---

## FAQ

### Q: Can I use Java 26 now?

**A:** Java 26 is in Release Candidate phase (as of March 11, 2026). General Availability is March 17, 2026. For production use, wait for GA. For development/testing, RC builds are stable.

### Q: Do I need to change my code when upgrading Java 26 → 27?

**A:** No breaking changes for DTR. Update `<release>26</release>` in pom.xml and rebuild. All preview features continue to work with `--enable-preview`.

### Q: When will preview features graduate to stable?

**A:** Likely in Java 27 (H2 2026), based on the preview pattern:
- 4th preview → stable (JEP 530, 525)
- 2nd preview → stable (JEP 526, 524)

### Q: Should I use HTTP/3 immediately?

**A:** Only if your test server supports HTTP/3 (e.g., Netty, Vert.x, nginx). Most servers still use HTTP/2. HTTP/3 is useful for **mobile testing** where connection migration matters.

### Q: What about the Vector API?

**A:** Not applicable to DTR (SIMD is for numerical computing). Skip JEP 529.

### Q: Is Structured Concurrency stable?

**A:** It's the 6th preview (Java 26). Likely stable in Java 27. The API has been stable for 2+ years; stabilization is mainly formality.

### Q: What happens to my Java 26 code when Java 27 is released?

**A:** No changes. Java maintains **backward compatibility**. Java 26 code runs unchanged on Java 27, 28, 29, etc. Preview features just require `--enable-preview` flag.

### Q: Should I target Java 26 or 27 for DTR 2.6.0?

**A:** Target Java **26 GA** (March 17, 2026):

```xml
<maven.compiler.release>26</maven.compiler.release>
```

This ensures access to all Java 26 features (stable and preview). Older Java versions (22–25) can still use DTR 2.5.x.

### Q: What about Java 27 or 28?

**A:** Java releases a new major version every 6 months (September and March). Java 27 GA: September 2026. No need to plan beyond Java 26 until then.

### Q: Does DTR 2.6.0 require Java 26?

**A:** It requires **Java 26 minimum** (for sealed classes, records, pattern matching). Older Java versions should use DTR 2.5.x (Java 26 or earlier).

### Q: How do I verify my code has no illegal reflective access?

```bash
mvnd clean verify 2>&1 | grep -i "warning.*reflective"
# Should produce no output (only deprecation warnings for experimental features)
```

### Q: Can I use multiple preview features in one project?

**A:** Yes, as long as `--enable-preview` is active **globally** in maven-compiler-plugin. Individual JEPs don't need per-feature flags.

### Q: What's the minimum Java version for DTR 2.6.0?

**A:** Java 26 GA or RC (March 17, 2026+). Sealed classes, records, and pattern matching require Java 16+, but DTR standardizes on Java 26.

### Q: Will DTR 2.5.x continue to work on Java 26?

**A:** Yes, but you'll miss Java 26–specific optimizations (G1 sync, AOT caching, HTTP/3). Recommended: upgrade to 2.6.0+ when available.

### Q: How do I report issues with Java 26 preview features?

**A:** Use OpenJDK's bug tracker:

```
https://bugs.openjdk.org/browse/JDK
Component: "Specification" (for JEP issues)
```

Or report to DTR GitHub: https://github.com/seanchatmangpt/dtr/issues

---

## Resources

### Official OpenJDK References

- [OpenJDK JDK 26 Release](https://openjdk.org/projects/jdk/26/)
- [Java Enhancement Proposals (JEPs)](https://openjdk.org/jeps/0)

### Java 26 JEP Documentation

- **[JEP 530](https://openjdk.org/jeps/530)** — Primitive Types in Patterns, instanceof, and switch (Fourth Preview)
- **[JEP 526](https://openjdk.org/jeps/526)** — Lazy Constants (Second Preview)
- **[JEP 525](https://openjdk.org/jeps/525)** — Structured Concurrency (Sixth Preview)
- **[JEP 517](https://openjdk.org/jeps/517)** — HTTP/3 for the HTTP Client API (First Preview)
- **[JEP 516](https://openjdk.org/jeps/516)** — Ahead-of-Time Object Caching with Any GC
- **[JEP 524](https://openjdk.org/jeps/524)** — PEM Encodings of Cryptographic Objects (Second Preview)
- **[JEP 522](https://openjdk.org/jeps/522)** — G1 GC: Improve Throughput by Reducing Synchronization
- **[JEP 529](https://openjdk.org/jeps/529)** — Vector API (Eleventh Incubator)
- **[JEP 500](https://openjdk.org/jeps/500)** — Prepare to Make Final Mean Final
- **[JEP 504](https://openjdk.org/jeps/504)** — Remove the Java Applet API

### Java 26 News & Announcements

- [InfoQ: JDK 26 Features](https://www.infoq.com/news/2026/02/java-26-so-far/)
- [HappyCoders: Java 26 Features with Examples](https://www.happycoders.eu/java/java-26-features/)
- [Foojay: Java 26 What's New](https://foojay.io/today/java-26-whats-new/)

### Build & Tooling

- [OpenJDK Build Downloads](https://jdk.java.net/26/) — Java 26 RC builds
- [Maven 4 Release](https://maven.apache.org/) — Maven 4.0.0+
- [Maven Daemon (mvnd)](https://github.com/apache/maven-mvnd) — Fast Maven builds

### DTR Resources

- **Project:** https://github.com/seanchatmangpt/dtr
- **Issues:** https://github.com/seanchatmangpt/dtr/issues
- **Documentation:** [CLAUDE.md](./CLAUDE.md) (this repository)

### Performance & JVM Tuning

- [GC Tuning Guide](https://docs.oracle.com/javase/17/gctuning/) — G1 GC options
- [JFR (Java Flight Recorder)](https://docs.oracle.com/javase/17/docs/api/jdk.jfr/module-summary.html) — Profiling
- [JMH (Java Microbenchmark Harness)](https://github.com/openjdk/jmh) — Benchmarking

---

## Summary

Java 26 (GA: March 17, 2026) introduces 10 JEPs that improve DTR in three ways:

1. **Language:** JEP 530 (primitive patterns) refines type-safe pattern dispatch
2. **Concurrency:** JEP 525 (structured concurrency) improves virtual thread lifecycle; JEP 517 (HTTP/3) modernizes networking
3. **Performance:** JEP 522 (G1 sync) provides automatic throughput gains; JEP 516 (AOT caching) enables container startup optimization

**Immediate Actions (March 2026):**

1. Install Java 26 GA when available
2. Update `<release>26</release>` in pom.xml
3. Run `mvnd clean verify` to ensure compatibility
4. Benchmark concurrent rendering (expect 5–15% improvement from JEP 522)

**Later (Java 27, H2 2026):**

1. Remove `--enable-preview` when JEPs stabilize
2. Upgrade DTR 2.5.x → 2.6.0+ to adopt Java 26 optimizations
3. Consider HTTP/3 if test servers support QUIC

**Long-term (Java 28+, 2027):**

1. Reassess JVM tuning based on latest GC improvements
2. Adopt new features as they mature

For questions or issues, refer to the [OpenJDK JEP tracker](https://openjdk.org/jeps/0) or [DTR GitHub](https://github.com/seanchatmangpt/dtr/issues).

---

**Document Version:** 1.0
**Last Updated:** March 11, 2026
**Target Audience:** Java developers adopting Java 26 in DTR
**License:** Apache 2.0
