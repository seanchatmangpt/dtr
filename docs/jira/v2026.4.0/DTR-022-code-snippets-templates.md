# DTR-022: Code Snippets and Templates for IDE Integration

**Priority**: P3
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,ide,snippets,templates,vscode,intellij,2026.4.0

## Description
Create comprehensive code snippets and live templates for VS Code and IntelliJ IDEA that accelerate DTR documentation test development. These snippets will provide 20+ commonly used patterns, reducing boilerplate and enforcing best practices across the codebase.

## Scope

### VS Code Snippets
- Language-specific snippet files for Java
- Snippets for common DTR patterns (test structure, assertions, documentation)
- Prefix-based completion with descriptive names
- Placeholders for tab navigation through snippet fields
- Support for both JUnit and TestNG

### IntelliJ Live Templates
- Live template definitions for DTR patterns
- Context-aware templates (test class, test method, documentation)
- Abbreviations for quick insertion
- Variables for smart cursor positioning
- Template groups for organized access

## Snippet Categories

### 1. Test Structure Snippets (6 items)
- `dtr-test` - Complete DocTest class structure
- `dtr-method` - Single test method with DtrContext
- `dtr-setup` - @BeforeEach setup method
- `dtr-teardown` - @AfterEach teardown method
- `dtr-class` - Test class with annotations
- `dtr-nested` - Nested test class structure

### 2. Documentation Snippets (8 items)
- `say-para` - say() for paragraphs
- `say-code` - sayCode() with language
- `say-table` - sayTable() with 2D array
- `say-warn` - sayWarning() alert
- `say-note` - sayNote() alert
- `say-ref` - sayRef() cross-reference
- `say-model` - sayCodeModel() for class structure
- `say-bench` - sayBenchmark() for performance

### 3. Assertion Snippets (4 items)
- `assert-say` - sayAndAssertThat() pattern
- `assert-eq` - Equality assertion with documentation
- `assert-true` - Boolean assertion with documentation
- `assert-except` - Exception testing with documentation

### 4. Java 26 Feature Snippets (6 items)
- `record-dtr` - Record with documentation
- `sealed-dtr` - Sealed hierarchy with documentation
- `pattern-dtr` - Pattern matching with documentation
- `switch-dtr` - Switch expressions with documentation
- `stream-dtr` - Stream operations with documentation
- `concurrent-dtr` - Concurrent code with documentation

### 5. Advanced Patterns (6 items)
- `say-cfg` - Control flow graph documentation
- `say-callgraph` - Call graph visualization
- `say-contract` - Interface contract verification
- `say-evolution` - Git evolution timeline
- `say-json` - JSON data documentation
- `say-mermaid` - Custom Mermaid diagram

## Acceptance Criteria

### VS Code Snippets
- [ ] Snippet file created at `.vscode/java.code-snippets`
- [ ] All 30+ snippets defined and functional
- [ ] Snippets appear in IntelliSense with correct prefixes
- [ ] Tab navigation works through all placeholders
- [ ] Snippets expand with proper indentation
- [ ] Snippets include cursor position at logical end point
- [ ] README documents all snippet prefixes and usage

### IntelliJ Live Templates
- [ ] Live template file created at `.idea/fileTemplates/includes/`
- [ ] All 30+ templates defined and functional
- [ ] Templates appear in "Insert Live Template" dialog
- [ ] Tab key expands templates and navigates placeholders
- [ ] Templates respect Java code style settings
- [ ] Templates group as "DTR" in template list
- [ ] Documentation covers all template abbreviations

### Quality Standards
- [ ] Snippets follow DTR coding standards
- [ ] Generated code compiles without errors
- [ ] Snippets use consistent naming conventions
- [ ] Placeholders have descriptive default values
- [ ] Snippets handle both JUnit 5 and TestNG
- [ ] All snippets tested with real examples

### Documentation
- [ ] Snippet reference guide created
- [ ] Each snippet documented with example usage
- [ ] Visual screenshots show snippet expansion
- [ ] Best practices guide for snippet contribution

## Technical Notes

### VS Code Snippet Format

