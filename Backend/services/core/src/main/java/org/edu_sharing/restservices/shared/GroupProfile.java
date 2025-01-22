package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import org.edu_sharing.repository.client.rpc.Group;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class GroupProfile implements Serializable {

  private String displayName = null;
  private String groupType = null;
  private String groupEmail = null;
  private String scopeType = null;
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
