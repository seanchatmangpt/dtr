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
package org.r10r.doctester;

import org.r10r.doctester.DocTester;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.r10r.doctester.rendermachine.RenderMachine;
import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;

import org.mockito.Mockito;

public class DocTesterLifecycleTest extends DocTester {

    // A static mock to test lifecycle of class
    public static RenderMachine renderMachineMock = Mockito.mock(RenderMachine.class);

    @BeforeAll
    public static void asserThatRenderEngineHasBeenInitialized() {
        Assertions.assertNotNull(renderMachine);
        assertThat(renderMachine, CoreMatchers.equalTo(renderMachineMock));
    }

    @AfterAll
    public static void asserThatRenderEngineHasBeenShutDownCorrectly() {
        Mockito.verify(renderMachineMock).finishAndWriteOut();
        Assertions.assertNull(renderMachine);
    }

    @Override
    public RenderMachine getRenderMachine() {
        // to verify that something on renderMachineMock has been called
        return renderMachineMock;
    }

}
