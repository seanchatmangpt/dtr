# `SlideRenderMachine`

> **Package:** `io.github.seanchatmangpt.dtr.render.slides`  

Slide deck render machine generating presentation decks. Converts test execution into presentation slides (Reveal.js HTML, etc.). Maps say* methods to slide content: sayNextSection → new slide, say → bullets, etc.

```java
public final class SlideRenderMachine extends RenderMachine {
    // SlideRenderMachine
}
```

---

## Methods

### `SlideRenderMachine`

Create a slide render machine with the given template.

| Parameter | Description |
| --- | --- |
| `template` | the slide template (Reveal.js, Marp, etc.) |

---

