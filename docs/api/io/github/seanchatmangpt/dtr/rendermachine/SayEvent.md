# `SayEvent`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine`  
> **Since:** `2.0.0`  

Sealed event hierarchy for the DTR render pipeline. <p>Every {@code say*} invocation on a {@link RenderMachine} corresponds to a {@code SayEvent} subtype. The sealed hierarchy with exhaustive switch expressions ensures compile-time completeness — adding a new event type forces every renderer to handle it or fail compilation.</p> <p>This is the canonical demonstration of sealed classes + records + pattern matching working together as a type-safe event system. The pattern:</p> <pre>{@code String rendered = switch (event) {     case SayEvent.TextEvent(var text)           -> renderParagraph(text);     case SayEvent.SectionEvent(var heading)     -> renderSection(heading);     case SayEvent.CodeEvent(var code, var lang) -> renderCode(code, lang);     // ... exhaustive — compiler enforces completeness }; }</pre> <p>No visitor pattern. No dispatch maps. No instanceof chains. No defaults. The type system proves every case is handled.</p> <p>Inspired by Project Babylon's code model approach: the event structure IS the documentation contract, not a description of it.</p>

```java
public sealed interface SayEvent permits SayEvent.TextEvent, SayEvent.SectionEvent, SayEvent.CodeEvent, SayEvent.TableEvent, SayEvent.JsonEvent, SayEvent.NoteEvent, SayEvent.WarningEvent, SayEvent.KeyValueEvent, SayEvent.UnorderedListEvent, SayEvent.OrderedListEvent, SayEvent.AssertionsEvent, SayEvent.CitationEvent, SayEvent.FootnoteEvent, SayEvent.RefEvent, SayEvent.RawEvent, SayEvent.CodeModelEvent, SayEvent.MethodCodeModelEvent, SayEvent.ControlFlowGraphEvent, SayEvent.CallGraphEvent, SayEvent.OpProfileEvent, SayEvent.BenchmarkEvent, SayEvent.MermaidEvent, SayEvent.DocCoverageEvent, SayEvent.EnvProfileEvent, SayEvent.RecordSchemaEvent, SayEvent.ExceptionEvent, SayEvent.AsciiChartEvent {
}
```

