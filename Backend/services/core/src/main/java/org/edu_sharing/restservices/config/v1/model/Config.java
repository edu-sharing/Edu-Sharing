package org.edu_sharing.restservices.config.v1.model;

import lombok.Getter;
import lombok.Setter;
import org.edu_sharing.alfresco.service.config.model.Language;
import org.edu_sharing.alfresco.service.config.model.Values;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class Config {
	@JsonProperty private String contextId;
	@JsonProperty private Values current;
	@JsonProperty private Values global;
	@JsonProperty private Language language;
}
