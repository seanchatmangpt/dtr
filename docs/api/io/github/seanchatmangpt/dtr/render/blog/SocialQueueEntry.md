# `SocialQueueEntry`

> **Package:** `io.github.seanchatmangpt.dtr.render.blog`  

Immutable record representing a single social media queue entry. Used to batch social media content (tweets, LinkedIn posts, etc.) for external publishing APIs.

```java
public record SocialQueueEntry( String platform, String content, String url, String createdAt ) {
    // SocialQueueEntry
}
```

---

## Methods

### `SocialQueueEntry`

Create a new social queue entry with current timestamp.

| Parameter | Description |
| --- | --- |
| `platform` | the social platform |
| `content` | the post content |
| `url` | the target URL |

---

