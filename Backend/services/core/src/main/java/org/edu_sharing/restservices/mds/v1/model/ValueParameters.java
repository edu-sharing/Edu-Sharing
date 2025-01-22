package org.edu_sharing.restservices.mds.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;;

@Data
public class ValueParameters {

	@JsonProperty(required = true)
	private String query;
	@JsonProperty(required = true)
	private String property;
	@JsonProperty(required = true)
	@Schema(description = "prefix of the value (or \"-all-\" for all values)")
	private String pattern;
}
