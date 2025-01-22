package org.edu_sharing.restservices.shared;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;;

@Data
public class NodePermissions {

	@JsonProperty(required = true)
	private ACL localPermissions = null;

	@JsonProperty(required = true)
	private List<ACE> inheritedPermissions = null;
}
