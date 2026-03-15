# Contributing to DTR: 30-Minute Quickstart

Get from zero to making your first contribution to DTR in 30 minutes. This guide is for developers who want to contribute to the DTR project itself.

**Time to first contribution:** 30 minutes
**Prerequisites:** Basic Git knowledge, Java development experience

---

## Prerequisites Check (5 minutes)

### 1. Verify Java 26 is Installed

DTR requires Java 26+ with preview features enabled.

```bash
# Check Java version
java -version
# Expected: openjdk version "26.ea.13" or higher

# If not installed, install via SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 26.ea.13-graal
sdk use java 26.ea.13-graal

# Verify installation
java -version
```

### 2. Verify Maven mvnd is Installed

DTR uses Maven 4+ with the mvnd daemon (recommended for speed).

```bash
# Check mvnd version
mvnd --version
# Expected: Apache Maven 4.0.0-rc-3+ or mvnd 2.0.0+

# If not installed, on macOS:
brew install mvnd

# On Linux:
wget https://github.com/apache/maven-mvnd/releases/download/2.0.0/mvnd-2.0.0-linux-amd64.tar.gz
tar -xzf mvnd-2.0.0-linux-amd64.tar.gz
export PATH="$PATH:$PWD/mvnd-2.0.0-linux-amd64/bin"

# Verify installation
mvnd --version
```

### 3. Verify Git is Installed

```bash
# Check Git version
git --version
# Expected: git version 2.x or higher

# If not installed, on macOS:
# Git is included with Xcode Command Line Tools
xcode-select --install

# On Linux:
sudo apt-get install git  # Ubuntu/Debian
sudo yum install git      # RHEL/CentOS
```

**✅ Prerequisites check complete!** Move to project setup.

---

## Project Setup (5 minutes)

### 1. Clone the Repository

```bash
# Clone your fork (replace YOUR_USERNAME with your GitHub username)
git clone https://github.com/YOUR_USERNAME/dtr.git
cd dtr

# Add upstream remote to track the main repository
git remote add upstream https://github.com/seanchatmangpt/dtr.git

# Verify remotes
git remote -v
# Expected output:
# origin    https://github.com/YOUR_USERNAME/dtr.git (fetch)
# origin    https://github.com/YOUR_USERNAME/dtr.git (push)
# upstream  https://github.com/seanchatmangpt/dtr.git (fetch)
# upstream  https://github.com/seanchatmangpt/dtr.git (push)
```

### 2. Verify the Build

```bash
# Clean build with all tests
mvnd clean verify

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Total time: XX seconds
```

**If the build fails:**

- Check Java version: `java -version` must be 26+
- Check Maven version: `mvnd --version` must be 4.0.0-rc-3+
- Check network: Some tests require internet access
- Try with verbose output: `mvnd clean verify -X`

### 3. Run Tests

```bash
# Run all tests
mvnd test

# Expected output:
# [INFO] Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
# [INFO] BUILD SUCCESS
```

**✅ Project setup complete!** You can now build and test DTR locally.

---

## Making Your First Change (10 minutes)

### Step 1: Choose a Simple Task

Good first contributions for beginners:

1. **Fix typos in documentation** - Search for misspelled words in markdown files
2. **Add Javadoc to undocumented methods** - Add documentation to public methods
3. **Improve test coverage** - Add tests for uncovered code paths
4. **Add example tests** - Create new documentation examples

**Find issues to work on:**

