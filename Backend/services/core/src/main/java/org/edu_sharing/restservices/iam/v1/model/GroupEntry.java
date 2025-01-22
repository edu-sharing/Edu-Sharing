package org.edu_sharing.restservices.iam.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.Group;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class GroupEntry  {

  @JsonProperty(required = true)
  private Group group = null;
}
