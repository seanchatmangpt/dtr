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
package io.github.seanchatmangpt.dtr.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Saves document lines to a JSON file and diffs current lines against a saved snapshot.
 *
 * <p>Snapshots are stored under {@code target/docs/snapshots/{key}.json} as JSON arrays
 * of strings. The key is sanitized so that only alphanumeric characters and underscores
 * are used in the file name.</p>
 *
 * <p>All public methods are static and never throw — errors are swallowed and reflected
 * in the return value (empty path or empty diff lists).</p>
 *
 * <p>Typical usage inside a DTR doc-test via {@code RenderMachineImpl}:</p>
 * <pre>{@code
 * // Save current document state:
 * var saved = DocumentSnapshot.save("my-snapshot", currentLines);
 * DocumentSnapshot.toMarkdown(saved).forEach(markdownDocument::add);
 *
 * // On next run, diff against the saved state:
 * var diff = DocumentSnapshot.diff("my-snapshot", currentLines);
 * DocumentSnapshot.diffToMarkdown(diff).forEach(markdownDocument::add);
 * }</pre>
 *
 * @since 2026.1.0
 */
public final class DocumentSnapshot {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SNAPSHOTS_DIR = "target/docs/snapshots";

    private DocumentSnapshot() {}

    // -------------------------------------------------------------------------
    // Public result records
    // -------------------------------------------------------------------------

    /**
     * Result of a successful {@link #save} call.
     *
     * @param key       the sanitized snapshot key
     * @param lineCount number of lines persisted
     * @param path      absolute path of the written JSON file, or {@code ""} on error
     */
    public record SaveResult(String key, int lineCount, String path) {}

    /**
     * Result of a {@link #diff} call.
     *
     * @param key         the sanitized snapshot key
     * @param added       lines present in {@code currentLines} but not in the old snapshot
     *                    (order-preserving, first occurrence)
     * @param removed     lines present in the old snapshot but not in {@code currentLines}
     *                    (order-preserving, first occurrence)
     * @param hadPrevious {@code true} if a prior snapshot existed before this diff call
     */
    public record DiffResult(String key, List<String> added, List<String> removed, boolean hadPrevious) {}

    // -------------------------------------------------------------------------
    // Core operations
    // -------------------------------------------------------------------------

