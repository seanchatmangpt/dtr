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
package io.github.seanchatmangpt.dtr;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all DTR-specific errors.
 *
 * <p>Provides actionable error messages with "what + why + how to fix" pattern.
 * Uses builder pattern for fluent construction and supports structured error context.</p>
 *
 * <p><strong>Error message pattern:</strong></p>
 * <ul>
 *   <li><strong>What:</strong> Clear description of what failed</li>
 *   <li><strong>Why:</strong> Root cause or precondition that failed</li>
 *   <li><strong>How to fix:</strong> Actionable steps to resolve</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw DtrException.builder()
 *     .errorCode("DTR-001")
 *     .message("Failed to resolve cross-reference")
 *     .context("targetClass", "com.example.MissingClass")
 *     .context("anchor", "nonexistent-section")
 *     .cause(new ClassNotFoundException())
 *     .build();
 * }</pre>
 *
 * <p>Subclasses should use the builder to construct their instances:</p>
 * <pre>{@code
 * public class InvalidDocTestRefException extends DtrException {
 *     public InvalidDocTestRefException(String className, String anchor) {
 *         super(DtrException.builder()
 *             .errorCode("DTR-REF-001")
 *             .message("Invalid DocTest reference: %s#%s".formatted(className, anchor))
 *             .context("className", className)
 *             .context("anchor", anchor));
 *     }
 * }
 * }</pre>
 */
public class DtrException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final Map<String, Object> context;

    /**
     * Creates a new DTR exception with structured metadata.
     *
     * @param message  the error message
     * @param errorCode the error code (e.g., "DTR-001")
     * @param context  structured context map for debugging
     * @param cause    the underlying cause
     */
    protected DtrException(String message, String errorCode, Map<String, Object> context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context != null ? Map.copyOf(context) : Map.of();
    }

    /**
     * Creates a new DTR exception using a builder.
     *
     * @param builder the builder instance
     */
    protected DtrException(Builder builder) {
        this(builder.message, builder.errorCode, builder.context, builder.cause);
    }

    /**
     * Creates a new builder for constructing DTR exceptions.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the error code for this exception.
     *
     * @return the error code (e.g., "DTR-001")
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the structured context map for debugging.
     *
     * @return an unmodifiable map of context key-value pairs
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * Builder for fluent construction of DTR exceptions.
     *
     * <p>Provides a fluent API for constructing exceptions with structured context:
     * <pre>{@code
     * throw DtrException.builder()
     *     .errorCode("DTR-001")
     *     .message("Something went wrong")
     *     .context("key", "value")
     *     .cause(cause)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private String message;
        private String errorCode;
        private Map<String, Object> context = new HashMap<>();
        private Throwable cause;

        /**
         * Sets the error message.
         *
         * @param message the error message
         * @return this builder for chaining
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the error code.
         *
         * @param code the error code (e.g., "DTR-001")
         * @return this builder for chaining
         */
        public Builder errorCode(String code) {
            this.errorCode = code;
            return this;
        }

        /**
         * Adds a key-value pair to the error context.
         *
         * @param key   the context key
         * @param value the context value
         * @return this builder for chaining
         */
        public Builder context(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        /**
         * Sets all context entries at once.
         *
         * @param context the context map to copy
         * @return this builder for chaining
         */
        public Builder context(Map<String, Object> context) {
            if (context != null) {
                this.context.putAll(context);
            }
            return this;
        }

        /**
         * Sets the underlying cause.
         *
         * @param cause the underlying throwable
         * @return this builder for chaining
         */
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        /**
         * Builds and returns the DTR exception.
         *
         * @return a new DTR exception instance
         * @throws IllegalStateException if message is not set
         */
        public DtrException build() {
            if (message == null) {
                throw new IllegalStateException("Error message is required");
            }
            return new DtrException(this);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        if (errorCode != null) {
            sb.append("[").append(errorCode).append("]");
        }
        sb.append(": ").append(getMessage());
        if (!context.isEmpty()) {
            sb.append("\nContext: ").append(context);
        }
        return sb.toString();
    }
}
