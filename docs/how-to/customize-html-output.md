# How-to: Customize HTML Output

## Add custom CSS

Create a file at:

```
src/test/resources/org/dtr/custom_dtr_stylesheet.css
```

DTR copies this file to the output directory and links it from every generated HTML page, after the default Bootstrap styles. Your rules override the defaults.

## Change the header color

```css
/* src/test/resources/org/dtr/custom_dtr_stylesheet.css */

.navbar-inverse {
    background-color: #1a3a5c;
    border-color: #0d2035;
}

a.navbar-brand {
    color: #4fc3f7 !important;
}
```

## Add a logo to the header

Embed the image as a base64 data URI to avoid external file dependencies:

```css
.navbar-brand::before {
    content: '';
    display: inline-block;
    width: 24px;
    height: 24px;
    margin-right: 8px;
    background-image: url('data:image/png;base64,iVBORw0KGgo...');
    background-size: contain;
    background-repeat: no-repeat;
    vertical-align: middle;
}
```

## Style assertion boxes

```css
/* Make pass boxes use your brand green */
.alert-success {
    background-color: #d4edda;
    border-color: #28a745;
    color: #155724;
}

/* Make fail boxes more prominent */
.alert-danger {
    background-color: #f8d7da;
    border-color: #dc3545;
    color: #721c24;
    font-weight: bold;
}
```

## Style request/response panels

```css
/* Request panel — blue accent */
.panel-primary {
    border-color: #1a3a5c;
}

.panel-primary > .panel-heading {
    background-color: #1a3a5c;
    border-color: #1a3a5c;
}

/* Response panel — neutral */
.panel-default > .panel-heading {
    background-color: #f5f5f5;
    font-family: 'Courier New', monospace;
}
```

## Change body font

```css
body {
    font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
    font-size: 15px;
    line-height: 1.6;
}

pre, code {
    font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace;
}
```

## Add raw HTML to documentation

Use `sayRaw(String)` to inject arbitrary HTML into the generated page:

```java
sayRaw("<div class='alert alert-info'>"
    + "<strong>Note:</strong> This endpoint is rate-limited to 100 req/min."
    + "</div>");
```

Or embed a table:

```java
sayRaw("""
    <table class="table table-bordered">
        <thead><tr><th>Status Code</th><th>Meaning</th></tr></thead>
        <tbody>
            <tr><td>200</td><td>Success</td></tr>
            <tr><td>400</td><td>Bad Request</td></tr>
            <tr><td>401</td><td>Unauthorized</td></tr>
            <tr><td>404</td><td>Not Found</td></tr>
            <tr><td>500</td><td>Server Error</td></tr>
        </tbody>
    </table>
    """);
```

## Change the output filename

By default the HTML file is named after the fully-qualified test class name. Override it:

```java
public class MyApiDocTest extends DTR {

    @Before
    public void configureOutputFile() {
        setClassNameForDTROutputFile("my-api-reference");
    }
}
```

This produces `target/site/dtr/my-api-reference.html`.

## Publishing the documentation

The output directory `target/site/dtr/` is self-contained:

- `index.html` — lists all DocTests
- `*.html` — individual DocTest pages
- `assets/` — Bootstrap and jQuery (bundled, no CDN dependency)

Copy the entire directory to any web server or GitHub Pages.
