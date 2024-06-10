package org.edu_sharing.restservices.node.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.ArrayList;
import java.util.List;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.shared.NodeSearch.Facet;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchResult {

	private List<Node> nodes = new ArrayList<>();
	private Pagination pagination = null;
	private List<Facet> facets = null;

	/**
	   **/
	@Schema(required = true, description = "")
	@JsonProperty("nodes")
	public List<Node> getNodes() {
		return nodes;
	}

	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}

	/**
	   **/
	@Schema(required = true, description = "")
	@JsonProperty("pagination")
	public Pagination getPagination() {
		return pagination;
	}

	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}

	/**
   **/
	@Schema(required = true, description = "")
	@JsonProperty("facets")
	public List<Facet> getFacets() {
		return facets;
	}

	public void setFacets(List<Facet> facets) {
		this.facets = facets;
	}

}
