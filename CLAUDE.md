# DTR Quick Reference | v2.5.0 | Java 25.0.2 | Maven 4.0.0-rc-5 | mvnd 2.0.0-rc-3
**Rules:** (1) REAL measurements only — `System.nanoTime()`, report: value+units+Java+iterations. (2) Always use real DTR CLI — JUnit 5 + DtrContext + RenderMachine pipeline, never bypass. (3) Toolchain: Java `/usr/lib/jvm/java-25-openjdk-amd64`, mvnd `/opt/mvnd/bin/mvnd`, `.mvn/maven.config`: `--enable-preview release=25`.
## Build
```bash
mvnd test -pl dtr-integration-test -Dtest=ApiControllerDocTest   # run test → docs/test/<Class>.md
python3 maven-proxy-auth.py &                                     # fix auth errors (listens :3128)
mvnd clean install -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
mvnd --stop && mvnd test ...                                      # restart daemon on failures
```
## Modules
`dtr-core/` JAR — RenderMachine + say* API + TestBrowser + JUnit 5 extension | `dtr-integration-test/` WAR — Ninja Framework integration tests, extend `NinjaApiDtr` | `dtr-benchmarks/` fat JAR — JMH benchmarks | `dtr-cli/` Python — CLI exports/publishing
## say* API
| Method | Output | Method | Output |
|--------|--------|--------|--------|
| `sayNextSection(String)` | H1+TOC | `sayWarning(String)` | `[!WARNING]` |
| `say(String)` | Paragraph | `sayNote(String)` | `[!NOTE]` |
| `sayCode(String,lang)` | Fenced block | `sayKeyValue(Map)` | 2-col table |
| `sayTable(String[][])` | MD table | `sayAndMakeRequest(Request)` | HTTP + doc |
| `sayJson(Object)` | JSON block | `sayAndAssertThat(msg,val,matcher)` | Assert + doc |
## Test Patterns
```java
// A: standalone  @ExtendWith(DtrExtension.class) class T { @Test void t(DtrContext ctx) { ctx.sayNextSection("X"); } }
// B: HTTP+docs   class T extends NinjaApiDtr { @Test void t() { sayAndMakeRequest(Request.GET().url(testServerUrl())); } }
```
## Key Files
`dtr-core/…/rendermachine/RenderMachineCommands.java` full API | `dtr-core/…/junit5/DtrExtension.java` lifecycle | `dtr-integration-test/…/controllers/ApiControllerDocTest.java` canonical example | `dtr-integration-test/…/controllers/utils/NinjaApiDtr.java` HTTP base | `maven-proxy-auth.py` proxy | `.mvn/maven.config` flags

**Updated:** 2026-03-14 | **Branch:** claude/add-claude-documentation-c5zqy
