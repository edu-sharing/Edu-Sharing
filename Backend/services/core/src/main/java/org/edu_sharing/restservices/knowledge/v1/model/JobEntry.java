package org.edu_sharing.restservices.knowledge.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JobEntry  {

  @JsonProperty(required = true)
  private Job data = null;

}
