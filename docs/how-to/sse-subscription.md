# How-To: Document Evolution Timelines with sayEvolutionTimeline

Generate a git log timeline for any Java class using DTR 2.6.0's `sayEvolutionTimeline` method.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayEvolutionTimeline Does

`sayEvolutionTimeline(Class<?> clazz, int maxEntries)` uses `git log` to find commits that touched the source file corresponding to the given class. It renders the result as a Mermaid timeline diagram showing:

- Commit date
- Author
- Commit message summary
- Number of lines changed

This replaces SSE subscription guides, which relied on the removed HTTP stack. Evolution timelines are useful for tracking when and why a class changed over time — essential context for documentation and maintenance.

---

## Basic Usage

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class ClassEvolutionDocTest {

    @Test
    void documentUserServiceEvolution(DtrContext ctx) {
        ctx.sayNextSection("UserService Evolution Timeline");
        ctx.say("The following timeline shows the last 10 significant changes to UserService, " +
                "providing context for its current design decisions.");

        ctx.sayEvolutionTimeline(UserService.class, 10);

        ctx.sayNote("Each entry shows the commit date, author, and a summary of changes. " +
                    "Use this to understand the history behind API decisions.");
    }
}
```

---

## Track Multiple Classes

Document the evolution of related classes together:

```java
@Test
void documentDomainEvolution(DtrContext ctx) {
    ctx.sayNextSection("Domain Model Evolution");
    ctx.say("Timeline of changes to core domain classes over the last 5 commits each:");

    ctx.say("**User:**");
    ctx.sayEvolutionTimeline(User.class, 5);

    ctx.say("**Order:**");
    ctx.sayEvolutionTimeline(Order.class, 5);

    ctx.say("**Product:**");
    ctx.sayEvolutionTimeline(Product.class, 5);
}
```

---

## Combine Evolution with Current State

Pair the timeline with the current class diagram to show what it looked like at the end of its evolution:

```java
@Test
void documentCurrentAndHistory(DtrContext ctx) {
    ctx.sayNextSection("PaymentService: History and Current State");

    ctx.say("**Commit history** (last 8 changes):");
    ctx.sayEvolutionTimeline(PaymentService.class, 8);

    ctx.say("**Current class structure:**");
    ctx.sayClassDiagram(PaymentService.class);

    ctx.say("**Documentation coverage:**");
    ctx.sayDocCoverage(PaymentService.class);
}
```

---

## Track an API's Evolution During a Migration

When performing a major refactor, document the timeline before and after:

```java
@Test
void documentMigrationHistory(DtrContext ctx) {
    ctx.sayNextSection("OrderService Migration History");
    ctx.say("OrderService was migrated from DTR 2.5.x HTTP stack to java.net.http.HttpClient " +
            "in version 2.6.0. The following timeline shows the migration commits:");

    ctx.sayEvolutionTimeline(OrderService.class, 15);

    ctx.sayNote("The migration removed all sayAndMakeRequest calls and replaced them " +
                "with java.net.http.HttpClient invocations.");
}
```

---

## Document a Record's Schema and History

Records are the primary data type in DTR tests. Document both what they contain and how they got there:

```java
record User(long id, String name, String email, boolean active, java.time.Instant createdAt) {}

@Test
void documentUserRecord(DtrContext ctx) {
    ctx.sayNextSection("User Record: Schema and History");

    ctx.say("**Current schema:**");
    ctx.sayRecordComponents(User.class);

    ctx.say("**Evolution** (last 6 commits):");
    ctx.sayEvolutionTimeline(User.class, 6);

    ctx.sayNote("Fields were added incrementally. " +
                "The `active` flag replaced a `status` enum in commit 3. " +
                "The `createdAt` field was added in commit 5 for audit compliance.");
}
```

---

## Best Practices

**Use maxEntries = 10 as a default.** More than 15 entries makes the timeline too long to scan. Use fewer entries for stable classes, more for rapidly evolving ones.

**Combine with sayDocCoverage.** A class with a long evolution history but low documentation coverage needs attention. Show both in the same test.

**Add narrative context.** `sayEvolutionTimeline` shows the what; use `ctx.say()` to explain the why. Describe the major phases of evolution in prose.

**Run in CI.** Include evolution timeline tests in your documentation CI pipeline so stakeholders see up-to-date history on every build.

**Requires git.** The method shells out to `git log`. It produces an empty result in repositories without git history or in detached checkout environments.

---

## See Also

- [Render ASCII Charts](sse-parsing.md) — sayAsciiChart for numerical data visualization
- [Benchmark Inline](sse-reconnection.md) — sayBenchmark for performance documentation
- [Generate Class Diagrams](websockets-broadcast.md) — sayClassDiagram for current structure
