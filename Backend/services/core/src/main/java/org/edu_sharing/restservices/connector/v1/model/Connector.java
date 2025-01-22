package org.edu_sharing.restservices.connector.v1.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;;

@Data
public class Connector {
	private String id;
	private String icon;
	@JsonProperty(required = true)
	private boolean showNew;
	private String[] parameters;
	private ConnectorFileType[] filetypes;
	private boolean onlyDesktop;
	private boolean hasViewMode;
	private String mdsGroup;
}
