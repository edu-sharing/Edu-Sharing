package org.edu_sharing.restservices.iam.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.Pagination;

import com.fasterxml.jackson.annotation.JsonProperty;

;

@Data
public class GroupEntries  {
  @JsonProperty(required = true)
  private List<Group> groups = null;

  @JsonProperty(required = true)
  private Pagination pagination = null;

}
