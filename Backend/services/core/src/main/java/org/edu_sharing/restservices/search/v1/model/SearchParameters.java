package org.edu_sharing.restservices.search.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SearchParameters extends SearchParametersFacets {

	private List<String> permissions;
	private boolean resolveCollections = false;
	private boolean resolveUsernames = false;
	private boolean returnSuggestions = false;
	private List<String> excludes = new ArrayList<>();

	@Override
	@JsonProperty(required = false) // explicit set required false
	public List<String> getFacets() {
		return super.getFacets();
	}
}
