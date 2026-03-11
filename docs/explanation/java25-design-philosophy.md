# Explanation: Java 25 Design Philosophy

Java 25 brings together virtual threads, records, sealed classes, and pattern matching into a coherent design philosophy. This document explains how they work together to enable simpler, safer, more scalable code.

---

## The Core Vision: From Complexity to Clarity

Java's design philosophy has evolved over decades. Java 25 represents a shift toward:

**From:** Complex frameworks + intricate threading + boilerplate
**To:** Simple code + lightweight concurrency + transparent data models

---

## Four Pillars of Java 25

### 1. Lightweight Concurrency (Virtual Threads)

**Problem:** Platform threads are expensive. Building scalable services requires callbacks, reactive frameworks, and expertise in async patterns.

**Solution:** Virtual threads allow writing natural blocking code that scales to millions of concurrent tasks.

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // Code reads sequentially; scales like async
    executor.submit(() -> {
        Response resp = client.get(url);      // Blocks naturally
        Data data = parseJson(resp);          // Sequential code
        db.insert(data);                      // JVM handles scaling
    });
}
```

**Impact:** Services can handle 10x more concurrent connections with the same memory.

---

### 2. Type-Safe Data Models (Records + Sealed Classes)

**Problem:** Data carriers require boilerplate. Open class hierarchies lose type safety in pattern matching.

**Solution:** Records eliminate boilerplate; sealed classes enforce exhaustiveness.

```java
sealed interface ApiPayload {
    record Success(String data) implements ApiPayload {}
    record Error(int code, String message) implements ApiPayload {}
}

// Compiler guarantees all cases handled
String describe(ApiPayload payload) {
    return switch (payload) {
        case ApiPayload.Success s -> "OK: " + s.data();
        case ApiPayload.Error e -> e.code() + ": " + e.message();
    };
}
```

**Impact:** Less boilerplate, more type safety, fewer runtime errors.

---

### 3. Natural Pattern Matching

**Problem:** Casting and `instanceof` checks are verbose and error-prone.

**Solution:** Pattern matching allows destructuring types inline.

```java
// Old
if (result instanceof ApiResult) {
    ApiResult r = (ApiResult) result;
    String data = r.getData();
}

// Java 25
if (result instanceof ApiResult(String data)) {
    System.out.println(data);  // Destructured automatically
}

// With sealed types: exhaustive matching
String outcome = switch (result) {
    case ApiResult.Success(String data) -> "OK: " + data;
    case ApiResult.Failure(String error) -> "FAIL: " + error;
};
```

**Impact:** Code that expresses intent directly, with compiler verification.

---

### 4. Immutable-by-Default Data

**Problem:** Mutable data causes concurrency bugs, requires synchronization, complicates reasoning.

**Solution:** Records are immutable; side effects are explicit.

```java
// Record: immutable by design
record User(String name, int age) {}

User u1 = new User("alice", 30);
// u1.name = "bob";  // ❌ Compile error

// Modify via explicit construction
User u2 = new User("bob", 30);

// Thread-safe: no synchronization needed
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        User u = u1;  // Safe: immutable
        process(u);
    });
}
```

**Impact:** Thread-safe by default; no hidden side effects.

---

## How They Work Together

These four features are **synergistic** — each makes the others more powerful.

### Virtual Threads + Records + Pattern Matching

**Scenario:** Handle many concurrent API requests, returning typed responses.

```java
sealed interface Request {
    record GetUser(int id) implements Request {}
    record CreateUser(String name, String email) implements Request {}
}

sealed interface Response {
    record UserFound(String name, String email) implements Response {}
    record UserNotFound(int id) implements Response {}
    record Created(int id) implements Response {}
    record BadRequest(String field, String reason) implements Response {}
}

// Handle 1000 concurrent requests
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (Request req : requests) {
        executor.submit(() -> {
            // Natural sequential code
            Response response = switch (req) {
                case Request.GetUser(int id) -> {
                    User user = db.findUser(id);  // Blocks naturally
                    yield user != null
                        ? new Response.UserFound(user.name, user.email)
                        : new Response.UserNotFound(id);
                }
                case Request.CreateUser(String name, String email) -> {
                    int newId = db.insertUser(name, email);  // Blocks naturally
                    yield new Response.Created(newId);
                }
            };

            // Exhaustive matching: if you add a request type, compiler errors
            sendResponse(response);  // Type-safe response
        });
    }
}
```

**Benefits:**
- ✅ Scalable: handles 1000 concurrent requests on a few platform threads
- ✅ Safe: sealed types guarantee all request/response cases handled
- ✅ Clear: code reads naturally, doesn't obscure intent with callbacks
- ✅ Efficient: immutable data, no thread safety overhead

---

## Comparison: Old vs. New Approaches

### Old Approach (Pre-Java 25)

```java
// Callback-based async
ExecutorService threadPool = Executors.newFixedThreadPool(100);  // Fixed size!

