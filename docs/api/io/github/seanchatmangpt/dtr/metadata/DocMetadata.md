# `DocMetadata`

> **Package:** `io.github.seanchatmangpt.dtr.metadata`  

Immutable metadata record capturing build context, git information, and runtime environment at test execution time. Used for embedding proof of execution in generated documentation (LaTeX, PDF receipts, manifests). Java 26 Enhancement (JEP 516 - AoT Object Caching): The metadata is computed once at JVM startup and cached globally, avoiding repeated external process invocations (mvn -version, git commands, hostname). This eliminates 500ms-2.5s per test suite when running multiple test classes. The cached object graph can be preserved across JVM restarts via Project Leyden CRaC checkpointing.

```java
public record DocMetadata( String projectName, String projectVersion, String buildTimestamp, // ISO 8601 Instant string String javaVersion, String mavenVersion, String gitCommit, // git rev-parse HEAD String gitBranch, // git rev-parse --abbrev-ref HEAD String gitAuthor, // git config user.name String buildHost, // hostname or environment Map<String, String> systemProperties ) {
    // getInstance, fromBuild, computeFromBuild, getProperty, getMavenVersion, getGitCommit, getGitBranch, getGitAuthor, ... (10 total)
}
```

---

## Methods

### `captureSystemProperties`

Capture relevant system properties into a map for serialization.

---

### `computeFromBuild`

Internal: Compute metadata from build environment. Called once at class initialization time via static field.

---

### `fromBuild`

Create a DocMetadata instance from the current build/runtime environment. Reads git metadata via git commands, system properties, and Java runtime information. Note: This is called once at JVM startup (class initialization). Do NOT call this repeatedly; use getInstance() instead.

> [!WARNING]
> **Deprecated:** Use getInstance() for the cached global instance

---

### `getGitAuthor`

Get git user name via `git config user.name`.

---

### `getGitBranch`

Get current git branch via `git rev-parse --abbrev-ref HEAD`.

---

### `getGitCommit`

Get current git commit hash via `git rev-parse HEAD`.

---

### `getHostname`

Get hostname via system property or `hostname` command.

---

### `getInstance`

Get the globally cached DocMetadata instance. Thread-safe and thread-confined (initialized at class load time).

> **Returns:** the cached metadata for this JVM instance

---

### `getMavenVersion`

Extract Maven version from system property or environment.

---

### `getProperty`

Get a system property with fallback to Maven properties.

---

