# Explanation: Documentation Philosophy

This document explains the philosophy behind DocTester — why it was built this way, and what assumptions it makes about how API documentation should work.

---

## The documentation decay problem

API documentation has a fundamental problem: it lives separately from the code it describes.

A developer changes an endpoint. They update the tests (because broken tests fail the build). They forget to update the documentation (because outdated docs don't fail the build). Six months later, someone reads the docs, follows them exactly, and gets unexpected results. Trust in the documentation erodes.

This is the "documentation rot" problem, and it's endemic to software projects. The further documentation is from the code, the faster it rots.

---

## The DocTester approach: documentation is the test

DocTester's answer is radical: **make the documentation executable**.

Instead of writing tests and documentation separately, you write them together. The same code that asserts `response.httpStatus() == 200` also generates the paragraph that says "The server responds with 200 OK." If the behavior changes, the test fails — and you must fix the test — and fixing the test updates the documentation automatically.

This isn't a new idea. Python's doctest module pioneered it in 1999. DocTester brings the same philosophy to Java REST API testing.

---

## What this means for how you write tests

DocTester changes the mental model for test authorship.

**Traditional test:** "Does this code produce the correct output?"

**DocTest:** "Does this API behave as documented? And is the documentation accurate?"

The questions are related but not the same. A traditional test can be minimal: just enough to verify correctness. A DocTest must be comprehensible to a human reader who doesn't know your codebase.

This has practical consequences:

**DocTests should use realistic examples.** Don't test with `user.name = "foo"`. Use `user.name = "Alice"` — a real-looking name that makes the documentation readable.

**DocTests should tell a story.** A good DocTest walks through a typical workflow: here's how you authenticate, here's how you create a resource, here's what you get back, here's how you retrieve it. Each `say()` call is a sentence in that story.

**DocTests should document the normal path, not every edge case.** Edge cases (validation errors, rate limiting, unauthorized access) can be documented too, but the primary narrative should be what a developer does to successfully use the API.

---

## Say what matters, not everything

Not every HTTP call needs to be documented. The `makeRequest` vs `sayAndMakeRequest` distinction exists for this reason.

When you test a protected endpoint, you need to log in first. But login mechanics aren't the point of the section "Creating an Article" — they're plumbing. Log in silently with `makeRequest`, then document the interesting part with `sayAndMakeRequest`.

The documentation you generate is read by other developers. Ask yourself: "Would they need to know about this HTTP call to understand how to use the API?" If yes, use `sayAndMakeRequest`. If it's internal test scaffolding, use `makeRequest`.

---

## The relationship to Diataxis

[Diataxis](https://diataxis.fr/) is a documentation framework that identifies four types of documentation, each serving a different user need:

| Type | User state | Purpose |
|---|---|---|
| Tutorial | Learning | "Take me through a complete example" |
| How-to guide | Working | "Help me accomplish this specific task" |
| Reference | Looking up | "Tell me exactly what this does" |
| Explanation | Studying | "Help me understand why" |

DocTester generates output that sits somewhere between **reference** and **tutorial** — it shows what an API does (reference) by walking through a worked example (tutorial). The narrative text you write with `say()` is the tutorial layer; the request/response panels are the reference layer.

Good DocTest output does both. A developer scanning the panels can see the API mechanics at a glance. A developer reading the text understands the intent and context.

This documentation site you're reading now is organized around Diataxis — tutorials, how-to guides, reference, and explanation. DocTester's generated output is a complementary form of documentation that focuses specifically on demonstrating API behavior through running examples.

---

## Limitations to accept

DocTester's approach has trade-offs to be aware of:

**Tests must be integration tests.** DocTester requires a running server to make HTTP requests. You can't generate documentation from unit tests. This is the right trade-off for API documentation — readers want to see real requests against a real server — but it means DocTester belongs in your integration test phase, not unit test phase.

**Documentation is per test class.** One test class = one HTML page. If your API is large, organize it into multiple DocTest classes, each covering a logical section.

**Output is read-only HTML.** DocTester generates static HTML files. If you want interactive documentation (a live Swagger UI, for example), DocTester isn't the right tool. It's best for generating human-readable API reference pages that can be hosted on any static site.

**JUnit 4 only.** DocTester currently uses JUnit 4's `@Before`/`@AfterClass` lifecycle. JUnit 5 support would require a different extension mechanism. If your project has migrated to JUnit 5, you can still use DocTester if you add `junit-vintage-engine` to run JUnit 4 tests.
