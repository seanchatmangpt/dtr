# DTR VS Code Extension

## Overview

The DTR Preview extension for Visual Studio Code provides live preview capabilities for DTR documentation tests, enabling real-time rendering and seamless integration with your test-driven documentation workflow.

## Features

### Live Preview Panel
- **Real-time preview** of DTR documentation as you write tests
- **Auto-refresh** when tests execute via Maven/Gradle
- **Syntax highlighting** for markdown, code blocks, and DTR-specific elements
- **Table of contents** navigation for long documentation

### Inline Gutter Preview
- **Inline annotations** showing preview snippets in the editor gutter
- **Quick status indicators** (✓ documentation generated, ⚠ preview pending)
- **Click to navigate** to full preview panel

### Test Integration
- **Automatic detection** of DTR test files (classes extending DtrTest or using @LivePreview)
- **Run tests** with integrated terminal (mvnd test, gradle test)
- **Watch mode** for continuous preview updates during development

### Navigation
- **Jump to test** from preview content (Ctrl+Click / Cmd+Click)
- **Jump to preview** from test methods (Alt+D)
- **Breadcrumb navigation** for test class → test method → documentation section

## Installation

### From Marketplace (Recommended)
1. Open VS Code
2. Press `Ctrl+Shift+X` (Cmd+Shift+X on Mac) to open Extensions
3. Search for "DTR Preview"
4. Click "Install"

### From VSIX
```bash
code --install-extension dtr-preview-2026.2.0.vsix
```