interface ApiRequest {
    void handle(Consumer<ApiResponse> callback);
}

class GetUser implements ApiRequest {
    int id;
    public void handle(Consumer<ApiResponse> callback) {
        threadPool.submit(() -> {
            try {
                User user = db.findUser(id);
                callback.accept(new UserResponse(user));
            } catch (Exception e) {
                callback.accept(new ErrorResponse(e));
            }
        });
    }
}

// Callback pyramid
request.handle(response -> {
    if (response instanceof UserResponse) {
        process((UserResponse) response);
    } else if (response instanceof ErrorResponse) {
        handleError((ErrorResponse) response);
    }
});
```

**Drawbacks:**
- ❌ Fixed thread pool size (bottleneck)
- ❌ Callback nesting (hard to read)
- ❌ Manual type checks + casts (error-prone)
- ❌ Exception handling scattered
- ❌ Stack traces useless (all in callback pool)

### Java 25 Approach

```java
sealed interface Request permits GetUser, CreateUser { }
sealed interface Response permits UserFound, UserNotFound, Created { }

// Natural sequential code, automatic scaling
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        Response response = switch (request) {
            case GetUser(int id) -> {
                User user = db.findUser(id);  // Blocks naturally
                yield user != null
                    ? new UserFound(user.name)
                    : new UserNotFound(id);
            }
            case CreateUser(String name, _) -> new Created(name);
        };
        sendResponse(response);
    });
}
```

**Benefits:**
- ✅ Unlimited concurrency (virtual threads scale)
- ✅ Sequential code (easy to read)
- ✅ Type-safe patterns (exhaustive matching)
- ✅ Structured error handling
- ✅ Readable stack traces

---

## Design Trade-offs

### Memory vs. Simplicity

**Virtual Threads:**
- Trade: Slightly more memory per virtual thread than single async task
- Gain: Dramatically simpler code, no callback chains

### Strict Immutability vs. Flexibility

**Records:**
- Trade: Can't mutate; must create new instances
- Gain: Thread-safe, hashable, predictable equality

### Bounded Hierarchies vs. Open Extension

**Sealed Classes:**
- Trade: Can't extend outside the sealed list
- Gain: Exhaustive pattern matching, compiler verification

These trade-offs are **intentional:** Java 25 prioritizes **safety and clarity over raw flexibility**.

---

## When to Use Java 25 Features

| Feature | Use When | Avoid When |
|---------|----------|-----------|
| Virtual Threads | I/O-bound workloads at scale | CPU-intensive compute, real-time systems |
| Records | Data carriers, DTOs, immutable values | Complex objects with mutable state |
| Sealed Classes | Fixed type hierarchies, pattern matching | Open frameworks, plugin architectures |
| Pattern Matching | Type-safe case handling | Simple type checks |

---

## The Bigger Vision

Java 25 is moving toward a model where:

1. **Concurrency is built-in**, not bolted-on
2. **Data is transparent and immutable** by default
3. **Types are verified at compile time**, not runtime
4. **Code expresses intent clearly**, hiding complexity

This reflects modern understanding:
- Shared mutable state is the root of many bugs
- Compiler verification prevents whole classes of errors
- Scalability doesn't require frameworks
- Natural, readable code is achievable at scale

---

## Ecosystem Impact

As Java 25 features mature:
- **Frameworks simplify** — no need for complex async machinery
- **Onboarding improves** — new developers write natural code, not callbacks
- **Performance scales** — services handle more load with same resources
- **Reliability increases** — fewer runtime surprises

---

## See Also

- [Tutorial: Virtual Threads](../tutorials/virtual-threads-lightweight-concurrency.md)
- [Tutorial: Records and Sealed Classes](../tutorials/records-sealed-classes.md)
- [Explanation: Why Virtual Threads Matter](virtual-threads-philosophy.md)
- [Explanation: Why Records and Sealed Classes](records-sealed-philosophy.md)
