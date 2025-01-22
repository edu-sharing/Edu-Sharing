package org.edu_sharing.restservices.knowledge.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Job  {

  @JsonProperty(required = true)
  private String id = null;

  @JsonProperty(required = true)
  private String status = null;
}
