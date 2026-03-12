# How to Publish Documentation to Blogging Platforms

## Problem
Your documentation is great, but it's only visible on your internal docs site. You want to share it with the developer community by publishing to platforms like Dev.to, Medium, Hashnode, and LinkedIn. Managing multiple platform submissions is tedious and error-prone.

## Solution Overview

DTR can export documentation in formats compatible with major blogging platforms and manage a publishing queue. One Markdown file can be cross-posted to multiple platforms with a single command.

## Supported Platforms

| Platform | Use Case | Audience | Best For |
|----------|----------|----------|----------|
| **Dev.to** | Community blogs | Developers | Short tutorials, tips, news |
| **Medium** | Long-form publishing | General tech + niche | In-depth articles, stories |
| **Hashnode** | Developer community | Backend/DevOps | Technical deep dives |
| **LinkedIn** | Professional network | Industry leaders | Thought leadership, opinions |
| **Substack** | Newsletter platform | Subscribers | Series, ongoing columns |

## Step 1: Set Up Authentication

### Get API Keys

**Dev.to:**
1. Go to https://dev.to/settings/account
2. Scroll to "DEV Community API Keys"
3. Copy your API key

**Medium:**
1. Go to https://medium.com/me/settings
2. Scroll to "Integration tokens"
3. Create a new token

**Hashnode:**
1. Go to https://hashnode.com/settings/developer
2. Create a Personal Access Token

**LinkedIn:**
1. Go to https://www.linkedin.com/developers/apps
2. Create a new app
3. Get your Access Token

**Substack:**
1. Go to https://substack.com/settings/publication
2. Find API keys
3. Copy publication ID and API key

### Store API Keys
```bash
# Create config file
mkdir -p ~/.config/doctester/
cat > ~/.config/doctester/platforms.yaml << EOF
platforms:
  devto:
    api_key: "your_devto_api_key_here"
  medium:
    api_key: "your_medium_api_key_here"
  hashnode:
    api_key: "your_hashnode_api_key_here"
  linkedin:
    access_token: "your_linkedin_token_here"
  substack:
    api_key: "your_substack_api_key_here"
    publication_id: "your_publication_id"
EOF

chmod 600 ~/.config/doctester/platforms.yaml
```

Or use environment variables:
```bash
export DTR_DEVTO_API_KEY="your_devto_api_key"
export DTR_MEDIUM_API_KEY="your_medium_api_key"
export DTR_HASHNODE_API_KEY="your_hashnode_api_key"
export DTR_LINKEDIN_TOKEN="your_linkedin_token"
export DTR_SUBSTACK_API_KEY="your_substack_api_key"
```

## Step 2: Export for Single Platform

### Export for Dev.to
```bash
dtr export guide.md --format blog --platform devto

# Output: guide.devto.json
# Contains formatted content ready for Dev.to API
```

### Export for Medium
```bash
dtr export guide.md --format blog --platform medium

# Output: guide.medium.json
```

### Export for Hashnode
```bash
dtr export guide.md --format blog --platform hashnode

# Output: guide.hashnode.json
```

### Export for LinkedIn
```bash
dtr export guide.md --format blog --platform linkedin

# Output: guide.linkedin.json
```

### Export for Substack
```bash
dtr export guide.md --format blog --platform substack

# Output: guide.substack.json
```

## Step 3: Add Metadata to Your Document

Add a metadata block at the top of your Markdown file to customize platform-specific settings:

```markdown
---
title: "Getting Started with DocTester"
description: "Learn how to generate living documentation from JUnit tests"
tags: [java, testing, documentation, devops]
cover_image: "images/cover.png"
canonical_url: "https://example.com/docs/getting-started"
published: false
scheduled_for: "2026-03-15T10:00:00Z"
---

# Getting Started with DocTester

## Introduction

DTR is a comprehensive documentation generator...
```

Supported metadata:

| Field | Platforms | Notes |
|-------|-----------|-------|
| `title` | All | Article headline (50-100 chars) |
| `description` | All | SEO description (150-160 chars) |
| `tags` | All | 1-5 relevant tags |
| `cover_image` | Medium, Hashnode, LinkedIn | URL to cover photo |
| `canonical_url` | Dev.to, Medium | Original article URL (if published elsewhere) |
| `published` | All | `true`/`false` — publish immediately or draft |
| `scheduled_for` | All | ISO 8601 timestamp for scheduled posting |
| `devto_series` | Dev.to | Connect to existing series |
| `medium_publication` | Medium | Target specific Medium publication |
| `hashnode_tags` | Hashnode | Hashnode-specific tag IDs |
| `linkedin_document_type` | LinkedIn | `article`, `video`, `poll` |

## Step 4: Publish to Platform

### Publish Immediately
```bash
# Publish to Dev.to (creates draft by default)
dtr publish guide.md --platform devto

# Publish live immediately
dtr publish guide.md --platform devto --live

# Verify publication
dtr publish guide.md --platform devto --dry-run  # Preview before publishing
```

### Schedule for Later
```bash
# Schedule publication for specific date
dtr publish guide.md --platform devto --schedule "2026-03-15T10:00:00Z"

# Platform will auto-publish at scheduled time
```

### Update Existing Post
```bash
# Update a previously published post (requires post ID)
dtr publish guide.md --platform devto --post-id abc123 --update

# Post ID is returned from initial publication
```

