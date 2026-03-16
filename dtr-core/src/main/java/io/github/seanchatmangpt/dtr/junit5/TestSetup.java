package io.github.seanchatmangpt.dtr.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be executed before each test for setup purposes.
 * Alternative to JUnit's @BeforeEach with DTR-specific lifecycle integration.
 *
 * <p>Methods annotated with @TestSetup:
 * <ul>
 * <li>Can accept DtrContext as a parameter</li>
 * <li>Execute before @Test methods</li>
 * <li>Can be used for documentation initialization</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestSetup {
}
