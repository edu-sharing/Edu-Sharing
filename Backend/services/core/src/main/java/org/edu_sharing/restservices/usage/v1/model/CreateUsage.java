package org.edu_sharing.restservices.usage.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateUsage {
    public String appId;
    public String courseId;
    public String resourceId;
    public String nodeId;
    public String nodeVersion;
}
