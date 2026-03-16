# DTR-021: Debugging Configurations for IDE Integration

**Priority**: P3
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,ide,debugging,vscode,intellij,2026.4.0

## Description
Create debugging configuration files for VS Code and IntelliJ IDEA that enable developers to debug DTR documentation tests with full breakpoint support, variable inspection, and step-through debugging capabilities. These configurations will be consumed by the IDE extensions/plugins created in DTR-019 and DTR-020.

## Scope

### VS Code Configurations
- `.vscode/launch.json` - Launch configurations for DTR test debugging
- `.vscode/tasks.json` - Build tasks for compiling tests before debugging
- Configuration for Maven/Gradle test execution
- Support for debugging individual tests and test suites

### IntelliJ Configurations
- `.idea/runConfigurations/` - XML run configuration files for DTR tests
- Templates for JUnit and TestNG debugging
- Support for method-level, class-level, and package-level debugging
- Integration with Maven/Gradle build systems

## Acceptance Criteria

### VS Code Configurations
- [ ] `.vscode/launch.json` provides "Debug DTR Test" configuration
- [ ] "Debug Current Test" launches debugger for test at cursor
- [ ] "Debug All Tests in File" runs all tests with debugging enabled
- [ ] "Debug Failed Tests" re-runs only failed tests from last run
- [ ] Breakpoints work in test methods and production code
- [ ] Variable inspection shows DtrContext state
- [ ] Configuration supports both JUnit and TestNG

### IntelliJ Configurations
- [ ] `.idea/runConfigurations/` contains DTR debug configuration templates
- [ ] "Debug DTR Test" context menu appears on test methods
- [ ] Configuration uses proper working directory and classpath
- [ ] JVM arguments include proper DTR agent options
- [ ] Breakpoints work in test methods and production code
- [ ] Variable inspection shows DtrContext state
- [ ] Configuration survives Git operations (in .gitignore)

### Build Integration
- [ ] VS Code tasks.json builds tests before debugging
- [ ] IntelliJ configuration triggers compilation automatically
- [ ] Hot reload works when available (Java 21+)
- [ ] Error messages clearly indicate missing dependencies

### Documentation
- [ ] README explains how to use debugging configurations
- [ ] Troubleshooting guide covers common issues
- [ ] Examples show debugging workflow with screenshots
- [ ] Configuration files are well-commented

## Technical Notes

### VS Code Configuration Structure

#### .vscode/launch.json
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug Current DTR Test",
      "request": "launch",
      "mainClass": "io.github.seanchatmangpt.dtr.junit.DtrTestLauncher",
      "args": "${file}",
      "vmArgs": "-enablepreview -ea",
      "classPaths": [
        "${workspaceFolder}/target/classes",
        "${workspaceFolder}/target/test-classes"
      ],
      "projectName": "dtr-core",
      "preLaunchTask": "build-tests"
    },
    {
      "type": "java",
      "name": "Debug All DTR Tests",
      "request": "launch",
      "mainClass": "org.junit.platform.console.ConsoleLauncher",
      "args": [
        "execute",
        "--select-class",
        "${fileBasenameNoExtension}",
        "--include-classname",
        ".*DocTest"
      ],
      "vmArgs": "-enablepreview -ea",
      "classPaths": [
        "${workspaceFolder}/target/classes",
        "${workspaceFolder}/target/test-classes"
      ],
      "projectName": "dtr-core"
    },
    {
      "type": "java",
      "name": "Attach to DTR Test Process",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005
    }
  ]
}
```

#### .vscode/tasks.json
```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "build-tests",
      "type": "shell",
      "command": "mvnd",
      "args": ["test-compile"],
      "group": "build",
      "problemMatcher": ["$javac"]
    },
    {
      "label": "run-all-tests",
      "type": "shell",
      "command": "mvnd",
      "args": ["test"],
      "group": "test",
      "problemMatcher": ["$junit"]
    }
  ]
}
```

### IntelliJ Configuration Structure

#### .idea/runConfigurations/DTR_Test__Current_.xml
```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="DTR Test (Current)" type="JUnit" factoryName="JUnit">
    <option name="MAIN_CLASS_NAME" value="io.github.seanchatmangpt.dtr.junit.DtrTestLauncher" />
    <option name="VM_PARAMETERS" value="-enablepreview -ea" />
    <option name="PARAMETERS" value="$FilePath$" />
    <option name="WORKING_DIRECTORY" value="$MODULE_WORKING_DIR$" />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
```

#### .idea/runConfigurations/DTR_Test__All_.xml
```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="DTR Test (All)" type="JUnit" factoryName="JUnit">
    <option name="TEST_OBJECT" value="package" />
    <option name="VM_PARAMETERS" value="-enablepreview -ea" />
    <option name="PARAMETERS" value="" />
    <option name="WORKING_DIRECTORY" value="$MODULE_WORKING_DIR />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
```

### Git Ignore Configuration

#### .gitignore additions
```
# IDE-specific run configurations (templates are in repo, user configs are ignored)
.idea/runConfigurations/*.xml
!.idea/runConfigurations/DTR_Test__Current_.xml
!.idea/runConfigurations/DTR_Test__All_.xml
```

### Debugging Features

#### Breakpoint Support
- Line breakpoints in test methods
- Conditional breakpoints based on DTR context
- Exception breakpoints for DtrException
- Field watchpoints on DtrContext state

#### Variable Inspection
- View rendered output during test execution
- Inspect DtrContext output buffer
- Examine test metadata and annotations
- Watch expressions for say* method results

#### Step-through Debugging
- Step into say* method implementations
- Step over rendering logic
- Step out of complex test scenarios
- Drop to frame for re-execution

## Dependencies

### Required
- **DTR-019** (VS Code Extension): Consumes .vscode/launch.json
- **DTR-020** (IntelliJ Plugin): Consumes .idea/runConfigurations/
- **DTR-022** (Code Snippets): Debug configurations should work with code snippets

### System Requirements
- VS Code 1.80+ with Java Extension Pack
- IntelliJ IDEA 2023.2+ (Community or Ultimate)
- Java 17+ for IDE, Java 26.ea+ for test execution
- Maven 4.0+ or Gradle 8.0+

## References

### Internal
- `/Users/sac/dtr/docs/tutorials/` - Tutorial examples for testing debug configs
- `/Users/sac/dtr/.vscode/` - Current VS Code settings
- `/Users/sac/dtr/.idea/` - Current IntelliJ settings

### External
- [VS Code Java Debugging](https://code.visualstudio.com/docs/java/debugging)
- [IntelliJ Debug Configuration](https://www.jetbrains.com/help/idea/run-debug-configuration.html)
- [JUnit 5 Debugging](https://junit.org/junit5/docs/current/user-guide/#running-tests-debug)

## Success Metrics

### Functionality
- Debug configurations work on first attempt for 90%+ of users
- Breakpoints hit consistently in all test scenarios
- Variable inspection shows accurate DTR state
- No configuration required for out-of-the-box usage

### Documentation
- Setup time < 2 minutes from clone to first debug session
- Troubleshooting guide resolves 95%+ of common issues
- Examples cover all debugging scenarios (unit, integration, stress)

### Quality
- Configurations tested on Windows, macOS, and Linux
- Works with both JUnit and TestNG
- Compatible with Maven and Gradle builds
- Performance: No noticeable overhead vs. normal debugging
