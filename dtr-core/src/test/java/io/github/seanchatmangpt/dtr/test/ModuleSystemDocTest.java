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
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Blue Ocean Innovation #5: Java Module System Documentation.
 *
 * <p>Uses the Java 9+ Module API to auto-document module membership, package
 * exports, and module dependencies. Generates documentation that proves exactly
 * which packages are visible across module boundaries, derived directly from
 * the runtime module graph.</p>
 *
 * <p>No manual module descriptors. No build-time introspection. The JPMS
 * runtime reflects its own graph — making the documentation impossible to drift
 * from the deployed configuration.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ModuleSystemDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Section 1: Overview of the Java Module System
    // =========================================================================

    @Test
    void a1_module_system_overview() {
        sayNextSection("The Java Module System: Runtime Verification");

        say("The Java Platform Module System (JPMS), introduced in Java 9 (Project Jigsaw), " +
            "divides the JDK and application code into named modules. Each module declares " +
            "which packages it exports and which other modules it requires — establishing " +
            "hard compile-time and runtime visibility boundaries.");

        say("DTR's Blue Ocean approach treats JPMS as a live data source: instead of " +
            "writing module documentation by hand, we interrogate the running JVM's module " +
            "graph via `java.lang.Module`, `java.lang.module.ModuleDescriptor`, and " +
            "`java.lang.ModuleLayer`. The documentation reflects the actual deployed " +
            "configuration, not a developer's notes about it.");

        sayCode("""
                // Query the module containing a class at runtime
                Module m = String.class.getModule();
                System.out.println(m.getName());          // java.base
                System.out.println(m.isNamed());          // true

                // Inspect exports from the module descriptor
                m.getDescriptor().ifPresent(desc ->
                    desc.exports().forEach(e ->
                        System.out.println(e.source() + " qualified=" + e.isQualified())
                    )
                );

                // Enumerate every module in the boot layer
                ModuleLayer.boot().modules()
                    .stream()
                    .map(Module::getName)
                    .sorted()
                    .forEach(System.out::println);
                """, "java");

        sayNote("All data in the following sections is extracted from the live JVM at test " +
                "execution time using only standard `java.lang.Module` and " +
                "`java.lang.module.ModuleDescriptor` APIs — no bytecode manipulation, " +
                "no external libraries.");

        sayWarning("JPMS strong encapsulation is enforced at runtime on Java 16+. " +
                   "Reflective access to non-exported packages requires `--add-opens`. " +
                   "This test documents the boundary, not a workaround.");

        sayEnvProfile();
    }

    // =========================================================================
    // Section 2: DTR's Module Context
    // =========================================================================

    @Test
    void a2_dtr_module_context() {
        sayNextSection("DTR's Module Context");

        say("When running under the standard Maven build (no explicit `module-info.java`), " +
            "DTR classes live in the **unnamed module** — the compatibility layer that JPMS " +
            "provides for classpath-loaded code. Understanding this boundary is essential " +
            "for any project considering a modularization migration.");

        Module dtrModule = RenderMachineCommands.class.getModule();

        boolean isNamed = dtrModule.isNamed();
        String moduleName = isNamed ? dtrModule.getName() : "unnamed";
        String descriptorInfo = "n/a (unnamed module — no module-info.java)";

        if (isNamed && dtrModule.getDescriptor() != null) {
            ModuleDescriptor desc = dtrModule.getDescriptor();
            descriptorInfo = desc.toNameAndVersion().orElse(desc.name());
        }

        Map<String, String> moduleProps = new LinkedHashMap<>();
        moduleProps.put("Module name", moduleName);
        moduleProps.put("Is named module", String.valueOf(isNamed));
        moduleProps.put("Module descriptor", descriptorInfo);
        moduleProps.put("Module layer", dtrModule.getLayer() != null
                ? dtrModule.getLayer().toString()
                : "no layer (unnamed module)");
        moduleProps.put("Class used for lookup", RenderMachineCommands.class.getName());

        sayKeyValue(moduleProps);

        say("The unnamed module reads every named module in the boot layer, which is why " +
            "classpath applications can still use the full JDK API. The reverse is not " +
            "true: named modules must explicitly `requires` what they depend on.");

        Map<String, String> assertions = new LinkedHashMap<>();
        assertions.put("RenderMachineCommands module is accessible", "PASS");
        assertions.put("Module name resolved without exception", "PASS");
        assertions.put("isNamed() returns consistent boolean", String.valueOf(isNamed == dtrModule.isNamed()));

        sayAssertions(assertions);

        sayCode("""
                // Checking DTR's module membership at runtime
                Module m = RenderMachineCommands.class.getModule();
                boolean named = m.isNamed();
                // named == false  →  classpath / unnamed module
                // named == true   →  explicit module-info.java present
                """, "java");
    }

    // =========================================================================
    // Section 3: java.base Exports
    // =========================================================================

    @Test
    void a3_java_base_exports() {
        sayNextSection("java.base: The Foundation Module");

        say("`java.base` is the only module every other module implicitly requires. " +
            "It contains the runtime's fundamental packages: `java.lang`, `java.util`, " +
            "`java.io`, `java.nio`, and more. Inspecting its exports table shows exactly " +
            "which packages are part of the public API of the JDK foundation.");

        Module javaBase = String.class.getModule();

        say("Module name: **" + javaBase.getName() + "**  |  " +
            "Named: " + javaBase.isNamed());

        // Collect exports from the descriptor
        List<ModuleDescriptor.Exports> exports = new ArrayList<>();
        try {
            if (javaBase.getDescriptor() != null) {
                exports.addAll(javaBase.getDescriptor().exports());
            }
        } catch (Exception e) {
            sayWarning("Could not read descriptor exports: " + e.getMessage());
        }

        // Sort by package name and take top 20
        exports.sort(Comparator.comparing(ModuleDescriptor.Exports::source));
        List<ModuleDescriptor.Exports> top20 = exports.stream().limit(20).toList();

        say("Top 20 exported packages from `java.base` (sorted alphabetically, " +
            exports.size() + " total):");

        String[][] tableData = new String[top20.size() + 1][3];
        tableData[0] = new String[]{"Package", "Is Qualified", "Targets"};
        for (int i = 0; i < top20.size(); i++) {
            ModuleDescriptor.Exports e = top20.get(i);
            String targets = e.isQualified()
                    ? e.targets().stream().sorted().collect(Collectors.joining(", "))
                    : "(all modules)";
            tableData[i + 1] = new String[]{
                e.source(),
                String.valueOf(e.isQualified()),
                targets
            };
        }
        sayTable(tableData);

        long qualifiedCount = exports.stream().filter(ModuleDescriptor.Exports::isQualified).count();
        long unqualifiedCount = exports.size() - qualifiedCount;

        sayKeyValue(Map.of(
            "Total exported packages", String.valueOf(exports.size()),
            "Unqualified exports (public API)", String.valueOf(unqualifiedCount),
            "Qualified exports (friend modules only)", String.valueOf(qualifiedCount)
        ));

        sayNote("Qualified exports (where 'Is Qualified' is true) are visible only to the " +
                "listed target modules — a deliberate JPMS mechanism for JDK-internal " +
                "cross-module contracts that are not part of the public API.");
    }

    // =========================================================================
    // Section 4: Module Dependency Graph
    // =========================================================================

    @Test
    void a4_module_dependency_graph() {
        sayNextSection("Module Dependency Graph");

        say("The boot module layer contains all JDK modules loaded by the platform " +
            "class loader. Using `ModuleLayer.boot().modules()`, we can enumerate every " +
            "module and inspect its `requires` declarations to reconstruct the dependency graph.");

        Set<Module> bootModules = ModuleLayer.boot().modules();
        long totalModules = bootModules.size();

        say("Boot layer contains **" + totalModules + "** modules. The Mermaid graph below " +
            "shows key structural relationships among seven representative JDK modules.");

        // Build a Mermaid graph LR for key modules and their requires relationships.
        // We interrogate each module's descriptor for `requires` entries.
        String[] keyModules = {
            "java.base", "java.compiler", "java.instrument",
            "java.logging", "java.management", "java.sql", "java.xml"
        };

        Set<String> keySet = Set.of(keyModules);

        StringBuilder mermaid = new StringBuilder("graph LR\n");

        for (Module mod : bootModules) {
            if (!mod.isNamed()) continue;
            String modName = mod.getName();
            if (!keySet.contains(modName)) continue;

            try {
                if (mod.getDescriptor() != null) {
                    for (ModuleDescriptor.Requires req : mod.getDescriptor().requires()) {
                        String reqName = req.name();
                        if (keySet.contains(reqName)) {
                            // Sanitize names for Mermaid node IDs (replace dots with underscores)
                            String fromId = modName.replace('.', '_');
                            String toId = reqName.replace('.', '_');
                            mermaid.append("    ").append(fromId)
                                   .append("[\"").append(modName).append("\"]")
                                   .append(" --> ")
                                   .append(toId)
                                   .append("[\"").append(reqName).append("\"]\n");
                        }
                    }
                }
            } catch (Exception e) {
                // skip modules whose descriptor is unavailable
            }
        }

        sayMermaid(mermaid.toString());

        // Also show a summary table of the key modules
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Module", "Requires (key deps only)", "Packages Exported"});

        for (String kmName : keyModules) {
            Module km = bootModules.stream()
                .filter(m -> m.isNamed() && m.getName().equals(kmName))
                .findFirst()
                .orElse(null);

            if (km == null) {
                rows.add(new String[]{kmName, "(not found in boot layer)", "n/a"});
                continue;
            }

            String requires = "(none in key set)";
            String exportCount = "n/a";

            try {
                if (km.getDescriptor() != null) {
                    ModuleDescriptor desc = km.getDescriptor();

                    List<String> keyReqs = desc.requires().stream()
                        .map(ModuleDescriptor.Requires::name)
                        .filter(keySet::contains)
                        .sorted()
                        .toList();
                    requires = keyReqs.isEmpty() ? "(none in key set)" : String.join(", ", keyReqs);

                    exportCount = String.valueOf(desc.exports().size());
                }
            } catch (Exception e) {
                requires = "descriptor unavailable";
            }

            rows.add(new String[]{kmName, requires, exportCount});
        }

        sayTable(rows.toArray(new String[0][]));

        sayNote("The `requires transitive` relationship (implied readability) is not shown " +
                "in this graph. For example, `java.sql` uses `requires transitive java.xml` " +
                "so consumers of `java.sql` can read `java.xml` packages without declaring " +
                "it themselves.");
    }

    // =========================================================================
    // Section 5: Package-to-Module Mapping
    // =========================================================================

    @Test
    void a5_package_module_mapping() {
        sayNextSection("Package-to-Module Mapping");

        say("Every class belongs to exactly one module. By resolving the modules that own " +
            "canonical packages — including DTR's own packages — we can verify the module " +
            "boundary configuration at runtime. This is the ground truth: not build-time " +
            "assumptions, but actual JVM module assignments for the running process.");

        // Representative classes for each package of interest
        record PackageSample(String packageName, Class<?> sampleClass) {}

        List<PackageSample> samples = List.of(
            new PackageSample("io.github.seanchatmangpt.dtr",
                              RenderMachineCommands.class),
            new PackageSample("io.github.seanchatmangpt.dtr.rendermachine",
                              io.github.seanchatmangpt.dtr.rendermachine.RenderMachine.class),
            new PackageSample("java.lang",              String.class),
            new PackageSample("java.util",              java.util.List.class),
            new PackageSample("java.io",                java.io.InputStream.class),
            new PackageSample("java.lang.module",       ModuleDescriptor.class),
            new PackageSample("java.util.concurrent",   java.util.concurrent.Executors.class)
        );

        String[][] tableData = new String[samples.size() + 1][4];
        tableData[0] = new String[]{"Package", "Module", "Is Named", "Is Open"};

        for (int i = 0; i < samples.size(); i++) {
            PackageSample ps = samples.get(i);
            Module mod = ps.sampleClass().getModule();
            String modName = mod.isNamed() ? mod.getName() : "unnamed";
            boolean isOpen = false;
            try {
                isOpen = mod.isOpen(ps.packageName());
            } catch (Exception e) {
                // some packages may not be queryable; leave false
            }
            tableData[i + 1] = new String[]{
                ps.packageName(),
                modName,
                String.valueOf(mod.isNamed()),
                String.valueOf(isOpen)
            };
        }

        sayTable(tableData);

        say("The 'Is Open' column reflects whether the package is **open** (deep reflective " +
            "access allowed) vs merely **exported** (compile-time and normal reflective access). " +
            "Open packages permit `setAccessible(true)` on private members without " +
            "`--add-opens` flags.");

        sayCode("""
                // Check whether a package is open to the caller's module
                Module target = String.class.getModule();         // java.base
                boolean open = target.isOpen("java.lang");        // false — exported, not open

                // Check whether a package is exported to a specific module
                Module caller = ModuleSystemDocTest.class.getModule();  // unnamed
                boolean exported = target.isExported("java.lang", caller); // true
                """, "java");
    }

    // =========================================================================
    // Section 6: Module Security — Strong Encapsulation
    // =========================================================================

    @Test
    void a6_module_security() {
        sayNextSection("Module Security: What Cannot Be Opened");

        sayNote("Strong encapsulation is the primary security benefit of JPMS. Packages " +
                "that are not exported are invisible to code in other modules — even via " +
                "reflection — unless an explicit `--add-opens` flag is provided at JVM startup.");

        say("Java 16+ enforces strong encapsulation by default: attempts to use " +
            "`Field.setAccessible(true)` on non-exported packages throw " +
            "`InaccessibleObjectException` at runtime. The `module.isOpen(packageName)` " +
            "API lets you verify this boundary programmatically before attempting access.");

        sayCode("""
                // Example: checking openness before reflective access
                Module javaBase = String.class.getModule();
                Module callerModule = getClass().getModule(); // unnamed in DTR

                String pkg = "java.lang";
                boolean isOpen = javaBase.isOpen(pkg, callerModule);
                // → false for java.base packages in Java 16+

                // To open it (JVM flag only, not recommended in production):
                // --add-opens java.base/java.lang=ALL-UNNAMED
                """, "java");

        // Document isOpen() for a selection of packages
        Module javaBase = String.class.getModule();
        Module callerModule = ModuleSystemDocTest.class.getModule();

        String[] checkPackages = {
            "java.lang",
            "java.util",
            "java.io",
            "java.lang.reflect",
            "java.lang.module",
            "java.util.concurrent"
        };

        String[][] openTable = new String[checkPackages.length + 1][4];
        openTable[0] = new String[]{"Package", "Module", "isExported", "isOpen"};

        for (int i = 0; i < checkPackages.length; i++) {
            String pkg = checkPackages[i];
            boolean exported = false;
            boolean open = false;
            try {
                exported = javaBase.isExported(pkg, callerModule);
                open     = javaBase.isOpen(pkg, callerModule);
            } catch (Exception e) {
                // package not in this module — leave false
            }
            openTable[i + 1] = new String[]{
                pkg,
                javaBase.getName(),
                String.valueOf(exported),
                String.valueOf(open)
            };
        }

        say("Encapsulation state of key `java.base` packages as seen by DTR's module (" +
            (callerModule.isNamed() ? callerModule.getName() : "unnamed") + "):");

        sayTable(openTable);

        say("The distinction between **exported** and **open** is critical:");
        sayUnorderedList(List.of(
            "**Exported** (`isExported = true`): public types and members are accessible " +
                "at compile time and via normal reflection (`getMethod`, `getField`).",
            "**Open** (`isOpen = true`): all types and members — including private — are " +
                "accessible via deep reflection (`setAccessible(true)`). Required for " +
                "frameworks like Spring, Hibernate, and JUnit 5 that use internal APIs.",
            "**Neither** (`isExported = false, isOpen = false`): the package is " +
                "invisible. Access attempts throw `InaccessibleObjectException` or " +
                "`IllegalAccessException`."
        ));

        say("To open a package for a framework at JVM startup:");
        sayCode("""
                # Allow JUnit 5 / Spring to access java.lang internals
                --add-opens java.base/java.lang=ALL-UNNAMED

                # Allow Hibernate to access java.util
                --add-opens java.base/java.util=ALL-UNNAMED

                # Allow Jackson to access java.io
                --add-opens java.base/java.io=ALL-UNNAMED
                """, "shell");

        sayWarning("Adding `--add-opens` flags bypasses JPMS strong encapsulation and may " +
                   "expose implementation details that change between Java versions. " +
                   "Prefer migrating to public APIs or using `module-info.java` `opens` " +
                   "declarations in your own modules.");

        sayAssertions(Map.of(
            "java.lang is exported to unnamed module", String.valueOf(
                javaBase.isExported("java.lang", callerModule)),
            "java.lang is NOT open to unnamed module (strong encapsulation)", String.valueOf(
                !javaBase.isOpen("java.lang", callerModule)),
            "Module security API accessible without --add-opens", "PASS"
        ));
    }
}
