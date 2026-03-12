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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Record capturing the set of annotations present on a Java class or member.
 *
 * <p>This immutable value object represents the reflection metadata for a single class,
 * including the fully-qualified class name and the set of annotations that decorate it.
 * The annotation list is defensively copied and made unmodifiable.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * AnnotationProfile profile = new AnnotationProfile(
 *     "com.example.UserService",
 *     List.of(
 *         "org.springframework.stereotype.Service",
 *         "org.springframework.transaction.annotation.Transactional"
 *     )
 * );
 *
 * System.out.println(profile.className());           // "com.example.UserService"
 * System.out.println(profile.annotationNames());     // [Service, Transactional]
 * }</pre>
 *
 * <p><strong>Compact Constructor Validation:</strong>
 * The compact canonical constructor validates:
 * <ul>
 *   <li>{@code className} is not null or blank</li>
 *   <li>{@code annotationNames} list is not null (but may be empty)</li>
 *   <li>No annotation names are null or blank</li>
 *   <li>The list is defensively wrapped in {@link Collections#unmodifiableList(List)}</li>
 * </ul>
 *
 * @param className        The fully-qualified class name being annotated
 * @param annotationNames  List of fully-qualified annotation class names applied to the class
 *
 * @since Java 25
 */
public record AnnotationProfile(String className, List<String> annotationNames) {

    /**
     * Compact canonical constructor with defensive copying and validation.
     *
     * @throws IllegalArgumentException if className is null/blank or annotationNames is null
     * @throws IllegalArgumentException if any annotation name is null or blank
     */
    public AnnotationProfile {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("className cannot be null or blank");
        }
        Objects.requireNonNull(annotationNames, "annotationNames cannot be null");

        // Validate no null or blank entries
        for (String annotName : annotationNames) {
            if (annotName == null || annotName.isBlank()) {
                throw new IllegalArgumentException("annotation names cannot be null or blank");
            }
        }

        // Defensive copy and make unmodifiable
        annotationNames = Collections.unmodifiableList(List.copyOf(annotationNames));
    }
}
