# Reference: HttpConstants

**Package:** `org.r10r.doctester.testbrowser`
**File:** `doctester-core/src/main/java/org/r10r/doctester/testbrowser/HttpConstants.java`

`HttpConstants` provides string constants for HTTP methods, content types, and header names. You rarely need to use these directly — they are used internally by `Request` and `TestBrowserImpl`.

---

## HTTP methods

| Constant | Value |
|---|---|
| `HttpConstants.HEAD` | `"HEAD"` |
| `HttpConstants.GET` | `"GET"` |
| `HttpConstants.POST` | `"POST"` |
| `HttpConstants.PUT` | `"PUT"` |
| `HttpConstants.PATCH` | `"PATCH"` |
| `HttpConstants.DELETE` | `"DELETE"` |

Use `Request.GET()`, `Request.POST()`, etc. instead.

---

## Content types

| Constant | Value |
|---|---|
| `HttpConstants.APPLICATION_JSON` | `"application/json"` |
| `HttpConstants.APPLICATION_JSON_WITH_CHARSET_UTF8` | `"application/json; charset=utf-8"` |
| `HttpConstants.APPLICATION_XML` | `"application/xml"` |
| `HttpConstants.APPLICATION_XML_WITH_CHARSET_UTF_8` | `"application/xml; charset=utf-8"` |

Use `Request.contentTypeApplicationJson()` or `Request.contentTypeApplicationXml()` instead.

---

## Header names

| Constant | Value |
|---|---|
| `HttpConstants.HEADER_CONTENT_TYPE` | `"Content-Type"` |
| `HttpConstants.HEADER_ACCEPT` | `"Accept"` |

---

## When to use HttpConstants directly

Use these constants when checking response headers:

```java
import org.r10r.doctester.testbrowser.HttpConstants;

Response response = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/data")));

String contentType = response.headers().get(HttpConstants.HEADER_CONTENT_TYPE);
sayAndAssertThat(
    "Response is JSON",
    contentType,
    containsString(HttpConstants.APPLICATION_JSON));
```
