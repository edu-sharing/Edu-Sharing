package org.edu_sharing.restservices.organization.v1.model;

import lombok.Data;
import org.edu_sharing.service.organization.GroupSignupMethod;

@Data
public class GroupSignupDetails {
    private GroupSignupMethod signupMethod;
    private String signupPassword;
}
