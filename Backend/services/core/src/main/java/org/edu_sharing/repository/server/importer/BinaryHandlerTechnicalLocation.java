package org.edu_sharing.repository.server.importer;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.w3c.dom.Node;

public class BinaryHandlerTechnicalLocation implements BinaryHandler{
	
	Logger logger = Logger.getLogger(BinaryHandlerTechnicalLocation.class);
	
	MCAlfrescoAPIClient mcAlfrescoAPIClient = null;
	
	public BinaryHandlerTechnicalLocation()  throws Throwable {
		ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();
		AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
		Map<String, String> authInfo = authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());
		mcAlfrescoAPIClient = (MCAlfrescoAPIClient) RepoFactory.getInstance(homeRep.getAppId(), authInfo);
	}
	
	@Override
	public void safe(String alfrescoNodeId, RecordHandlerInterfaceBase recordHandler, Node nodeRecord, String set) {
		importFromURL(alfrescoNodeId, recordHandler);
	}

	protected boolean importFromURL(String alfrescoNodeId, RecordHandlerInterfaceBase recordHandler) {
		String technicalLocation = (String) recordHandler.getProperties().get(CCConstants.LOM_PROP_TECHNICAL_LOCATION);
		String sourceId = (String) recordHandler.getProperties().get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
		if(technicalLocation!=null)
			technicalLocation = technicalLocation.trim();
		if(technicalLocation != null && technicalLocation.startsWith("http")){

			try{
				URL url = new URL(
						URIUtil.encodeQuery(technicalLocation)
				);
				URLConnection uc = url.openConnection();
				mcAlfrescoAPIClient.writeContent(MCAlfrescoAPIClient.storeRef, alfrescoNodeId, uc.getInputStream(), uc.getContentType(), null, CCConstants.CM_PROP_CONTENT);
				try(InputStream is = mcAlfrescoAPIClient.getContent(alfrescoNodeId)) {
					logger.info("Successfully imported data for " + sourceId + ": " + is.available() + " bytes");
				}
				return true;
			}catch(Throwable e){
				logger.error("Failed to import data for " + sourceId + ": " + technicalLocation + ": " + e.getMessage());
				logger.error(e.getMessage(), e);
			}

		}else{
			logger.error("don't know where to get this:"+technicalLocation);
		}
		return false;
	}
}
