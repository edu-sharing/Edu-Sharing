package org.edu_sharing.restservices.node.v1.model;


import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.shared.NodeSearch.Facet;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class SearchResult {

	@JsonProperty(required = true)
	private List<Node> nodes = new ArrayList<>();

	@JsonProperty(required = true)
	private Pagination pagination;

	@JsonProperty(required = true)
	private List<Facet> facets;
}
