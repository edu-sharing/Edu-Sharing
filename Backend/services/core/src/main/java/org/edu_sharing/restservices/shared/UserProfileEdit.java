package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The editable UserProfile from RestApi, extended for additional fields only relevant for editing
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserProfileEdit extends UserProfile {
    private long sizeQuota;
}
