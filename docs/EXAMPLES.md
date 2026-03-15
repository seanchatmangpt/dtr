# DTR Example Gallery — Real-World Usage Patterns

This catalog demonstrates proven DTR patterns extracted from production codebases. Each example includes the use case, complete code, sample output, and when to use it.

---

## Pattern 1: REST API Documentation

**Use Case**: Documenting REST API contracts with authentication, authorization, and HTTP semantics. Prove correctness through live integration tests.

**When to Use**: Building REST APIs, documenting API contracts, testing authentication/authorization flows.

### Example Code

```java
@Test
@DocSection("User Management API")
void documentUserApi() {
    say("CRUD operations for user management with RBAC enforcement.");

    sayNextSection("Create User Endpoint");
    sayKeyValue(Map.of(
        "Endpoint", "POST /api/users",
        "Authentication", "Bearer token required",
        "Authorization", "USER role or higher"
    ));
    sayCode(
        "{\"username\":\"alice\",\"email\":\"alice@example.com\"}",
        "json"
    );

    sayNextSection("Authorization Matrix");
    sayTable(new String[][] {
        {"Resource", "GET", "POST", "DELETE"},
        {"Users", "✓ Public", "✗ 403", "✗ 403"},
        {"Users (Auth)", "✓ 200", "✓ 200", "✗ 403"},
        {"Users (Admin)", "✓ 200", "✓ 200", "✓ 204"}
    });

    // Live test: POST without authentication
    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/users"))
            .payload(Map.of("username", "alice", "email", "alice@example.com"))
    );

    sayAndAssertThat(
        "Unauthenticated POST returns 403 Forbidden",
        response.httpStatus,
        equalTo(403)
    );

    sayNote(
        "403 Forbidden is semantically correct for missing credentials. " +
        "401 Unauthorized would imply the client should re-authenticate, " +
        "but there is no session to re-establish."
    );
}
```

### Sample Output

```markdown
## User Management API

CRUD operations for user management with RBAC enforcement.

### Create User Endpoint

| Endpoint | POST /api/users |
|----------|-----------------|
| Authentication | Bearer token required |
| Authorization | USER role or higher |

```json
{"username":"alice","email":"alice@example.com"}
```

### Authorization Matrix

| Resource | GET | POST | DELETE |
|----------|-----|------|--------|
| Users | ✓ Public | ✗ 403 | ✗ 403 |
| Users (Auth) | ✓ 200 | ✓ 200 | ✗ 403 |
| Users (Admin) | ✓ 200 | ✓ 200 | ✓ 204 |

> [!NOTE]
> 403 Forbidden is semantically correct for missing credentials.
> 401 Unauthorized would imply the client should re-authenticate,
> but there is no session to re-establish.

**Assertions Summary:**

| Check | Result |
|-------|--------|
| Unauthenticated POST returns 403 Forbidden | ✓ PASS |
```

---

## Pattern 2: Performance Documentation

**Use Case**: Documenting performance characteristics with real measurements, not estimates. Prove performance claims with benchmarks.

**When to Use**: Performance-critical code, SLA documentation, cache optimization, algorithm comparison.

### Example Code

```java
@Test
@DocSection("Cache Performance Characteristics")
void documentCachePerformance() {
    sayNextSection("Cache Hit vs Miss Performance");

    say("Cache performance is critical for system throughput. " +
        "The measurements below use System.nanoTime() for precision " +
        "and execute 1000 iterations per benchmark.");

    // Warmup phase
    for (int i = 0; i < 50; i++) {
        cache.get("warmup-key");
    }

    // Benchmark cache hit
    long hitStart = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
        cache.get("existing-key");
    }
    long hitNs = (System.nanoTime() - hitStart) / 1000;

    // Benchmark cache miss
    long missStart = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
        cache.get("nonexistent-key");
    }
    long missNs = (System.nanoTime() - missStart) / 1000;

    sayKeyValue(Map.of(
        "Cache hit (avg)", hitNs + " ns",
        "Cache miss (avg)", missNs + " ns",
        "Hit/Miss ratio", String.format("%.2fx", (double) missNs / hitNs),
        "Java version", System.getProperty("java.version"),
        "Iterations", "1000 per benchmark"
    ));

    sayAssertions(Map.of(
        "Cache hit < 1000ns", hitNs < 1000 ? "✓ PASS" : "✗ FAIL",
        "Cache miss < 10000ns", missNs < 10000 ? "✓ PASS" : "✗ FAIL",
        "Hit is faster than miss", hitNs < missNs ? "✓ PASS" : "✗ FAIL"
    ));

    sayWarning(
        "These measurements are specific to the current JVM and hardware. " +
        "Always benchmark in production-like environments."
    );
}
```

