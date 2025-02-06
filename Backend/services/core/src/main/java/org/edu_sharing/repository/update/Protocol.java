package org.edu_sharing.repository.update;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.UserEnvironmentToolFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Scope("prototype")
public class Protocol {

	private final AuthenticationService authenticationService;
	private final NodeService nodeService;
	private final UserEnvironmentToolFactory userEnvironmentToolFactory;

	MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();


//	@Deprecated
//	public Protocol(){
//		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
//		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
//
//		authenticationService = serviceRegistry.getAuthenticationService();
//		nodeService = serviceRegistry.getNodeService();
//	}
	@Autowired
	public Protocol(AuthenticationService authenticationService, NodeService nodeService, UserEnvironmentToolFactory userEnvironmentToolFactory){

		this.authenticationService = authenticationService;
		this.nodeService = nodeService;
		this.userEnvironmentToolFactory = userEnvironmentToolFactory;
	}
	
	public NodeRef getSysUpdateEntry(String updaterId) throws Throwable{
		
		Map<String,String> authInfo = new HashMap<>();
		authInfo.put(CCConstants.AUTH_USERNAME, authenticationService.getCurrentUserName());
		authInfo.put(CCConstants.AUTH_TICKET, authenticationService.getCurrentTicket());
		String eduSystemFolderUpdate;
		NodeRef updateInfoRef;
		eduSystemFolderUpdate = userEnvironmentToolFactory.createEnvironmentTool(ApplicationInfoList.getHomeRepository().getAppId(), authInfo)
				.getEdu_SharingSystemFolderUpdate();
		updateInfoRef = NodeServiceFactory.getLocalService().getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
				eduSystemFolderUpdate,
				CCConstants.CCM_TYPE_SYSUPDATE,
				CCConstants.CCM_PROP_SYSUPDATE_ID,
				updaterId
		);

		
		return updateInfoRef;
	}
	
	public void writeSysUpdateEntry(String updaterId) throws Throwable{
		Map<QName, Serializable> updateInfoProps = new HashMap<>();
		
		updateInfoProps.put(ContentModel.PROP_NAME, updaterId);
		updateInfoProps.put(QName.createQName(CCConstants.CCM_PROP_SYSUPDATE_ID),updaterId);
		updateInfoProps.put(QName.createQName(CCConstants.CCM_PROP_SYSUPDATE_DATE),new Date());
		
		Map<String,String> authInfo = new HashMap<>();
		authInfo.put(CCConstants.AUTH_USERNAME, authenticationService.getCurrentUserName());
		authInfo.put(CCConstants.AUTH_TICKET, authenticationService.getCurrentTicket());
		
		String eduSystemFolderUpdate = userEnvironmentToolFactory.createEnvironmentTool(ApplicationInfoList.getHomeRepository().getAppId(), authInfo)
				.getEdu_SharingSystemFolderUpdate();

		nodeService.createNode(new NodeRef(MCAlfrescoAPIClient.storeRef,eduSystemFolderUpdate), QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS),QName.createQName(updaterId), QName.createQName(CCConstants.CCM_TYPE_SYSUPDATE), updateInfoProps);
	}
}
