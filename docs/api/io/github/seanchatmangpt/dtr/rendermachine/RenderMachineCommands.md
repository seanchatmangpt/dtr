# `RenderMachineCommands`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine`  
> **Since:** `1.0`  

Core documentation-output contract for the DTR render machine. <p>Every {@code say*} method maps to a distinct documentation primitive (paragraph, heading, table, code block, etc.).  Implementations route the calls to one or more output engines (Markdown, LaTeX, HTML, blog, …).

```java
public interface RenderMachineCommands {
    // say, sayNextSection, sayRaw, sayTable, sayCode, sayWarning, sayNote, sayKeyValue, ... (58 total)
}
```

---

## Methods

### `say`

A text that will be rendered as a paragraph in the documentation. No escaping is done. You can use markdown formatting inside the text.

| Parameter | Description |
| --- | --- |
| `text` | A text that may contain markdown formatting like "This is my **bold** text". |

---

### `sayActorMessages`

Documents actor-model message passing: which actors send what messages to which targets. Renders a Mermaid sequence diagram showing concurrent asynchronous communication without shared state. <p>Blue ocean: Joe Armstrong's "share nothing" discipline made visible — every message hop is explicit, auditable, and always up to date.</p>

| Parameter | Description |
| --- | --- |
| `title` | diagram title |
| `actors` | actor names in order of appearance |
| `messages` | list of {@code [sender, receiver, message]} triples |

---

### `sayAgentLoop`

Documents an AI agent's reasoning loop: observations (inputs the agent perceives), decisions (actions it chose), and tools (external calls it made). Renders a sequence diagram showing the agent ↔ environment interaction over one full loop iteration. <p>Blue ocean: the first documentation primitive designed specifically for agentic AI workflows — no other documentation framework has this.</p>

| Parameter | Description |
| --- | --- |
| `agentName` | name of the agent (e.g. "DTR Documentation Agent") |
| `observations` | what the agent observed (in order) |
| `decisions` | what the agent decided (in order) |
| `tools` | external tool calls the agent made (in order) |

---

### `sayAndon`

Documents a Toyota Andon-cord production-status board: each workstation and its current status. Renders a table with status-coded rows (✅ Normal / ⚠️ Caution / ❌ Stopped) and a station-health summary. <p>Blue ocean: production dashboards are ephemeral; this generates a point-in-time snapshot in the documentation record.</p>

| Parameter | Description |
| --- | --- |
| `system` | system or line name (e.g. "Payment Processing Line") |
| `stations` | station / service names |
| `statuses` | status strings: "NORMAL", "CAUTION", or "STOPPED" |

---

### `sayAnnotationProfile`

Documents all annotations on a class and its methods using reflection. <p>Blue ocean innovation: renders the complete annotation landscape of any class — class-level annotations and per-method annotations in a structured table. No manual listing, no drift — derived from the bytecode at test runtime.</p>

| Parameter | Description |
| --- | --- |
| `clazz` | the class to inspect for annotations |

---

### `sayApiDiff`

Computes the semantic diff between two class versions (e.g. old API vs. new API) using reflection. Produces three tables: added methods, removed methods, and signature-changed methods. <p>Blue ocean: breaking-change documentation that is automatically complete — no manual "CHANGELOG" entries needed.</p>

| Parameter | Description |
| --- | --- |
| `before` | class representing the previous API version |
| `after` | class representing the new API version |

---

### `sayAsciiChart`

Renders an inline ASCII horizontal bar chart for numeric data. Bars are drawn with Unicode block characters ({@code ████}) normalized to the maximum value. No external dependencies — pure Java string math.

| Parameter | Description |
| --- | --- |
| `label` | the chart title |
| `values` | the numeric values (one bar per value) |
| `xLabels` | labels for each bar (must have the same length as {@code values}) |

---

### `sayAssertions`

Renders assertion results in a table format with Check and Result columns.

| Parameter | Description |
| --- | --- |
| `assertions` | A map where keys are check descriptions and values are results. |

---

### `sayBenchmark`

Measures the given task with explicit warmup and measurement round counts.

| Parameter | Description |
| --- | --- |
| `label` | a human-readable label for the benchmark |
| `task` | the code to benchmark |
| `warmupRounds` | number of warmup iterations (discarded from results) |
| `measureRounds` | number of measured iterations |

---

### `sayCallGraph`

Renders a Mermaid {@code graph LR} showing all method-to-method call relationships in the given class, extracted from InvokeOp nodes in each method's Java 26 Code Reflection IR. Only methods annotated with {@code @CodeReflection} contribute edges.

