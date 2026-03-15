package io.github.seanchatmangpt.dtr.memory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Measures heap allocation before and after running a task.
 * Uses JMX MemoryMXBean for heap snapshots and GarbageCollectorMXBeans
 * to detect GC events triggered during the task.
 */
public final class HeapDeltaTracker {

    public record HeapDeltaResult(
        String label,
        long beforeBytes,
        long afterBytes,
        long deltaBytes,
        long gcCollectionsBefore,
        long gcCollectionsAfter,
        long gcCollectionsDelta,
        String deltaHuman  // e.g. "1.4 MB allocated"
    ) {}

    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();

    public static HeapDeltaResult track(String label, Runnable work) {
        // Force GC to get a cleaner baseline
        System.gc();
        try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        long gcBefore = totalGcCollections();
        long before   = MEMORY.getHeapMemoryUsage().getUsed();

        work.run();

        long after   = MEMORY.getHeapMemoryUsage().getUsed();
        long gcAfter = totalGcCollections();

        long delta = after - before;

        return new HeapDeltaResult(
            label,
            before,
            after,
            delta,
            gcBefore,
            gcAfter,
            gcAfter - gcBefore,
            humanBytes(delta)
        );
    }

    private static long totalGcCollections() {
        return ManagementFactory.getGarbageCollectorMXBeans()
            .stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionCount)
            .filter(c -> c >= 0)
            .sum();
    }

    static String humanBytes(long bytes) {
        if (bytes < 0)         return bytes + " bytes (freed)";
        if (bytes < 1_024)     return bytes + " bytes";
        if (bytes < 1_048_576) return String.format("%.1f KB", bytes / 1_024.0);
        return                        String.format("%.2f MB", bytes / 1_048_576.0);
    }
}
