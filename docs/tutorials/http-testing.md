# Tutorial 2: REST API Documentation

**Target Audience**: Developers who completed Tutorial 1
**Duration**: 20 minutes
**Prerequisites**: Tutorial 1 (Introduction to DTR)

---

## What You'll Learn

After completing this tutorial, you will be able to:

- Document REST API endpoints with complete request/response specifications
- Document authentication requirements and security headers
- Document error responses with status codes and conditions
- Document rate limiting and pagination
- Organize API documentation by resource and operation
- Test API contracts and validate schemas

---

## Scenario: Documenting a User API

Imagine you're building a user management service with these endpoints:

- `GET /api/users/{id}` - Fetch a single user
- `GET /api/users` - List all users (with pagination)
- `POST /api/users` - Create a new user
- `PUT /api/users/{id}` - Update a user
- `DELETE /api/users/{id}` - Delete a user

Your goal: Create documentation that serves as both API reference and executable tests.

---

## Project Setup

Create a new test class for user API documentation:

```bash
# Create test file
mkdir -p src/test/java/com/example/doctest
touch src/test/java/com/example/doctest/UserApiDocTest.java
```

Basic structure:

```java
package com.example.doctest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.example.doctest.DtrContext;
import com.example.doctest.DtrContextField;
import com.example.doctest.DtrTest;

@DisplayName("User API Documentation")
class UserApiDocTest extends DtrTest {

    @DtrContextField
    DtrContext ctx;

    @Test
    @DisplayName("GET /api/users/{id}")
    @DocTestRef(id = "get-user-by-id")
    void getUserById() {
        ctx.sayNextSection("GET /api/users/{id}");
        ctx.say("Retrieves a single user by their ID.");

        // TODO: Document request, response, errors
    }
}
```

---

## Step 1: Document Request Specification

Document the HTTP method, URL, headers, and parameters:

```java
@Test
@DocTestRef(id = "get-user-by-id")
void getUserById() {
    ctx.sayNextSection("GET /api/users/{id}");
    ctx.say("Retrieves a single user by their ID.");

    ctx.sayNextSection("Request");
    ctx.sayKeyValue(Map.of(
        "Method", "GET",
        "URL", "/api/users/{id}",
        "Authentication", "Bearer token required",
        "Content-Type", "Not applicable (GET request)"
    ));

    ctx.sayNextSection("Path Parameters");
    ctx.sayTable(new String[][] {
        {"Parameter", "Type", "Description", "Example"},
        {"id", "integer", "Unique user identifier", "42"}
    });

    ctx.sayNextSection("Query Parameters");
    ctx.sayTable(new String[][] {
        {"Parameter", "Type", "Required", "Description"},
        {"fields", "string", "No", "Comma-separated list of fields to include"},
        {"include", "string", "No", "Related resources to include (e.g., 'profile')"}
    });

    ctx.sayNextSection("Request Example");
    ctx.sayCode("""
GET /api/users/42?fields=id,username,email&include=profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Host: api.example.com
""", "http");
}
```

---

## Step 2: Document Response Specification

Document the response body, headers, and status codes:

```java
ctx.sayNextSection("Response");

// Success response
ctx.sayCode("""
HTTP/1.1 200 OK
Content-Type: application/json
X-RateLimit-Remaining: 99
X-Request-ID: req_abc123

{
  "data": {
    "id": 42,
    "username": "alice",
    "email": "alice@example.com",
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2024-03-01T14:22:00Z"
  },
  "included": {
    "profile": {
      "bio": "Software developer",
      "location": "San Francisco"
    }
  }
}
""", "json");

// Document response schema
ctx.sayNextSection("Response Schema");
ctx.sayCode("""
interface UserResponse {
  data: {
    id: number;              // Unique user ID
    username: string;        // Unique username (3-30 chars)
    email: string;           // Valid email address
    created_at: string;      // ISO 8601 timestamp
    updated_at: string;      // ISO 8601 timestamp
  };
  included?: {
    profile?: {
      bio: string;
      location: string;
    }
  };
}
""", "typescript");

// Document response headers
ctx.sayNextSection("Response Headers");
ctx.sayTable(new String[][] {
    {"Header", "Description", "Example"},
    {"Content-Type", "Response format", "application/json"},
    {"X-RateLimit-Remaining", "Remaining requests", "99"},
    {"X-Request-ID", "Request tracker", "req_abc123"},
    {"ETag", "Resource version", "33a64df551425fcc"}
});
```

---

## Step 3: Document Error Responses

Document all possible error conditions:

