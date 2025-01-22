package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class NodeSearch {

	private List<String> ignored = null;

	@JsonProperty(required = true)
	private List<NodeRef> result = null;

	@JsonProperty(required = true)
	private List<Facet> facets = null;

	private List<Suggest> suggests = null;

	@JsonProperty(required = true)
	private Integer count = null;

	@JsonProperty(required = true)
	private Integer skip = null;
	private List<Node> nodes = null;


	@Data
	public static class Facet {

		@Data
		public static class Value {
			@JsonProperty(required = true)
			private String value = null;

			@JsonProperty(required = true)
			private Integer count = null;
		}

		@JsonProperty(required = true)
		private String property = null;
		@JsonProperty(required = true)
		private List<Value> values = new ArrayList<>();
		private Long sumOtherDocCount = null;
	}

	@Data
	public static class Suggest{
		@JsonProperty(required = true)
		@Schema(description = "suggested text")
		String text;

		@Schema(description = "suggested text with corrected words highlighted")
		String highlighted;

		@JsonProperty(required = true)
		@Schema(description = "score of the suggestion")
		double score;
	}
}
