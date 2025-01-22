package org.edu_sharing.restservices.ltiplatform.v13.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Tool {
    private String domain;
    private String description;
    private String appId;
    private String name;
    private String logo;
    private boolean customContentOption = false;
    private String resourceType;
}