### Manual Installation
1. Download the latest release from [GitHub Releases](https://github.com/seanchatmangpt/dtr/releases)
2. Open VS Code
3. Go to Extensions → ... (three dots) → Install from VSIX
4. Select the downloaded `.vsix` file

## Configuration

### Settings

Configure the extension via VS Code settings (`settings.json`):

```json
{
  // General Preview Settings
  "dtr.preview.enabled": true,
  "dtr.preview.refreshRateMs": 500,
  "dtr.preview.autoOpen": false,

  // Inline Gutter Settings
  "dtr.preview.inlineGutter.enabled": true,
  "dtr.preview.inlineGutter.maxLines": 5,

  // Test Integration
  "dtr.test.autoRunOnSave": false,
  "dtr.test.command": "mvnd test",
  "dtr.test.watchMode": false,

  // Styling
  "dtr.preview.customCss": "",
  "dtr.preview.fontSize": 14,
  "dtr.preview.lineHeight": 1.6,

  // Panel Behavior
  "dtr.preview.position": "right",
  "dtr.preview.showInSidebar": false,
  "dtr.preview.openOnTestFailure": true
}
```

### Workspace-Specific Configuration

Create or edit `.vscode/settings.json` in your project:

```json
{
  "dtr.test.command": "mvnd verify -Dtest=UserProfileTest",
  "dtr.preview.refreshRateMs": 300,
  "dtr.preview.position": "bottom"
}
```

## Usage

### Quick Start

1. **Open a DTR test file** (e.g., `UserProfileTest.java`)

2. **Open preview panel**
   - Press `Ctrl+Shift+P` (Cmd+Shift+P on Mac)
   - Type "DTR: Open Preview"
   - Press Enter

3. **Run tests**
   - Press `Ctrl+Shift+P`
   - Type "DTR: Run Tests"
   - Preview updates automatically

### Keyboard Shortcuts

| Command | Windows/Linux | Mac | Description |
|---------|---------------|-----|-------------|
| Open Preview | `Ctrl+Shift+D` | `Cmd+Shift+D` | Open preview panel for current test |
| Toggle Inline Gutter | `Ctrl+Alt+G` | `Cmd+Option+G` | Show/hide inline preview |
| Run Tests | `Ctrl+Alt+R` | `Cmd+Option+R` | Execute DTR tests |
| Refresh Preview | `Ctrl+Alt+L` | `Cmd+Option+L` | Force refresh preview |
| Jump to Test | `Ctrl+Click` | `Cmd+Click` | Navigate from preview to test |
| Close Preview | `Ctrl+Shift+W` | `Cmd+Shift+W` | Close preview panel |

### Command Palette Commands

- `DTR: Open Preview` - Open preview panel for current test file
- `DTR: Close Preview` - Close preview panel
- `DTR: Run Tests` - Execute DTR tests for current file
- `DTR: Run All Tests` - Execute all DTR tests in workspace
- `DTR: Toggle Inline Gutter` - Enable/disable inline preview
- `DTR: Refresh Preview` - Force refresh preview content
- `DTR: Clear Preview Cache` - Clear cached preview data
- `DTR: Export Preview as HTML` - Export preview to standalone HTML
- `DTR: Export Preview as PDF` - Export preview to PDF

### Context Menu Commands

Right-click in editor for:
- "Open DTR Preview"
- "Run DTR Tests"
- "Generate Preview File"
- "Copy Preview Link"

## Live Preview Annotation

The extension recognizes the `@LivePreview` annotation:

```java
import io.github.seanchatmangpt.dtr.ide.LivePreview;

@LivePreview(refreshRateMs = 300, inlineGutter = true)
public class UserProfileTest extends DtrTest {

    @Test
    public void demonstrateUserProfile(DtrContext ctx) {
        ctx.sayNextSection("User Profile");
        ctx.say("This appears in live preview");
    }
}
```

### Annotation Attributes

- `refreshRateMs` - Polling frequency (default: 500ms)
- `inlineGutter` - Show inline preview in gutter (default: true)
- `autoOpen` - Auto-open preview when tests run (default: false)
- `customCss` - Custom styling (IDE-specific)
- `includeMetadata` - Include test execution metadata (default: true)

## Preview File Format

The extension generates `.dtr.preview` files alongside test output:

```json
{
  "version": "1.0",
  "testClass": "com.example.UserProfileTest",
  "testMethod": "demonstrateUserProfile",
  "timestamp": "2026-03-15T10:30:00Z",
  "content": "# User Profile\n\nThis appears in live preview",
  "metadata": {
    "executionTimeMs": 45,
    "status": "SUCCESS",
    "refreshRateMs": 300,
    "inlineGutter": true
  }
}
```

## Advanced Features

### Watch Mode

Enable continuous preview updates during development:

```json
{
  "dtr.test.watchMode": true,
  "dtr.test.autoRunOnSave": true
}
```

### Custom CSS Styling

Apply custom styles to preview:

```json
{
  "dtr.preview.customCss": ".dtr-header { color: #0066cc; }"
}
```

Or in `settings.json`:

```json
{
  "dtr.preview.customCss": "
    .dtr-header { font-weight: bold; color: #333; }
    .dtr-code-block { background: #f5f5f5; }
    .dtr-table { border-collapse: collapse; }
  "
}
```

### Export Options

Export preview to various formats:

- **HTML**: Standalone HTML file with embedded styles
- **PDF**: Formatted PDF via Chrome/Puppeteer
- **Markdown**: Raw markdown source
- **JSON**: Structured JSON representation

Use command palette: `DTR: Export Preview as...`

### Multi-Root Workspace Support

The extension supports multi-root workspaces:

- Each root can have independent DTR configuration
- Preview panel shows current active root's documentation
- Test execution respects per-root test commands

## Troubleshooting

### Preview Not Updating

1. **Check test execution**: Ensure tests are running successfully
   ```bash
   mvnd test
   ```

2. **Verify preview file generation**:
   ```bash
   ls -la target/test-results/dtr/
   ```

3. **Increase refresh rate**:
   ```json
   { "dtr.preview.refreshRateMs": 1000 }
   ```

4. **Clear preview cache**:
   - Command Palette: `DTR: Clear Preview Cache`

### Inline Gutter Not Showing

1. **Check annotation**:
   ```java
   @LivePreview(inlineGutter = true)  // Ensure this is set
   ```

2. **Verify setting**:
   ```json
   { "dtr.preview.inlineGutter.enabled": true }
   ```

3. **Check file size**: Inline gutter is disabled for files >500 lines

### Test Execution Fails

1. **Verify test command**:
   ```json
   { "dtr.test.command": "mvnd test" }
   ```

2. **Check terminal output**: View "DTR Tests" output channel

3. **Ensure Java extension is installed**: Required for Java test support

### Preview Shows Outdated Content

1. **Force refresh**: `Ctrl+Alt+L` (Cmd+Option+L on Mac)

2. **Re-run tests**: `DTR: Run Tests`

3. **Clear cache**: `DTR: Clear Preview Cache`

## Contributing

The extension is open source. Contribute at:

**GitHub**: https://github.com/seanchatmangpt/dtr-vscode

### Development Setup

```bash
# Clone repository
git clone https://github.com/seanchatmangpt/dtr-vscode.git
cd dtr-vscode

# Install dependencies
npm install

# Run in development mode
npm run watch

# Package extension
npm run package
```

## Changelog

### 2026.2.0 (Current)
- Initial release of DTR Preview extension
- Live preview panel with auto-refresh
- Inline gutter preview support
- Test integration with Maven/Gradle
- Navigation shortcuts and commands
- Export to HTML/PDF

## Support

- **Issues**: https://github.com/seanchatmangpt/dtr/issues
- **Discussions**: https://github.com/seanchatmangpt/dtr/discussions
- **Documentation**: https://dtr.dev/docs

## License

MIT License - See [LICENSE](https://github.com/seanchatmangpt/dtr/blob/main/LICENSE) for details.
