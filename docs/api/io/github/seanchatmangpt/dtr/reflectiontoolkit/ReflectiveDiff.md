# `ReflectiveDiff`

> **Package:** `io.github.seanchatmangpt.dtr.reflectiontoolkit`  
> **Since:** `Java 25`  

Record capturing a single field-level difference between two object instances. <p>This immutable value object represents a comparison of a single field across two versions of an object, useful for generating detailed diff reports, change logs, and side-by-side comparisons in API documentation and test assertions. <p><strong>Usage Example:</strong> <pre>{@code ReflectiveDiff diff = new ReflectiveDiff(     "email",     "alice@example.com",     "alice.smith@example.com",     true ); System.out.println(diff.fieldName());          // "email" System.out.println(diff.beforeValueString());  // "alice@example.com" System.out.println(diff.afterValueString());   // "alice.smith@example.com" System.out.println(diff.changed());            // true }</pre> <p><strong>Typical Use in Testing:</strong> <pre>{@code // Document a field change during an update request User originalUser = ...;    // {email: "alice@example.com"} User updatedUser = ...;     // {email: "alice.smith@example.com"} ReflectiveDiff emailDiff = new ReflectiveDiff(     "email",     originalUser.getEmail(),     updatedUser.getEmail(),     !originalUser.getEmail().equals(updatedUser.getEmail()) ); sayNextSection("User Update Results"); sayAndMakeRequest(putRequest);  // PATCH /users/1 with updated data say("Field modified: " + emailDiff.fieldName()); }</pre> <p><strong>Compact Constructor Validation:</strong> The compact canonical constructor validates: <ul>   <li>{@code fieldName} is not null or blank</li>   <li>{@code beforeValueString} is not null (but may be empty or "null")</li>   <li>{@code afterValueString} is not null (but may be empty or "null")</li>   <li>The {@code changed} flag is consistent with value comparison (warning if inconsistent)</li> </ul>

```java
public record ReflectiveDiff( String fieldName, String beforeValueString, String afterValueString, boolean changed) {
}
```

