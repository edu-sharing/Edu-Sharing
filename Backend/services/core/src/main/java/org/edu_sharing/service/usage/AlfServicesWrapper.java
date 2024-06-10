/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.service.usage;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.*;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail2.core.EmailException;
import org.edu_sharing.alfresco.HasPermissionsWork;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.Mail;
import org.springframework.context.ApplicationContext;

/**
 * 		   This class is used by the webservice and authentication
 *         classes that are deployed in alfresco context. So it is not hot
 *         deployable. For acessing the alfresco services, the idea is to have
 *         separate classes for the ccsearch context and the alfresco context.
 *         so we ensure that the classes used by ccsearch context are
 *         hotdeployable. disadvantage: redundant code
 */
public class AlfServicesWrapper implements UsageDAO{

	private static Log logger = LogFactory.getLog(AlfServicesWrapper.class);
	ServiceRegistry serviceRegistry = null;
	AuthenticationService authenticationService = null;
	NodeService nodeService = null;
	OwnableService ownableService = null;

	SearchService searchService = null;

	ApplicationContext applicationContext = null;

	public static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

	public AlfServicesWrapper() {
		applicationContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		authenticationService = serviceRegistry.getAuthenticationService();
		searchService = serviceRegistry.getSearchService();
		ownableService = serviceRegistry.getOwnableService();
		nodeService = serviceRegistry.getNodeService();
	}

	public AlfServicesWrapper(Map<String,String> authInfo) {
		this();
		authenticationService.validate((String) authInfo.get(CCConstants.AUTH_TICKET));
	}

	public Map<String, String> authenticate(String username, String password) {
		authenticationService.authenticate(username, password.toCharArray());
		String ticket = authenticationService.getCurrentTicket();
		Map<String, String> authInfo = new HashMap<>();
		authInfo.put(CCConstants.AUTH_USERNAME, username);
		authInfo.put(CCConstants.AUTH_TICKET, ticket);
		return authInfo;
	}

	public Map<String, Map<String, Object>> getUsages(String nodeId) {
		Map<String, Map<String, Object>> result = getChildrenByType(nodeId, CCConstants.CCM_TYPE_USAGE);
		return result;
	}
	
	public Map<String,Object> getProperties(NodeRef nodeRef){
		Map<QName, Serializable> childPropMap = nodeService.getProperties(nodeRef);
		Map<String, Object> resultProps = new HashMap<>();
		for (QName qname : childPropMap.keySet()) {

			Serializable object = childPropMap.get(qname);
			String value = null;

			if (object instanceof ArrayList) {

				// TODO allow multivalues
				value = (String) ((ArrayList) object).get(0);
				
			} else if(object instanceof Date) {
				value = new Long(((Date)object).getTime()).toString();
			} else if(object != null){

				value = object.toString();
			}
			value = formatData(qname.toString(), value);
			resultProps.put(qname.toString(), value);
		}
		
		return resultProps;
	}

