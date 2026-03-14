# `MultiRenderMachine`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine`  

Delegating render machine that routes method calls to multiple machines simultaneously using virtual threads and structured concurrency. <p>Enables multi-format output: one test execution produces Markdown, LaTeX, PDF, slides, and blog posts in parallel by dispatching each {@code say*} call to all contained machines concurrently.</p> <p>Transparent to test code: use exactly like a single RenderMachine.</p> <p><strong>Java 25 showcase — structured concurrency:</strong> Each dispatch uses {@link StructuredTaskScope} (JEP 492). This replaces manual future waiting and error aggregation with JVM-native structured semantics: if any renderer fails, the error is propagated immediately with full context. This is simpler, safer, and faster than CompletableFuture chains.</p> <p><strong>Java 25/26 showcase — virtual threads:</strong> All tasks run on virtual threads (Project Loom) which are JVM-scheduled, not OS-scheduled. They have near-zero creation overhead, making one-virtual-thread-per-machine-per-call practical even for high-frequency {@code say*} calls. When DTR generates 8+ simultaneous output formats, wall-clock time is the slowest single renderer, not the sum of all renderers.</p> <p><strong>Java 25 Enhancement:</strong> Structured concurrency with StructuredTaskScope ensures all rendering tasks are properly managed: if any renderer fails mid-rendering, the exception is propagated immediately, and all tasks are cleaned up properly.</p> <p>Example:</p> <pre>{@code RenderMachine multiMachine = new MultiRenderMachine(     new RenderMachineImpl(),      // Markdown     new RenderMachineLatex(...)   // LaTeX/PDF ); }</pre>

```java
public final class MultiRenderMachine extends RenderMachine {
    // MultiRenderMachine, MultiRenderMachine, MultiRenderMachine, dispatchToAll, finishAndWriteOut, MultiRenderException, getCauses
}
```

---

## Methods

### `MultiRenderException`

Constructs an exception that aggregates one or more render-machine failures.

| Parameter | Description |
| --- | --- |
| `message` | human-readable summary of the failure |
| `causes` | all exceptions thrown by individual render machines |

---

### `MultiRenderMachine`

Create a multi-render machine delegating to the given machines (varargs).

| Parameter | Description |
| --- | --- |
| `machines` | the render machines to dispatch to |

---

### `dispatchToAll`

Dispatches an action to all contained render machines concurrently using structured concurrency. <p>Java 25 Enhancement (JEP 492 - Structured Concurrency):</p> <ol>   <li>Creates a {@link StructuredTaskScope} scope</li>   <li>Forks one task per machine — each task calls the action on its machine via a virtual thread</li>   <li>Joins when all tasks complete (blocks until all finish or an exception is thrown)</li>   <li>Propagates exceptions with full structured context</li> </ol> <p>This replaces the previous manual future-waiting pattern with JVM-native semantics. StructuredTaskScope ensures:</p> <ul>   <li><strong>No error swallowing:</strong> If any renderer fails, the failure is immediate</li>   <li><strong>Automatic cleanup:</strong> All tasks are guaranteed to complete before method returns</li>   <li><strong>Structured lifetime:</strong> Try-with-resources ensures proper scope closure</li>   <li><strong>Zero boilerplate:</strong> Simple, declarative concurrency patterns</li> </ul>

| Parameter | Description |
| --- | --- |
| `action` | the operation to invoke on each render machine |

| Exception | Description |
| --- | --- |
| `MultiRenderException` | if any machine fails (wraps first exception) |

---

### `finishAndWriteOut`

Finalizes all contained render machines concurrently using virtual threads. <p>This is where virtual threads provide the most measurable performance benefit: file I/O is blocking. A LaTeX compilation can take several seconds. Running all finalizations in parallel means the total wall-clock time is the slowest single finalizer, not the sum of all.</p>

---

### `getCauses`

All exceptions from failing render machines. */

---

