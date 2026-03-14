# How-To: Verify Interface Contracts with sayContractVerification

Document that all implementations of an interface fulfill its contract using DTR 2.6.0's `sayContractVerification` method.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayContractVerification Does

`sayContractVerification(Class<?> contract, Class<?>... impls)` uses reflection to:
1. Enumerate all methods declared by the contract interface
2. Check that each implementation provides non-default implementations
3. Generate a coverage matrix table in the documentation

This replaces gRPC-specific testing guides, which relied on the removed HTTP stack. Contract verification is applicable to any interface — service layers, repositories, adapters.

---

## Basic Example

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class UserServiceContractDocTest {

    // Define the contract
    interface UserService {
        User findById(long id);
        User create(String name, String email);
        void update(long id, String name);
        void delete(long id);
        java.util.List<User> findAll();
    }

    record User(long id, String name, String email) {}

    // Production implementation
    static class UserServiceImpl implements UserService {
        public User findById(long id) { return new User(id, "alice", "alice@example.com"); }
        public User create(String name, String email) { return new User(1L, name, email); }
        public void update(long id, String name) { /* update logic */ }
        public void delete(long id) { /* delete logic */ }
        public java.util.List<User> findAll() { return java.util.List.of(); }
    }

    // Stub implementation for tests
    static class UserServiceStub implements UserService {
        public User findById(long id) { return new User(id, "stub", "stub@example.com"); }
        public User create(String name, String email) { return new User(99L, name, email); }
        public void update(long id, String name) { /* no-op */ }
        public void delete(long id) { /* no-op */ }
        public java.util.List<User> findAll() { return java.util.List.of(new User(1L, "stub", "stub@example.com")); }
    }

    @Test
    void verifyUserServiceContract(DtrContext ctx) {
        ctx.sayNextSection("UserService Contract Verification");
        ctx.say("All implementations of UserService must fulfill the following contract:");

        ctx.sayContractVerification(
            UserService.class,
            UserServiceImpl.class,
            UserServiceStub.class
        );

        ctx.sayNote("The coverage matrix shows which methods each implementation provides.");
    }
}
```

---

## Document the Contract with sayCallGraph

Use `sayCallGraph` to visualize how an implementation class's methods call each other:

```java
@Test
void documentCallGraph(DtrContext ctx) {
    ctx.sayNextSection("UserServiceImpl Call Graph");
    ctx.say("The following Mermaid diagram shows the internal call structure " +
            "of UserServiceImpl:");

    ctx.sayCallGraph(UserServiceImpl.class);
}
```

---

## Document a Sealed Result Type

Contract verification often involves sealed result types. Document them too:

```java
sealed interface ServiceResult<T> {
    record Success<T>(T value) implements ServiceResult<T> {}
    record NotFound(long id) implements ServiceResult<Object> {}
    record ValidationError(String field, String message) implements ServiceResult<Object> {}
}

@Test
void documentResultTypes(DtrContext ctx) {
    ctx.sayNextSection("Service Result Types");
    ctx.say("Service methods return a sealed ServiceResult type. " +
            "Callers must handle all cases exhaustively:");

    ctx.sayCode("""
        ServiceResult<User> result = userService.findById(42);
        String output = switch (result) {
            case ServiceResult.Success<User>(User u) -> "Found: " + u.name();
            case ServiceResult.NotFound(long id) -> "User " + id + " not found";
            case ServiceResult.ValidationError(String f, String m) -> f + ": " + m;
        };
        """, "java");

    ctx.sayNote("Using sealed types with pattern matching ensures all error cases " +
                "are handled at compile time.");
}
```

---

## Verify Multiple Contracts

```java
@Test
void verifyAllContracts(DtrContext ctx) {
    ctx.sayNextSection("Service Layer Contract Coverage");
    ctx.say("Verifying all service implementations against their contracts:");

    ctx.sayContractVerification(UserService.class, UserServiceImpl.class, UserServiceStub.class);
    ctx.sayContractVerification(OrderService.class, OrderServiceImpl.class);
    ctx.sayContractVerification(ProductService.class, ProductServiceImpl.class, ProductServiceMock.class);

    ctx.sayNote("Implementations marked MISSING are incomplete and must be fixed before release.");
}
```

---

## Best Practices

**Define contracts as interfaces.** Even if you only have one implementation, an interface makes the contract explicit and testable.

**Include stubs and mocks.** Verify that test doubles implement the full contract — partial stubs can hide missing behaviors.

**Combine with sayCallGraph.** After verifying the contract is fully implemented, visualize the internal call structure for maintainers.

**Use sealed result types.** Pattern match on `sealed interface` results instead of throwing exceptions — exhaustive switches catch missing cases at compile time.

---

## See Also

- [Document Call Graphs](grpc-streaming.md) — sayCallGraph details
- [Document Coverage Matrix](grpc-error-handling.md) — sayDocCoverage for broader coverage reporting
- [Pattern Matching with Sealed Records](pattern-matching.md) — Exhaustive type handling
