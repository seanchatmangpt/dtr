# Field Injection Guide

DTR supports multiple patterns for accessing `DtrContext` in your tests. This guide explains the **field injection** pattern, which allows you to declare the context once at the class level.

## Overview

Field injection provides a cleaner alternative to method parameter injection, especially for test classes with many test methods that all need documentation context.

## Quick Start

### Option 1: Field Injection (Recommended)

```java
@ExtendWith(DtrExtension.class)
class MyApiTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void listUsers() {
        ctx.say("Returns all users");
    }

    @Test
    void createUser() {
        ctx.say("Creates a new user");
    }
}
```

### Option 2: Parameter Injection (Still Valid)

```java
@ExtendWith(DtrExtension.class)
class MyApiTest {
    @Test
    void listUsers(DtrContext ctx) {
        ctx.say("Returns all users");
    }

    @Test
    void createUser(DtrContext ctx) {
        ctx.say("Creates a new user");
    }
}
```

### Option 3: Composite Annotation (Most Concise)

```java
@DtrTest
class MyApiTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void listUsers() {
        ctx.say("Returns all users");
    }
}
```

### Option 4: Inheritance (Legacy)

```java
class MyApiTest extends DtrTest {
    @Test
    void listUsers() {
        say("Returns all users");  // Direct say* method access
    }
}
```

## Comparison

| Pattern | Pros | Cons | Best For |
|---------|------|------|----------|
| **Field Injection** | Clean method signatures, one declaration | Slightly less explicit | Tests with many methods |
| **Parameter Injection** | Explicit dependencies | Verbose for many methods | Tests needing different setup per method |
| **@DtrTest** | Most concise | Annotation overhead | New projects |
| **Inheritance** | Direct say* access | Ties to base class | Legacy codebases |

## Features

### All Access Modifiers Supported

```java
@ExtendWith(DtrExtension.class)
class MyTest {
    @DtrContextField
    private DtrContext privateCtx;      // Works

    @DtrContextField
    protected DtrContext protectedCtx;   // Works

    @DtrContextField
    DtrContext packageCtx;               // Works (package-private)

    @DtrContextField
    public DtrContext publicCtx;         // Works
}
```

### Multiple Fields

```java
@ExtendWith(DtrExtension.class)
class MyTest {
    @DtrContextField
    private DtrContext ctx1;

    @DtrContextField
    private DtrContext ctx2;

    @Test
    void test() {
        ctx1.say("First context");
        ctx2.say("Second context");
        // Both share the same underlying RenderMachine
    }
}
```

### Coexistence with Parameter Injection

```java
@ExtendWith(DtrExtension.class)
class MyTest {
    @DtrContextField
    private DtrContext fieldCtx;

    @Test
    void test(DtrContext paramCtx) {
        fieldCtx.say("Via field");
        paramCtx.say("Via parameter");
        // Both work independently
    }
}
```

## Thread Safety

Each test method receives fresh `DtrContext` instances, but all instances share the same `RenderMachine`. This ensures:

- **Test isolation**: Each test gets its own context
- **Consistent output**: All documentation goes to the same file
- **Thread safety**: No shared mutable state between test methods

## Best Practices

1. **Use private fields** - Encapsulate the context from external access
2. **Name consistently** - Use `ctx` or `context` for clarity
3. **Prefer field injection** for test classes with 3+ methods
4. **Use parameter injection** when each test needs different setup
5. **Mix patterns as needed** - Both approaches work together

## Migration Guide

### From Parameter Injection to Field Injection

**Before:**
```java
@ExtendWith(DtrExtension.class)
class MyTest {
    @Test
    void test1(DtrContext ctx) { ctx.say("..."); }
    @Test
    void test2(DtrContext ctx) { ctx.say("..."); }
    @Test
    void test3(DtrContext ctx) { ctx.say("..."); }
}
```

**After:**
```java
@ExtendWith(DtrExtension.class)
class MyTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test1() { ctx.say("..."); }
    @Test
    void test2() { ctx.say("..."); }
    @Test
    void test3() { ctx.say("..."); }
}
```

### From Inheritance to Field Injection

**Before:**
```java
class MyTest extends DtrTest {
    @Test
    void test() {
        say("...");
    }
}
```

**After:**
```java
@DtrTest
class MyTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test() {
        ctx.say("...");
    }
}
```

## Troubleshooting

### Field is null after injection

Ensure your test class uses `@ExtendWith(DtrExtension.class)` or `@DtrTest`:

```java
// WRONG - No extension
class MyTest {
    @DtrContextField
    private DtrContext ctx;  // Will be null!
}

// CORRECT
@ExtendWith(DtrExtension.class)
class MyTest {
    @DtrContextField
    private DtrContext ctx;  // Injected correctly
}
```

### Wrong field type error

Only `DtrContext` type fields are supported:

```java
// WRONG
@DtrContextField
private String ctx;  // Error: Wrong type

// CORRECT
@DtrContextField
private DtrContext ctx;  // Correct type
```

## See Also

- [API Reference](../reference/api.md)
- [Annotation Reference](../reference/annotations.md)
- [Quick Start Guide](../QUICKSTART.md)