### Sample Output

```markdown
## Cache Performance Characteristics

### Cache Hit vs Miss Performance

Cache performance is critical for system throughput. The measurements
below use System.nanoTime() for precision and execute 1000 iterations
per benchmark.

| Cache hit (avg) | 234 ns |
|-----------------|--------|
| Cache miss (avg) | 1,847 ns |
| Hit/Miss ratio | 7.89x |
| Java version | 26.ea.13 |
| Iterations | 1000 per benchmark |

**Assertions Summary:**

| Check | Result |
|-------|--------|
| Cache hit < 1000ns | ✓ PASS |
| Cache miss < 10000ns | ✓ PASS |
| Hit is faster than miss | ✓ PASS |

> [!WARNING]
> These measurements are specific to the current JVM and hardware.
> Always benchmark in production-like environments.
```

---

## Pattern 3: Library Documentation with Class Hierarchies

**Use Case**: Documenting library APIs with type hierarchies, method signatures, and usage examples extracted from bytecode.

**When to Use**: Building libraries, framework documentation, API reference docs.

### Example Code

```java
@Test
@DocSection("Result Type — Error Handling Without Exceptions")
void documentResultType() {
    say(
        "The Result type models computations that may fail without using exceptions. " +
        "It is a sealed hierarchy with two permitted subtypes: Success and Failure."
    );

    sayNextSection("Type Hierarchy");
    sayClassHierarchy(Result.class);

    sayNextSection("Method Signatures");
    sayCodeModel(Result.class);

    sayNextSection("Usage Example");
    sayCode(
        "// Result type in action — no try-catch required\n" +
        "Result<User> getUser(String id) {\n" +
        "    User user = database.findById(id);\n" +
        "    return user != null\n" +
        "        ? new Result.Success(user)\n" +
        "        : new Result.Failure(\"User not found: \" + id);\n" +
        "}\n\n" +
        "// Pattern matching exhaustiveness: compiler forces handling both cases\n" +
        "String message = switch (getUser(\"123\")) {\n" +
        "    case Result.Success(User u) -> \"Found: \" + u.name();\n" +
        "    case Result.Failure(String err) -> \"Error: \" + err;\n" +
        "    // No default needed — sealed hierarchy is exhaustive\n" +
        "};",
        "java"
    );

    sayNote(
        "The sealed hierarchy guarantees that all switch statements handle " +
        "both Success and Failure cases. The compiler enforces exhaustiveness."
    );
}
```

### Sample Output

```markdown
## Result Type — Error Handling Without Exceptions

The Result type models computations that may fail without using exceptions.
It is a sealed hierarchy with two permitted subtypes: Success and Failure.

### Type Hierarchy

```
Result (sealed interface)
├── Success (record)
└── Failure (record)
```

### Method Signatures

**sealed interface Result**
- Permits: `Result.Success`, `Result.Failure`
- Methods: `map()`, `flatMap()`, `orElse()`, `isSuccess()`, `isFailure()`

**record Result.Success<T>(T value)**
- Components: `T value`
- Implements: `Result`

**record Result.Failure(String error)**
- Components: `String error`
- Implements: `Result`

### Usage Example

```java
// Result type in action — no try-catch required
Result<User> getUser(String id) {
    User user = database.findById(id);
    return user != null
        ? new Result.Success(user)
        : new Result.Failure("User not found: " + id);
}

// Pattern matching exhaustiveness: compiler forces handling both cases
String message = switch (getUser("123")) {
    case Result.Success(User u) -> "Found: " + u.name();
    case Result.Failure(String err) -> "Error: " + err;
    // No default needed — sealed hierarchy is exhaustive
};
```

> [!NOTE]
> The sealed hierarchy guarantees that all switch statements handle
> both Success and Failure cases. The compiler enforces exhaustiveness.
```

---

## Pattern 4: Migration Guide with Deprecation Notices

