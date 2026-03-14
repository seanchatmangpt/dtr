# `CallSiteRecord`

> **Package:** `io.github.seanchatmangpt.dtr.reflectiontoolkit`  
> **Since:** `Java 26`  

Record capturing the call site (location in code) where a method invocation occurred. <p>This lightweight, immutable value object represents a point in the source code, useful for stack trace analysis, debugging, and reflection-based introspection. <p><strong>Usage Example:</strong> <pre>{@code CallSiteRecord site = new CallSiteRecord(     "com.example.UserService",     "createUser",     42 ); System.out.println(site.className());       // "com.example.UserService" System.out.println(site.methodName());      // "createUser" System.out.println(site.lineNumber());      // 42 }</pre> <p><strong>Compact Constructor Validation:</strong> The compact canonical constructor validates: <ul>   <li>{@code className} is not null or blank</li>   <li>{@code methodName} is not null or blank</li>   <li>{@code lineNumber} is positive (>= 0)</li> </ul>

```java
public record CallSiteRecord(String className, String methodName, int lineNumber) {
}
```

