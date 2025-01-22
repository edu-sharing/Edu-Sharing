package org.edu_sharing.restservices.login.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScopeAccess  {

  @JsonProperty(required = true)
  private boolean hasAccess;

  
  public ScopeAccess(boolean hasAccess) {
	this.hasAccess = hasAccess;
  }
}
