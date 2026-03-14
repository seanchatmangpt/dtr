# How-To: Use sayRecordComponents for Constraint Validation

Document the constraints and validation rules of your Java records using DTR 2.6.0's `sayRecordComponents` method.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayRecordComponents Does

`sayRecordComponents(Class<? extends Record>)` uses reflection to enumerate each component (field) of a record and generate a schema table showing:

- Component name
- Type
- Annotations (including validation annotations like `@NotNull`, `@Min`, `@Size`)

This replaces query parameter testing guides, which relied on the removed HTTP stack.

---

## Basic Schema Documentation

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class SearchRequestDocTest {

    record SearchRequest(
        String query,
        int page,
        int pageSize,
        String sortBy,
        String sortOrder
    ) {
        // Compact constructor for validation
        SearchRequest {
            if (query == null || query.isBlank())
                throw new IllegalArgumentException("query must not be blank");
            if (page < 1)
                throw new IllegalArgumentException("page must be >= 1, got: " + page);
            if (pageSize < 1 || pageSize > 100)
                throw new IllegalArgumentException("pageSize must be 1-100, got: " + pageSize);
            if (!java.util.Set.of("asc", "desc").contains(sortOrder))
                throw new IllegalArgumentException("sortOrder must be 'asc' or 'desc'");
        }
    }

    @Test
    void documentSearchRequest(DtrContext ctx) {
        ctx.sayNextSection("SearchRequest Constraints");
        ctx.say("The SearchRequest record enforces the following constraints in its compact constructor:");

        ctx.sayRecordComponents(SearchRequest.class);

        ctx.say("**Validation rules:**");
        ctx.sayTable(new String[][] {
            {"Field", "Type", "Constraint", "Default"},
            {"query", "String", "Not blank", "—"},
            {"page", "int", ">= 1", "1"},
            {"pageSize", "int", "1–100", "20"},
            {"sortBy", "String", "Any field name", "\"createdAt\""},
            {"sortOrder", "String", "\"asc\" or \"desc\"", "\"desc\""}
        });
    }
}
```

---

## Document Validation with Compact Constructors

Java 26 records support validation in the compact constructor. Document what happens when validation fails:

```java
@Test
void documentValidationErrors(DtrContext ctx) {
    ctx.sayNextSection("SearchRequest Validation Errors");
    ctx.say("The compact constructor throws IllegalArgumentException for invalid values:");

    // Document each validation error
    ctx.say("**Invalid page number:**");
    try {
        new SearchRequest("java", 0, 10, "name", "asc");
    } catch (IllegalArgumentException e) {
        ctx.sayException(e);
    }

    ctx.say("**Invalid page size:**");
    try {
        new SearchRequest("java", 1, 500, "name", "asc");
    } catch (IllegalArgumentException e) {
        ctx.sayException(e);
    }

    ctx.say("**Invalid sort order:**");
    try {
        new SearchRequest("java", 1, 10, "name", "ASCENDING");
    } catch (IllegalArgumentException e) {
        ctx.sayException(e);
    }

    ctx.sayWarning("All validation errors must be translated to HTTP 400 Bad Request " +
                   "by the API layer before returning to clients.");
}
```

---

## Document Multiple Related Records

Show the schema for an entire request/response pair:

```java
record CreateUserRequest(
    String name,
    String email,
    String password,
    String role
) {}

record CreateUserResponse(
    long id,
    String name,
    String email,
    String role,
    java.time.Instant createdAt
) {}

record UserListResponse(
    java.util.List<CreateUserResponse> users,
    int total,
    int page,
    int pageSize
) {}

@Test
void documentUserApiSchemas(DtrContext ctx) {
    ctx.sayNextSection("User API Schemas");

    ctx.say("**POST /api/users — Request:**");
    ctx.sayRecordComponents(CreateUserRequest.class);
    ctx.sayJson(new CreateUserRequest("alice", "alice@example.com", "secret123", "USER"));

    ctx.say("**POST /api/users — Response (201):**");
    ctx.sayRecordComponents(CreateUserResponse.class);
    ctx.sayJson(new CreateUserResponse(
        42L, "alice", "alice@example.com", "USER", java.time.Instant.now()));

    ctx.say("**GET /api/users — Response (200):**");
    ctx.sayRecordComponents(UserListResponse.class);
}
```

---

## Document a Pagination Record

```java
record PaginationParams(int page, int pageSize, String sortBy, String direction) {
    PaginationParams {
        if (page < 1) throw new IllegalArgumentException("page must be >= 1");
        if (pageSize < 1 || pageSize > 200) throw new IllegalArgumentException("pageSize must be 1-200");
        if (!java.util.Set.of("asc", "desc").contains(direction))
            throw new IllegalArgumentException("direction must be 'asc' or 'desc'");
    }

    static PaginationParams defaults() {
        return new PaginationParams(1, 20, "id", "asc");
    }
}

@Test
void documentPagination(DtrContext ctx) {
    ctx.sayNextSection("Pagination Parameters");
    ctx.say("All list endpoints support pagination via PaginationParams:");

    ctx.sayRecordComponents(PaginationParams.class);

    ctx.say("Default values:");
    ctx.sayJson(PaginationParams.defaults());

    ctx.say("The parameters map directly to query string parameters:");
    ctx.sayCode("GET /api/users?page=2&pageSize=50&sortBy=name&direction=asc", "http");
}
```

---

## Best Practices

**Use compact constructors for validation.** Java 26 records with compact constructors are the cleanest way to express invariants. The constraints live with the data type.

**Document defaults separately.** `sayRecordComponents` shows types and annotations but not default values. Add a `sayJson` call with a `defaults()` factory method to show defaults clearly.

**Pair with sayException.** After showing the schema, document what happens when constraints are violated.

**Use sealed types for multi-case validation results.** If validation can fail in many ways, return a sealed `ValidationResult` rather than throwing. Document the result type with `sayClassDiagram`.

---

## See Also

- [Document Record Schemas (deep-dive)](upload-files.md) — sayRecordComponents full detail
- [Document Exception Handling](test-xml-endpoints.md) — sayException for validation errors
- [Document JSON Payloads](test-json-endpoints.md) — sayJson for payload examples
