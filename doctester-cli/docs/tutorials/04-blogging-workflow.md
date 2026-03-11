# Publishing to Dev.to, Medium, and Other Blogging Platforms

DocTester CLI helps you reach multiple audiences by publishing your documentation to popular blogging platforms. Write once, publish everywhere!

## Supported Blogging Platforms

DocTester CLI supports exporting to:

| Platform | Best For | Audience |
| --- | --- | --- |
| **Dev.to** | Developer articles, tutorials | Developers & tech community |
| **Medium** | Long-form technical writing | General audience |
| **Hashnode** | Developer blogs, dev stories | Developers & engineers |
| **LinkedIn** | Professional insights, announcements | Professionals & networking |
| **Substack** | Newsletters, deep dives | Subscribers & newsletters |

Each platform has its own format and audience, so DocTester CLI automatically formats your content for each.

## Why Publish to Multiple Platforms?

- **Reach** — Different audiences hang out on different platforms
- **SEO** — Syndicated content reaches more search results
- **Backup** — Content exists in multiple places
- **Engagement** — Different platforms have different engagement patterns
- **Repurpose** — Documentation becomes blog posts, tutorials, and newsletters

## Getting Started: Set Up Platform Accounts

### Dev.to

1. Go to [dev.to/new](https://dev.to/new)
2. Sign up (free!)
3. Go to [Settings → Integrations](https://dev.to/settings/extensions)
4. Copy your API key

### Medium

1. Go to [medium.com](https://medium.com)
2. Sign up (free with email)
3. Go to [Settings → Security and apps](https://medium.com/me/settings/security)
4. Create an integration token

### Hashnode

1. Go to [hashnode.com](https://hashnode.com)
2. Sign up (free!)
3. Go to [Dashboard → Integrations](https://hashnode.com/integrations)
4. Generate API key

### LinkedIn

1. Create a LinkedIn developer app at [linkedin.com/developers](https://linkedin.com/developers)
2. Get your API credentials

### Substack

1. Go to [substack.com](https://substack.com)
2. Create a publication
3. Go to [Settings → API tokens](https://substack.com/settings/apps)

## Your First Blog Export

Let's publish a documentation file to Dev.to.

### Create Blog Post Content

Create a file called `api-tutorial.md`:

```markdown
---
title: "Getting Started with Our REST API"
description: "A comprehensive guide to authenticating and using our REST API"
tags: ["api", "tutorial", "rest"]
published: true
---

# Getting Started with Our REST API

## Overview

Our REST API makes it easy to integrate with our platform.

## Authentication

All requests require a Bearer token:

\`\`\`bash
curl -H "Authorization: Bearer YOUR_TOKEN" \\
  https://api.example.com/v1/users
\`\`\`

## Creating a User

\`\`\`bash
curl -X POST https://api.example.com/v1/users \\
  -H "Authorization: Bearer YOUR_TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "Alice",
    "email": "alice@example.com"
  }'
\`\`\`

## Response Format

All responses are JSON:

\`\`\`json
{
  "id": 1,
  "name": "Alice",
  "email": "alice@example.com",
  "created_at": "2026-03-11T00:00:00Z"
}
\`\`\`

## Next Steps

- Read the [full API reference](https://docs.example.com)
- Try the [API playground](https://playground.example.com)
```

### Export to Dev.to Format

```bash
# Export formatted for Dev.to
dtr export to-blog api-tutorial.md --platform devto -o devto-post.md
```

This creates `devto-post.md` with Dev.to's specific formatting (front matter, tags, etc.).

### Publish to Dev.to

```bash
# Set your API key
export DEVTO_API_KEY=your_api_key_here

# Publish (requires account setup)
dtr push blog devto-post.md --api-key $DEVTO_API_KEY
```

Your post is now live on Dev.to!

## Multi-Platform Publishing Workflow

Publish the same content to multiple platforms at once:

```bash
#!/bin/bash
# Publish to all platforms

ARTICLE="api-tutorial.md"

echo "1. Exporting to all platforms..."
dtr export to-blog "$ARTICLE" --platform devto -o article-devto.md
dtr export to-blog "$ARTICLE" --platform medium -o article-medium.md
dtr export to-blog "$ARTICLE" --platform hashnode -o article-hashnode.md

echo "2. Publishing to Dev.to..."
dtr push blog article-devto.md --platform devto --api-key $DEVTO_API_KEY

echo "3. Publishing to Medium..."
dtr push blog article-medium.md --platform medium --api-key $MEDIUM_API_KEY

echo "4. Publishing to Hashnode..."
dtr push blog article-hashnode.md --platform hashnode --api-key $HASHNODE_API_KEY

echo "✓ Published to all platforms!"
```

Save as `publish-all.sh`:

```bash
chmod +x publish-all.sh
./publish-all.sh
```

## Managing Multiple Articles

If you have many articles, organize them:

```
blog/
├── articles/
│   ├── 01-intro-to-api.md
│   ├── 02-authentication.md
│   ├── 03-best-practices.md
│   └── 04-troubleshooting.md
└── config.yml

# Export all to Dev.to format
dtr export to-blog blog/articles/ -o blog/devto/ --platform devto -r
```

## Front Matter & Metadata

Each platform supports different metadata. DocTester CLI handles this automatically.

### Dev.to Format

```markdown
---
title: "My Article"
description: "Short description"
tags: ["api", "tutorial"]
published: true
series: "API Series"
cover_image: "https://example.com/image.jpg"
---
```

### Medium Format

```markdown
---
title: "My Article"
subtitle: "Optional subtitle"
tags: ["api", "tutorial"]
canonical_url: "https://my-blog.com/article"
---
```

### Hashnode Format

```markdown
---
title: "My Article"
description: "Short description"
tags: ["api", "tutorial"]
cover: "https://example.com/image.jpg"
seriesId: "series-id-123"
---
```

DocTester CLI automatically converts your metadata to the correct format.

## Tips for Success

### 1. Cross-Link Your Articles

Reference your documentation site in published articles:

```markdown
For the complete API reference, see [full documentation](https://docs.example.com).
```

### 2. Use Consistent Metadata

Tagging helps readers discover related content:

```bash
# Tag all articles with similar topics
dtr export to-blog article.md --platform devto --tag api --tag tutorial
```

### 3. Schedule Publication

Some platforms allow scheduling. Check platform docs:

```bash
# Publish at specific time (platform-dependent)
dtr push blog article.md --platform devto --schedule "2026-03-15 09:00 UTC"
```

### 4. Track Cross-Posting

Set canonical URLs to avoid SEO penalties when syndicating:

```markdown
---
canonical_url: "https://my-blog.com/original-article"
---
```

## Automated Publishing with CI/CD

Use GitHub Actions to automatically publish whenever documentation changes:

```yaml
name: Publish to Blogging Platforms

on:
  push:
    paths:
      - 'docs/blog/**'
    branches: [main]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.12'

      - name: Install DocTester CLI
        run: pip install -e doctester-cli/

      - name: Export and publish to Dev.to
        run: |
          dtr export to-blog docs/blog/*.md --platform devto -o /tmp/devto/
          dtr push blog /tmp/devto/ --platform devto
        env:
          DEVTO_API_KEY: ${{ secrets.DEVTO_API_KEY }}

      - name: Export and publish to Medium
        run: |
          dtr export to-blog docs/blog/*.md --platform medium -o /tmp/medium/
          dtr push blog /tmp/medium/ --platform medium
        env:
          MEDIUM_API_KEY: ${{ secrets.MEDIUM_API_KEY }}
```

This automatically publishes to Dev.to and Medium whenever you push markdown files to `docs/blog/`.

## Common Tasks

### Export with Custom Tags

```bash
dtr export to-blog article.md --platform devto \
  --tag "python" --tag "api" --tag "tutorial"
```

### Batch Export Everything

```bash
# Export all markdown files in current directory
dtr export to-blog *.md --platform devto -o devto-posts/ -r
```

### Update Published Post

```bash
# Modify your article, then update
dtr push blog article.md --platform devto --update-id 12345
```

### Preview Before Publishing

```bash
# View what will be published without sending
dtr export to-blog article.md --platform devto --dry-run
```

## Troubleshooting

**"API key invalid"**
- Double-check the API key from your platform settings
- Ensure environment variable is set: `echo $DEVTO_API_KEY`
- Regenerate API key if in doubt

**"Title or description missing"**
- Add front matter to your markdown:
  ```markdown
  ---
  title: "My Article"
  description: "What it's about"
  ---
  ```

**"Publishing fails silently"**
- Add verbose flag: `dtr push blog article.md --verbose`
- Check platform API status pages
- Verify article doesn't violate platform guidelines

**"Article not appearing immediately"**
- Most platforms publish instantly, but Dev.to may require approval
- Check platform dashboard for moderation status
- Review platform community guidelines

## Platform-Specific Notes

### Dev.to
- Free account required
- Articles appear immediately (usually)
- Good for tech community
- Best for tutorials and how-to guides

### Medium
- Freemium model (articles behind paywall)
- Use canonical URL to avoid penalties
- Large audience
- Good for in-depth articles

### Hashnode
- Free blogging platform
- Growing developer audience
- Supports series organization
- Good for technical deep dives

### LinkedIn
- Professional network
- Share with your network
- Limited API access
- Good for announcements

### Substack
- Newsletter platform
- Subscriber-based
- Great for regular content
- Good for newsletters

## Next Steps

You've learned:
- ✅ Set up blogging platform accounts
- ✅ Export to multiple platforms
- ✅ Publish in bulk
- ✅ Automate with CI/CD
- ✅ Cross-post to reach more readers

Keep exploring:

- **[Publishing Guide](../how-to/publishing.md)** — Publish to GitHub Pages, S3, and beyond
- **[Directory Management](../how-to/directory-management.md)** — Organize and archive
- **[Format Conversion](02-export-formats.md)** — Convert between formats

---

**Publish everywhere!** 📢

Still have questions? Open an [issue on GitHub](https://github.com/seanchatmangpt/doctester/issues).
