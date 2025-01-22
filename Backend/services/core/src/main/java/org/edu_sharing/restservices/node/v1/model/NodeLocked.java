package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;;

@Data
public class NodeLocked {

	@JsonProperty(required = true, value = "isLocked")
	boolean locked = false;
}
