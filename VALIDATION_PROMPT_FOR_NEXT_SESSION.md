# DTR Pre-Maven Central Release - Validation Prompt

**For Next Session:** Use this prompt to validate all pre-release work before proceeding to Maven Central.

---

## Validation Prompt for Claude Code

**Copy and paste this into Claude Code to validate the project:**

```
Validate the DTR project for Maven Central release readiness using these checks:

BRANCH: claude/rename-doctester-to-dtr-m79sP

1. **Verify Git Status**
   - Command: git log --oneline | head -10
   - Expected: See 6 commits ending with "a71688e fix: Correct README imports..."

2. **Verify Maven Coordinates**
   - Check pom.xml root: groupId should be io.github.seanchatmangpt.dtr (NOT org.r10r)
   - Check all artifacts: dtr, dtr-core, dtr-integration-test, dtr-benchmarks (NOT doctester-*)
   - Verify version: 2.5.0-SNAPSHOT across all modules

3. **Verify Java Package Structure**
   - Command: grep -r "package io.github.seanchatmangpt.dtr" dtr-core/src/main/java | wc -l
   - Expected: 94+ files in new package
   - Command: grep -r "package org.r10r.doctester" dtr-core/src/main/java | wc -l
   - Expected: 0 old package references

4. **Verify README.md Imports**
   - Line 18: Should be "import io.github.seanchatmangpt.dtr.DocTester;"
   - Line 601: Should be "import io.github.seanchatmangpt.dtr.DocTester;"
   - NOT "import io.github.seanchatmangpt.dtr.doctester.DocTester;"

5. **Verify Test Resources**
   - Command: ls dtr-core/src/test/resources/io/github/seanchatmangpt/dtr/
   - Expected: custom_doctester_stylesheet.css exists in new location
   - Command: ls dtr-core/src/test/resources/org/doctester/ 2>&1
   - Expected: "No such file or directory" (old path removed)

6. **Verify Documentation**
   - README.md: Should reference io.github.seanchatmangpt.dtr:dtr-core
   - CONTRIBUTING.md: Should exist at root level
   - CODE_OF_CONDUCT.md: Should exist at root level
   - License: Apache 2.0 preserved

7. **Verify No Old References (Critical)**
   - Command: grep -r "org.r10r" pom.xml dtr-core/pom.xml dtr-integration-test/pom.xml dtr-benchmarks/pom.xml 2>/dev/null | wc -l
   - Expected: 0 (no old groupId references in critical files)
   - Command: grep -r "doctester-core" dtr-core/src/main/java/ dtr-integration-test/src/main/java/ 2>/dev/null | wc -l
   - Expected: 0 (no old artifact references in Java code)

8. **Verify Governance**
   - CONTRIBUTING.md exists and contains development setup instructions
   - CODE_OF_CONDUCT.md exists with Contributor Covenant text

9. **Run Build Test (Optional)**
   - Command: mvnd clean install -DskipTests 2>&1 | tail -20
   - Expected: BUILD SUCCESS (no compilation errors)

If all checks pass, the project is ready for:
1. Version bump (2.5.0-SNAPSHOT → 2.5.0)
2. Maven Central release via Sonatype Central Repository Portal

Report any failures for quick diagnosis.
```

---

## Quick Validation Checklist

Print this and check off as you validate:

- [ ] 6 git commits present (a71688e is latest)
- [ ] pom.xml: groupId = io.github.seanchatmangpt.dtr
- [ ] All modules: artifactId = dtr-*
- [ ] Version: 2.5.0-SNAPSHOT consistent
- [ ] 94+ files in io.github.seanchatmangpt.dtr package
- [ ] 0 files in org.r10r.doctester package
- [ ] README.md imports correct (no .doctester)
- [ ] Test resource: io/github/seanchatmangpt/dtr/custom_doctester_stylesheet.css exists
- [ ] Old test resource: org/doctester/ is gone
- [ ] CONTRIBUTING.md exists
- [ ] CODE_OF_CONDUCT.md exists
- [ ] No "org.r10r" in pom.xml files
- [ ] No "doctester-" artifact refs in Java code
- [ ] Maven build succeeds (optional verification)

---

## Files Modified Summary

**169 Files Total:**
- Session 1 (Rename): 115 files
- Session 2 (Maven Central Fixes): 54 files  
- Session 3 (Final Cleanup): 3 files
- Verification Reports: 4 files

**Git History:**
1. 7129e92 - docs: Rename DocTester references to DTR
2. 4515a99 - fix: Resolve all blocking issues for Maven Central release
3. 4b6b228 - docs: Add final verification report...
4. 0a48c09 - docs: Add verification summary
5. 0339003 - docs: Add verification index
6. a71688e - fix: Correct README imports and migrate test resources

---

## What Was Fixed

✅ Java version mismatch (25 → 26)
✅ 16 test documentation references  
✅ 119 groupId references in docs
✅ 173 GitHub repository URLs
✅ Missing CONTRIBUTING.md
✅ Missing CODE_OF_CONDUCT.md
✅ README.md import paths
✅ Test resource file path

---

## Ready for Release When:

✅ All validation checks pass
✅ Working tree is clean (git status shows clean)
✅ All 6 commits are present
✅ Maven build succeeds (optional but recommended)

Then proceed with:
1. Merge PR to main
2. Update version to 2.5.0
3. Build and sign: `mvnd clean verify -P release`
4. Deploy: `mvnd deploy -P release`
5. Publish via Sonatype Central

---

**Session ID:** session_019CAqUj2PdX3JZNH79xYFEG
**Branch:** claude/rename-doctester-to-dtr-m79sP
**Status:** Ready for validation ✅
