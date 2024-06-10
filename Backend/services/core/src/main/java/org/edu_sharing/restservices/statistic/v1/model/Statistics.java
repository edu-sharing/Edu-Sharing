package org.edu_sharing.restservices.statistic.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;;

public class Statistics {
	
	List<StatisticEntry> entries = new ArrayList<>();
	
	public void setEntries(List<StatisticEntry> entries) {
		this.entries = entries;
	}
	
	@Schema(required = true, description = "")
	@JsonProperty("entries")
	public List<StatisticEntry> getEntries() {
		return entries;
	}
}
