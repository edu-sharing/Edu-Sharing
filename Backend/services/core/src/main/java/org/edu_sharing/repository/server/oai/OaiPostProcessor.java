package org.edu_sharing.repository.server.oai;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The {@code OaiPostProcessor} interface defines a contract for post-processing
 * OAI-PMH responses. Implementations of this interface provide a way to modify
 * or enhance the response XML based on the request parameters.
 */
public interface OaiPostProcessor {

    /**
     * Processes the given response XML based on the provided parameters.
     *
     * @param parameterMap a map of request parameters, where each key is a parameter
     *                     name and each value is an array of parameter values.
     *                     This map should not be {@code null}.
     * @param responseXML  the XML response to be processed. This XML string should
     *                     not be {@code null}.
     * @return the post-processed XML response as a non-null string.
     */
    @NotNull
    String postProcessResponse(@NotNull Map<String, String[]> parameterMap, @NotNull String responseXML);
}
