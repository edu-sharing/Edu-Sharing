package org.edu_sharing.restservices.node.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.NodePermissions;
import com.fasterxml.jackson.annotation.JsonProperty;


@Data
public class NodePermissionEntry  {
  @JsonProperty(required = true)
  private NodePermissions permissions = null;
}
