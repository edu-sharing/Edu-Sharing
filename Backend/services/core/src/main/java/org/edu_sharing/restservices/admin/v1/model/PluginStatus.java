package org.edu_sharing.restservices.admin.v1.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PluginStatus {
    private String version;
    private String name;
    private Boolean enabled;
}
