package org.edu_sharing.restservices.connector.v1.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

;

@Getter
@Setter
@Schema(description = "")
public class ConnectorList {
	@JsonProperty("url")
	private String url;
	@JsonProperty("connectors")
	private List<Connector> connectors;
	private List<Connector> simpleConnectors;
}
