package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.reflectiontoolkit.CallSiteRecord;
import io.github.seanchatmangpt.dtr.util.BlueOceanLayer;
import io.github.seanchatmangpt.dtr.util.Vision2030Utils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * DTR documentation test for Vision 2030 utilities and the Blue Ocean abstraction layer.
 *
 * <p>Demonstrates two new classes:</p>
 * <ul>
 *   <li>{@link Vision2030Utils} — static helpers for system fingerprints, benchmark
 *       comparisons, and class metadata</li>
 *   <li>{@link BlueOceanLayer} — composite "documentation profiles" that orchestrate
 *       multiple say* calls into higher-level operations</li>
 * </ul>
 *
 * <p>All measurements are real. All output is generated from live class structure.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class Vision2030UtilsTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Vision2030Utils — Static Helpers
    // =========================================================================

    @Test
    void a1_systemFingerprint_returns_environment_metadata() {
        sayNextSection("Vision2030Utils.systemFingerprint()");
        say("Returns a reproducibility fingerprint of the current runtime. "
                + "Useful as metadata footer for any benchmark or test section.");

        sayCode("""
                Map<String, String> fingerprint = Vision2030Utils.systemFingerprint();
                sayKeyValue(fingerprint);
                """, "java");

        var fingerprint = Vision2030Utils.systemFingerprint();
        sayKeyValue(fingerprint);

        sayAndAssertThat("Fingerprint has Java Version",
                fingerprint.containsKey("Java Version"), is(true));
        sayAndAssertThat("Fingerprint has Processors",
                fingerprint.containsKey("Processors"), is(true));
        sayAndAssertThat("Fingerprint size", fingerprint.size(), greaterThanOrEqualTo(6));
    }

    @Test
    void a2_benchmarkComparison_side_by_side_table() {
        sayNextSection("Vision2030Utils.benchmarkComparison()");
        say("Runs two benchmarks and returns a side-by-side comparison table. "
                + "All numbers are real `System.nanoTime()` measurements.");

        sayCode("""
                String[][] comparison = Vision2030Utils.benchmarkComparison(
                    "HashMap.get()", () -> Map.of("k", 1).get("k"),
                    "String.valueOf()", () -> String.valueOf(42));
                sayTable(comparison);
                """, "java");

        var comparison = Vision2030Utils.benchmarkComparison(
                "HashMap.get()", () -> Map.of("k", 1).get("k"),
                "String.valueOf()", () -> String.valueOf(42),
                20, 200);
        sayTable(comparison);

        sayAndAssertThat("Comparison has header row", comparison[0][0], equalTo("Metric"));
        sayAndAssertThat("Comparison has 6 data rows", comparison.length, equalTo(6));
    }

    @Test
    void a3_classMetadata_returns_class_info() {
        sayNextSection("Vision2030Utils.classMetadata()");
        say("Returns structured metadata about any class — name, package, module, "
                + "sealed/record/interface status, and public method count.");

        var meta = Vision2030Utils.classMetadata(RenderMachineCommands.class);
        sayKeyValue(meta);

        sayAndAssertThat("Is interface", meta.get("Interface"), equalTo("true"));
        sayAndAssertThat("Has public methods",
                Integer.parseInt(meta.get("Public Methods")), greaterThan(0));
    }

    // =========================================================================
    // BlueOceanLayer — Composite Documentation Profiles
    // =========================================================================

    @Test
    void b1_documentClassProfile_composite() {
        sayNextSection("BlueOceanLayer.documentClassProfile()");
        say("One call produces a complete class profile: metadata, hierarchy, "
                + "code model, annotation profile, and documentation coverage.");

        sayCode("""
                BlueOceanLayer.documentClassProfile(this, CallSiteRecord.class);
                """, "java");

        BlueOceanLayer.documentClassProfile(this, CallSiteRecord.class);
    }

    @Test
    void b2_documentPerformanceProfile_composite() {
        sayNextSection("BlueOceanLayer.documentPerformanceProfile()");
        say("Benchmarks multiple tasks, renders individual results plus a head-to-head "
                + "comparison table, and appends the environment fingerprint.");

        BlueOceanLayer.documentPerformanceProfile(this,
                "Map Lookup vs String Conversion",
                new String[]{"HashMap.get()", "String.valueOf()"},
                new Runnable[]{
                        () -> Map.of("key", 42).get("key"),
                        () -> String.valueOf(42)
                });
    }

    @Test
    void b3_documentRecordProfile_composite() {
        sayNextSection("BlueOceanLayer.documentRecordProfile()");
        say("Documents a Java record's schema alongside an example instance as JSON.");

        var example = new CallSiteRecord("Vision2030UtilsTest", "b3_documentRecordProfile", 130);
        BlueOceanLayer.documentRecordProfile(this, CallSiteRecord.class, example);
    }

    @Test
    void b4_documentContractCompliance_composite() {
        sayNextSection("BlueOceanLayer.documentContractCompliance()");
        say("Verifies interface contract compliance: verification matrix, class diagram, "
                + "and git evolution timeline — in one call.");

        BlueOceanLayer.documentContractCompliance(this,
                RenderMachineCommands.class,
                RenderMachineImpl.class);
    }

    @Test
    void b5_documentErrorPattern_composite() {
        sayNextSection("BlueOceanLayer.documentErrorPattern()");
        say("Documents error handling patterns with exception chain and root cause warning.");

        var root = new NullPointerException("config was null");
        var wrapped = new IllegalStateException("Failed to initialize Vision 2030 module", root);
        BlueOceanLayer.documentErrorPattern(this, wrapped);
    }

    @Test
    void b6_documentSystemLandscape_composite() {
        sayNextSection("BlueOceanLayer.documentSystemLandscape()");
        say("Full system landscape: env profile, OS metrics, thread state, "
                + "security providers, and filtered system properties.");

        BlueOceanLayer.documentSystemLandscape(this);
    }
}
