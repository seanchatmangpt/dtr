# `AnnotationProfile`

> **Package:** `io.github.seanchatmangpt.dtr.reflectiontoolkit`  
> **Since:** `Java 26`  

Record capturing the set of annotations present on a Java class or member. <p>This immutable value object represents the reflection metadata for a single class, including the fully-qualified class name and the set of annotations that decorate it. The annotation list is defensively copied and made unmodifiable. <p><strong>Usage Example:</strong> <pre>{@code AnnotationProfile profile = new AnnotationProfile(     "com.example.UserService",     List.of(         "org.springframework.stereotype.Service",         "org.springframework.transaction.annotation.Transactional"     ) ); System.out.println(profile.className());           // "com.example.UserService" System.out.println(profile.annotationNames());     // [Service, Transactional] }</pre> <p><strong>Compact Constructor Validation:</strong> The compact canonical constructor validates: <ul>   <li>{@code className} is not null or blank</li>   <li>{@code annotationNames} list is not null (but may be empty)</li>   <li>No annotation names are null or blank</li>   <li>The list is defensively wrapped in {@link Collections#unmodifiableList(List)}</li> </ul>

```java
public record AnnotationProfile(String className, List<String> annotationNames) {
}
```

