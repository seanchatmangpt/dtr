package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.contract.InvariantTable;
import io.github.seanchatmangpt.dtr.contract.InvariantTable.InvariantResult;
import io.github.seanchatmangpt.dtr.contract.InvariantTable.InvariantRow;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class InvariantTableDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    @Test
    public void testListInvariants() {
        sayNextSection("sayInvariantTable — Design by Contract Documentation");
        say("DTR can document pre-conditions, post-conditions, and class invariants "
                + "as a verified table. Every row is a named, executed boolean check — "
                + "<em>not a comment, a proof.</em>");

        List<Integer> list = new ArrayList<>();
        int initialSize = 0;

        // Demonstrate the invariant table API
        InvariantResult result = InvariantTable.builder()
            .pre("list is not null",            () -> list != null)
            .pre("initial size is 0",           () -> list.size() == initialSize)
            .check("add(42) returns true",      () -> list.add(42))
            .post("size increased by 1",        () -> list.size() == initialSize + 1)
            .post("last element is 42",         () -> list.get(list.size() - 1) == 42)
            .invariant("list is not empty",     () -> !list.isEmpty())
            .invariant("get(0) == 42",          () -> list.get(0) == 42)
            .evaluate();

        renderResult(result);

        sayAndAssertThat("All invariants pass", result.allPass(), is(true));
    }

    @Test
    public void testSortingPostConditions() {
        sayNextSection("sayInvariantTable — Sorting Algorithm Post-Conditions");
        say("After sorting a list, the post-conditions are: the list is in ascending order "
                + "and has the same size as before.");

        List<Integer> data = new ArrayList<>(List.of(5, 3, 8, 1, 9, 2));
        int sizeBefore = data.size();

        Collections.sort(data);

        InvariantResult result = InvariantTable.builder()
            .post("size unchanged",             () -> data.size() == sizeBefore)
            .post("first \u2264 last",          () -> data.get(0) <= data.get(data.size() - 1))
            .post("data[0] is minimum",         () -> data.get(0) == 1)
            .post("data[n-1] is maximum",       () -> data.get(data.size() - 1) == 9)
            .invariant("ascending order",       () -> {
                for (int i = 0; i < data.size() - 1; i++) {
                    if (data.get(i) > data.get(i + 1)) return false;
                }
                return true;
            })
            .evaluate();

        renderResult(result);

        sayAndAssertThat("All sorting post-conditions hold", result.allPass(), is(true));
    }

    private void renderResult(InvariantResult result) {
        String[][] table = new String[result.rows().size() + 1][3];
        table[0] = new String[]{"Invariant", "Kind", "Result"};
        for (int i = 0; i < result.rows().size(); i++) {
            InvariantRow row = result.rows().get(i);
            table[i + 1] = new String[]{row.name(), row.kind(), row.symbol()};
        }
        sayTable(table);
        say("Summary: <strong>" + result.passing() + " passing</strong>, "
                + "<strong>" + result.failing() + " failing</strong>");
    }
}
