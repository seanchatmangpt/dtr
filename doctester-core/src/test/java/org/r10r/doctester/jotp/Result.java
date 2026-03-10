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
package org.r10r.doctester.jotp;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A sealed Result type modelling either a successful value ({@code Success<T>})
 * or a failure ({@code Failure<E>}).
 *
 * <p>Sourced from <a href="https://github.com/cchacin/java-maven-template">
 * cchacin/java-maven-template</a> and adapted for DocTester's JOTP doctests.
 *
 * <p>Demonstrates Java 25 features:
 * <ul>
 *   <li>Sealed interfaces with {@code permits}</li>
 *   <li>Records as sealed subtypes</li>
 *   <li>Record deconstruction patterns in {@code switch} expressions</li>
 *   <li>{@code instanceof} pattern matching with deconstruction</li>
 *   <li>Exhaustive {@code switch} (no {@code default} needed)</li>
 * </ul>
 *
 * @param <T> the success value type
 * @param <E> the failure error type
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    /** Wraps a successful value. */
    record Success<T, E>(T value) implements Result<T, E> {}

    /** Wraps a failure error. */
    record Failure<T, E>(E error) implements Result<T, E> {}

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /** Creates a successful {@code Result} wrapping {@code value}. */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /** Creates a failed {@code Result} wrapping {@code error}. */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    /**
     * Executes {@code supplier} and returns {@code Success} if it completes
     * normally, or {@code Failure} if it throws.
     */
    static <T, E extends Exception> Result<T, E> of(ThrowingSupplier<T, E> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            @SuppressWarnings("unchecked")
            E error = (E) e;
            return failure(error);
        }
    }

    // -------------------------------------------------------------------------
    // Predicates
    // -------------------------------------------------------------------------

    /** Returns {@code true} if this is a {@link Success}. */
    default boolean isSuccess() {
        return this instanceof Success<T, E>;
    }

    /** Returns {@code true} if this is a {@link Failure}. */
    default boolean isFailure() {
        return this instanceof Failure<T, E>;
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    /**
     * Transforms the success value with {@code mapper}; passes {@link Failure}
     * through unchanged.
     */
    default <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> new Success<>(mapper.apply(value));
            case Failure<T, E>(var error) -> new Failure<>(error);
        };
    }

    /**
     * Transforms the error with {@code mapper}; passes {@link Success} through
     * unchanged.
     */
    default <F> Result<T, F> mapError(Function<? super E, ? extends F> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> new Success<>(value);
            case Failure<T, E>(var error) -> new Failure<>(mapper.apply(error));
        };
    }

    /**
     * Chains an operation that itself returns a {@code Result}.  Short-circuits
     * on {@link Failure}.
     */
    default <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> mapper.apply(value);
            case Failure<T, E>(var error) -> new Failure<>(error);
        };
    }

    // -------------------------------------------------------------------------
    // Extraction
    // -------------------------------------------------------------------------

    /**
     * Exhaustive pattern match: applies {@code onSuccess} or {@code onFailure}
     * and returns a single result of type {@code U}.
     */
    default <U> U fold(
            Function<? super T, ? extends U> onSuccess,
            Function<? super E, ? extends U> onFailure) {
        return switch (this) {
            case Success<T, E>(var value) -> onSuccess.apply(value);
            case Failure<T, E>(var error) -> onFailure.apply(error);
        };
    }

    /**
     * Returns the success value, or applies {@code recovery} to the error to
     * produce a fallback value.
     */
    default T recover(Function<? super E, ? extends T> recovery) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> recovery.apply(error);
        };
    }

    /**
     * Returns this {@link Success}, or replaces a {@link Failure} with the
     * {@code Result} produced by {@code recovery}.
     */
    default Result<T, E> recoverWith(Function<? super E, ? extends Result<T, E>> recovery) {
        return switch (this) {
            case Success<T, E> s -> s;
            case Failure<T, E>(var error) -> recovery.apply(error);
        };
    }

    /**
     * Returns the success value or throws {@link RuntimeException} if this is a
     * {@link Failure}.
     */
    default T orElseThrow() {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> {
                if (error instanceof Exception e) {
                    throw new RuntimeException(e);
                }
                throw new RuntimeException("Result failed with error: " + error);
            }
        };
    }

    /** Returns the success value or {@code defaultValue} if this is a {@link Failure}. */
    default T orElse(T defaultValue) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E> ignored -> defaultValue;
        };
    }

    /** Returns the success value or the value produced by {@code supplier} if this is a {@link Failure}. */
    default T orElseGet(Supplier<? extends T> supplier) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E> ignored -> supplier.get();
        };
    }

    // -------------------------------------------------------------------------
    // Side effects
    // -------------------------------------------------------------------------

    /**
     * Calls {@code consumer} with the success value (for logging/metrics); returns
     * {@code this} unchanged.
     */
    default Result<T, E> peek(Consumer<? super T> consumer) {
        if (this instanceof Success<T, E>(var value)) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Calls {@code consumer} with the error value (for logging/metrics); returns
     * {@code this} unchanged.
     */
    default Result<T, E> peekError(Consumer<? super E> consumer) {
        if (this instanceof Failure<T, E>(var error)) {
            consumer.accept(error);
        }
        return this;
    }

    // -------------------------------------------------------------------------
    // Functional interface
    // -------------------------------------------------------------------------

    /** A {@link java.util.function.Supplier} variant that may throw a checked exception. */
    @FunctionalInterface
    interface ThrowingSupplier<T, E extends Exception> {
        T get() throws E;
    }
}
