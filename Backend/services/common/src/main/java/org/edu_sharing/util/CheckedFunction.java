package org.edu_sharing.util;


import java.util.function.Function;

/**
 * Represents a function that accepts one argument and produces a result.
 * This is a functional interface whose functional method is apply(Object).
 * @param <T> the type of the input to the function
 * @param <R> the type of the output to the function
 */
@FunctionalInterface
public interface CheckedFunction<T, R, E extends Exception> {
    /** Applies this function to the given argument.
     * @param t the function argument
     * @return the function result
     * @throws E thrown by this function
     */
    R apply(T t) throws E;

    /**
     * Applies this function to the given argument.
     * @param checkedFunction function to execute inside a try catch and throws a runtime exception in case of a thrown exception
     * @param <T> the type of the input to the function
     * @param <R> the type of the output to the function
     * @return the function object {@link Function} that might throw a {@link RuntimeException}
     */
    static <T, R, E extends Exception> Function<T, R> wrap(CheckedFunction<T, R, E> checkedFunction) {
        return t -> {
            try {
                return checkedFunction.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }


    /**
     * Applies this function to the given argument.
     * @param checkedFunction function to execute inside a try catch and returns a default value in case of a thrown exception
     * @param <T> the type of the input to the function
     * @param <R> the type of the output to the function
     * @return the function object {@link Function} that returns the default value in case of an exception
     */
    static <T, R, E extends Exception> Function<T, R> wrap(CheckedFunction<T, R, E> checkedFunction, R defaultValue) {
        return t -> {
            try {
                return checkedFunction.apply(t);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }
}
