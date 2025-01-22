package org.edu_sharing.restservices.search.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;

import java.util.List;

@Data
public class SearchParametersFacets {
    @JsonProperty(required = true)
    private List<String> facets;
    @Schema(defaultValue = "5")
    Integer facetMinCount = 5;
    @Schema(defaultValue = "10")
    Integer facetLimit = 10;
    String facetSuggest;
    @JsonProperty(required = true)
    private List<MdsQueryCriteria> criteria;
}
