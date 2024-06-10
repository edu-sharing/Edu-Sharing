package org.edu_sharing.restservices.shared;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;


@Schema(description = "")
public class NodeVersion  {
  
  private NodeVersionRef version = null;
  private String comment = null;
  private String modifiedAt = null;
  private Person modifiedBy = null;
  private String contentUrl = null;
  private Map<String, String[]> properties = null;

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty(value = "version")
  public NodeVersionRef getVersion() {
    return version;
  }
  public void setVersion(NodeVersionRef version) {
    this.version = version;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty(value = "comment")
  public String getComment() {
    return comment;
  }
  public void setComment(String comment) {
    this.comment = comment;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty(value = "modifiedAt")
  public String getModifiedAt() {
    return modifiedAt;
  }
  public void setModifiedAt(String modifiedAt) {
    this.modifiedAt = modifiedAt;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty(value = "modifiedBy")
  public Person getModifiedBy() {
    return modifiedBy;
  }
  public void setModifiedBy(Person modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  
  /**
   **/
  @Schema(description = "")
  @JsonProperty(value = "contentUrl")
  public String getContentUrl() {
    return contentUrl;
  }
  public void setContentUrl(String contentUrl) {
    this.contentUrl = contentUrl;
  }

  /**
   **/
  @Schema(description = "")
  @JsonProperty(value = "properties")
  public Map<String, String[]> getMetadata() {
    return properties;
  }
  public void setProperties(Map<String, String[]> metadata) {
    this.properties = metadata;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeVersion {\n");
    
    sb.append("  version: ").append(version).append("\n");
    sb.append("  comment: ").append(comment).append("\n");
    sb.append("  modifiedAt: ").append(modifiedAt).append("\n");
    sb.append("  modifiedBy: ").append(modifiedBy).append("\n");
    sb.append("  contentUrl: ").append(contentUrl).append("\n");
    sb.append("  properties: ").append(properties).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
