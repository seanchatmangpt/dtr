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
package io.github.seanchatmangpt.dtr.junit5;

import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;

/**
 * Marker interface for JUnit 5 DTR test classes.
 *
 * <p>Test classes can implement this interface to indicate they use DTR
 * functionality. When combined with {@link DtrExtension}, this provides
 * full access to the DTR API.
 *
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
 * class MyApiDocTest implements DtrCommands {
 *
 *     // Test methods can inject DtrContext
 *     @Test
 *     void testGetUsers(DtrContext ctx) {
 *         ctx.sayNextSection("User API");
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @see DtrExtension
 * @see DtrContext
 */
public interface DtrCommands extends RenderMachineCommands {
    // Marker interface - all methods inherited from RenderMachineCommands
}
