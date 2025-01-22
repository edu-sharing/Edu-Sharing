package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.edu_sharing.restservices.shared.NodeSearch.Facet;
import com.fasterxml.jackson.annotation.JsonProperty;


@Data
public class SearchResult<T> {

	@JsonProperty(required = true)
	private List<T> nodes = new ArrayList<T>();
	@JsonProperty(required = true)
	private Pagination pagination = null;
	@JsonProperty(required = true)
	private List<Facet> facets = null;
	private List<NodeSearch.Suggest> suggests = null;
	private List<String> ignored;
}
