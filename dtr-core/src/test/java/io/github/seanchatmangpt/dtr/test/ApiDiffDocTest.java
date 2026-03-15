/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

/**
 * DTR documentation test for the {@code sayApiDiff} innovation.
 *
 * <p>{@code sayApiDiff(Class<?> before, Class<?> after)} computes the semantic diff between
 * two class versions using Java reflection. It produces three categorized tables:
 * <em>added</em> methods, <em>removed</em> methods, and <em>signature-changed</em> methods.
 * Because the diff is derived directly from bytecode at test runtime, it cannot drift from
 * the real API — it IS the real API.</p>
 *
 * <p>Tests in this class cover three scenarios:</p>
 * <ol>
 *   <li>A realistic service evolution from V1 to V2 — breaking changes visible at a glance.</li>
 *   <li>A comparison of a class with itself — proves the "no changes" base case renders cleanly.</li>
 *   <li>A real-world comparison between {@link RenderMachineCommands} and {@link RenderMachine}
 *       to show {@code sayApiDiff} applied to production DTR types.</li>
 * </ol>
 *
 * <p>All three tests call real DTR infrastructure. No measurements are simulated or estimated.</p>
 *
 * @see io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands#sayApiDiff(Class, Class)
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ApiDiffDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Inline API versions used as diff subjects
    // =========================================================================

    /**
     * Version 1 of a fictional UserService.
     * Public API: findUser, deleteUser, listUsers.
     */
    static class UserServiceV1 {
        public String findUser(String id) { return null; }
        public void deleteUser(String id) {}
        public List<String> listUsers() { return null; }
    }

    /**
     * Version 2 of the fictional UserService.
     * Breaking changes versus V1:
     * <ul>
     *   <li>deleteUser removed — callers will fail at runtime if not updated.</li>
     *   <li>findUserByEmail added — new lookup path.</li>
     *   <li>updateUser added — new mutation path.</li>
     * </ul>
     */
    static class UserServiceV2 {
        public String findUser(String id) { return null; }
        public String findUserByEmail(String email) { return null; }
        public List<String> listUsers() { return null; }
        public void updateUser(String id, String data) {}
    }

    // =========================================================================
    // Test methods
    // =========================================================================

    /**
     * Documents the evolution from UserServiceV1 to UserServiceV2.
     *
     * <p>Shows why breaking-change detection matters: one removed method can silently
     * compile against the old API and fail at runtime in consumers that were never
     * recompiled. {@code sayApiDiff} surfaces this in the generated documentation
     * so readers know exactly what changed before upgrading.</p>
     */
    @Test
    void a1_sayApiDiff_user_service_evolution() {
        sayNextSection("sayApiDiff — UserService V1 to V2 Evolution");

        say("API compatibility is one of the hardest guarantees to maintain across " +
                "releases. A single method removal or signature change silently breaks " +
                "downstream consumers that compiled against the old version. " +
                "`sayApiDiff` surfaces every change by comparing the public method " +
                "sets of two class versions using Java reflection — no source access " +
                "required, no manual changelog entry needed.");

        say("The two versions below represent a realistic service refactor. " +
                "V1 exposes `deleteUser`, which V2 removes in favour of a softer " +
                "deactivation path (not shown). V2 adds `findUserByEmail` and " +
                "`updateUser`. Any caller relying on `deleteUser` will break.");

        sayCode("""
                // V1 — original contract
                static class UserServiceV1 {
                    public String findUser(String id) { return null; }
                    public void deleteUser(String id) {}
                    public List<String> listUsers() { return null; }
                }

                // V2 — evolved contract (deleteUser removed, two methods added)
                static class UserServiceV2 {
                    public String findUser(String id) { return null; }
                    public String findUserByEmail(String email) { return null; }
                    public List<String> listUsers() { return null; }
                    public void updateUser(String id, String data) {}
                }

                // In test:
                sayApiDiff(UserServiceV1.class, UserServiceV2.class);
                """, "java");

        sayWarning("Removing `deleteUser` is a binary-breaking change. " +
                "Any compiled caller that references this method will throw " +
                "NoSuchMethodError at runtime until recompiled against V2.");

        sayApiDiff(UserServiceV1.class, UserServiceV2.class);

        say("The diff tables above are generated entirely from bytecode via " +
                "`Class.getDeclaredMethods()`. No source files or Javadoc are required. " +
                "The documentation is always consistent with the binary on the classpath.");

        sayNote("Run `sayApiDiff` in a dedicated test per release to make every " +
                "breaking change visible in the generated docs before the artifact ships.");
    }

    /**
     * Documents the no-changes base case: comparing a class with itself.
     *
     * <p>A clean diff — zero added, zero removed, zero changed — is as important
     * to document as a diff with changes. It gives readers confidence that a release
     * is fully backward-compatible and that the tooling is functioning correctly.</p>
     */
    @Test
    void a2_sayApiDiff_same_api_no_changes() {
        sayNextSection("sayApiDiff — No Changes (Same API Compared with Itself)");

        say("A diff that shows no changes is an explicit, machine-verified claim of " +
                "backward compatibility. Comparing a class with itself is the simplest " +
                "way to confirm that `sayApiDiff` produces an empty result — and " +
                "therefore that a patch release has introduced no API modifications.");

        say("Here, `UserServiceV1` is passed as both the `before` and `after` argument. " +
                "Every method present in V1 is also present in V1 with an identical " +
                "signature, so all three tables (added, removed, changed) must be empty.");

        sayCode("""
                // Both arguments are the same class — no diff expected
                sayApiDiff(UserServiceV1.class, UserServiceV1.class);
                """, "java");

        sayApiDiff(UserServiceV1.class, UserServiceV1.class);

        sayNote("An empty diff is not the same as skipping the diff. " +
                "Generating it explicitly provides a documented, timestamped assertion " +
                "that the API surface has not changed between runs.");
    }

    /**
     * Documents a real-world API diff between {@link RenderMachineCommands} and
     * {@link RenderMachine}.
     *
     * <p>{@code RenderMachineCommands} is the minimal public contract — the interface
     * every render machine must satisfy. {@code RenderMachine} is the abstract base class
     * that extends that contract with lifecycle and output-routing methods. Diffing them
     * reveals the exact set of methods that {@code RenderMachine} adds beyond the core
     * interface, making the architectural boundary explicit in the generated documentation.</p>
     */
    @Test
    void a3_sayApiDiff_render_machine_versions() {
        sayNextSection("sayApiDiff — RenderMachineCommands vs RenderMachine (Real-World Diff)");

        say("`RenderMachineCommands` defines the minimal `say*` contract that all DTR " +
                "render machines must implement. `RenderMachine` is the abstract base " +
                "class that inherits that contract and extends it with additional " +
                "lifecycle and output-routing methods such as `setFileName`, " +
                "`saySlideOnly`, `sayDocOnly`, and `saySpeakerNote`.");

        say("Running `sayApiDiff` across these two types reveals the exact surface that " +
                "`RenderMachine` adds beyond the core interface. This makes the " +
                "architectural boundary self-documenting: any method visible in the " +
                "\"added\" table is an extension point not required by the contract.");

        sayCode("""
                sayApiDiff(
                    RenderMachineCommands.class,  // before: the interface contract
                    RenderMachine.class           // after:  the abstract base class
                );
                """, "java");

        sayApiDiff(
                RenderMachineCommands.class,
                RenderMachine.class
        );

        say("The diff is computed at test runtime from the live `.class` files on the " +
                "classpath. If `RenderMachine` gains or loses methods in a future release, " +
                "the generated documentation updates automatically on the next test run " +
                "without any manual changelog edit.");

        sayNote("Use `sayApiDiff(interface, abstractClass)` as a recurring pattern to " +
                "document the layered extension points in any framework you ship with DTR.");
    }
}
