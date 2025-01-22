package org.edu_sharing.restservices.node.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.NodeVersion;
import com.fasterxml.jackson.annotation.JsonProperty;


@Data
public class NodeVersionEntry  {

  @JsonProperty(required = true)
  private NodeVersion version = null;
}
