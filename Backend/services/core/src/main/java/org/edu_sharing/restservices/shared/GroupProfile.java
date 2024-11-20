package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.edu_sharing.repository.client.rpc.Group;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

;

@Getter
@Setter
@Schema(description = "")
public class GroupProfile implements Serializable {

  @JsonProperty
  private String displayName = null;
  @JsonProperty
  private String groupType = null;
  @JsonProperty
  private String groupEmail = null;
  @JsonProperty
  private String scopeType = null;
  @JsonProperty
  @JsonPropertyDescription("custom attributes for this group, can be consumed and provided by custom backend mappers")
  private Map<String, Serializable> customAttributes;

  public GroupProfile(){}

  public GroupProfile(GroupProfile GroupProfile) {
    displayName=GroupProfile.getDisplayName();
    groupType=GroupProfile.getGroupType();
    groupEmail=GroupProfile.getGroupEmail();
    scopeType=GroupProfile.getScopeType();
    customAttributes =new HashMap<>(GroupProfile.getCustomAttributes());
  }
  public GroupProfile(Group group) {
    displayName=group.getAuthorityDisplayName();
    groupType=group.getGroupType();
  }
}
