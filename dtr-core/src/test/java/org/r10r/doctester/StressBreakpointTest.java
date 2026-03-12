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

import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

/**
 * Extreme stress tests to find actual breakpoints where DocTester
 * runs out of memory or becomes unacceptably slow.
 *
 * Key bottleneck: RenderMachineImpl accumulates all HTML in a List of String
 * in memory, then joins them with Guava Joiner.on("\n").join() into a single
 * String before writing to file. This means peak memory is ~2x the total
 * content size (list + joined string).
 */
public class StressBreakpointTest extends DocTester {

    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();

    private static long usedMemoryMB() {
        return MEMORY.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    /**
     * Push say() count to 1 million. Each say() adds 3 strings to the list.
     * At 1M says = 3M list entries. With ~80 bytes avg per entry,
     * that's ~240MB in list + ~240MB for joined string = ~480MB peak.
     * With 2GB heap this should be near the limit.
     */
    @Test
    public void stressMillionSays() {
        sayNextSection("Breakpoint: 1 million say() calls");

        int total = 1_000_000;
        long startTime = System.nanoTime();
        long startMem = usedMemoryMB();

        for (int i = 0; i < total; i++) {
            say("Line " + i + ": padding content for realistic size measurement.");

            if (i % 250_000 == 0 && i > 0) {
                long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                long mem = usedMemoryMB();
                System.out.println("[BREAKPOINT] say #" + i + ": " + elapsed + "ms, heap=" + mem + "MB");
            }
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        long endMem = usedMemoryMB();
        System.out.println("[BREAKPOINT] 1M says complete: " + elapsed + "ms, heap=" + endMem + "MB (delta=" + (endMem - startMem) + "MB)");

        say("--- 1M says: " + elapsed + "ms, mem=" + endMem + "MB ---");
    }

    /**
     * Test a single massive payload. The Guava Joiner has to handle this
     * as one element in the list, but the final String concatenation
     * will be enormous.
     */
    @Test
    public void stressSingleHugePayload() {
        sayNextSection("Breakpoint: single huge payload");

        // Try 100MB, 200MB, 500MB payloads
        int[] sizesMB = {100, 200, 500};

        for (int sizeMB : sizesMB) {
            long startTime = System.nanoTime();
            long startMem = usedMemoryMB();

            try {
                String payload = "A".repeat(sizeMB * 1024 * 1024);
                sayRaw("<pre>" + payload + "</pre>");

                long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                long endMem = usedMemoryMB();
                System.out.println("[BREAKPOINT] " + sizeMB + "MB payload: " + elapsed + "ms, heap=" + endMem + "MB");
                say("--- " + sizeMB + "MB payload: OK in " + elapsed + "ms ---");
            } catch (OutOfMemoryError e) {
                System.out.println("[BREAKPOINT] OOM at " + sizeMB + "MB payload! heap=" + usedMemoryMB() + "MB");
                say("--- " + sizeMB + "MB payload: OOM BREAKPOINT ---");
                break;
            }
        }
    }

    /**
     * Test maximum number of sections. Each section adds to both
     * htmlDocument (3 strings) and headerTitle/headerId (1 each).
     * The sidebar navigation grows linearly.
     */
    @Test
    public void stressMassiveSectionCount() {
        sayNextSection("Breakpoint: massive section count");

        int total = 50_000;
        long startTime = System.nanoTime();

        for (int i = 0; i < total; i++) {
            sayNextSection("Section number " + i + " with a moderately long title for testing");
            say("Content for section " + i);
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        long mem = usedMemoryMB();
        System.out.println("[BREAKPOINT] " + total + " sections: " + elapsed + "ms, heap=" + mem + "MB");
        say("--- " + total + " sections: " + elapsed + "ms ---");
    }

    /**
     * Test maximum assertions count. Each assertion adds 3 strings
     * (div open, message, div close).
     */
    @Test
    public void stressMassiveAssertions() {
        sayNextSection("Breakpoint: massive assertion count");

        int total = 500_000;
        long startTime = System.nanoTime();

        for (int i = 0; i < total; i++) {
            sayAndAssertThat("Assert #" + i + " value matches", i, equalTo(i));
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        long mem = usedMemoryMB();
        System.out.println("[BREAKPOINT] " + total + " assertions: " + elapsed + "ms, heap=" + mem + "MB");
        say("--- " + total + " assertions: " + elapsed + "ms ---");
    }
}
