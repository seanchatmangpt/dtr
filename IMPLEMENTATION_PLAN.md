# Implementation Plan: Extend say* API for Rich Documentation

## Objective
Add new `say*` methods to `DTR` and `RenderMachineCommands` to make documentation generation more versatile and less HTTP-centric. Enable richer formatting (tables, code blocks, callouts, lists) beyond the current paragraph/section/raw triumvirate.

## Current API
```
say(String text)                  // paragraph
sayNextSection(String headline)   // section heading
sayRaw(String markdown)           // raw markdown injection
sayAndMakeRequest(Request)        // HTTP request + response logging
sayAndAssertThat(String, T, Matcher)  // assertion + documentation
```

## Proposed New Methods

### 1. **`sayTable(String[][] data)`**
Generate markdown table from 2D array.
- **Signature**: `void sayTable(String[][] data)`
- **Example**:
  ```java
  sayTable(new String[][] {
      {"Name", "Status", "Score"},
      {"Alice", "Active", "95"},
      {"Bob", "Inactive", "87"}
  });
  ```
- **Output**: Markdown table with headers (first row as TH)
- **Use case**: API response comparisons, test results matrices, feature matrices

### 2. **`sayCode(String code, String language)`**
Render code block with syntax highlighting hint.
- **Signature**: `void sayCode(String code, String language)`
- **Example**: `sayCode("SELECT * FROM users;", "sql")`
- **Output**: ` ```sql\n...code...\n``` `
- **Use case**: Database queries, gRPC payloads, configuration examples

### 3. **`sayWarning(String message)`**
Render a warning callout box.
- **Signature**: `void sayWarning(String message)`
- **Output**: `> [!WARNING]\n> message` (GitHub-style alert)
- **Use case**: Deprecation notices, important caveats, side effects

### 4. **`sayNote(String message)`**
Render an info callout box.
- **Signature**: `void sayNote(String message)`
- **Output**: `> [!NOTE]\n> message`
- **Use case**: Clarifications, tips, context

### 5. **`sayKeyValue(Map<String, String> pairs)`**
Render key-value pairs in a readable format.
- **Signature**: `void sayKeyValue(Map<String, String> pairs)`
- **Example**: `sayKeyValue(Map.of("Host", "localhost:8080", "Port", "8080"))`
- **Output**: Definition list or 2-column markdown table
- **Use case**: Configuration summary, HTTP headers, metadata

### 6. **`sayUnorderedList(List<String> items)`**
Render unordered list.
- **Signature**: `void sayUnorderedList(List<String> items)`
- **Output**: Markdown bullet list
- **Use case**: Feature checklists, step summaries, prerequisites

### 7. **`sayOrderedList(List<String> items)`**
Render ordered list.
- **Signature**: `void sayOrderedList(List<String> items)`
- **Output**: Numbered markdown list
- **Use case**: Workflow steps, sequential instructions

### 8. **`sayJson(Object object)`**
Serialize object to formatted JSON and render in code block.
- **Signature**: `void sayJson(Object object)`
- **Output**: Pretty-printed JSON in ` ```json\n...\n``` `
- **Use case**: Payload preview, configuration display, data structure documentation

### 9. **`sayAssertions(Map<String, String> assertions)`** *(Advanced)*
Render assertion results in a table (similar to sayAndAssertThat but for non-test data).
- **Signature**: `void sayAssertions(Map<String, String> assertions)`
- **Output**: Table with "Check" / "Result" columns
- **Use case**: Manual validation logs, batch result summary

## Implementation Strategy

### Phase 1: Interface Contracts
1. Add all 9 new method signatures to `RenderMachineCommands` interface
2. Add corresponding delegation methods to `DTR` class
3. Each method has a Javadoc explaining purpose and markdown output

### Phase 2: RenderMachineImpl Implementation
1. Implement each method in `RenderMachineImpl`
2. Each method appends markdown-formatted strings to `markdownDocument` list
3. Follow existing `say()` pattern: add blank line before content for spacing

### Phase 3: Unit Tests
1. Add test class `RenderMachineExtensionTest` in `dtr-core` tests
2. Verify each new method produces valid markdown
3. Test edge cases (empty arrays, null values, special characters)

### Phase 4: Integration Test
1. Add sample test method in `dtr-integration-test` that uses all new `say*` methods
2. Generate sample HTML output to verify rendering

### Phase 5: Documentation Update
1. Update CLAUDE.md with new API examples
2. Add 80/20 guide section showing non-HTTP documentation patterns

## Key Design Decisions

1. **No HTML output**: All methods generate markdown; let markdown renderers (GitHub, Pandoc, Hugo) handle styling.
2. **Consistency**: All methods follow `say*` naming convention and append to `markdownDocument`.
3. **Simplicity**: Methods are small and focused; no builder pattern (unlike `Request`).
4. **Backward compatible**: Existing tests unaffected; new methods are purely additive.
5. **Type-safe**: Use `Object` for JSON serialization, `List`/`Map` for collections; no varargs strings.

## Files to Modify

| File | Changes |
|------|---------|
| `dtr-core/src/main/java/org/r10r/dtr/rendermachine/RenderMachineCommands.java` | Add 9 method signatures |
| `dtr-core/src/main/java/org/r10r/dtr/DTR.java` | Add 9 delegation methods |
| `dtr-core/src/main/java/org/r10r/dtr/rendermachine/RenderMachineImpl.java` | Implement 9 methods |
| `dtr-core/src/test/java/org/r10r/dtr/rendermachine/RenderMachineExtensionTest.java` | New test class (create) |
| `CLAUDE.md` | Update API reference section |

## Testing Plan

- Unit tests: 15–20 test methods (2-3 per new method)
- Edge case coverage: empty inputs, null, special markdown chars
- Integration test: one method `testAllNewSayMethods()` showing all 9 in action
- Manual verification: render output and verify markdown readability

## Acceptance Criteria

- [ ] All 9 new methods defined in `RenderMachineCommands`
- [ ] All 9 methods delegated in `DTR`
- [ ] All 9 methods implemented in `RenderMachineImpl`
- [ ] Unit tests pass with >95% line coverage for new code
- [ ] Integration test produces valid markdown output
- [ ] CLAUDE.md updated with examples and rationale
- [ ] Build passes: `mvnd clean verify`
- [ ] No breaking changes to existing API

---

## Next Steps
1. Confirm plan with user
2. Start with `RenderMachineCommands` interface
3. Parallel: implement in `RenderMachineImpl` and `DTR`
4. Write tests as we go (TDD style)
5. Update documentation
6. Commit and push to `claude/plan-major-release-LmHxG`
