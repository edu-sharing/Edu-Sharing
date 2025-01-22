package org.edu_sharing.restservices.statistic.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FilterEntry {

    @JsonProperty(required = true)
    private String property;

    @JsonProperty(required = true)
    private String[] values;

}
