package org.edu_sharing.restservices.search.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

;

@Getter
@Setter
@Schema(description = "")
public class SearchParameters extends SearchParametersFacets{

	private List<String> permissions;
	private boolean resolveCollections = false;
	private boolean resolveUsernames = false;
	private boolean returnSuggestions = false;
	private List<String> excludes = new ArrayList<>();

	@JsonProperty
	public List<String> getPermissions() {
		return permissions;
	}

	@Schema(required = false, description = "")
	@JsonProperty("facets")
	public List<String> getFacets() { return super.getFacets();}
}
