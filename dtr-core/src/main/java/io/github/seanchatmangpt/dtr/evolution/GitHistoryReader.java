package io.github.seanchatmangpt.dtr.evolution;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads git commit history for a specific class file using {@link ProcessBuilder}.
 *
 * <p>Follows the exact pattern established by {@code DocMetadata}'s git helper
 * methods — shells out to git, captures output, returns empty list on any failure
 * so tests never fail due to git unavailability.</p>
 */
public final class GitHistoryReader {

    private GitHistoryReader() {}

    /**
     * A single git commit entry.
     *
     * @param hash    short commit hash (7 chars)
     * @param date    author date in ISO format
     * @param author  author name
     * @param subject commit subject line
     */
    public record GitEntry(String hash, String date, String author, String subject) {}

    /**
     * Returns up to {@code maxEntries} commits that touched the source file for
     * the given class. Returns an empty list if git is unavailable or the file
     * cannot be found in git history.
     *
     * @param clazz      the class whose source file history to retrieve
     * @param maxEntries maximum number of commits to return
     * @return list of git entries, newest first
     */
    public static List<GitEntry> read(Class<?> clazz, int maxEntries) {
        String fileName = clazz.getSimpleName() + ".java";
        try {
            var process = new ProcessBuilder(
                    "git", "log",
                    "--follow",
                    "--pretty=format:%h|%as|%an|%s",
                    "-n", String.valueOf(maxEntries),
                    "--", "**/" + fileName)
                    .redirectErrorStream(true)
                    .start();

            var entries = new ArrayList<GitEntry>();
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var parts = line.split("\\|", 4);
                    if (parts.length == 4) {
                        entries.add(new GitEntry(parts[0], parts[1], parts[2], parts[3]));
                    }
                }
            }
            process.waitFor();
            return entries;
        } catch (Exception e) {
            return List.of();
        }
    }
}
