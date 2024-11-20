package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.connector.v1.model.Connector;
import org.edu_sharing.restservices.connector.v1.model.ConnectorFileType;
import org.edu_sharing.restservices.connector.v1.model.ConnectorList;
import org.edu_sharing.service.connector.ConnectorServiceFactory;

public class ConnectorDAO {
	
	private static Logger logger = Logger.getLogger(ConnectorDAO.class);
	
	public static ConnectorList getConnectorList() throws DAOException{
		
		ConnectorList result = new ConnectorList();
		
		org.edu_sharing.alfresco.service.connector.ConnectorList connectorList = ConnectorServiceFactory.getConnectorList();

		// simple connectors do not require any service!
		result.setSimpleConnectors(connectorList.getSimpleConnectors().stream().map(
				connector -> {
					Connector resultConnector = new Connector();
					mapConnectorFiletypes(connector.getFiletypes(), resultConnector);

					resultConnector.setIcon(connector.getIcon());
					resultConnector.setId(connector.getId());
					resultConnector.setOnlyDesktop(connector.isOnlyDesktop());
					resultConnector.setShowNew(connector.isShowNew());
					resultConnector.setMdsGroup(connector.getMdsGroup());
					return resultConnector;
				}

		).collect(Collectors.toList()));

		ApplicationInfo service = ApplicationInfoList.getRepositoryInfoByType(ApplicationInfo.TYPE_CONNECTOR);
		if(service==null){
			logger.debug("No connector registered, register a connector via admin tools first");
			return result;
		}
		result.setUrl(service.getContentUrl());
		result.setConnectors(connectorList.getConnectors().stream().map(
				connector -> {
					Connector resultConnector = new Connector();
					mapConnectorFiletypes(connector.getFiletypes(), resultConnector);

					resultConnector.setIcon(connector.getIcon());
					resultConnector.setId(connector.getId());
					resultConnector.setOnlyDesktop(connector.isOnlyDesktop());
					resultConnector.setHasViewMode(connector.isHasViewMode());
					if(connector.getParameters() != null){
						resultConnector.setParameters(connector.getParameters().toArray(new String[0]));
					}
					resultConnector.setShowNew(connector.isShowNew());
					return resultConnector;
				}

		).collect(Collectors.toList()));
		return result;

	}

	private static void mapConnectorFiletypes(List<org.edu_sharing.alfresco.service.connector.ConnectorFileType> fileTypes, Connector resultConnector) {
		for(org.edu_sharing.alfresco.service.connector.ConnectorFileType cft : fileTypes){
			ConnectorFileType cftResult = new ConnectorFileType();
			cftResult.setCcresourcesubtype(cft.getCcresourcesubtype());
			cftResult.setCcressourcetype(cft.getCcressourcetype());
			cftResult.setCcressourceversion(cft.getCcressourceversion());
			cftResult.setEditorType(cft.getEditorType());
			cftResult.setCreatable(cft.isCreateable());
			cftResult.setEditable(cft.isEditable());
			cftResult.setFiletype(cft.getFiletype());
			cftResult.setMimetype(cft.getMimetype());

			ConnectorFileType[] types = resultConnector.getFiletypes();
			if(types == null || types.length == 0){
				types = new ConnectorFileType[]{cftResult};
				resultConnector.setFiletypes(types);
			}else{
				ArrayList<ConnectorFileType> list = new ArrayList<>(Arrays.asList(types));
				list.add(cftResult);
				resultConnector.setFiletypes(list.toArray(new ConnectorFileType[list.size()]));
			}
		}
	}
}
