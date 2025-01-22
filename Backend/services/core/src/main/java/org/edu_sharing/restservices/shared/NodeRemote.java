package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;;

@Data
public class NodeRemote {
	@JsonProperty(required = true)
	Node node;

	@JsonProperty(required = true)
	Node remote;
}