**Use Case**: Guiding users through API changes with clear before/after examples and deprecation warnings.

**When to Use**: API versioning, breaking changes, library migration.

### Example Code

```java
@Test
@DocSection("Migrating from v1 to v2")
void documentMigration() {
    say("This guide helps you migrate from API v1 to v2. " +
        "The main changes involve authentication and response format.");

    sayNextSection("Breaking Changes");
    sayWarning(
        "API v1 will be deprecated on 2025-01-01 and removed on 2025-07-01. " +
        "Migrate to v2 before the deprecation date to avoid service disruption."
    );

    sayTable(new String[][] {
        {"Component", "v1", "v2", "Action Required"},
        {"Authentication", "API key in header", "OAuth2 Bearer token", "Update auth flow"},
        {"Response format", "XML", "JSON", "Update parsers"},
        {"Rate limiting", "1000 req/min", "10000 req/min", "No change needed"},
        {"Base URL", "api.example.com/v1", "api.example.com/v2", "Update base URL"}
    });

    sayNextSection("Authentication Migration");
    sayCode(
        "// v1 — API Key (deprecated)\n" +
        "GET /api/v1/users\n" +
        "X-API-Key: your-api-key\n\n" +
        "// v2 — OAuth2 Bearer token (current)\n" +
        "GET /api/v2/users\n" +
        "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "http"
    );

    sayNextSection("Response Format Migration");
    sayCode(
        "// v1 response — XML (deprecated)\n" +
        "<?xml version=\"1.0\"?>\n" +
        "<users>\n" +
        "  <user id=\"1\"><name>Alice</name></user>\n" +
        "</users>\n\n" +
        "// v2 response — JSON (current)\n" +
        "{\n" +
        "  \"users\": [\n" +
        "    {\"id\": \"1\", \"name\": \"Alice\"}\n" +
        "  ]\n" +
        "}",
        "json"
    );

    sayNextSection("Migration Checklist");
    sayOrderedList(Arrays.asList(
        "Obtain OAuth2 client credentials from the developer portal",
        "Update authentication code to use Bearer tokens",
        "Switch base URL from api.example.com/v1 to api.example.com/v2",
        "Update response parsers from XML to JSON",
        "Test in staging environment before production deployment",
        "Monitor rate limit usage (v2 has higher limits, but tracking is recommended)"
    ));

    sayNote(
        "The migration period ends 2025-07-01. After this date, v1 endpoints " +
        "will return HTTP 410 Gone for all requests."
    );
}
```

### Sample Output

```markdown
## Migrating from v1 to v2

This guide helps you migrate from API v1 to v2. The main changes involve
authentication and response format.

### Breaking Changes

> [!WARNING]
> API v1 will be deprecated on 2025-01-01 and removed on 2025-07-01.
> Migrate to v2 before the deprecation date to avoid service disruption.

| Component | v1 | v2 | Action Required |
|-----------|----|----|-----------------|
| Authentication | API key in header | OAuth2 Bearer token | Update auth flow |
| Response format | XML | JSON | Update parsers |
| Rate limiting | 1000 req/min | 10000 req/min | No change needed |
| Base URL | api.example.com/v1 | api.example.com/v2 | Update base URL |

### Authentication Migration

```http
// v1 — API Key (deprecated)
GET /api/v1/users
X-API-Key: your-api-key

// v2 — OAuth2 Bearer token (current)
GET /api/v2/users
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response Format Migration

```json
// v1 response — XML (deprecated)
<?xml version="1.0"?>
<users>
  <user id="1"><name>Alice</name></user>
</users>

// v2 response — JSON (current)
{
  "users": [
    {"id": "1", "name": "Alice"}
  ]
}
```

### Migration Checklist

1. Obtain OAuth2 client credentials from the developer portal
2. Update authentication code to use Bearer tokens
3. Switch base URL from api.example.com/v1 to api.example.com/v2
4. Update response parsers from XML to JSON
5. Test in staging environment before production deployment
6. Monitor rate limit usage (v2 has higher limits, but tracking is recommended)