```java
ctx.sayNextSection("Error Responses");

ctx.sayTable(new String[][] {
    {"Status", "Code", "Title", "Description"},
    {"400", "bad_request", "Invalid request", "Malformed request syntax or parameters"},
    {"401", "unauthorized", "Authentication required", "Missing or invalid Bearer token"},
    {"403", "forbidden", "Access denied", "Authenticated but not authorized"},
    {"404", "not_found", "User not found", "No user exists with the provided ID"},
    {"429", "rate_limit_exceeded", "Too many requests", "Rate limit exceeded (100 req/min)"}
});

// Detailed error examples
ctx.sayNextSection("Error Response Examples");

ctx.sayWarning("401 Unauthorized - Missing Token");
ctx.sayCode("""
HTTP/1.1 401 Unauthorized
Content-Type: application/json
WWW-Authenticate: Bearer

{
  "error": {
    "code": "unauthorized",
    "message": "Authentication required",
    "documentation_url": "https://api.example.com/docs/authentication"
  }
}
""", "json");

ctx.sayWarning("404 Not Found - Invalid User ID");
ctx.sayCode("""
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "error": {
    "code": "not_found",
    "message": "User not found",
    "details": {
      "resource_type": "User",
      "resource_id": "999"
    }
  }
}
""", "json");
```

---

## Step 4: Document Authentication

Document authentication mechanisms clearly:

```java
ctx.sayNextSection("Authentication");

ctx.say("This endpoint requires Bearer token authentication. Include the token in the Authorization header:");

ctx.sayCode("""
Authorization: Bearer <your_access_token>
""", "http");

ctx.sayNextSection("Obtaining an Access Token");

ctx.say("Tokens are obtained via the authentication endpoint:");

ctx.sayCode("""
POST /api/auth/token
Content-Type: application/json

{
  "client_id": "your_client_id",
  "client_secret": "your_client_secret",
  "grant_type": "client_credentials"
}
""", "json");

ctx.sayNote("Access tokens expire after 1 hour. Refresh tokens are valid for 30 days.");
```

---

## Step 5: Document Rate Limiting

Document usage limits and throttling behavior:

```java
ctx.sayNextSection("Rate Limiting");

ctx.sayKeyValue(Map.of(
    "Limit", "100 requests per minute",
    "Window", "Rolling 60-second window",
    "Headers", "X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset"
));

ctx.sayNextSection("Rate Limit Headers");
ctx.sayTable(new String[][] {
    {"Header", "Description", "Example"},
    {"X-RateLimit-Limit", "Request limit per window", "100"},
    {"X-RateLimit-Remaining", "Remaining requests", "95"},
    {"X-RateLimit-Reset", "Unix timestamp when limit resets", "1709251200"}
});

ctx.sayWarning("Exceeding the rate limit returns HTTP 429 with a Retry-After header.");
```

---

## Step 6: Complete Example

Full test class with all documentation:

```java
package com.example.doctest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.Map;
import static com.example.doctest.DtrContext.*;

@DisplayName("User API Documentation")
class UserApiDocTest extends DtrTest {

    @DtrContextField
    DtrContext ctx;

    @Test
    @DisplayName("GET /api/users/{id}")
    @DocTestRef(id = "get-user-by-id")
    void getUserById() {
        ctx.sayNextSection("GET /api/users/{id}");
        ctx.say("Retrieves a single user by their ID.");

        ctx.sayNextSection("Request");
        ctx.sayKeyValue(Map.of(
            "Method", "GET",
            "URL", "/api/users/{id}",
            "Authentication", "Bearer token required"
        ));

        ctx.sayNextSection("Path Parameters");
        ctx.sayTable(new String[][] {
            {"Parameter", "Type", "Description", "Example"},
            {"id", "integer", "Unique user identifier", "42"}
        });

        ctx.sayNextSection("Response");
        ctx.sayCode("""
{
  "data": {
    "id": 42,
    "username": "alice",
    "email": "alice@example.com"
  }
}
""", "json");

        ctx.sayNextSection("Error Responses");
        ctx.sayTable(new String[][] {
            {"Status", "Description"},
            {"404", "User not found"},
            {"401", "Unauthorized"}
        });

        ctx.sayNextSection("Authentication");
        ctx.say("Requires Bearer token in Authorization header.");
    }
}
```

---

## Step 7: Document POST Endpoint (Create User)

Document a request with a body:

```java
@Test
@DisplayName("POST /api/users")
@DocTestRef(id = "create-user")
void createUser() {
    ctx.sayNextSection("POST /api/users");
    ctx.say("Creates a new user account.");

    ctx.sayNextSection("Request");
    ctx.sayCode("""
POST /api/users
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "alice",
  "email": "alice@example.com",
  "password": "secure_password_123"
}
""", "http");

    ctx.sayNextSection("Request Body Schema");
    ctx.sayTable(new String[][] {
        {"Field", "Type", "Required", "Constraints"},
        {"username", "string", "Yes", "3-30 chars, alphanumeric + underscore"},
        {"email", "string", "Yes", "Valid email format"},
        {"password", "string", "Yes", "Min 12 chars, requires special char"}
    });

    ctx.sayNextSection("Response");
    ctx.sayCode("""
HTTP/1.1 201 Created
Location: /api/users/42
Content-Type: application/json

{
  "data": {
    "id": 42,
    "username": "alice",
    "email": "alice@example.com",
    "created_at": "2024-03-14T10:30:00Z"
  }
}
""", "json");

    ctx.sayNextSection("Error Responses");
    ctx.sayTable(new String[][] {
        {"Status", "Description"},
        {"400", "Validation error (invalid email, weak password)"},
        {"409", "Username or email already exists"},
        {"422", "Unprocessable entity (malformed JSON)"}
    });
}
```