	@Override
	public Map<String, Object> getUsageOnNodeOrParents(String lmsId, String courseId, String objectNodeId, String resourceId) throws Exception {
		NodeRef nodeId=new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,objectNodeId);
		while(nodeId!=null) {
			Map<String, Object> usage = getUsage(lmsId, courseId, nodeId.getId(), resourceId);
			if(usage!=null)
				return usage;
			nodeId=nodeService.getPrimaryParent(nodeId).getParentRef();
		}
		return null;
	}

	@Override
	public Map<String, Object> getUsage(String lmsId, String courseId, String objectNodeId, String resourceId) throws Exception {
		Map<String, Map<String, Object>> children = getChildrenByType(objectNodeId, CCConstants.CCM_TYPE_USAGE);
		for (String key : children.keySet()) {
			Map<String, Object> usageNode = children.get(key);
			String tmpAppId = (String) usageNode.get(CCConstants.CCM_PROP_USAGE_APPID);
			String tmpCourseId = (String) usageNode.get(CCConstants.CCM_PROP_USAGE_COURSEID);
			// String tmpAppUser = (String)usageNode.get(CCConstants.CCM_PROP_USAGE_APPUSER);
			String tmpObjectNodeId = (String) usageNode.get(CCConstants.CCM_PROP_USAGE_PARENTNODEID);
			String tmpResourceId = (String) usageNode.get(CCConstants.CCM_PROP_USAGE_RESSOURCEID);

			if (lmsId != null
					&& lmsId.equals(tmpAppId)
					&& courseId != null
					&& courseId.equals(tmpCourseId)
					&& objectNodeId != null
					&& objectNodeId.equals(tmpObjectNodeId)
					&& ((resourceId == null && tmpResourceId == null) || (resourceId != null && resourceId
							.equals(tmpResourceId)))) {
				return usageNode;
			}
		}
		return null;
	}
	
	@Override
	public Map<String, Object> getUsage(String usageId) throws Exception {
		
		return getProperties(new NodeRef(storeRef,usageId));
	}

	public Map<String, Map<String, Object>> getUsagesByCourse(String lmsId, String courseId) throws Exception {
		Map<String, Map<String, Object>> result = new HashMap<>();

		// prevent getting all usages by using * as courseId
		if(courseId.contains("*")){
			logger.error("courseId:" + courseId + " is not valid");
			return null;
		}
		
		String queryString = "TYPE:\"{http://www.campuscontent.de/model/1.0}usage\" AND @ccm\\:usagecourseid:" + courseId +" AND @ccm\\:usageappid:" + lmsId;
		ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, queryString);
		for (NodeRef nodeRef : resultSet.getNodeRefs()) {
			
			try{
				Map<QName, Serializable> tmpprops = nodeService.getProperties(nodeRef);
				Map<String, Object> props = new HashMap<>();
				for (QName key : tmpprops.keySet()) {
					String propName = key.toString();
					Object propValue = tmpprops.get(key);
					if(propValue != null) props.put(propName, propValue.toString());
					ChildAssociationRef childssocRef = nodeService.getPrimaryParent(nodeRef);
					props.put(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID, childssocRef.getParentRef().getId());
				}
				result.put(nodeRef.getId(), props);
			}catch(org.alfresco.service.cmr.repository.InvalidNodeRefException e){
				logger.error("nodeRef: "+nodeRef+" does not exist. maybe an archived usage node:"+e.getMessage());
			}
		}
		return result;
	}
	
	@Override
	public Map<String, Map<String, Object>> getUsagesByAppId(String appId) throws Exception {
		Map<String, Map<String, Object>> result = new HashMap<>();
		String queryString = "TYPE:\"{http://www.campuscontent.de/model/1.0}usage\" AND @ccm\\:usageappid:" + appId;
		ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, queryString);
		for (NodeRef nodeRef : resultSet.getNodeRefs()) {
			
			try{
				Map<QName, Serializable> tmpprops = nodeService.getProperties(nodeRef);
				Map<String, Object> props = new HashMap<>();
				for (QName key : tmpprops.keySet()) {
					String propName = key.toString();
					Object propValue = tmpprops.get(key);
					if(propValue != null) props.put(propName, propValue.toString());
				}
				ChildAssociationRef childssocRef = nodeService.getPrimaryParent(nodeRef);
				props.put(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID, childssocRef.getParentRef().getId());
				
				result.put(nodeRef.getId(), props);
			}catch(org.alfresco.service.cmr.repository.InvalidNodeRefException e){
				logger.error("nodeRef: "+nodeRef+" does not exist. maybe an archived usage node:"+e.getMessage());
			}
		}
		return result;
	}
	
	@Override
	public Map<String, Map<String, Object>> getUsages(String repositoryId, String nodeId, Long from, Long to) throws Exception {
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
		if(appInfo == null) {
			throw new Exception("unknown application " +repositoryId);
		}
		
		if(!ApplicationInfo.TYPE_REPOSITORY.equals(appInfo.getType())) {
			throw new Exception("application " + repositoryId +" is not an repository");
		}
			
		if(repositoryId.equals("-home-")) {
			repositoryId = ApplicationInfoList.getHomeRepository().getAppId();
		}
		
		final String repositoryIdF = repositoryId;
		
		
		
		RunAsWork<Map<String, Map<String, Object>>> runAs = () -> {

            Map<String, Map<String, Object>> result = new HashMap<>();

            if(ApplicationInfoList.getHomeRepository().getAppId().equals(appInfo.getAppId())) {
                String queryString = "TYPE:\"{http://www.campuscontent.de/model/1.0}usage\"";
                if(nodeId != null && nodeId.trim().length() > 0) {
                    queryString += " AND @ccm\\:usageparentnodeid:" + nodeId;
                }
                if(from != null) {

                    String fromFormated = ISO8601DateFormat.format(new Date(from));

                    Long to2 = (to == null) ? new Date().getTime() : to;
                    String toFormated = ISO8601DateFormat.format(new Date(to2));
                    queryString += " AND @cm\\:created:[" + fromFormated + " TO " + toFormated + "]";
                }

                ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, queryString);
                for (NodeRef nodeRef : resultSet.getNodeRefs()) {

                    try{

                        Map<String, Object> props = getProperties(nodeRef);
                        ChildAssociationRef childssocRef = nodeService.getPrimaryParent(nodeRef);
                        props.put(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID, childssocRef.getParentRef().getId());

                        result.put(nodeRef.getId(), props);
                    }catch(InvalidNodeRefException e){
                        logger.error("nodeRef: "+nodeRef+" does not exist. maybe an archived usage node:"+e.getMessage());
                    }
                }
            }else {
                String queryString = "TYPE:\"{http://www.campuscontent.de/model/1.0}remoteobject\" AND @ccm\\:remoterepositoryid:" + repositoryIdF;
                if(nodeId != null && nodeId.trim().length() > 0) {
                    queryString += " AND @ccm\\:remotenodeid:" + nodeId;
                }
                if(from != null) {

                    String fromFormated = ISO8601DateFormat.format(new Date(from));

                    Long to2 = (to == null) ? new Date().getTime() : to;
                    String toFormated = ISO8601DateFormat.format(new Date(to2));
                    queryString += " AND @cm\\:created:[" + fromFormated + " TO " + toFormated + "]";
                }

                ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, queryString);
                for (NodeRef nodeRef : resultSet.getNodeRefs()) {

                    try{

                        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
                        for(ChildAssociationRef childRef : childAssocs) {
                            if(QName.createQName(CCConstants.CCM_TYPE_USAGE).equals(nodeService.getType(childRef.getChildRef()))){

                                Map<String, Object> props = getProperties(childRef.getChildRef());
                                ChildAssociationRef childssocRef = nodeService.getPrimaryParent(childRef.getChildRef());
                                props.put(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID, childssocRef.getParentRef().getId());

                                result.put(childRef.getChildRef().getId(), props);
                            }
                        }

                    }catch(InvalidNodeRefException e){
                        logger.error("nodeRef: "+nodeRef+" does not exist. maybe an archived usage node:"+e.getMessage());
                    }
                }
            }

            return result;
        };
		
		return AuthenticationUtil.runAsSystem(runAs);
	}

	public Map<String, Map<String, Object>> getChildrenByType(String nodeId, String type) {
		return this.getChildrenByType(storeRef, nodeId, type);
	}

	public Map<String, Map<String, Object>> getChildrenByType(StoreRef store, String nodeId, String type) {
		Map<String, Map<String, Object>> result = new HashMap<>();
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(new NodeRef(store, nodeId),Collections.singleton(QName.createQName(type)));
		for (ChildAssociationRef child : childAssocList) {
			result.put(child.getChildRef().getId(), getProperties(child.getChildRef()));
		}
		return result;
	}
	
	public static String formatData(String key, Object value) {
		String returnValue = null;
		if (key != null && value != null) {
						
			if (value instanceof Date) {
				logger.info("value is instanceof date");
				DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.GERMANY);
				
				try {
					
					if (value instanceof Date) {
						returnValue = df.format((Date) value);
					}

				} catch (Exception e) {
					logger.error("Exception", e);
				}
			}
			if (value instanceof String) {
				returnValue = (String) value;
			}
			if (value instanceof Number) {
				returnValue = value.toString();
			}
		}
		return returnValue;
	}

	public void removeNode(String nodeID, String fromID) {
		nodeService.removeChild(new NodeRef(storeRef, fromID), new NodeRef(storeRef, nodeID));
	}

	public boolean hasPermissions(String nodeId, String authority, String[] permissions) throws Exception {

		Map<String, Boolean> hasPResult = null;
		try {
			
			ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
			ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
			OwnableService ownableService = serviceRegistry.getOwnableService();
			PermissionService permissionService = serviceRegistry.getPermissionService();
			
			String userId = authority;
			
			if (userId.equals(PermissionService.OWNER_AUTHORITY)) {
				userId = ownableService.getOwner(new NodeRef(storeRef, nodeId));
				logger.info(PermissionService.OWNER_AUTHORITY + " mapping on userId:" + userId);
			}

			hasPResult = AuthenticationUtil.runAs(new HasPermissionsWork(permissionService, userId, permissions, nodeId), userId);
			
			for (String permission : permissions) {
				Boolean tmpBool = hasPResult.get(permission);
				if (tmpBool == null || tmpBool.booleanValue() == false) {
					return false;
				}
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}

		return true;

	}

	public void updateNode(String nodeId, Map<String, Object> _props) {
		this.updateNode(storeRef, nodeId, _props);
	}

	public void updateNode(StoreRef store, String nodeId, Map<String, Object> _props) {

		Map<QName, Serializable> props = transformPropMap(_props);
		NodeRef nodeRef = new NodeRef(store, nodeId);

		// don't do this cause it overwrites all Properties even the content
		// nodeService.setProperties(new NodeRef(store, nodeId), props);
		for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
			nodeService.setProperty(nodeRef, entry.getKey(), entry.getValue());
		}

	}

	Map<QName, Serializable> transformPropMap(Map<String, ?> map) {
		Map<QName, Serializable> result = new HashMap<>();
		for (String key : map.keySet()) {
			result.put(QName.createQName(key), (Serializable)map.get(key));
		}
		return result;
	}

	public String createNode(String parentID, String nodeTypeString, String childAssociation, Map<String, String> _props) {
		return this.createNode(storeRef, parentID, nodeTypeString, childAssociation, _props);
	}

	public String createNode(StoreRef store, String parentID, String nodeTypeString, String childAssociation,
			Map<String, String> _props) {

		Map<QName, Serializable> properties = transformPropMap(_props);

		NodeRef parentNodeRef = new NodeRef(store, parentID);
		QName nodeType = QName.createQName(nodeTypeString);
		ChildAssociationRef childRef = nodeService.createNode(parentNodeRef, QName.createQName(childAssociation), QName
				.createQName(childAssociation), nodeType, properties);
		return childRef.getChildRef().getId();
	}

	public String createPersonAccessElement(String personId, String username, String applicationId, Boolean accessAllowed,
			String accesskey){
		logger.info("starting... personId:" + personId + " username:" + username + " applicationId:" + applicationId
				+ " accessAllowed:" + accessAllowed + " accesskey:" + accesskey);
		Map<String, String> persAccEleProps = new HashMap<>();
		if (accessAllowed == null) {
			persAccEleProps.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCACCESS, new Boolean(false).toString());
		} else {
			persAccEleProps.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCACCESS, accessAllowed.toString());
		}
		persAccEleProps.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCACTIVATEKEY, accesskey);
		persAccEleProps.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCAPPID, applicationId);
		persAccEleProps.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCUSERID, username);

		String persAccEleId = createNode(personId, CCConstants.CM_TYPE_PERSONACCESSELEMENT,
				CCConstants.CM_ASSOC_PERSON_ACCESSLIST, persAccEleProps);

		logger.info("returning:" + persAccEleId);
		return persAccEleId;
	}

	public void sendActivationRequestMail(String receivermail, String applicationId, String username, String accesskey)
			throws EmailException {
		logger.info("start sending ActivationReauestMail...");
		ApplicationInfo homerepository = ApplicationInfoList.getHomeRepository();
		
		String domain = homerepository.getDomain();
		if(domain == null || domain.trim().equals("")){
			domain = homerepository.getHost();
		}
		
		String activateApplicationLink = homerepository.getClientprotocol()+"://" + domain + ":" + homerepository.getClientport() + "/"
				+ homerepository.getWebappname()+"/appactivation?appId=" + applicationId + "&appUserId=" + username + "&key=" + accesskey + "&mail="
				+ receivermail;
		
		logger.info("activateApplicationLink:" +activateApplicationLink);
		
		Mail mail = new Mail();

		ApplicationInfo appInfoRemoteApp = ApplicationInfoList.getRepositoryInfoById(applicationId);
		String appCaption = appInfoRemoteApp.getAppCaption();
		
		if(appCaption == null) appCaption = appInfoRemoteApp.getAppId();
		
		String messageText = CCConstants.getSendActivationRequestMailText(username, receivermail, appCaption, homerepository
				.getAppCaption(), activateApplicationLink);

		mail.sendMail(receivermail, "Antrag auf Freischaltung", messageText);

		logger.info("... return");
	}
	
	@Override
	public String createUsage(String parentId, Map<String, Object> properties) {
		String usageId = this.createNode(parentId, CCConstants.CCM_TYPE_USAGE, CCConstants.CCM_ASSOC_USAGEASPECT_USAGES,(Map)properties);
		return usageId;
	}
	
	@Override
	public void updateUsage(String usageNodeId, Map<String, Object> properties) {
		this.updateNode(usageNodeId, properties);		
	}
	
	@Override
	public void removeUsage(String appId, String courseId, String parentNodeId, String resourceId) throws Exception {
		logger.info("appId:"+appId +" courseId:"+courseId+ " parentNodeId:"+parentNodeId+" resourceId:"+resourceId);
		Map<String, Object> usage = this.getUsage(appId, courseId, parentNodeId, resourceId);
		if(usage != null){
			String parentId = (String)usage.get(CCConstants.CCM_PROP_USAGE_PARENTNODEID);
			String usageId = (String)usage.get(CCConstants.SYS_PROP_NODE_UID);
			logger.info("parentId:"+parentId+" usageId:"+usageId);
			this.removeNode(usageId, parentId);
		}else{
			throw new Exception("no usage found for appId:"+appId +" courseId:"+courseId+ " parentNodeId:"+parentNodeId+" resourceId:"+resourceId);
		}
	}
	
	@Override
	public boolean removeUsages(String appId, String courseId) throws Exception {
		Map<String, Map<String, Object>> usages = this.getUsagesByCourse(appId, courseId);
		
		boolean allDeleted = true;
		
		for(String key: usages.keySet()){
			Map<String,Object> props = usages.get(key);
			String tmpCourseId = (String)props.get(CCConstants.CCM_PROP_USAGE_COURSEID);
			String tmpPrimaryParentId = (String)props.get(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID);
			String tmpAppId =  (String)props.get(CCConstants.CCM_PROP_USAGE_APPID);
			// for security
			if(tmpCourseId.equals(courseId) && tmpAppId.equals(appId)){
				boolean removeNodeSuccess = true;
				try{
					this.removeNode(key, tmpPrimaryParentId);
				}catch(Exception e){
					logger.error(e.getMessage(), e);
					removeNodeSuccess = false;
				}
				allDeleted = (allDeleted && removeNodeSuccess);
			}
		}
		
		return allDeleted;
	}
	
}
