package org.edu_sharing.restservices.connector.v1.model;

import lombok.Data;

import java.util.List;

@Data
public class ConnectorList {
	private String url;
	private List<Connector> connectors;
	private List<Connector> simpleConnectors;
}
