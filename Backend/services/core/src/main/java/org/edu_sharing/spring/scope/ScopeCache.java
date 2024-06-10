package org.edu_sharing.spring.scope;

import java.util.Collection;

/**
 * A special-purpose cache interface specifically for the {@link GenericScope} to use to
 * manage cached bean instances. Implementations generally fall into two categories: those
 * that store values "globally" (i.e. one instance per key), and those that store
 * potentially multiple instances per key based on context (e.g. via a thread local). All
 * implementations should be thread safe.
 *
 * <a href="https://github.com/spring-cloud/spring-cloud-commons">Source by spring cloud</a>
 */
public interface ScopeCache {

    /**
     * Removes the object with this name from the cache.
     * @param name The object name.
     * @return The object removed, or null if there was none.
     */
    Object remove(String name);

    /**
     * Clears the cache and returns all objects in an unmodifiable collection.
     * @return All objects stored in the cache.
     */
    Collection<Object> clear();

    /**
     * Gets the named object from the cache.
     * @param name The name of the object.
     * @return The object with that name, or null if there is none.
     */
    Object get(String name);

    /**
     * Put a value in the cache if the key is not already used. If one is already present
     * with the name provided, it is not replaced, but is returned to the caller.
     * @param name The key.
     * @param value The new candidate value.
     * @return The value that is in the cache at the end of the operation.
     */
    Object put(String name, Object value);

}