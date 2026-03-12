# How to Add Images, Tables, and Code Examples

## Problem
Plain text documentation is bland. You need to include images, structured data tables, code snippets with syntax highlighting, warnings, and other rich content to make your docs clear and professional.

## Solution Overview

DTR supports all standard Markdown elements plus extended features. Content renders consistently across all export formats (HTML, PDF, slides, blog).

## Images

### Basic Image Syntax
```markdown
![Alt text describing the image](path/to/image.png)

![Screenshot of dashboard](images/dashboard.png)
```

### With Captions and Size
```markdown
![API flow diagram](images/api-flow.png "API Request/Response Flow")
_Figure 1: Typical request-response cycle_

![Mobile mockup](images/mobile.png "Width: 300px")
```

**Best Practices:**
- Use relative paths: `images/screenshot.png` (relative to `.md` file)
- Supported formats: PNG, JPG, GIF, SVG
- For PDF: ensure images are in vector format (SVG) when possible
- For web: optimize images (<100KB for quick loading)

## Tables

### Simple Markdown Table
```markdown
| Header 1 | Header 2 | Header 3 |
|----------|----------|----------|
| Row 1 Col 1 | Row 1 Col 2 | Row 1 Col 3 |
| Row 2 Col 1 | Row 2 Col 2 | Row 2 Col 3 |
```

### Aligned Columns
```markdown
| Left | Center | Right |
|:-----|:------:|-------:|
| Text | Data | 1234 |
| More | Values | 5678 |
```

### Complex Table Example
```markdown
| Method | Endpoint | Auth | Rate Limit |
|--------|----------|------|-----------|
| GET | `/api/users` | Bearer | 100/min |
| POST | `/api/users` | Bearer | 10/min |
| DELETE | `/api/users/{id}` | Bearer | 10/min |
```

Tables work in all formats. For complex layouts in PDF, consider code blocks instead.

## Code Examples

### Basic Code Block
```markdown
\`\`\`python
def greet(name):
    print(f"Hello, {name}!")
\`\`\`
```

### With Language-Specific Syntax Highlighting
```markdown
\`\`\`java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
\`\`\`

\`\`\`bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer TOKEN"
\`\`\`

\`\`\`json
{
  "name": "Alice",
  "email": "alice@example.com",
  "active": true
}
\`\`\`

\`\`\`sql
SELECT id, name, email FROM users WHERE active = 1;
\`\`\`
```

### With Line Numbers and Highlighting
```markdown
\`\`\`java {3-5}
public class Example {
    public void demo() {
        System.out.println("Line 3");  // Highlighted
        System.out.println("Line 4");  // Highlighted
        System.out.println("Line 5");  // Highlighted
    }
}
\`\`\`
```

Supported languages: python, java, javascript, sql, bash, xml, yaml, html, css, and 100+.

## Callouts (Alerts)

### NOTE (Informational)
```markdown
> [!NOTE]
> This is helpful context that enhances understanding.
```

### WARNING (Critical)
```markdown
> [!WARNING]
> Do not use this in production without testing.
```

### TIP (Helpful Suggestion)
```markdown
> [!TIP]
> Consider using this approach for better performance.
```

Callouts render as styled boxes in HTML/PDF/slides. They're visually distinct from body text.

## Math Equations (LaTeX/PDF)

### Inline Math
```markdown
The Pythagorean theorem: $a^2 + b^2 = c^2$
```

### Display Math
```markdown
$$
E = mc^2
$$

$$
\sum_{i=1}^{n} i = \frac{n(n+1)}{2}
$$
```

Math expressions use LaTeX syntax. Rendered as images in HTML, native in PDF.

## Embedded Content

### Links
```markdown
[Link text](https://example.com)

[Internal section](#section-title)

[Reference to code](path/to/file.java)
```

### Horizontal Rule
```markdown
---
```

### Lists (Ordered)
```markdown
1. First step
2. Second step
3. Third step
```

### Lists (Unordered)
```markdown
- Item A
- Item B
- Item C
```

### Nested Lists
```markdown
- Parent item
  - Child item
  - Another child
- Another parent
```

## Complete Example

```markdown
# API Documentation

## Users Endpoint

The `/api/users` endpoint manages user records.

![User flow diagram](images/user-flow.png "User Creation Flow")

### Endpoint Details

| Property | Value |
|----------|-------|
| **Base URL** | `https://api.example.com` |
| **Authentication** | Bearer Token |
| **Rate Limit** | 100 requests/minute |

### Create a User (POST)

Send a POST request with user data:

\`\`\`bash
curl -X POST https://api.example.com/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "Alice",
    "email": "alice@example.com"
  }'
\`\`\`

Response (201 Created):

\`\`\`json
{
  "id": 123,
  "name": "Alice",
  "email": "alice@example.com",
  "createdAt": "2026-03-11T12:00:00Z"
}
\`\`\`

> [!NOTE]
> Email must be unique across all users. Duplicates will be rejected.

> [!WARNING]
> Do not expose your Bearer token in public repositories.

### Success Criteria

- ✓ HTTP 201 status code
- ✓ Response includes user ID
- ✓ User can log in immediately
```

## Exporting with Rich Content

All rich content (images, tables, code) renders correctly in all formats:

```bash
# Export preserving all formatting
dtr export guide.md output.pdf --format latex

dtr export guide.md output.html --format html --highlight-theme atom-dark

dtr export guide.md slides.html --format slides
```

## Tips & Best Practices

1. **Keep images small** — compress before including
2. **Use consistent table styling** — readers expect uniformity
3. **Label code blocks** — always include language identifier
4. **Use callouts sparingly** — they lose impact if overused
5. **Link related sections** — help readers navigate

## Troubleshooting

**"Image not found"** → Use relative paths, verify image exists in repository

**"Table formatting broken"** → Ensure pipes `|` align vertically

**"Code highlighting missing"** → Specify language: `` ```python `` not `` ``` ``

**"Math not rendering"** → LaTeX syntax only works in PDF; use images for web

## Next Steps
- [Export in Different Formats](formats.md)
- [Batch Export Multiple Files](batch-export.md)
