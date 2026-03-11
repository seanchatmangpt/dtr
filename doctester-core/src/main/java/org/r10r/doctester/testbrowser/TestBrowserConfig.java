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
package org.r10r.doctester.testbrowser;

import java.time.Duration;

/**
 * Configuration options for the TestBrowser HTTP client.
 *
 * <p>This record provides configuration options for customizing HTTP client behavior
 * such as timeouts, redirect handling, and connection pooling.
 *
 * <p>Usage:
 * <pre>{@code
 * var config = new TestBrowserConfig(
 *     Duration.ofSeconds(5),   // connectTimeout
 *     Duration.ofSeconds(60),  // readTimeout
 *     false,                   // followRedirects
 *     50                       // maxConnections
 * );
 *
 * // Or use defaults
 * var defaultConfig = TestBrowserConfig.defaults();
 * }</pre>
 *
 * @param connectTimeout time to wait for a connection to be established
 * @param readTimeout maximum time to wait for data from the server
 * @param followRedirects whether to automatically follow HTTP redirects
 * @param maxConnections maximum number of concurrent connections
 */
public record TestBrowserConfig(
    Duration connectTimeout,
    Duration readTimeout,
    boolean followRedirects,
    int maxConnections
) {

    /**
     * Creates a default configuration with sensible values.
     *
     * <p>Default values:
     * <ul>
     *   <li>connectTimeout: 10 seconds</li>
     *   <li>readTimeout: 30 seconds</li>
     *   <li>followRedirects: true</li>
     *   <li>maxConnections: 100</li>
     * </ul>
     *
     * @return a new TestBrowserConfig with default values
     */
    public static TestBrowserConfig defaults() {
        return new TestBrowserConfig(
            Duration.ofSeconds(10),
            Duration.ofSeconds(30),
            true,
            100
        );
    }

    /**
     * Creates a configuration optimized for fast local testing.
     *
     * <p>Uses shorter timeouts suitable for local test servers.
     *
     * @return a new TestBrowserConfig with fast local testing values
     */
    public static TestBrowserConfig fastLocal() {
        return new TestBrowserConfig(
            Duration.ofSeconds(2),
            Duration.ofSeconds(5),
            true,
            10
        );
    }

    /**
     * Creates a builder for constructing a custom configuration.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TestBrowserConfig.
     */
    public static class Builder {
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(30);
        private boolean followRedirects = true;
        private int maxConnections = 100;

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public Builder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public TestBrowserConfig build() {
            return new TestBrowserConfig(connectTimeout, readTimeout, followRedirects, maxConnections);
        }
    }
}
