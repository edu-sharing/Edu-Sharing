package org.edu_sharing.restservices.statistic.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class StatisticEntry {

	@JsonProperty(required = true)
	private String property;

	@JsonProperty(required = true)
	private List<StatisticEntity> entities = new ArrayList<>();
}
