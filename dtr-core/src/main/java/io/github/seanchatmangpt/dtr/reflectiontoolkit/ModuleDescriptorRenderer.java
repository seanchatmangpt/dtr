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
package io.github.seanchatmangpt.dtr.reflectiontoolkit;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders JPMS module descriptor information for any class's module.
 *
 * <p>Covers all five directive types: {@code requires}, {@code exports},
 * {@code opens}, {@code uses}, and {@code provides}. Works for any named
 * module on the module path. For classes on the class-path (unnamed module)
 * a graceful note is returned instead.
 *
 * <p>No external dependencies — uses only {@code java.lang.module.ModuleDescriptor}
 * and standard Java reflection.
 *
 * @since 2026
 */
public final class ModuleDescriptorRenderer {

    private ModuleDescriptorRenderer() {}

    /**
     * One directive section (e.g., "requires") with its tabular rows.
     *
     * @param directive the JPMS directive name
     * @param rows      each row is a {@code String[]} of one or two cells
     */
    public record ModuleSection(String directive, List<String[]> rows) {}

    /**
     * Complete descriptor report for one module.
     *
     * @param moduleName  JPMS module name, or {@code "unnamed module"}
     * @param version     version string, or {@code "unversioned"}
     * @param isOpen      true when the module is declared {@code open}
     * @param isAutomatic true when the module was synthesised from a plain JAR
     * @param sections    one {@link ModuleSection} per non-empty directive
     */
    public record ModuleReport(
            String moduleName,
            String version,
            boolean isOpen,
            boolean isAutomatic,
            List<ModuleSection> sections) {}

    /**
     * Builds a {@link ModuleReport} for the module that contains {@code clazz}.
     *
     * <p>If the class lives in the unnamed module (class-path), returns a report
     * with a single informational section rather than throwing an exception.
     *
     * @param clazz any class whose module descriptor to read (must not be null)
     * @return a fully-populated {@link ModuleReport}
     */
    public static ModuleReport render(Class<?> clazz) {
        Module module = clazz.getModule();

        if (!module.isNamed()) {
            return new ModuleReport(
                    "unnamed module",
                    "unversioned",
                    false,
                    false,
                    List.of(new ModuleSection(
                            "note",
                            List.<String[]>of(new String[]{"This class is in the unnamed module (class-path)."}))));
        }

        ModuleDescriptor desc = module.getDescriptor();
        String name = desc.name();
        String version = desc.version().map(Object::toString).orElse("unversioned");
        boolean isOpen = desc.isOpen();
        boolean isAutomatic = desc.isAutomatic();

        List<ModuleSection> sections = new ArrayList<>();

        // requires
        var requires = desc.requires().stream()
                .sorted(Comparator.comparing(ModuleDescriptor.Requires::name))
                .map(r -> new String[]{
                        r.name(),
                        r.modifiers().stream()
                                .map(m -> m.toString().toLowerCase())
                                .sorted()
                                .collect(Collectors.joining(", "))})
                .toList();
        if (!requires.isEmpty()) {
            sections.add(new ModuleSection("requires", requires));
        }

        // exports
        var exports = desc.exports().stream()
                .sorted(Comparator.comparing(ModuleDescriptor.Exports::source))
                .map(e -> new String[]{
                        e.source(),
                        e.targets().isEmpty() ? "all" : String.join(", ", e.targets())})
                .toList();
        if (!exports.isEmpty()) {
            sections.add(new ModuleSection("exports", exports));
        }

        // opens
        var opens = desc.opens().stream()
                .sorted(Comparator.comparing(ModuleDescriptor.Opens::source))
                .map(o -> new String[]{
                        o.source(),
                        o.targets().isEmpty() ? "all" : String.join(", ", o.targets())})
                .toList();
        if (!opens.isEmpty()) {
            sections.add(new ModuleSection("opens", opens));
        }

        // uses
        var uses = desc.uses().stream()
                .sorted()
                .map(u -> new String[]{u})
                .toList();
        if (!uses.isEmpty()) {
            sections.add(new ModuleSection("uses", uses));
        }

        // provides
        var provides = desc.provides().stream()
                .sorted(Comparator.comparing(ModuleDescriptor.Provides::service))
                .map(p -> new String[]{
                        p.service(),
                        String.join(", ", p.providers())})
                .toList();
        if (!provides.isEmpty()) {
            sections.add(new ModuleSection("provides", provides));
        }

        return new ModuleReport(name, version, isOpen, isAutomatic, sections);
    }
}