| Parameter | Description |
| --- | --- |
| `clazz` | the class whose call graph to render |

---

### `sayCallSite`

Documents the current call site using {@link StackWalker}. <p>Blue ocean innovation: the documentation knows exactly where it was written. Every section generated by this call carries provenance — class, method, line number — derived from the live JVM stack at the moment of invocation.</p> <p>Uses {@code StackWalker.getInstance(RETAIN_CLASS_REFERENCE)} to walk the call stack and find the first frame outside of DTR's own machinery.</p>

---

### `sayCite`

Renders a citation reference with page number specification.

| Parameter | Description |
| --- | --- |
| `citationKey` | The BibTeX citation key to reference. |
| `pageRef` | The page reference (e.g., "42" or "pp. 10-15"). |

---

### `sayClassDiagram`

Auto-generates a Mermaid {@code classDiagram} from the given classes using reflection ({@link Class#getSuperclass()}, {@link Class#getInterfaces()}, {@link Class#getDeclaredMethods()}). Inheritance and implementation relationships are rendered as directed edges.

| Parameter | Description |
| --- | --- |
| `classes` | the classes to include in the diagram |

---

### `sayClassHierarchy`

Renders the full class hierarchy (superclass chain + interfaces) as a tree. <p>Blue ocean innovation: visualizes the inheritance graph of any class using only {@link Class#getSuperclass()} and {@link Class#getInterfaces()} — no external deps. Documents "where does this class fit in the type hierarchy" automatically.</p>

| Parameter | Description |
| --- | --- |
| `clazz` | the class whose hierarchy to render |

---

### `sayCode`

Renders a code block with optional syntax highlighting language hint.

| Parameter | Description |
| --- | --- |
| `code` | The code content. |
| `language` | The programming language for syntax highlighting (e.g., "java", "sql", "json"). |

---

### `sayCodeModel`

Documents a method's structure using Project Babylon CodeReflection API. <p>On Java 26+, uses {@code java.lang.reflect.code.CodeReflection.reflect(method)} to introspect the method's bytecode operations — control flow, method calls, field accesses, etc. Renders a detailed breakdown of operation types and their counts.</p> <p>On Java 26 and earlier, gracefully falls back to rendering the method signature (parameters with types, return type) extracted via reflection.</p> <p>Example:</p> <pre>{@code sayCodeModel(SayEvent.class.getMethod("toString")); // Java 26+: Renders operation breakdown (INVOKE: 3, FIELD_READ: 1, etc.) // Java 26-: Renders String toString() signature only }</pre>

| Parameter | Description |
| --- | --- |
| `method` | the method to introspect and document (must not be null) |

---

### `sayComplexityProfile`

Empirically profiles algorithmic complexity by running a task factory at increasing input sizes and measuring wall-clock time. Renders a measurement table and infers the growth class (O(1), O(n), O(n²), etc.). <p>Blue ocean: documentation that PROVES complexity instead of asserting it.</p>

| Parameter | Description |
| --- | --- |
| `label` | descriptive label (e.g. "ArrayList.contains()") |
| `taskFactory` | produces a {@code Runnable} for the given input size {@code n} |
| `ns` | array of input sizes to measure (e.g. {100, 1_000, 10_000}) |

---

### `sayContractVerification`

Documents interface contract coverage across implementation classes. For each public method in the contract interface, checks whether each implementation provides a concrete override (✅ direct), inherits it (↗ inherited), or is missing it entirely (❌ MISSING). Uses only standard Java reflection. <p>If the contract is a sealed interface, permitted subclasses are automatically detected so the user does not need to enumerate them.</p>

| Parameter | Description |
| --- | --- |
| `contract` | the interface whose methods to verify |
| `implementations` | zero or more implementation classes to check |

---

### `sayControlFlowGraph`

Documents the control flow graph of a {@code @CodeReflection}-annotated method using the Java 26 Code Reflection API (JEP 516). Renders a Mermaid {@code flowchart TD} diagram where each basic block is a node and branch ops produce directed edges. Falls back to a text note on older runtimes.

| Parameter | Description |
| --- | --- |
| `method` | the method whose CFG to render (should be annotated with               {@code @java.lang.reflect.code.CodeReflection}) |

---

### `sayDataFlow`

Documents a data transformation pipeline by executing each stage against sample inputs and capturing intermediate outputs. Renders a flowchart and a table showing input → stage output at each step. <p>Blue ocean: ETL / pipeline docs are perpetually stale; this makes them executable so they cannot drift.</p>

| Parameter | Description |
| --- | --- |
| `title` | pipeline name (e.g. "Order Processing Pipeline") |
| `stages` | ordered list of stage labels |
| `transforms` | ordered list of {@code java.util.function.Function<Object,Object>}                   applied sequentially to the sample input |
| `sample` | representative input value for the first stage |

---

### `sayDecisionTree`

Documents a decision algorithm as a Mermaid {@code flowchart TD}. The {@code branches} map encodes the tree: keys are node labels (questions / conditions); values are either a {@code String} (leaf answer) or a nested {@code Map<String,Object>} (sub-tree). Renders at most 5 levels deep. <p>Blue ocean: decision logic is usually buried in code; this surfaces it as an auto-generated, always-current diagram.</p>

| Parameter | Description |
| --- | --- |
| `title` | chart title / root question label |
| `branches` | decision tree encoded as nested map |

---

### `sayDocCoverage`

Renders a documentation coverage report for the given classes — which public methods were documented in this test vs. which were not. Coverage is tracked automatically as {@code say*} methods are called.

| Parameter | Description |
| --- | --- |
| `classes` | the classes whose public API to check for documentation coverage |

---

### `sayEnvProfile`

Renders a zero-parameter environment snapshot: Java version, OS, available processors, max heap (MB), timezone, and DTR version. Useful as a "generated with" footer in any documentation section.

---

### `sayEvolutionTimeline`

Derives the git commit history for the source file of the given class using {@code git log --follow} and renders it as a timeline table (commit, date, author, subject). Falls back gracefully with a note if git is unavailable. <p>Follows the same {@code ProcessBuilder} + try/catch + fallback pattern already used in {@code DocMetadata}.</p>

| Parameter | Description |
| --- | --- |
| `clazz` | the class whose source file history to document |
| `maxEntries` | maximum number of commits to include (most recent first) |

---

### `sayException`

Documents an exception — type, message, full cause chain, and the top 5 stack frames — in a structured table. Useful in error-handling and resilience documentation sections.

| Parameter | Description |
| --- | --- |
| `t` | the throwable to document (must not be null) |

---

### `sayFaultTolerance`

Documents a "let it crash" fault-tolerance scenario: a list of failures paired with supervisor-driven recoveries. Renders a two-column table plus a recovery-ratio metric (recoveries / failures). <p>Blue ocean: makes the implicit restart contract explicit — reviewers can audit fault coverage without reading OTP supervisor specs.</p>

| Parameter | Description |
| --- | --- |
| `scenario` | scenario name (e.g. "Database connection pool exhausted") |
| `failures` | failure descriptions in order |
| `recoveries` | supervisor recovery actions in corresponding order |

---

### `sayFootnote`

Renders a footnote with the given text.

| Parameter | Description |
| --- | --- |
| `text` | The footnote content. |

---

### `sayHeatmap`

Renders a 2-D ASCII heatmap using Unicode block characters (░▒▓█) for matrix data. Normalises values to [0, 1] and maps to four intensity levels. Ideal for correlation matrices, confusion matrices, and perf heat maps. <p>Blue ocean: 2-D visualisation in plain text — works in any terminal, any Markdown renderer, and any CI log.</p>

| Parameter | Description |
| --- | --- |
| `title` | descriptive title shown above the heatmap |
| `matrix` | 2-D data array [rows][cols] |
| `rowLabels` | labels for each row |
| `colLabels` | labels for each column |

---

### `sayJavadoc`

Renders the extracted Javadoc for a method: description, parameter table, return value, throws, and {@code @since} — sourced from {@code docs/meta/javadoc.json} (generated by the dtr-javadoc Rust binary). <p>If the JSON index is not present or the method has no entry, this is a no-op.</p>

| Parameter | Description |
| --- | --- |
| `method` | the method whose Javadoc to render (must not be null) |

---

### `sayJson`

Serializes an object to JSON and renders it in a code block.

| Parameter | Description |
| --- | --- |
| `object` | The object to serialize (will be rendered as pretty-printed JSON). |

---

### `sayKaizen`

Documents a Kaizen continuous-improvement event: measures a metric before and after the improvement, renders a comparison table with absolute delta and percentage improvement, and calls out the improvement ratio. <p>Blue ocean: Toyota's improvement discipline applied to software metrics — latency, throughput, defect rate — with machine-verified numbers.</p>

| Parameter | Description |
| --- | --- |
| `metric` | name of the metric (e.g. "P99 latency", "Build time") |
| `before` | measurement samples before the improvement |
| `after` | measurement samples after the improvement |
| `unit` | unit label (e.g. "ms", "s", "defects/kloc") |

---

### `sayKanban`

Documents a Kanban board snapshot: items in backlog, in progress (WIP), and done. Renders as a three-column markdown table and reports the WIP count and flow efficiency (done / total). <p>Blue ocean: living documentation of work state — the board IS the documentation, auto-generated from your tracking data.</p>

| Parameter | Description |
| --- | --- |
| `board` | board name (e.g. "Sprint 42", "Q2 Infrastructure") |
| `backlog` | items not yet started |
| `wip` | items currently in progress |
| `done` | completed items |

---

### `sayKeyValue`

Renders key-value pairs in a readable format.

| Parameter | Description |
| --- | --- |
| `pairs` | A map of keys to values. |

---

### `sayMermaid`

Renders a raw Mermaid diagram as a fenced {@code mermaid} code block. Mermaid renders natively on GitHub, GitLab, and Obsidian.

| Parameter | Description |
| --- | --- |
| `diagramDsl` | the Mermaid DSL string (e.g., "flowchart TD\n    A --> B") |

---

### `sayMuda`

Documents a Muda (waste) analysis: identifies the seven TPS wastes present in a process and the corresponding improvement actions. Renders a waste-type → description → action table and a waste-elimination ratio. <p>Blue ocean: waste is identified in retrospectives but rarely committed to documentation — this makes the analysis a first-class artefact.</p>

| Parameter | Description |
| --- | --- |
| `process` | process name (e.g. "Manual deployment pipeline") |
| `wastes` | waste descriptions (what waste was found) |
| `improvements` | improvement actions for each waste |

---

### `sayNextSection`

A heading that will appear as a top-level section in the documentation and in the table of contents. No escaping is done.

| Parameter | Description |
| --- | --- |
| `headline` | The section heading text. |

---

### `sayNote`

Renders an info callout box (GitHub-style [!NOTE] alert).

| Parameter | Description |
| --- | --- |
| `message` | The info message. |

---

### `sayOpProfile`

Renders a lightweight operation-count table for a method using the Java 26 Code Reflection API — same IR traversal as {@link #sayCodeModel(java.lang.reflect.Method)} but without the IR excerpt, for quick performance characterization.

| Parameter | Description |
| --- | --- |
| `method` | the method to profile (should be annotated with {@code @CodeReflection}) |

---

### `sayOrderedList`

Renders an ordered (numbered) list.

| Parameter | Description |
| --- | --- |
| `items` | List of strings to render as numbered items. |

---

### `sayParallelTrace`

Renders a parallel execution trace as a Mermaid Gantt chart. Each agent is a section; each {@code timeSlot} is a {@code long[2]} of {startMs, endMs} relative to trace start. <p>Blue ocean: multi-agent / multi-thread execution is hard to reason about from logs; this makes the timeline visual and living.</p>

| Parameter | Description |
| --- | --- |
| `title` | chart title |
| `agents` | agent/thread names (same length as timeSlots) |
| `timeSlots` | parallel list of {startMs, durationMs} per agent |

---

### `sayPatternMatch`

Documents Erlang-style pattern matching: a set of patterns, the values they are matched against, and whether each match succeeds. Renders a three-column table with ✅ / ❌ match indicators. <p>Blue ocean: pattern-matching logic is tested but never documented — this surfaces the full match matrix as executable specification.</p>

| Parameter | Description |
| --- | --- |
| `title` | section title |
| `patterns` | pattern strings (e.g. "{ok, Value}", "_") |
| `values` | value strings tested against corresponding patterns |
| `matches` | true if the pattern matched, false if it did not |

---

### `sayPokaYoke`

Documents Poka-yoke (mistake-proofing) devices: each mistake-proof mechanism and whether it was verified as effective. Renders a mechanism → verified table with ✅ / ❌ indicators and an effectiveness %. <p>Blue ocean: error-prevention mechanisms are described in runbooks but never tested and documented together — this unifies both.</p>

| Parameter | Description |
| --- | --- |
| `operation` | operation name (e.g. "Production deployment") |
| `mistakeProofs` | mistake-proofing mechanism descriptions |
| `verified` | true if the mechanism was confirmed effective |

---

### `sayPropertyBased`

Documents a logical property by evaluating a predicate against a list of sample inputs. Renders a table showing each input, the predicate result, and a PASS/FAIL marker. Fails the test if any input violates the property. <p>Blue ocean: property-based invariant documentation — proves correctness across representative examples inline with the narrative.</p>

| Parameter | Description |
| --- | --- |
| `property` | description of the invariant (e.g. "result is always positive") |
| `check` | predicate that must hold for every input |
| `inputs` | representative sample inputs |

---

### `sayRaw`

Injects raw content directly into the documentation output. Use this for custom markdown or other content that bypasses normal formatting.

| Parameter | Description |
| --- | --- |
| `rawMarkdown` | Raw content to inject (e.g., markdown tables, code blocks, or HTML). |

---

### `sayRecordComponents`

Renders a schema table for a Java record class — component names, types, and any annotations present. Uses {@link Class#getRecordComponents()} (Java 16+).

| Parameter | Description |
| --- | --- |
| `recordClass` | the record class to document |

---

### `sayRef`

Renders a cross-reference to another DocTest's section. The reference is resolved using the CrossReferenceIndex and rendered as a markdown link in Markdown mode or a LaTeX \ref{} command in LaTeX mode.

| Parameter | Description |
| --- | --- |
| `ref` | the cross-reference to another DocTest section |

---

### `sayReflectiveDiff`

Compares two objects field-by-field using reflection and renders a diff table. <p>Blue ocean innovation: shows exactly what changed between two states of an object — before/after, v1/v2, request/response. Uses {@link java.lang.reflect.Field} with {@code setAccessible(true)} to compare every declared field. Documents "what changed" automatically.</p>

| Parameter | Description |
| --- | --- |
| `before` | the object representing the "before" state |
| `after` | the object representing the "after" state (must be same type as before) |

---

### `sayStateMachine`

Renders a finite state machine as a Mermaid {@code stateDiagram-v2} diagram. Keys in {@code transitions} are "FROM:EVENT" strings; values are destination state names. The initial state is taken as the first key's source. <p>Blue ocean: state machines pervade systems yet are almost never documented in a living, executable form.</p>

| Parameter | Description |
| --- | --- |
| `title` | human-readable title shown above the diagram |
| `transitions` | map of "FROM:EVENT" → "TO_STATE" |

---

### `sayStringProfile`

Analyzes a string and renders its structural profile using Java string APIs. <p>Blue ocean innovation: renders word count, line count, character category distribution, and Unicode metrics for any string — using only {@link String#chars()}, {@link String#lines()}, and {@link Character} APIs. Invaluable for documenting text processing and NLP APIs.</p>

| Parameter | Description |
| --- | --- |
| `text` | the string to profile (may be a sample payload, template, etc.) |

---

### `saySupervisionTree`

Documents an Erlang/OTP-style supervision tree showing which supervisors manage which workers. Renders as a Mermaid {@code graph TD} with supervisor → child edges, restart strategies, and worker counts. <p>Blue ocean: surfaces fault-tolerance topology that is implicit in OTP app files but never visualised in documentation.</p>

| Parameter | Description |
| --- | --- |
| `title` | diagram title (e.g. "Payment Service Supervision Tree") |
| `supervisors` | map of supervisor name → list of child names |

---

### `sayTable`

Renders a markdown table from a 2D string array. The first row is treated as table headers.

| Parameter | Description |
| --- | --- |
| `data` | A 2D array where each row is a list of cells. First row becomes TH. |

---

### `sayTimeSeries`

Documents how a metric evolves over time with an ASCII sparkline and trend summary (min, max, mean, direction). Zero dependencies — pure Java string math using Unicode block characters (▁▂▃▄▅▆▇█). <p>Blue ocean: no existing documentation framework shows metric trends inline alongside the narrative.</p>

| Parameter | Description |
| --- | --- |
| `label` | descriptive label for the metric (e.g. "GC pause (ms)") |
| `values` | numeric samples in chronological order |
| `timestamps` | human-readable timestamp strings (same length as values) |

---

### `sayUnorderedList`

Renders an unordered (bullet) list.

| Parameter | Description |
| --- | --- |
| `items` | List of strings to render as bullet points. |

---

### `sayValueStream`

Documents a Value Stream Map: the sequence of process steps from demand to delivery, each with a measured cycle time. Renders a bar chart of cycle times and reports total lead time, value-adding time, and efficiency. <p>Blue ocean: value stream mapping is a whiteboard exercise; this generates a measurable, always-current version from real data.</p>

| Parameter | Description |
| --- | --- |
| `product` | product or feature name (e.g. "Feature → Production") |
| `steps` | process step names in flow order |
| `cycleTimeMs` | measured cycle time for each step in milliseconds |

---

### `sayWarning`

Renders a warning callout box (GitHub-style [!WARNING] alert).

| Parameter | Description |
| --- | --- |
| `message` | The warning message. |

---

