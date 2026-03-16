package io.github.seanchatmangpt.dtr.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for tests that require authentication.
 * Can be used by test runners to skip authentication-required tests
 * when credentials are not available.
 *
 * <p>Usage:
 * <pre>
 * &#64;Test
 * &#64;AuthenticatedTest
 * void testProtectedApi(DtrContext ctx) {
 *     // Test requires valid auth token
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedTest {
    /**
     * Description of what authentication is required.
     */
    String value() default "";
}
