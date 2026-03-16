# DTR-013: Test Setup Annotations (@TestSetup and @AuthenticatedTest)

**Priority**: P4
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx, qol, test-authoring, annotations, setup, authentication

## Description

Introduce declarative annotations for common test setup patterns to reduce boilerplate and improve test readability. Two primary annotations will be created:

1. **@TestSetup**: Execute methods before documentation begins (pre-test setup, logging, configuration)
2. **@AuthenticatedTest**: Auto-populate `DtrContext` with authentication state for API testing

Currently, setup code must be manually written in each test or `@BeforeEach` method:

```java
@ExtendWith(DtrExtension.class)
class ApiTest {
    @BeforeEach
    void setup(DtrContext ctx) {
        ctx.sayKeyValue(Map.of("Environment", "test"));
        ctx.sayNote("Running in test mode");
    }

    @Test
    void testApi(DtrContext ctx) {
        ctx.sayKeyValue(Map.of("Auth", "Bearer test-token"));
        // actual test...
    }
}
```

With these annotations:

```java
@ExtendWith(DtrExtension.class)
@TestSetup(environment = "test", mode = "testing")
class ApiTest {
    @Test
    @AuthenticatedTest(token = "test-token", type = AuthType.BEARER)
    void testApi(DtrContext ctx) {
        // Setup and auth automatically documented!
    }
}
```

## Acceptance Criteria

### @TestSetup Annotation

- [ ] Create `@TestSetup` annotation with configurable parameters
- [ ] Support parameters: `environment`, `mode`, `description`, `tags[]`
- [ ] Execute setup before documentation rendering begins
- [ ] Automatically call `sayKeyValue()` with provided parameters
- [ ] Support `@TestSetup` at class level (applies to all tests) and method level
- [ ] Method-level annotation overrides class-level settings

### @AuthenticatedTest Annotation

- [ ] Create `@AuthenticatedTest` annotation for authentication scenarios
- [ ] Support parameters: `token`, `username`, `type` (enum: BEARER, BASIC, API_KEY)
- [ ] Support `tokenSource` (literal, environment variable, file path)
- [ ] Automatically inject authentication metadata into documentation
- [ ] Support multiple authentication schemes (OAuth2, JWT, Basic Auth)
- [ ] Include security warnings in generated docs

### Shared Requirements

- [ ] Both annotations work with `@DtrTest` and `@ExtendWith(DtrExtension.class)`
- [ ] Compatible with static imports (`import static io.github.seanchatmangpt.dtr.Dtr.*`)
- [ ] Comprehensive unit tests for all annotation combinations
- [ ] Integration tests demonstrating real-world usage patterns
- [ ] Update javadoc with examples for both annotations
- [ ] Add tutorial section: "Test Setup Best Practices"
- [ ] Ensure backward compatibility - existing tests work unchanged

## Technical Notes

### File Paths

- **Annotations**:
  - `src/main/java/io/github/seanchatmangpt/dtr/junit5/TestSetup.java`
  - `src/main/java/io/github/seanchatmangpt/dtr/junit5/AuthenticatedTest.java`
  - `src/main/java/io/github/seanchatmangpt/dtr/junit5/AuthType.java` (enum)
- **Extension logic**: Modify `DtrExtension` to process annotations
- **Tests**:
  - `src/test/java/io/github/seanchatmangpt/dtr/junit5/TestSetupTest.java`
  - `src/test/java/io/github/seanchatmangpt/dtr/junit5/AuthenticatedTestTest.java`

### Implementation Details

#### @TestSetup Annotation

```java
package io.github.seanchatmangpt.dtr.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures test setup metadata automatically.
 *
 * <p>Usage:
 * <pre>
 * &#64;TestSetup(
 *     environment = "test",
 *     mode = "integration",
 *     description = "Tests user registration flow"
 * )
 * class MyTest { }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TestSetup {
    String environment() default "test";
    String mode() default "testing";
    String description() default "";
    String[] tags() default {};
}
```