- Browse [GitHub Issues with "good first issue" label](https://github.com/seanchatmangpt/dtr/labels/good%20first%20issue)
- Check [GitHub Discussions](https://github.com/seanchatmangpt/dtr/discussions) for feature requests
- Review the codebase and identify improvements

### Step 2: Create a Feature Branch

```bash
# Ensure you're on the latest main branch
git fetch upstream
git checkout main
git merge upstream/main

# Create a new branch for your change
git checkout -b my-first-contribution

# Verify branch
git branch
# Expected: * my-first-contribution
```

### Step 3: Make Your Change

**Example 1: Fix a typo in documentation**

```bash
# Find the file with the typo
grep -r "recieve" docs/  # Example: misspelled "receive"

# Edit the file
nano docs/README.md  # Or use your preferred editor

# Fix the typo: "recieve" → "receive"
```

**Example 2: Add Javadoc to a method**

```bash
# Find undocumented public methods
find dtr-core/src/main/java -name "*.java" -exec grep -L "^\s*\*" {} \;

# Edit a file
nano dtr-core/src/main/java/io/github/seanchatmangpt/dtr/SomeClass.java

# Add Javadoc before the method:
/**
 * Does something useful.
 *
 * @param input the input parameter
 * @return the result
 */
public void someMethod(String input) {
    // ...
}
```

**Example 3: Add a test**

```bash
# Create a new test file
nano dtr-core/src/test/java/io/github/seanchatmangpt/dtr/MyExampleDocTest.java

# Add this content:
package io.github.seanchatmangpt.dtr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class MyExampleDocTest extends DtrTest {

    @Test
    @DocSection("My Example")
    void myExampleTest() {
        say("This is an example test.");
        sayCode("int x = 42;", "java");
    }
}
```

### Step 4: Test Your Changes

```bash
# Run tests to verify your changes don't break anything
mvnd test

# Run specific test class (if you added a test)
mvnd test -Dtest=MyExampleDocTest

# Run full verification (including integration tests)
mvnd clean verify

# Expected: All tests pass, BUILD SUCCESS
```

### Step 5: Commit Your Changes

```bash
# Check what changed
git status

# Stage your changes
git add docs/README.md
# OR for multiple files:
git add .

# Commit with a clear message
git commit -m "docs: fix typo in README

- Fixed misspelling of 'receive'
- Improved clarity in installation section"

# View commit
git log -1 --stat
```

**Commit message format:**

- `docs:` - Documentation changes
- `feat:` - New feature
- `fix:` - Bug fix
- `test:` - Adding or updating tests
- `refactor:` - Code refactoring
- `chore:` - Build process, dependencies, etc.

**✅ First change complete!** Now push and create a pull request.

---

## Creating a Pull Request (10 minutes)

### Step 1: Push to Your Fork

```bash
# Push your branch to your fork
git push origin my-first-contribution

# Expected output:
# * [new branch]      my-first-contribution -> my-first-contribution
```

### Step 2: Create Pull Request on GitHub

1. **Open your browser** to: `https://github.com/YOUR_USERNAME/dtr`

2. **Click "Compare & pull request"** button (GitHub should show this automatically)

3. **If you don't see the button:**
   - Click "Pull requests" tab
   - Click "New pull request"
   - Click "compare across forks" link
   - Select base repository: `seanchatmangpt/dtr` ← base: `main`
   - Select head repository: `YOUR_USERNAME/dtr` ← compare: `my-first-contribution`

4. **Fill in PR details:**

**Title:** Clear, concise description of your change
```
docs: fix typo in README installation instructions
```

**Description:**
```markdown
## What Changed
- Fixed misspelling of "receive" in installation section
- Improved clarity in prerequisite instructions

## Why
Typo fix to improve documentation quality.

## Testing
- Verified build passes: `mvnd clean verify`
- Confirmed fix resolves the issue
- No breaking changes

## Checklist
- [x] Tests pass locally
- [x] Commit message follows conventions
- [x] PR title follows conventions
- [x] Description is clear and complete

## Related Issues
Fixes #123 (if applicable)
```

5. **Click "Create pull request"**

### Step 3: Respond to Feedback

1. **Watch for CI results** - GitHub Actions will run tests automatically
   - All checks must pass (green ✓)
   - If any fail, click "Details" to see the error log

2. **Address reviewer feedback**
   - Maintainers may request changes
   - Make requested changes in your branch
   - Commit changes: `git commit -am "address review feedback"`
   - Push updates: `git push origin my-first-contribution`
   - PR will update automatically

3. **Celebrate!** 🎉
   - Once approved, your PR will be merged
   - Your contribution will be credited in release notes
   - You're now a DTR contributor!

### Step 4: Clean Up (Optional)

After your PR is merged:

```bash
# Switch back to main
git checkout main

# Pull latest changes from upstream
git fetch upstream
git merge upstream/main

# Delete your local branch
git branch -d my-first-contribution

# Delete remote branch (optional)
git push origin --delete my-first-contribution
```

---

## Good First Tasks

### For Documentation Contributors

- **Fix typos:** Search the codebase for common misspellings
- **Improve clarity:** Rewrite confusing sections
- **Add examples:** Add code examples to API documentation
- **Translate:** Add translations for existing documentation

### For Code Contributors

- **Add tests:** Increase test coverage for existing features
- **Add Javadoc:** Document undocumented public methods
- **Fix bugs:** Look for issues labeled "bug"
- **Small features:** Implement simple enhancements

### Find Issues to Work On

1. **GitHub Issues:** [dtr/issues](https://github.com/seanchatmangpt/dtr/issues)
   - Filter by labels: `good first issue`, `documentation`, `help wanted`
   - Comment on the issue to claim it

2. **GitHub Discussions:** [dtr/discussions](https://github.com/seanchatmangpt/dtr/discussions)
   - Propose new features
   - Ask questions before implementing

3. **Code Review:** [dtr/pulls](https://github.com/seanchatmangpt/dtr/pulls)
   - Review other PRs to learn the codebase
   - Provide constructive feedback

---

## Common Issues & Solutions

### Build Fails with "Java 26 required"

**Problem:** Maven enforcer plugin rejects Java version

**Solution:**
```bash
java -version  # Must be 26.ea.13 or higher
sdk use java 26.ea.13-graal  # Switch to Java 26
export JAVA_HOME=$(/usr/libexec/java_home -v 26)  # macOS
```

### Tests Fail When Run in IDE

**Problem:** Tests fail in IntelliJ IDEA or Eclipse

**Solution:** Configure IDE to use `--enable-preview`

- **IntelliJ IDEA:**
  - File → Settings → Build, Execution, Deployment → Build Tools → Maven → Runner
  - VM Options: `--enable-preview`

- **Eclipse:**
  - Run → Run Configurations → JUnit → Arguments
  - VM Arguments: `--enable-preview`

### Push Rejected - "Protected Branch"

**Problem:** Can't push directly to `main` branch

**Solution:** Never push to `main` directly. Always create a branch:
```bash
git checkout -b my-feature
# Make changes
git push origin my-feature
# Create PR from branch
```

### Merge Conflicts When Updating Main

**Problem:** `git merge upstream/main` has conflicts

**Solution:**
```bash
# Fetch latest upstream
git fetch upstream

# Merge with upstream main
git merge upstream/main

# Resolve conflicts in your editor
# Remove conflict markers and keep desired code

# Mark conflicts as resolved
git add <resolved-files>

# Complete the merge
git commit

# Continue with your work
```

---

## Next Steps

Congratulations on your first contribution! Here's what to explore next:

### Learn the Codebase

- **[Full Contributing Guide](/Users/sac/dtr/CONTRIBUTING.md)** - Detailed contribution guidelines
- **[Project Structure](/Users/sac/dtr/docs/contributing/codebase-tour.md)** - Overview of DTR architecture
- **[Developer Workflow](/Users/sac/dtr/docs/contributing/making-changes.md)** - Advanced development workflows

### Advanced Contributions

- **[Release Process](/Users/sac/dtr/docs/contributing/releasing.md)** - How releases are managed
- **[Local CI Testing](/Users/sac/dtr/CONTRIBUTING.md#local-ci-testing-with-act)** - Test CI workflows locally
- **[Java 26 Features](/Users/sac/dtr/docs/explanation/java26-code-reflection.md)** - Advanced Java 26 features in DTR

### Stay Connected

- **Star the repo:** [github.com/seanchatmangpt/dtr](https://github.com/seanchatmangpt/dtr)
- **Watch releases:** Get notified of new versions
- **Join discussions:** Participate in [GitHub Discussions](https://github.com/seanchatmangpt/dtr/discussions)

---

## Additional Resources

### Documentation

- **[Full Documentation Index](/Users/sac/dtr/docs/index.md)** - All DTR documentation
- **[say* API Reference](/Users/sac/dtr/docs/reference/say-api.md)** - Complete API documentation
- **[Tutorials](/Users/sac/dtr/docs/tutorials/)** - Learn DTR by doing

### Developer Tools

- **[CLAUDE.md](/Users/sac/dtr/CLAUDE.md)** - Developer quick reference
- **[BUILD_FIXES.md](/Users/sac/dtr/docs/BUILD_FIXES.md)** - Common build issues and solutions

### Community

- **[GitHub Issues](https://github.com/seanchatmangpt/dtr/issues)** - Bug reports and feature requests
- **[GitHub Discussions](https://github.com/seanchatmangpt/dtr/discussions)** - General questions and ideas

---

**Thank you for contributing to DTR!** 🎉

Your contributions help make documentation and testing better for everyone. Every contribution matters, whether it's a typo fix, a new test, or a major feature.

**Questions?** Open a [GitHub Discussion](https://github.com/seanchatmangpt/dtr/discussions) or reach out to maintainers.
