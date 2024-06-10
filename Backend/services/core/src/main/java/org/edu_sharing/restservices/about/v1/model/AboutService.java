package org.edu_sharing.restservices.about.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class AboutService {
  
  private String name = null;
  private List<ServiceInstance> instances = new ArrayList<>();

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("instances")
  public List<ServiceInstance> getInstances() {
    return instances;
  }
  public void setInstances(List<ServiceInstance> instances) {
    this.instances = instances;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Service {\n");
    
    sb.append("  name: ").append(name).append("\n");
    sb.append("  instances: ").append(instances).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
