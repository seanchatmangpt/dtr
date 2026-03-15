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
package io.github.seanchatmangpt.dtr.githotspot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzes Git commit history for Java source files to identify maintenance hotspots.
 *
 * <p>A hotspot is a source file that has been modified many times by many authors —
 * a reliable signal that the file is complex, frequently broken, or a shared boundary
 * between subsystems. This class uses {@code git log --follow} to track renames and
 * aggregates commit frequency plus author churn into a {@link HotspotResult}.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * var result = GitHotspotAnalyzer.analyze(DtrContext.class, "/home/user/dtr");
 * if (result.found()) {
 *     sayTable(GitHotspotAnalyzer.toTable(result));
 * }
 * }</pre>
 */
public final class GitHotspotAnalyzer {

    private static final int MAX_COMMITS = 100;

    private GitHotspotAnalyzer() {}

    /**
     * Per-author commit contribution within a single source file's history.
     *
     * @param author  the Git author name
     * @param commits number of commits attributed to this author for the file
     */
    public record AuthorStat(String author, int commits) {}

    /**
     * Complete hotspot analysis result for one source file.
     *
     * @param sourceFile   relative source path used in the git query (e.g., {@code io/github/.../Foo.java})
     * @param totalCommits total number of commits touching this file (capped at {@value MAX_COMMITS})
     * @param firstCommit  date string of the earliest commit, or empty if unavailable
     * @param lastCommit   date string of the most recent commit, or empty if unavailable
     * @param topAuthors   authors sorted by descending commit count
     * @param found        {@code true} when git was reachable and the file has recorded history
     */
    public record HotspotResult(
            String sourceFile,
            int totalCommits,
            String firstCommit,
            String lastCommit,
            List<AuthorStat> topAuthors,
            boolean found) {}

    /**
     * Runs {@code git log --follow} against {@code projectRoot} to analyze the commit
     * history of the source file corresponding to {@code clazz}.
     *
     * <p>The class is converted to a relative path of the form
     * {@code io/github/example/ClassName.java}. The git query uses a glob pattern
     * so the file can sit anywhere under the project root (e.g., inside a module).</p>
     *
     * <p>Output is capped at {@value MAX_COMMITS} commits to bound execution time.</p>
     *
     * @param clazz       the Java class whose source file to look up
     * @param projectRoot absolute path to the git repository root
     * @return a {@link HotspotResult}; {@code found=false} on any error or missing history
     */
    public static HotspotResult analyze(Class<?> clazz, String projectRoot) {
        var sourceRelPath = toSourceRelPath(clazz);
        var fileName = clazz.getSimpleName() + ".java";

        try {
            var pb = new ProcessBuilder(
                    "git", "log",
                    "--follow",
                    "--max-count=" + MAX_COMMITS,
                    "--format=%H|%an|%ad|%s",
                    "--date=short",
                    "--", "**/" + fileName
            );
            pb.directory(new java.io.File(projectRoot));
            pb.redirectErrorStream(true);

            var process = pb.start();
            var lines = new ArrayList<String>();

            try (var reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var trimmed = line.strip();
                    if (!trimmed.isEmpty()) {
                        lines.add(trimmed);
                    }
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0 || lines.isEmpty()) {
                return notFound(sourceRelPath);
            }

            // Parse lines: "%H|%an|%ad|%s"
            // Subject may itself contain '|', so split with limit=4
            var authorCounts = new LinkedHashMap<String, Integer>();
            String firstCommit = "";
            String lastCommit = "";

            for (int i = 0; i < lines.size(); i++) {
                var parts = lines.get(i).split("\\|", 4);
                if (parts.length < 3) {
                    continue;
                }
                var author = parts[1].strip();
                var date = parts[2].strip();

                authorCounts.merge(author, 1, Integer::sum);

                // lines are newest-first from git log
                if (i == 0) {
                    lastCommit = date;
                }
                if (i == lines.size() - 1) {
                    firstCommit = date;
                }
            }

            var topAuthors = authorCounts.entrySet().stream()
                    .map(e -> new AuthorStat(e.getKey(), e.getValue()))
                    .sorted((a, b) -> Integer.compare(b.commits(), a.commits()))
                    .toList();

            return new HotspotResult(
                    sourceRelPath,
                    lines.size(),
                    firstCommit,
                    lastCommit,
                    topAuthors,
                    true
            );

        } catch (Exception e) {
            return notFound(sourceRelPath);
        }
    }

    /**
     * Converts the analysis result into a 2D table suitable for {@code sayTable()}.
     *
     * <p>The first row is headers. Subsequent rows contain the summary metrics and
     * one row per author (up to the top 5).</p>
     *
     * @param result a {@link HotspotResult} from {@link #analyze}
     * @return a {@code String[][]} ready for {@code sayTable()}
     */
    public static String[][] toTable(HotspotResult result) {
        if (!result.found()) {
            return new String[][] {
                {"Source File", "Status"},
                {result.sourceFile(), "No git history found"}
            };
        }

        var rows = new ArrayList<String[]>();
        rows.add(new String[]{"Metric", "Value"});
        rows.add(new String[]{"Source File", result.sourceFile()});
        rows.add(new String[]{"Total Commits", String.valueOf(result.totalCommits())});
        rows.add(new String[]{"First Commit", result.firstCommit()});
        rows.add(new String[]{"Last Commit", result.lastCommit()});
        rows.add(new String[]{"Unique Authors", String.valueOf(result.topAuthors().size())});

        if (!result.topAuthors().isEmpty()) {
            rows.add(new String[]{"", ""});
            rows.add(new String[]{"Author", "Commits"});
            var cap = Math.min(result.topAuthors().size(), 5);
            for (int i = 0; i < cap; i++) {
                var stat = result.topAuthors().get(i);
                rows.add(new String[]{stat.author(), String.valueOf(stat.commits())});
            }
        }

        return rows.toArray(new String[0][]);
    }

    /**
     * Formats the analysis result as a list of Markdown lines for narrative reporting.
     *
     * @param result a {@link HotspotResult} from {@link #analyze}
     * @return ordered list of Markdown-formatted strings
     */
    public static List<String> toMarkdown(HotspotResult result) {
        if (!result.found()) {
            return List.of(
                    "**Source file:** `%s`".formatted(result.sourceFile()),
                    "No git history found for this file in the specified project root."
            );
        }

        var lines = new ArrayList<String>();
        lines.add("**Source file:** `%s`".formatted(result.sourceFile()));
        lines.add("**Total commits:** %d".formatted(result.totalCommits()));
        lines.add("**First commit:** %s".formatted(result.firstCommit()));
        lines.add("**Last commit:** %s".formatted(result.lastCommit()));
        lines.add("**Unique authors:** %d".formatted(result.topAuthors().size()));

        if (!result.topAuthors().isEmpty()) {
            lines.add("");
            lines.add("Top contributors:");
            var cap = Math.min(result.topAuthors().size(), 5);
            for (int i = 0; i < cap; i++) {
                var stat = result.topAuthors().get(i);
                lines.add("- %s (%d commits)".formatted(stat.author(), stat.commits()));
            }
        }

        return List.copyOf(lines);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a class to a relative source path, e.g.
     * {@code io/github/seanchatmangpt/dtr/junit5/DtrContext.java}.
     */
    private static String toSourceRelPath(Class<?> clazz) {
        return clazz.getName().replace('.', '/') + ".java";
    }

    private static HotspotResult notFound(String sourceRelPath) {
        return new HotspotResult(sourceRelPath, 0, "", "", List.of(), false);
    }
}
