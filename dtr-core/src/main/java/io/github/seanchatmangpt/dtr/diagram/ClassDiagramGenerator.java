package io.github.seanchatmangpt.dtr.diagram;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Generates Mermaid {@code classDiagram} DSL from Java class structures using
 * standard reflection ({@link Class#getSuperclass()}, {@link Class#getInterfaces()},
 * {@link Class#getDeclaredMethods()}).
 *
 * <p>The resulting diagram renders natively on GitHub, GitLab, and Obsidian.</p>
 */
public final class ClassDiagramGenerator {

    private ClassDiagramGenerator() {}

    /**
     * Generates a Mermaid {@code classDiagram} for the given classes.
     *
     * @param classes the classes to include in the diagram
     * @return Mermaid classDiagram DSL string
     */
    public static String generate(Class<?>... classes) {
        var sb = new StringBuilder("classDiagram\n");

        for (Class<?> clazz : classes) {
            // Superclass inheritance relationship
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                sb.append("    ").append(superClass.getSimpleName())
                        .append(" <|-- ").append(clazz.getSimpleName()).append("\n");
            }

            // Interface implementation relationships
            for (Class<?> iface : clazz.getInterfaces()) {
                sb.append("    ").append(iface.getSimpleName())
                        .append(" <|.. ").append(clazz.getSimpleName()).append("\n");
            }
        }

        // Class bodies — public methods
        for (Class<?> clazz : classes) {
            sb.append("    class ").append(clazz.getSimpleName()).append(" {\n");

            var publicMethods = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .limit(6) // cap at 6 to keep diagram readable
                    .toList();

            for (Method m : publicMethods) {
                String params = Arrays.stream(m.getParameterTypes())
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", "));
                sb.append("        +").append(m.getName()).append("(").append(params).append(")\n");
            }

            sb.append("    }\n");
        }

        return sb.toString().trim();
    }
}
