package org.edu_sharing.restservices.collection.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.restservices.shared.Node;

@Data
public class CollectionEntry  {

  @JsonProperty(required = true)
  private Node collection = null;
}
