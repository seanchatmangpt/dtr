# How-to: Test XML Endpoints

## Send an XML request body

Set `Content-Type: application/xml` and pass a JAXB-annotated object:

```java
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CreateArticleRequest {
    public String title;
    public String body;
    public String author;
}
```

```java
var request = new CreateArticleRequest();
request.title = "My Article";
request.body = "Content here";
request.author = "Alice";

Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/articles.xml"))
        .contentTypeApplicationXml()
        .payload(request));
```

DTR serializes to XML using Jackson's `XmlMapper`. The HTML output shows the pretty-printed XML.

## Receive and deserialize an XML response

Use `response.payloadAs(MyClass.class)` — it auto-detects XML from the `Content-Type` header:

```java
@XmlRootElement
public class Article {
    public Long id;
    public String title;
    public String body;
}
```

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/articles/1.xml")));

Article article = response.payloadAs(Article.class);
sayAndAssertThat("Title correct", "My Article", equalTo(article.title));
```

## Force XML deserialization

If the server doesn't return the right `Content-Type`:

```java
Article article = response.payloadXmlAs(Article.class);
```

## Deserialize XML with generics

```java
import com.fasterxml.jackson.core.type.TypeReference;

@XmlRootElement
public class ArticleList {
    public List<Article> articles;
}

ArticleList list = response.payloadXmlAs(new TypeReference<ArticleList>() {});
```

## Accept header for XML

If the server uses content negotiation, also set the `Accept` header:

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/articles/1"))
        .addHeader("Accept", "application/xml"));
```

## Complete example

```java
@Test
public void testXmlEndpoints() {

    sayNextSection("XML API");

    say("The articles endpoint supports both JSON and XML. "
        + "Use the .xml suffix or Accept header to request XML.");

    @XmlRootElement
    class Article {
        public Long id;
        public String title;
    }

    Response response = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/articles.xml")));

    sayAndAssertThat("XML response received", 200, equalTo(response.httpStatus()));

    say("The response Content-Type is `application/xml` and the body "
        + "contains a standard XML document:");

    Article article = response.payloadAs(Article.class);
    sayAndAssertThat("Article has a title", article.title, notNullValue());
}
```
