# DTR-019: VS Code Extension for DTR

**Priority**: P2
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,ide,vscode,2026.4.0

## Description
Develop a comprehensive VS Code extension that provides deep integration with DTR documentation testing framework. The extension should enable developers to write, preview, and execute documentation tests directly within VS Code with real-time feedback and navigation capabilities.

## Key Features

### 1. Live Preview Panel
- Split-pane preview showing rendered documentation alongside source code
- Real-time markdown rendering with DTR-specific syntax highlighting
- Auto-refresh on file save with configurable debounce
- Support for all DTR output formats (Markdown, LaTeX, Blog, Slides)

### 2. Inline Gutter Preview
- Hover tooltips showing rendered documentation for specific code blocks
- Gutter icons indicating which methods have documentation tests
- Quick preview of say* method output without leaving editor
- Inline error display for documentation test failures

### 3. Test Execution Integration
- Run individual documentation tests from Code Lens
- Run all tests in file/folder/workspace via command palette
- Test results in Problems panel with clickable navigation
- Debug support for documentation test execution

### 4. Navigation Support
- Go to Definition from say* method calls to test implementations
- Find References for documented methods across codebase
- Symbol search for DtrContext methods and test classes
- Outline view showing documentation test structure

## Acceptance Criteria

### Core Functionality
- [ ] Extension installs successfully from VS Code Marketplace
- [ ] Live Preview panel renders DTR documentation with accurate formatting
- [ ] Preview updates within 500ms of file save
- [ ] Gutter icons appear for all methods with @DocTest annotations

### Test Execution
- [ ] Code Lens appears above all @DocTest annotated methods
- [ ] "Run Test" command executes individual tests and shows results
- [ ] "Run All Tests" executes workspace tests and aggregates results
- [ ] Test failures display with stack traces in Problems panel

### Navigation
- [ ] F12 (Go to Definition) navigates from say* calls to implementations
- [ ] Shift+F12 (Find References) finds all documentation test references
- [ ] Outline view shows test methods grouped by class
- [ ] Breadcrumbs navigation works within test files

### User Experience
- [ ] Extension activates only when Java/DocTest files are opened
- [ ] Settings page exposes all configuration options
- [ ] Keyboard shortcuts provided for common operations
- [ ] Extension works with VS Code Remote (SSH, Containers, WSL)

## Technical Notes

### Extension Structure
```
vscode-dtr/
├── src/
│   ├── extension.ts          # Main entry point
│   ├── preview/
│   │   ├── previewProvider.ts # Markdown preview provider
│   │   └── previewManager.ts  # Preview panel management
│   ├── decoration/
│   │   ├── gutterIcon.ts      # Gutter icon decoration
│   │   └── inlinePreview.ts   # Inline hover preview
│   ├── testing/
│   │   ├── testRunner.ts      # Test execution logic
│   │   └── resultsParser.ts   # Parse JUnit/TestNG output
│   ├── navigation/
│   │   ├── definitionProvider.ts
│   │   └── referencesProvider.ts
│   └── language/
│       └── dtrSyntax.ts       # DTR-specific syntax highlighting
├── package.json               # Extension manifest
├── tsconfig.json
└── README.md
```

### Key Dependencies
- `vscode-extension-telemetry`: Usage analytics
- `vscode-languageclient`: For Java Language Server integration
- `java`: VS Code Java extension (required dependency)
- `markdown-it`: Markdown rendering with custom DTR rules

### VS Code API Usage
- `vscode.TextDocumentContentProvider`: For preview panel
- `vscode.CodeLensProvider`: For test execution Code Lens
- `vscode.DocumentSymbolProvider`: For outline view
- `vscode.DefinitionProvider`: For Go to Definition
- `vscode.ReferenceProvider`: For Find References

### Configuration Settings
```json
{
  "dtr.preview.autoRefresh": true,
  "dtr.preview.refreshDelay": 500,
  "dtr.testing.framework": "junit",
  "dtr.gutterIcons.enabled": true,
  "dtr.inlinePreview.enabled": true,
  "dtr.java.home": null
}
```

## Dependencies

### Required
- **DTR-021** (Debugging Configurations): Extension needs .vscode/launch.json for test debugging
- **DTR-022** (Code Snippets): Extension should consume and suggest DTR code snippets

### Recommended
- VS Code Java Extension Pack (redhat.java)
- Java Test Runner (vscjava.vscode-java-test)
- Maven for Java (vscjava.vscode-maven)

## References

### Internal
- `/Users/sac/dtr/docs/reference/say-api-methods.md` - say* API reference for syntax highlighting
- `/Users/sac/dtr/docs/tutorials/` - Tutorial examples for preview testing
- `/Users/sac/dtr/CLAUDE.md` - Project guidelines

### External
- [VS Code Extension API](https://code.visualstudio.com/api)
- [VS Code Java Extension Guide](https://github.com/redhat-developer/vscode-java)
- [Markdown-it Documentation](https://github.com/markdown-it/markdown-it)

## Success Metrics

### Adoption
- Extension published to VS Code Marketplace
- 100+ installations within first month
- Average rating ≥ 4.0/5.0

### Quality
- 90%+ test coverage for extension code
- Zero critical bugs in first release
- Response time < 100ms for preview updates

### Developer Experience
- Setup time < 5 minutes from install to first preview
- Zero configuration required for basic usage
- Comprehensive documentation with examples
