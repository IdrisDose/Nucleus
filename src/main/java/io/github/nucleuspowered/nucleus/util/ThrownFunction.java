/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

/**
 * A function that accepts one argument and returns a result, but can also throw a checked exception.
 *
 * @param <I> The argument type.
 * @param <R> The type of the result.
 * @param <X> The type of {@link Exception} that could be thrown.
 */
@FunctionalInterface
public interface ThrownFunction<I, R, X extends Throwable> {

    R accept(I input) throws X;
}
