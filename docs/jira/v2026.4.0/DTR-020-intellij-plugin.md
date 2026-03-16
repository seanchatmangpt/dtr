# DTR-020: IntelliJ Plugin for DTR

**Priority**: P2
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,ide,intellij,2026.4.0

## Description
Develop a comprehensive IntelliJ IDEA plugin that provides deep integration with DTR documentation testing framework. The plugin should enable developers to write, preview, and execute documentation tests directly within IntelliJ with real-time feedback and navigation capabilities.

## Key Features

### 1. Tool Window for Preview
- Dedicated tool window showing rendered documentation
- Split layout with source code and preview side-by-side
- Real-time markdown rendering with DTR-specific syntax
- Tabbed interface for multiple documentation files
- Export preview to HTML/PDF

### 2. Gutter Icons for Preview
- Gutter icons on methods with @DocTest annotations
- Click icon to open documentation in preview tool window
- Hover shows quick preview of rendered output
- Color-coded icons indicating test status (pass/fail/unknown)
- Batch action to preview all documentation tests in file

### 3. Run Configuration Integration
- Custom run configuration type for documentation tests
- "Run DTR Test" action in context menu
- "Debug DTR Test" with breakpoint support
- Run all tests in package/module/project
- Test results displayed in standard Test Runner UI

### 4. Navigation Support
- Go to Declaration from say* method calls
- Find Usages for documented methods
- Structure view showing documentation test hierarchy
- Navigate to test from implementation and vice versa
- Quick documentation popup for DtrContext methods

## Acceptance Criteria

### Core Functionality
- [ ] Plugin installs successfully from JetBrains Marketplace
- [ ] Tool window appears in View → Tool Windows → "DTR Preview"
- [ ] Preview renders DTR documentation with accurate formatting
- [ ] Preview updates on file save and with "Refresh Preview" action

### Gutter Integration
- [ ] Gutter icons appear for all @DocTest annotated methods
- [ ] Clicking icon opens preview tool window and navigates to section
- [ ] Hover shows 3-line preview of rendered documentation
- [ ] Icons update color based on last test run result

### Test Execution
- [ ] "Run DTR Test" context menu appears on test methods
- [ ] Run configuration type appears in "Edit Configurations" dialog
- [ ] Test results appear in standard Test Runner UI
- [ ] Debug mode supports breakpoints in test methods

### Navigation
- [ ] Ctrl+B / Cmd+B navigates from say* calls to implementations
- [ ] Alt+F7 / Opt+F7 finds all documentation test references
- [ ] Structure view shows test methods grouped by class
- [ ] Navigate → Test/Implementation toggles between code and test

### User Experience
- [ ] Plugin activates only for Java projects with DTR dependency
- [ ] Settings page exposes all configuration options
- [ ] Keyboard shortcuts provided for common operations
- [ ] Plugin works with IntelliJ IDEA Community and Ultimate

## Technical Notes

### Plugin Structure
```
intellij-dtr/
├── src/
│   └── main/
│       ├── java/
│       │   └── io/github/seanchatmangpt/dtr/intellij/
│       │       ├── DtrPlugin.java           # Main plugin class
│       │       ├── preview/
│       │       │   ├── DtrPreviewToolWindow.java
│       │       │   ├── PreviewPanel.java
│       │       │   └── RenderEngine.java
│       │       ├── gutter/
│       │       │   ├── DtrGutterIconRenderer.java
│       │       │   └── TestStatusIndicator.java
│       │       ├── run/
│       │       │   ├── DtrRunConfiguration.java
│       │       │   ├── DtrRunConfigurationType.java
│       │       │   └── DtrRunProfileState.java
│       │       ├── navigation/
│       │       │   ├── DtrGotoDeclarationHandler.java
│       │       │   └── DtrFindUsagesProvider.java
│       │       └── inspection/
│       │           └── DtrDocTestInspection.java
│       └── resources/
│           ├── META-INF/
│           │   └── plugin.xml              # Plugin manifest
│           └── icons/
│               ├── gutter_icon.png
│               ├── gutter_icon_pass.png
│               └── gutter_icon_fail.png
├── build.gradle.xml
└── CHANGELOG.md
```

### Key Dependencies
```gradle
dependencies {
    compileOnly 'com.intellij.intellij.platform:core:'
    implementation 'com.intellij.plugins:markdown:'
    implementation 'org.commonmark:commonmark:0.21.0'
    implementation 'org.commonmark:ext-gfm-tables:0.21.0'
    testImplementation 'com.intellij.intellij.platform:core:'
}
```

### IntelliJ Platform APIs
- `com.intellij.openapi.wm.ToolWindowFactory`: For preview tool window
- `com.intellij.codeInsight.daemon.LineMarkerProvider`: For gutter icons
- `com.intellij.execution.configurations.RunConfiguration`: For test runner
- `com.intellij.codeInsight.navigation.GotoDeclarationHandler`: For navigation
- `com.intellij.find.findUsages.FindUsagesProvider`: For find references

### plugin.xml Extension Points
```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- Tool Window -->
    <toolWindow id="DTR Preview" factoryClass="...DtrPreviewToolWindowFactory"/>

    <!-- Gutter Icons -->
    <codeInsight.lineMarkerProvider language="JAVA"
        implementationClass="...DtrLineMarkerProvider"/>

    <!-- Run Configuration -->
    <configurationType implementation="...DtrRunConfigurationType"/>

    <!-- Navigation -->
    <gotoDeclarationHandler implementation="...DtrGotoDeclarationHandler"/>
    <findUsagesProvider implementation="...DtrFindUsagesProvider"/>

    <!-- Structure View -->
    <lang.psi.structureViewFactory language="JAVA"
        implementationClass="...DtrStructureViewFactory"/>
</extensions>
```

### Configuration Settings
```xml
<applicationConfigurable groupId="tools" displayName="DTR"
    instance="DtrConfigurable"/>
```

## Dependencies

### Required
- **DTR-021** (Debugging Configurations): Plugin needs .idea/runConfigurations for test debugging
- **DTR-022** (Code Snippets): Plugin should consume and suggest DTR live templates

### Recommended
- IntelliJ IDEA Community Edition 2023.2+ or Ultimate 2023.2+
- Java 17+ SDK configured in IntelliJ
- Maven or Gradle integration for project setup

## References

### Internal
- `/Users/sac/dtr/docs/reference/say-api-methods.md` - say* API reference for syntax highlighting
- `/Users/sac/dtr/docs/tutorials/` - Tutorial examples for preview testing
- `/Users/sac/dtr/CLAUDE.md` - Project guidelines

### External
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform DevKit](https://plugins.jetbrains.com/docs/intellij/using-gradle.html)
- [Code Samples](https://github.com/JetBrains/intellij-sdk-code-samples)

## Success Metrics

### Adoption
- Plugin published to JetBrains Marketplace
- 100+ installations within first month
- Average rating ≥ 4.0/5.0

### Quality
- 80%+ test coverage for plugin code
- Zero critical bugs in first release
- Compatibility with IntelliJ IDEA 2023.2+

### Developer Experience
- Setup time < 5 minutes from install to first preview
- Zero configuration required for basic usage
- Comprehensive documentation with examples
- Performance: Preview renders < 200ms for typical files
