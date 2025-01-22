package org.edu_sharing.restservices.about.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;;

@Data
public class ServiceInstance  {

  @JsonProperty(required = true)
  private ServiceVersion version = null;

  @JsonProperty(required = true)
  private String endpoint = null;
}