## Step 5: Multi-Platform Publishing

### Cross-Post to Multiple Platforms
```bash
# Publish to Dev.to, Medium, and Hashnode simultaneously
dtr publish guide.md --platform devto,medium,hashnode --live

# Creates a post on each platform with consistent formatting
```

### Use Publishing Queue

Instead of publishing immediately, queue posts for later review:

```bash
# Export to publishing queue (no upload yet)
dtr export guide.md --format blog --platform devto,medium --queue

# Output: queue.json with all pending posts

# Review queue
dtr queue list

# Output:
# 1. [DRAFT] "Getting Started with DocTester" → Dev.to
# 2. [DRAFT] "Getting Started with DocTester" → Medium
```

### Review and Publish from Queue
```bash
# Publish all queued posts
dtr queue publish --all

# Publish specific posts
dtr queue publish --id 1,2

# Update post before publishing
dtr queue edit --id 1 --title "New Title"

# Remove post from queue
dtr queue remove --id 1
```

## Advanced Publishing Workflows

### Scenario 1: Schedule a Content Series
```bash
#!/bin/bash
# Publish tutorial series over 4 weeks

dtr publish week1-intro.md --platform devto \
  --schedule "2026-03-15T10:00:00Z" \
  --devto-series "docker-tutorial"

dtr publish week2-networking.md --platform devto \
  --schedule "2026-03-22T10:00:00Z" \
  --devto-series "docker-tutorial"

dtr publish week3-volumes.md --platform devto \
  --schedule "2026-03-29T10:00:00Z" \
  --devto-series "docker-tutorial"

dtr publish week4-advanced.md --platform devto \
  --schedule "2026-04-05T10:00:00Z" \
  --devto-series "docker-tutorial"

echo "Tutorial series scheduled!"
```

### Scenario 2: Cross-Post with Platform-Specific Customization
```bash
# Create platform-specific versions
cat > tutorial-base.md << EOF
---
title: "API Testing Best Practices"
tags: [testing, api, devops]
---

# API Testing Best Practices
...
EOF

# Dev.to version (open, community-focused)
cat > tutorial-devto.md << EOF
---
title: "API Testing Best Practices"
description: "Learn proven patterns for testing REST APIs"
tags: [testing, api, devops, beginners]
---

# API Testing Best Practices

(Community version with more explanation)
...
EOF

# Medium version (premium, storytelling)
cat > tutorial-medium.md << EOF
---
title: "API Testing Best Practices: A Developer's Journey"
description: "How we improved our API testing"
tags: [testing, api, devops, engineering]
cover_image: "images/medium-cover.png"
---

# API Testing Best Practices: A Developer's Journey

(More narrative, personal angle)
...
EOF

# Publish both
dtr publish tutorial-devto.md --platform devto --live
dtr publish tutorial-medium.md --platform medium --live
```

### Scenario 3: Monitor Cross-Posted Content

```bash
# Track post performance across platforms
dtr stats list

# Output:
# Platform | Title | Views | Likes | Comments | Posted
# ---------|-------|-------|-------|----------|--------
# Dev.to   | Getting Started... | 1200 | 45 | 12 | 2026-03-11
# Medium   | Getting Started... | 800 | 23 | 8 | 2026-03-11
# Hashnode | Getting Started... | 450 | 18 | 5 | 2026-03-11

# Export analytics to CSV
dtr stats export --format csv --output analytics.csv
```

## Platform-Specific Tips

### Dev.to
- Max title: 128 characters
- Supports `{% ... %}` liquid tags for embeds
- Series can include up to 10 articles
- Best time to post: Tue-Thu 9-10am EST

### Medium
- Min 2000 characters recommended
- Paywall requires Medium Partner Program membership
- Can request premium exclusive access
- Best time to post: Morning hours (5-9am EST)

### Hashnode
- Supports Hashnode-specific markdown (footnotes, syntax highlighting)
- Can link to personal Hashnode blog
- Custom domain support for premium
- Best time to post: Weekday evenings (5-9pm EST)

### LinkedIn
- Character limit: 3000 characters (use threading for longer)
- Emojis increase engagement
- Best time to post: Tuesday-Thursday, 8-10am local time
- Use hashtags strategically (#5-10)

### Substack
- Newsletter format (email + web)
- Subscription model supported
- Supports table of contents and footnotes
- Best time to send: Tuesday-Wednesday mornings

## Troubleshooting

**"Authentication failed"** → Verify API key is correct and has not expired. Check `~/.config/doctester/platforms.yaml` permissions.

**"Image not found"** → Use absolute URLs (https://...) not relative paths. Files must be accessible on the internet.

**"Post rejected for policy violation"** → Review platform guidelines. Medium and Dev.to have content policies.

**"Character limit exceeded"** → Some platforms have limits; use `dtr preview --platform medium` to check before publishing.

**"Post not appearing"** → Check if post is in draft mode. Use `--live` flag to publish immediately.

**"Scheduled post didn't publish"** → Verify platform's timezone matches your scheduling time. Some platforms have scheduling windows.

## Next Steps
- [Export in Different Formats](formats.md)
- [Batch Export Multiple Files](batch-export.md)
- [Integrate with Maven](maven-integration.md)
