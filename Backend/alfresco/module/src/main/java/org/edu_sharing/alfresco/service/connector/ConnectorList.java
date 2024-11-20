package org.edu_sharing.alfresco.service.connector;

import com.typesafe.config.Optional;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ConnectorList implements Serializable {

	List<Connector> connectors;
	@Optional
	List<SimpleConnector> simpleConnectors = new ArrayList<>();
}
