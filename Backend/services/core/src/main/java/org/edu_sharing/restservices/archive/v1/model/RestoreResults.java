package org.edu_sharing.restservices.archive.v1.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;;

@Data
public class RestoreResults {

	@JsonProperty(required = true)
	private List<RestoreResult> results;
}
