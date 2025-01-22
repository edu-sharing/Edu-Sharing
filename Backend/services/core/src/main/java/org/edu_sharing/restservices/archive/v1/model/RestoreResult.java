package org.edu_sharing.restservices.archive.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestoreResult {

	@JsonProperty(required = true)
	String archiveNodeId;

	@JsonProperty(required = true)
	String nodeId;

	@JsonProperty(required = true)
	String parent;

	@JsonProperty(required = true)
	String path;

	@JsonProperty(required = true)
	String name;

	@JsonProperty(required = true)
	String restoreStatus;
}
