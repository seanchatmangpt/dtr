/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.reflectiontoolkit.ModuleDescriptorRenderer;
import io.github.seanchatmangpt.dtr.reflectiontoolkit.ModuleDescriptorRenderer.ModuleReport;
import io.github.seanchatmangpt.dtr.reflectiontoolkit.ModuleDescriptorRenderer.ModuleSection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;

/**
 * Documentation test for {@code sayModuleDescriptor} — the DTR blue-ocean
 * innovation that renders JPMS module contracts as verified documentation.
 *
 * <p>No other documentation tool auto-documents {@code requires}, {@code exports},
 * {@code opens}, {@code uses}, and {@code provides} directives from live
 * {@link java.lang.module.ModuleDescriptor} objects. DTR does.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ModuleDescriptorDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    @Test
    void a1_sayModuleDescriptor_javaBase() {
        sayNextSection("sayModuleDescriptor — JPMS Module Contract Documentation");

        say("DTR can document the complete JPMS module descriptor for any class. "
                + "This makes module boundaries and service contracts "
                + "*part of the verified documentation*.");

        say("The feature uses `java.lang.module.ModuleDescriptor` — available on any "
                + "Java 9+ runtime — and structures all five JPMS directive types into "
                + "sortable tables: `requires`, `exports`, `opens`, `uses`, and `provides`.");

        ModuleReport report = ModuleDescriptorRenderer.render(String.class);

        sayAndAssertThat("String is in a named module", report.moduleName(), notNullValue());
        sayAndAssertThat("Module name is java.base",
                report.moduleName(), is("java.base"));

        sayTable(new String[][]{
                {"Property", "Value"},
                {"Module", report.moduleName()},
                {"Version", report.version()},
                {"Open", String.valueOf(report.isOpen())},
                {"Automatic", String.valueOf(report.isAutomatic())},
                {"Directive sections", String.valueOf(report.sections().size())}
        });

        for (ModuleSection section : report.sections()) {
            sayNextSection("Directive: " + section.directive());

            if (section.rows().isEmpty()) {
                say("(none)");
                continue;
            }

            String[] header = switch (section.directive()) {
                case "requires" -> new String[]{"Module", "Modifiers"};
                case "exports", "opens" -> new String[]{"Package", "To"};
                case "uses" -> new String[]{"Service"};
                case "provides" -> new String[]{"Service", "Providers"};
                default -> new String[]{"Value"};
            };

            // Cap table rows to avoid enormous output for java.base exports (500+ packages)
            int maxRows = 20;
            var rows = section.rows();
            int displayCount = Math.min(rows.size(), maxRows);

            String[][] table = new String[displayCount + 1][];
            table[0] = header;
            for (int i = 0; i < displayCount; i++) {
                table[i + 1] = rows.get(i);
            }
            sayTable(table);

            if (rows.size() > maxRows) {
                sayNote("Showing first %d of %d %s directives."
                        .formatted(maxRows, rows.size(), section.directive()));
            }
        }

        sayNote("JPMS module descriptor is read via java.lang.module.ModuleDescriptor — "
                + "works for any named module on the module path.");
    }

    @Test
    void a2_sayModuleDescriptor_unnamedModule() {
        sayNextSection("sayModuleDescriptor — Unnamed Module Handling");

        say("Classes loaded from the class-path (not the module-path) live in the "
                + "unnamed module. DTR reports this gracefully rather than throwing "
                + "an exception.");

        ModuleReport report = ModuleDescriptorRenderer.render(DtrTest.class);

        sayTable(new String[][]{
                {"Property", "Value"},
                {"Module", report.moduleName()},
                {"Version", report.version()},
                {"Sections", String.valueOf(report.sections().size())}
        });

        // For unnamed modules the renderer returns exactly one informational section
        boolean isUnnamed = "unnamed module".equals(report.moduleName());
        if (isUnnamed) {
            sayAndAssertThat("unnamed module returns informational section",
                    report.sections().size(), is(1));
            say("DtrTest is on the class-path — the unnamed module note was returned correctly.");
        } else {
            say("DtrTest is on the module-path in module `" + report.moduleName() + "`.");
            sayAndAssertThat("module name is non-null", report.moduleName(), notNullValue());
        }

        sayNote("The unnamed-module code path avoids NullPointerExceptions that would "
                + "otherwise occur when calling getDescriptor() on an unnamed module.");
    }

    @Test
    void a3_sayModuleDescriptor_javaUtilModule() {
        sayNextSection("sayModuleDescriptor — java.base via java.util.List");

        say("Demonstrates that any class from the same named module yields the "
                + "same `ModuleReport`. Here we use `java.util.List` to confirm that "
                + "both `String` and `List` share the `java.base` module.");

        ModuleReport fromString = ModuleDescriptorRenderer.render(String.class);
        ModuleReport fromList = ModuleDescriptorRenderer.render(java.util.List.class);

        sayAndAssertThat("String and List are in the same module",
                fromString.moduleName(), is(fromList.moduleName()));

        sayTable(new String[][]{
                {"Class", "Module", "Version"},
                {"java.lang.String", fromString.moduleName(), fromString.version()},
                {"java.util.List", fromList.moduleName(), fromList.version()}
        });
    }
}
