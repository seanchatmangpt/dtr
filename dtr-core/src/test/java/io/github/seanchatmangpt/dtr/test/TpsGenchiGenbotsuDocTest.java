package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * TPS Station 4 — Genchi Genbutsu: Go See the Code
 *
 * <p>Genchi Genbutsu (現地現物) literally means "go to the actual place, see the
 * actual thing." Toyota managers are expected to observe problems on the factory
 * floor directly, not from spreadsheets. Taiichi Ohno would draw a chalk circle
 * on the floor and tell engineers to stand in it for hours, watching the line,
 * until they could see the waste with their own eyes.</p>
 *
 * <p>In software, the equivalent is runtime introspection: observe the actual JVM
 * state, actual thread counts, actual memory usage, actual call sites — not
 * estimated or assumed values. Joe Armstrong was explicit about this: "Measure
 * everything. Trust nothing you haven't measured. An estimate is a guess dressed
 * in a suit."</p>
 *
 * <p>This test documents the Genchi Genbutsu pattern for Java 26: using
 * StackWalker, ManagementFactory, and DTR's sayCallSite/sayEnvProfile to
 * observe the actual runtime state of the system at the moment of documentation.
 * The documentation cannot lie — it was generated from the live JVM.</p>
 */
class TpsGenchiGenbotsuDocTest extends DtrTest {

