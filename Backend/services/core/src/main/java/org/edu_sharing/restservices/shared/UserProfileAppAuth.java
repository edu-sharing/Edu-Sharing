package org.edu_sharing.restservices.shared;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class UserProfileAppAuth extends UserProfile {
    Map<String,String[]> extendedAttributes = new HashMap<>();

    @JsonProperty
    public Map<String, String[]> getExtendedAttributes() {
        return extendedAttributes;
    }
}
