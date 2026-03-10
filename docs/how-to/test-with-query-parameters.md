# How-to: Test with Query Parameters

## Add a single query parameter

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl()
            .path("/api/users")
            .addQueryParameter("search", "alice")));
```

Produces: `GET /api/users?search=alice`

## Add multiple query parameters

Chain `addQueryParameter` calls:

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl()
            .path("/api/articles")
            .addQueryParameter("page", "2")
            .addQueryParameter("pageSize", "20")
            .addQueryParameter("sortBy", "createdAt")
            .addQueryParameter("order", "desc")));
```

Produces: `GET /api/articles?page=2&pageSize=20&sortBy=createdAt&order=desc`

## URL-encode special characters

`addQueryParameter` handles URL encoding automatically:

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl()
            .path("/api/search")
            .addQueryParameter("q", "hello world & more")));
```

Produces: `GET /api/search?q=hello+world+%26+more`

## Build the URL separately for reuse

```java
Url searchUrl = testServerUrl()
    .path("/api/products")
    .addQueryParameter("category", "electronics")
    .addQueryParameter("inStock", "true");

Response response = sayAndMakeRequest(Request.GET().url(searchUrl));
```

## Complete example

```java
@Test
public void testPaginationAndFiltering() {

    sayNextSection("Pagination and Filtering");

    say("The /api/articles endpoint supports pagination via `page` and `pageSize` parameters, "
        + "and filtering via `author` and `tag` parameters.");

    say("Page 1 — first 10 articles:");

    Response page1 = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl()
                .path("/api/articles")
                .addQueryParameter("page", "1")
                .addQueryParameter("pageSize", "10")));

    sayAndAssertThat("First page returned", 200, equalTo(page1.httpStatus()));

    say("Filter by author:");

    Response filtered = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl()
                .path("/api/articles")
                .addQueryParameter("author", "alice")
                .addQueryParameter("pageSize", "5")));

    sayAndAssertThat("Filter works", 200, equalTo(filtered.httpStatus()));
}
```
