# io.github.seanchatmangpt.dtr.test.AndonDocTest

## Table of Contents

- [a1: sayAndon() — Production Microservices Health Board](#a1sayandonproductionmicroserviceshealthboard)
- [a2: sayAndon() — CI/CD Pipeline Station Board](#a2sayandoncicdpipelinestationboard)
- [a3: sayAndon() — PostgreSQL Fleet Operational Status (SRE View)](#a3sayandonpostgresqlfleetoperationalstatussreview)


## a1: sayAndon() — Production Microservices Health Board

Toyota's Andon cord allowed any worker to stop the production line when a defect was found. The critical insight was organisational, not mechanical: the cord gave every worker the authority and the obligation to make a problem visible before it propagated downstream. In software, the Andon board gives the team a shared view of production health at a glance. A station in CAUTION is an open defect — visible, owned, and actively being addressed. A station in STOPPED is the cord pulled: work at that station has halted until the root cause is resolved.

The board below represents the Payment Platform at a single point in time. Eight stations span the full request path from the API gateway through to the caching layer. Two stations — Payment Processor and Database Replica — are in CAUTION state: p99 latency has exceeded the SLA threshold but no outage has been declared. On-call has been notified. All other stations are operating within their normal envelopes.

```java
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
```

### Andon Board: Payment Platform — Production Health Board

| Station | Status |
| --- | --- |
| API Gateway | ✅ NORMAL |
| Auth Service | ✅ NORMAL |
| Payment Processor | ⚠️ CAUTION |
| Ledger Service | ✅ NORMAL |
| Notification Service | ✅ NORMAL |
| Database Primary | ✅ NORMAL |
| Database Replica | ⚠️ CAUTION |
| Redis Cache | ✅ NORMAL |

| Status | Count |
| --- | --- |
| ✅ Normal | `6` |
| ⚠️ Caution | `2` |
| ❌ Stopped | `0` |

| Station | Status | Responsibility |
| --- | --- | --- |
| API Gateway | NORMAL | Edge ingress, TLS termination, rate limiting |
| Auth Service | NORMAL | JWT validation, session management |
| Payment Processor | CAUTION | Card authorisation, p99 > SLA — under investigation |
| Ledger Service | NORMAL | Double-entry bookkeeping, audit trail |
| Notification Service | NORMAL | Async email/SMS dispatch |
| Database Primary | NORMAL | OLTP writes, leader of replication group |
| Database Replica | CAUTION | Read replica, replication lag rising — under investigation |
| Redis Cache | NORMAL | Session store, idempotency keys |

| Key | Value |
| --- | --- |
| `System` | `Payment Platform — Production Health Board` |
| `Total stations` | `8` |
| `NORMAL` | `6` |
| `CAUTION` | `2` |
| `STOPPED` | `0` |
| `sayAndon() overhead` | `1609887 ns (Java 25.0.2)` |

> [!NOTE]
> CAUTION status means degraded performance (p99 > SLA) but no outage. On-call has been notified and is investigating. Traffic is still being served; no customer-facing error rate increase has been observed.

> [!WARNING]
> Two stations in CAUTION state simultaneously increases correlated failure risk. Payment Processor and Database Replica share a network path: if the replica falls further behind it may be removed from the read pool, which increases load on the primary, which may surface as Payment Processor timeouts. WIP limit on investigations: max 2 concurrent.

## a2: sayAndon() — CI/CD Pipeline Station Board

A CI/CD pipeline is a production line in the TPS sense: raw material (a git commit) enters at one end and a shippable artifact exits at the other. Each pipeline stage is a workstation with a defined input, a defined output, and a clear pass/fail criterion. The Andon board for a pipeline makes the state of every station visible to the whole team at a glance. When a station goes STOPPED, the Andon cord has been pulled — work stops at that point and upstream stations continue only to their natural buffer.

The board below represents the DTR CI/CD pipeline during an active release cycle. Seven upstream stations — from unit tests through smoke tests — are all NORMAL. The eighth station, Production Deploy, is STOPPED. This is not a failure. It is the Poka-yoke human-approval gate: no automated system is permitted to deploy to production without an explicit human decision. The pipeline is healthy; the cord has been pulled deliberately and is waiting to be released.

```java
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
```

### Andon Board: DTR CI/CD Pipeline — Build Station Board

| Station | Status |
| --- | --- |
| Unit Tests | ✅ NORMAL |
| Integration Tests | ✅ NORMAL |
| Static Analysis | ✅ NORMAL |
| Security Scan | ✅ NORMAL |
| Artifact Build | ✅ NORMAL |
| Staging Deploy | ✅ NORMAL |
| Smoke Tests | ✅ NORMAL |
| Production Deploy | ❌ STOPPED |

| Status | Count |
| --- | --- |
| ✅ Normal | `7` |
| ⚠️ Caution | `0` |
| ❌ Stopped | `1` |

### Andon Board: DTR CI/CD Pipeline — Build Station Board

| Station | Status |
| --- | --- |
| Unit Tests | ✅ NORMAL |
| Integration Tests | ✅ NORMAL |
| Static Analysis | ✅ NORMAL |
| Security Scan | ✅ NORMAL |
| Artifact Build | ✅ NORMAL |
| Staging Deploy | ✅ NORMAL |
| Smoke Tests | ✅ NORMAL |
| Production Deploy | ❌ STOPPED |

| Status | Count |
| --- | --- |
| ✅ Normal | `7` |
| ⚠️ Caution | `0` |
| ❌ Stopped | `1` |

### Andon Board: DTR CI/CD Pipeline — Build Station Board

| Station | Status |
| --- | --- |
| Unit Tests | ✅ NORMAL |
| Integration Tests | ✅ NORMAL |
| Static Analysis | ✅ NORMAL |
| Security Scan | ✅ NORMAL |
| Artifact Build | ✅ NORMAL |
| Staging Deploy | ✅ NORMAL |
| Smoke Tests | ✅ NORMAL |
| Production Deploy | ❌ STOPPED |

| Status | Count |
| --- | --- |
| ✅ Normal | `7` |
| ⚠️ Caution | `0` |
| ❌ Stopped | `1` |

### Andon Board: DTR CI/CD Pipeline — Build Station Board

| Station | Status |
| --- | --- |
| Unit Tests | ✅ NORMAL |
| Integration Tests | ✅ NORMAL |
| Static Analysis | ✅ NORMAL |
| Security Scan | ✅ NORMAL |
| Artifact Build | ✅ NORMAL |
| Staging Deploy | ✅ NORMAL |
| Smoke Tests | ✅ NORMAL |
| Production Deploy | ❌ STOPPED |

| Status | Count |
| --- | --- |
| ✅ Normal | `7` |
| ⚠️ Caution | `0` |
| ❌ Stopped | `1` |

### Andon Board: DTR CI/CD Pipeline — Build Station Board

| Station | Status |
| --- | --- |
| Unit Tests | ✅ NORMAL |
| Integration Tests | ✅ NORMAL |
| Static Analysis | ✅ NORMAL |
| Security Scan | ✅ NORMAL |
| Artifact Build | ✅ NORMAL |
| Staging Deploy | ✅ NORMAL |
| Smoke Tests | ✅ NORMAL |
| Production Deploy | ❌ STOPPED |

| Status | Count |
| --- | --- |
| ✅ Normal | `7` |
| ⚠️ Caution | `0` |
| ❌ Stopped | `1` |

### Andon Board: DTR CI/CD Pipeline — Build Station Board

| Station | Status |
| --- | --- |
| Unit Tests | ✅ NORMAL |
| Integration Tests | ✅ NORMAL |
| Static Analysis | ✅ NORMAL |
| Security Scan | ✅ NORMAL |
| Artifact Build | ✅ NORMAL |
| Staging Deploy | ✅ NORMAL |
| Smoke Tests | ✅ NORMAL |
| Production Deploy | ❌ STOPPED |

| Status | Count |
| --- | --- |
| ✅ Normal | `7` |
| ⚠️ Caution | `0` |
| ❌ Stopped | `1` |

### Andon Board: DTR CI/CD Pipeline — Build Station Board

| Station | Status |
| --- | --- |
| Unit Tests | ✅ NORMAL |
| Integration Tests | ✅ NORMAL |
| Static Analysis | ✅ NORMAL |
| Security Scan | ✅ NORMAL |
| Artifact Build | ✅ NORMAL |
| Staging Deploy | ✅ NORMAL |
| Smoke Tests | ✅ NORMAL |
| Production Deploy | ❌ STOPPED |

| Status | Count |
| --- | --- |
| ✅ Normal | `7` |
| ⚠️ Caution | `0` |
| ❌ Stopped | `1` |

### Andon Board: DTR CI/CD Pipeline — Build Station Board

| Station | Status |
| --- | --- |
| Unit Tests | ✅ NORMAL |
| Integration Tests | ✅ NORMAL |
| Static Analysis | ✅ NORMAL |
| Security Scan | ✅ NORMAL |
| Artifact Build | ✅ NORMAL |
| Staging Deploy | ✅ NORMAL |
| Smoke Tests | ✅ NORMAL |
| Production Deploy | ❌ STOPPED |

| Status | Count |
| --- | --- |
| ✅ Normal | `7` |
| ⚠️ Caution | `0` |
| ❌ Stopped | `1` |

### Andon Board: DTR CI/CD Pipeline — Build Station Board

| Station | Status |
| --- | --- |
| Unit Tests | ✅ NORMAL |
| Integration Tests | ✅ NORMAL |
| Static Analysis | ✅ NORMAL |
| Security Scan | ✅ NORMAL |
| Artifact Build | ✅ NORMAL |
| Staging Deploy | ✅ NORMAL |
| Smoke Tests | ✅ NORMAL |
| Production Deploy | ❌ STOPPED |

| Status | Count |
| --- | --- |
| ✅ Normal | `7` |
| ⚠️ Caution | `0` |
| ❌ Stopped | `1` |

### Andon Board: DTR CI/CD Pipeline — Build Station Board

| Station | Status |
| --- | --- |
| Unit Tests | ✅ NORMAL |
| Integration Tests | ✅ NORMAL |
| Static Analysis | ✅ NORMAL |
| Security Scan | ✅ NORMAL |
| Artifact Build | ✅ NORMAL |
| Staging Deploy | ✅ NORMAL |
| Smoke Tests | ✅ NORMAL |
| Production Deploy | ❌ STOPPED |

| Status | Count |
| --- | --- |
| ✅ Normal | `7` |
| ⚠️ Caution | `0` |
| ❌ Stopped | `1` |

| Station | Status | Gate type | Automation |
| --- | --- | --- | --- |
| Unit Tests | NORMAL | Auto pass/fail | mvnd test --enable-preview |
| Integration Tests | NORMAL | Auto pass/fail | mvnd verify -Pintegration |
| Static Analysis | NORMAL | Auto pass/fail | Checkstyle + SpotBugs |
| Security Scan | NORMAL | Auto pass/fail | OWASP dependency-check |
| Artifact Build | NORMAL | Auto pass/fail | mvnd package -Prelease |
| Staging Deploy | NORMAL | Auto pass/fail | GitHub Actions deploy job |
| Smoke Tests | NORMAL | Auto pass/fail | curl health endpoint |
| Production Deploy | STOPPED | Human approval gate | Manual: gh workflow run |

| Key | Value |
| --- | --- |
| `System` | `DTR CI/CD Pipeline — Build Station Board` |
| `Total stations` | `8` |
| `NORMAL` | `7` |
| `CAUTION` | `0` |
| `STOPPED` | `1` |
| `sayAndon() avg overhead` | `35900 ns avg (10 iterations, Java 25.0.2)` |

> [!NOTE]
> STOPPED does not mean FAILED. The Production Deploy station is waiting for explicit human approval. This is an intentional Poka-yoke: no automated deployment to production without a human pull on the cord. The station will return to NORMAL the moment a release engineer issues the approval via 'gh workflow run deploy.yml'.

> [!WARNING]
> A pipeline board that never shows STOPPED for Production Deploy is a warning sign, not a sign of health. It means either the human approval gate has been removed (a policy violation) or no releases have been attempted in the reporting window. Both states warrant investigation.

## a3: sayAndon() — PostgreSQL Fleet Operational Status (SRE View)

Database replication topologies are difficult to reason about when the state of each node lives in a separate monitoring console. An SRE responding to an incident at 02:00 UTC should be able to see the entire fleet state in one view before deciding which runbook to execute. The Andon board for a database fleet plays the same role that the assembly-line board plays in a factory: it shows the state of every workstation, makes degraded nodes visually distinct from healthy ones, and ensures that no node is invisible during a high-stress response.

The fleet below spans three AWS regions: US East, EU West, and AP South. Six nodes are tracked: one primary and one or two replicas per region. db-replica-us-east-2 is in STOPPED state: replication lag exceeded 60 seconds, triggering the automatic read-pool removal runbook. The replica is healthy at the Postgres level but is no longer serving read traffic. db-replica-eu-west-1 is in CAUTION state: disk utilisation has reached 85% and the evacuation playbook is active.

```java
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
```

### Andon Board: PostgreSQL Fleet — 2026-Q1 Operational Status

| Station | Status |
| --- | --- |
| db-primary-us-east | ✅ NORMAL |
| db-replica-us-east-1 | ✅ NORMAL |
| db-replica-us-east-2 | ❌ STOPPED |
| db-primary-eu-west | ✅ NORMAL |
| db-replica-eu-west-1 | ⚠️ CAUTION |
| db-primary-ap-south | ✅ NORMAL |

| Status | Count |
| --- | --- |
| ✅ Normal | `4` |
| ⚠️ Caution | `1` |
| ❌ Stopped | `1` |

| Node | Region | Role | Status | Incident |
| --- | --- | --- | --- | --- |
| db-primary-us-east | us-east-1 | Primary | NORMAL | None |
| db-replica-us-east-1 | us-east-1 | Replica | NORMAL | None |
| db-replica-us-east-2 | us-east-1 | Replica | STOPPED | Replication lag > 60s — removed from read pool |
| db-primary-eu-west | eu-west-1 | Primary | NORMAL | None |
| db-replica-eu-west-1 | eu-west-1 | Replica | CAUTION | Disk 85% full — evacuation in progress |
| db-primary-ap-south | ap-south-1 | Primary | NORMAL | None |

| Key | Value |
| --- | --- |
| `System` | `PostgreSQL Fleet — 2026-Q1 Operational Status` |
| `Total nodes` | `6` |
| `NORMAL` | `4` |
| `CAUTION` | `1` |
| `STOPPED` | `1` |
| `Read-pool capacity` | `Reduced: us-east-2 replica offline` |
| `Disk evacuation` | `Active: eu-west-1 replica` |
| `sayAndon() overhead` | `62941 ns (Java 25.0.2)` |

> [!NOTE]
> db-replica-us-east-2 STOPPED: the node is alive and replicating — it was removed from the application read pool because lag exceeded the 60-second SLA. Reads that were routed to this replica are now served entirely by db-replica-us-east-1 and db-primary-us-east. The primary is absorbing additional read load until the replica catches up and is reinstated by the replication-lag runbook.

> [!WARNING]
> db-replica-eu-west-1 CAUTION: disk at 85% full, evacuation in progress. If disk reaches 95% before evacuation completes, Postgres will enter read-only mode on that node. At that point the node must be escalated to STOPPED and the EU West read pool will run on the primary alone. Target evacuation completion: within 4 hours of CAUTION declaration.

---
*Generated by [DTR](http://www.dtr.org)*
