package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTestAnnotation;
import io.github.seanchatmangpt.dtr.junit5.DtrContext;
import io.github.seanchatmangpt.dtr.DocSection;
import io.github.seanchatmangpt.dtr.DocDescription;
import org.junit.jupiter.api.Test;

/**
 * Basic test for @DtrTestAnnotation functionality.
 */
@DtrTestAnnotation
public class DtrTestAnnotationBasicTest {

    @Test
    void testAnnotationWorks(DtrContext ctx) {
        ctx.sayNextSection("@DtrTestAnnotation Test");
        ctx.say("This test verifies @DtrTestAnnotation annotation works correctly.");
        ctx.sayNote("No @AfterAll cleanup needed - it's automatic!");
        
        if (ctx == null) {
            throw new AssertionError("DtrContext should be injected");
        }
    }

    @Test
    void testConfigurationAttributes(DtrContext ctx) {
        ctx.sayNextSection("Configuration Attributes");
        ctx.say("The @DtrTestAnnotation annotation supports outputDir and format attributes:");
        ctx.sayCode("@DtrTestAnnotation(outputDir = \"docs/api\", format = \"latex\")", "java");
    }

    @Test
    @DocSection("Annotation Compatibility")
    @DocDescription("Verifies @DtrTestAnnotation works with existing DTR annotations.")
    void testAnnotationCompatibility(DtrContext ctx) {
        ctx.say("The @DtrTestAnnotation annotation is fully compatible with:");
        ctx.sayOrderedList(java.util.List.of(
            "@DocSection",
            "@DocDescription", 
            "@DocNote",
            "@DocWarning",
            "@DocCode"
        ));
    }
}
