/*
 * Copyright (C) 2026 the original author or authors.
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
package io.github.seanchatmangpt.dtr.schemaevolution;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Reads git commit history for a Java class's source file using {@code git log --follow}.
 *
 * <p>Converts a class reference into a relative source path, locates the file under the
 * project root using {@code find}, then shells out to {@code git log --follow} to gather
 * the commit history. The result is a {@link SchemaEvolutionResult} record containing the
 * class name, source path, and an ordered list of {@link CommitEntry} records.</p>
 *
 * <p>All operations are defensive: no exception is ever thrown to the caller. Any error
 * (git unavailable, file not found, process timeout) results in an empty commit list.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * var result = SchemaEvolutionReader.read(MyRecord.class, "/home/user/dtr");
 * SchemaEvolutionReader.toMarkdown(result).forEach(ctx::sayRaw);
 * }</pre>
 *
 * @since 2026.1.0
 */
public final class SchemaEvolutionReader {

    private static final int MAX_DISPLAY_ENTRIES = 20;
    private static final int PROCESS_TIMEOUT_SECONDS = 10;

    private SchemaEvolutionReader() {}

    // -------------------------------------------------------------------------
    // Public records
    // -------------------------------------------------------------------------

    /**
     * A single commit entry from {@code git log --follow}.
     *
     * @param hash    short commit hash (7 chars)
     * @param date    author date in ISO short format (YYYY-MM-DD)
     * @param author  git author name
     * @param subject commit subject line (first line of message)
     */
    public record CommitEntry(String hash, String date, String author, String subject) {}

    /**
     * The full result of reading schema evolution history for one class.
     *
     * @param className      fully-qualified class name (e.g., {@code io.github.example.Foo})
     * @param commits        ordered list of commits, newest-first; empty when git is unavailable
     * @param sourceFilePath relative source path used in the git query (e.g., {@code src/main/java/.../Foo.java}),
     *                       or the computed path when the file was not found on disk
     */
    public record SchemaEvolutionResult(String className, List<CommitEntry> commits, String sourceFilePath) {}

    // -------------------------------------------------------------------------
    // Core read method
    // -------------------------------------------------------------------------

    /**
     * Reads the git commit history for the source file corresponding to {@code clazz}.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Compute the relative source path from the class name:
     *       {@code com.example.Foo} → {@code com/example/Foo.java}</li>
     *   <li>Search for the file under {@code projectRoot/src} using {@code find}.</li>
     *   <li>Run {@code git -C <projectRoot> log --follow --pretty=format:"%h|%ad|%an|%s"
     *       --date=short -- <relPath>} to collect history.</li>
     *   <li>Parse each output line by splitting on {@code |} with limit 4.</li>
     * </ol>
     *
     * <p>Returns an empty commit list — not an exception — when git is unavailable,
     * the file cannot be found, or the process times out after {@value PROCESS_TIMEOUT_SECONDS}
     * seconds.</p>
     *
     * @param clazz       the class whose source file to trace; must not be null
     * @param projectRoot filesystem path to the git repository root (may be relative or absolute)
     * @return a non-null {@link SchemaEvolutionResult}; commits list may be empty but never null
     */
    public static SchemaEvolutionResult read(Class<?> clazz, String projectRoot) {
        var className = clazz.getName();
        // Compute the canonical relative source path from the class name
        var relSourcePath = className.replace('.', '/') + ".java";
        var simpleFileName = clazz.getSimpleName() + ".java";

        try {
            var projectRootFile = new File(projectRoot);

            // Locate the actual file path under src/ using find
            var foundPath = findSourceFile(simpleFileName, projectRootFile);
            // Use the found path relative to projectRoot if available; else fall back to computed path
            var gitRelPath = foundPath != null ? foundPath : relSourcePath;

            var commits = runGitLog(projectRootFile, gitRelPath);
            return new SchemaEvolutionResult(className, commits, gitRelPath);

        } catch (Exception ignored) {
            return new SchemaEvolutionResult(className, List.of(), relSourcePath);
        }
    }

