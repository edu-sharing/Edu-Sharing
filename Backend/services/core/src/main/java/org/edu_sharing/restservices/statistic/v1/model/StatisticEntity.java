package org.edu_sharing.restservices.statistic.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;;

@Data
public class StatisticEntity {
	@JsonProperty(required = true)
	private String value;
	@JsonProperty(required = true)
	private Integer count;
}
