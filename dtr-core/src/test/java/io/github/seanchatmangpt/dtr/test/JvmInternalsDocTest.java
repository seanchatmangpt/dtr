/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.is;

/**
 * 80/20 Blue Ocean Innovation: JVM Internals and Runtime Profile Documentation.
 *
 * <p>Documents the live JVM environment from five complementary angles that no
 * other testing library exposes as first-class documentation artifacts:</p>
 *
 * <ol>
 *   <li><strong>a1_environment</strong> — complete runtime environment snapshot via {@code sayEnvProfile()}</li>
 *   <li><strong>a2_memory_profile</strong> — heap statistics with ASCII bar chart visualization</li>
 *   <li><strong>a3_thread_snapshot</strong> — thread dump and assertion on live thread count</li>
 *   <li><strong>a4_system_properties</strong> — filtered {@code java.*} system properties</li>
 *   <li><strong>a5_module_system</strong> — JPMS module dependency graph for DTR classes</li>
 * </ol>
 *
 * <p>All measurements are real ({@code Runtime.getRuntime()} and
 * {@code Thread.getAllStackTraces()}). No hardcoded values. No stubs.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class JvmInternalsDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // a1: Runtime Environment Snapshot
    // =========================================================================

    @Test
    void a1_environment() {
        sayNextSection("JVM Runtime Environment");

        say("A reproducible record of the exact runtime environment under which these " +
                "tests were executed. Every benchmark, assertion, and measurement in this " +
                "document is only meaningful in the context of the environment below. " +
                "DTR captures this automatically via `sayEnvProfile()` — one call, zero parameters.");

        sayEnvProfile();

        say("Additional JVM identity properties captured directly from `System.getProperty()`:");

        var jvmProperties = new LinkedHashMap<String, String>();
        jvmProperties.put("java.version",         System.getProperty("java.version", "(unavailable)"));
        jvmProperties.put("java.vm.name",          System.getProperty("java.vm.name", "(unavailable)"));
        jvmProperties.put("java.vm.version",       System.getProperty("java.vm.version", "(unavailable)"));
        jvmProperties.put("java.vm.vendor",        System.getProperty("java.vm.vendor", "(unavailable)"));
        jvmProperties.put("java.class.version",    System.getProperty("java.class.version", "(unavailable)"));
        jvmProperties.put("java.home",             System.getProperty("java.home", "(unavailable)"));
        jvmProperties.put("sun.arch.data.model",   System.getProperty("sun.arch.data.model", "(unavailable)"));
        jvmProperties.put("java.vm.info",          System.getProperty("java.vm.info", "(unavailable)"));

        sayKeyValue(jvmProperties);

        sayNote("These properties come from the running JVM process at test execution time. " +
                "They are embedded in the generated document so benchmark results remain " +
                "reproducible and attributable to a specific JVM build.");
    }

    // =========================================================================
    // a2: Heap Memory Profile
    // =========================================================================

    @Test
    void a2_memory_profile() {
        sayNextSection("JVM Heap Memory Profile");

        say("Heap memory statistics sampled from `Runtime.getRuntime()` at the moment " +
                "this test method executes. Values reflect the post-GC heap state observed " +
                "by the JVM — not theoretical maximums from JVM startup flags.");

        sayCode("""
                // Sampling heap statistics from the live JVM
                Runtime rt        = Runtime.getRuntime();
                long totalBytes   = rt.totalMemory();   // committed heap
                long maxBytes     = rt.maxMemory();     // -Xmx ceiling
                long freeBytes    = rt.freeMemory();    // uncommitted within committed
                long usedBytes    = totalBytes - freeBytes;
                """, "java");

        Runtime rt      = Runtime.getRuntime();
        long totalBytes = rt.totalMemory();
        long maxBytes   = rt.maxMemory();
        long freeBytes  = rt.freeMemory();
        long usedBytes  = totalBytes - freeBytes;

        long totalMb = totalBytes / (1024L * 1024L);
        long maxMb   = maxBytes   / (1024L * 1024L);
        long freeMb  = freeBytes  / (1024L * 1024L);
        long usedMb  = usedBytes  / (1024L * 1024L);

        var heapStats = new LinkedHashMap<String, String>();
        heapStats.put("Max Heap (Xmx ceiling)",   maxMb   + " MB");
        heapStats.put("Total Heap (committed)",   totalMb + " MB");
        heapStats.put("Used Heap (total - free)", usedMb  + " MB");
        heapStats.put("Free Heap (uncommitted)",  freeMb  + " MB");

        sayKeyValue(heapStats);

        say("Heap utilization visualized as an ASCII bar chart. Values are in MB, " +
                "normalized to the maximum observed value:");

        sayAsciiChart(
                "Heap Memory (MB)",
                new double[]{(double) freeMb, (double) usedMb},
                new String[]{"Free", "Used"}
        );

        sayWarning("Heap values are point-in-time measurements. GC events between " +
                "the `rt.totalMemory()` and `rt.freeMemory()` calls can produce " +
                "apparent inconsistencies. For stable profiling, use Java Flight " +
                "Recorder or `java.lang.management.MemoryMXBean` with `gc()` before sampling.");

        sayNote("On Java 26 with ZGC or Shenandoah, the committed heap (`totalMemory`) " +
                "grows and shrinks dynamically. The max heap ceiling (`maxMemory`) remains " +
                "fixed at the value passed to `-Xmx` (or the OS-derived default).");
    }

    // =========================================================================
    // a3: Thread Snapshot
    // =========================================================================

    @Test
    void a3_thread_snapshot() {
        sayNextSection("JVM Thread Snapshot");

        say("A live thread snapshot documents the concurrency model active at the moment " +
                "of test execution. On Java 21+, this includes virtual threads alongside " +
                "platform threads. The `sayThreadDump()` call below captures all thread " +
                "states from `java.lang.management.ThreadMXBean`.");

        say("**Full thread dump (all JVM threads at test execution time):**");
        sayThreadDump();

        say("Thread count verified with a Hamcrest assertion against " +
                "`Thread.getAllStackTraces().size()`. This API returns all live platform " +
                "threads visible to the current thread group:");

        sayCode("""
                // Thread.getAllStackTraces() returns a Map<Thread, StackTraceElement[]>
                // for every live platform thread at call time.
                int threadCount = Thread.getAllStackTraces().size();
                sayAndAssertThat("Live platform thread count > 0", threadCount > 0, is(true));
                """, "java");

        int threadCount = Thread.getAllStackTraces().size();
        sayAndAssertThat("Live platform thread count > 0", threadCount > 0, is(true));

        say("Thread count observed during this test run: **" + threadCount + "** platform threads. " +
                "This includes the JUnit runner thread, the main thread, GC threads, and any " +
                "background threads started by the JVM or test framework.");

        sayNote("Virtual threads created with `Thread.ofVirtual()` or " +
                "`Executors.newVirtualThreadPerTaskExecutor()` are not returned by " +
                "`Thread.getAllStackTraces()` — they are not pinned to platform threads " +
                "and are not tracked by that legacy API. Use `ThreadMXBean.dumpAllThreads()` " +
                "for a comprehensive view that includes virtual thread carriers.");
    }

    // =========================================================================
    // a4: System Properties (java.* filter)
    // =========================================================================

    @Test
    void a4_system_properties() {
        sayNextSection("JVM System Properties: java.* Namespace");

        say("System properties in the `java.*` namespace form the canonical identity " +
                "contract of a running JVM. They are set at JVM startup, are read-only " +
                "in practice, and determine compatibility, feature availability, and " +
                "behavioral defaults for every class on the classpath.");

        say("The table below is generated by `saySystemProperties(\"java.*\")` — a " +
                "regex-filtered view of `System.getProperties()` that shows only the " +
                "`java.*` keys, sorted alphabetically:");

        saySystemProperties("java\\..*");

        sayNote("Key properties for Java 26 preview feature activation: " +
                "`java.version` must be 26 or higher, and `java.vm.info` will include " +
                "the string `mixed mode` with `--enable-preview` active. " +
                "Preview features such as Code Reflection (JEP 516) and Value Objects " +
                "(JEP 401) are gated on both the compiler flag `--enable-preview` at " +
                "compile time and the JVM flag `--enable-preview` at runtime. " +
                "Without both flags, preview APIs throw `UnsupportedOperationException` " +
                "rather than silently degrading.");

        sayWarning("Never use `System.getProperties()` to pass mutable state between " +
                "test methods. System properties are global, untyped, and not thread-safe " +
                "under concurrent modification. For test configuration, use `@SystemProperty` " +
                "(JUnit Pioneer) or environment variables injected by the CI runner.");
    }

    // =========================================================================
    // a5: Module System
    // =========================================================================

    @Test
    void a5_module_system() {
        sayNextSection("JPMS Module System: DTR Module Dependencies");

        say("The Java Platform Module System (JPMS), introduced in Java 9 (Project Jigsaw), " +
                "defines strict encapsulation boundaries for every JAR on the module path. " +
                "Understanding which modules DTR depends on — and which modules the JDK " +
                "provides — is essential for embedding DTR in restricted environments such " +
                "as GraalVM native images, OSGi containers, or custom JVM runtime images " +
                "built with `jlink`.");

        say("`sayModuleDependencies()` uses `Class.getModule()` and `Module.getDescriptor()` " +
                "to introspect live JPMS module metadata at runtime. No static analysis, " +
                "no classpath scanning — the module graph is read from the running JVM:");

        sayModuleDependencies(DtrTest.class, String.class);

        say("The canonical `module-info.java` pattern for a DTR-based documentation module:");

        sayCode("""
                // module-info.java — DTR documentation module pattern (Java 9+)
                module com.example.docs {

                    // DTR core — provides DtrTest, DtrContext, RenderMachine
                    requires io.github.seanchatmangpt.dtr;

                    // JUnit 5 — test lifecycle and extension model
                    requires org.junit.jupiter.api;

                    // Hamcrest — sayAndAssertThat matchers
                    requires org.hamcrest;

                    // Open test packages to JUnit engine for reflection-based discovery
                    opens com.example.docs.test to org.junit.platform.commons;
                }
                """, "java");

        sayNote("On Java 9+, unnamed modules (JARs on the classpath rather than the " +
                "module path) have unrestricted access to `java.base` and each other, " +
                "but cannot `require` named modules. DTR test classes typically run in " +
                "the unnamed module during `mvn test` because most projects use the " +
                "classpath, not the module path. The `sayModuleDependencies()` output " +
                "above reflects this: classes in the unnamed module report their module " +
                "as the unnamed module placeholder.");
    }
}
