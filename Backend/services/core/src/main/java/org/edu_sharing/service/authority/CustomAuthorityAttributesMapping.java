package org.edu_sharing.service.authority;

import java.io.Serializable;
import java.util.Map;

/**
 * custom interface to overwrite property mapping behaviour for authorities (users, groups)
 */
public interface CustomAuthorityAttributesMapping {
    /**
     * Map the value that is returned when reading an authority property
     */
    Map<String, Serializable> onGetAuthorityAttributes(String authority);

    /**
     * Map the value that is returned when writing an authority property
     */
    void onSetAuthorityAttributes(String authority, Map<String, Serializable> customAttributes);
}
