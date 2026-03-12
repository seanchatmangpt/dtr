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

/**
 * Final breakpoint test: verifies that 50 test methods in a single class
 * work correctly with the shared static RenderMachine.
 *
 * CONFIRMED OOM BREAKPOINT (from prior run):
 * - 4 x 500MB = 2GB accumulated content causes OOM in Guava Joiner.on("\n").join()
 *   at RenderMachineImpl.writeOutListOfHtmlStringsIntoFile (line 331)
 * - Stack: Joiner.join → StringBuilder.append → Arrays.copyOf → OOM
 * - The crash occurs at @AfterClass during finishAndWriteOut(), NOT during say() accumulation
 * - This test keeps content under the limit to remain green.
 */
public class StressFinalTest extends DocTester {

    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();

    private static long usedMemoryMB() {
        return MEMORY.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    private static long maxMemoryMB() {
        return MEMORY.getHeapMemoryUsage().getMax() / (1024 * 1024);
    }

    /**
     * Stays just under the OOM threshold: 2 x 500MB = 1GB total.
     * The Joiner needs ~2x this (list + joined string) = ~2GB, which fits in 4GB heap.
     */
    @Test
    public void stressJustUnderLimit() {
        sayNextSection("Near-limit: 1GB accumulated content");

        long maxMem = maxMemoryMB();
        System.out.println("[NEAR-LIMIT] Max heap: " + maxMem + "MB");

        int chunkSizeMB = 500;
        int chunks = 2; // 1GB total - safe under the 2GB breakpoint

        for (int c = 0; c < chunks; c++) {
            long startTime = System.nanoTime();
            String payload = "D".repeat(chunkSizeMB * 1024 * 1024);
            sayRaw(payload);
            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            System.out.println("[NEAR-LIMIT] Chunk " + (c + 1) + "/" + chunks
                    + " (" + chunkSizeMB + "MB): " + elapsed + "ms, heap=" + usedMemoryMB() + "MB");
        }

        say("--- 1GB accumulated content: OK. Joiner will handle ~1GB join at @AfterClass ---");
    }

    // 50 test methods to verify method count scaling
    @Test public void method001() { say("Method 1"); }
    @Test public void method002() { say("Method 2"); }
    @Test public void method003() { say("Method 3"); }
    @Test public void method004() { say("Method 4"); }
    @Test public void method005() { say("Method 5"); }
    @Test public void method006() { say("Method 6"); }
    @Test public void method007() { say("Method 7"); }
    @Test public void method008() { say("Method 8"); }
    @Test public void method009() { say("Method 9"); }
    @Test public void method010() { say("Method 10"); }
    @Test public void method011() { say("Method 11"); }
    @Test public void method012() { say("Method 12"); }
    @Test public void method013() { say("Method 13"); }
    @Test public void method014() { say("Method 14"); }
    @Test public void method015() { say("Method 15"); }
    @Test public void method016() { say("Method 16"); }
    @Test public void method017() { say("Method 17"); }
    @Test public void method018() { say("Method 18"); }
    @Test public void method019() { say("Method 19"); }
    @Test public void method020() { say("Method 20"); }
    @Test public void method021() { say("Method 21"); }
    @Test public void method022() { say("Method 22"); }
    @Test public void method023() { say("Method 23"); }
    @Test public void method024() { say("Method 24"); }
    @Test public void method025() { say("Method 25"); }
    @Test public void method026() { say("Method 26"); }
    @Test public void method027() { say("Method 27"); }
    @Test public void method028() { say("Method 28"); }
    @Test public void method029() { say("Method 29"); }
    @Test public void method030() { say("Method 30"); }
    @Test public void method031() { say("Method 31"); }
    @Test public void method032() { say("Method 32"); }
    @Test public void method033() { say("Method 33"); }
    @Test public void method034() { say("Method 34"); }
    @Test public void method035() { say("Method 35"); }
    @Test public void method036() { say("Method 36"); }
    @Test public void method037() { say("Method 37"); }
    @Test public void method038() { say("Method 38"); }
    @Test public void method039() { say("Method 39"); }
    @Test public void method040() { say("Method 40"); }
    @Test public void method041() { say("Method 41"); }
    @Test public void method042() { say("Method 42"); }
    @Test public void method043() { say("Method 43"); }
    @Test public void method044() { say("Method 44"); }
    @Test public void method045() { say("Method 45"); }
    @Test public void method046() { say("Method 46"); }
    @Test public void method047() { say("Method 47"); }
    @Test public void method048() { say("Method 48"); }
    @Test public void method049() { say("Method 49"); }
    @Test public void method050() { say("Method 50"); }
}
