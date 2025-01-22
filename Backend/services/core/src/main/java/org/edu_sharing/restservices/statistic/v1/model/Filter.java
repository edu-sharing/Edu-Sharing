package org.edu_sharing.restservices.statistic.v1.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;;

@Data
public class Filter {
	@JsonProperty(required = true)
	private List<FilterEntry> entries;
}
