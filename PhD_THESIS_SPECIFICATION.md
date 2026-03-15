# PhD Thesis Specification: "Agentic Loops in Documentation Automation"

## Executive Decision

**Title:** "Agentic Loops: How Autonomous Agent Teams Transform Software Documentation into Executable Contracts"

**Format:** Academic white paper + technical implementation guide (60-80 pages total)
**Audience:** Computer Science PhD programs + industry technical leaders
**Target Journal/Conference:** ICSE, FSE, or OOPSLA
**Publication Goal:** Demonstrate that documentation-as-code + agent autonomy solves the docs-code divergence problem

---

## Question 1: Scope

### Decision: THREE CONCENTRIC CIRCLES

**Circle 1: Core Innovation (Chapters 1-4)**
- Problem: Documentation diverges from code; tests prove code, docs don't
- Solution: DTR (Documentation Testing Runtime) — tests ARE docs
- Proof: Java 26 implementation with 105+ unit tests
- Impact: docs stay synchronized via `mvnd verify` gate

**Circle 2: Agent Autonomy Pattern (Chapters 5-7)**
- Insight: Single agent (Claude) can't optimize end-to-end systems
- Solution: Launch 5+ autonomous agents + orchestration framework
- Proof: 5-layer Rust stack (scanner, cache, oracle, remediate, cli)
- Metrics: 5-131x performance improvement via parallelism
- Philosophy: Agents decide alone, validate before declaring done, escalate only genuine conflicts

**Circle 3: Vision 2030 — The Phase Change (Chapter 8)**
- Historical: Humans write code, then document it (divergence)
- Current (2026): Documentation testing ensures sync (DTR)
- Future (2030): Agents generate + optimize both code AND docs together
- Why it matters: AI-assisted development requires docs-as-contract

### What's NOT Included
- Full DTR API reference (belongs in Javadoc)
- Complete Rust crate implementation (belongs in crates/)
- Maven Central release procedures (belongs in CONTRIBUTING.md)
- General AI/ML theory (focused, not survey paper)

---

## Question 2: Format

### Decision: Hybrid Academic + Technical

**Structure:**

```
Part 1: THESIS (Academic, 40 pages)
├─ Chapter 1: Introduction & Problem Statement
├─ Chapter 2: Related Work (docs-as-code, AI agents, testing)
├─ Chapter 3: DTR Architecture & Design
├─ Chapter 4: Experimental Validation (Java 26 benchmark suite)
├─ Chapter 5: Agent Autonomy Framework
├─ Chapter 6: Five-Layer Performance Optimization
├─ Chapter 7: Lessons Learned & Tradeoffs
├─ Chapter 8: Vision 2030 - The Phase Change
└─ Conclusion & Future Work

Part 2: APPENDIX (Technical, 20-40 pages)
├─ Appendix A: DTR Quick Start (runnable example)
├─ Appendix B: Rust Stack Architecture (with code snippets)
├─ Appendix C: Benchmark Raw Data (.csv)
├─ Appendix D: Agent Autonomy Decision Framework (from CLAUDE.md)
├─ Appendix E: Reproducibility (how to run all validations)
└─ Appendix F: Implementation Details (for review/extension)
```

**Output Format:**
- Primary: PDF (LaTeX)
- Secondary: Markdown (GitHub-readable)
- Interactive: Jupyter notebooks for benchmarks

---

## Question 3: Audience

### Decision: DUAL AUDIENCE (Academic + Industry)

**Primary: PhD Programs & Research Community**
- Contribution: Shows how agent autonomy solves documentation divergence
- Rigor: Formal problem statement, hypothesis, validation via controlled experiments
- Novelty: Most docs-as-code work is ad-hoc; this is systematic + measured
- Impact: Opens research direction in agentic documentation

**Secondary: Industry Technical Leaders (engineering directors, architects)**
- Relevance: Can they adopt this in their team?
- Practical: Working code in both Java 26 + Rust
- Business: Performance gains (5-131x), time savings (docs sync automatically)
- Risk: Honest discussion of tradeoffs (when docs-as-code breaks)

---

## Question 4: Key Sections Emphasized

### Decision: EQUAL WEIGHT TO THREE PILLARS

**Pillar 1: THEORY (40% of content)**
- Problem formalization: "Documentation divergence as a code quality debt"
- DTR as solution: Executable contracts that prevent divergence
- Agent autonomy as meta-pattern: How teams scale beyond single developers
- Vision 2030: Why this matters for the future of AI-assisted development

**Pillar 2: PRACTICE (40% of content)**
- DTR implementation: 105+ tests as documentation
- Rust stack: 5-layer optimization (scanner→cache→oracle→remediate→cli)
- Benchmarks: 27 Criterion.rs measurements (.xx precision)
- Reproducible: "You can run all of this"

**Pillar 3: PHILOSOPHY (20% of content)**
- "Calendar owns the year" (CalVer invariant)
- "Human owns semantics, script owns arithmetic" (release authority)
- "Agents decide, validate, escalate only conflicts" (autonomy framework)
- Why these matter for sustainable software

---

## Detailed Outline

### PART 1: THESIS (40 pages)

#### Chapter 1: Introduction (3 pages)
- Hook: "Why does your README describe code from 6 months ago?"
- Problem: Documentation diverges from code despite testing
- Hypothesis: Documentation should BE the test
- Contribution: DTR system + agent autonomy framework
- Scope: Focus on Java 26 + Rust optimization, not general AI

#### Chapter 2: Related Work (4 pages)
- Docs-as-code: Markdown, Sphinx, AsciiDoc (existing)
- DocTest precedent: Python, Rust, Haskell docstrings
- DTR novelty: Executable documentation that drives validation
- Agent autonomy: LLMs, multi-agent systems, orchestration
- Gap: No prior work combines docs-testing + agent autonomy