#### .vscode/java.code-snippets
```json
{
  "DTR Test Class": {
    "prefix": "dtr-test",
    "scope": "java",
    "description": "Complete DTR DocTest class structure",
    "body": [
      "package ${1:package};",
      "",
      "import io.github.seanchatmangpt.dtr.junit.*;",
      "import io.github.seanchatmangpt.dtr.context.DtrContext;",
      "import org.junit.jupiter.api.*;",
      "",
      "import static org.hamcrest.MatcherAssert.assertThat;",
      "import static org.hamcrest.Matchers.*;",
      "",
      "class ${2:ClassName}DocTest {",
      "    ",
      "    private DtrContext ctx;",
      "",
      "    @BeforeEach",
      "    void setUp() {",
      "        ctx = new DtrContext();",
      "        ctx.setOutputTarget(DtrOutputTarget.MARKDOWN);",
      "    }",
      "    ",
      "    @Test",
      "    void ${3:testMethodName}() {",
      "        ${4:// Test implementation}",
      "        ctx.say(\"${5:Documentation text}\");",
      "        ",
      "        ${6:// Assertions}",
      "        assertThat(${7:actual}, is(${8:expected}));",
      "    }",
      "    ",
      "    $0",
      "}"
    ]
  },
  "DTR say Code": {
    "prefix": "say-code",
    "scope": "java",
    "description": "Insert sayCode() method call",
    "body": [
      "ctx.sayCode(${1:\"${2:code}\"}, \"${3:java}\");$0"
    ]
  },
  "DTR sayAndAssertThat": {
    "prefix": "assert-say",
    "scope": "java",
    "description": "Insert sayAndAssertThat() pattern",
    "body": [
      "ctx.sayAndAssertThat(",
      "    \"${1:label}\",",
      "    ${2:actual},",
      "    is(${3:expected})",
      ");$0"
    ]
  },
  "DTR Benchmark": {
    "prefix": "say-bench",
    "scope": "java",
    "description": "Insert sayBenchmark() for performance testing",
    "body": [
      "ctx.sayBenchmark(",
      "    \"${1:label}\",",
      "    () -> ${2:taskToMeasure},",
      "    ${3:100},  /* warmup rounds */",
      "    ${4:1000} /* measure rounds */",
      ");$0"
    ]
  },
  "DTR Record Documentation": {
    "prefix": "record-dtr",
    "scope": "java",
    "description": "Record with DTR documentation",
    "body": [
      "public record ${1:RecordName}(",
      "    ${2:type} ${3:fieldName}",
      ") {",
      "    ",
      "    static class DocTest extends DtrTest {",
      "        @Test",
      "        void documentRecord() {",
      "            ctx.sayRecordComponents(${1:RecordName}.class);",
      "        }",
      "    }",
      "    $0",
      "}"
    ]
  }
}
```

### IntelliJ Live Template Format

#### .idea/fileTemplates/includes/DTR_Live_Templates.xml
```xml
<templateSet group="DTR">
  <!-- Test Structure Templates -->
  <template name="DTR-TEST" value="package $PACKAGE_NAME;&#10;&#10;import io.github.seanchatmangpt.dtr.junit.*;&#10;import io.github.seanchatmangpt.dtr.context.DtrContext;&#10;import org.junit.jupiter.api.*;&#10;&#10;import static org.hamcrest.MatcherAssert.assertThat;&#10;import static org.hamcrest.Matchers.*;&#10;&#10;class $CLASS$DocTest {&#10;    private DtrContext ctx;&#10;&#10;    @BeforeEach&#10;    void setUp() {&#10;        ctx = new DtrContext();&#10;        ctx.setOutputTarget(DtrOutputTarget.MARKDOWN);&#10;    }&#10;    &#10;    $END$&#10;}"
    toReformat="true" toShortenFQNames="true">
    <variable name="PACKAGE" expression="packageName()" defaultValue="" alwaysStopAt="true" />
    <variable name="CLASS" expression="className()" defaultValue="" alwaysStopAt="true" />
    <context>
      <option name="JAVA_DECLARATION" value="true" />
    </context>
  </template>

  <!-- Documentation Templates -->
  <template name="SAY-CODE" value="ctx.sayCode(&quot;$CODE$&quot;, &quot;$LANG$&quot;);$END$"
    toReformat="true" toShortenFQNames="true">
    <variable name="CODE" expression="" defaultValue="code" alwaysStopAt="true" />
    <variable name="LANG" expression="" defaultValue="java" alwaysStopAt="true" />
    <context>
      <option name="JAVA_STATEMENT" value="true" />
    </context>
  </template>

  <!-- Assertion Templates -->
  <template name="ASSERT-SAY" value="ctx.sayAndAssertThat(&#10;    &quot;$LABEL$&quot;,&#10;    $ACTUAL$,&#10;    is($EXPECTED$)&#10;);$END$"
    toReformat="true" toShortenFQNames="true">
    <variable name="LABEL" expression="" defaultValue="label" alwaysStopAt="true" />
    <variable name="ACTUAL" expression="" defaultValue="actual" alwaysStopAt="true" />
    <variable name="EXPECTED" expression="" defaultValue="expected" alwaysStopAt="true" />
    <context>
      <option name="JAVA_STATEMENT" value="true" />
    </context>
  </template>

  <!-- Java 26 Feature Templates -->
  <template name="RECORD-DTR" value="public record $NAME$($TYPE$ $FIELD$) {&#10;    static class DocTest extends DtrTest {&#10;        @Test&#10;        void documentRecord() {&#10;            ctx.sayRecordComponents($NAME$.class);&#10;        }&#10;    }&#10;    $END$&#10;}"
    toReformat="true" toShortenFQNames="true">
    <variable name="NAME" expression="" defaultValue="RecordName" alwaysStopAt="true" />
    <variable name="TYPE" expression="" defaultValue="String" alwaysStopAt="true" />
    <variable name="FIELD" expression="" defaultValue="field" alwaysStopAt="true" />
    <context>
      <option name="JAVA_DECLARATION" value="true" />
    </context>
  </template>
</templateSet>
```

