package org.edu_sharing.restservices.organization.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.Organization;
import org.edu_sharing.restservices.shared.Pagination;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;


@Data
public class OrganizationEntries  {
  
  private List<Organization> list = null;
  private Pagination pagination=null;
  private boolean canCreate=false;
  
  @JsonProperty("canCreate")
  public boolean isCanCreate() {
	return canCreate;
  }
  public void setCanCreate(boolean canCreate) {
	this.canCreate = canCreate;
  }
/**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("organizations")
  public List<Organization> getList() {
    return list;
  }
  public void setList(List<Organization> list) {
    this.list = list;
  }
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("pagination")
  public Pagination getPagination() {
    return pagination;
  }
  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

}
