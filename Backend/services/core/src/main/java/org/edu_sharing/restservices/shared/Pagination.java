package org.edu_sharing.restservices.shared;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.repository.server.SearchResultNodeRef;

@Data
public class Pagination  {

  @JsonProperty(required = true)
  private Integer total = null;
  @JsonProperty(required = true)
  private Integer from = null;
  @JsonProperty(required = true)
  private Integer count = null;

  public Pagination(){}
  public Pagination(org.edu_sharing.service.search.model.SearchResult<?> result) {
	  from=result.getSkipCount();
	  total=result.getTotalCount();
	  count=result.getCount();
  }
  public Pagination(SearchResultNodeRef result) {
    from=result.getStartIDX();
    total=result.getNodeCount();
    count=result.getData().size();
  }
  public <T extends Node>  Pagination(List<T> result) {
    from=0;
    total=result.size();
    count=result.size();
  }
}
