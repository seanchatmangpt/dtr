# io.github.seanchatmangpt.dtr.test.GcMetricsDocTest

## Table of Contents

- [JEP 490: ZGC Generational Mode — Default in Java 26](#jep490zgcgenerationalmodedefaultinjava26)
- [GarbageCollectorMXBean Introspection](#garbagecollectormxbeanintrospection)
- [Heap Usage Profile: Before, During, and After Allocation](#heapusageprofilebeforeduringandafterallocation)
- [GC Pause Simulation: 100K Short-Lived Objects](#gcpausesimulation100kshortlivedobjects)
- [Recommended GC Configuration for DTR Workloads](#recommendedgcconfigurationfordtrworkloads)


## JEP 490: ZGC Generational Mode — Default in Java 26

ZGC (Z Garbage Collector) is a scalable, low-latency garbage collector designed to deliver sub-millisecond pause times regardless of heap size. JEP 490, delivered as part of Java 21 and now the promoted default generational mode in Java 26, introduces a two-generation heap structure — a young generation for short-lived objects and an old generation for long-lived ones. This generational split dramatically improves allocation throughput because the GC can focus most collection effort on the young generation, where the vast majority of objects die.

Prior to JEP 490, ZGC operated non-generationally: every collection cycle scanned the entire heap. Generational ZGC retains ZGC's concurrent, region-based design while adding the classic generational hypothesis as an optimization layer. The result is higher throughput with the same sub-millisecond pause guarantee.

### Environment Profile

| Property | Value |
| --- | --- |
| Java Version | `25.0.2` |
| Java Vendor | `Ubuntu` |
| OS | `Linux amd64` |
| Processors | `4` |
| Max Heap | `4022 MB` |
| Timezone | `Etc/UTC` |
| DTR Version | `2.6.0` |
| Timestamp | `2026-03-15T11:11:39.949266565Z` |

| Key | Value |
| --- | --- |
| `JEP 490 pause goal` | `< 1 ms regardless of heap size` |
| `Java version (this JVM)` | `25.0.2` |
| `ZGC detected` | `no (running OpenJDK 64-Bit Server VM)` |
| `Active GC(s)` | `G1 Young Generation, G1 Concurrent GC, G1 Old Generation` |
| `Generational structure` | `yes — multiple GC beans observed` |
| `GC bean count` | `3` |
| `Recommended ZGC flag` | `-XX:+UseZGC (generational by default in Java 26)` |

> [!NOTE]
> If the active GC shown above is not ZGC, the JVM was started without -XX:+UseZGC. In CI, G1GC is often the default. The documentation here covers the ZGC API contract; the observability methods work identically for all GCs.

> [!WARNING]
> ZGC Generational Mode requires Java 21+. In Java 26 it is the default ZGC mode when -XX:+UseZGC is specified. Do not set -XX:-ZGenerational explicitly unless you need non-generational ZGC for diagnostic comparison.

| Check | Result |
| --- | --- |
| Java version property is accessible | `PASS — 25.0.2` |
| ManagementFactory.getGarbageCollectorMXBeans() returns at least one bean | `PASS — 3 bean(s) found` |
| GC names are non-empty strings | `PASS — names: G1 Young Generation, G1 Concurrent GC, G1 Old Generation` |

## GarbageCollectorMXBean Introspection

The standard java.lang.management.ManagementFactory.getGarbageCollectorMXBeans() API provides a bean per GC phase (e.g., ZGC Minor, ZGC Major, or G1 Young/Old). Each bean reports the memory pools it manages, how many collection cycles have completed, and total elapsed collection time in milliseconds. This section captures those values from the live JVM.

```java
// Standard GC observability — works for ZGC, G1, Shenandoah, and Serial
import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;

List<GarbageCollectorMXBean> gcBeans =
    ManagementFactory.getGarbageCollectorMXBeans();

for (GarbageCollectorMXBean bean : gcBeans) {
    String name       = bean.getName();
    long   count      = bean.getCollectionCount();  // -1 if undefined
    long   timeMs     = bean.getCollectionTime();   // -1 if undefined
    String[] pools    = bean.getMemoryPoolNames();
}
```

| GC Name | Memory Pool | Collection Count | Collection Time (ms) |
| --- | --- | --- | --- |
| G1 Young Generation | G1 Eden Space | 1 | 10 |
| G1 Young Generation | G1 Survivor Space | 1 | 10 |
| G1 Young Generation | G1 Old Gen | 1 | 10 |
| G1 Concurrent GC | G1 Old Gen | 0 | 0 |
| G1 Old Generation | G1 Eden Space | 0 | 0 |
| G1 Old Generation | G1 Survivor Space | 0 | 0 |
| G1 Old Generation | G1 Old Gen | 0 | 0 |

| Key | Value |
| --- | --- |
| `GC beans present` | `3` |
| `Source API` | `ManagementFactory.getGarbageCollectorMXBeans() + getMemoryMXBean()` |
| `Heap committed (MB)` | `254` |
| `Heap max (MB)` | `4022` |
| `Heap used (MB)` | `36` |

| Check | Result |
| --- | --- |
| Total GC collection count >= 0 | `PASS — 1 total collections` |
| Heap used bytes > 0 | `PASS — 38383992 bytes` |
| Heap committed bytes > 0 | `PASS — 266338304 bytes` |

## Heap Usage Profile: Before, During, and After Allocation

This section captures three heap snapshots from the live JVM: (1) baseline before any intentional allocation, (2) immediately after allocating approximately 10 MB of byte arrays, and (3) after calling System.gc() to hint that a collection is desirable. All values are read directly from MemoryMXBean.getHeapMemoryUsage().

System.gc() is a hint only — the JVM may ignore it, defer it, or perform a partial collection. ZGC in particular runs concurrently and may not respond immediately. The post-GC snapshot reflects whatever state the JVM reaches shortly after the hint; it does not guarantee that all allocated objects were reclaimed.

| Phase | Init (MB) | Used (MB) | Committed (MB) | Max (MB) |
| --- | --- | --- | --- | --- |
| Before allocation | 252 | 36 | 254 | 4022 |
| During allocation (~10 MB added) | 252 | 44 | 254 | 4022 |
| After System.gc() hint | 252 | 7 | 40 | 4022 |

> [!NOTE]
> The 'During allocation' row should show heap usage higher than 'Before'. The delta depends on JIT optimizations, GC concurrent activity, and other threads running in the test JVM. Measured delta: 8192 KB.

> [!WARNING]
> System.gc() is a hint and not a command. ZGC and Shenandoah run concurrently and may reclaim memory asynchronously. The post-GC snapshot may not reflect full reclamation within the 200ms observation window.

| Check | Result |
| --- | --- |
| MemoryMXBean reports consistent init across phases | `PASS — init is stable at 252 MB` |
| Heap used after GC hint >= 0 | `PASS — 8113592 bytes` |
| Heap used during allocation >= 0 | `PASS — 46772600 bytes` |

## GC Pause Simulation: 100K Short-Lived Objects

This section measures the effect of creating 100,000 short-lived objects — a mix of strings and small int arrays — on GC collection counts and elapsed wall time. The measurement records the GC collection count delta before and after the allocation burst plus a System.gc() hint, providing a lower-bound estimate of GC activity triggered by the workload.

ZGC's generational mode is specifically optimized for exactly this workload: short-lived objects are collected from the young generation in concurrent minor cycles, avoiding full-heap stops. The collection count delta and elapsed time reported here are real measurements from the live JVM.

| Metric | Value | Notes |
| --- | --- | --- |
| Objects created | 100000 | strings + int[] pairs |
| Allocation wall time | 27 ms | System.nanoTime(), real measurement |
| Allocation time (ns) | 27499724 | 100000 objects |
| GC collections triggered | 1 | delta before/after + System.gc() |
| GC time delta | 0 ms | sum across all GC beans |
| Avg ns per object | 274 | allocation only |

> [!NOTE]
> ZGC Generational Mode targets young-generation collections that pause for under 1 ms. G1GC minor pauses are typically 5–50 ms for this workload size. The GC collection count delta includes any concurrent background cycles that completed during the 200 ms observation window after System.gc().

| Check | Result |
| --- | --- |
| GC collection count after >= count before | `PASS — delta: 1` |
| Elapsed time measured with System.nanoTime() | `PASS — 27499724 ns (27 ms)` |
| All 100000 objects allocated without error | `PASS — loop completed, elapsed 27 ms` |

## Recommended GC Configuration for DTR Workloads

DTR tests run in-process with the JVM that executes Maven Surefire. The ideal GC choice for DTR is one that minimises latency spikes (so documentation generation does not pause mid-test) and handles moderate heap churn from string building and JSON serialization. ZGC Generational Mode is the recommended choice for Java 26+.

The configuration below is tested against Java 26.ea.13+ with --enable-preview. It is suitable for .mvn/jvm.config or Surefire argLine in pom.xml. G1GC is listed as the CI fallback for environments where ZGC is not available (older JDK, non-HotSpot JVMs).

```shell
# .mvn/jvm.config — ZGC Generational (Java 26 default when ZGC is active)
--enable-preview
-XX:+UseZGC
-Xms256m
-Xmx1g
-XX:SoftMaxHeapSize=768m
-XX:ZCollectionInterval=0
-Xlog:gc*:file=target/gc.log:time,uptime,level,tags:filecount=3,filesize=10m
```

```xml
<!-- pom.xml — Surefire plugin configuration for CI -->
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>
            --enable-preview
            -XX:+UseZGC
            -Xms256m
            -Xmx1g
            -XX:SoftMaxHeapSize=768m
            -Xlog:gc:file=${project.build.directory}/gc.log
        </argLine>
    </configuration>
</plugin>
```

| JVM Flag | Purpose | Recommended Value |
| --- | --- | --- |
| -XX:+UseZGC | Activate ZGC (generational by default in Java 26) | required |
| -Xms256m | Initial heap — avoids early expansion pauses | 256m–512m |
| -Xmx1g | Max heap — prevents OOM in large test suites | 1g–2g |
| -XX:SoftMaxHeapSize | Target heap before ZGC expands — reduces footprint | 75% of Xmx |
| -XX:ZCollectionInterval | Force periodic collection (0=on-demand only) | 0 (default) |
| -Xlog:gc* | Structured GC logging for post-mortem analysis | file=target/gc.log |
| -XX:+UseG1GC | G1GC fallback for environments without ZGC | omit if ZGC available |

| Key | Value |
| --- | --- |
| `VM name` | `OpenJDK 64-Bit Server VM` |
| `Java version` | `25.0.2` |
| `Available processors` | `4` |
| `Active GC (this test run)` | `G1 Young Generation, G1 Concurrent GC, G1 Old Generation` |
| `Heap max (this JVM)` | `4022 MB` |

> [!NOTE]
> ZGC sub-millisecond pause times are achieved regardless of heap size because all marking, relocation, and compaction phases run concurrently with application threads. The only stop-the-world phase is a brief initial mark and a final relocate-roots step, both measured in tens of microseconds in practice.

> [!WARNING]
> Do not set -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC in production or CI environments. Epsilon performs no GC at all — it is a no-op collector for allocation-rate benchmarks only and will cause OOM in any real workload.

| Check | Result |
| --- | --- |
| Heap max > 0 (JVM has bounded heap) | `PASS — 4022 MB max` |
| Java version property available | `PASS — 25.0.2` |
| GarbageCollectorMXBeans accessible at test runtime | `PASS — 3 bean(s): G1 Young Generation, G1 Concurrent GC, G1 Old Generation` |

---
*Generated by [DTR](http://www.dtr.org)*