    // -------------------------------------------------------------------------
    // Markdown rendering
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link SchemaEvolutionResult} into a list of Markdown lines.
     *
     * <p>Output includes a heading, a note about the source path, and a Markdown table
     * with columns {@code Hash}, {@code Date}, {@code Author}, {@code Subject}. When
     * no history is available a note is rendered instead of the table. Display is capped
     * at {@value MAX_DISPLAY_ENTRIES} entries.</p>
     *
     * @param result a non-null {@link SchemaEvolutionResult}
     * @return ordered list of Markdown-formatted strings; never null, never empty
     */
    public static List<String> toMarkdown(SchemaEvolutionResult result) {
        var lines = new ArrayList<String>();

        lines.add("### Schema Evolution: %s".formatted(result.className()));
        lines.add("");
        lines.add("**Source path:** `%s`".formatted(result.sourceFilePath()));
        lines.add("");

        if (result.commits().isEmpty()) {
            lines.add("> [!NOTE]");
            lines.add("> No git history found for this class. "
                    + "This is expected in shallow clones or environments without git access.");
            return List.copyOf(lines);
        }

        var displayCommits = result.commits().stream()
                .limit(MAX_DISPLAY_ENTRIES)
                .toList();

        lines.add("| Hash | Date | Author | Subject |");
        lines.add("|------|------|--------|---------|");
        for (var entry : displayCommits) {
            // Escape pipe characters in the subject so Markdown tables render correctly
            var safeSubject = entry.subject().replace("|", "\\|");
            lines.add("| `%s` | %s | %s | %s |".formatted(
                    entry.hash(), entry.date(), entry.author(), safeSubject));
        }

        int total = result.commits().size();
        if (total > MAX_DISPLAY_ENTRIES) {
            lines.add("");
            lines.add("_Showing %d of %d commits._".formatted(MAX_DISPLAY_ENTRIES, total));
        }

        return List.copyOf(lines);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Uses {@code find} to locate a Java source file by simple name under
     * {@code projectRoot/src}. Returns a path relative to {@code projectRoot},
     * or {@code null} when the file is not found or the process fails.
     */
    private static String findSourceFile(String simpleFileName, File projectRootFile) {
        try {
            var srcDir = new File(projectRootFile, "src");
            if (!srcDir.isDirectory()) {
                // Some modules may not have a local src dir; try parent or root
                srcDir = projectRootFile;
            }
            var pb = new ProcessBuilder("find", srcDir.getAbsolutePath(), "-name", simpleFileName);
            pb.redirectErrorStream(true);
            var process = pb.start();

            String found = null;
            try (var reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var trimmed = line.strip();
                    if (!trimmed.isEmpty()) {
                        found = trimmed;
                        break; // take the first match
                    }
                }
            }

            process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (found == null) {
                return null;
            }

            // Make it relative to projectRoot so git can resolve it
            var absRoot = projectRootFile.getCanonicalPath();
            var absFound = new File(found).getCanonicalPath();
            if (absFound.startsWith(absRoot + File.separator)) {
                return absFound.substring(absRoot.length() + 1);
            }
            return absFound;

        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Runs {@code git -C <projectRoot> log --follow --pretty=format:"%h|%ad|%an|%s"
     * --date=short -- <relPath>} and parses the output into a list of {@link CommitEntry}
     * records. Returns an empty list on any error or timeout.
     */
    private static List<CommitEntry> runGitLog(File projectRootFile, String relPath) {
        try {
            var pb = new ProcessBuilder(
                    "git", "-C", projectRootFile.getAbsolutePath(),
                    "log",
                    "--follow",
                    "--pretty=format:%h|%ad|%an|%s",
                    "--date=short",
                    "--", relPath
            );
            pb.redirectErrorStream(true);

            var process = pb.start();
            var entries = new ArrayList<CommitEntry>();

            try (var reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var trimmed = line.strip();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    // Split on | with limit=4 so subject (which may contain |) is preserved
                    var parts = trimmed.split("\\|", 4);
                    if (parts.length == 4) {
                        entries.add(new CommitEntry(
                                parts[0].strip(),
                                parts[1].strip(),
                                parts[2].strip(),
                                parts[3].strip()));
                    } else if (parts.length == 3) {
                        // Subject may be empty
                        entries.add(new CommitEntry(
                                parts[0].strip(),
                                parts[1].strip(),
                                parts[2].strip(),
                                ""));
                    }
                    // Lines with fewer than 3 parts are malformed — skip silently
                }
            }

            boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return List.of();
            }

            return List.copyOf(entries);

        } catch (Exception ignored) {
            return List.of();
        }
    }
}
