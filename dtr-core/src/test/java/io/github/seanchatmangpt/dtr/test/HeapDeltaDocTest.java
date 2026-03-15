package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.memory.HeapDeltaTracker;
import io.github.seanchatmangpt.dtr.memory.HeapDeltaTracker.HeapDeltaResult;
import org.junit.jupiter.api.Test;

public class HeapDeltaDocTest extends DtrTest {

    @Test
    public void testAllocatingAList() {
        sayNextSection("sayHeapDelta — Real Heap Allocation Measurement");
        say("DTR can document the <em>actual</em> memory footprint of any code path "
                + "using JMX heap snapshots. This proves memory behaviour — no estimates, "
                + "no guesswork.");

        HeapDeltaResult result = HeapDeltaTracker.track(
            "Allocate ArrayList of 10_000 Strings",
            () -> {
                java.util.List<String> list = new java.util.ArrayList<>(10_000);
                for (int i = 0; i < 10_000; i++) {
                    list.add("item-" + i);
                }
            }
        );

        String[][] table = new String[][]{
            {"Metric", "Value"},
            {"Label",           result.label()},
            {"Before (bytes)",  String.valueOf(result.beforeBytes())},
            {"After (bytes)",   String.valueOf(result.afterBytes())},
            {"Delta",           result.deltaHuman()},
            {"GC triggered",    result.gcCollectionsDelta() > 0 ? "yes (" + result.gcCollectionsDelta() + ")" : "no"}
        };
        sayTable(table);

        say("Heap delta of <code>" + result.deltaHuman() + "</code> measured for: "
                + "<em>" + result.label() + "</em>");

        sayNote("JMX heap snapshots are approximate — GC activity between measurements "
                + "can affect results. Run with a stable heap for best accuracy.");
    }

    @Test
    public void testZeroAllocationPath() {
        sayNextSection("sayHeapDelta — Near-Zero Allocation Path");
        say("Primitive arithmetic should allocate nothing on the heap. "
                + "DTR documents this guarantee explicitly.");

        HeapDeltaResult result = HeapDeltaTracker.track(
            "Sum 1M ints (primitive loop)",
            () -> {
                long sum = 0;
                for (int i = 0; i < 1_000_000; i++) sum += i;
            }
        );

        say("Heap delta for primitive sum: <strong>" + result.deltaHuman() + "</strong>");
        sayNote("Primitive operations stay on the stack — no heap allocation expected.");
    }
}
