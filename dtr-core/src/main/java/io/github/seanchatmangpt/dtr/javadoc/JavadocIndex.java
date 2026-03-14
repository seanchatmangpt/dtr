package io.github.seanchatmangpt.dtr.javadoc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Static index of Javadoc entries extracted from Java source by the dtr-javadoc Rust binary.
 *
 * <p>Loaded once at class initialization from {@code docs/meta/javadoc.json}.
 * Keys are {@code fully.qualified.ClassName#methodName}.</p>
 *
 * <p>If the JSON file does not exist (e.g. Rust binary not run yet), the index is empty
 * and all lookups return {@link Optional#empty()}.</p>
 */
public final class JavadocIndex {

    private static final Logger logger = LoggerFactory.getLogger(JavadocIndex.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, JavadocEntry> INDEX = loadIndex();

    private JavadocIndex() {}

    /**
     * Looks up the Javadoc entry for the given method.
     *
     * @param method the method to look up
     * @return the Javadoc entry, or empty if not found
     */
    public static Optional<JavadocEntry> lookup(Method method) {
        String key = method.getDeclaringClass().getName() + "#" + method.getName();
        return Optional.ofNullable(INDEX.get(key));
    }

    private static Map<String, JavadocEntry> loadIndex() {
        Path path = Path.of("docs/meta/javadoc.json");
        if (!Files.exists(path)) {
            logger.debug("dtr-javadoc: {} not found — sayJavadoc() will return empty results. "
                + "Run 'make extract-javadoc' to generate it.", path);
            return Map.of();
        }
        try {
            TypeReference<Map<String, JavadocEntry>> typeRef = new TypeReference<>() {};
            Map<String, JavadocEntry> index = MAPPER.readValue(path.toFile(), typeRef);
            logger.debug("dtr-javadoc: loaded {} entries from {}", index.size(), path);
            return Map.copyOf(index);
        } catch (IOException e) {
            logger.warn("dtr-javadoc: failed to load {}: {}", path, e.getMessage());
            return Map.of();
        }
    }
}
