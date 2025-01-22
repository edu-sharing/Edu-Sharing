package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserProfileAppAuth extends UserProfile {
    private Map<String,String[]> extendedAttributes = new HashMap<>();
}
