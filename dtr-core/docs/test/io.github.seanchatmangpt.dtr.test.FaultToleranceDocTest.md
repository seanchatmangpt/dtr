# io.github.seanchatmangpt.dtr.test.FaultToleranceDocTest

## Table of Contents

- [sayFaultTolerance — PostgreSQL Connection Pool Exhaustion](#sayfaulttolerancepostgresqlconnectionpoolexhaustion)
- [sayFaultTolerance — Payment Gateway Cascade Failure](#sayfaulttolerancepaymentgatewaycascadefailure)
- [sayFaultTolerance — DTR Render Pipeline Failures](#sayfaulttolerancedtrrenderpipelinefailures)


## sayFaultTolerance — PostgreSQL Connection Pool Exhaustion

Joe Armstrong wrote: 'A supervisor is just a process whose only job is to start and restart workers. Simple processes with a single responsibility.' That sentence contains the entire design philosophy of fault-tolerant systems. The supervisor does not try to handle every failure mode itself. It delegates recovery to the appropriate worker restart policy and trusts that a fresh worker, given a clean environment, will succeed where the crashed worker failed.

PostgreSQL connection pool exhaustion is the canonical database failure mode for Java services under load. It is not a single failure — it is a family of four distinct failure signatures, each with a different root cause and a different optimal supervisor response. Treating all four as 'connection failed, retry' is the error that turns a recoverable incident into a full outage. {@code sayFaultTolerance} makes each failure-to-recovery mapping explicit, so the team can verify the strategy before 3 AM.

```java
// Document each distinct failure and its supervisor recovery action.
// Lists are parallel: failures.get(i) is recovered by recoveries.get(i).
sayFaultTolerance(
    "PostgreSQL Connection Pool Exhaustion",
    List.of(
        "Connection timeout (pool exhausted)",
        "TCP connection reset by peer",
        "SSL handshake failed",
        "Max connections reached (100/100)"
    ),
    List.of(
        "Supervisor kills worker, restarts with backoff: 1s, 2s, 4s",
        "One-for-one restart, new TCP socket allocated",
        "Restart with fresh SSL context",
        "Supervisor evicts idle connections, worker restarts"
    )
);
```

Four failure signatures, four recovery actions. The exponential backoff on pool exhaustion (1 s, 2 s, 4 s) prevents the thundering-herd problem that would otherwise result from every waiting thread retrying simultaneously. The TCP reset recovery allocates a fresh socket rather than attempting to rehabilitate the broken one — consistent with the 'let it crash' philosophy of discarding corrupt state rather than repairing it. The SSL context is similarly discarded and recreated: SSL state machines are complex enough that partial-failure repair is error-prone. The pool-saturation response evicts idle connections first to create capacity before restarting the blocked worker.

> [!NOTE]
> The 'one-for-one' restart strategy documented here is an Erlang/OTP term: only the failed worker is restarted, not all workers in the supervision tree. This is correct for independent connection workers. If workers share mutable state (e.g. a shared counter), a 'one-for-all' strategy may be required — add a note to the recovery string to make the dependency explicit.

### Fault Tolerance: PostgreSQL Connection Pool Exhaustion

| # | Failure | Supervisor Recovery |
| --- | --- | --- |
| 1 | Connection timeout (pool exhausted) | Supervisor kills worker, restarts with backoff: 1s, 2s, 4s |
| 2 | TCP connection reset by peer | One-for-one restart, new TCP socket allocated |
| 3 | SSL handshake failed | Restart with fresh SSL context |
| 4 | Max connections reached (100/100) | Supervisor evicts idle connections, worker restarts |

| Metric | Value |
| --- | --- |
| Failures documented | `4` |
| Recovery actions | `4` |
| Recovery coverage | `100%` |

| Failure signature | Root cause | Recovery class |
| --- | --- | --- |
| Connection timeout (pool exhausted) | All pool slots in use | Exponential backoff restart |
| TCP connection reset by peer | Network or server-side close | Socket-level one-for-one restart |
| SSL handshake failed | Certificate or cipher mismatch | Fresh SSL context restart |
| Max connections reached (100/100) | Pool hard limit saturated | Evict idle, then restart worker |

| Key | Value |
| --- | --- |
| `Failures documented` | `4` |
| `Recovery coverage` | `100%` |
| `sayFaultTolerance render time` | `1891362 ns` |
| `Java version` | `25.0.2` |
| `Recovery actions` | `4` |

> [!WARNING]
> The backoff ceiling of 4 s documented here is a starting point, not a universal recommendation. Services with low traffic and a long-lived connection pool may tolerate a longer ceiling (e.g. 30 s) to reduce reconnect storms. High-throughput services may require jitter in addition to exponential backoff. Validate the ceiling against observed pool recovery times in your production environment before hardening this runbook.

| Check | Result |
| --- | --- |
| Failure list length matches recovery list length (4 == 4) | `✓ PASS` |
| sayFaultTolerance renders without throwing | `✓ PASS` |
| Exponential backoff documented for pool exhaustion | `✓ PASS` |
| Recovery coverage computed as 100% | `✓ PASS` |
| SSL context discarded on handshake failure | `✓ PASS` |

## sayFaultTolerance — Payment Gateway Cascade Failure

Payment systems sit at the intersection of three hard constraints: latency (users abandon checkouts after seconds), correctness (double charges are a regulatory and reputational catastrophe), and availability (downtime is immediate, measurable revenue loss). A payment gateway failure is therefore never just a technical event — it is a business event with a specific category and a specific required response.

The four failure modes below are representative of the failure taxonomy that every payment integration team encounters in production. They are not hypothetical. Stripe API timeouts occur during their maintenance windows and under load spikes. Rate limit 429s occur in any high-volume merchant integration. Card declines are a normal business outcome, not a system error, and routing them through the supervisor restart machinery is a common and costly mistake. Network partitions occur whenever the gateway's CDN or DNS has an incident. Each failure demands a different response: the supervisor must distinguish among them.

```java
sayFaultTolerance(
    "Payment Gateway Cascade Failure",
    List.of(
        "Stripe API timeout (30s)",
        "Rate limit exceeded (429)",
        "Invalid card declined",
        "Network partition: gateway unreachable"
    ),
    List.of(
        "Circuit breaker opens, fallback to Braintree gateway",
        "Exponential backoff with jitter: retry after 60s",
        "Return {error, card_declined} to caller — no restart needed",
        "Supervisor activates offline queue, drains on reconnect"
    )
);
```

The circuit breaker on the Stripe timeout is the key architectural decision documented here. Rather than retrying Stripe indefinitely, the supervisor opens the circuit and routes traffic to the Braintree fallback. This prevents a single gateway's latency from becoming the caller's latency. The 429 rate-limit response uses jitter in addition to exponential backoff to avoid the synchronised retry storm that occurs when many callers receive a 429 simultaneously and all back off for exactly the same duration.

The card decline case is the most instructive: it is not a system fault at all. The gateway processed the request successfully and returned a well-formed business error. The correct response is to surface that error to the caller — not to restart the payment worker, not to retry, not to log a WARN. Restarting on business errors is the root cause of double-charge incidents. {@code sayFaultTolerance} documents 'no restart needed' explicitly so that the correct behaviour is part of the team's shared specification.

### Fault Tolerance: Payment Gateway Cascade Failure

| # | Failure | Supervisor Recovery |
| --- | --- | --- |
| 1 | Stripe API timeout (30s) | Circuit breaker opens, fallback to Braintree gateway |
| 2 | Rate limit exceeded (429) | Exponential backoff with jitter: retry after 60s |
| 3 | Invalid card declined | Return {error, card_declined} to caller — no restart needed |
| 4 | Network partition: gateway unreachable | Supervisor activates offline queue, drains on reconnect |

| Metric | Value |
| --- | --- |
| Failures documented | `4` |
| Recovery actions | `4` |
| Recovery coverage | `100%` |

| Failure | HTTP status / signal | Recovery strategy | Restart? |
| --- | --- | --- | --- |
| Stripe API timeout (30s) | socket timeout | Circuit breaker + fallback | Yes — on Braintree |
| Rate limit exceeded (429) | HTTP 429 | Backoff with jitter | Yes — after 60s delay |
| Invalid card declined | HTTP 402 | Return error to caller | No |
| Network partition: gateway unreachable | connection refused | Offline queue + drain | Yes — deferred |

| Key | Value |
| --- | --- |
| `Failures documented` | `4` |
| `Recovery coverage` | `100%` |
| `sayFaultTolerance render time` | `132841 ns` |
| `Java version` | `25.0.2` |
| `Recovery actions` | `4` |

> [!NOTE]
> The offline queue recovery for network partition depends on idempotent payment requests. Each queued payment must carry a unique idempotency key so that draining the queue after reconnect cannot produce a duplicate charge. Stripe and Braintree both support idempotency keys natively. If your payment gateway does not, the offline queue strategy is unsafe and should be replaced with synchronous retry once the partition heals.

> [!WARNING]
> The circuit breaker threshold (one 30s timeout → open) is aggressive for illustration. Production thresholds are typically configured as N failures within a rolling window (e.g. 5 failures in 60s). Setting the threshold too low causes the breaker to open on transient noise; too high and it fails to protect the system during genuine gateway degradation. Instrument and tune against real gateway SLA data.

| Check | Result |
| --- | --- |
| Failure list length matches recovery list length (4 == 4) | `✓ PASS` |
| sayFaultTolerance renders without throwing | `✓ PASS` |
| Recovery coverage computed as 100% | `✓ PASS` |
| Circuit breaker fallback documented for timeout | `✓ PASS` |
| Card decline documented as no-restart path | `✓ PASS` |

## sayFaultTolerance — DTR Render Pipeline Failures

DTR documents its own fault tolerance using {@code sayFaultTolerance}. This is not a contrived self-referential example — the render pipeline runs on CI hardware with constrained resources, writes output to a filesystem shared with the build, and processes benchmarks that can generate large intermediate data structures. The three failure modes below are production failure signatures observed in the DTR CI pipeline, not hypothetical scenarios.

The render pipeline is structured as a supervision tree. Each renderer (Markdown, LaTeX, HTML, JSON) is an independent worker. The FileWriter that flushes each renderer's output to disk is a separate worker supervised by the same tree. If the FileWriter fails due to disk saturation, only the FileWriter is restarted — the rendering workers that produced the output are unaffected. If a RenderWorker fails due to OOM during a large benchmark, only that worker is restarted with a reduced allocation. The supervision tree isolates faults at the narrowest possible scope.

```java
// DTR documents its own render-pipeline fault tolerance.
// Each failure corresponds to a real CI failure mode.
sayFaultTolerance(
    "DTR Render Pipeline Failures",
    List.of(
        "FileWriter: disk full",
        "RenderWorker: OOM during large benchmark",
        "IndexWorker: concurrent modification"
    ),
    List.of(
        "Supervisor restarts FileWriter, writes to /tmp fallback",
        "One-for-one restart with reduced heap allocation",
        "Transient fault: restart immediately, reprocess from last checkpoint"
    )
);
```

The /tmp fallback for disk saturation deserves explanation. When the primary output path (target/docs/test-results/) is on a partition that has filled during a large benchmark run, the supervisor cannot simply retry the write — the disk is full. The fallback writes output to /tmp, which is on a separate partition on CI runners. The build then copies from /tmp to the artifact upload path as a post-step. This two-phase write strategy adds latency but eliminates data loss.

The concurrent modification failure in the IndexWorker is the most interesting case. The index is built from the outputs of all four renderers running concurrently. If the index is updated while a renderer is still writing, the iterator throws ConcurrentModificationException. This is a transient fault — the data is consistent, just accessed at the wrong moment. The correct response is an immediate restart from the last checkpoint: the checkpoint captures the fully-written renderer outputs, so the index rebuild reads only stable data.

### Fault Tolerance: DTR Render Pipeline Failures

| # | Failure | Supervisor Recovery |
| --- | --- | --- |
| 1 | FileWriter: disk full | Supervisor restarts FileWriter, writes to /tmp fallback |
| 2 | RenderWorker: OOM during large benchmark | One-for-one restart with reduced heap allocation |
| 3 | IndexWorker: concurrent modification | Transient fault: restart immediately, reprocess from last checkpoint |

| Metric | Value |
| --- | --- |
| Failures documented | `3` |
| Recovery actions | `3` |
| Recovery coverage | `100%` |

| Worker | Failure mode | Fault class | Supervisor strategy |
| --- | --- | --- | --- |
| FileWriter | Disk full (ENOSPC) | Resource | Restart + /tmp fallback |
| RenderWorker | OOM during large benchmark | Resource | One-for-one restart, reduced heap |
| IndexWorker | ConcurrentModificationException | Transient | Immediate restart from checkpoint |

| Key | Value |
| --- | --- |
| `Failures documented` | `3` |
| `Recovery coverage` | `100%` |
| `sayFaultTolerance render time` | `69568 ns` |
| `Java version` | `25.0.2` |
| `Recovery actions` | `3` |

> [!NOTE]
> The checkpoint strategy for the IndexWorker requires that renderer output files are written atomically: each renderer writes to a temp file and renames it into place. A rename on the same filesystem is atomic on POSIX systems. The checkpoint therefore observes either the old version or the new version of each file — never a partial write. This invariant is what makes 'restart from last checkpoint' safe for the IndexWorker.

> [!WARNING]
> The /tmp fallback path is CI-specific. Local developer machines may not have a separate /tmp partition with sufficient capacity. If you run large benchmark suites locally and encounter disk saturation, increase the primary partition capacity rather than relying on the /tmp fallback. The fallback exists for CI resilience, not as a permanent disk management strategy.

| Check | Result |
| --- | --- |
| Failure list length matches recovery list length (3 == 3) | `✓ PASS` |
| sayFaultTolerance renders without throwing | `✓ PASS` |
| Checkpoint restart documented for transient IndexWorker fault | `✓ PASS` |
| Recovery coverage computed as 100% | `✓ PASS` |
| /tmp fallback documented for disk saturation | `✓ PASS` |

---
*Generated by [DTR](http://www.dtr.org)*