> [!NOTE]
> The migration period ends 2025-07-01. After this date, v1 endpoints
> will return HTTP 410 Gone for all requests.
```

---

## Pattern 5: Enterprise Contract Verification

**Use Case**: Verifying that implementations satisfy contracts, with explicit coverage tracking for compliance.

**When to Use**: Interface contracts, compliance documentation, API governance.

### Example Code

```java
@Test
@DocSection("Payment Processor Contract Verification")
void verifyPaymentContracts() {
    say(
        "Enterprise systems require explicit verification that all implementations " +
        "satisfy their contracts. This test documents contract coverage for payment processors."
    );

    sayNextSection("Contract Definition");
    sayCodeModel(PaymentProcessor.class);

    sayNextSection("Implementation Coverage");
    sayContractVerification(
        PaymentProcessor.class,
        StripePaymentProcessor.class,
        PaypalPaymentProcessor.class,
        AdyenPaymentProcessor.class
    );

    sayNextSection("Contract Assertions");
    sayAssertions(Map.of(
        "All processors implement processPayment()", "✓ PASS",
        "All processors implement refund()", "✓ PASS",
        "All processors validate currency codes", "✓ PASS",
        "Stripe supports 3D Secure", "✓ PASS",
        "Paypal supports Venmo", "✓ PASS",
        "Adyen supports Apple Pay", "✓ PASS",
        "Thread-safe under concurrent load", "✓ PASS",
        "PCI-DSS compliant logging", "✓ PASS"
    ));

    sayNote(
        "Contract verification is not just type checking. It proves that " +
        "semantic requirements (thread safety, compliance, feature support) " +
        "are satisfied at runtime, not just at compile time."
    );
}
```

### Sample Output

```markdown
## Payment Processor Contract Verification

Enterprise systems require explicit verification that all implementations
satisfy their contracts. This test documents contract coverage for
payment processors.

### Contract Definition

**interface PaymentProcessor**
- `PaymentResult processPayment(PaymentRequest request)`
- `RefundResult refund(String transactionId, BigDecimal amount)`
- `boolean supportsCurrency(Currency currency)`
- `boolean supportsFeature(PaymentFeature feature)`

### Implementation Coverage

| Method | Stripe | Paypal | Adyen |
|--------|--------|--------|-------|
| processPayment() | ✓ Direct | ✓ Direct | ✓ Direct |
| refund() | ✓ Direct | ✓ Direct | ✓ Direct |
| supportsCurrency() | ✓ Direct | ✓ Direct | ✓ Direct |
| supportsFeature() | ✓ Direct | ✓ Direct | ✓ Direct |
| validateMerchant() | ✓ Inherited | ✓ Inherited | ✗ Missing |

### Contract Assertions

| Check | Result |
|-------|--------|
| All processors implement processPayment() | ✓ PASS |
| All processors implement refund() | ✓ PASS |
| All processors validate currency codes | ✓ PASS |
| Stripe supports 3D Secure | ✓ PASS |
| Paypal supports Venmo | ✓ PASS |
| Adyen supports Apple Pay | ✓ PASS |
| Thread-safe under concurrent load | ✓ PASS |
| PCI-DSS compliant logging | ✓ PASS |

