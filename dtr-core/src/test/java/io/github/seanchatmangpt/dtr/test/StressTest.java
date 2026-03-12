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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

/**
 * Stress tests to find DTR breakpoints for maximum
 * documentation generation size and count.
 */
public class StressTest extends DtrTest {

    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();

    private static long usedMemoryMB() {
        return MEMORY.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    // =========================================================================
    // Test 1: Maximum say() calls in a single test method
    // =========================================================================
    @Test
    public void stressSayCallCount() {
        sayNextSection("Stress: say() call count");

        int[] counts = {100, 1_000, 10_000, 50_000, 100_000};

        for (int count : counts) {
            long startMem = usedMemoryMB();
            long startTime = System.nanoTime();

            for (int i = 0; i < count; i++) {
                say("Say call number " + i + " with some padding text to simulate real documentation content.");
            }

            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            long endMem = usedMemoryMB();

            say("--- say() x " + count + ": " + elapsed + "ms, mem=" + endMem + "MB (delta=" + (endMem - startMem) + "MB) ---");
        }
    }

    // =========================================================================
    // Test 2: Maximum sayNextSection() calls (sidebar navigation scaling)
    // =========================================================================
    @Test
    public void stressSectionCount() {
        sayNextSection("Stress: section count");

        int[] counts = {100, 1_000, 5_000, 10_000};

        for (int count : counts) {
            long startTime = System.nanoTime();
            long startMem = usedMemoryMB();

            for (int i = 0; i < count; i++) {
                sayNextSection("Section " + count + " batch item " + i);
            }

            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            long endMem = usedMemoryMB();
            say("--- sections x " + count + ": " + elapsed + "ms, mem=" + endMem + "MB (delta=" + (endMem - startMem) + "MB) ---");
        }
    }

    // =========================================================================
    // Test 3: Maximum sayRaw() payload size (single large content block)
    // =========================================================================
    @Test
    public void stressLargePayloadSize() {
        sayNextSection("Stress: large payload size");

        // Generate payloads of increasing size
        int[] sizesKB = {1, 10, 100, 1_000, 10_000, 50_000};

        for (int sizeKB : sizesKB) {
            long startTime = System.nanoTime();
            long startMem = usedMemoryMB();

            String payload = "X".repeat(sizeKB * 1024);
            sayRaw("<pre>" + payload + "</pre>");

            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            long endMem = usedMemoryMB();
            say("--- payload " + sizeKB + "KB: " + elapsed + "ms, mem=" + endMem + "MB (delta=" + (endMem - startMem) + "MB) ---");
        }
    }

    // =========================================================================
    // Test 4: Maximum sayAndAssertThat() calls
    // =========================================================================
    @Test
    public void stressAssertionCount() {
        sayNextSection("Stress: assertion count");

        int[] counts = {100, 1_000, 10_000, 50_000};

        for (int count : counts) {
            long startTime = System.nanoTime();
            long startMem = usedMemoryMB();

            for (int i = 0; i < count; i++) {
                sayAndAssertThat("Assertion " + i + " passes", true, is(true));
            }

            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            long endMem = usedMemoryMB();
            say("--- assertions x " + count + ": " + elapsed + "ms, mem=" + endMem + "MB (delta=" + (endMem - startMem) + "MB) ---");
        }
    }

    // =========================================================================
    // Test 5: Combined stress - many sections with many says and assertions
    // =========================================================================
    @Test
    public void stressCombinedLoad() {
        sayNextSection("Stress: combined load");

        int sections = 500;
        int saysPerSection = 50;
        int assertsPerSection = 10;

        long startTime = System.nanoTime();
        long startMem = usedMemoryMB();

        for (int s = 0; s < sections; s++) {
            sayNextSection("Combined Section " + s);

            for (int i = 0; i < saysPerSection; i++) {
                say("Section " + s + " paragraph " + i + ": Lorem ipsum dolor sit amet, "
                        + "consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore.");
            }

            for (int a = 0; a < assertsPerSection; a++) {
                sayAndAssertThat("Section " + s + " assertion " + a, s + a, equalTo(s + a));
            }
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        long endMem = usedMemoryMB();
        say("--- combined " + sections + " sections x " + saysPerSection + " says x "
                + assertsPerSection + " asserts: " + elapsed + "ms, mem=" + endMem + "MB (delta=" + (endMem - startMem) + "MB) ---");
    }

    // =========================================================================
    // Test 6: HTML file size breakpoint - escalating total content
    // =========================================================================
    @Test
    public void stressOutputFileSize() {
        sayNextSection("Stress: output file size");

        // Each iteration adds ~100 bytes of HTML content
        // Target: find where file write or join becomes slow
        int totalSays = 200_000;
        long startTime = System.nanoTime();
        long startMem = usedMemoryMB();

        for (int i = 0; i < totalSays; i++) {
            say("Row " + i + ": ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz 0123456789");

            if (i % 50_000 == 0 && i > 0) {
                long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                long mem = usedMemoryMB();
                System.out.println("[StressTest] say #" + i + ": " + elapsed + "ms, mem=" + mem + "MB");
            }
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        long endMem = usedMemoryMB();
        say("--- file size stress " + totalSays + " says: " + elapsed + "ms, mem=" + endMem + "MB (delta=" + (endMem - startMem) + "MB) ---");
    }

    // =========================================================================
    // Test 7: Guava Joiner.on("\n").join() stress - the finishAndWriteOut bottleneck
    // =========================================================================
    @Test
    public void stressJoinerBottleneck() {
        sayNextSection("Stress: Joiner bottleneck");

        // The real bottleneck is Joiner.on("\n").join(finalHtmlDocument) in writeOutListOfHtmlStringsIntoFile
        // Each say() adds 3 strings to the list. At 200K says that's 600K list entries.
        // Let's push to find where the join becomes problematic.

        int totalSays = 300_000;
        long startTime = System.nanoTime();

        for (int i = 0; i < totalSays; i++) {
            say("Joiner stress line " + i);
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        long mem = usedMemoryMB();
        say("--- joiner stress " + totalSays + " says (" + (totalSays * 3) + " list entries): "
                + elapsed + "ms, mem=" + mem + "MB ---");

        // The actual bottleneck happens during finishAndWriteOut() which runs at @AfterAll
    }
}
