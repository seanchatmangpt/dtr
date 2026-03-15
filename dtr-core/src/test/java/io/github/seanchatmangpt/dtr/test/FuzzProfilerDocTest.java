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
import io.github.seanchatmangpt.dtr.fuzz.FuzzProfiler;
import io.github.seanchatmangpt.dtr.fuzz.FuzzProfiler.CategoryCount;
import io.github.seanchatmangpt.dtr.fuzz.FuzzProfiler.FuzzResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;

/**
 * Documentation test for {@link FuzzProfiler} — demonstrates {@code sayFuzzProfile}
 * by profiling random integer and string inputs and rendering the distribution as
 * a documentation table with Unicode bar charts.
 *
 * <p>Fixed seeds are used throughout so the documentation is reproducible.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class FuzzProfilerDocTest extends DtrTest {

    private static final Random RANDOM = new Random(42L); // fixed seed for reproducibility

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    @Test
    void a1_sayFuzzProfile_integer_distribution() {
        sayNextSection("sayFuzzProfile — Random Input Distribution Analysis");

        say("DTR profiles how random inputs distribute across categories. "
                + "This documents statistical coverage and boundary behavior — "
                + "proving that your code handles all input classes proportionally.");

        sayCode("""
                FuzzResult result = FuzzProfiler.profile(
                    "Random int distribution",
                    () -> random.nextInt(200) - 100,  // -100 to +99
                    n -> {
                        if (n < -50)      return "very negative (< -50)";
                        else if (n < 0)   return "negative (-50 to -1)";
                        else if (n == 0)  return "zero";
                        else if (n <= 50) return "positive (1 to 50)";
                        else              return "very positive (> 50)";
                    },
                    10_000
                );
                """, "java");

        FuzzResult result = FuzzProfiler.profile(
            "Random int distribution",
            () -> RANDOM.nextInt(200) - 100,  // range: -100 to +99
            n -> {
                if (n < -50)      return "very negative (< -50)";
                else if (n < 0)   return "negative (-50 to -1)";
                else if (n == 0)  return "zero";
                else if (n <= 50) return "positive (1 to 50)";
                else              return "very positive (> 50)";
            },
            10_000
        );

        // Render the distribution table
        var dist = result.distribution();
        var table = new String[dist.size() + 1][4];
        table[0] = new String[]{"Category", "Count", "Pct", "Distribution"};
        for (int i = 0; i < dist.size(); i++) {
            CategoryCount c = dist.get(i);
            table[i + 1] = new String[]{
                c.category(),
                String.valueOf(c.count()),
                "%.1f%%".formatted(c.percentage()),
                c.bar()
            };
        }
        sayTable(table);

        say("Most common: **" + result.mostCommon() + "** | "
                + "Least common: **" + result.leastCommon() + "**");
        say("Total samples: " + result.sampleCount() + " (seed=42, reproducible)");

        sayNote("A uniform distribution over 200 integers produces ~25% per quartile "
                + "and ~0.5% for the exact-zero bucket — consistent with seed=42 results above.");

        sayAndAssertThat("5 categories profiled", result.distribution().size(), is(5));
        sayAndAssertThat("sample count matches requested", result.sampleCount(), is(10_000));
    }

    @Test
    void a2_sayFuzzProfile_string_length_distribution() {
        sayNextSection("sayFuzzProfile — String Length Distribution");

        say("Documents how string lengths distribute when generating random strings. "
                + "Useful for validating that edge cases (empty, short, long) are covered "
                + "in your string-processing code.");

        sayCode("""
                FuzzResult result = FuzzProfiler.profile(
                    "Random string length",
                    () -> generateGaussianString(rng, 15),
                    n -> {
                        int len = n.length();
                        if (len == 0)       return "empty";
                        else if (len <= 5)  return "short (1-5)";
                        else if (len <= 15) return "medium (6-15)";
                        else if (len <= 30) return "long (16-30)";
                        else                return "very long (30+)";
                    },
                    1_000
                );
                """, "java");

        // Use a seeded local random for reproducibility in the string-length test
        var seededRng = new Random(99L);

        FuzzResult result = FuzzProfiler.profile(
            "Random string length",
            () -> {
                int len = (int) Math.abs(seededRng.nextGaussian() * 15);
                var sb = new StringBuilder(len);
                for (int i = 0; i < len; i++) {
                    sb.append((char) ('a' + seededRng.nextInt(26)));
                }
                return sb.toString();
            },
            n -> {
                int len = n.length();
                if (len == 0)       return "empty";
                else if (len <= 5)  return "short (1-5)";
                else if (len <= 15) return "medium (6-15)";
                else if (len <= 30) return "long (16-30)";
                else                return "very long (30+)";
            },
            1_000
        );

        var dist = result.distribution();
        var table = new String[dist.size() + 1][4];
        table[0] = new String[]{"Category", "Count", "Pct", "Distribution"};
        for (int i = 0; i < dist.size(); i++) {
            CategoryCount c = dist.get(i);
            table[i + 1] = new String[]{
                c.category(),
                String.valueOf(c.count()),
                "%.1f%%".formatted(c.percentage()),
                c.bar()
            };
        }
        sayTable(table);

        say("Most common: **" + result.mostCommon() + "** | "
                + "Least common: **" + result.leastCommon() + "**");
        say("Total samples: " + result.sampleCount() + " (seed=99, Gaussian sigma=15)");

        sayNote("Use `Random(seed)` for reproducible documentation. "
                + "A Gaussian generator naturally concentrates around the mean length, "
                + "leaving tails (empty and very-long) as rare edge cases.");

        sayAndAssertThat("sample count is 1000", result.sampleCount(), is(1_000));
        sayAndAssertThat("distribution is non-empty", result.distribution().isEmpty(), is(false));
    }

    @Test
    void a3_sayFuzzProfile_record_api_documentation() {
        sayNextSection("sayFuzzProfile — FuzzProfiler API Reference");

        say("The `FuzzProfiler` class is a pure utility with no external dependencies. "
                + "It produces two records: `CategoryCount` (per-bucket statistics) "
                + "and `FuzzResult` (the full profile).");

        sayRecordComponents(FuzzProfiler.CategoryCount.class);
        say("");
        sayRecordComponents(FuzzProfiler.FuzzResult.class);

        sayNote("Both records are immutable value carriers — safe to share across threads "
                + "and to serialize as documentation artifacts.");
    }
}
