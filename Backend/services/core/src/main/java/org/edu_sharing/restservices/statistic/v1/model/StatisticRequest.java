package org.edu_sharing.restservices.statistic.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(description = "settings for statistic request")
public class StatisticRequest {
	
	Filter filter;
	
	List<String> properties = new ArrayList<>();
	
	@Schema(required = true, description = "")
	@JsonProperty("filter")
	public Filter getFilter() {
		return filter;
	}
	public void setFilter(Filter filter) {
		this.filter = filter;
	}
	
	@Schema(required = true, description = "")
	@JsonProperty("properties")
	public List<String> getProperties() {
		return properties;
	}
	
	public void setProperties(List<String> properties) {
		this.properties = properties;
	}
	
}