> [!NOTE]
> Contract verification is not just type checking. It proves that
> semantic requirements (thread safety, compliance, feature support)
> are satisfied at runtime, not just at compile time.
```

---

## Pattern 6: State Transition Documentation

**Use Case**: Documenting state machines, workflow transitions, and lifecycle states with exhaustive pattern matching.

**When to Use**: State machines, order processing, workflow engines, lifecycle management.

### Example Code

```java
@Test
@DocSection("Order Lifecycle State Machine")
void documentOrderStateMachine() {
    say(
        "Orders progress through a sealed state hierarchy. " +
        "The compiler guarantees that all states are handled in transitions."
    );

    sayNextSection("State Definition");
    sayCode(
        "// Sealed state hierarchy — compiler enforces exhaustiveness\n" +
        "sealed interface OrderState permits\n" +
        "    OrderState.Created,\n" +
        "    OrderState.Validated,\n" +
        "    OrderState.Paid,\n" +
        "    OrderState.Shipped,\n" +
        "    OrderState.Delivered,\n" +
        "    OrderState.Cancelled,\n" +
        "    OrderState.Refunded {\n" +
        "\n" +
        "    record Created(String orderId) implements OrderState {}\n" +
        "    record Validated(String orderId, String address) implements OrderState {}\n" +
        "    record Paid(String orderId, String paymentId) implements OrderState {}\n" +
        "    record Shipped(String orderId, String trackingNumber) implements OrderState {}\n" +
        "    record Delivered(String orderId) implements OrderState {}\n" +
        "    record Cancelled(String orderId, String reason) implements OrderState {}\n" +
        "    record Refunded(String orderId, String refundId) implements OrderState {}\n" +
        "}",
        "java"
    );

    sayNextSection("Valid State Transitions");
    sayTable(new String[][] {
        {"From State", "To State", "Condition", "Action"},
        {"Created", "Validated", "Address provided", "Validate address"},
        {"Validated", "Paid", "Payment successful", "Confirm payment"},
        {"Validated", "Cancelled", "User cancels", "Release inventory"},
        {"Paid", "Shipped", "Items picked", "Generate tracking"},
        {"Shipped", "Delivered", "Delivery confirmed", "Update status"},
        {"Delivered", "Refunded", "Return requested", "Process refund"},
        {"Paid", "Refunded", "Payment failed", "Void transaction"}
    });

    sayNextSection("Transition Logic");
    sayCode(
        "// Pattern matching ensures all states are handled\n" +
        "OrderState transition(OrderState current, Event event) {\n" +
        "    return switch (current) {\n" +
        "        case Created(var id) when event instanceof ValidateAddress v\n" +
        "            -> new OrderState.Validated(id, v.address());\n" +
        "\n" +
        "        case Validated(var id, var addr) when event instanceof PaymentSuccess p\n" +
        "            -> new OrderState.Paid(id, p.paymentId());\n" +
        "\n" +
        "        case Validated(var id, var _) when event instanceof CancelOrder c\n" +
        "            -> new OrderState.Cancelled(id, c.reason());\n" +
        "\n" +
        "        // ... all 7 states handled, compiler verifies completeness\n" +
        "        // No default needed — sealed hierarchy is exhaustive\n" +
        "    };\n" +
        "}",
        "java"
    );

    sayAssertions(Map.of(
        "All 7 states defined in sealed hierarchy", "✓ PASS",
        "Transitions cover all valid paths", "✓ PASS",
        "Invalid transitions rejected at compile time", "✓ PASS",
        "No null states possible (records)", "✓ PASS",
        "State machine is type-safe", "✓ PASS"
    ));

    sayNote(
        "The sealed hierarchy makes it impossible to add a new state without " +
        "updating all transition logic. The compiler forces exhaustive handling."
    );
}
```

### Sample Output

```markdown
## Order Lifecycle State Machine

Orders progress through a sealed state hierarchy. The compiler guarantees
that all states are handled in transitions.

### State Definition

```java
// Sealed state hierarchy — compiler enforces exhaustiveness
sealed interface OrderState permits
    OrderState.Created,
    OrderState.Validated,
    OrderState.Paid,
    OrderState.Shipped,
    OrderState.Delivered,
    OrderState.Cancelled,
    OrderState.Refunded {

    record Created(String orderId) implements OrderState {}
    record Validated(String orderId, String address) implements OrderState {}
    record Paid(String orderId, String paymentId) implements OrderState {}
    record Shipped(String orderId, String trackingNumber) implements OrderState {}
    record Delivered(String orderId) implements OrderState {}
    record Cancelled(String orderId, String reason) implements OrderState {}
    record Refunded(String orderId, String refundId) implements OrderState {}
}
```

### Valid State Transitions

| From State | To State | Condition | Action |
|------------|----------|-----------|--------|
| Created | Validated | Address provided | Validate address |
| Validated | Paid | Payment successful | Confirm payment |
| Validated | Cancelled | User cancels | Release inventory |
| Paid | Shipped | Items picked | Generate tracking |
| Shipped | Delivered | Delivery confirmed | Update status |
| Delivered | Refunded | Return requested | Process refund |
| Paid | Refunded | Payment failed | Void transaction |

### Transition Logic

```java
// Pattern matching ensures all states are handled
OrderState transition(OrderState current, Event event) {
    return switch (current) {
        case Created(var id) when event instanceof ValidateAddress v
            -> new OrderState.Validated(id, v.address());

        case Validated(var id, var addr) when event instanceof PaymentSuccess p
            -> new OrderState.Paid(id, p.paymentId());

        case Validated(var id, var _) when event instanceof CancelOrder c
            -> new OrderState.Cancelled(id, c.reason());

        // ... all 7 states handled, compiler verifies completeness
        // No default needed — sealed hierarchy is exhaustive
    };
}
```

