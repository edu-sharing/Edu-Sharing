package org.edu_sharing.restservices.config.v1.model;

import lombok.Data;
import org.edu_sharing.alfresco.service.config.model.Language;
import org.edu_sharing.alfresco.service.config.model.Values;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class Config {
	private String contextId;
	private Values current;
	private Values global;
	private Language language;
}
