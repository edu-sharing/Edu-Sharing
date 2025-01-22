package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;;

@Data
public class Suggestions {

	@JsonProperty(required = true)
	private List<Suggestion> values;

	@Data
	public static class Suggestion{

		@JsonProperty(required = true)
		private String replacementString;

		@JsonProperty(required = true)
		private String displayString;
		private String key;
	}
}
