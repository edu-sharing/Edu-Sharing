package org.edu_sharing.restservices.about.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AboutService {

  @JsonProperty(required = true)
  private String name = null;

  @JsonProperty(required = true)
  private List<ServiceInstance> instances = new ArrayList<>();
}
