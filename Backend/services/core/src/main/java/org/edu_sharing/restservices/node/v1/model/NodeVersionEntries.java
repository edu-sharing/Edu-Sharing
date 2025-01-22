package org.edu_sharing.restservices.node.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.NodeVersion;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class NodeVersionEntries  {

  @JsonProperty(required = true)
  private List<NodeVersion> versions = null;
}
