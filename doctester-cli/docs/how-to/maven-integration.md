# How to Integrate DTR with Maven Builds

## Problem
You're running JUnit tests in a Maven project and want documentation automatically generated during the build. Manual exports are tedious — you want docs generated as part of `mvn clean test` or `mvn verify`.

## Solution Overview

DTR includes a Maven plugin that automatically generates documentation from tests. The plugin:
- Runs after test execution
- Captures test output and HTTP exchanges
- Generates Markdown, HTML, PDF, or slides
- Integrates with CI/CD pipelines

## Step 1: Add Plugin to pom.xml

Add the DTR Maven plugin to your project's `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.seanchatmangpt.dtr</groupId>
            <artifactId>dtr-maven-plugin</artifactId>
            <version>2.5.0</version>
            <configuration>
                <format>markdown</format>
                <outputDirectory>${project.basedir}/docs/generated</outputDirectory>
                <skip>false</skip>
            </configuration>
            <executions>
                <execution>
                    <phase>post-integration-test</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Step 2: Basic Configuration

Minimal setup (Markdown output to `target/docs`):

```xml
<plugin>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-maven-plugin</artifactId>
    <version>2.5.0</version>
</plugin>
```

Run your tests:
```bash
mvn clean test
# Docs auto-generated to: target/docs/
```

## Step 3: Advanced Configuration

### Multiple Output Formats
```xml
<plugin>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-maven-plugin</artifactId>
    <version>2.5.0</version>
    <configuration>
        <formats>
            <format>markdown</format>
            <format>html</format>
            <format>latex</format>
        </formats>
        <outputDirectory>${project.basedir}/docs</outputDirectory>
    </configuration>
</plugin>
```

### With Custom Templates
```xml
<configuration>
    <format>latex</format>
    <latexTemplate>acm-conference</latexTemplate>
    <htmlTemplate>bootstrap</htmlTemplate>
    <highlightTheme>atom-dark</highlightTheme>
    <outputDirectory>${project.basedir}/docs/generated</outputDirectory>
</configuration>
```

### Slides & Blog Export
```xml
<configuration>
    <formats>
        <format>markdown</format>
        <format>slides</format>
        <format>blog</format>
    </formats>
    <slidesTemplate>revealjs</slidesTemplate>
    <blogPlatforms>
        <platform>devto</platform>
        <platform>medium</platform>
    </blogPlatforms>
</configuration>
```

## Step 4: Use Maven Properties

Control documentation generation via CLI properties:

```bash
# Generate Markdown (default)
mvn clean test

# Generate PDF with ACM template
mvn clean test -Ddoctester.format=latex -Ddoctester.template=acm-conference

# Generate HTML with custom theme
mvn clean test -Ddoctester.format=html -Ddoctester.theme=bootstrap

# Generate multiple formats
mvn clean test -Ddoctester.formats=markdown,html,latex

# Skip documentation generation
mvn clean test -Ddoctester.skip=true

# Custom output directory
mvn clean test -Ddoctester.outputDir=src/docs
```

## Step 5: Maven Profiles for Different Outputs

Create profiles for different scenarios:

```xml
<profiles>
    <!-- Development: fast markdown only -->
    <profile>
        <id>dev-docs</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <build>
            <plugins>
                <plugin>
                    <groupId>io.github.seanchatmangpt.dtr</groupId>
                    <artifactId>dtr-maven-plugin</artifactId>
                    <configuration>
                        <format>markdown</format>
                        <skip>false</skip>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>

    <!-- Publication: all formats + blog -->
    <profile>
        <id>publish-docs</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>io.github.seanchatmangpt.dtr</groupId>
                    <artifactId>dtr-maven-plugin</artifactId>
                    <configuration>
                        <formats>
                            <format>markdown</format>
                            <format>html</format>
                            <format>latex</format>
                            <format>slides</format>
                            <format>blog</format>
                        </formats>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>

    <!-- Academic: PDF only with research template -->
    <profile>
        <id>academic-docs</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>io.github.seanchatmangpt.dtr</groupId>
                    <artifactId>dtr-maven-plugin</artifactId>
                    <configuration>
                        <format>latex</format>
                        <latexTemplate>arxiv</latexTemplate>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Usage:
```bash
mvn clean test -Pdev-docs        # Fast development docs
mvn clean test -Ppublish-docs    # Full publication suite
mvn clean test -Pacademic-docs   # PDF only (arXiv format)
```

## Step 6: Integrate with CI/CD

### GitHub Actions
```yaml
name: Build & Generate Docs

