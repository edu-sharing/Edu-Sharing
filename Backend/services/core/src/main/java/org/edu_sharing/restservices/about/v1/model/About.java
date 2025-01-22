package org.edu_sharing.restservices.about.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class About  {

  @JsonProperty(required = true)
  private ServiceVersion version = null;

  private RenderingService renderingService2 = null;

  @JsonProperty(required = true)
  private List<AboutService> services = new ArrayList<>();

  private List<PluginInfo> plugins = new ArrayList<>();
  private List<FeatureInfo> features = new ArrayList<>();
  private String themesUrl;
  private long lastCacheUpdate;

}
