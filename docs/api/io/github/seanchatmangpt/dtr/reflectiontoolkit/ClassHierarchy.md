# `ClassHierarchy`

> **Package:** `io.github.seanchatmangpt.dtr.reflectiontoolkit`  
> **Since:** `Java 25`  

Record capturing the inheritance and interface hierarchy of a Java class. <p>This immutable record represents the type relationships for a class, storing the chain of superclasses (from direct parent to java.lang.Object) and the set of interfaces directly implemented. Both lists are defensively copied and made unmodifiable. <p><strong>Usage Example:</strong> <pre>{@code ClassHierarchy hierarchy = new ClassHierarchy(     Collections.unmodifiableList(List.of(         "com.example.AbstractService",         "java.lang.Object"     )),     Collections.unmodifiableList(List.of(         "java.io.Serializable",         "java.lang.Cloneable"     )) ); System.out.println(hierarchy.superclassChainNames());        // [AbstractService, Object] System.out.println(hierarchy.implementedInterfaceNames());   // [Serializable, Cloneable] }</pre> <p><strong>Compact Constructor Validation:</strong> The compact canonical constructor validates: <ul>   <li>Both lists are not null (but may be empty)</li>   <li>No class/interface names are null or blank</li>   <li>Both lists are defensively wrapped in {@link Collections#unmodifiableList(List)}</li> </ul>

```java
public record ClassHierarchy( List<String> superclassChainNames, List<String> implementedInterfaceNames) {
}
```

