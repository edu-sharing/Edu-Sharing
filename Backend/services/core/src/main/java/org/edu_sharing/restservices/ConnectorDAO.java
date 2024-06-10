package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.Arrays;

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
		
		ApplicationInfo service = ApplicationInfoList.getRepositoryInfoByType(ApplicationInfo.TYPE_CONNECTOR);
		if(service==null){
			logger.debug("No connector registered, register a connector via admin tools first");
			return result;
		}
		result.setUrl(service.getContentUrl());
		
		for(org.edu_sharing.alfresco.service.connector.Connector connector : connectorList.getConnectors()){
			Connector resultConnector = new Connector();
			
			for(org.edu_sharing.alfresco.service.connector.ConnectorFileType cft : connector.getFiletypes()){
				ConnectorFileType cftResult = new ConnectorFileType();
				cftResult.setCcresourcesubtype(cft.getCcresourcesubtype());
				cftResult.setCcressourcetype(cft.getCcressourcetype());
				cftResult.setCcressourceversion(cft.getCcressourceversion());
				cftResult.setEditorType(cft.getEditorType());
				cftResult.setCreatable(cft.isCreateable());
				cftResult.setEditable(cft.isEditable());
				cftResult.setFiletype(cft.getFiletype());
				cftResult.setMimetype(cft.getMimetype());
				
				ConnectorFileType[] fileTypes = resultConnector.getFiletypes();
				if(fileTypes == null || fileTypes.length == 0){
					fileTypes = new ConnectorFileType[]{cftResult};
					resultConnector.setFiletypes(fileTypes);
				}else{
					ArrayList<ConnectorFileType> list = new ArrayList<>(Arrays.asList(fileTypes));
					list.add(cftResult);
					resultConnector.setFiletypes(list.toArray(new ConnectorFileType[list.size()]));
				}
			}
			
			resultConnector.setIcon(connector.getIcon());
			resultConnector.setId(connector.getId());
			resultConnector.setOnlyDesktop(connector.isOnlyDesktop());
			resultConnector.setHasViewMode(connector.isHasViewMode());
			if(connector.getParameters() != null){
				resultConnector.setParameters(connector.getParameters().toArray(new String[0]));
			}
			resultConnector.setShowNew(connector.isShowNew());
			
			Connector[] connectors = result.getConnectors();
			if(connectors == null || connectors.length == 0){
				result.setConnectors(new Connector[]{resultConnector});
			}else{
				ArrayList<Connector> resultConnectorList  = new ArrayList<>(Arrays.asList(connectors));
				resultConnectorList.add(resultConnector);
				result.setConnectors(resultConnectorList.toArray(new Connector[0]));
			}
		
		}
		
		return result;
		
	}
}