#### Chapter 3: DTR Architecture (6 pages)
- Overview: 5 components (junit5, RenderMachine, say* API, DtrContext, output pipeline)
- Design philosophy: Real code, real measurement, test-driven
- Java 26 features: Sealed classes, pattern matching, text blocks
- Verification gate: `mvnd verify` as CI contract
- Example: Simple DTR test (2-3 pages with code)

#### Chapter 4: Experimental Validation (8 pages)
- Methodology: Criterion.rs benchmarks, 100+ iterations per measurement
- Baseline: Traditional docs vs DTR docs (sync rates)
- Measurement: 27 benchmark scenarios across 5 implementations
- Results: 105/105 tests pass, 98% cache hit rate, <10ms end-to-end (hot)
- Statistical: Confidence intervals, outlier analysis, reproducibility

#### Chapter 5: Agent Autonomy Framework (5 pages)
- Problem: Single agent can't optimize 5 layers efficiently
- Solution: Launch 5+ autonomous agents in parallel
- Decision framework: What can agents decide? When escalate?
- Results: 5-131x speedup via parallelism vs serial
- Philosophy: "Ask forgiveness, not permission" encoded in CLAUDE.md

#### Chapter 6: Five-Layer Performance Optimization (6 pages)
- Layer 1 (Scanner): Tree-sitter AST, aho-corasick, 2.46ms/file
- Layer 2 (Cache): blake3 SIMD, redb, DashMap, 0.19µs L1 hit
- Layer 3 (Oracle): Naive Bayes, temporal decay, 365ns/file
- Layer 4 (Remediate): Crop rope, similar, tempfile, 3.45µs/edit
- Layer 5 (CLI): Two-phase warmup, parallelism, 76µs single (hot)
- Scaling: 1→10→100 files shows sublinear growth due to cache

#### Chapter 7: Lessons Learned & Tradeoffs (4 pages)
- What worked: CalVer versioning, one-command release, CI as gate
- What was hard: Getting C++ developers to write Rust; tree-sitter learning curve
- Tradeoffs: Real measurement takes time (no synthetic data)
- Scalability: Tested to 1000 files; extrapolated to 10K
- Limitations: Java-specific; not yet multi-language

#### Chapter 8: Vision 2030 — The Phase Change (2 pages)
- Observation: Documentation has moved LEFT in the development cycle
- 2020: Docs written AFTER code (divergence)
- 2026: Docs tested WITH code (DTR)
- 2030: Agents generate BOTH code AND docs together, validated in lockstep
- Why: AI-assisted development makes sync possible + necessary
- Impact: Becomes non-negotiable for production systems

#### Conclusion & Future Work (2 pages)
- Summary: DTR + agent autonomy solves docs divergence
- Contribution: Practical system + generalizable framework
- Future: Multi-language support, IDE integration, LLM-generated docs validation

---

### PART 2: APPENDIX (20-40 pages)

**Appendix A: Quick Start (4 pages)**
- Install Java 26, mvnd, Maven
- Write a DTR test
- Run `mvnd verify`
- Output: Markdown + HTML + PDF

**Appendix B: Rust Stack Architecture (8 pages)**
- Workspace structure, 5 crates, dependencies
- Code walkthrough: scanner extraction, cache hashing, oracle scoring
- Design patterns: OnceLock caching, rayon parallelism, zero-copy buffers
- Testing: 105+ unit tests with real data

**Appendix C: Benchmark Raw Data (8 pages)**
- CSV: All 27 benchmarks, 100 iterations each, mean/median/stddev/CI
- Graphs: Distribution plots, scaling curves, cache hit rates
- Metadata: Java version, CPU, OS, compilation flags

**Appendix D: Agent Autonomy Decision Framework (4 pages)**
- From CLAUDE.md: Authority boundaries, escalation criteria
- Examples: "When to launch agents", "When to ask permission"
- Pattern: 5-agent team structure, sync points, integration

**Appendix E: Reproducibility (4 pages)**
- Exact commands to build, test, benchmark
- Environment setup (Java 26 EA, SDKMAN, mvnd)
- Expected outputs and how to validate
- Time estimates per task

**Appendix F: Implementation Details (optional, 12 pages)**
- DTR say* API reference with examples
- CalVer versioning algorithm (pseudocode)
- Release automation via GitHub Actions
- H-Guard semantic lie detection patterns

---

## Validation Criteria

The thesis is complete when:

✅ **Theory:** 3 chapters on problem/solution/vision
✅ **Practice:** Working DTR system + 5-layer Rust stack with code
✅ **Measurement:** 27 benchmarks, statistical rigor, reproducible
✅ **Contribution:** Novel enough for ICSE/FSE (not just engineering report)
✅ **Readable:** PhD student can understand in 2 hours; implement in 1 week

---

## Timeline Estimate

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| Write thesis (Chapters 1-8) | 3-4 days | 40-page PDF |
| Create appendices | 2-3 days | Runnable examples + raw data |
| Create Jupyter notebooks | 1-2 days | Interactive benchmark exploration |
| Final review + polish | 1 day | Publication-ready |
| **TOTAL** | **~1 week** | **PhD thesis ready** |

---

## Success Criteria

- [ ] Thesis submitted to arXiv
- [ ] Code on GitHub with reproduction instructions
- [ ] Benchmarks repeatable by independent party
- [ ] Readable by both academics AND industry engineers
- [ ] Future research direction clear (Vision 2030)

---

**Next Step:** Write Chapter 1 (Introduction) — establish the problem and hook the reader.
