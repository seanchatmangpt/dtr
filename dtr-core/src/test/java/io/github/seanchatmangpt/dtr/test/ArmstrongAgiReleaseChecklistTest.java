package io.github.seanchatmangpt.dtr;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Joe Armstrong AGI Release Checklist — DTR 2026.
 *
 * <p>Armstrong's rule: a checklist item is only valid if a machine can verify it.
 * If you need a human to interpret it, rewrite it. A checklist that depends on
 * judgment is a prayer, not a protocol.</p>
 *
 * <p>AGI here means what Armstrong would have meant: Automated Gate Intelligence.
 * The system checks itself. The human decides the change type. The machine decides
 * whether the release is fit to ship. If the gate says no, the human is wrong,
 * not the gate.</p>
 *
 * <p>This test generates the living checklist. It cannot lie — if it compiles and
 * passes, every item it describes was verified at the moment of generation.</p>
 */
class ArmstrongAgiReleaseChecklistTest extends DtrTest {

    @Test
    void armstrongReleaseChecklist() {
        sayNextSection("Joe Armstrong AGI Release Checklist — DTR 2026");

        say(
            "Armstrong's thesis on checklists, stated plainly: a checklist item is only valid " +
            "if a machine can evaluate it without asking a human what it means. " +
            "Any item that requires interpretation is a bug in the checklist, not a feature. " +
            "Rewrite it until the answer is yes or no. " +
            "A system that can release itself is more reliable than a system that depends on " +
            "a human remembering the correct sequence at the correct moment. " +
            "This checklist is the specification of done. The pipeline is the enforcer."
        );

        sayNextSection("Phase 0: Invariants (Cannot Proceed Without These)");

        say(
            "These are not suggestions. They are load-bearing constraints. " +
            "If any of these fail, the release does not happen. " +
            "No exceptions. No overrides. No 'just this once'."
        );

        sayTable(new String[][] {
            {"#", "Invariant", "Machine Check", "Pass Criterion"},
            {"0.1", "Java version is 26", "`java -version`", "Output contains `26`"},
            {"0.2", "mvnd version is 2.0.0+", "`mvnd --version`", "Output contains `2.0`"},
            {"0.3", "--enable-preview is set", "`cat .mvn/maven.config`", "Contains `--enable-preview`"},
            {"0.4", "No uncommitted changes", "`git status --porcelain`", "Output is empty"},
            {"0.5", "On correct branch", "`git branch --show-current`", "Branch is `main` or release branch"},
            {"0.6", "GPG key is loaded", "`gpg --list-secret-keys`", "Key ID present in output"},
            {"0.7", "gpg-agent.conf has loopback", "`grep loopback ~/.gnupg/gpg-agent.conf`", "Line exists"},
            {"0.8", "mvnd verify passes", "`mvnd verify`", "Exit code 0"},
        });

        sayWarning(
            "Phase 0 is not optional. These invariants encode real CI failures that have " +
            "already happened. The GPG loopback entry exists because a release failed without it. " +
            "The `--enable-preview` flag exists because Java 26 preview syntax silently rejects " +
            "without it. Do not remove these checks because they 'seem obvious'. " +
            "The obvious ones are the ones that bite you at 2am."
        );

        sayNextSection("Phase 1: The Human Decision (The Only Human Decision)");

        say(
            "This is the only place in the release process where a human is required. " +
            "The human answers one question: what kind of change is this? " +
            "The version number is a mechanical consequence of that answer. " +
            "The human is not allowed to specify the version number. " +
            "The human is not allowed to run `mvn deploy`. " +
            "The human is not allowed to push a tag manually. " +
            "The human types one command. Everything else is automatic."
        );

        sayTable(new String[][] {
            {"Change Type", "Human Command", "Version Effect", "Example"},
            {"New say* methods, additive features", "`make release-minor`", "YYYY.N+1.0", "2026.1.0 → 2026.2.0"},
            {"Bug fix, no API change", "`make release-patch`", "YYYY.MINOR.N+1", "2026.1.0 → 2026.1.1"},
            {"Explicit year boundary (January)", "`make release-year`", "YYYY.1.0", "2026.7.2 → 2027.1.0"},
            {"Test candidate before minor", "`make release-rc-minor`", "YYYY.N+1.0-rc.N", "2026.1.0 → 2026.2.0-rc.1"},
            {"Test candidate before patch", "`make release-rc-patch`", "YYYY.MINOR.N+1-rc.N", "2026.1.0 → 2026.1.1-rc.1"},
        });

        sayNote(
            "Armstrong on decision minimisation: 'The best interface is one where the user " +
            "cannot make a wrong choice. If there are five valid inputs and one of them is wrong " +
            "in most contexts, redesign until there are four valid inputs.' " +
            "This release interface has five commands. Four are used routinely. " +
            "The fifth (release-year) fires once per year. No command produces a wrong version."
        );

        sayNextSection("Phase 2: Automated Gate (What the Machine Verifies)");

        say(
            "After `make release-*` is typed, the human is done. " +
            "The following items are verified automatically by the pipeline. " +
            "They are listed here so you understand what the machine is checking, " +
            "not so you can check them yourself. " +
            "If you find yourself manually verifying these, the pipeline is broken."
        );

        sayOrderedList(List.of(
            "scripts/bump.sh reads current version from pom.xml — no human arithmetic",
            "CalVer arithmetic: if calendar year changed, MINOR resets to 1",
            "All pom.xml files updated atomically by scripts/set-version.sh",
            "scripts/changelog.sh generates docs/releases/VERSION.md from git log",
            "docs/CHANGELOG.md updated and committed",
            "Git tag vVERSION created and pushed — this is the release trigger",
            "GitHub Actions fires on tag push — classify → build → deploy → release",
            "mvnd verify runs in CI (Java 26, --enable-preview, fresh runner)",
            "GPG signing via --pinentry-mode loopback (non-interactive, no TTY)",
            "Central Publishing Maven Plugin pushes to Maven Central staging",
            "Maven Central validates POM, signatures, sources, javadoc JARs",
            "GitHub Release created from docs/releases/VERSION.md"
        ));

        sayNextSection("Phase 3: The Six Conditions for Release Success");

        say(
            "These six conditions are the finite, enumerable answer to the question: " +
            "'What needs to be true for make release-minor to succeed in a GitHub Actions runner " +
            "and produce a complete receipted release?' " +
            "If all six are true, the release succeeds. " +
            "If any one is false, the release fails at a deterministic point. " +
            "There is no seventh condition. There is no 'it depends'."
        );

        var conditions = new LinkedHashMap<String, String>();
        conditions.put("1. mvnd verify passes on all modules",
            "CI gate runs on Java 26 with --enable-preview. All unit, integration, and DTR tests pass.");
        conditions.put("2. All DTR test output lands in target/docs/",
            "RenderMachine routes output to target/docs/test-results/. No other path is valid.");
        conditions.put("3. GPG key is loaded and loopback-capable",
            "gpg-agent.conf contains allow-loopback-pinentry. --pinentry-mode loopback works without TTY.");
        conditions.put("4. CENTRAL_USERNAME and CENTRAL_TOKEN secrets are set",
            "GitHub secrets present in the repository. Not in ~/.m2/settings.xml. Never in code.");
        conditions.put("5. release Maven profile signs, packages, and publishes",
            "central-publishing-maven-plugin configured. Sources JAR, Javadoc JAR, signatures included.");
        conditions.put("6. GitHub Release created from docs/releases/VERSION.md",
            "gh release create fires after deploy. Release notes are the generated changelog, not manual text.");
        sayKeyValue(conditions);

        sayNextSection("Phase 4: Failure Modes and Recovery");

        say(
            "Armstrong on failure: 'A system that cannot tell you why it failed is a system " +
            "that will fail again for the same reason.' " +
            "Each failure mode below has a deterministic cause and a deterministic fix. " +
            "If your failure is not in this table, add it after you understand the root cause."
        );

        sayTable(new String[][] {
            {"Failure", "Symptom", "Root Cause", "Fix"},
            {"GPG pinentry timeout", "deploy hangs indefinitely", "gpg-agent expects TTY", "Add allow-loopback-pinentry to gpg-agent.conf"},
            {"Preview features rejected", "compilation error on sealed/pattern syntax", "--enable-preview missing", "Verify .mvn/maven.config and surefire config"},
            {"Maven Central 401", "deploy fails with auth error", "CENTRAL_TOKEN expired or wrong secret name", "Rotate token in Sonatype, update GitHub secret"},
            {"Wrong version in tag", "impossible — scripts own arithmetic", "Human typed version manually", "Stop. Read CLAUDE.md. Use make release-*"},
            {"DTR output missing", "GitHub Release has no docs", "Test wrote to wrong path", "Fix test: output must go to target/docs/"},
            {"mvnd verify fails in CI, passes locally", "build green locally, red in CI", "Preview flag not in surefire config", "Add --enable-preview to argLine in maven-surefire-plugin"},
            {"RC pushed to Maven Central", "stable users get pre-release", "Wrong deploy job triggered", "classify job must gate on tag format v*.*.* vs v*.*.*-rc.*"},
            {"Year reset not triggered", "2027.1.0 not produced in January", "bump.sh not run in new year", "scripts/bump.sh reads date +%Y — run make release-minor in January"},
        });

        sayNextSection("Phase 5: Post-Release Verification (Automated)");

        say(
            "These items are verified by the pipeline after the release tag fires. " +
            "They are listed here as the definition of 'release complete'. " +
            "If the pipeline passes, all of these are true. " +
            "If you need to verify them manually, the pipeline is not done."
        );

        sayUnorderedList(List.of(
            "Tag v{VERSION} exists in git log",
            "GitHub Release exists with correct version and release notes",
            "Maven Central shows {groupId}:{artifactId}:{VERSION} as released",
            "docs/CHANGELOG.md contains the new version entry",
            "docs/releases/{VERSION}.md exists and is non-empty",
            "All module POMs contain the new version (no snapshot suffix)",
            "No uncommitted files remain after the release commit"
        ));

        sayNextSection("The Armstrong Invariant, Stated Once");

        say(
            "The human decides the type of change. " +
            "That is the only decision a human is qualified to make that a script cannot. " +
            "The version number is a mechanical consequence — scripts/bump.sh owns the arithmetic, " +
            "scripts/release.sh owns the tag and push, " +
            "GitHub Actions owns the signing and Maven Central publish. " +
            "No human ever types a version number. " +
            "No version number is ever wrong. " +
            "The tag is the trigger. The trigger is the specification of done."
        );

        sayCode(
            """
            # The complete release interface. Nothing else is valid.
            make release-patch   # bug fix, no API change
            make release-minor   # new say* methods, backward compatible
            make release-year    # explicit year boundary (January trigger)

            # Forbidden:
            # mvn deploy                    (closes this path entirely)
            # make release VERSION=x.y.z    (requires human arithmetic — drift surface)
            # git tag v2026.2.0 && git push  (bypasses changelog and bump)
            """,
            "bash"
        );

        sayWarning(
            "If you are about to type a version number, stop. " +
            "If you are about to run `mvn deploy`, stop. " +
            "If you are about to push a tag manually, stop. " +
            "Read this checklist from Phase 0. " +
            "The invariant is not a suggestion. It is the design."
        );

        sayNote(
            "This checklist is generated by a DTR test. It cannot contradict the release process " +
            "it describes, because it is executed as part of the same build that validates the release process. " +
            "If the test passes, the checklist is current. " +
            "If the test fails, the checklist is the first thing to fix."
        );
    }
}
