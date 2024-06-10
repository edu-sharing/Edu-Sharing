package org.edu_sharing.restservices.about.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class About  {
  
  private ServiceVersion version = null;
  private List<AboutService> services = new ArrayList<>();
  private List<PluginInfo> plugins = new ArrayList<>();
  private String themesUrl;
  private long lastCacheUpdate;

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("version")
  public ServiceVersion getVersion() {
    return version;
  }
  public void setVersion(ServiceVersion version) {
    this.version = version;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("services")
  public List<AboutService> getServices() {
    return services;
  }
  public void setServices(List<AboutService> services) {
    this.services = services;
  }

  @JsonProperty
  public long getLastCacheUpdate() {
    return lastCacheUpdate;
  }

  public void setLastCacheUpdate(long lastCacheUpdate) {
    this.lastCacheUpdate = lastCacheUpdate;
  }

  public List<PluginInfo> getPlugins() {
    return plugins;
  }

  public void setPlugins(List<PluginInfo> plugins) {
    this.plugins = plugins;
  }

  @JsonProperty
  public String getThemesUrl() {
	return themesUrl;
	}
	public void setThemesUrl(String themesUrl) {
		this.themesUrl = themesUrl;
	}
@Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class About {\n");
    
    sb.append("  version: ").append(version).append("\n");
    sb.append("  services: ").append(services).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
