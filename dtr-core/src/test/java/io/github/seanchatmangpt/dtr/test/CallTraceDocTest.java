package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.trace.CallTraceRecorder;
import io.github.seanchatmangpt.dtr.trace.CallTraceRecorder.Step;
import io.github.seanchatmangpt.dtr.trace.CallTraceRecorder.StepResult;
import io.github.seanchatmangpt.dtr.trace.CallTraceRecorder.TraceResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;

public class CallTraceDocTest extends DtrTest {

    @Test
    public void testBubbleSortTrace() {
        sayNextSection("sayCallTrace — Step-by-Step Algorithm Execution Trace");
        say("DTR documents algorithms with real intermediate values. "
                + "Each step shows its name, actual output, and elapsed time. "
                + "No pseudocode — this is the algorithm running.");

        var data = new ArrayList<>(List.of(5, 3, 8, 1, 9, 2));

        TraceResult trace = CallTraceRecorder.record(List.of(
            new Step("Initial state",           () -> data.toString()),
            new Step("Find minimum",            () -> {
                int min = data.stream().mapToInt(Integer::intValue).min().orElse(-1);
                return "min = " + min;
            }),
            new Step("Sort (Collections.sort)", () -> {
                Collections.sort(data);
                return data.toString();
            }),
            new Step("First element (min)",     () -> String.valueOf(data.get(0))),
            new Step("Last element (max)",      () -> String.valueOf(data.get(data.size() - 1))),
            new Step("Verify sorted",           () -> {
                for (int i = 0; i < data.size() - 1; i++) {
                    if (data.get(i) > data.get(i + 1)) return "NOT SORTED at index " + i;
                }
                return "ascending order confirmed";
            })
        ));

        String[][] table = new String[trace.steps().size() + 1][4];
        table[0] = new String[]{"#", "Step", "Output", "Elapsed"};
        for (int i = 0; i < trace.steps().size(); i++) {
            StepResult s = trace.steps().get(i);
            table[i + 1] = new String[]{
                String.valueOf(s.index()),
                s.name(),
                s.output(),
                s.elapsedHuman()
            };
        }
        sayTable(table);

        say("Total trace time: " + CallTraceRecorder.humanNs(trace.totalNs()));

        sayAndAssertThat("Final state is sorted", data.get(0), is(1));
        sayAndAssertThat("Trace has 6 steps", trace.steps().size(), is(6));
    }

    @Test
    public void testStringProcessingPipeline() {
        sayNextSection("sayCallTrace — String Processing Pipeline");
        say("Traces a multi-step text transformation pipeline, showing the string "
                + "at each stage of processing.");

        String[] current = {"  Hello, World! This is DTR.  "};

        TraceResult trace = CallTraceRecorder.record(List.of(
            new Step("Raw input",      () -> "\"" + current[0] + "\""),
            new Step("trim()",         () -> { current[0] = current[0].trim();
                                               return "\"" + current[0] + "\""; }),
            new Step("toLowerCase()",  () -> { current[0] = current[0].toLowerCase();
                                               return "\"" + current[0] + "\""; }),
            new Step("replace commas", () -> { current[0] = current[0].replace(",", "");
                                               return "\"" + current[0] + "\""; }),
            new Step("word count",     () -> current[0].split("\\s+").length + " words"),
            new Step("length (chars)", () -> current[0].length() + " chars")
        ));

        String[][] table = new String[trace.steps().size() + 1][4];
        table[0] = new String[]{"#", "Step", "Output", "Elapsed"};
        for (int i = 0; i < trace.steps().size(); i++) {
            StepResult s = trace.steps().get(i);
            table[i + 1] = new String[]{
                String.valueOf(s.index()),
                s.name(),
                s.output(),
                s.elapsedHuman()
            };
        }
        sayTable(table);

        sayAndAssertThat("Trace has 6 steps", trace.steps().size(), is(6));
    }
}
