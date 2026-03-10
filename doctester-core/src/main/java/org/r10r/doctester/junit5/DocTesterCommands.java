/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.r10r.doctester.junit5;

import org.r10r.doctester.rendermachine.RenderMachineCommands;

/**
 * Marker interface for JUnit 5 DocTester test classes.
 *
 * <p>Test classes can implement this interface to indicate they use DocTester
 * functionality. When combined with {@link DocTesterExtension}, this provides
 * full access to the DocTester API.
 *
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(DocTesterExtension.class)
 * class MyApiDocTest implements DocTesterCommands {
 *
 *     // Test methods can inject DocTesterContext
 *     @Test
 *     void testGetUsers(DocTesterContext ctx) {
 *         ctx.sayNextSection("User API");
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @see DocTesterExtension
 * @see DocTesterContext
 */
public interface DocTesterCommands extends RenderMachineCommands {
    // Marker interface - all methods inherited from RenderMachineCommands
}
