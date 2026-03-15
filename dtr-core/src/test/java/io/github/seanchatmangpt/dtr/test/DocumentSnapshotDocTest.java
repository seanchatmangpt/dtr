/*
 * Copyright 2026 the original author or authors.
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
import io.github.seanchatmangpt.dtr.snapshot.DocumentSnapshot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Documentation test for {@link DocumentSnapshot}.
 *
 * <p>Demonstrates snapshot saving and diffing of live document state.
 * Tests run in alphabetical order so that {@code t02_saveSnapshot} persists the
 * snapshot that {@code t03_diffSnapshot} reads back.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class DocumentSnapshotDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // t01 — Overview
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("Document Snapshot & Diff");

        say("""
            `DocumentSnapshot` captures the current state of a DTR document as a JSON \
            array on disk and can diff that state against a later revision.  \
            This gives authors a concrete audit trail of how generated documentation \
            changes across test runs — without needing an external VCS diff.""");

        say("""
            Two complementary operations are exposed:

            - **save** — write the current document lines to \
              `target/docs/snapshots/{key}.json`.
            - **diff** — compare current lines against the previously saved snapshot, \
              then overwrite the snapshot with the new state.""");

        sayCode("""
                // Save the document state at this point in the test:
                var saved = DocumentSnapshot.save("my-doc", currentLines);
                DocumentSnapshot.toMarkdown(saved).forEach(markdownDocument::add);

                // On a later run (or a later test method), diff against the saved state:
                var diff = DocumentSnapshot.diff("my-doc", currentLines);
                DocumentSnapshot.diffToMarkdown(diff).forEach(markdownDocument::add);
                """, "java");

        sayNote("""
            The snapshot key is sanitized — any non-alphanumeric character is replaced \
            by `_` — so `"my-doc"` and `"my_doc"` resolve to the same file. \
            All methods are static and never throw; errors surface as an empty path \
            in the `SaveResult`.""");
    }

    // =========================================================================
    // t02 — Save snapshot via sayDocumentSnapshot
    // =========================================================================

    @Test
    void t02_saveSnapshot() {
        sayNextSection("Saving the Current Document State");

        say("""
            Calling `sayDocumentSnapshot(key)` captures the document lines accumulated \
            so far in the current test run and persists them to \
            `target/docs/snapshots/{key}.json`.""");

        // This call captures everything rendered above (including t01 output) and saves it.
        sayDocumentSnapshot("test-snapshot-wave3");

        say("Snapshot saved.");
    }

    // =========================================================================
    // t03 — Diff snapshot via sayDocumentDiff
    // =========================================================================

    @Test
    void t03_diffSnapshot() {
        sayNextSection("Diffing Against a Previous Snapshot");

        say("""
            Calling `sayDocumentDiff(key)` reads the snapshot written by t02, \
            computes added and removed lines relative to the current document state, \
            then overwrites the snapshot with the newest state.""");

        // Diffs against the snapshot saved in t02.
        sayDocumentDiff("test-snapshot-wave3");

        say("""
            The diff above shows lines added since t02 ran. \
            Because t03 itself adds content before the diff call, at least the \
            heading and paragraph lines from this section appear in the *added* list.""");
    }

    // =========================================================================
    // t04 — Direct save
    // =========================================================================

    @Test
    void t04_directSave() {
        sayNextSection("Direct Save via DocumentSnapshot.save()");

        say("""
            `DocumentSnapshot.save(key, lines)` can be called directly without going \
            through the render machine.  The result record exposes the key, line count, \
            and absolute file path for immediate inspection.""");

        var lines = List.of("line1", "line2", "line3");
        var result = DocumentSnapshot.save("direct-test", lines);

        sayKeyValue(Map.of(
            "Key",        result.key(),
            "Line count", String.valueOf(result.lineCount()),
            "Path",       result.path().isEmpty() ? "(error)" : result.path()
        ));

        assertEquals(3, result.lineCount(),
            "SaveResult.lineCount() must equal the number of lines passed to save()");
    }

    // =========================================================================
    // t05 — Direct diff
    // =========================================================================

    @Test
    void t05_directDiff() {
        sayNextSection("Direct Diff via DocumentSnapshot.diff()");

        say("""
            `DocumentSnapshot.diff(key, currentLines)` compares the supplied lines \
            against the snapshot written by t04 and returns a `DiffResult` record \
            describing what changed.  Here `"new-line"` was added and `"line2"` was removed.""");

        // t04 saved ["line1", "line2", "line3"]; now we change the set.
        var current = List.of("line1", "line3", "new-line");
        var result = DocumentSnapshot.diff("direct-test", current);

        sayTable(new String[][]{
            {"Direction", "Lines"},
            {"Added",     String.join(", ", result.added())},
            {"Removed",   String.join(", ", result.removed())}
        });

        assertNotNull(result.added(),   "DiffResult.added() must not be null");
        assertNotNull(result.removed(), "DiffResult.removed() must not be null");
    }
}
