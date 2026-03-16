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
package io.github.seanchatmangpt.dtr.render.slides;

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
import io.github.seanchatmangpt.dtr.evolution.GitHistoryReader;
import io.github.seanchatmangpt.dtr.javadoc.JavadocEntry;
import io.github.seanchatmangpt.dtr.javadoc.JavadocIndex;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slide deck render machine generating presentation slides.
 *
 * Converts test execution into presentation slides (Reveal.js HTML, etc.).
 * Maps say* methods to slide content: sayNextSection → new slide, say → bullets, etc.
 */
public final class SlideRenderMachine extends RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(SlideRenderMachine.class);
    private static final String BASE_DIR = "target/site/dtr/slides";

    private final SlideTemplate template;
    private final StringBuilder slideBuffer = new StringBuilder();
    private final List<String> speakerNotes = new ArrayList<>();
    private String fileName;
    private String currentTitle = "";
    private List<String> currentBullets = new ArrayList<>();
    private String currentSpeakerNote = "";

    /**
     * Create a slide render machine with the given template.
     *
     * @param template the slide template (Reveal.js, Marp, etc.)
     */
    public SlideRenderMachine(SlideTemplate template) {
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
        // Convert paragraph to bullet point on slide
        currentBullets.add(text);
    }

    @Override
    public void sayNextSection(String heading) {
        if (heading == null || heading.isEmpty()) {
            return;
        }
        // Finish current slide if any
        if (!currentTitle.isEmpty() || !currentBullets.isEmpty()) {
            flushCurrentSlide();
        }
        // Start new section
        currentTitle = heading;
        currentBullets = new ArrayList<>();
        slideBuffer.append(template.formatSectionSlide(heading));
    }

    @Override
    public void sayRaw(String markdown) {
        // For slides, skip raw markdown (not applicable)
    }

    @Override
    public void sayTable(String[][] data) {
        flushCurrentSlide();
        slideBuffer.append(template.formatTableSlide(data));
    }

    @Override
    public void sayCode(String code, String language) {
        flushCurrentSlide();
        slideBuffer.append(template.formatCodeSlide(code, language));
    }

    @Override
    public void sayWarning(String message) {
        flushCurrentSlide();
        slideBuffer.append(template.formatNoteSlide(message, "warning"));
    }

    @Override
    public void sayNote(String message) {
        flushCurrentSlide();
        slideBuffer.append(template.formatNoteSlide(message, "info"));
    }

    @Override
    public void sayKeyValue(Map<String, String> pairs) {
        // Render as table slide
        if (pairs != null && !pairs.isEmpty()) {
            String[][] data = new String[pairs.size() + 1][2];
            data[0] = new String[]{"Key", "Value"};
            int row = 1;
            for (var entry : pairs.entrySet()) {
                data[row][0] = entry.getKey();
                data[row][1] = entry.getValue();
                row++;
            }
            sayTable(data);
        }
    }

    @Override
    public void sayUnorderedList(List<String> items) {
        if (items != null) {
            currentBullets.addAll(items);
        }
    }

    @Override
    public void sayOrderedList(List<String> items) {
        // For slides, treat ordered list as bullets
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                currentBullets.add((i + 1) + ". " + items.get(i));
            }
        }
    }

    @Override
    public void sayJson(Object object) {
        flushCurrentSlide();
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(object);
            slideBuffer.append(template.formatCodeSlide(json, "json"));
        } catch (Exception e) {
            logger.warn("Could not serialize object to JSON", e);
        }
    }

    @Override
    public void sayAssertions(Map<String, String> assertions) {
        if (assertions != null && !assertions.isEmpty()) {
            String[][] data = new String[assertions.size() + 1][2];
            data[0] = new String[]{"Check", "Result"};
            int row = 1;
            for (var entry : assertions.entrySet()) {
                data[row][0] = entry.getKey();
                data[row][1] = entry.getValue();
                row++;
            }
            sayTable(data);
        }
    }

    @Override
    public void sayRef(DocTestRef ref) {
        if (ref != null) {
            currentBullets.add("See " + ref.anchor());
        }
    }

    @Override
    public void sayCite(String citationKey) {
        if (citationKey != null && !citationKey.isEmpty()) {
            currentBullets.add("[" + citationKey + "]");
        }
    }

    @Override
    public void sayCite(String citationKey, String pageRef) {
        if (citationKey != null && !citationKey.isEmpty()) {
            currentBullets.add("[" + citationKey + " p. " + pageRef + "]");
        }
    }

    @Override
    public void sayFootnote(String text) {
        if (text != null && !text.isEmpty()) {
            currentBullets.add("* " + text);
        }
    }

    @Override
    public void sayCodeModel(Class<?> clazz) {
        if (clazz == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Class: " + clazz.getSimpleName()));
        currentBullets.add("Package: `" + clazz.getPackageName() + "`");
        currentBullets.add("Type: " + (clazz.isInterface() ? "Interface" : clazz.isRecord() ? "Record" : "Class"));
        flushCurrentSlide();
    }

    @Override
    public void sayCodeModel(java.lang.reflect.Method method) {
        if (method == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Method: " + method.getName()));
        currentBullets.add("Return: `" + method.getReturnType().getSimpleName() + "`");
        currentBullets.add("Parameters: " + method.getParameterCount());
        flushCurrentSlide();
    }

    @Override
    public void sayMethodSignature(java.lang.reflect.Method method) {
        // For slide output, delegate to sayCodeModel(Method) for consistent rendering
        sayCodeModel(method);
    }

    @Override
    public void sayCallSite() {
        var walker = java.lang.StackWalker.getInstance(java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE);
        walker.walk(frames -> {
            frames.skip(2).findFirst().ifPresent(frame -> {
                currentBullets.add("📍 " + frame.getClassName() + "#" + frame.getMethodName());
            });
            return null;
        });
    }

    @Override
    public void sayAnnotationProfile(Class<?> clazz) {
        if (clazz == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Annotations: " + clazz.getSimpleName()));
        for (var a : clazz.getAnnotations()) {
            currentBullets.add("`@" + a.annotationType().getSimpleName() + "`");
        }
        flushCurrentSlide();
    }

    @Override
    public void sayClassHierarchy(Class<?> clazz) {
        if (clazz == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Hierarchy: " + clazz.getSimpleName()));
        List<String> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            hierarchy.add(current.getSimpleName());
            current = current.getSuperclass();
        }
        currentBullets.add(String.join(" → ", hierarchy));
        flushCurrentSlide();
    }

    @Override
    public void sayStringProfile(String text) {
        if (text == null || text.isEmpty()) return;
        currentBullets.add("String: " + text.length() + " chars, " + text.split("\\s+").length + " words");
    }

    @Override
    public void sayReflectiveDiff(Object before, Object after) {
        if (before == null || after == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Diff"));
        currentBullets.add("Comparing " + before.getClass().getSimpleName());
        flushCurrentSlide();
    }

    @Override
    public void sayControlFlowGraph(java.lang.reflect.Method method) {
        if (method == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("CFG: " + method.getName()));
        currentBullets.add("Requires @CodeReflection annotation");
        flushCurrentSlide();
    }

    @Override
    public void sayCallGraph(Class<?> clazz) {
        if (clazz == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Call Graph: " + clazz.getSimpleName()));
        currentBullets.add("Requires @CodeReflection annotation");
        flushCurrentSlide();
    }

    @Override
    public void sayOpProfile(java.lang.reflect.Method method) {
        if (method == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Op Profile: " + method.getName()));
        currentBullets.add("Requires @CodeReflection annotation");
        flushCurrentSlide();
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

        Arrays.sort(times);
        double avg = Arrays.stream(times).average().orElse(0);

        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Benchmark: " + label));
        currentBullets.add(String.format("Average: %.0f ns", avg));
        currentBullets.add(String.format("P99: %d ns", times[(int)(measureRounds * 0.99)]));
        flushCurrentSlide();
    }

    @Override
    public void sayMermaid(String diagramDsl) {
        if (diagramDsl == null || diagramDsl.isEmpty()) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatCodeSlide(diagramDsl, "mermaid"));
    }

    @Override
    public void sayClassDiagram(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Class Diagram"));
        for (Class<?> c : classes) {
            if (c != null) currentBullets.add(c.getSimpleName());
        }
        flushCurrentSlide();
    }

    @Override
    public void sayDocCoverage(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Doc Coverage"));
        for (Class<?> c : classes) {
            if (c != null) currentBullets.add(c.getSimpleName());
        }
        flushCurrentSlide();
    }

    @Override
    public void sayEnvProfile() {
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Environment"));
        currentBullets.add("Java: `" + System.getProperty("java.version") + "`");
        currentBullets.add("OS: `" + System.getProperty("os.name") + "`");
        currentBullets.add("Processors: `" + Runtime.getRuntime().availableProcessors() + "`");
        flushCurrentSlide();
    }

    @Override
    public void sayRecordComponents(Class<? extends Record> recordClass) {
        if (recordClass == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Record: " + recordClass.getSimpleName()));
        var components = recordClass.getRecordComponents();
        if (components != null) {
            for (var comp : components) {
                currentBullets.add(comp.getName() + ": `" + comp.getType().getSimpleName() + "`");
            }
        }
        flushCurrentSlide();
    }

    @Override
    public void sayException(Throwable t) {
        if (t == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatNoteSlide(
            t.getClass().getSimpleName() + ": " + t.getMessage(), "error"));
    }

    @Override
    public void sayAsciiChart(String label, double[] values, String[] xLabels) {
        if (label == null || values == null || values.length == 0) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide(label));

        double max = Arrays.stream(values).max().orElse(1.0);
        if (max == 0) max = 1.0;
        int barWidth = 15;

        for (int i = 0; i < values.length; i++) {
            String rowLabel = (xLabels != null && i < xLabels.length) ? xLabels[i] : ("" + i);
            int filled = (int) Math.round((values[i] / max) * barWidth);
            String bar = "█".repeat(filled) + "░".repeat(barWidth - filled);
            currentBullets.add(rowLabel + ": " + bar);
        }
        flushCurrentSlide();
    }

    @Override
    public void sayContractVerification(Class<?> contract, Class<?>... implementations) {
        if (contract == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Contract: " + contract.getSimpleName()));
        if (implementations != null) {
            for (Class<?> impl : implementations) {
                if (impl != null) currentBullets.add(impl.getSimpleName());
            }
        }
        flushCurrentSlide();
    }

    @Override
    public void sayEvolutionTimeline(Class<?> clazz, int maxEntries) {
        if (clazz == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Evolution: " + clazz.getSimpleName()));

        int limit = maxEntries > 0 ? maxEntries : 10;
        var entries = GitHistoryReader.read(clazz, limit);

        if (entries.isEmpty()) {
            currentBullets.add("Git history unavailable");
        } else {
            for (var entry : entries) {
                currentBullets.add("`" + entry.hash() + "` " + entry.date() + " — " + entry.subject());
            }
            currentBullets.add("*" + entries.size() + " commits*");
        }
        flushCurrentSlide();
    }

    @Override
    public void sayJavadoc(java.lang.reflect.Method method) {
        if (method == null) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Javadoc: " + method.getName()));

        var entry = JavadocIndex.lookup(method);

        if (entry.isEmpty()) {
            currentBullets.add("Javadoc not available");
        } else {
            JavadocEntry je = entry.get();

            // Description
            if (je.description() != null && !je.description().isEmpty()) {
                currentBullets.add(je.description());
            }

            // Parameters
            if (je.params() != null && !je.params().isEmpty()) {
                currentBullets.add("**Parameters:**");
                for (var param : je.params()) {
                    currentBullets.add("- `" + param.name() + "`: " + param.description());
                }
            }

            // Returns
            if (je.returns() != null && !je.returns().isEmpty()) {
                currentBullets.add("**Returns:** " + je.returns());
            }
        }
        flushCurrentSlide();
    }

    @Override
    public void saySystemProperties() {
        saySystemProperties(null);
    }

    @Override
    public void saySystemProperties(String regexFilter) {
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("System Properties"));

        var props = System.getProperties();
        var entryStream = props.entrySet().stream();
        if (regexFilter != null && !regexFilter.isBlank()) {
            var pattern = java.util.regex.Pattern.compile(regexFilter);
            var predicate = pattern.asPredicate();
            entryStream = entryStream.filter(e -> predicate.test(e.getKey().toString()));
        }

        var sortedEntries = entryStream
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .limit(10)
                .toList();

        for (var entry : sortedEntries) {
            currentBullets.add("`" + entry.getKey() + "`");
        }
        if (props.size() > 10) {
            currentBullets.add("... " + (props.size() - 10) + " more");
        }
        flushCurrentSlide();
    }

    @Override
    public void saySecurityManager() {
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Security"));

        var sm = System.getSecurityManager();
        currentBullets.add("Security Manager: " + (sm != null ? "Present" : "Absent"));

        var providers = java.security.Security.getProviders();
        for (int i = 0; i < Math.min(3, providers.length); i++) {
            currentBullets.add(providers[i].getName());
        }
        flushCurrentSlide();
    }

    @Override
    public void sayModuleDependencies(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Module Dependencies"));

        Map<Module, List<Class<?>>> moduleMap = Arrays.stream(classes)
                .filter(clazz -> clazz != null)
                .collect(Collectors.groupingBy(Class::getModule, LinkedHashMap::new, Collectors.toList()));

        for (var entry : moduleMap.entrySet()) {
            Module module = entry.getKey();
            String moduleName = module.isNamed() ? module.getName() : "Unnamed";
            currentBullets.add(moduleName);
        }
        flushCurrentSlide();
    }

    @Override
    public void sayThreadDump() {
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Threads"));

        var threadMXBean = ManagementFactory.getThreadMXBean();
        currentBullets.add("Thread Count: " + threadMXBean.getThreadCount());
        currentBullets.add("Daemon Threads: " + threadMXBean.getDaemonThreadCount());
        currentBullets.add("Peak Threads: " + threadMXBean.getPeakThreadCount());
        flushCurrentSlide();
    }

    @Override
    public void sayOperatingSystem() {
        flushCurrentSlide();
        slideBuffer.append(template.formatSectionSlide("Operating System"));

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        currentBullets.add("OS: " + osBean.getName() + " " + osBean.getVersion());
        currentBullets.add("Arch: " + osBean.getArch());
        currentBullets.add("Processors: " + osBean.getAvailableProcessors());
        flushCurrentSlide();
    }

    @Override
    public void sayTweetable(String text) {
        // Ignored for slides
    }

    @Override
    public void sayTldr(String text) {
        // Ignored for slides
    }

    @Override
    public void sayCallToAction(String url) {
        // Ignored for slides
    }

    @Override
    public void sayHeroImage(String altText) {
        // Ignored for Reveal.js (would need CDN URL)
    }

    @Override
    public void saySlideOnly(String text) {
        if (text != null && !text.isEmpty()) {
            currentBullets.add(text);
        }
    }

    @Override
    public void sayDocOnly(String text) {
        // Ignored for slides
    }

    @Override
    public void saySpeakerNote(String text) {
        if (text != null && !text.isEmpty()) {
            currentSpeakerNote = text;
        }
    }

    @Override
    public void finishAndWriteOut() {
        // Flush any remaining content
        flushCurrentSlide();

        // Write Reveal.js HTML
        writeSlidesDeck();

        logger.info("Generated slides for {} to {}/{}.{}",
            fileName, BASE_DIR, fileName, template.fileExtension());
    }

    private void flushCurrentSlide() {
        if (!currentTitle.isEmpty() || !currentBullets.isEmpty()) {
            slideBuffer.append(template.formatContentSlide(currentTitle, currentBullets, currentSpeakerNote));
            currentTitle = "";
            currentBullets = new ArrayList<>();
            currentSpeakerNote = "";
        }
    }

    private void writeSlidesDeck() {
        try {
            File dir = new File(BASE_DIR);
            dir.mkdirs();

            File outFile = new File(dir, fileName + "." + template.fileExtension());

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {

                // Write HTML5 Reveal.js template
                writer.write("""
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>%s</title>
                        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/dist/reveal.min.css">
                        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/dist/theme/black.min.css">
                        <style>
                            .reveal pre { width: 100%%; }
                            .reveal code { padding: 2px 8px; background: rgba(255,255,255,0.1); }
                            .highlight-warn { background: rgba(255,100,0,0.3); padding: 20px; border-radius: 5px; }
                            .highlight-error { background: rgba(255,0,0,0.3); padding: 20px; border-radius: 5px; }
                            .highlight-info { background: rgba(0,100,255,0.3); padding: 20px; border-radius: 5px; }
                        </style>
                    </head>
                    <body>
                        <div class="reveal">
                            <div class="slides">
                    """.formatted(fileName));

                // Write slide content
                writer.write(slideBuffer.toString());

                writer.write("""
                            </div>
                        </div>
                        <script src="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/dist/reveal.min.js"></script>
                        <script src="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/plugin/highlight/highlight.min.js"></script>
                        <script>
                            Reveal.initialize({
                                hash: true,
                                center: true,
                                transition: 'slide',
                                plugins: [RevealHighlight]
                            });
                        </script>
                    </body>
                    </html>
                    """);
            }
        } catch (IOException e) {
            logger.error("Failed to write slide deck", e);
            throw new RuntimeException(e);
        }
    }
}