    @Test
    void genchiGenbotsu_goSeeTheCode() {
        sayNextSection("TPS Station 4 — Genchi Genbutsu: Go See the Code");

        say(
            "Ohno's chalk circle is not a metaphor — it is a measurement protocol. " +
            "Stand here. Watch. Count. Record. Do not theorise. Do not average. " +
            "The defect rate you see in this cycle is the defect rate for this cycle. " +
            "Not the projected defect rate. Not the historical average. " +
            "What is happening right now, in this place, with this material. " +
            "In code: 'this measurement, this JVM, this heap, this load, right now.'"
        );

        sayNote(
            "Armstrong, on the same idea: 'Profile first. Guess later — no, actually, " +
            "never guess. A guess is just a poorly-justified opinion. " +
            "System.nanoTime() is not a guess. Run it a million times, throw away the outliers, " +
            "take the median. That is a measurement. That is what you put in the commit message.'"
        );

        sayNextSection("Live Call Site Observation");

        say(
            "sayCallSite() uses Java's StackWalker API to report the exact location in the " +
            "source tree from which it was called. This is Genchi Genbutsu applied to " +
            "documentation: the doc knows where it was generated. It cannot be copied to " +
            "a different file and remain accurate — the call site is part of the content."
        );

        sayCallSite();

        sayNextSection("Live JVM Environment Observation");

        say(
            "sayEnvProfile() reads the actual JVM state at the moment of execution: " +
            "Java version, available processors, heap usage, timezone, DTR version. " +
            "This is not a hardcoded table — it is the Ohno chalk circle applied to the JVM."
        );

        sayEnvProfile();

        sayNextSection("Thread Count: The Factory Floor Census");

        say(
            "In a Toyota factory, the floor census counts workers per station, " +
            "idle workers, and workers waiting for materials. In a JVM, the equivalent " +
            "is thread count by state: RUNNABLE, BLOCKED, WAITING, TIMED_WAITING. " +
            "A high BLOCKED count is a lock contention signal. " +
            "A high WAITING count is a queue starvation signal. " +
            "Neither is visible from the application layer — only from the JVM."
        );

        var threadBean = ManagementFactory.getThreadMXBean();
        var threadInfo = new LinkedHashMap<String, String>();
        threadInfo.put("Live thread count",           String.valueOf(threadBean.getThreadCount()));
        threadInfo.put("Peak thread count",           String.valueOf(threadBean.getPeakThreadCount()));
        threadInfo.put("Daemon thread count",         String.valueOf(threadBean.getDaemonThreadCount()));
        threadInfo.put("Total started (cumulative)", String.valueOf(threadBean.getTotalStartedThreadCount()));
        sayKeyValue(threadInfo);

        sayNextSection("Memory: Heap as Inventory");

        say(
            "Toyota's Just-In-Time principle: inventory is waste. The equivalent in the JVM " +
            "is heap usage: allocated objects that are not immediately needed are waste. " +
            "Genchi Genbutsu means measuring the actual heap state right now, not the " +
            "theoretical peak. The GC overhead ratio is the factory's inventory carrying cost."
        );

        var memBean = ManagementFactory.getMemoryMXBean();
        var heap = memBean.getHeapMemoryUsage();
        var memInfo = new LinkedHashMap<String, String>();
        memInfo.put("Heap used",       String.format("%.2f MB", heap.getUsed() / 1_048_576.0));
        memInfo.put("Heap committed",  String.format("%.2f MB", heap.getCommitted() / 1_048_576.0));
        memInfo.put("Heap max",        String.format("%.2f MB", heap.getMax() / 1_048_576.0));
        memInfo.put("Heap utilisation",String.format("%.1f%%", (double) heap.getUsed() / heap.getMax() * 100));
        sayKeyValue(memInfo);

        sayNextSection("StackWalker: The Ohno Circle Applied to Call Chains");

        say(
            "StackWalker (JEP 259, Java 9) is the programmatic Ohno circle: stand in the " +
            "call stack and observe. Filter by package. Collect only what you need. " +
            "The lazy stream API means you never materialise frames you don't observe. " +
            "This is Genchi Genbutsu with zero waste: look at exactly what you need, " +
            "nothing more."
        );

        sayCode(
            """
            // StackWalker: observe the actual call chain at runtime
            // RETAIN_CLASS_REFERENCE gives Class<?> objects — not just string names
            var walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

            // Genchi Genbutsu: go see who called me
            List<String> callerChain = walker.walk(frames -> frames
                .filter(f -> f.getClassName().startsWith("io.github.seanchatmangpt"))
                .map(f -> "%s::%s (line %d)".formatted(
                    f.getClassName(),
                    f.getMethodName(),
                    f.getLineNumber()))
                .limit(5)
                .toList()
            );

            // Result is the ACTUAL call chain — not an estimate, not a mock
            // If this method is called from a different context, the result changes
            """,
            "java"
        );

        var observedStack = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk(frames -> frames
                .filter(f -> f.getClassName().startsWith("io.github.seanchatmangpt"))
                .map(f -> f.getClassName() + "::" + f.getMethodName() + " (line " + f.getLineNumber() + ")")
                .limit(5)
                .toList()
            );

        sayUnorderedList(observedStack.isEmpty()
            ? List.of("(no DTR frames in current call chain)")
            : observedStack);

        sayNextSection("The Genchi Genbutsu Invariants");

        sayTable(new String[][] {
            {"Invariant", "Description", "Violation Symptom"},
            {"Measure, don't estimate",    "All reported metrics come from JVM MBeans or System.nanoTime()", "Hardcoded numbers in test output"},
            {"Observe at runtime",         "Metrics are collected at test execution, not at test write time",  "Static final constants in output"},
            {"Document the observer",      "sayCallSite() pins the location of the observation",              "Copied docs with wrong line numbers"},
            {"No averages without data",   "Report median + p99 from real samples, not guesses",               "\"approximately X ms\" in documentation"},
            {"Environment is part of fact","sayEnvProfile() is mandatory for any performance claim",           "Performance claim without Java version"},
        });

        sayWarning(
            "Any documentation that reports a performance number without sayEnvProfile() " +
            "violates the Genchi Genbutsu invariant. A benchmark that ran on Java 21 is not " +
            "the same benchmark as one that ran on Java 26. The environment is part of the " +
            "measurement. If you don't record the environment, the measurement is not reproducible. " +
            "Armstrong: 'A result without a timestamp and a platform description is anecdote, not data.'"
        );

        sayOrderedList(List.of(
            "All performance claims documented via sayBenchmark() — not estimates",
            "All environment claims documented via sayEnvProfile() — not hardcoded",
            "All call-site claims documented via sayCallSite() — not assumed",
            "Thread state observations via ManagementFactory — not inferred",
            "Heap observations via MemoryMXBean — not from GC logs after the fact",
            "StackWalker filters to the minimum needed frames — no full-stack dumps in docs"
        ));
    }
}