#### @AuthenticatedTest Annotation

```java
package io.github.seanchatmangpt.dtr.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks test as requiring authentication and documents auth metadata.
 *
 * <p>Usage:
 * <pre>
 * &#64;AuthenticatedTest(
 *     type = AuthType.BEARER,
 *     tokenSource = AuthTokenSource.ENV_VAR,
 *     tokenEnvVar = "TEST_AUTH_TOKEN"
 * )
 * &#64;Test
 * void testSecureEndpoint(DtrContext ctx) { }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AuthenticatedTest {
    AuthType type() default AuthType.BEARER;
    AuthTokenSource tokenSource() default AuthTokenSource.LITERAL;
    String token() default "";
    String tokenEnvVar() default "";
    String tokenFile() default "";
    String username() default "";
    String description() default "Requires authentication";
}

enum AuthType {
    BEARER, BASIC, API_KEY, OAUTH2, JWT
}

enum AuthTokenSource {
    LITERAL, ENV_VAR, FILE
}
```

#### DtrExtension Processing Logic

```java
private void processTestSetup(ExtensionContext context, DtrContext ctx) {
    // Check method-level annotation first
    TestSetup methodSetup = getTestMethod(context)
        .getAnnotation(TestSetup.class);

    // Fall back to class-level annotation
    TestSetup classSetup = getTestClass(context)
        .getAnnotation(TestSetup.class);

    TestSetup setup = (methodSetup != null) ? methodSetup : classSetup;

    if (setup != null) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("Environment", setup.environment());
        metadata.put("Mode", setup.mode());
        if (!setup.description().isEmpty()) {
            metadata.put("Description", setup.description());
        }
        ctx.sayKeyValue(metadata);
    }
}

private void processAuthenticatedTest(ExtensionContext context, DtrContext ctx) {
    AuthenticatedTest auth = getTestMethod(context)
        .getAnnotation(AuthenticatedTest.class);

    if (auth != null) {
        Map<String, String> authMetadata = new LinkedHashMap<>();
        authMetadata.put("Authentication", auth.type().name());

        String token = resolveToken(auth);
        if (!token.isEmpty()) {
            // Redact sensitive tokens in docs
            authMetadata.put("Token", token.substring(0, Math.min(10, token.length())) + "...");
        }

        ctx.sayKeyValue(authMetadata);
        ctx.sayWarning("Authentication credentials are test-only. Never use in production.");
    }
}
```

### Test Scenarios

1. **Class-level @TestSetup**: One annotation applies to all test methods
2. **Method-level @TestSetup**: Overrides class-level settings for specific test
3. **No @TestSetup**: Tests work as before (backward compatibility)
4. **@AuthenticatedTest with literal token**: Direct token value
5. **@AuthenticatedTest with env var**: Token from `System.getenv()`
6. **@AuthenticatedTest with file**: Token read from file path
7. **Combined @TestSetup + @AuthenticatedTest**: Both annotations on same test
8. **Multiple test classes**: Each class gets independent setup

### Security Considerations

- **Token redaction**: Never log full tokens in documentation
- **Environment variable support**: Avoid hardcoding credentials
- **File-based tokens**: Support for secure credential storage
- **Warning injection**: Auto-add security warnings to authenticated tests

## Dependencies

- **DTR-009** (@DtrTest unified annotation) - Should integrate seamlessly
- **DTR-012** (@AutoFinishDocTest) - Independent feature, can proceed in parallel

## References

- Related to: `@BeforeEach` lifecycle in JUnit 5
- Similar to: Spring Boot's `@TestPropertySource`
- Documentation: Update `docs/tutorials/http-testing.md` with auth examples
- Security: Follow OWASP guidelines for credential handling

## Success Metrics

- Reduces test setup boilerplate by 5-10 lines per test class
- Improves test readability (setup intent is declarative)
- Supports 80% of common setup patterns with annotations
- Maintains 100% backward compatibility
- Zero performance overhead (setup runs at same time as manual code)