on: [push, pull_request]

jobs:
  test-and-docs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '25'
          distribution: 'openjdk'

      - name: Run tests with docs generation
        run: mvn clean verify -Ppublish-docs

      - name: Upload docs as artifact
        uses: actions/upload-artifact@v3
        with:
          name: generated-docs
          path: target/docs/

      - name: Deploy docs to GitHub Pages
        if: github.ref == 'refs/heads/main'
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./target/docs
```

### Jenkins
```groovy
pipeline {
    agent any
    stages {
        stage('Build & Test') {
            steps {
                sh 'mvn clean verify -Ppublish-docs'
            }
        }
        stage('Archive Docs') {
            steps {
                archiveArtifacts artifacts: 'target/docs/**/*',
                                 allowEmptyArchive: true
            }
        }
    }
    post {
        always {
            publishHTML([
                reportDir: 'target/docs',
                reportFiles: 'index.html',
                reportName: 'Generated Documentation'
            ])
        }
    }
}
```

## Step 7: Verify Output

After running tests, check the generated documentation:

```bash
mvn clean test

# View Markdown
cat target/docs/index.md

# View HTML (open in browser)
open target/docs/index.html

# View PDF (if generated)
open target/docs/index.pdf
```

## Complete Example pom.xml

```xml
<?xml version="1.0"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-api</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <!-- Testing -->
        <dependency>
            <groupId>io.github.seanchatmangpt.dtr</groupId>
            <artifactId>dtr-core</artifactId>
            <version>2.5.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <release>25</release>
                </configuration>
            </plugin>

            <plugin>
                <groupId>io.github.seanchatmangpt.dtr</groupId>
                <artifactId>dtr-maven-plugin</artifactId>
                <version>2.5.0</version>
                <configuration>
                    <formats>
                        <format>markdown</format>
                        <format>html</format>
                    </formats>
                    <outputDirectory>${project.basedir}/docs/generated</outputDirectory>
                </configuration>
                <executions>
                    <execution>
                        <phase>post-integration-test</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## Common Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `format` | String | `markdown` | Output format: markdown, html, latex, slides, blog |
| `outputDirectory` | Path | `target/docs` | Where to write generated docs |
| `latexTemplate` | String | — | Template: acm-conference, arxiv, ieee, nature, us-patent |
| `htmlTemplate` | String | `bootstrap` | HTML theme: bootstrap, tailwind, minimal, github |
| `highlightTheme` | String | `atom-dark` | Code syntax highlighting theme |
| `skip` | Boolean | `false` | Skip documentation generation |
| `failOnError` | Boolean | `true` | Fail build if doc generation fails |

## Troubleshooting

**"Plugin not found"** → Ensure DTR Maven plugin is published to Maven Central or local repository

**"OutOfMemoryError"** → Increase Maven heap: `export MAVEN_OPTS="-Xmx2g"`

**"pdflatex not found"** → Install TeX: `sudo apt install texlive-xetex` or download MacTeX

**"Skip not working"** → Use `-Ddoctester.skip=true` (Maven property takes precedence)

**"Docs not generated in CI"** → Verify build succeeds; check console output for errors

## Next Steps
- [Export in Different Formats](formats.md)
- [Publish to Blogging Platforms](blog-publishing.md)
- [Batch Export Multiple Files](batch-export.md)
