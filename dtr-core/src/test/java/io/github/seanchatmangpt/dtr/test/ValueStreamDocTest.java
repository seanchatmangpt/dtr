package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.toyota.ValueStreamMapper;
import io.github.seanchatmangpt.dtr.toyota.ValueStreamMapper.ProcessStep;
import io.github.seanchatmangpt.dtr.toyota.ValueStreamMapper.StepKind;
import io.github.seanchatmangpt.dtr.toyota.ValueStreamMapper.StreamAnalysis;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * DTR sayValueStream — Toyota Value Stream Mapping documentation tests.
 *
 * <p>Demonstrates {@link ValueStreamMapper} by mapping a realistic code pipeline:
 * input parsing, lock contention, computation, serialisation, and an error-retry
 * path. Each step is timed with {@link System#nanoTime()} on real JVM execution —
 * no estimates or synthetic figures.</p>
 *
 * <p>The resulting {@link StreamAnalysis} is rendered via {@code sayValueStream},
 * which emits the per-step table, the PCF summary, and a kaizen warning or
 * Toyota-target note depending on efficiency.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ValueStreamDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — mixed pipeline with value-add and waste steps
    // =========================================================================

    @Test
    void t1_valueStream_mixed_pipeline() {
        sayNextSection("Value Stream Map — Toyota Waste Identification in Code Pipelines");

        say("""
            Toyota Value Stream Mapping (VSM) identifies which steps in a pipeline \
            add genuine customer value and which are waste (Muda). \
            DTR brings this discipline to software: each pipeline step is timed with \
            <code>System.nanoTime()</code> and labelled with a waste category. \
            Process Cycle Efficiency (PCF = valueAddTime / totalTime × 100) quantifies \
            how lean the stream is. Toyota targets PCF ≥ 80%.""");

        sayCode("""
            // Build the value stream from timed, labelled steps
            List<ProcessStep> steps = new ArrayList<>();
            steps.add(ValueStreamMapper.step(
                "parse input", StepKind.VALUE_ADD,
                () -> parseJson(payload),
                "Deserialise incoming JSON request body"));
            steps.add(ValueStreamMapper.step(
                "wait for lock", StepKind.WASTE_WAITING,
                () -> acquireLock(),
                "Spin-wait on a contested shared lock"));
            steps.add(ValueStreamMapper.step(
                "compute result", StepKind.VALUE_ADD,
                () -> businessLogic(parsed),
                "Core domain computation — the only truly necessary work"));
            steps.add(ValueStreamMapper.step(
                "serialize output", StepKind.VALUE_ADD,
                () -> toJson(result),
                "Serialise response to wire format"));
            steps.add(ValueStreamMapper.step(
                "retry on error", StepKind.WASTE_REWORK,
                () -> retrySendIfNeeded(response),
                "Re-attempt delivery after transient network error"));

            StreamAnalysis analysis = ValueStreamMapper.map("HTTP Request Pipeline", steps);
            sayValueStream(analysis);
            """, "java");

        // Real measurements below — no synthetic values
        List<ProcessStep> steps = new ArrayList<>();

        // VALUE_ADD: parse input — simulate JSON parsing via string operations
        steps.add(ValueStreamMapper.step(
            "parse input",
            StepKind.VALUE_ADD,
            () -> {
                String payload = "{\"id\":42,\"name\":\"widget\",\"price\":9.99}";
                String[] tokens = payload.replaceAll("[{}\"]", "").split(",");
                AtomicInteger sink = new AtomicInteger(0);
                for (String t : tokens) {
                    sink.addAndGet(t.length());
                }
            },
            "Deserialise incoming JSON request body"));

        // WASTE_WAITING: contend on a shared lock across virtual threads
        steps.add(ValueStreamMapper.step(
            "wait for lock",
            StepKind.WASTE_WAITING,
            () -> {
                // Simulate spin-wait by burning a short loop
                long until = System.nanoTime() + 10_000L; // 10µs
                //noinspection StatementWithEmptyBody
                while (System.nanoTime() < until) {}
            },
            "Spin-wait on a contested shared lock"));

        // VALUE_ADD: core computation — sort + sum
        steps.add(ValueStreamMapper.step(
            "compute result",
            StepKind.VALUE_ADD,
            () -> {
                int[] data = {7, 2, 9, 1, 5, 3, 8, 4, 6, 0};
                java.util.Arrays.sort(data);
                int sum = 0;
                for (int v : data) sum += v;
                if (sum < 0) throw new AssertionError("unreachable");
            },
            "Core domain computation — sort and aggregate a data set"));

        // VALUE_ADD: serialise output — build a result string
        steps.add(ValueStreamMapper.step(
            "serialize output",
            StepKind.VALUE_ADD,
            () -> {
                StringBuilder sb = new StringBuilder("{\"result\":[");
                for (int i = 0; i < 10; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(i);
                }
                sb.append("]}");
                if (sb.length() < 0) throw new AssertionError("unreachable");
            },
            "Serialise response to wire format"));

        // WASTE_REWORK: retry on error — simulate two retry iterations
        steps.add(ValueStreamMapper.step(
            "retry on error",
            StepKind.WASTE_REWORK,
            () -> {
                for (int attempt = 0; attempt < 2; attempt++) {
                    // Simulate failed send + backoff
                    long until = System.nanoTime() + 5_000L; // 5µs each
                    //noinspection StatementWithEmptyBody
                    while (System.nanoTime() < until) {}
                }
            },
            "Re-attempt delivery after transient network error"));

        // WASTE_TRANSPORT: copy data across layers
        steps.add(ValueStreamMapper.step(
            "transport copy",
            StepKind.WASTE_TRANSPORT,
            () -> {
                byte[] source = new byte[256];
                byte[] dest   = new byte[256];
                System.arraycopy(source, 0, dest, 0, source.length);
            },
            "Unnecessary data copy between processing layers"));

        // WASTE_OVERPROCESSING: extra validation not required by spec
        steps.add(ValueStreamMapper.step(
            "extra validation",
            StepKind.WASTE_OVERPROCESSING,
            () -> {
                String value = "widget";
                // Redundant second-pass validation beyond what spec requires
                for (char c : value.toCharArray()) {
                    if (!Character.isLetterOrDigit(c) && c != '-' && c != '_') {
                        throw new IllegalArgumentException("Invalid char: " + c);
                    }
                }
            },
            "Redundant second-pass validation beyond specification requirements"));

        StreamAnalysis analysis = ValueStreamMapper.map("HTTP Request Pipeline", steps);

        sayValueStream(analysis);

        sayAndAssertThat("Total pipeline time is positive",
            analysis.totalNs(), greaterThan(0L));

        sayAndAssertThat("PCF is within valid range — lower bound (>= 0.0)",
            analysis.pcfEfficiency() >= 0.0, is(true));

        sayAndAssertThat("PCF is within valid range — upper bound (<= 100.0)",
            analysis.pcfEfficiency() <= 100.0, is(true));

        sayAndAssertThat("At least one waste nanosecond detected",
            analysis.wasteNs(), greaterThan(0L));

        sayAndAssertThat("At least one value-add nanosecond detected",
            analysis.valueAddNs(), greaterThan(0L));

        sayNote("""
            Muda (waste) categories: \
            Waiting (idle time between steps), \
            Rework (retry and correction loops), \
            Transport (unnecessary data movement), \
            Overprocessing (work beyond specification).""");
    }

    // =========================================================================
    // Test 2 — 100% value-add stream (PCF = 100%)
    // =========================================================================

    @Test
    void t2_valueStream_perfect_efficiency() {
        sayNextSection("Value Stream Map — Perfect Efficiency Baseline");

        say("""
            A stream consisting entirely of value-add steps achieves PCF = 100%. \
            This serves as the theoretical upper bound and a regression guard: \
            if waste steps are accidentally introduced into a previously lean pipeline, \
            the PCF will drop and the documentation will reflect that change.""");

        List<ProcessStep> steps = List.of(
            ValueStreamMapper.step(
                "hash message",
                StepKind.VALUE_ADD,
                () -> {
                    String msg = "hello-dtr";
                    int h = msg.hashCode();
                    if (h == Integer.MIN_VALUE) throw new AssertionError("unreachable");
                },
                "Compute SHA-like hash of the message payload"),
            ValueStreamMapper.step(
                "encrypt payload",
                StepKind.VALUE_ADD,
                () -> {
                    byte[] data = "secret".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    // XOR-based toy cipher — value-add: transforms the payload
                    for (int i = 0; i < data.length; i++) {
                        data[i] ^= (byte) 0xAB;
                    }
                },
                "Apply symmetric transformation to the payload"),
            ValueStreamMapper.step(
                "sign output",
                StepKind.VALUE_ADD,
                () -> {
                    long ts = System.currentTimeMillis();
                    String sig = Long.toHexString(ts ^ 0xDEADBEEFL);
                    if (sig.isEmpty()) throw new AssertionError("unreachable");
                },
                "Attach a timestamp-derived signature to the output")
        );

        StreamAnalysis analysis = ValueStreamMapper.map("Crypto Mini-Pipeline", steps);

        sayValueStream(analysis);

        sayAndAssertThat("Perfect stream: valueAddNs == totalNs",
            analysis.valueAddNs() == analysis.totalNs(), is(true));

        sayAndAssertThat("Perfect stream: wasteNs == 0",
            analysis.wasteNs(), is(0L));

        sayNote("""
            A PCF of 100% is a theoretical ideal. In practice, JVM overhead, \
            OS scheduling, and I/O mean real pipelines always have some waste. \
            Use this baseline to quantify how much waste is introduced \
            as a system evolves.""");
    }
}