### Template Variable Conventions

#### Placeholder Naming
- `$PACKAGE$` - Java package name
- `$CLASS$` - Simple class name
- `$METHOD$` - Method name
- `$CODE$` - Code block content
- `$LANG$` - Programming language identifier
- `$LABEL$` - Human-readable label
- `$ACTUAL$` - Actual value in assertion
- `$EXPECTED$` - Expected value in assertion
- `$TYPE$` - Java type
- `$FIELD$` - Field/variable name

#### Default Values
- Provide sensible defaults for all placeholders
- Use DTR-specific defaults where appropriate
- Include examples in default values

### Testing Strategy

#### Validation Checklist
- [ ] All snippets compile without syntax errors
- [ ] Placeholders work with tab navigation
- [ ] Snippets respect code style settings
- [ ] No duplicate prefixes or abbreviations
- [ ] Snippets work in both fresh and existing files
- [ ] Cross-platform compatibility (Windows, macOS, Linux)

#### Example Test Cases
```java
// Test: dtr-test snippet creates valid class
@Test
void test_snippet_creates_valid_class() {
    String snippet = expandSnippet("dtr-test");
    assertCompiles(snippet);
    hasAnnotation(snippet, "org.junit.jupiter.api.Test");
    hasField(snippet, "DtrContext ctx");
}

// Test: say-code snippet has correct parameters
@Test
void test_say_code_snippet_parameters() {
    String snippet = expandSnippet("say-code");
    containsMethodCall(snippet, "sayCode");
    hasParameters(snippet, 2); // code, language
}
```

## Dependencies

### Required
- **DTR-019** (VS Code Extension): Should integrate with snippets for auto-suggestions
- **DTR-020** (IntelliJ Plugin): Should integrate with live templates for auto-suggestions
- **DTR-021** (Debugging Configurations): Snippets should generate debuggable code

### Documentation Sources
- `/Users/sac/dtr/docs/reference/say-api-methods.md` - API reference for snippet parameters
- `/Users/sac/dtr/docs/tutorials/` - Tutorial examples for snippet validation

## References

### Internal
- `/Users/sac/dtr/docs/reference/say-api-methods.md` - Complete say* API reference
- `/Users/sac/dtr/docs/tutorials/` - Tutorial examples demonstrating snippet patterns
- `/Users/sac/dtr/CLAUDE.md` - Project coding standards

### External
- [VS Code Snippets Guide](https://code.visualstudio.com/docs/editor/userdefinedsnippets)
- [IntelliJ Live Templates](https://www.jetbrains.com/help/idea/using-live-templates.html)
- [Java Code Conventions](https://oracle.com/java/technologies/javase/codeconventions-contents.html)

## Success Metrics

### Functionality
- All 30+ snippets work correctly in both IDEs
- Snippets reduce typing time by 70%+ for common patterns
- Zero syntax errors in generated code
- 95%+ of new tests use snippets (measured via code review)

### Adoption
- Snippets referenced in quickstart guide
- All tutorial examples updated to use snippets
- Community contributions of additional snippets

### Quality
- Snippets follow DTR coding standards
- Consistent naming across VS Code and IntelliJ
- Documentation covers all snippets with examples
- Snippets tested on all supported platforms
