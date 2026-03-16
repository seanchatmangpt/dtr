# io.github.seanchatmangpt.dtr.test.BlueOceanInnovationsTest

## Table of Contents

- [A1: sayCodeModel(Method) — Java 26 Code Reflection](#a1saycodemodelmethodjava26codereflection)
- [A2: sayControlFlowGraph(Method) — Mermaid CFG](#a2saycontrolflowgraphmethodmermaidcfg)
- [A3: sayCallGraph(Class<?>) — Method Call Relationships](#a3saycallgraphclassmethodcallrelationships)
- [A4: sayOpProfile(Method) — Lightweight Op Stats](#a4sayopprofilemethodlightweightopstats)
- [B1: sayBenchmark() — Inline Performance Documentation](#b1saybenchmarkinlineperformancedocumentation)
- [B2: sayMermaid() + sayClassDiagram() — Mermaid Diagrams](#b2saymermaidsayclassdiagrammermaiddiagrams)
- [B3: sayDocCoverage() — Documentation Coverage Report](#b3saydoccoveragedocumentationcoveragereport)
- [C1: sayEnvProfile() — Zero-Parameter Environment Snapshot](#c1sayenvprofilezeroparameterenvironmentsnapshot)
- [C2: sayRecordComponents() — Java Record Schema](#c2sayrecordcomponentsjavarecordschema)
- [C3: sayException() — Exception Chain Documentation](#c3sayexceptionexceptionchaindocumentation)
- [C4: sayContractVerification() — Interface Contract Coverage](#c4saycontractverificationinterfacecontractcoverage)
- [C5: sayEvolutionTimeline() — Git Evolution Timeline](#c5sayevolutiontimelinegitevolutiontimeline)
- [C5: sayAsciiChart() — Inline ASCII Bar Chart](#c5sayasciichartinlineasciibarchart)
- [C7: saySecurityManager() — Java Security Environment](#c7saysecuritymanagerjavasecurityenvironment)
- [C8: sayThreadDump() — JVM Thread State](#c8saythreaddumpjvmthreadstate)


## A1: sayCodeModel(Method) — Java 26 Code Reflection

Implements the previously-stubbed `sayCodeModel(Method)` using the Java 26 Code Reflection API (JEP 516 / Project Babylon). When a method is annotated with `@CodeReflection`, `method.codeModel()` returns an `Optional<CoreOps.FuncOp>` whose IR tree is walked to extract operation types, block counts, and an IR excerpt.

```java
// On Java 26+: annotate method to make its code model available
// @java.lang.reflect.code.CodeReflection
static int exampleSum(int a, int b) {
    if (a > 0) { return a + b; }
    return b;
}

// In test:
Method m = BlueOceanInnovationsTest.class
    .getDeclaredMethod("exampleSum", int.class, int.class);
sayCodeModel(m);  // uses Code Reflection IR when available
```

### Method Code Model: `int exampleSum(int, int)`

*(Code model not available — method not annotated with `@CodeReflection` or runtime < Java 26)*

**Signature:** `int exampleSum(int, int)`

> [!NOTE]
> When `@CodeReflection` is present and Java 26+ preview is enabled, the table shows real op types from the JVM's code model. Without the annotation, the method falls back to signature rendering.

## A2: sayControlFlowGraph(Method) — Mermaid CFG

Extracts the control flow graph from a `@CodeReflection`-annotated method and renders it as a Mermaid `flowchart TD` diagram. Each basic block becomes a node; branch op successors become directed edges. Renders natively on GitHub, GitLab, and Obsidian.

### Control Flow Graph: `exampleSum`

*(Control flow graph not available — method requires `@CodeReflection` annotation and Java 26+)*

> [!NOTE]
> If no code model is available, a fallback message is shown. Enable with `--enable-preview` and `@CodeReflection`.

## A3: sayCallGraph(Class<?>) — Method Call Relationships

For each `@CodeReflection`-annotated method in the given class, extracts all `InvokeOp` targets from the Code Reflection IR and renders a Mermaid `graph LR` showing caller → callee relationships.

### Call Graph: `BlueOceanInnovationsTest`

*(Call graph not available — methods require `@CodeReflection` annotation and Java 26+)*

> [!NOTE]
> Only methods annotated with `@CodeReflection` contribute edges. Non-annotated methods are skipped.

## A4: sayOpProfile(Method) — Lightweight Op Stats

Same Code Reflection traversal as `sayCodeModel(Method)` but renders only the op-count table — no IR excerpt. One-liner for quick performance characterization of a method's complexity.

### Op Profile: `int exampleSum(int, int)`

*(Op profile not available — method requires `@CodeReflection` annotation and Java 26+)*

## B1: sayBenchmark() — Inline Performance Documentation

Atomically measures and documents real performance in one call. Uses `System.nanoTime()` in a tight loop with configurable warmup rounds. Uses Java 26 virtual threads (`StructuredTaskScope`) for parallel warmup batches to reduce JIT cold-start bias. Reports avg/min/max/p99 ns and throughput ops/sec.

```java
var map = Map.of("key", 42);
sayBenchmark("HashMap.get() lookup",
    () -> map.get("key"),
    50,    // warmup rounds
    500);  // measure rounds
```

### Benchmark: HashMap.get() lookup

| Metric | Result |
| --- | --- |
| Avg | `365 ns` |
| Min | `41 ns` |
| Max | `78125 ns` |
| p99 | `750 ns` |
| Ops/sec | `2,739,726` |
| Warmup rounds | `50` |
| Measure rounds | `500` |
| Java | `26` |

String concatenation benchmark — shows allocation cost:

### Benchmark: String.valueOf(int)

| Metric | Result |
| --- | --- |
| Avg | `538 ns` |
| Min | `208 ns` |
| Max | `44166 ns` |
| p99 | `7917 ns` |
| Ops/sec | `1,858,736` |
| Warmup rounds | `50` |
| Measure rounds | `200` |
| Java | `26` |

> [!NOTE]
> All numbers are real `System.nanoTime()` measurements on Java 26. No simulation.

## B2: sayMermaid() + sayClassDiagram() — Mermaid Diagrams

Two new diagram methods:

- `sayMermaid(String dsl)` — raw passthrough: render any Mermaid diagram
- `sayClassDiagram(Class<?>... classes)` — auto-generates `classDiagram` DSL from reflection

**Raw Mermaid passthrough:**

```mermaid
sequenceDiagram
    participant Test
    participant DtrContext
    participant RenderMachine
    Test->>DtrContext: sayBenchmark("label", task)
    DtrContext->>RenderMachine: sayBenchmark("label", task)
    RenderMachine->>BenchmarkRunner: run(task, 50, 500)
    BenchmarkRunner-->>RenderMachine: Result(avgNs, p99Ns, opsPerSec)
    RenderMachine-->>Test: markdown table written
```

**Auto-generated class diagram from reflection:**

### Class Diagram: RenderMachine, RenderMachineImpl, RenderMachineCommands

```mermaid
classDiagram
    RenderMachineCommands <|.. RenderMachine
    RenderMachine <|-- RenderMachineImpl
    class RenderMachine {
        +finishAndWriteOut()
        +sayAnnotationProfile(Class)
        +sayAsciiChart(String, double[], String[])
        +sayBenchmark(String, Runnable, int, int)
        +sayBenchmark(String, Runnable)
        +sayCallGraph(Class)
    }
    class RenderMachineImpl {
        +convertTextToId(String)
        +finishAndWriteOut()
        +say(String)
        +sayAnnotationProfile(Class)
        +sayAsciiChart(String, double[], String[])
        +sayAssertions(Map)
    }
    class RenderMachineCommands {
        +say(String)
        +sayAnnotationProfile(Class)
        +sayAsciiChart(String, double[], String[])
        +sayAssertions(Map)
        +sayBenchmark(String, Runnable)
        +sayBenchmark(String, Runnable, int, int)
    }
```

## B3: sayDocCoverage() — Documentation Coverage Report

The first documentation coverage tool for Java — analogous to code coverage but for API documentation. Tracks which `say*` methods were called during the test and which public methods of the target class were documented.

```java
int x = 1 + 1;
```

| Method | Coverage |
| --- | --- |
| sayCode | demonstrated above |
| sayTable | this table |

> [!WARNING]
> sayDocCoverage tracks documented method names automatically.

Coverage report for `RenderMachineCommands` — the core say* API interface:

### Documentation Coverage: `RenderMachineCommands`

*(Coverage data not available — use DtrContext.sayDocCoverage() in tests)*

## C1: sayEnvProfile() — Zero-Parameter Environment Snapshot

One-liner that documents the complete runtime environment. No parameters — reads `System.getProperty()` and `Runtime.getRuntime()`. Useful as a reproducibility footer in any benchmark or test section.

### Environment Profile

| Property | Value |
| --- | --- |
| Java Version | `26` |
| Java Vendor | `Oracle Corporation` |
| OS | `Mac OS X aarch64` |
| Processors | `16` |
| Max Heap | `12288 MB` |
| Timezone | `America/Los_Angeles` |
| DTR Version | `2.6.0` |
| Timestamp | `2026-03-16T03:13:48.143867Z` |

## C2: sayRecordComponents() — Java Record Schema

Documents a Java record's component schema using `Class.getRecordComponents()` (Java 16+). Shows component names, types, generic types, and annotations. Zero new reflection machinery — reuses `getRecordComponents()` already present in `sayCodeModel(Class<?>)`.

```java
record CallSiteRecord(String className, String methodName, int lineNumber) {}
```

### Record Schema: `CallSiteRecord`

| Component | Type | Generic Type | Annotations |
| --- | --- | --- | --- |
| `className` | `String` | — | — |
| `methodName` | `String` | — | — |
| `lineNumber` | `int` | — | — |

The schema is live — if the record changes, the docs update automatically on next test run.

## C3: sayException() — Exception Chain Documentation

Documents a `Throwable` with its type, message, full cause chain, and top 5 stack frames. Uses only standard `Throwable` API — zero new dependencies. Essential for resilience and error-handling documentation.

### Exception: `IllegalArgumentException`

**Message:** value must be positive

**Cause chain:**
- `NullPointerException`: key was null

**Stack Trace (top 5 frames):**

| # | Class | Method | Line |
| --- | --- | --- | --- |
| 1 | `io.github.seanchatmangpt.dtr.test.BlueOceanInnovationsTest` | `c3_sayException_exception_chain_documentation` | 271 |
| 2 | `jdk.internal.reflect.DirectMethodHandleAccessor` | `invoke` | 104 |
| 3 | `java.lang.reflect.Method` | `invoke` | 565 |
| 4 | `org.junit.platform.commons.util.ReflectionUtils` | `invokeMethod` | 701 |
| 5 | `org.junit.platform.commons.support.ReflectionSupport` | `invokeMethod` | 502 |

> [!NOTE]
> The cause chain is fully unwound so readers see every level of exception wrapping.

## C4: sayContractVerification() — Interface Contract Coverage

Documents interface contract coverage across implementation classes. For each public method in the contract interface, checks whether each implementation class provides a concrete override (✅ direct), inherits it (↗ inherited), or is missing it entirely (❌ MISSING). Uses only standard Java reflection — no external dependencies.

### Contract Verification: `RenderMachineCommands`

| Method | RenderMachineImpl |
| --- | --- |
| `void say(String)` | ✅ direct |
| `void sayAnnotationProfile(Class)` | ✅ direct |
| `void sayAsciiChart(String, double[], String[])` | ✅ direct |
| `void sayAssertions(Map)` | ✅ direct |
| `void sayBenchmark(String, Runnable)` | ✅ direct |
| `void sayBenchmark(String, Runnable, int, int)` | ✅ direct |
| `void sayCallGraph(Class)` | ✅ direct |
| `void sayCallSite()` | ✅ direct |
| `void sayCite(String)` | ✅ direct |
| `void sayCite(String, String)` | ✅ direct |
| `void sayClassDiagram(Class[])` | ✅ direct |
| `void sayClassHierarchy(Class)` | ✅ direct |
| `void sayCode(String, String)` | ✅ direct |
| `void sayCodeModel(Class)` | ✅ direct |
| `void sayCodeModel(Method)` | ✅ direct |
| `void sayContractVerification(Class, Class[])` | ✅ direct |
| `void sayControlFlowGraph(Method)` | ✅ direct |
| `void sayDocCoverage(Class[])` | ✅ direct |
| `void sayEnvProfile()` | ✅ direct |
| `void sayEvolutionTimeline(Class, int)` | ✅ direct |
| `void sayException(Throwable)` | ✅ direct |
| `void sayFootnote(String)` | ✅ direct |
| `void sayJavadoc(Method)` | ✅ direct |
| `void sayJson(Object)` | ✅ direct |
| `void sayKeyValue(Map)` | ✅ direct |
| `void sayMermaid(String)` | ✅ direct |
| `void sayMethodSignature(Method)` | ✅ direct |
| `void sayModuleDependencies(Class[])` | ✅ direct |
| `void sayNextSection(String)` | ✅ direct |
| `void sayNote(String)` | ✅ direct |
| `void sayOpProfile(Method)` | ✅ direct |
| `void sayOperatingSystem()` | ✅ direct |
| `void sayOrderedList(List)` | ✅ direct |
| `void sayRaw(String)` | ✅ direct |
| `void sayRecordComponents(Class)` | ✅ direct |
| `void sayRef(DocTestRef)` | ✅ direct |
| `void sayReflectiveDiff(Object, Object)` | ✅ direct |
| `void saySecurityManager()` | ✅ direct |
| `void sayStringProfile(String)` | ✅ direct |
| `void saySystemProperties()` | ✅ direct |
| `void saySystemProperties(String)` | ✅ direct |
| `void sayTable(String[][])` | ✅ direct |
| `void sayThreadDump()` | ✅ direct |
| `void sayUnorderedList(List)` | ✅ direct |
| `void sayWarning(String)` | ✅ direct |

**All contract methods covered across all implementations.**

> [!NOTE]
> If the contract is a sealed interface, permitted subclasses are auto-detected.

## C5: sayEvolutionTimeline() — Git Evolution Timeline

Derives the git commit history for the source file of the given class using `git log --follow` and renders it as a timeline table (commit hash, date, author, subject). Falls back gracefully with a NOTE if git is unavailable.

### Evolution Timeline: `RenderMachineImpl`

| Commit | Date | Author | Summary |
| --- | --- | --- | --- |
| `4b0655c` | 2026-03-15 | Sean Chatman | feat: implement DTR 2026.4.0 DX/QoL improvements (Phase 0-1) |
| `6a9cc57` | 2026-03-14 | Sean Chatman | feat(validation): environment-validation session results |
| `6a3ab6f` | 2026-03-14 | Claude | chore: eradicate all remaining Java 25 references (80/20 gaps) |
| `2155e33` | 2026-03-14 | Claude | Close 80/20 gaps: LICENSE, String.formatted, license plugin, TODO |
| `e6d8943` | 2026-03-14 | Claude | feat: TPS enforcement — fail build on missing Javadoc, generate docs/api/ |
| `45f1390` | 2026-03-14 | Claude | feat: add dtr-javadoc Rust extraction tool and sayJavadoc API |
| `4fd505d` | 2026-03-14 | Claude | refactor: strip HTTP methods from DtrTest, DtrContext, DtrExtension, MultiRenderMachine, RenderMachineImpl |
| `6279901` | 2026-03-14 | Claude | feat: add sayContractVerification and sayEvolutionTimeline + fix MultiRenderMachine |
| `dd1c236` | 2026-03-14 | Claude | feat: DTR v2.6.0 Blue Ocean 80/20 innovation — 13 new say* methods |
| `f8aa8d6` | 2026-03-12 | Claude | fix: close remaining audit gaps for Fortune 500 readiness |

*10 most recent commits touching `RenderMachineImpl.java`*

## C5: sayAsciiChart() — Inline ASCII Bar Chart

Renders a horizontal ASCII bar chart using Unicode block characters (`████`). No external dependencies — pure Java string math. Bars are normalized to the maximum value. Ideal for displaying benchmark p-values or coverage percentages.

```java
sayAsciiChart("Response Time (ms)",
    new double[]{12, 38, 47, 52},
    new String[]{"p50","p95","p99","max"});
```

### Chart: Response Time (ms)

```
p50    █████░░░░░░░░░░░░░░░  12
p95    ███████████████░░░░░  38
p99    ██████████████████░░  47
max    ████████████████████  52
```

Benchmark results from b1 rendered as a chart:

### Benchmark: Chart demonstration

| Metric | Result |
| --- | --- |
| Avg | `90 ns` |
| Min | `41 ns` |
| Max | `416 ns` |
| p99 | `416 ns` |
| Ops/sec | `11,111,111` |
| Warmup rounds | `20` |
| Measure rounds | `100` |
| Java | `26` |

## C7: saySecurityManager() — Java Security Environment

Documents the complete Java security environment — security manager presence, installed security providers, available cryptographic algorithms, and SecureRandom implementation details. Essential for security-sensitive code documentation and FIPS compliance verification.

**Example usage:**

```java
// One-liner to document security environment
saySecurityManager();

// Renders:
// 1. Security Manager Status (present/absent)
// 2. Security Providers (name, version, info)
// 3. Available Cryptographic Algorithms
// 4. SecureRandom implementation details
```

**Current JVM security landscape:**

### Security Manager

| Property | Status |
| --- | --- |
| Security Manager | `ABSENT` |
| Class | — |

### Security Providers

| Provider | Version | Info |
| --- | --- | --- |
| `SUN` | `26.0` | SUN (DSA key/parameter generation; DSA signing; SHA-1, MD5 digests; SecureRandom; X.509 certificates; PKCS12, JKS & DKS keystores; PKIX CertPathValidator; PKIX CertPathBuilder; LDAP, Collection CertStores, JavaPolicy Policy; JavaLoginConfig Configuration) |
| `SunRsaSign` | `26.0` | Sun RSA signature provider |
| `SunEC` | `26.0` | Sun Elliptic Curve provider |
| `SunJSSE` | `26.0` | Sun JSSE provider(PKCS12, SunX509/PKIX key/trust factories, SSLv3/TLSv1/TLSv1.1/TLSv1.2/TLSv1.3/DTLSv1.0/DTLSv1.2) |
| `SunJCE` | `26.0` | SunJCE Provider (implements RSA, DES, Triple DES, AES, Blowfish, ARCFOUR, RC2, PBE, Diffie-Hellman, HMAC, ChaCha20, DHKEM, ML-KEM, and HKDF) |
| `SunJGSS` | `26.0` | Sun (Kerberos v5, SPNEGO) |
| `SunSASL` | `26.0` | Sun SASL provider(implements client mechanisms for: DIGEST-MD5, EXTERNAL, PLAIN, CRAM-MD5, NTLM; server mechanisms for: DIGEST-MD5, CRAM-MD5, NTLM) |
| `XMLDSig` | `26.0` | XMLDSig (DOM XMLSignatureFactory; DOM KeyInfoFactory; C14N 1.0, C14N 1.1, Exclusive C14N, Base64, Enveloped, XPath, XPath2, XSLT TransformServices) |
| `SunPCSC` | `26.0` | Sun PC/SC provider |
| `JdkLDAP` | `26.0` | JdkLDAP Provider (implements LDAP CertStore) |
| `JdkSASL` | `26.0` | JDK SASL provider(implements client and server mechanisms for GSSAPI) |
| `Apple` | `26.0` | Apple Provider |
| `SunPKCS11` | `26.0` | Unconfigured and unusable PKCS11 provider |

### Available Cryptographic Algorithms

**KeyPairGenerator:**
`RSA`, `X25519`, `DIFFIEHELLMAN`, `ML-DSA-65`, `ML-DSA-87`, `X448`, `ML-DSA-44`, `ED25519`, `ML-KEM`, `ML-DSA`, `ML-KEM-512`, `ML-KEM-768`, `ML-KEM-1024`, `DSA`, `ED448`, `RSASSA-PSS`, `XDH`, `EC`, `EDDSA`

**Cipher:**
`AES_192/GCM/NOPADDING`, `AES_256/CBC/NOPADDING`, `AES_128/KW/NOPADDING`, `AES_128/CBC/NOPADDING`, `AES_256/KW/NOPADDING`, `PBEWITHMD5ANDDES`, `PBEWITHHMACSHA256ANDAES_256`, `PBEWITHHMACSHA512/256ANDAES_256`, `PBEWITHSHA1ANDRC4_128`, `AES_192/OFB/NOPADDING`, `DESEDEWRAP`, `RC2`, `PBEWITHSHA1ANDRC4_40`, `RSA`, `AES_192/CFB/NOPADDING`, `AES_192/KW/PKCS5PADDING`, `AES_256/KWP/NOPADDING`, `AES_128/CFB/NOPADDING`, `DESEDE`, `BLOWFISH` ... (39 more)

**MessageDigest:**
`SHA3-512`, `SHA-1`, `SHA-384`, `SHAKE128-256`, `SHAKE256-512`, `SHA3-384`, `SHA-224`, `SHA-512/256`, `SHA-256`, `MD2`, `SHA-512/224`, `SHA3-256`, `SHA-512`, `SHA3-224`, `MD5`

### SecureRandom

| Property | Value |
| --- | --- |
| Strong Algorithm | `NativePRNGBlocking` |
| Provider | `SUN` |
| Default Algorithm | `NativePRNG` |
| Provider | `SUN` |

> [!NOTE]
> Security providers vary by JVM vendor and version. Common providers include SUN, SunRsaSign, SunJCE, SunJSSE, and SunPKCS11. The algorithm list shows what crypto operations are available without external libraries.

**Use cases:**

- Documenting FIPS 140-2 compliance crypto providers
- Verifying security manager is installed in sandboxed environments
- Checking available algorithms for encryption/hashing operations
- Auditing JVM security configuration for production deployments

## C8: sayThreadDump() — JVM Thread State

Documents the current JVM thread state with aggregate metrics and per-thread details. Uses {@link java.lang.management.ManagementFactory#getThreadMXBean()} to introspect the JVM's thread state without external tools. Invaluable for concurrency behavior documentation and thread pool sizing decisions.

**Example usage:**

```java
// One-liner to document thread state
sayThreadDump();

// Renders:
// 1. Thread Summary (thread count, daemon count, peak count, total started)
// 2. Thread Details (ID, name, state, alive, interrupted for each thread)
```

**Current JVM thread state:**

### Thread Summary

| Metric | Value |
| --- | --- |
| Thread Count | `17` |
| Daemon Thread Count | `16` |
| Peak Thread Count | `17` |
| Total Started Thread Count | `17` |

### Thread Details

| Thread ID | Name | State | Alive | Interrupted |
| --- | --- | --- | --- | --- |
| `3` | `main` | `RUNNABLE` | `true` | `N/A` |
| `15` | `Reference Handler` | `RUNNABLE` | `true` | `N/A` |
| `16` | `Finalizer` | `WAITING` | `true` | `N/A` |
| `17` | `Signal Dispatcher` | `RUNNABLE` | `true` | `N/A` |
| `32` | `Notification Thread` | `RUNNABLE` | `true` | `N/A` |
| `33` | `Common-Cleaner` | `TIMED_WAITING` | `true` | `N/A` |
| `34` | `surefire-forkedjvm-stream-flusher` | `TIMED_WAITING` | `true` | `N/A` |
| `36` | `surefire-forkedjvm-command-thread` | `RUNNABLE` | `true` | `N/A` |
| `50` | `VirtualThread-unblocker` | `RUNNABLE` | `true` | `N/A` |
| `52` | `ForkJoinPool-1-worker-1` | `WAITING` | `true` | `N/A` |
| `54` | `ForkJoinPool-1-worker-2` | `WAITING` | `true` | `N/A` |
| `57` | `ForkJoinPool-1-worker-4` | `WAITING` | `true` | `N/A` |
| `56` | `ForkJoinPool-1-worker-3` | `WAITING` | `true` | `N/A` |
| `58` | `ForkJoinPool-1-worker-5` | `TIMED_WAITING` | `true` | `N/A` |
| `60` | `ForkJoinPool-1-worker-6` | `WAITING` | `true` | `N/A` |
| `61` | `ForkJoinPool-1-worker-7` | `WAITING` | `true` | `N/A` |
| `66` | `process reaper` | `TIMED_WAITING` | `true` | `N/A` |

*17 live threads*

> [!NOTE]
> On Java 21+, virtual threads appear alongside platform threads. Thread states include NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, and TERMINATED. The peak thread count shows the maximum concurrent threads since JVM start.

**Use cases:**

- Documenting thread pool sizing decisions (e.g., ForkJoinPool.commonPool)
- Debugging deadlocks and thread starvation issues
- Verifying virtual thread usage on Java 21+
- Auditing thread leaks in long-running applications
- Showing concurrency behavior in parallel stream documentation

---
*Generated by [DTR](http://www.dtr.org)*
