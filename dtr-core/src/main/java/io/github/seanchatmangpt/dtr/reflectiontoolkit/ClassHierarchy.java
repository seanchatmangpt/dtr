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
 * Record capturing the inheritance and interface hierarchy of a Java class.
 *
 * <p>This immutable record represents the type relationships for a class,
 * storing the chain of superclasses (from direct parent to java.lang.Object)
 * and the set of interfaces directly implemented. Both lists are defensively
 * copied and made unmodifiable.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * ClassHierarchy hierarchy = new ClassHierarchy(
 *     Collections.unmodifiableList(List.of(
 *         "com.example.AbstractService",
 *         "java.lang.Object"
 *     )),
 *     Collections.unmodifiableList(List.of(
 *         "java.io.Serializable",
 *         "java.lang.Cloneable"
 *     ))
 * );
 *
 * System.out.println(hierarchy.superclassChainNames());        // [AbstractService, Object]
 * System.out.println(hierarchy.implementedInterfaceNames());   // [Serializable, Cloneable]
 * }</pre>
 *
 * <p><strong>Compact Constructor Validation:</strong>
 * The compact canonical constructor validates:
 * <ul>
 *   <li>Both lists are not null (but may be empty)</li>
 *   <li>No class/interface names are null or blank</li>
 *   <li>Both lists are defensively wrapped in {@link Collections#unmodifiableList(List)}</li>
 * </ul>
 *
 * @param superclassChainNames           Ordered list of superclass names from direct parent to Object
 * @param implementedInterfaceNames      List of directly implemented interface names
 *
 * @since Java 25
 */
public record ClassHierarchy(
        List<String> superclassChainNames,
        List<String> implementedInterfaceNames) {

    /**
     * Compact canonical constructor with defensive copying and validation.
     *
     * @throws NullPointerException if either list is null
     * @throws IllegalArgumentException if any class/interface name is null or blank
     */
    public ClassHierarchy {
        Objects.requireNonNull(superclassChainNames, "superclassChainNames cannot be null");
        Objects.requireNonNull(implementedInterfaceNames, "implementedInterfaceNames cannot be null");

        // Validate no null or blank entries in superclass chain
        for (String superName : superclassChainNames) {
            if (superName == null || superName.isBlank()) {
                throw new IllegalArgumentException("superclass names cannot be null or blank");
            }
        }

        // Validate no null or blank entries in interface list
        for (String ifaceName : implementedInterfaceNames) {
            if (ifaceName == null || ifaceName.isBlank()) {
                throw new IllegalArgumentException("interface names cannot be null or blank");
            }
        }

        // Defensive copy and make unmodifiable
        superclassChainNames = Collections.unmodifiableList(List.copyOf(superclassChainNames));
        implementedInterfaceNames = Collections.unmodifiableList(List.copyOf(implementedInterfaceNames));
    }
}
