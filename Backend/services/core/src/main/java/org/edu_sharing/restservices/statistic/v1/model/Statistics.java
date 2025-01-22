package org.edu_sharing.restservices.statistic.v1.model;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Statistics {
	@JsonProperty(required = true)
	private List<StatisticEntry> entries = new ArrayList<>();
}
