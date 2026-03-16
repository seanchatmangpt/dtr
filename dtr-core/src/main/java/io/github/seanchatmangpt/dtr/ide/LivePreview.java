package io.github.seanchatmangpt.dtr.ide;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables live preview of documentation in supported IDEs.
 *
 * <p>When this annotation is present on a test class or method, DTR generates
 * a companion {@code .dtr.preview} file alongside the test output that IDEs
 * can use for real-time preview rendering.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @LivePreview(refreshRateMs = 300, inlineGutter = true)
 * public class MyDocumentationTest extends DtrTest {
 *
 *     @Test
 *     public void demonstrateFeature(DtrContext ctx) {
 *         ctx.say("This content appears in the live preview");
 *     }
 * }
 * }</pre>
 *
 * <h2>IDE Integration</h2>
 * <p>Live preview requires IDE-specific extensions:</p>
 * <ul>
 *   <li><b>IntelliJ IDEA:</b> Install the "DTR Live Preview" plugin from the marketplace</li>
 *   <li><b>VS Code:</b> Install the "DTR Preview" extension</li>
 *   <li><b>Eclipse:</b> Install the "DTR Tools" plugin via Eclipse Marketplace</li>
 * </ul>
 *
 * <h2>Preview File Format</h2>
 * <p>Generated {@code .dtr.preview} files use JSON format:
 * <pre>{@code
 * {
 *   "version": "1.0",
 *   "testClass": "com.example.MyTest",
 *   "timestamp": "2026-03-15T10:30:00Z",
 *   "content": "# Documentation Content\n\n...",
 *   "metadata": {
 *     "refreshRateMs": 500,
 *     "inlineGutter": true
 *   }
 * }
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>Lower refresh rates (e.g., 100ms) provide more responsive updates but increase CPU usage</li>
 *   <li>Higher refresh rates (e.g., 1000ms) reduce CPU usage but may feel less responsive</li>
 *   <li>For large test suites, consider using {@code refreshRateMs = 1000} or higher</li>
 *   <li>Inline gutter rendering is disabled by default for projects with 100+ test methods</li>
 * </ul>
 *
 * @see DtrContext
 * @since 2026.2.0
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LivePreview {

    /**
     * Refresh rate in milliseconds for live preview updates.
     *
     * <p>Controls how frequently the IDE polls for changes to the preview file.
     * Lower values provide more responsive updates but increase CPU and I/O usage.</p>
     *
     * <p>Recommended values:</p>
     * <ul>
     *   <li>100-300ms: Interactive development, small test suites</li>
     *   <li>500ms: Default, balanced performance</li>
     *   <li>1000ms+: Large test suites, resource-constrained environments</li>
     * </ul>
     *
     * @return refresh rate in milliseconds, default 500ms
     */
    long refreshRateMs() default 500L;

    /**
     * Whether to show inline preview in the editor gutter.
     *
     * <p>When enabled, IDEs display a condensed preview of the documentation
     * alongside the test code in the editor gutter area. This provides
     * immediate visual feedback without opening a separate preview panel.</p>
     *
     * <p>Inline gutter preview is recommended for:</p>
     * <ul>
     *   <li>Documentation-heavy test methods</li>
     *   <li>Tutorial and guide content</li>
     *   <li>API documentation tests</li>
     * </ul>
     *
     * <p>Consider disabling inline gutter for:</p>
     * <ul>
     *   <li>Large test suites (100+ methods) to reduce visual clutter</li>
     *   <li>Tests with minimal documentation</li>
     *   <li>Performance-critical test runs</li>
     * </ul>
     *
     * @return true to enable inline gutter preview, default true
     */
    boolean inlineGutter() default true;

    /**
     * Whether to automatically open the preview panel when tests run.
     *
     * <p>When enabled, the IDE will automatically show the preview panel
     * (or activate the preview window) when DTR tests are executed.</p>
     *
     * @return true to auto-open preview, default false
     */
    boolean autoOpen() default false;

    /**
     * Custom CSS for preview rendering (IDE-specific).
     *
     * <p>Allows customization of preview styling. Format and support varies
     * by IDE implementation. Consult IDE-specific documentation for details.</p>
     *
     * <p>Example for IntelliJ IDEA:</p>
     * <pre>{@code
     * @LivePreview(customCss = ".dtr-header { color: #0066cc; }")
     * }</pre>
     *
     * @return CSS styling rules, default empty
     */
    String customCss() default "";

    /**
     * Whether to include test execution metadata in preview.
     *
     * <p>When enabled, preview includes execution time, test status,
     * and other runtime information alongside the documentation content.</p>
     *
     * @return true to include metadata, default true
     */
    boolean includeMetadata() default true;
}
