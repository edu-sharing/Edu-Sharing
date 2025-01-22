package org.edu_sharing.service.connector;

import org.edu_sharing.alfresco.service.connector.Connector;
import org.edu_sharing.alfresco.service.connector.ConnectorList;
import org.edu_sharing.alfresco.service.connector.ConnectorService;
import org.edu_sharing.alfresco.service.connector.SimpleConnector;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import java.util.ArrayList;
import java.util.List;

public class ConnectorServiceFactory {

	private static String CACHE_KEY = "CONNECTOR_CONFIG";

	public static ConnectorService getConnectorService(){
		return new ConnectorService();
	}

	public static ConnectorList getConnectorList(){
		return getConnectorList(ToolPermissionServiceFactory.getInstance());
	}
	public static ConnectorList getConnectorList(ToolPermissionService toolPermissionService){

		try{
			if(new MCAlfrescoAPIClient().isAdmin()){
				return getConnectorService().getConnectorList();
			}
		}catch(Exception e){

		}

		ConnectorList filteredList = new ConnectorList();

		List<Connector> filteredConnectors = new ArrayList<>();
		for(Connector connector : getConnectorService().getConnectorList().getConnectors()){
			if(toolPermissionService.hasToolPermissionForConnector(connector.getId())){
				filteredConnectors.add(connector);
			}
		}
		filteredList.setConnectors(filteredConnectors);
		if(getConnectorService().getConnectorList().getSimpleConnectors() != null) {
			List<SimpleConnector> simpleConnectors = new ArrayList<>();
			for (SimpleConnector connector : getConnectorService().getConnectorList().getSimpleConnectors()) {
				if (toolPermissionService.hasToolPermissionForConnector(connector.getId())) {
					simpleConnectors.add(connector);
				}
			}
			filteredList.setSimpleConnectors(simpleConnectors);
		}
		return filteredList;
	}
}
