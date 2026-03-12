/**
 * Copyright (C) 2013 the original author or authors.
 *
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

package controllers.utils;

import ninja.utils.NinjaMode;
import ninja.standalone.NinjaJetty;
import ninja.utils.NinjaConstant;
import ninja.utils.NinjaTestServer;

import io.github.seanchatmangpt.dtr.DocTester;
import io.github.seanchatmangpt.dtr.testbrowser.Url;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

public abstract class NinjaApiDoctester extends DocTester {

	public NinjaTestServer ninjaTestServer;

    public NinjaApiDoctester() {
    }

    @BeforeEach
    public void startServerInTestMode() {
        System.setProperty(NinjaConstant.MODE_KEY_NAME, NinjaConstant.MODE_TEST);
        ninjaTestServer = NinjaTestServer.builder()
            .ninjaMode(NinjaMode.test)
            .standaloneClass(NinjaJetty.class)
            .port(0) // use random port
            .build();
    }

    @AfterEach
    public void shutdownServer() {
    	System.clearProperty(NinjaConstant.MODE_KEY_NAME);
        ninjaTestServer.shutdown();
    }

    @Override
    public Url testServerUrl() {
    	return Url.host(ninjaTestServer.getServerAddress());
    }

}