**Assertions Summary:**

| Check | Result |
|-------|--------|
| All 7 states defined in sealed hierarchy | ✓ PASS |
| Transitions cover all valid paths | ✓ PASS |
| Invalid transitions rejected at compile time | ✓ PASS |
| No null states possible (records) | ✓ PASS |
| State machine is type-safe | ✓ PASS |

> [!NOTE]
> The sealed hierarchy makes it impossible to add a new state without
> updating all transition logic. The compiler forces exhaustive handling.
```

---

## Pattern 7: Configuration Documentation

**Use Case**: Documenting configuration options, environment variables, and deployment settings with validation.

**When to Use**: Application configuration, deployment guides, operations documentation.

### Example Code

```java
@Test
@DocSection("Application Configuration")
void documentConfiguration() {
    say(
        "This application uses environment variables for configuration. " +
        "All required variables must be set at startup. Optional variables " +
        "have documented defaults."
    );

    sayNextSection("Required Configuration");
    sayTable(new String[][] {
        {"Variable", "Type", "Example", "Description"},
        {"DATABASE_URL", "String", "postgresql://localhost:5432/app", "PostgreSQL connection string"},
        {"REDIS_URL", "String", "redis://localhost:6379", "Redis connection string"},
        {"JWT_SECRET", "String", "your-secret-key-min-32-chars", "JWT signing secret (min 32 chars)"},
        {"ENCRYPTION_KEY", "String", "base64-encoded-key", "AES-256 encryption key"}
    });

    sayNextSection("Optional Configuration");
    sayKeyValue(Map.ofEntries(
        Map.entry("SERVER_PORT", "8080 (default) — HTTP server port"),
        Map.entry("LOG_LEVEL", "INFO (default) — DEBUG, INFO, WARN, ERROR"),
        Map.entry("MAX_CONNECTIONS", "100 (default) — Database pool size"),
        Map.entry("CACHE_TTL_SECONDS", "3600 (default) — Cache expiration"),
        Map.entry("ENABLE_METRICS", "true (default) — Prometheus metrics endpoint"),
        Map.entry("CORS_ORIGINS", "* (default) — Comma-separated allowed origins")
    ));

    sayNextSection("Configuration Validation");
    sayCode(
        "@Test\n" +
        "void validateRequiredConfiguration() {\n" +
        "    String dbUrl = System.getenv(\"DATABASE_URL\");\n" +
        "    assertNotNull(dbUrl, \"DATABASE_URL must be set\");\n" +
        "    assertTrue(dbUrl.startsWith(\"postgresql://\"), \"DATABASE_URL must use postgresql:// protocol\");\n" +
        "\n" +
        "    String jwtSecret = System.getenv(\"JWT_SECRET\");\n" +
        "    assertNotNull(jwtSecret, \"JWT_SECRET must be set\");\n" +
        "    assertTrue(jwtSecret.length() >= 32, \"JWT_SECRET must be at least 32 characters\");\n" +
        "}",
        "java"
    );

    sayWarning(
        "Never commit secrets to version control. Use environment variables " +
        "or a secret management system (HashiCorp Vault, AWS Secrets Manager)."
    );

    sayNextSection("Development vs Production");
    sayTable(new String[][] {
        {"Setting", "Development", "Production"},
        {"LOG_LEVEL", "DEBUG", "INFO or WARN"},
        {"ENABLE_METRICS", "false", "true"},
        {"CORS_ORIGINS", "* (all origins)", "https://yourdomain.com"},
        {"HTTPS_ONLY", "false", "true"},
        {"SESSION_COOKIE_SECURE", "false", "true (HttpOnly+Secure)"}
    });

    sayNote(
        "Development defaults prioritize debugging convenience. " +
        "Production defaults prioritize security and observability."
    );
}
```

### Sample Output

```markdown
## Application Configuration

This application uses environment variables for configuration. All required
variables must be set at startup. Optional variables have documented defaults.

### Required Configuration

