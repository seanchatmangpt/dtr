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
 * Record capturing the call site (location in code) where a method invocation occurred.
 *
 * <p>This lightweight, immutable value object represents a point in the source code,
 * useful for stack trace analysis, debugging, and reflection-based introspection.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * CallSiteRecord site = new CallSiteRecord(
 *     "com.example.UserService",
 *     "createUser",
 *     42
 * );
 *
 * System.out.println(site.className());       // "com.example.UserService"
 * System.out.println(site.methodName());      // "createUser"
 * System.out.println(site.lineNumber());      // 42
 * }</pre>
 *
 * <p><strong>Compact Constructor Validation:</strong>
 * The compact canonical constructor validates:
 * <ul>
 *   <li>{@code className} is not null or blank</li>
 *   <li>{@code methodName} is not null or blank</li>
 *   <li>{@code lineNumber} is positive (>= 0)</li>
 * </ul>
 *
 * @param className    The fully-qualified class name (e.g., "org.r10r.doctester.DocTester")
 * @param methodName   The method name (e.g., "sayNextSection")
 * @param lineNumber   The source code line number (0-based or 1-based depending on convention)
 *
 * @since Java 25
 */
public record CallSiteRecord(String className, String methodName, int lineNumber) {

    /**
     * Compact canonical constructor with defensive validation.
     *
     * @throws IllegalArgumentException if className or methodName are null/blank, or lineNumber < 0
     */
    public CallSiteRecord {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("className cannot be null or blank");
        }
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("methodName cannot be null or blank");
        }
        if (lineNumber < 0) {
            throw new IllegalArgumentException("lineNumber must be >= 0, got " + lineNumber);
        }
    }
}
