package org.edu_sharing.restservices.shared;


import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.repository.server.SearchResultNodeRef;


@Schema(description = "")
public class Pagination  {
  
  private Integer total = null;
  private Integer from = null;
  private Integer count = null;

  public Pagination(){}
  public Pagination(org.edu_sharing.service.search.model.SearchResult result) {
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
/**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("total")
  public Integer getTotal() {
    return total;
  }
  public void setTotal(Integer total) {
    this.total = total;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("from")
  public Integer getFrom() {
    return from;
  }
  public void setFrom(Integer from) {
    this.from = from;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("count")
  public Integer getCount() {
    return count;
  }
  public void setCount(Integer count) {
    this.count = count;
  }

}
