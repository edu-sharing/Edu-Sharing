package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class NodeVersionRef  {

  @JsonProperty(required = true)
  private NodeRef node = null;
  @JsonProperty(required = true)
  private int major = 0;
  @JsonProperty(required = true)
  private int minor = 0;

  @Override
  public String toString()  {
      return "class NodeVersionRef {\n" +
              "  node: " + node + "\n" +
              "  major: " + major + "\n" +
              "  minor: " + minor + "\n" +
              "}\n";
  }
}
