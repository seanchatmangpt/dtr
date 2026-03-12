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
package io.github.seanchatmangpt.dtr.metadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immutable metadata record capturing build context, git information, and
 * runtime environment at test execution time.
 *
 * Used for embedding proof of execution in generated documentation (LaTeX,
 * PDF receipts, manifests).
 *
 * Java 26 Enhancement (JEP 516 - AoT Object Caching):
 * The metadata is computed once at JVM startup and cached globally, avoiding
 * repeated external process invocations (mvn -version, git commands, hostname).
 * This eliminates 500ms-2.5s per test suite when running multiple test classes.
 * The cached object graph can be preserved across JVM restarts via Project Leyden
 * CRaC checkpointing.
 */
public record DocMetadata(
    String projectName,
    String projectVersion,
    String buildTimestamp,     // ISO 8601 Instant string
    String javaVersion,
    String mavenVersion,
    String gitCommit,          // git rev-parse HEAD
    String gitBranch,          // git rev-parse --abbrev-ref HEAD
    String gitAuthor,          // git config user.name
    String buildHost,          // hostname or environment
    Map<String, String> systemProperties
) {

    private static final Logger logger = LoggerFactory.getLogger(DocMetadata.class);

    /**
     * Globally cached DocMetadata instance, computed once at class initialization time.
     * JEP 516 optimization: avoids spawning external processes (mvn, git, hostname)
     * for every test class initialization.
     *
     * Access via getInstance() for thread-safe lazy global state.
     */
    private static final DocMetadata CACHED_INSTANCE = computeFromBuild();

    /**
     * Get the globally cached DocMetadata instance.
     * Thread-safe and thread-confined (initialized at class load time).
     *
     * @return the cached metadata for this JVM instance
     */
    public static DocMetadata getInstance() {
        return CACHED_INSTANCE;
    }

    /**
     * Create a DocMetadata instance from the current build/runtime environment.
     * Reads git metadata via git commands, system properties, and Java runtime
     * information.
     *
     * Note: This is called once at JVM startup (class initialization).
     * Do NOT call this repeatedly; use getInstance() instead.
     *
     * @deprecated Use getInstance() for the cached global instance
     */
    @Deprecated(since = "2.5.0", forRemoval = false)
    public static DocMetadata fromBuild() {
        return getInstance();
    }

    /**
     * Internal: Compute metadata from build environment.
     * Called once at class initialization time via static field.
     */
    private static DocMetadata computeFromBuild() {
        return new DocMetadata(
            getProperty("project.name", "unknown"),
            getProperty("project.version", "unknown"),
            Instant.now().toString(),
            System.getProperty("java.version", "unknown"),
            getMavenVersion(),
            getGitCommit(),
            getGitBranch(),
            getGitAuthor(),
            getHostname(),
            captureSystemProperties()
        );
    }

    /**
     * Get a system property with fallback to Maven properties.
     */
    private static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Extract Maven version from system property or environment.
     */
    private static String getMavenVersion() {
        // Maven sets M2_HOME or sets the version in a system property
        String mavenVersion = System.getProperty("maven.version");
        if (mavenVersion != null) {
            return mavenVersion;
        }
        // Try to detect from command execution (mvnd or mvn)
        try {
            var processBuilder = new ProcessBuilder("mvn", "-version");
            var process = processBuilder.start();
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var match = output.split("\n")[0]; // First line typically contains version
            return match.contains("Apache Maven") ? match.trim() : "unknown";
        } catch (IOException e) {
            logger.debug("Could not determine Maven version", e);
            return "unknown";
        }
    }

    /**
     * Get current git commit hash via `git rev-parse HEAD`.
     */
    private static String getGitCommit() {
        try {
            var processBuilder = new ProcessBuilder("git", "rev-parse", "HEAD");
            var process = processBuilder.start();
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return output.trim();
        } catch (IOException e) {
            logger.debug("Could not determine git commit", e);
            return "unknown";
        }
    }

    /**
     * Get current git branch via `git rev-parse --abbrev-ref HEAD`.
     */
    private static String getGitBranch() {
        try {
            var processBuilder = new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD");
            var process = processBuilder.start();
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return output.trim();
        } catch (IOException e) {
            logger.debug("Could not determine git branch", e);
            return "unknown";
        }
    }

    /**
     * Get git user name via `git config user.name`.
     */
    private static String getGitAuthor() {
        try {
            var processBuilder = new ProcessBuilder("git", "config", "user.name");
            var process = processBuilder.start();
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return output.trim();
        } catch (IOException e) {
            logger.debug("Could not determine git author", e);
            return "unknown";
        }
    }

    /**
     * Get hostname via system property or `hostname` command.
     */
    private static String getHostname() {
        String hostname = System.getProperty("hostname");
        if (hostname != null && !hostname.isEmpty()) {
            return hostname;
        }
        try {
            var processBuilder = new ProcessBuilder("hostname");
            var process = processBuilder.start();
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return output.trim();
        } catch (IOException e) {
            return System.getProperty("user.name", "unknown");
        }
    }

    /**
     * Capture relevant system properties into a map for serialization.
     */
    private static Map<String, String> captureSystemProperties() {
        var props = new LinkedHashMap<String, String>();
        var keyNames = new String[] {
            "java.version",
            "java.vendor",
            "java.vm.name",
            "os.name",
            "os.version",
            "os.arch",
            "user.timezone",
            "project.name",
            "project.version",
            "maven.version"
        };

        for (var key : keyNames) {
            String value = System.getProperty(key);
            if (value != null) {
                props.put(key, value);
            }
        }

        return Collections.unmodifiableMap(props);
    }
}
