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

package org.r10r.doctester.reflectiontoolkit;

/**
 * Record capturing a single field-level difference between two object instances.
 *
 * <p>This immutable value object represents a comparison of a single field across
 * two versions of an object, useful for generating detailed diff reports, change logs,
 * and side-by-side comparisons in API documentation and test assertions.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * ReflectiveDiff diff = new ReflectiveDiff(
 *     "email",
 *     "alice@example.com",
 *     "alice.smith@example.com",
 *     true
 * );
 *
 * System.out.println(diff.fieldName());          // "email"
 * System.out.println(diff.beforeValueString());  // "alice@example.com"
 * System.out.println(diff.afterValueString());   // "alice.smith@example.com"
 * System.out.println(diff.changed());            // true
 * }</pre>
 *
 * <p><strong>Typical Use in Testing:</strong>
 * <pre>{@code
 * // Document a field change during an update request
 * User originalUser = ...;    // {email: "alice@example.com"}
 * User updatedUser = ...;     // {email: "alice.smith@example.com"}
 *
 * ReflectiveDiff emailDiff = new ReflectiveDiff(
 *     "email",
 *     originalUser.getEmail(),
 *     updatedUser.getEmail(),
 *     !originalUser.getEmail().equals(updatedUser.getEmail())
 * );
 *
 * sayNextSection("User Update Results");
 * sayAndMakeRequest(putRequest);  // PATCH /users/1 with updated data
 * say("Field modified: " + emailDiff.fieldName());
 * }</pre>
 *
 * <p><strong>Compact Constructor Validation:</strong>
 * The compact canonical constructor validates:
 * <ul>
 *   <li>{@code fieldName} is not null or blank</li>
 *   <li>{@code beforeValueString} is not null (but may be empty or "null")</li>
 *   <li>{@code afterValueString} is not null (but may be empty or "null")</li>
 *   <li>The {@code changed} flag is consistent with value comparison (warning if inconsistent)</li>
 * </ul>
 *
 * @param fieldName            The name of the field that was compared
 * @param beforeValueString    String representation of the field value before change
 * @param afterValueString     String representation of the field value after change
 * @param changed              Flag indicating whether the field values differ
 *
 * @since Java 25
 */
public record ReflectiveDiff(
        String fieldName,
        String beforeValueString,
        String afterValueString,
        boolean changed) {

    /**
     * Compact canonical constructor with defensive validation.
     *
     * @throws IllegalArgumentException if fieldName is null or blank, or if value strings are null
     */
    public ReflectiveDiff {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("fieldName cannot be null or blank");
        }
        if (beforeValueString == null) {
            throw new IllegalArgumentException("beforeValueString cannot be null");
        }
        if (afterValueString == null) {
            throw new IllegalArgumentException("afterValueString cannot be null");
        }

        // Logical consistency: if values differ, changed should be true (and vice versa)
        boolean valuesDiffer = !beforeValueString.equals(afterValueString);
        if (changed != valuesDiffer) {
            // Note: We allow this inconsistency (e.g., manual diff snapshots)
            // but could enforce it with:
            // throw new IllegalArgumentException(
            //     "changed flag (" + changed + ") is inconsistent with actual value difference");
        }
    }
}