    /**
     * Saves {@code lines} as a JSON array to {@code target/docs/snapshots/{key}.json}.
     *
     * <p>The snapshots directory is created if it does not exist. On any I/O or
     * serialization error the method returns a {@link SaveResult} with {@code path=""}.
     *
     * @param key   snapshot identifier (sanitized: non-alphanumeric chars become {@code _})
     * @param lines the document lines to persist
     * @return a {@link SaveResult} describing the outcome
     */
    public static SaveResult save(String key, List<String> lines) {
        var sanitized = sanitize(key);
        try {
            var dir = new File(SNAPSHOTS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            var file = new File(dir, sanitized + ".json");
            var json = MAPPER.writeValueAsString(lines);
            Files.writeString(file.toPath(), json);
            return new SaveResult(sanitized, lines.size(), file.getAbsolutePath());
        } catch (Exception e) {
            return new SaveResult(sanitized, lines.size(), "");
        }
    }

    /**
     * Diffs {@code currentLines} against the previously saved snapshot for {@code key}.
     *
     * <p>The algorithm preserves order and uses set semantics for membership:
     * <ul>
     *   <li>{@code added}   — lines in {@code currentLines} not present in the old snapshot</li>
     *   <li>{@code removed} — lines in the old snapshot not present in {@code currentLines}</li>
     * </ul>
     *
     * <p>If no prior snapshot exists, the current lines are saved and a {@link DiffResult}
     * with empty {@code added}/{@code removed} and {@code hadPrevious=false} is returned.
     * After diffing the current snapshot is always overwritten with {@code currentLines}.</p>
     *
     * @param key          snapshot identifier (sanitized)
     * @param currentLines the document lines representing the current state
     * @return a {@link DiffResult} describing what changed
     */
    public static DiffResult diff(String key, List<String> currentLines) {
        var sanitized = sanitize(key);
        var snapshotFile = Path.of(SNAPSHOTS_DIR, sanitized + ".json");

        if (!Files.exists(snapshotFile)) {
            // No prior snapshot — save and report no diff
            save(sanitized, currentLines);
            return new DiffResult(sanitized, List.of(), List.of(), false);
        }

        List<String> oldLines;
        try {
            var json = Files.readString(snapshotFile);
            var type = MAPPER.getTypeFactory().constructCollectionType(List.class, String.class);
            oldLines = MAPPER.readValue(json, type);
        } catch (Exception e) {
            // Unreadable snapshot — treat as if no previous snapshot
            save(sanitized, currentLines);
            return new DiffResult(sanitized, List.of(), List.of(), false);
        }

        var oldSet = new LinkedHashSet<>(oldLines);
        var currentSet = new LinkedHashSet<>(currentLines);

        // added: in current but not in old (preserve current order)
        var added = new ArrayList<String>();
        for (var line : currentLines) {
            if (!oldSet.contains(line)) {
                added.add(line);
            }
        }

        // removed: in old but not in current (preserve old order)
        var removed = new ArrayList<String>();
        for (var line : oldLines) {
            if (!currentSet.contains(line)) {
                removed.add(line);
            }
        }

        // Overwrite snapshot with current state
        save(sanitized, currentLines);

        return new DiffResult(sanitized, List.copyOf(added), List.copyOf(removed), true);
    }

    // -------------------------------------------------------------------------
    // Markdown rendering helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link SaveResult} to markdown lines suitable for appending to a document.
     *
     * @param result the save result to render
     * @return list of markdown lines (never null)
     */
    public static List<String> toMarkdown(SaveResult result) {
        var lines = new ArrayList<String>();
        lines.add("### Document Snapshot Saved");
        lines.add("");
        lines.add("| Key | Lines | Path |");
        lines.add("|-----|-------|------|");
        var pathDisplay = result.path().isEmpty() ? "(error — not saved)" : result.path();
        lines.add("| %s | %d | %s |".formatted(result.key(), result.lineCount(), pathDisplay));
        lines.add("");
        return List.copyOf(lines);
    }

    /**
     * Converts a {@link DiffResult} to markdown lines suitable for appending to a document.
     *
     * <p>Added and removed line lists are capped at 10 entries each to keep the output readable.</p>
     *
     * @param result the diff result to render
     * @return list of markdown lines (never null)
     */
    public static List<String> diffToMarkdown(DiffResult result) {
        var lines = new ArrayList<String>();
        lines.add("### Document Diff: " + result.key());
        lines.add("");

        if (result.hadPrevious()) {
            lines.add("A previous snapshot was found and compared.");
        } else {
            lines.add("No previous snapshot existed — current state saved as baseline.");
        }
        lines.add("");

        // Added lines
        lines.add("**Added lines (%d):**".formatted(result.added().size()));
        if (result.added().isEmpty()) {
            lines.add("_none_");
        } else {
            var cap = Math.min(result.added().size(), 10);
            for (int i = 0; i < cap; i++) {
                lines.add("- `" + escape(result.added().get(i)) + "`");
            }
            if (result.added().size() > 10) {
                lines.add("- _... and %d more_".formatted(result.added().size() - 10));
            }
        }
        lines.add("");

        // Removed lines
        lines.add("**Removed lines (%d):**".formatted(result.removed().size()));
        if (result.removed().isEmpty()) {
            lines.add("_none_");
        } else {
            var cap = Math.min(result.removed().size(), 10);
            for (int i = 0; i < cap; i++) {
                lines.add("- `" + escape(result.removed().get(i)) + "`");
            }
            if (result.removed().size() > 10) {
                lines.add("- _... and %d more_".formatted(result.removed().size() - 10));
            }
        }
        lines.add("");

        lines.add("**Summary:** %d added, %d removed.".formatted(
                result.added().size(), result.removed().size()));
        lines.add("");

        return List.copyOf(lines);
    }

    // -------------------------------------------------------------------------
    // Internal utilities
    // -------------------------------------------------------------------------

    /** Replaces every non-alphanumeric character in {@code key} with {@code _}. */
    static String sanitize(String key) {
        if (key == null || key.isEmpty()) {
            return "_";
        }
        return key.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /** Escapes backticks in a line so it is safe inside an inline code span. */
    private static String escape(String line) {
        return line.replace("`", "\\`");
    }
}
