/**
 * EXAMPLE: Enhanced Error Messages for ReferenceResolver.java
 *
 * This file shows the BEFORE/AFTER transformation for all error messages
 * in ReferenceResolver.java, following the "what + why + how to fix" pattern.
 *
 * Use this as a template for enhancing other files.
 */

package io.github.seanchatmangpt.dtr.crossref;

// ... imports ...

public class ReferenceResolver {

    // ... fields and methods ...

    /**
     * ENHANCEMENT #1: Invalid DocTest Reference
     *
     * BEFORE (Line 132):
     * throw new InvalidDocTestRefException(
     *     "DocTest class not found in index: " + targetClass.getName());
     */
    public String resolveLabel(DocTestRef ref) {
        Class<?> targetClass = ref.docTestClass();
        String anchor = ref.anchor();

        Map<String, String> labels = sectionLabels.get(targetClass);
        if (labels == null) {
            // AFTER:
            throw new InvalidDocTestRefException(
                "DocTest class not found in index: " + targetClass.getName() + ". " +
                "Cross-references require the target DocTest to have been executed first. " +
                "Fix: Ensure " + targetClass.getSimpleName() + " is included in the test suite " +
                "and that sayNextSection() has been called before referencing it. " +
                "Use @Order annotation to control test execution order if needed."
            );
            // END AFTER
        }

        /**
         * ENHANCEMENT #2: Invalid Anchor
         *
         * BEFORE (Line 138):
         * throw new InvalidAnchorException(
         *     "Anchor not found in " + targetClass.getSimpleName() + ": " + anchor);
         */
        String label = labels.get(anchor);
        if (label == null) {
            // AFTER:
            throw new InvalidAnchorException(
                "Anchor not found in " + targetClass.getSimpleName() + ": " + anchor + ". " +
                "Anchors are auto-generated from section titles via sayNextSection(). " +
                "Fix: Ensure the target DocTest calls sayNextSection(\"" +
                anchor.replaceAll("-", " ") + "\") or similar. " +
                "Anchor format: lowercase with hyphens (e.g., 'User Creation' -> 'user-creation')."
            );
            // END AFTER
        }

        return label;
    }

    // ... rest of class ...
}

/**
 * SUMMARY OF CHANGES:
 *
 * 1. Error #1 (InvalidDocTestRefException):
 *    - Added context: cross-references require prior execution
 *    - Added fix: include target in test suite
 *    - Added guidance: use @Order annotation
 *
 * 2. Error #2 (InvalidAnchorException):
 *    - Added context: anchors auto-generated from sayNextSection()
 *    - Added fix: call sayNextSection() with correct title
 *    - Added example: anchor format conversion
 *
 * BOTH ENHANCEMENTS:
 * - Preserved original error information
 * - Added "why" (context)
 * - Added "how to fix" (actionable steps)
 * - Added examples where helpful
 * - Maintained exception type for compatibility
 */

/**
 * TESTING STRATEGY FOR THIS FILE:
 *
 * 1. Unit Test: Trigger each error and verify message content
 * @Test
 * void testInvalidDocTestRefErrorMessage() {
 *     DocTestRef invalidRef = new DocTestRef(NonExistentTest.class, "some-anchor");
 *     Exception e = assertThrows(InvalidDocTestRefException.class,
 *         () -> resolver.resolveLabel(invalidRef));
 *     assertTrue(e.getMessage().contains("not found in index"));
 *     assertTrue(e.getMessage().contains("Fix:"));
 *     assertTrue(e.getMessage().contains("@Order"));
 * }
 *
 * 2. DocTest: Document the error in user-facing documentation
 * @Test
 * void testCrossReferenceError() {
 *     DtrContext ctx = new DtrContext(renderMachine);
 *     ctx.sayNextSection("Cross-Reference Errors");
 *     ctx.say("When you reference a non-existent DocTest, you get:");
 *     ctx.sayCode("DocTest class not found in index: ...", "text");
 *     ctx.sayNote("Fix: Include the target test in your suite");
 * }
 *
 * 3. Integration Test: Verify error recovery works
 * @Test
 * void testCrossReferenceAfterFix() {
 *     // First, trigger the error
 *     // Then, apply the fix
 *     // Verify resolution succeeds
 * }
 */
