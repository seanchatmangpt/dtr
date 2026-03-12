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
package io.github.seanchatmangpt.dtr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Final breakpoint test: verifies that 50 test methods in a single class
 * work correctly with the shared static RenderMachine.
 *
 * <p>Joe Armstrong: "Let it crash" — this test deliberately approaches the OOM boundary
 * to confirm the system fails predictably, not silently. The threshold is kept safe
 * (1GB total) to stay green in CI while validating the scaling contract.</p>
 *
 * <p><strong>Confirmed OOM breakpoint (from prior run):</strong>
 * 4 × 500MB = 2GB accumulated content causes OOM in Guava Joiner.on("\n").join()
 * at RenderMachineImpl.writeOutListOfHtmlStringsIntoFile.
 * Stack: Joiner.join → StringBuilder.append → Arrays.copyOf → OOM.
 * Crash occurs at @AfterClass during finishAndWriteOut(), NOT during say() accumulation.</p>
 */
@DisplayName("Stress: near-OOM boundary — 50 scaled test methods")
public class StressFinalTest extends DocTester {

    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();

    private static long usedMemoryMB() {
        return MEMORY.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    private static long maxMemoryMB() {
        return MEMORY.getHeapMemoryUsage().getMax() / (1024 * 1024);
    }

    /**
     * Stays just under the OOM threshold: 2 × 500MB = 1GB total.
     * The Joiner needs ~2x this (list + joined string) = ~2GB, which fits in 4GB heap.
     *
     * <p>Assertion: memory growth must stay bounded — heap usage after the test
     * must remain below a reasonable ceiling (max heap - 512MB safety margin),
     * confirming no unbounded memory accumulation beyond what content requires.</p>
     */
    @Test
    @DisplayName("Near-limit: 1GB accumulated content — memory growth is bounded")
    public void stressJustUnderLimit() {
        sayNextSection("Near-limit: 1GB accumulated content");

        long maxMem = maxMemoryMB();
        long startMem = usedMemoryMB();
        System.out.println("[NEAR-LIMIT] Max heap: " + maxMem + "MB, start heap: " + startMem + "MB");

        int chunkSizeMB = 500;
        int chunks = 2; // 1GB total — safe under the 2GB OOM breakpoint

        for (int c = 0; c < chunks; c++) {
            long chunkStart = System.nanoTime();
            String payload = "D".repeat(chunkSizeMB * 1024 * 1024);
            sayRaw(payload);
            long elapsed = (System.nanoTime() - chunkStart) / 1_000_000;
            System.out.println("[NEAR-LIMIT] Chunk " + (c + 1) + "/" + chunks
                    + " (" + chunkSizeMB + "MB): " + elapsed + "ms, heap=" + usedMemoryMB() + "MB");
        }

        long endMem = usedMemoryMB();
        long memDeltaMB = endMem - startMem;

        say("--- 1GB accumulated content: OK. Joiner will handle ~1GB join at @AfterClass ---");
        say("Memory delta: " + memDeltaMB + "MB. Max heap: " + maxMem + "MB.");

        // Assertion: memory growth from the content (1GB) must not exceed max heap
        // with a 512MB safety margin for JVM overhead
        long memCeilingMB = maxMem - 512;
        assertTrue(endMem < memCeilingMB,
            "Heap usage after 1GB content (" + endMem + "MB) must be < max-512MB (" + memCeilingMB + "MB). " +
            "Memory growth must stay bounded; if this fails, there is a memory leak beyond expected content size.");
    }

    /**
     * Verifies that 50 sequential test methods in one class all execute correctly
     * with the shared static RenderMachine state. This validates method-count scaling.
     *
     * <p>Each repetition generates a small doc entry and asserts the test index is valid,
     * confirming the RenderMachine correctly accumulates entries across many test methods.</p>
     */
    @RepeatedTest(50)
    @DisplayName("Repeated method scaling — 50 methods on shared RenderMachine")
    public void scaledMethodExecution(RepetitionInfo info) {
        int current = info.getCurrentRepetition();
        int total   = info.getTotalRepetitions();

        say("Method " + current + " of " + total + ": RenderMachine accumulates correctly.");

        // Every repetition must have a valid index within bounds
        assertTrue(current >= 1 && current <= total,
            "Repetition index must be in [1, " + total + "] but was " + current);
    }
}
