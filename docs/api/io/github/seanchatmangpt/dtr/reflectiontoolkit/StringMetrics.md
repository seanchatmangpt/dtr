# `StringMetrics`

> **Package:** `io.github.seanchatmangpt.dtr.reflectiontoolkit`  
> **Since:** `Java 26`  

Record capturing detailed metrics about a string's content and composition. <p>This immutable record represents various statistical measures of a string: word count, line count, character count, unique character count, letter count, and non-ASCII character count. These metrics are useful for text analysis, documentation generation, and content validation in API testing frameworks. <p><strong>Usage Example:</strong> <pre>{@code String text = "Hello World\nFoo Bar"; StringMetrics metrics = new StringMetrics(     4,          // wordCount: "Hello", "World", "Foo", "Bar"     2,          // lineCount: 2 lines     19,         // characterCount: total chars including whitespace/newlines     13,         // uniqueCharCount: distinct characters     8,          // letterCount: letters only (H,e,l,l,o,F,o,B,a,r minus duplicates)     0           // nonAsciiCount: 0 (all ASCII) ); System.out.println(metrics.wordCount());           // 4 System.out.println(metrics.lineCount());           // 2 System.out.println(metrics.characterCount());      // 19 }</pre> <p><strong>Compact Constructor Validation:</strong> The compact canonical constructor validates: <ul>   <li>All counts are non-negative (>= 0)</li>   <li>{@code uniqueCharCount} does not exceed {@code characterCount}</li>   <li>{@code letterCount} does not exceed {@code characterCount}</li>   <li>{@code nonAsciiCount} does not exceed {@code characterCount}</li> </ul>

```java
public record StringMetrics( long wordCount, long lineCount, long characterCount, long uniqueCharCount, long letterCount, long nonAsciiCount) {
}
```

