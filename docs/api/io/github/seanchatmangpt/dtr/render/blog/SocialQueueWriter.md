# `SocialQueueWriter`

> **Package:** `io.github.seanchatmangpt.dtr.render.blog`  

Writes social media queue entries to JSON for batch publishing. Generates social-queue.json containing tweets, LinkedIn posts, and other platform-specific content ready for external APIs (Twitter, LinkedIn, etc.).

```java
public final class SocialQueueWriter {
    // writeSocialQueue, SocialQueue
}
```

---

## Methods

### `SocialQueue`

Create a new social queue with current timestamp.

| Parameter | Description |
| --- | --- |
| `docTest` | the doc test name |
| `entries` | the queue entries |

---

### `writeSocialQueue`

Write social queue entries to JSON file.

| Parameter | Description |
| --- | --- |
| `docTestName` | the name of the doc test (for organization) |
| `tweetables` | list of tweetable content (≤280 chars each) |
| `tldr` | the TL;DR summary for social previews |
| `cta` | the call-to-action URL |

---

