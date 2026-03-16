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
package io.github.seanchatmangpt.dtr.crossref;

import io.github.seanchatmangpt.dtr.DtrException;

/**
 * Thrown when a cross-reference targets an anchor (section) that does not exist
 * in the target DocTest.
 */
public class InvalidAnchorException extends DtrException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message descriptive error message
     */
    public InvalidAnchorException(String message) {
        super(builder().message(message).errorCode("DTR-ANCHOR-001"));
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message descriptive error message
     * @param cause the underlying cause
     */
    public InvalidAnchorException(String message, Throwable cause) {
        super(builder().message(message).errorCode("DTR-ANCHOR-001").cause(cause));
    }
}
