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
package io.github.seanchatmangpt.dtr.render.blog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.seanchatmangpt.dtr.crossref.DocTestRef;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blog post render machine generating platform-specific markdown.
 *
 * Converts test execution into blog post markdown compatible with Dev.to,
 * Medium, Substack, LinkedIn, and Hashnode. Generates social media queue
 * entries for tweets and platform-specific content.
 */
public final class BlogRenderMachine extends RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(BlogRenderMachine.class);

    private static final String BASE_DIR = "target/site/dtr/blog";
    private final BlogTemplate template;
    private final StringBuilder buffer = new StringBuilder();
    private final List<String> tweetables = new ArrayList<>();
    private final List<String> sections = new ArrayList<>();
    private String fileName;
    private String tldr = "";
    private String cta = "";
    private int wordCount = 0;

    /**
     * Create a blog render machine with the given platform template.
     *
     * @param template the blog platform template (Dev.to, Medium, etc.)
     */
    public BlogRenderMachine(BlogTemplate template) {
        this.template = template;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void say(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        buffer.append(text).append("\n\n");
        wordCount += text.split("\\s+").length;
    }

    @Override
    public void sayNextSection(String heading) {
        if (heading == null || heading.isEmpty()) {
            return;
        }
        sections.add(heading);
        buffer.append("## ").append(heading).append("\n\n");
    }

    @Override
    public void sayRaw(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return;
        }
        buffer.append(markdown).append("\n\n");
    }

    @Override
    public void sayTable(String[][] data) {
        if (data == null || data.length == 0) {
            return;
        }
        for (String[] row : data) {
            buffer.append("| ");
            for (String cell : row) {
                buffer.append(cell != null ? cell : "").append(" | ");
            }
            buffer.append("\n");
            if (data[0] == row) {
                // Header separator
                buffer.append("|");
                for (int i = 0; i < row.length; i++) {
                    buffer.append(" --- |");
                }
                buffer.append("\n");
            }
        }
        buffer.append("\n");
    }

    @Override
    public void sayCode(String code, String language) {
        if (code == null || code.isEmpty()) {
            return;
        }
        String lang = language != null ? language : "text";
        buffer.append("```").append(lang).append("\n")
            .append(code).append("\n```\n\n");
        wordCount += 2;
    }

    @Override
    public void sayWarning(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        buffer.append("> [!WARNING]\n> ").append(message).append("\n\n");
        wordCount += message.split("\\s+").length;
    }

    @Override
    public void sayNote(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        buffer.append("> [!NOTE]\n> ").append(message).append("\n\n");
        wordCount += message.split("\\s+").length;
    }

    @Override
    public void sayKeyValue(Map<String, String> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return;
        }
        buffer.append("| Key | Value |\n| --- | --- |\n");
        for (var entry : pairs.entrySet()) {
            buffer.append("| ").append(entry.getKey()).append(" | ")
                .append(entry.getValue()).append(" |\n");
        }
        buffer.append("\n");
    }

    @Override
    public void sayUnorderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (String item : items) {
            buffer.append("- ").append(item).append("\n");
            wordCount += item.split("\\s+").length;
        }
        buffer.append("\n");
    }

    @Override
    public void sayOrderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            buffer.append(i + 1).append(". ").append(items.get(i)).append("\n");
            wordCount += items.get(i).split("\\s+").length;
        }
        buffer.append("\n");
    }

    @Override
    public void sayJson(Object object) {
        if (object == null) {
            return;
        }
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(object);
            sayCode(json, "json");
        } catch (Exception e) {
            logger.warn("Could not serialize object to JSON", e);
        }
    }

    @Override
    public void sayAssertions(Map<String, String> assertions) {
        if (assertions == null || assertions.isEmpty()) {
            return;
        }
        buffer.append("| Check | Result |\n| --- | --- |\n");
        for (var entry : assertions.entrySet()) {
            buffer.append("| ").append(entry.getKey()).append(" | ")
                .append(entry.getValue()).append(" |\n");
        }
        buffer.append("\n");
    }

    @Override
    public void sayRef(DocTestRef ref) {
        // Render as markdown link to another doc
        if (ref == null) {
            return;
        }
        buffer.append("[See ").append(ref.anchor()).append("](../markdown/")
            .append(ref.docTestClass()).append(".md#")
            .append(ref.anchor().toLowerCase().replace(" ", "-")).append(")\n\n");
    }

    @Override
    public void sayCite(String citationKey) {
        if (citationKey == null || citationKey.isEmpty()) {
            return;
        }
        buffer.append("[^").append(citationKey).append("]");
    }

    @Override
    public void sayCite(String citationKey, String pageRef) {
        if (citationKey == null || citationKey.isEmpty()) {
            return;
        }
        buffer.append("[^").append(citationKey).append(":").append(pageRef).append("]");
    }

    @Override
    public void sayFootnote(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        buffer.append(" ").append(template.footnoteMarker(1)).append(" ");
        wordCount += text.split("\\s+").length;
    }

    @Override
    public void sayCodeModel(Class<?> clazz) {
        if (clazz == null) return;
        buffer.append("### Class: `").append(clazz.getSimpleName()).append("`\n\n");
        buffer.append("**Package:** `").append(clazz.getPackageName()).append("`\n\n");

        if (clazz.isSealed()) {
            buffer.append("**Sealed hierarchy:** Yes\n");
            var permitted = clazz.getPermittedSubclasses();
            if (permitted != null && permitted.length > 0) {
                buffer.append("**Permitted subclasses:**\n");
                for (Class<?> sub : permitted) {
                    buffer.append("- `").append(sub.getSimpleName()).append("`\n");
                }
            }
        } else if (clazz.isRecord()) {
            buffer.append("**Type:** Record\n");
        } else if (clazz.isInterface()) {
            buffer.append("**Type:** Interface\n");
        } else if (clazz.isEnum()) {
            buffer.append("**Type:** Enum\n");
        }
        buffer.append("\n");
    }

    @Override
    public void sayCodeModel(java.lang.reflect.Method method) {
        if (method == null) return;
        buffer.append("### Method: `").append(method.getName()).append("`\n\n");
        buffer.append("| Property | Value |\n| --- | --- |\n");
        buffer.append("| Return Type | `").append(method.getReturnType().getSimpleName()).append("` |\n");
        buffer.append("| Declaring Class | `").append(method.getDeclaringClass().getSimpleName()).append("` |\n");
        buffer.append("| Parameter Count | ").append(method.getParameterCount()).append(" |\n");
        buffer.append("\n");
    }

    @Override
    public void sayCallSite() {
        var walker = java.lang.StackWalker.getInstance(java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE);
        walker.walk(frames -> {
            frames.skip(2).findFirst().ifPresent(frame -> {
                buffer.append("> 📍 **Call Site:** `").append(frame.getClassName())
                    .append("#").append(frame.getMethodName())
                    .append(":").append(frame.getLineNumber()).append("`\n\n");
            });
            return null;
        });
    }

    @Override
    public void sayAnnotationProfile(Class<?> clazz) {
        if (clazz == null) return;
        buffer.append("### Annotation Profile: `").append(clazz.getSimpleName()).append("`\n\n");

        var classAnnotations = clazz.getAnnotations();
        if (classAnnotations.length > 0) {
            buffer.append("**Class annotations:**\n");
            for (var a : classAnnotations) {
                buffer.append("- `@").append(a.annotationType().getSimpleName()).append("`\n");
            }
            buffer.append("\n");
        }
    }

    @Override
    public void sayClassHierarchy(Class<?> clazz) {
        if (clazz == null) return;
        buffer.append("### Class Hierarchy: `").append(clazz.getSimpleName()).append("`\n\n");

        List<String> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            hierarchy.add("`" + current.getSimpleName() + "`");
            current = current.getSuperclass();
        }
        buffer.append(String.join(" → ", hierarchy)).append("\n\n");
    }

    @Override
    public void sayStringProfile(String text) {
        if (text == null || text.isEmpty()) return;
        buffer.append("### String Profile\n\n");
        buffer.append("| Metric | Value |\n| --- | --- |\n");
        buffer.append("| Length | ").append(text.length()).append(" |\n");
        buffer.append("| Words | ").append(text.split("\\s+").length).append(" |\n");
        buffer.append("| Lines | ").append(text.lines().count()).append(" |\n");
        buffer.append("\n");
    }

    @Override
    public void sayReflectiveDiff(Object before, Object after) {
        if (before == null || after == null) return;
        buffer.append("### Reflective Diff\n\n");
        buffer.append("| Field | Before | After |\n| --- | --- | --- |\n");
        try {
            for (var field : before.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object beforeVal = field.get(before);
                Object afterVal = field.get(after);
                if (!java.util.Objects.equals(beforeVal, afterVal)) {
                    buffer.append("| `").append(field.getName()).append("` | ")
                        .append(beforeVal != null ? "`" + beforeVal + "`" : "null")
                        .append(" | ")
                        .append(afterVal != null ? "`" + afterVal + "`" : "null")
                        .append(" |\n");
                }
            }
        } catch (Exception e) {
            buffer.append("| *(Error: ").append(e.getMessage()).append(")* | — | — |\n");
        }
        buffer.append("\n");
    }

    @Override
    public void sayControlFlowGraph(java.lang.reflect.Method method) {
        if (method == null) return;
        buffer.append("### Control Flow Graph: `").append(method.getName()).append("`\n\n");
        buffer.append("> CFG visualization requires Java 26+ with `@CodeReflection` annotation.\n\n");
    }

    @Override
    public void sayCallGraph(Class<?> clazz) {
        if (clazz == null) return;
        buffer.append("### Call Graph: `").append(clazz.getSimpleName()).append("`\n\n");
        buffer.append("> Call graph visualization requires Java 26+ with `@CodeReflection` annotation.\n\n");
    }

    @Override
    public void sayOpProfile(java.lang.reflect.Method method) {
        if (method == null) return;
        buffer.append("### Op Profile: `").append(method.getName()).append("`\n\n");
        buffer.append("> Op profile requires Java 26+ with `@CodeReflection` annotation.\n\n");
    }

    @Override
    public void sayBenchmark(String label, Runnable task) {
        sayBenchmark(label, task, 50, 500);
    }

    @Override
    public void sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds) {
        if (label == null || task == null) return;

        // Warmup
        for (int i = 0; i < warmupRounds; i++) {
            task.run();
        }

        // Measure
        long[] times = new long[measureRounds];
        for (int i = 0; i < measureRounds; i++) {
            long start = System.nanoTime();
            task.run();
            times[i] = System.nanoTime() - start;
        }

        // Calculate stats
        Arrays.sort(times);
        long min = times[0];
        long max = times[measureRounds - 1];
        long sum = 0;
        for (long t : times) sum += t;
        double avg = (double) sum / measureRounds;
        long p99 = times[(int) (measureRounds * 0.99)];
        double opsPerSec = 1_000_000_000.0 / avg;

        buffer.append("### Benchmark: ").append(label).append("\n\n");
        buffer.append("| Metric | Value |\n| --- | --- |\n");
        buffer.append(String.format("| Average | `%.0f ns` |\n", avg));
        buffer.append(String.format("| Min | `%d ns` |\n", min));
        buffer.append(String.format("| Max | `%d ns` |\n", max));
        buffer.append(String.format("| P99 | `%d ns` |\n", p99));
        buffer.append(String.format("| Throughput | `%.0f ops/sec` |\n", opsPerSec));
        buffer.append("\n*").append(measureRounds).append(" measured rounds after ")
            .append(warmupRounds).append(" warmup rounds*\n\n");
    }

    @Override
    public void sayMermaid(String diagramDsl) {
        if (diagramDsl == null || diagramDsl.isEmpty()) return;
        buffer.append("```mermaid\n").append(diagramDsl).append("\n```\n\n");
    }

    @Override
    public void sayClassDiagram(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;
        buffer.append("```mermaid\nclassDiagram\n");
        for (Class<?> clazz : classes) {
            if (clazz == null) continue;
            buffer.append("    class ").append(clazz.getSimpleName()).append(" {\n");
            for (var method : clazz.getDeclaredMethods()) {
                buffer.append("        +").append(method.getName()).append("()\n");
            }
            buffer.append("    }\n");
        }
        buffer.append("```\n\n");
    }

    @Override
    public void sayDocCoverage(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;
        buffer.append("### Documentation Coverage\n\n");
        buffer.append("> Documentation coverage analysis available in primary markdown output.\n\n");
    }

    @Override
    public void sayEnvProfile() {
        buffer.append("### Environment Profile\n\n");
        buffer.append("| Property | Value |\n| --- | --- |\n");
        buffer.append("| Java Version | `").append(System.getProperty("java.version")).append("` |\n");
        buffer.append("| OS | `").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("`\n");
        buffer.append("| Available Processors | `").append(Runtime.getRuntime().availableProcessors()).append("` |\n");
        buffer.append("| Max Memory | `").append(Runtime.getRuntime().maxMemory() / (1024 * 1024)).append(" MB` |\n");
        buffer.append("\n");
    }

    @Override
    public void sayRecordComponents(Class<? extends Record> recordClass) {
        if (recordClass == null) return;
        buffer.append("### Record Schema: `").append(recordClass.getSimpleName()).append("`\n\n");

        var components = recordClass.getRecordComponents();
        if (components == null || components.length == 0) {
            buffer.append("*(No record components)*\n\n");
            return;
        }

        buffer.append("| Component | Type |\n| --- | --- |\n");
        for (var comp : components) {
            buffer.append("| `").append(comp.getName()).append("` | `")
                .append(comp.getType().getSimpleName()).append("` |\n");
        }
        buffer.append("\n");
    }

    @Override
    public void sayException(Throwable t) {
        if (t == null) return;
        buffer.append("### Exception: `").append(t.getClass().getSimpleName()).append("`\n\n");
        buffer.append("**Message:** ").append(t.getMessage() != null ? t.getMessage() : "*(no message)*").append("\n\n");

        // Cause chain
        List<String> causeChain = new ArrayList<>();
        Throwable cause = t.getCause();
        while (cause != null) {
            causeChain.add("`" + cause.getClass().getSimpleName() + "`" +
                (cause.getMessage() != null ? ": " + cause.getMessage() : ""));
            cause = cause.getCause();
        }
        if (!causeChain.isEmpty()) {
            buffer.append("**Cause chain:**\n");
            for (String c : causeChain) {
                buffer.append("- ").append(c).append("\n");
            }
            buffer.append("\n");
        }

        // Stack trace
        var frames = t.getStackTrace();
        int limit = Math.min(5, frames.length);
        if (limit > 0) {
            buffer.append("**Top stack frames:**\n");
            for (int i = 0; i < limit; i++) {
                buffer.append("- `").append(frames[i].getClassName()).append("#")
                    .append(frames[i].getMethodName()).append(":")
                    .append(frames[i].getLineNumber()).append("`\n");
            }
        }
        buffer.append("\n");
    }

    @Override
    public void sayAsciiChart(String label, double[] values, String[] xLabels) {
        if (label == null || values == null || values.length == 0) return;

        buffer.append("### ").append(label).append("\n\n```\n");
        double max = Arrays.stream(values).max().orElse(1.0);
        if (max == 0) max = 1.0;
        int barWidth = 20;

        for (int i = 0; i < values.length; i++) {
            String rowLabel = (xLabels != null && i < xLabels.length) ? xLabels[i] : ("" + i);
            int filled = (int) Math.round((values[i] / max) * barWidth);
            int empty = barWidth - filled;
            String bar = "█".repeat(filled) + "░".repeat(empty);
            buffer.append(String.format("%-6s %s  %.0f\n", rowLabel, bar, values[i]));
        }
        buffer.append("```\n\n");
    }

    @Override
    public void sayContractVerification(Class<?> contract, Class<?>... implementations) {
        if (contract == null) return;
        buffer.append("### Contract: `").append(contract.getSimpleName()).append("`\n\n");
        buffer.append("> Contract verification details available in primary markdown output.\n\n");
    }

    @Override
    public void sayEvolutionTimeline(Class<?> clazz, int maxEntries) {
        if (clazz == null) return;
        buffer.append("### Evolution Timeline: `").append(clazz.getSimpleName()).append("`\n\n");
        buffer.append("> Git history available in primary markdown output.\n\n");
    }

    @Override
    public void sayJavadoc(java.lang.reflect.Method method) {
        if (method == null) return;
        buffer.append("### Javadoc: `").append(method.getName()).append("`\n\n");
        buffer.append("> Javadoc extraction available in primary markdown output.\n\n");
    }

    @Override
    public void saySystemProperties() {
        saySystemProperties(null);
    }

    @Override
    public void saySystemProperties(String regexFilter) {
        var props = System.getProperties();
        var entryStream = props.entrySet().stream();
        if (regexFilter != null && !regexFilter.isBlank()) {
            var pattern = java.util.regex.Pattern.compile(regexFilter);
            var predicate = pattern.asPredicate();
            entryStream = entryStream.filter(e -> predicate.test(e.getKey().toString()));
        }

        var sortedEntries = entryStream
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .toList();

        if (sortedEntries.isEmpty()) {
            buffer.append("> No system properties found matching filter: `").append(regexFilter).append("`\n\n");
            return;
        }

        buffer.append("### JVM System Properties");
        if (regexFilter != null && !regexFilter.isBlank()) {
            buffer.append(" (filtered: `").append(regexFilter).append("`)");
        }
        buffer.append("\n\n| Property | Value |\n| --- | --- |\n");

        for (var entry : sortedEntries) {
            buffer.append("| `").append(entry.getKey()).append("` | `")
                .append(entry.getValue().toString().replace("|", "\\|")).append("` |\n");
        }
        buffer.append("\n*").append(sortedEntries.size()).append(" properties*\n\n");
    }

    @Override
    public void saySecurityManager() {
        buffer.append("### Security Environment\n\n");

        // Security Manager status
        var sm = System.getSecurityManager();
        buffer.append("| Property | Status |\n| --- | --- |\n");
        buffer.append("| Security Manager | `").append(sm != null ? "PRESENT" : "ABSENT").append("` |\n\n");

        // Providers
        buffer.append("**Security Providers:**\n");
        var providers = java.security.Security.getProviders();
        for (var provider : providers) {
            buffer.append("- `").append(provider.getName()).append("` (v").append(provider.getVersion()).append(")\n");
        }
        buffer.append("\n");
    }

    @Override
    public void sayModuleDependencies(Class<?>... classes) {
        if (classes == null || classes.length == 0) {
            buffer.append("> No classes provided for module dependency analysis.\n\n");
            return;
        }

        buffer.append("### Module Dependencies\n\n");

        Map<Module, List<Class<?>>> moduleMap = Arrays.stream(classes)
                .filter(clazz -> clazz != null)
                .collect(Collectors.groupingBy(Class::getModule, LinkedHashMap::new, Collectors.toList()));

        for (var entry : moduleMap.entrySet()) {
            Module module = entry.getKey();
            String moduleName = module.isNamed() ? "`" + module.getName() + "`" : "**Unnamed Module** (classpath)";
            buffer.append("#### ").append(moduleName).append("\n\n");

            if (!module.isNamed()) {
                buffer.append("Classes from this module:\n");
                for (Class<?> c : entry.getValue()) {
                    buffer.append("- `").append(c.getName()).append("`\n");
                }
            } else {
                var descriptor = module.getDescriptor();
                if (descriptor != null) {
                    buffer.append("| Property | Value |\n| --- | --- |\n");
                    buffer.append("| Name | `").append(descriptor.name()).append("` |\n");
                    buffer.append("| Automatic | `").append(descriptor.isAutomatic()).append("` |\n");
                }
            }
            buffer.append("\n");
        }
    }

    @Override
    public void sayThreadDump() {
        var threadMXBean = ManagementFactory.getThreadMXBean();

        buffer.append("### Thread Summary\n\n");
        buffer.append("| Metric | Value |\n| --- | --- |\n");
        buffer.append("| Thread Count | `").append(threadMXBean.getThreadCount()).append("` |\n");
        buffer.append("| Daemon Thread Count | `").append(threadMXBean.getDaemonThreadCount()).append("` |\n");
        buffer.append("| Peak Thread Count | `").append(threadMXBean.getPeakThreadCount()).append("` |\n");
        buffer.append("\n### Thread Details\n\n");
        buffer.append("| ID | Name | State |\n| --- | --- | --- |\n");

        long[] threadIds = threadMXBean.getAllThreadIds();
        for (long threadId : threadIds) {
            var threadInfo = threadMXBean.getThreadInfo(threadId);
            if (threadInfo != null) {
                buffer.append("| `").append(threadId).append("` | `")
                    .append(threadInfo.getThreadName().replace("|", "\\|")).append("` | `")
                    .append(threadInfo.getThreadState()).append("` |\n");
            }
        }
        buffer.append("\n*").append(threadIds.length).append(" live threads*\n\n");
    }

    @Override
    public void sayOperatingSystem() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        buffer.append("### Operating System Metrics\n\n");
        buffer.append("| Metric | Value |\n| --- | --- |\n");
        buffer.append("| OS Name | `").append(osBean.getName()).append("` |\n");
        buffer.append("| OS Version | `").append(osBean.getVersion()).append("` |\n");
        buffer.append("| OS Architecture | `").append(osBean.getArch()).append("` |\n");
        buffer.append("| Available Processors | `").append(osBean.getAvailableProcessors()).append("` |\n");

        double loadAvg = osBean.getSystemLoadAverage();
        buffer.append("| System Load Average | `").append(loadAvg >= 0 ? String.format("%.2f", loadAvg) : "N/A").append("` |\n");

        // Extended metrics if available
        try {
            var sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            buffer.append("| Process CPU Load | `").append(String.format("%.1f%%", sunOsBean.getProcessCpuLoad() * 100)).append("` |\n");
            buffer.append("| Total Physical Memory | `").append(sunOsBean.getTotalPhysicalMemorySize() / (1024 * 1024)).append(" MB` |\n");
            buffer.append("| Free Physical Memory | `").append(sunOsBean.getFreePhysicalMemorySize() / (1024 * 1024)).append(" MB` |\n");
        } catch (ClassCastException e) {
            // Extended metrics not available
        }
        buffer.append("\n");
    }

    @Override
    public void sayTweetable(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String tweet = text.length() > 280 ? text.substring(0, 277) + "..." : text;
        tweetables.add(tweet);
    }

    @Override
    public void sayTldr(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        this.tldr = text;
        wordCount += text.split("\\s+").length;
    }

    @Override
    public void sayCallToAction(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        this.cta = url;
    }

    @Override
    public void sayHeroImage(String altText) {
        // Blog platform templates handle hero images differently
        if (altText == null || altText.isEmpty()) {
            return;
        }
        buffer.insert(0, template.heroImage(altText) + "\n\n");
    }

    @Override
    public void saySlideOnly(String text) {
        // Ignored for blog output
    }

    @Override
    public void sayDocOnly(String text) {
        // Render in blog posts
        say(text);
    }

    @Override
    public void saySpeakerNote(String text) {
        // Ignored for blog output
    }

    @Override
    public void finishAndWriteOut() {
        // Write blog post markdown
        writeBlogPost();

        // Write social queue
        SocialQueueWriter.writeSocialQueue(fileName, tweetables, tldr, cta);

        logger.info("Generated blog post for {} to {}/{}/{}.md",
            fileName, BASE_DIR, template.platformName(), fileName);
    }

    private void writeBlogPost() {
        try {
            File dir = new File(BASE_DIR + "/" + template.platformName());
            dir.mkdirs();

            File outFile = new File(dir, fileName + ".md");

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {

                // Write front matter
                BlogTemplate.BlogMetadata meta = new BlogTemplate.BlogMetadata(
                    fileName,
                    "DTR",
                    tldr.isEmpty() ? "API Documentation" : tldr,
                    java.time.LocalDate.now().toString(),
                    wordCount,
                    fileName,
                    "https://github.com/seanchatmangpt/dtr",
                    Arrays.asList("api", "testing", "documentation")
                );

                writer.write(template.frontMatter(meta));
                writer.write("\n\n");

                // Write TLDR if present
                if (!tldr.isEmpty()) {
                    writer.write("> **TL;DR** ");
                    writer.write(tldr);
                    writer.write("\n\n");
                }

                // Write content
                writer.write(buffer.toString());

                // Write CTA if present
                if (!cta.isEmpty()) {
                    writer.write("\n\n### Learn More\n\n");
                    writer.write(template.formatCallToAction("Read Full Documentation", cta));
                }
            }
        } catch (IOException e) {
            logger.error("Failed to write blog post", e);
            throw new RuntimeException(e);
        }
    }
}
