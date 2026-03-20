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
import io.github.seanchatmangpt.dtr.reflectiontoolkit.CallSiteRecord;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.util.BlueOceanLayer;
import io.github.seanchatmangpt.dtr.util.Vision2030Utils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.*;

/**
 * Vision 2030 Showcase: the "hero" test that proves the whole DTR system works together.
 *
 * <p>Demonstrates every major capability of {@link Vision2030Utils} and {@link BlueOceanLayer}
 * in a single, end-to-end documentation test. Each section solves a real-world "job to be done":</p>
 * <ol>
 *   <li>Getting Started with Vision 2030 utilities</li>
 *   <li>System audit via BlueOceanLayer</li>
 *   <li>Performance showdown across Map implementations</li>
 *   <li>API contract review for RenderMachineCommands</li>
 *   <li>Record schema documentation for CallSiteRecord</li>
 *   <li>Error handling patterns with realistic exception chains</li>
 *   <li>Class deep dive into RenderMachineImpl</li>
 *   <li>TL;DR summary with social-ready callouts</li>
 * </ol>
 *
 * <p>All measurements are real ({@code System.nanoTime()}), all reflection is live,
 * all diagrams are generated from actual class structure. No simulation, no hard-coded numbers.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class Vision2030ShowcaseTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // a1: Getting Started
    // =========================================================================

    @Test
    void a1_getting_started() {
        sayNextSection("Getting Started with Vision 2030");

        say("Vision 2030 introduces two complementary layers on top of DTR's core say* API:");
        sayUnorderedList(List.of(
                "`Vision2030Utils` -- static helpers for system fingerprints, benchmark comparisons, and class metadata",
                "`BlueOceanLayer` -- composite documentation profiles that orchestrate multiple say* calls into higher-level operations"
        ));

        say("The simplest way to start is with a system fingerprint, which captures "
                + "the runtime environment for reproducibility:");

        sayCode("""
                // Capture environment metadata in one call
                Map<String, String> fingerprint = Vision2030Utils.systemFingerprint();
                sayKeyValue(fingerprint);

                // Or get structured class metadata
                Map<String, String> meta = Vision2030Utils.classMetadata(MyClass.class);
                sayKeyValue(meta);

                // BlueOceanLayer composes multiple say* calls into a single profile
                BlueOceanLayer.documentClassProfile(this, MyClass.class);
                """, "java");

        var fingerprint = Vision2030Utils.systemFingerprint();
        sayKeyValue(fingerprint);

        sayAndAssertThat("Fingerprint contains Java Version",
                fingerprint.containsKey("Java Version"), is(true));
        sayAndAssertThat("Fingerprint contains Processors",
                fingerprint.containsKey("Processors"), is(true));
        sayAndAssertThat("Fingerprint has at least 6 entries",
                fingerprint.size(), greaterThanOrEqualTo(6));

        sayNote("Vision2030Utils and BlueOceanLayer accept any `RenderMachineCommands` "
                + "implementation, so they work with both `DtrTest` (extends) and "
                + "`DtrContext` (injected by DtrExtension).");
    }

    // =========================================================================
    // a2: System Audit
    // =========================================================================

    @Test
    void a2_system_audit() {
        sayNextSection("System Audit");

        say("A full system landscape captures the runtime environment, OS metrics, "
                + "thread state, security providers, and filtered system properties -- "
                + "all in a single call. This is the reproducibility footer every "
                + "benchmark report needs.");

        sayCode("""
                BlueOceanLayer.documentSystemLandscape(this);
                """, "java");

        BlueOceanLayer.documentSystemLandscape(this);

        var fingerprint = Vision2030Utils.systemFingerprint();
        sayAndAssertThat("Java version is present",
                fingerprint.get("Java Version"), is(notNullValue()));
    }

    // =========================================================================
    // a3: Performance Showdown
    // =========================================================================

    @Test
    void a3_performance_showdown() {
        sayNextSection("Performance Showdown: Map Implementations");

        say("A head-to-head benchmark comparison of three JDK `Map` implementations "
                + "using `Vision2030Utils.benchmarkComparison()`. All numbers are real "
                + "`System.nanoTime()` measurements on Java "
                + System.getProperty("java.version") + ".");

        // Prepare maps with real data
        var hashMap = new HashMap<String, Integer>();
        var treeMap = new TreeMap<String, Integer>();
        var concurrentMap = new ConcurrentHashMap<String, Integer>();
        for (int i = 0; i < 1000; i++) {
            String key = "key-" + i;
            hashMap.put(key, i);
            treeMap.put(key, i);
            concurrentMap.put(key, i);
        }

        say("Each map is pre-populated with 1,000 entries. We measure `.get(\"key-500\")` "
                + "for a key in the middle of the keyspace.");

        say("**HashMap vs TreeMap** -- O(1) amortized vs O(log n) lookup:");

        var hashVsTree = Vision2030Utils.benchmarkComparison(
                "HashMap.get()", () -> hashMap.get("key-500"),
                "TreeMap.get()", () -> treeMap.get("key-500"),
                30, 300);
        sayTable(hashVsTree);

        sayAndAssertThat("Comparison table has header row",
                hashVsTree[0][0], equalTo("Metric"));
        sayAndAssertThat("Comparison table has 6 data rows",
                hashVsTree.length, equalTo(6));

        say("**HashMap vs ConcurrentHashMap** -- unsynchronized vs thread-safe:");

        var hashVsConcurrent = Vision2030Utils.benchmarkComparison(
                "HashMap.get()", () -> hashMap.get("key-500"),
                "ConcurrentHashMap.get()", () -> concurrentMap.get("key-500"),
                30, 300);
        sayTable(hashVsConcurrent);

        say("**Visual comparison** -- average lookup times:");

        long hashAvg = Long.parseLong(hashVsTree[1][1]);
        long treeAvg = Long.parseLong(hashVsTree[1][2]);
        long concurrentAvg = Long.parseLong(hashVsConcurrent[1][2]);

        sayAsciiChart("Average Lookup Time (ns)",
                new double[]{hashAvg, treeAvg, concurrentAvg},
                new String[]{"HashMap", "TreeMap", "ConcurrentHashMap"});

        sayNote("These are single-threaded benchmarks. ConcurrentHashMap's real advantage "
                + "emerges under concurrent access where HashMap is unsafe.");
    }

    // =========================================================================
    // a4: API Contract Review
    // =========================================================================

    @Test
    void a4_api_contract_review() {
        sayNextSection("API Contract Review: RenderMachineCommands");

        say("The `RenderMachineCommands` interface is the core contract for DTR's "
                + "documentation output. `BlueOceanLayer.documentContractCompliance()` "
                + "verifies that `RenderMachineImpl` implements every method in the "
                + "contract, generates a class diagram, and shows the git evolution "
                + "timeline -- all in one call.");

        sayCode("""
                BlueOceanLayer.documentContractCompliance(this,
                        RenderMachineCommands.class,
                        RenderMachineImpl.class);
                """, "java");

        BlueOceanLayer.documentContractCompliance(this,
                RenderMachineCommands.class,
                RenderMachineImpl.class);

        var meta = Vision2030Utils.classMetadata(RenderMachineCommands.class);
        sayAndAssertThat("RenderMachineCommands is an interface",
                meta.get("Interface"), equalTo("true"));
        sayAndAssertThat("RenderMachineCommands has many public methods",
                Integer.parseInt(meta.get("Public Methods")), greaterThan(20));
    }

    // =========================================================================
    // a5: Record Schema Documentation
    // =========================================================================

    @Test
    void a5_record_schema_documentation() {
        sayNextSection("Record Schema Documentation");

        say("Java records are the natural way to model structured data in DTR tests. "
                + "`BlueOceanLayer.documentRecordProfile()` renders the record's component "
                + "schema alongside a live example serialized as JSON.");

        sayCode("""
                // CallSiteRecord captures provenance: where was this call made?
                var example = new CallSiteRecord(
                        "Vision2030ShowcaseTest", "a5_record_schema_documentation", 185);
                BlueOceanLayer.documentRecordProfile(this, CallSiteRecord.class, example);
                """, "java");

        var example = new CallSiteRecord(
                "Vision2030ShowcaseTest", "a5_record_schema_documentation", 185);
        BlueOceanLayer.documentRecordProfile(this, CallSiteRecord.class, example);

        sayAndAssertThat("CallSiteRecord is a record",
                CallSiteRecord.class.isRecord(), is(true));
        sayAndAssertThat("CallSiteRecord has 3 components",
                CallSiteRecord.class.getRecordComponents().length, equalTo(3));
        sayAndAssertThat("Example className matches",
                example.className(), equalTo("Vision2030ShowcaseTest"));

        sayNote("Record schemas update automatically when the record class changes. "
                + "No manual synchronization required.");
    }

    // =========================================================================
    // a6: Error Handling Patterns
    // =========================================================================

    @Test
    void a6_error_handling_patterns() {
        sayNextSection("Error Handling Patterns");

        say("Documenting error handling is as important as documenting the happy path. "
                + "`BlueOceanLayer.documentErrorPattern()` renders exception chains with "
                + "full cause unwinding and a root-cause warning.");

        say("Consider a typical layered failure: a configuration file is missing, "
                + "which causes initialization to fail, which causes the service to "
                + "refuse requests:");

        // Build a realistic 3-level exception chain
        var rootCause = new java.io.FileNotFoundException("/etc/app/config.yaml");
        var initFailure = new IllegalStateException(
                "Failed to initialize configuration subsystem", rootCause);
        var serviceError = new RuntimeException(
                "Service unavailable: initialization incomplete", initFailure);

        sayCode("""
                var rootCause = new FileNotFoundException("/etc/app/config.yaml");
                var initFailure = new IllegalStateException(
                        "Failed to initialize configuration subsystem", rootCause);
                var serviceError = new RuntimeException(
                        "Service unavailable: initialization incomplete", initFailure);
                BlueOceanLayer.documentErrorPattern(this, serviceError);
                """, "java");

        BlueOceanLayer.documentErrorPattern(this, serviceError);

        // Verify the chain structure
        sayAndAssertThat("Service error has a cause",
                serviceError.getCause(), is(notNullValue()));
        sayAndAssertThat("Root cause is FileNotFoundException",
                rootCause.getClass().getSimpleName(),
                equalTo("FileNotFoundException"));

        int depth = 0;
        Throwable t = serviceError;
        while (t != null) {
            depth++;
            t = t.getCause();
        }
        sayAndAssertThat("Exception chain depth", depth, equalTo(3));

        say("**Best practice:** always preserve the cause chain when wrapping exceptions. "
                + "`documentErrorPattern()` automatically unwraps to the root cause and emits "
                + "a warning so readers immediately see the underlying problem.");
    }

    // =========================================================================
    // a7: Class Deep Dive
    // =========================================================================

    @Test
    void a7_class_deep_dive() {
        sayNextSection("Class Deep Dive: RenderMachineImpl");

        say("`BlueOceanLayer.documentClassProfile()` produces a complete class profile "
                + "in one call: metadata, class hierarchy, code model, annotation profile, "
                + "and documentation coverage. This is the go-to method when onboarding "
                + "a new team member or reviewing an unfamiliar class.");

        sayCode("""
                BlueOceanLayer.documentClassProfile(this, RenderMachineImpl.class);
                """, "java");

        BlueOceanLayer.documentClassProfile(this, RenderMachineImpl.class);

        var meta = Vision2030Utils.classMetadata(RenderMachineImpl.class);
        sayAndAssertThat("RenderMachineImpl is not an interface",
                meta.get("Interface"), equalTo("false"));
        sayAndAssertThat("RenderMachineImpl is not a record",
                meta.get("Record"), equalTo("false"));
        sayAndAssertThat("RenderMachineImpl has many public methods",
                Integer.parseInt(meta.get("Public Methods")), greaterThan(20));

        sayNote("The profile is generated entirely from live reflection -- if "
                + "`RenderMachineImpl` gains a new method tomorrow, the documentation "
                + "updates automatically on the next test run.");
    }

    // =========================================================================
    // a8: TL;DR Summary
    // =========================================================================

    @Test
    void a8_tldr_summary() {
        sayNextSection("TL;DR Summary");

        sayTldr("Vision 2030 adds two layers to DTR: Vision2030Utils for static helpers "
                + "(system fingerprints, benchmark comparisons, class metadata) and "
                + "BlueOceanLayer for composite documentation profiles (class profiles, "
                + "performance profiles, contract compliance, error patterns, system "
                + "landscapes). Together they turn a single test class into a "
                + "self-documenting, self-verifying technical report.");

        sayTweetable("DTR Vision 2030: one test class, eight sections, zero manual docs. "
                + "System audits, benchmark showdowns, contract reviews, and error patterns "
                + "-- all generated from live code. #Java26 #DTR #DocumentationTesting");

        say("**What we demonstrated in this showcase:**");
        sayTable(new String[][]{
                {"Section", "Capability", "Key API"},
                {"Getting Started", "Environment fingerprinting", "Vision2030Utils.systemFingerprint()"},
                {"System Audit", "Full runtime landscape", "BlueOceanLayer.documentSystemLandscape()"},
                {"Performance Showdown", "Head-to-head benchmarks", "Vision2030Utils.benchmarkComparison()"},
                {"API Contract Review", "Interface compliance", "BlueOceanLayer.documentContractCompliance()"},
                {"Record Schema", "Record profiling", "BlueOceanLayer.documentRecordProfile()"},
                {"Error Handling", "Exception chain docs", "BlueOceanLayer.documentErrorPattern()"},
                {"Class Deep Dive", "Full class profile", "BlueOceanLayer.documentClassProfile()"},
                {"Summary", "Social-ready output", "sayTldr() + sayTweetable() + sayCallToAction()"}
        });

        sayCallToAction("https://github.com/seanchatmangpt/dtr");

        sayAndAssertThat("Vision2030Utils.systemFingerprint() returns data",
                Vision2030Utils.systemFingerprint().isEmpty(), is(false));
        sayAndAssertThat("Vision2030Utils.classMetadata() works on any class",
                Vision2030Utils.classMetadata(Object.class).containsKey("Class"), is(true));

        sayNote("Every section in this showcase used `sayAndAssertThat()` to verify "
                + "real behavior. If any assertion fails, the test fails -- the "
                + "documentation cannot claim something the code does not prove. "
                + "Generated on Java " + System.getProperty("java.version") + ".");
    }
}
