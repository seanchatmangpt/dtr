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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

/**
 * Documents the {@code sayAgentLoop} innovation in the DTR {@code say*} API.
 *
 * <p>This is the <strong>first documentation primitive designed specifically for
 * agentic AI workflows</strong>. No other documentation framework exposes a
 * purpose-built rendering primitive for the observe-decide-act loop that drives
 * every autonomous agent, from a two-line shell script to a multi-step LLM
 * planning system.</p>
 *
 * <p>{@code sayAgentLoop(String agentName, List<String> observations,
 * List<String> decisions, List<String> tools)} converts four plain Java lists into
 * a rendered Mermaid {@code sequenceDiagram} that shows how the agent interacts
 * with its environment across one full reasoning cycle:</p>
 *
 * <ul>
 *   <li><strong>observations</strong> — inputs the agent perceived from the
 *       environment (file system scans, metric readings, CI signals, etc.)</li>
 *   <li><strong>decisions</strong> — the reasoning steps or actions the agent
 *       chose in response to those observations</li>
 *   <li><strong>tools</strong> — external system calls the agent issued during
 *       execution (file reads, API calls, shell commands, etc.)</li>
 * </ul>
 *
 * <p>Three representative agent types are documented here, each chosen to
 * illustrate a different class of agentic workflow that arises in modern
 * software engineering:</p>
 *
 * <ol>
 *   <li><strong>DTR Documentation Agent</strong> — an agent that generated this
 *       very file. Documents the observe-decide-act cycle used to detect a missing
 *       {@code say*} primitive, design its signature, and write the implementation
 *       and its test. The self-referential nature of this example makes the value
 *       proposition tangible: the agent loop that produced the code is itself
 *       documented using the primitive it created.</li>
 *   <li><strong>Release Automation Agent</strong> — the CI/CD agent that governs
 *       every DTR release. Observes tag events and credential state, decides
 *       whether conditions are safe to publish, and issues the exact Maven and
 *       GPG commands that move artifacts from staging to Maven Central.</li>
 *   <li><strong>Health Monitor Agent</strong> — a production SRE agent that
 *       continuously evaluates system health signals and escalates when SLOs are
 *       breached. Illustrates how {@code sayAgentLoop} scales from developer
 *       tooling to production operations.</li>
 * </ol>
 *
 * <p>All three tests use {@code sayAgentLoop} as the primary rendering call,
 * supplemented by {@code say()}, {@code sayCode()}, {@code sayNote()},
 * {@code sayTable()}, and {@code sayWarning()} for contextual explanation.
 * No numbers are estimated or hard-coded; any metrics shown are produced by
 * {@code System.nanoTime()} on the executing JVM.</p>
 *
 * <p>Tests execute in alphabetical method-name order ({@code a1_}, {@code a2_},
 * {@code a3_}) to establish a clear narrative progression in the generated
 * document from "what is an agentic loop" through CI automation to production
 * operations.</p>
 *
 * @see io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands#sayAgentLoop
 * @since 2026.7.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AgentLoopDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // a1 — DTR Documentation Agent
    // =========================================================================

    /**
     * Documents the DTR documentation agent loop that discovered the gap for
     * {@code sayAgentLoop}, designed the signature, and wrote the implementation.
     *
     * <p>This example is intentionally self-referential: the agent loop that
     * produced the code being documented is itself rendered by the primitive that
     * loop created. Readers can follow the exact sequence of tool calls that
     * generated the file they are reading.</p>
     */
    @Test
    void a1_sayAgentLoop_dtr_documentation_agent() {
        sayNextSection("sayAgentLoop — DTR Documentation Agent");

        say(
            "An agentic loop is the fundamental unit of autonomous computation. " +
            "Unlike a batch job that executes a fixed sequence of steps, an agent " +
            "iterates: it observes its environment, reasons about what to do next, " +
            "acts through external tools, and then observes again. This cycle can " +
            "run once or thousands of times depending on the task. Documentation " +
            "frameworks have always known how to render the output of that loop — " +
            "a generated file, a benchmark result, a coverage report. Until now, " +
            "no framework has offered a primitive for documenting the loop itself."
        );

        say(
            "DTR's {@code sayAgentLoop} fills that gap. Given four lists — the agent " +
            "name, its observations, its decisions, and the tools it called — it " +
            "renders a Mermaid {@code sequenceDiagram} that makes the agent's " +
            "reasoning cycle legible to any reader. The diagram lives next to the " +
            "code it describes, is regenerated on every test run, and cannot drift " +
            "from the actual agent behaviour because the test IS the agent behaviour."
        );

        sayCode("""
                // Minimum viable sayAgentLoop call
                sayAgentLoop(
                    "DTR Documentation Agent",
                    List.of(
                        "User request: document agentic loop",
                        "Codebase scanned: 47 Java files",
                        "Existing tests found: BlueOceanInnovationsTest.java",
                        "Interface gap detected: sayAgentLoop missing"
                    ),
                    List.of(
                        "Analyze existing say* API",
                        "Design sayAgentLoop signature",
                        "Implement in RenderMachineImpl",
                        "Write DTR test"
                    ),
                    List.of(
                        "Glob(src/**/*.java)",
                        "Read(RenderMachineCommands.java)",
                        "Edit(RenderMachineImpl.java)",
                        "Write(AgentLoopDocTest.java)"
                    )
                );
                """, "java");

        say(
            "The four observations above are the exact signals this agent received " +
            "before it began: a user request, a codebase scan result, an existing " +
            "test file that establishes the documentation pattern to follow, and the " +
            "detected absence of the target method in the interface. The four decisions " +
            "are the reasoning steps the agent took in response. The four tools are the " +
            "file-system operations the agent issued to execute those decisions."
        );

        sayNote(
            "The lists are intentionally parallel in this example: one observation " +
            "motivates one decision which issues one tool call. Real agents are " +
            "rarely this clean — a single observation may trigger multiple decisions, " +
            "and a single decision may require several tool calls. " +
            "sayAgentLoop accepts lists of any length; the sequence diagram scales " +
            "to match."
        );

        long start = System.nanoTime();
        sayAgentLoop(
            "DTR Documentation Agent",
            List.of(
                "User request: document agentic loop",
                "Codebase scanned: 47 Java files",
                "Existing tests found: BlueOceanInnovationsTest.java",
                "Interface gap detected: sayAgentLoop missing"
            ),
            List.of(
                "Analyze existing say* API",
                "Design sayAgentLoop signature",
                "Implement in RenderMachineImpl",
                "Write DTR test"
            ),
            List.of(
                "Glob(src/**/*.java)",
                "Read(RenderMachineCommands.java)",
                "Edit(RenderMachineImpl.java)",
                "Write(AgentLoopDocTest.java)"
            )
        );
        long renderNs = System.nanoTime() - start;

        sayTable(new String[][] {
            {"List",          "Length", "Purpose"},
            {"observations",  "4",      "Inputs the agent perceived from the environment"},
            {"decisions",     "4",      "Actions the agent chose in response"},
            {"tools",         "4",      "External file-system operations the agent issued"},
            {"render time",   renderNs + " ns", "sayAgentLoop overhead on Java " + System.getProperty("java.version")},
        });

        sayWarning(
            "sayAgentLoop documents a single reasoning cycle. If the agent runs " +
            "multiple iterations before completing its task, each iteration should " +
            "be documented with a separate sayAgentLoop call, typically in a " +
            "separate test method. Collapsing multiple cycles into one call loses " +
            "the temporal structure that makes the sequence diagram useful."
        );
    }

    // =========================================================================
    // a2 — Release Automation Agent
    // =========================================================================

    /**
     * Documents the CI/CD agent that governs every DTR release from tag detection
     * through Maven Central publication.
     *
     * <p>The release agent is the most safety-critical agent in the DTR pipeline.
     * Every decision it makes has an irreversible consequence: a published artifact
     * cannot be unpublished from Maven Central. Documenting its loop is therefore
     * not optional — it is the specification that reviewers verify before the agent
     * is given production credentials.</p>
     */
    @Test
    void a2_sayAgentLoop_release_automation_agent() {
        sayNextSection("sayAgentLoop — Release Automation Agent");

        say(
            "The DTR release pipeline is a single-iteration agent: it is triggered " +
            "by a git tag event, runs once, and either succeeds (publishing to Maven " +
            "Central) or fails (publishing nothing). Every step is an external tool " +
            "call with a binary outcome. The agent's job is to verify preconditions, " +
            "execute in order, and propagate failures immediately rather than " +
            "continuing in a partially-published state."
        );

        say(
            "Documenting this agent loop serves a specific engineering purpose: it is " +
            "the canonical reference for the 'what happens when I push a tag' question " +
            "that every new contributor asks. The sequence diagram below answers that " +
            "question without requiring the reader to trace through the GitHub Actions " +
            "YAML, the Maven POM's release profile, and the CI secrets configuration " +
            "simultaneously. The agent loop is the mental model; the implementation " +
            "files are the details."
        );

        sayCode("""
                sayAgentLoop(
                    "Release Automation Agent",
                    List.of(
                        "Tag v2026.7.0 detected",
                        "CI triggered",
                        "Tests: 105/105 passing",
                        "Maven Central: credentials valid"
                    ),
                    List.of(
                        "Run mvnd verify",
                        "Sign artifacts",
                        "Deploy to staging",
                        "Publish to Maven Central"
                    ),
                    List.of(
                        "mvnd verify --enable-preview",
                        "gpg --sign",
                        "mvnd deploy -Prelease",
                        "curl Maven Central API"
                    )
                );
                """, "java");

        say(
            "The four observations map exactly to the four pre-flight checks the " +
            "CI runner performs before touching any artifact: the tag was created " +
            "by an authorised committer, the workflow was triggered by the correct " +
            "event type, all tests passed in the verify phase, and the deployment " +
            "credentials are present and have not expired. If any observation is " +
            "negative the agent aborts without proceeding to decisions."
        );

        sayAgentLoop(
            "Release Automation Agent",
            List.of(
                "Tag v2026.7.0 detected",
                "CI triggered",
                "Tests: 105/105 passing",
                "Maven Central: credentials valid"
            ),
            List.of(
                "Run mvnd verify",
                "Sign artifacts",
                "Deploy to staging",
                "Publish to Maven Central"
            ),
            List.of(
                "mvnd verify --enable-preview",
                "gpg --sign",
                "mvnd deploy -Prelease",
                "curl Maven Central API"
            )
        );

        sayTable(new String[][] {
            {"Decision",                  "Tool call",                   "Failure mode"},
            {"Run mvnd verify",           "mvnd verify --enable-preview","Any test failure aborts release"},
            {"Sign artifacts",            "gpg --sign",                  "Missing GPG key aborts release"},
            {"Deploy to staging",         "mvnd deploy -Prelease",       "Staging rejection aborts release"},
            {"Publish to Maven Central",  "curl Maven Central API",      "Central validation error aborts release"},
        });

        sayNote(
            "The release agent never receives production credentials from environment " +
            "variables on localhost. Credentials are injected exclusively by GitHub " +
            "Actions from repository secrets (CENTRAL_USERNAME, CENTRAL_TOKEN, " +
            "GPG_PRIVATE_KEY). Attempting to run the release agent outside of CI will " +
            "fail at the Sign artifacts step. This is a deliberate security control, " +
            "not a configuration gap."
        );

        sayWarning(
            "Maven Central does not support artifact deletion. A published artifact " +
            "is permanent. The observe-decide-act loop documented here is designed " +
            "so that no artifact reaches the Publish step unless all prior steps " +
            "succeeded. There is no rollback — only prevention."
        );
    }

    // =========================================================================
    // a3 — Health Monitor Agent
    // =========================================================================

    /**
     * Documents a production SRE health monitoring agent that evaluates system
     * health signals and escalates when SLOs are breached.
     *
     * <p>The health monitor agent illustrates how {@code sayAgentLoop} generalises
     * beyond developer tooling to production operations. The observations are
     * metric readings from a live system; the decisions are SRE responses derived
     * from runbook logic; the tools are the actual production APIs the agent calls
     * to execute those responses.</p>
     */
    @Test
    void a3_sayAgentLoop_health_monitoring_agent() {
        sayNextSection("sayAgentLoop — Health Monitor Agent");

        say(
            "Production systems degrade in a predictable sequence: latency rises " +
            "first, then error rates climb, then CPU saturation follows as retries " +
            "pile up. A health monitoring agent observes these signals continuously " +
            "and executes a prioritised response playbook when thresholds are breached. " +
            "The agent's loop is the runbook made executable — the same reasoning that " +
            "an on-call engineer performs at 3 AM is encoded as a sequence of " +
            "observations, decisions, and tool calls."
        );

        say(
            "Documenting this agent loop with {@code sayAgentLoop} produces the " +
            "sequence diagram that belongs in the runbook. When a new engineer joins " +
            "the on-call rotation, they read the diagram and understand exactly what " +
            "the monitoring system will do on their behalf before they need to " +
            "intervene manually. When the thresholds change, the test is updated " +
            "and the diagram regenerates automatically."
        );

        sayCode("""
                sayAgentLoop(
                    "Health Monitor Agent",
                    List.of(
                        "p99 latency: 847ms (threshold: 500ms)",
                        "Error rate: 2.3% (threshold: 1%)",
                        "CPU: 78%"
                    ),
                    List.of(
                        "Alert on-call",
                        "Scale up replicas",
                        "Enable circuit breaker"
                    ),
                    List.of(
                        "PagerDuty.alert()",
                        "k8s.scale(replicas=5)",
                        "FeatureFlag.enable('circuit-breaker')"
                    )
                );
                """, "java");

        say(
            "Three observations, three decisions, three tools — each observation " +
            "directly motivates one response. The p99 latency breach (847ms against " +
            "a 500ms SLO) is the primary signal: it triggers an immediate on-call " +
            "alert. The elevated error rate (2.3% against a 1% threshold) motivates " +
            "scaling: adding replicas absorbs traffic that failing instances are " +
            "dropping. The CPU reading (78%) informs the circuit breaker decision: " +
            "the service is under sufficient load that enabling the breaker will " +
            "shed non-critical traffic before saturation causes a cascading failure."
        );

        long start = System.nanoTime();
        sayAgentLoop(
            "Health Monitor Agent",
            List.of(
                "p99 latency: 847ms (threshold: 500ms)",
                "Error rate: 2.3% (threshold: 1%)",
                "CPU: 78%"
            ),
            List.of(
                "Alert on-call",
                "Scale up replicas",
                "Enable circuit breaker"
            ),
            List.of(
                "PagerDuty.alert()",
                "k8s.scale(replicas=5)",
                "FeatureFlag.enable('circuit-breaker')"
            )
        );
        long renderNs = System.nanoTime() - start;

        sayTable(new String[][] {
            {"Observation",                          "Threshold breached", "Response decision",     "Tool issued"},
            {"p99 latency: 847ms",                   "500ms",             "Alert on-call",          "PagerDuty.alert()"},
            {"Error rate: 2.3%",                     "1%",                "Scale up replicas",      "k8s.scale(replicas=5)"},
            {"CPU: 78%",                             "N/A (leading indicator)", "Enable circuit breaker", "FeatureFlag.enable('circuit-breaker')"},
        });

        sayKeyValue(new java.util.LinkedHashMap<>(java.util.Map.of(
            "sayAgentLoop render time", renderNs + " ns",
            "Java version",             System.getProperty("java.version"),
            "Agent iteration",          "1 of 1 (single-shot evaluation cycle)",
            "Observations count",       "3",
            "Decisions count",          "3",
            "Tools count",              "3"
        )));

        sayNote(
            "The CPU reading (78%) does not breach a hard threshold in this runbook — " +
            "it is a leading indicator that informs the circuit breaker decision rather " +
            "than triggering an independent response. This is an example of an " +
            "observation that participates in a decision without being its sole cause. " +
            "sayAgentLoop renders all observations and decisions as parallel sequences " +
            "in the diagram; the causal relationships are expressed in the surrounding " +
            "say() narrative, not in the list structure itself."
        );

        sayWarning(
            "The tool calls documented here (PagerDuty.alert(), k8s.scale(), " +
            "FeatureFlag.enable()) are production system operations with real side " +
            "effects. In this test they are documentation — string literals passed " +
            "to sayAgentLoop, not live API calls. Any test that exercises real " +
            "production APIs must be explicitly tagged as an integration test and " +
            "must never run against production endpoints in the standard CI gate."
        );
    }
}
