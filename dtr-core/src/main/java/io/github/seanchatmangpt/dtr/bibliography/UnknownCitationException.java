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
package io.github.seanchatmangpt.dtr.bibliography;

import io.github.seanchatmangpt.dtr.DtrException;

/**
 * Thrown when a citation references a non-existent bibliography entry.
 *
 * This exception is thrown at test execution time (not compile time) when
 * sayCite() is called with a key that doesn't exist in the loaded bibliography.
 * Tests fail fast if a citation key is not found.
 */
public class UnknownCitationException extends DtrException {

    /**
     * Creates an exception for an unknown citation.
     *
     * @param key the unknown citation key
     */
    public UnknownCitationException(String key) {
        super(builder()
            .message("Unknown citation key: '%s'".formatted(key))
            .errorCode("DTR-CITE-001")
            .context("citationKey", key));
    }

    /**
     * Creates an exception with a custom message.
     *
     * @param message detailed error message
     */
    public UnknownCitationException(String message, Throwable cause) {
        super(builder()
            .message(message)
            .errorCode("DTR-CITE-001")
            .cause(cause));
    }
}
