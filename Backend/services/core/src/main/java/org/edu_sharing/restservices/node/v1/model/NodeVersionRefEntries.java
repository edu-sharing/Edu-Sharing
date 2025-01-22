package org.edu_sharing.restservices.node.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.NodeVersionRef;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class NodeVersionRefEntries  {

  @JsonProperty(required = true)
  private List<NodeVersionRef> versions = null;

}