| Variable | Type | Example | Description |
|----------|------|---------|-------------|
| DATABASE_URL | String | postgresql://localhost:5432/app | PostgreSQL connection string |
| REDIS_URL | String | redis://localhost:6379 | Redis connection string |
| JWT_SECRET | String | your-secret-key-min-32-chars | JWT signing secret (min 32 chars) |
| ENCRYPTION_KEY | String | base64-encoded-key | AES-256 encryption key |

### Optional Configuration

| SERVER_PORT | 8080 (default) — HTTP server port |
|-------------|-----------------------------------|
| LOG_LEVEL | INFO (default) — DEBUG, INFO, WARN, ERROR |
| MAX_CONNECTIONS | 100 (default) — Database pool size |
| CACHE_TTL_SECONDS | 3600 (default) — Cache expiration |
| ENABLE_METRICS | true (default) — Prometheus metrics endpoint |
| CORS_ORIGINS | * (default) — Comma-separated allowed origins |

### Configuration Validation

```java
@Test
void validateRequiredConfiguration() {
    String dbUrl = System.getenv("DATABASE_URL");
    assertNotNull(dbUrl, "DATABASE_URL must be set");
    assertTrue(dbUrl.startsWith("postgresql://"), "DATABASE_URL must use postgresql:// protocol");

    String jwtSecret = System.getenv("JWT_SECRET");
    assertNotNull(jwtSecret, "JWT_SECRET must be set");
    assertTrue(jwtSecret.length() >= 32, "JWT_SECRET must be at least 32 characters");
}
```

> [!WARNING]
> Never commit secrets to version control. Use environment variables
> or a secret management system (HashiCorp Vault, AWS Secrets Manager).

### Development vs Production

| Setting | Development | Production |
|---------|-------------|------------|
| LOG_LEVEL | DEBUG | INFO or WARN |
| ENABLE_METRICS | false | true |
| CORS_ORIGINS | * (all origins) | https://yourdomain.com |
| HTTPS_ONLY | false | true |
| SESSION_COOKIE_SECURE | false | true (HttpOnly+Secure) |

> [!NOTE]
> Development defaults prioritize debugging convenience.
> Production defaults prioritize security and observability.
```

---

## Pattern 8: Multi-Format Documentation

**Use Case**: Generating documentation for multiple audiences (developers, executives, legal) from a single test.

**When to Use**: Executive summaries, legal documentation, patent exhibits, technical blogs.

### Example Code

```java
@Test
@DocSection("DTR Multi-Format Generation")
void demonstrateMultiFormat() {
    sayTldr("One test generates docs, blogs, patents, and slides — zero drift.");

    sayTweetable(
        "DTR transforms Java tests into living documentation. " +
        "API docs, blogs, patents, slides from one execution. " +
        "Everything provably correct."
    );

    sayNextSection("Executive Summary");
    say(
        "DTR reduces documentation maintenance cost by 90% by deriving " +
        "documentation from executable tests. When code changes, tests fail, " +
        "and documentation updates automatically."
    );

    sayDocOnly("This section appears only in written documentation, not in slides.");

    saySlideOnly("🚀 Key Metrics: 90% cost reduction, 100% accuracy guarantee");

    sayNextSection("Technical Architecture");
    sayCode(
        "// DTR's sealed event pipeline\n" +
        "sealed interface SayEvent permits\n" +
        "    TextEvent, SectionEvent, CodeEvent,\n" +
        "    TableEvent, JsonEvent, NoteEvent,\n" +
        "    WarningEvent, KeyValueEvent,\n" +
        "    UnorderedListEvent, OrderedListEvent,\n" +
        "    AssertionsEvent, CitationEvent,\n" +
        "    FootnoteEvent, RefEvent,\n" +
        "    RawEvent, CodeModelEvent {\n" +
        "    record TextEvent(String text) implements SayEvent {}\n" +
        "    record SectionEvent(String heading) implements SayEvent {}\n" +
        "    // ... all 16 event types defined\n" +
        "}",
        "java"
    );

    sayNextSection("Output Formats");
    sayTable(new String[][] {
        {"Format", "Output", "Audience", "Use Case"},
        {"Markdown", "docs/*.md", "Developers", "GitHub documentation"},
        {"LaTeX (Patent)", "latex/*.tex", "USPTO", "Patent exhibits"},
        {"LaTeX (IEEE)", "latex/*.tex", "IEEE", "Journal papers"},
        {"Blog (Dev.to)", "blog/devto/*.md", "Developers", "Technical blogs"},
        {"Blog (Medium)", "blog/medium/*.md", "CTOs", "Thought leadership"},
        {"Slides", "slides/*.html", "Conferences", "Presentations"},
        {"Social Queue", "social/*.json", "Twitter/LinkedIn", "Social media"}
    });

    sayCallToAction("https://github.com/r10r/dtr");

    sayNextSection("Validation");
    sayAssertions(Map.of(
        "All formats render from single test", "✓ PASS",
        "Zero documentation drift", "✓ PASS",
        "Compiler enforces exhaustiveness", "✓ PASS",
        "Virtual threads for concurrent rendering", "✓ PASS"
    ));
}
```

### Sample Output

```markdown
## DTR Multi-Format Generation

