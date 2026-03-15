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

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Documentation test for the {@code sayAndon} primitive in DTR.
 *
 * <p>{@code sayAndon(String system, List<String> stations, List<String> statuses)}
 * renders a Toyota Andon-board snapshot: a station-to-status table where each row
 * carries a visual indicator (&#x2705; NORMAL / &#x26A0;&#xFE0F; CAUTION /
 * &#x274C; STOPPED) followed by a three-row summary that counts stations in each
 * state. The board is a point-in-time snapshot committed to the documentation
 * record — unlike a live dashboard, it cannot be overwritten by the next deploy.</p>
 *
 * <p>The Toyota Production System Andon cord gave every factory worker the authority
 * to stop the line the moment a defect appeared. In software the equivalent is a
 * shared, always-current status board that makes degraded stations visible to the
 * entire team without requiring a dashboard login. {@code sayAndon} brings that
 * board into the documentation pipeline so that every release carries a verifiable
 * health snapshot alongside the artifact.</p>
 *
 * <p>Three operational contexts are documented here:</p>
 * <ol>
 *   <li><strong>Production microservices</strong> — an eight-station payment platform
 *       with two stations in CAUTION state, illustrating the correlated-failure
 *       risk discussion that accompanies partial degradation.</li>
 *   <li><strong>CI/CD pipeline</strong> — an eight-stage build pipeline where the
 *       final production-deploy station is in STOPPED state, documenting the
 *       intentional Poka-yoke human-approval gate.</li>
 *   <li><strong>Database fleet (SRE view)</strong> — a six-node PostgreSQL fleet
 *       spanning three regions, with one replica removed from the read pool
 *       (STOPPED) and one approaching disk capacity (CAUTION).</li>
 * </ol>
 *
 * <p>All test methods invoke real Java code. Overhead measurements use
 * {@code System.nanoTime()} on the executing JVM. No output values are
 * estimated or hardcoded.</p>
 *
 * @see io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands#sayAndon
 * @since 2026.7.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AndonDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // a1 — Production Microservices Health Board
    // =========================================================================

    /**
     * Documents the Andon board for a payment platform production environment.
     *
     * <p>Two stations are in CAUTION state simultaneously: Payment Processor and
     * Database Replica. The test documents the correlated-failure risk that arises
     * when multiple degraded stations share a downstream dependency, and captures
     * the WIP limit on concurrent investigations recommended by the TPS-derived
     * runbook.</p>
     */
    @Test
    void a1_sayAndon_microservices_health() {
        sayNextSection("a1: sayAndon() — Production Microservices Health Board");

        say(
            "Toyota's Andon cord allowed any worker to stop the production line when " +
            "a defect was found. The critical insight was organisational, not mechanical: " +
            "the cord gave every worker the authority and the obligation to make a " +
            "problem visible before it propagated downstream. In software, the Andon " +
            "board gives the team a shared view of production health at a glance. A " +
            "station in CAUTION is an open defect — visible, owned, and actively being " +
            "addressed. A station in STOPPED is the cord pulled: work at that station " +
            "has halted until the root cause is resolved."
        );

        say(
            "The board below represents the Payment Platform at a single point in time. " +
            "Eight stations span the full request path from the API gateway through to " +
            "the caching layer. Two stations — Payment Processor and Database Replica — " +
            "are in CAUTION state: p99 latency has exceeded the SLA threshold but no " +
            "outage has been declared. On-call has been notified. All other stations " +
            "are operating within their normal envelopes."
        );

        sayCode("""
                sayAndon(
                    "Payment Platform — Production Health Board",
                    List.of(
                        "API Gateway",
                        "Auth Service",
                        "Payment Processor",
                        "Ledger Service",
                        "Notification Service",
                        "Database Primary",
                        "Database Replica",
                        "Redis Cache"
                    ),
                    List.of(
                        "NORMAL",
                        "NORMAL",
                        "CAUTION",
                        "NORMAL",
                        "NORMAL",
                        "NORMAL",
                        "CAUTION",
                        "NORMAL"
                    )
                );
                """, "java");

        List<String> stations = List.of(
            "API Gateway",
            "Auth Service",
            "Payment Processor",
            "Ledger Service",
            "Notification Service",
            "Database Primary",
            "Database Replica",
            "Redis Cache"
        );

        List<String> statuses = List.of(
            "NORMAL",
            "NORMAL",
            "CAUTION",
            "NORMAL",
            "NORMAL",
            "NORMAL",
            "CAUTION",
            "NORMAL"
        );

        long start = System.nanoTime();
        sayAndon("Payment Platform — Production Health Board", stations, statuses);
        long renderNs = System.nanoTime() - start;

        long cautionCount = statuses.stream().filter("CAUTION"::equals).count();
        long stoppedCount = statuses.stream().filter("STOPPED"::equals).count();
        long normalCount  = statuses.stream().filter("NORMAL"::equals).count();

        sayTable(new String[][] {
            {"Station",              "Status",  "Responsibility"},
            {"API Gateway",          "NORMAL",  "Edge ingress, TLS termination, rate limiting"},
            {"Auth Service",         "NORMAL",  "JWT validation, session management"},
            {"Payment Processor",    "CAUTION", "Card authorisation, p99 > SLA — under investigation"},
            {"Ledger Service",       "NORMAL",  "Double-entry bookkeeping, audit trail"},
            {"Notification Service", "NORMAL",  "Async email/SMS dispatch"},
            {"Database Primary",     "NORMAL",  "OLTP writes, leader of replication group"},
            {"Database Replica",     "CAUTION", "Read replica, replication lag rising — under investigation"},
            {"Redis Cache",          "NORMAL",  "Session store, idempotency keys"},
        });

        sayKeyValue(new LinkedHashMap<>() {{
            put("System",              "Payment Platform — Production Health Board");
            put("Total stations",      String.valueOf(stations.size()));
            put("NORMAL",              String.valueOf(normalCount));
            put("CAUTION",             String.valueOf(cautionCount));
            put("STOPPED",             String.valueOf(stoppedCount));
            put("sayAndon() overhead", renderNs + " ns (Java " + System.getProperty("java.version") + ")");
        }});

        sayNote(
            "CAUTION status means degraded performance (p99 > SLA) but no outage. " +
            "On-call has been notified and is investigating. Traffic is still being " +
            "served; no customer-facing error rate increase has been observed."
        );

        sayWarning(
            "Two stations in CAUTION state simultaneously increases correlated failure " +
            "risk. Payment Processor and Database Replica share a network path: if the " +
            "replica falls further behind it may be removed from the read pool, which " +
            "increases load on the primary, which may surface as Payment Processor " +
            "timeouts. WIP limit on investigations: max 2 concurrent."
        );
    }

    // =========================================================================
    // a2 — CI/CD Pipeline Station Board
    // =========================================================================

    /**
     * Documents the Andon board for the DTR CI/CD build pipeline.
     *
     * <p>All eight pipeline stations are green except the final production-deploy
     * station, which is in STOPPED state. The stop is intentional — a human
     * approval gate (Poka-yoke) prevents automated deployment to production. The
     * board documents the distinction between STOPPED-as-failure and
     * STOPPED-as-intentional-gate, which is a critical operational concept for
     * teams adopting continuous delivery practices.</p>
     */
    @Test
    void a2_sayAndon_ci_cd_pipeline() {
        sayNextSection("a2: sayAndon() — CI/CD Pipeline Station Board");

        say(
            "A CI/CD pipeline is a production line in the TPS sense: raw material " +
            "(a git commit) enters at one end and a shippable artifact exits at the " +
            "other. Each pipeline stage is a workstation with a defined input, a " +
            "defined output, and a clear pass/fail criterion. The Andon board for a " +
            "pipeline makes the state of every station visible to the whole team at a " +
            "glance. When a station goes STOPPED, the Andon cord has been pulled — " +
            "work stops at that point and upstream stations continue only to their " +
            "natural buffer."
        );

        say(
            "The board below represents the DTR CI/CD pipeline during an active " +
            "release cycle. Seven upstream stations — from unit tests through smoke " +
            "tests — are all NORMAL. The eighth station, Production Deploy, is " +
            "STOPPED. This is not a failure. It is the Poka-yoke human-approval gate: " +
            "no automated system is permitted to deploy to production without an " +
            "explicit human decision. The pipeline is healthy; the cord has been " +
            "pulled deliberately and is waiting to be released."
        );

        sayCode("""
                sayAndon(
                    "DTR CI/CD Pipeline — Build Station Board",
                    List.of(
                        "Unit Tests",
                        "Integration Tests",
                        "Static Analysis",
                        "Security Scan",
                        "Artifact Build",
                        "Staging Deploy",
                        "Smoke Tests",
                        "Production Deploy"
                    ),
                    List.of(
                        "NORMAL",
                        "NORMAL",
                        "NORMAL",
                        "NORMAL",
                        "NORMAL",
                        "NORMAL",
                        "NORMAL",
                        "STOPPED"
                    )
                );
                """, "java");

        List<String> stations = List.of(
            "Unit Tests",
            "Integration Tests",
            "Static Analysis",
            "Security Scan",
            "Artifact Build",
            "Staging Deploy",
            "Smoke Tests",
            "Production Deploy"
        );

        List<String> statuses = List.of(
            "NORMAL",
            "NORMAL",
            "NORMAL",
            "NORMAL",
            "NORMAL",
            "NORMAL",
            "NORMAL",
            "STOPPED"
        );

        // Measure over multiple iterations to report a stable average
        final int ITERATIONS = 10;
        long total = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long t0 = System.nanoTime();
            sayAndon("DTR CI/CD Pipeline — Build Station Board", stations, statuses);
            total += System.nanoTime() - t0;
        }
        long avgNs = total / ITERATIONS;

        long cautionCount = statuses.stream().filter("CAUTION"::equals).count();
        long stoppedCount = statuses.stream().filter("STOPPED"::equals).count();
        long normalCount  = statuses.stream().filter("NORMAL"::equals).count();

        sayTable(new String[][] {
            {"Station",           "Status",  "Gate type",          "Automation"},
            {"Unit Tests",        "NORMAL",  "Auto pass/fail",     "mvnd test --enable-preview"},
            {"Integration Tests", "NORMAL",  "Auto pass/fail",     "mvnd verify -Pintegration"},
            {"Static Analysis",   "NORMAL",  "Auto pass/fail",     "Checkstyle + SpotBugs"},
            {"Security Scan",     "NORMAL",  "Auto pass/fail",     "OWASP dependency-check"},
            {"Artifact Build",    "NORMAL",  "Auto pass/fail",     "mvnd package -Prelease"},
            {"Staging Deploy",    "NORMAL",  "Auto pass/fail",     "GitHub Actions deploy job"},
            {"Smoke Tests",       "NORMAL",  "Auto pass/fail",     "curl health endpoint"},
            {"Production Deploy", "STOPPED", "Human approval gate","Manual: gh workflow run"},
        });

        sayKeyValue(new LinkedHashMap<>() {{
            put("System",                       "DTR CI/CD Pipeline — Build Station Board");
            put("Total stations",               String.valueOf(stations.size()));
            put("NORMAL",                       String.valueOf(normalCount));
            put("CAUTION",                      String.valueOf(cautionCount));
            put("STOPPED",                      String.valueOf(stoppedCount));
            put("sayAndon() avg overhead",
                avgNs + " ns avg (" + ITERATIONS + " iterations, Java " +
                System.getProperty("java.version") + ")");
        }});

        sayNote(
            "STOPPED does not mean FAILED. The Production Deploy station is waiting " +
            "for explicit human approval. This is an intentional Poka-yoke: no automated " +
            "deployment to production without a human pull on the cord. The station will " +
            "return to NORMAL the moment a release engineer issues the approval via " +
            "'gh workflow run deploy.yml'."
        );

        sayWarning(
            "A pipeline board that never shows STOPPED for Production Deploy is a " +
            "warning sign, not a sign of health. It means either the human approval " +
            "gate has been removed (a policy violation) or no releases have been " +
            "attempted in the reporting window. Both states warrant investigation."
        );
    }

    // =========================================================================
    // a3 — Database Fleet Status (SRE Operational View)
    // =========================================================================

    /**
     * Documents the Andon board for a globally distributed PostgreSQL fleet.
     *
     * <p>Six nodes span three regions. One replica has been removed from the read
     * pool due to replication lag (STOPPED). One replica in EU West is approaching
     * disk capacity (CAUTION) with evacuation in progress. The board gives the
     * on-call SRE a single-glance fleet summary that replaces the need to log into
     * each region's monitoring console.</p>
     */
    @Test
    void a3_sayAndon_database_fleet() {
        sayNextSection("a3: sayAndon() — PostgreSQL Fleet Operational Status (SRE View)");

        say(
            "Database replication topologies are difficult to reason about when the " +
            "state of each node lives in a separate monitoring console. An SRE " +
            "responding to an incident at 02:00 UTC should be able to see the entire " +
            "fleet state in one view before deciding which runbook to execute. The " +
            "Andon board for a database fleet plays the same role that the assembly-line " +
            "board plays in a factory: it shows the state of every workstation, makes " +
            "degraded nodes visually distinct from healthy ones, and ensures that " +
            "no node is invisible during a high-stress response."
        );

        say(
            "The fleet below spans three AWS regions: US East, EU West, and AP South. " +
            "Six nodes are tracked: one primary and one or two replicas per region. " +
            "db-replica-us-east-2 is in STOPPED state: replication lag exceeded 60 " +
            "seconds, triggering the automatic read-pool removal runbook. The replica " +
            "is healthy at the Postgres level but is no longer serving read traffic. " +
            "db-replica-eu-west-1 is in CAUTION state: disk utilisation has reached " +
            "85% and the evacuation playbook is active."
        );

        sayCode("""
                sayAndon(
                    "PostgreSQL Fleet — 2026-Q1 Operational Status",
                    List.of(
                        "db-primary-us-east",
                        "db-replica-us-east-1",
                        "db-replica-us-east-2",
                        "db-primary-eu-west",
                        "db-replica-eu-west-1",
                        "db-primary-ap-south"
                    ),
                    List.of(
                        "NORMAL",
                        "NORMAL",
                        "STOPPED",
                        "NORMAL",
                        "CAUTION",
                        "NORMAL"
                    )
                );
                """, "java");

        List<String> stations = List.of(
            "db-primary-us-east",
            "db-replica-us-east-1",
            "db-replica-us-east-2",
            "db-primary-eu-west",
            "db-replica-eu-west-1",
            "db-primary-ap-south"
        );

        List<String> statuses = List.of(
            "NORMAL",
            "NORMAL",
            "STOPPED",
            "NORMAL",
            "CAUTION",
            "NORMAL"
        );

        long start = System.nanoTime();
        sayAndon("PostgreSQL Fleet — 2026-Q1 Operational Status", stations, statuses);
        long renderNs = System.nanoTime() - start;

        long cautionCount = statuses.stream().filter("CAUTION"::equals).count();
        long stoppedCount = statuses.stream().filter("STOPPED"::equals).count();
        long normalCount  = statuses.stream().filter("NORMAL"::equals).count();

        sayTable(new String[][] {
            {"Node",                  "Region",   "Role",    "Status",  "Incident"},
            {"db-primary-us-east",    "us-east-1","Primary", "NORMAL",  "None"},
            {"db-replica-us-east-1",  "us-east-1","Replica", "NORMAL",  "None"},
            {"db-replica-us-east-2",  "us-east-1","Replica", "STOPPED", "Replication lag > 60s — removed from read pool"},
            {"db-primary-eu-west",    "eu-west-1","Primary", "NORMAL",  "None"},
            {"db-replica-eu-west-1",  "eu-west-1","Replica", "CAUTION", "Disk 85% full — evacuation in progress"},
            {"db-primary-ap-south",   "ap-south-1","Primary","NORMAL",  "None"},
        });

        sayKeyValue(new LinkedHashMap<>() {{
            put("System",              "PostgreSQL Fleet — 2026-Q1 Operational Status");
            put("Total nodes",         String.valueOf(stations.size()));
            put("NORMAL",              String.valueOf(normalCount));
            put("CAUTION",             String.valueOf(cautionCount));
            put("STOPPED",             String.valueOf(stoppedCount));
            put("Read-pool capacity",  "Reduced: us-east-2 replica offline");
            put("Disk evacuation",     "Active: eu-west-1 replica");
            put("sayAndon() overhead", renderNs + " ns (Java " + System.getProperty("java.version") + ")");
        }});

        sayNote(
            "db-replica-us-east-2 STOPPED: the node is alive and replicating — it " +
            "was removed from the application read pool because lag exceeded the " +
            "60-second SLA. Reads that were routed to this replica are now served " +
            "entirely by db-replica-us-east-1 and db-primary-us-east. The primary " +
            "is absorbing additional read load until the replica catches up and is " +
            "reinstated by the replication-lag runbook."
        );

        sayWarning(
            "db-replica-eu-west-1 CAUTION: disk at 85% full, evacuation in progress. " +
            "If disk reaches 95% before evacuation completes, Postgres will enter " +
            "read-only mode on that node. At that point the node must be escalated " +
            "to STOPPED and the EU West read pool will run on the primary alone. " +
            "Target evacuation completion: within 4 hours of CAUTION declaration."
        );
    }
}
