# `DtrContext`

> **Package:** `io.github.seanchatmangpt.dtr.junit5`  

Context object for JUnit 5 DTR tests. <p>Provides access to all DTR functionality within JUnit 5 test methods. Can be injected as a parameter into test methods when using {@link DtrExtension}. <p>Usage: <pre>{@code @ExtendWith(DtrExtension.class) class MyApiDocTest {     @Test     void testGetUsers(DtrContext ctx) {         ctx.sayNextSection("User API");         ctx.say("Documentation for User API goes here.");     } } }</pre>

```java
public class DtrContext implements RenderMachineCommands {
    // DtrContext, sayRef, sayDocCoverage, saySlideOnly, sayDocOnly, saySpeakerNote, sayHeroImage, sayTweetable, ... (15 total)
}
```

---

## Methods

### `DtrContext`

Creates a new DtrContext.

| Parameter | Description |
| --- | --- |
| `renderMachine` | the render machine for documentation output |

---

### `getRenderMachine`

Gets the underlying RenderMachine.

> **Returns:** the render machine

---

### `sayAndAssertThat`

Overload for {@code boolean} primitives.

| Parameter | Description |
| --- | --- |
| `label` | the check description |
| `actual` | the actual value |
| `matcher` | the Hamcrest matcher |

---

### `sayCallToAction`

Renders a call-to-action link for blogs.

| Parameter | Description |
| --- | --- |
| `url` | the URL for the CTA button/link |

---

### `sayDocCoverage`

Renders a documentation coverage report for the given classes, using the set of method names tracked during this test via say* calls.

| Parameter | Description |
| --- | --- |
| `classes` | the classes whose public API to check for coverage |

---

### `sayDocOnly`

Renders content only for documentation/blog output (ignored by slide render machines).

| Parameter | Description |
| --- | --- |
| `text` | the text to render in docs only |

---

### `sayHeroImage`

Renders a hero image for blogs and slides (ignored by other formats).

| Parameter | Description |
| --- | --- |
| `altText` | the alt text for the image |

---

### `sayRef`

Convenience method to create and render a cross-reference to another DocTest's section. <p>Creates a {@link DocTestRef} and delegates to {@link #sayRef(DocTestRef)}. The reference is rendered as a markdown link in Markdown mode or as a LaTeX cross-reference command in LaTeX mode. In LaTeX, the resolved section number (e.g., "Section 3.2") is automatically substituted after compilation.</p>

| Parameter | Description |
| --- | --- |
| `docTestClass` | the target DocTest class (must not be null) |
| `anchor` | the section/anchor name within that DocTest, e.g., "user-registration"               (typically derived from @DocSection annotation value) |

---

### `saySlideOnly`

Renders content only for slide output (ignored by markdown/blog render machines).

| Parameter | Description |
| --- | --- |
| `text` | the text to render on slides only |

---

### `saySpeakerNote`

Renders speaker notes for slides (ignored by doc/blog render machines).

| Parameter | Description |
| --- | --- |
| `text` | the speaker notes text |

---

### `sayTldr`

Renders a TLDR (too long; didn't read) summary for blogs.

| Parameter | Description |
| --- | --- |
| `text` | the summary text |

---

### `sayTweetable`

Renders a tweetable excerpt (≤280 chars) for social media queue.

| Parameter | Description |
| --- | --- |
| `text` | the text to tweet (will be truncated to 280 chars) |

---