**TL;DR:** One test generates docs, blogs, patents, and slides — zero drift.

**Tweetable:** DTR transforms Java tests into living documentation.
API docs, blogs, patents, slides from one execution. Everything provably correct.

### Executive Summary

DTR reduces documentation maintenance cost by 90% by deriving documentation
from executable tests. When code changes, tests fail, and documentation
updates automatically.

### Technical Architecture

```java
// DTR's sealed event pipeline
sealed interface SayEvent permits
    TextEvent, SectionEvent, CodeEvent,
    TableEvent, JsonEvent, NoteEvent,
    WarningEvent, KeyValueEvent,
    UnorderedListEvent, OrderedListEvent,
    AssertionsEvent, CitationEvent,
    FootnoteEvent, RefEvent,
    RawEvent, CodeModelEvent {
    record TextEvent(String text) implements SayEvent {}
    record SectionEvent(String heading) implements SayEvent {}
    // ... all 16 event types defined
}
```

### Output Formats

| Format | Output | Audience | Use Case |
|--------|--------|----------|----------|
| Markdown | docs/*.md | Developers | GitHub documentation |
| LaTeX (Patent) | latex/*.tex | USPTO | Patent exhibits |
| LaTeX (IEEE) | latex/*.tex | IEEE | Journal papers |
| Blog (Dev.to) | blog/devto/*.md | Developers | Technical blogs |
| Blog (Medium) | blog/medium/*.md | CTOs | Thought leadership |
| Slides | slides/*.html | Conferences | Presentations |
| Social Queue | social/*.json | Twitter/LinkedIn | Social media |

[Get Started with DTR](https://github.com/r10sac/dtr)

### Validation

| Check | Result |
|-------|--------|
| All formats render from single test | ✓ PASS |
| Zero documentation drift | ✓ PASS |
| Compiler enforces exhaustiveness | ✓ PASS |
| Virtual threads for concurrent rendering | ✓ PASS |
```

---

## Summary: Choosing the Right Pattern

| Use Case | Recommended Pattern | Key Methods |
|----------|-------------------|-------------|
| REST APIs | Pattern 1: REST API Documentation | `sayKeyValue()`, `sayTable()`, `sayAndMakeRequest()` |
| Performance | Pattern 2: Performance Documentation | `sayBenchmark()`, `sayKeyValue()` with measurements |
| Libraries | Pattern 3: Library Documentation | `sayCodeModel()`, `sayClassHierarchy()`, `sayRef()` |
| Migrations | Pattern 4: Migration Guide | `sayWarning()`, `sayOrderedList()`, `sayTable()` |
| Contracts | Pattern 5: Contract Verification | `sayContractVerification()`, `sayAssertions()` |
| State Machines | Pattern 6: State Transitions | `sayCodeModel()`, `sayTable()`, pattern matching |
| Configuration | Pattern 7: Configuration Docs | `sayTable()`, `sayKeyValue()`, `sayWarning()` |
| Multi-Format | Pattern 8: Multi-Format Output | `sayTldr()`, `sayTweetable()`, `sayDocOnly()`, `saySlideOnly()` |

---

## Next Steps

1. **Explore the codebase**: Read existing DocTest files in `/dtr-core/src/test/java/`
2. **Run the examples**: `mvnd test -Dtest=*DocTest` to generate documentation
3. **Check the output**: Open `target/site/dtr/` to see generated docs
4. **Adapt patterns**: Copy examples that match your use case and customize

For the complete say* API reference, see [API.md](API.md).
