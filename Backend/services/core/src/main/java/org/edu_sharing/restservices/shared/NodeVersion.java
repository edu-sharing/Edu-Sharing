package org.edu_sharing.restservices.shared;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;


@Data
public class NodeVersion  {

  @JsonProperty(required = true)
  private NodeVersionRef version = null;

  @JsonProperty(required = true)
  private String comment = null;

  @JsonProperty(required = true)
  private String modifiedAt = null;

  @JsonProperty(required = true)
  private Person modifiedBy = null;
  private String contentUrl = null;
  private Map<String, String[]> properties = null;

  @JsonIgnore
  public Map<String, String[]> getMetadata() {
    return properties;
  }

  @Override
  public String toString()  {
      return "class NodeVersion {\n" +
              "  version: " + version + "\n" +
              "  comment: " + comment + "\n" +
              "  modifiedAt: " + modifiedAt + "\n" +
              "  modifiedBy: " + modifiedBy + "\n" +
              "  contentUrl: " + contentUrl + "\n" +
              "  properties: " + properties + "\n" +
              "}\n";
  }
}