---

## Step 8: Add Pagination Documentation

Document list endpoints with pagination:

```java
@Test
@DisplayName("GET /api/users (List)")
@DocTestRef(id = "list-users")
void listUsers() {
    ctx.sayNextSection("GET /api/users");
    ctx.say("Lists all users with pagination and filtering.");

    ctx.sayNextSection("Query Parameters");
    ctx.sayTable(new String[][] {
        {"Parameter", "Type", "Default", "Description"},
        {"page", "integer", "1", "Page number (1-indexed)"},
        {"per_page", "integer", "20", "Items per page (1-100)"},
        {"sort", "string", "created_at", "Sort field (username, email, created_at)"},
        {"order", "string", "asc", "Sort order (asc, desc)"}
    });

    ctx.sayNextSection("Response");
    ctx.sayCode("""
{
  "data": [
    {"id": 1, "username": "alice", "email": "alice@example.com"},
    {"id": 2, "username": "bob", "email": "bob@example.com"}
  ],
  "pagination": {
    "total": 150,
    "count": 20,
    "per_page": 20,
    "current_page": 1,
    "total_pages": 8,
    "links": {
      "next": "/api/users?page=2",
      "last": "/api/users?page=8"
    }
  }
}
""", "json");
}
```

---

## Testing API Contracts

Use DTR assertions to validate API responses:

```java
@Test
@DisplayName("GET /api/users/{id} - Contract Test")
@DocTestRef(id = "get-user-by-id-contract")
void getUserByIdContract() {
    ctx.sayNextSection("GET /api/users/{id} - Contract Validation");

    // Simulated API response
    String responseBody = """
        {
          "data": {
            "id": 42,
            "username": "alice",
            "email": "alice@example.com"
          }
        }
        """;

    // Document and validate
    ctx.sayNextSection("Response Validation");
    ctx.sayAndAssertThat("Response contains 'data' field",
        responseBody.contains("\"data\""), equalTo(true));

    ctx.sayAndAssertThat("Response contains 'id' field",
        responseBody.contains("\"id\""), equalTo(true));

    ctx.sayAndAssertThat("Response contains 'username' field",
        responseBody.contains("\"username\""), equalTo(true));

    ctx.sayNextSection("Schema Validation");
    ctx.sayKeyValue(Map.of(
        "id", "present: ✓",
        "username", "present: ✓",
        "email", "present: ✓",
        "created_at", "optional field"
    ));
}
```

---

## Exercise

**Task**: Document a `PUT /api/users/{id}` endpoint that updates user information.

**Requirements**:
1. Document the request body schema
2. Document partial updates (only provided fields are updated)
3. Document validation errors (400 status)
4. Document concurrent modification conflicts (409 status)
5. Include request/response examples

**Hint**: Use `sayTable()` for field validation rules and `sayWarning()` for conflict scenarios.

---

## Summary

In this tutorial, you learned:

- How to document REST API endpoints with complete specifications
- Documenting request parameters, headers, and body schemas
- Documenting response structures with examples
- Documenting error responses with status codes
- Documenting authentication and rate limiting
- Organizing API documentation by resource
- Adding contract tests to validate API behavior

---

## Next Tutorial

**Tutorial 3: Advanced Documentation Features**

Learn how to:
- Use Mermaid diagrams for API flows
- Document pagination and filtering
- Add versioning information
- Generate OpenAPI/Swagger specs
- Document webhooks and async operations

---

## Quick Reference: REST API Documentation

| Pattern | Use Case | Method |
|---------|----------|--------|
| Request spec | Method, URL, headers | `sayKeyValue()` |
| Request body | JSON payload | `sayCode(json, "json")` |
| Response schema | Data structure | `sayCode(typescript, "typescript")` |
| Error codes | Status mapping | `sayTable()` |
| Auth docs | Security requirements | `sayWarning()` |
| Rate limits | Usage thresholds | `sayKeyValue()` |
| Validation | Field constraints | `sayTable()` |
| Examples | Real usage | `sayCode(http, "http")` |

---

**Duration**: 20 minutes
**Difficulty**: Beginner
**Prerequisites**: Tutorial 1

For questions or issues, see [Documentation Overview](../DOCUMENTATION.md) or [Contributing Guide](../CONTRIBUTING.md).
