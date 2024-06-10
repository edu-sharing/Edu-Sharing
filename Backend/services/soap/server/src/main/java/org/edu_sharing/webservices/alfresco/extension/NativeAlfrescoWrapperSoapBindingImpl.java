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

package org.edu_sharing.webservices.alfresco.extension;

import java.io.Serializable;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.HasPermissionsWork;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class NativeAlfrescoWrapperSoapBindingImpl implements org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper {

	StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

	Logger logger = Logger.getLogger(NativeAlfrescoWrapperSoapBindingImpl.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	NodeService nodeService = serviceRegistry.getNodeService();

	SearchService searchService = (SearchService)applicationContext.getBean("scopedSearchService");//serviceRegistry.getSearchService();
	
	public java.util.HashMap getProperties(java.lang.String nodeId) throws java.rmi.RemoteException {
		// authenticate by current thread authinfo
		MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();

		try {
			return mcAlfrescoAPIClient.getProperties(nodeId);
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	@Override
	public HashMap getPropertiesExt(String storeProtocol, String storeId, String nodeId) throws RemoteException {
		try {
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getProperties(storeProtocol, storeId, nodeId);
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}

	public java.lang.String createNode(java.lang.String parentID, java.lang.String nodeTypeString, java.lang.String childAssociation, java.util.HashMap props) throws java.rmi.RemoteException {
		

		Map<QName, Serializable> properties = transformPropMap(props);

		NodeRef parentNodeRef = new NodeRef(storeRef, parentID);
		QName nodeType = QName.createQName(nodeTypeString);

		String assocName = (String) props.get(CCConstants.CM_NAME);
		if (assocName == null)
			assocName = "defaultAssociationName";
		assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;

		ChildAssociationRef childRef = nodeService.createNode(parentNodeRef, QName.createQName(childAssociation), QName.createQName(assocName), nodeType, properties);
		return childRef.getChildRef().getId();
	}
	
	@Override
	public String createNodeAtomicValues(String parentID,
			String nodeTypeString, String childAssociation, HashMap props)
			throws RemoteException {
		Map<QName, Serializable> properties = transformKeysToQName(props);

		NodeRef parentNodeRef = new NodeRef(storeRef, parentID);
		QName nodeType = QName.createQName(nodeTypeString);

		String assocName = (String) props.get(CCConstants.CM_NAME);
		if (assocName == null)
			assocName = "defaultAssociationName";
		assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;

		ChildAssociationRef childRef = nodeService.createNode(parentNodeRef, QName.createQName(childAssociation), QName.createQName(assocName), nodeType, properties);
		return childRef.getChildRef().getId();
	}

	public void updateNode(java.lang.String nodeId, java.util.HashMap properties) throws java.rmi.RemoteException {
		try {
			ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
			ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
			NodeService nodeService = serviceRegistry.getNodeService();

			Map<QName, Serializable> props = transformPropMap(properties);
			NodeRef nodeRef = new NodeRef(storeRef, nodeId);

			// don't do this cause it overwrites all Properties even the content
			// nodeService.setProperties(new NodeRef(store, nodeId), props);
			for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
				nodeService.setProperty(nodeRef, entry.getKey(), entry.getValue());
			}
		} catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage());
		}
	}
	
	@Override
	public void updateNodeAtomicValues(String nodeId, HashMap properties)
			throws RemoteException {
		try {
			ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
			ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
			NodeService nodeService = serviceRegistry.getNodeService();

			Map<QName, Serializable> props = transformKeysToQName(properties);
			NodeRef nodeRef = new NodeRef(storeRef, nodeId);

			// don't do this cause it overwrites all Properties even the content
			// nodeService.setProperties(new NodeRef(store, nodeId), props);
			for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
				nodeService.setProperty(nodeRef, entry.getKey(), entry.getValue());
			}
		} catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage());
		}
		
	}

	public java.util.HashMap getPropertiesSimple(java.lang.String nodeId) throws java.rmi.RemoteException {
		// authenticate by current thread auth info
		MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
		try {
			return mcAlfrescoAPIClient.getPropertiesSimple(nodeId);
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}

	public HashMap getChildren(String parentID, String type) throws RemoteException {
		// authenticate by current thread authinfo
		MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
		try {
			return mcAlfrescoAPIClient.getChildren(parentID, type);
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	public String getCompanyHomeNodeId(){
		MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
		return mcAlfrescoAPIClient.getCompanyHomeNodeId();
	}
	
	private Object getValue(String property, String _value) {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		DictionaryService dictionaryService = serviceRegistry.getDictionaryService();
		QName qnameProp = QName.createQName(property);
		PropertyDefinition propDef = dictionaryService.getProperty(qnameProp);
		DataTypeDefinition dataType = null;
		if (propDef == null) {
			logger.info("found no Dictionary Property Definition for Prop:" + property + " value:" + _value);
		} else {
			dataType = propDef.getDataType();
		}

		if (dataType == null) {
			return _value;
		}

		if (dataType.getName().isMatch(DataTypeDefinition.DATE) || dataType.getName().isMatch(DataTypeDefinition.DATETIME)) {

			try {
				return DateFormat.getDateInstance(DateFormat.LONG, Locale.GERMANY).parse(_value);
			} catch (ParseException e) {
				logger.error(property + " value:" + _value + "was no Date");
				return null;
			}
		}
		if (dataType.getName().isMatch(DataTypeDefinition.INT)) {
			try {
				return Integer.parseInt(_value);
			} catch (NumberFormatException e) {
				logger.error(property + " value:" + _value + "was no Integer");
				return null;
			}
		}
		if (dataType.getName().isMatch(DataTypeDefinition.DOUBLE) || dataType.getName().isMatch(DataTypeDefinition.FLOAT)) {
			try {
				return Double.parseDouble(_value);
			} catch (NumberFormatException e) {
				logger.error(property + " value:" + _value + "was no Double");
				return null;
			}
		}
		return _value;
	}
	
	Map<QName, Serializable> transformKeysToQName(HashMap map){
		try {
			Map<QName, Serializable> result = new HashMap<>();
			for (Object key : map.keySet()) {
				result.put(QName.createQName((String) key), (Serializable) map.get(key));
			}
			return result;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	Map<QName, Serializable> transformPropMap(HashMap map) {
		try {
			Map<QName, Serializable> result = new HashMap<>();
			for (Object key : map.keySet()) {
				String value = (String) map.get(key);
				// test if its xml
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder documentBuilder = factory.newDocumentBuilder();
				logger.info("key:" + key + " value:" + value);
				Document doc = documentBuilder.parse(new InputSource(new StringReader(value)));

				org.w3c.dom.Node firstChild = doc.getFirstChild();
				if (firstChild.getNodeName().equals("value")) {
					result.put(QName.createQName((String) key), (Serializable) getValue((String) key, getTextContent(firstChild)));
				} else if (firstChild.getNodeName().equals("list")) {
					result.put(QName.createQName((String) key), (Serializable) getList((String) key, firstChild));
				} else if (firstChild.getNodeName().equals("i18n")) {
					result.put(QName.createQName((String) key), getMLText(firstChild));
				}
			}
			return result;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	private MLText getMLText(org.w3c.dom.Node i18Node) {
		logger.info("A MLTEXT" + i18Node.getNodeName());
		MLText mlText = new MLText();

		XPathFactory pfactory = XPathFactory.newInstance();
		XPath xpath = pfactory.newXPath();
		try {
			
			NodeList nodeList = (NodeList) xpath.evaluate("entry", i18Node, XPathConstants.NODESET);
			logger.info("  MLTEXT nodeList size:" + nodeList.getLength());
			for (int idx = 0; idx < nodeList.getLength(); idx++) {
				org.w3c.dom.Node entryNode = nodeList.item(idx);
				String locale = getTextContent((org.w3c.dom.Node) xpath.evaluate("locale", entryNode, XPathConstants.NODE));
				String value = getTextContent((org.w3c.dom.Node) xpath.evaluate("value", entryNode, XPathConstants.NODE));
				logger.info("  MLTEXT locale:" + locale + " value:" + value);
				mlText.addValue(new Locale(locale), value);
			}
			return mlText;
			
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	private List getList(String property, org.w3c.dom.Node listNode) {
		List result = new ArrayList();
		for (int i = 0; i < listNode.getChildNodes().getLength(); i++) {
			org.w3c.dom.Node child = listNode.getChildNodes().item(i);
			logger.info("    " + child.getNodeName());
			if (child.getNodeName().equals("i18n")) {
				result.add(getMLText(child));
			} else if (child.getNodeName().equals("value")) {
				result.add(getValue(property, getTextContent(child)));
			}
		}
		return result;
	}

	private String getTextContent(Node node) {
		String result = null;
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node child = nodeList.item(i);

			if (child.getNodeType() == Node.TEXT_NODE) {
				result = child.getTextContent();
			}
		}

		// test if this is OK
		result = (result == null) ? "" : result;
		return result;
	}

	private String[] search(String store, String luceneQuery, String permission) throws Exception {
		StoreRef storeRef = new StoreRef(store);

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	
		PermissionService permissionService = serviceRegistry.getPermissionService();
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore(storeRef);
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.setQuery(luceneQuery);

		ResultSet resultSet = searchService.query(searchParameters);

		logger.info("found " + resultSet.length() + " nodes");
		List<NodeRef> resultNodeRefs = resultSet.getNodeRefs();

		List<String> nodeIds = new ArrayList<>();

		for (NodeRef nodeRef : resultNodeRefs) {
			if (permission != null && !permission.trim().equals("")) {
				AccessStatus accessStatus = permissionService.hasPermission(nodeRef, permission);
				if (accessStatus.compareTo(AccessStatus.ALLOWED) == 0) {
					nodeIds.add(nodeRef.getId());
				}
			} else {
				nodeIds.add(nodeRef.getId());
			}
		}

		String[] result = nodeIds.toArray(new String[nodeIds.size()]);
		return result;
	}

	public String[] searchNodeIds(String store, String luceneQuery, String permission) throws java.rmi.RemoteException {
		try {

			return this.search(store, luceneQuery, permission);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new java.rmi.RemoteException(e.getMessage(), e);
		}
	}

	/**
	 * workspace://SpacesStore
	 * @cm\:name:"test" CheckOut null or some props
	 */
	public RepositoryNode[] searchNodes(String store, String luceneQuery, String permission, String[] propertiesToReturn) throws java.rmi.RemoteException {

		try {

			ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
			ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
			NodeService nodeService = serviceRegistry.getNodeService();

			StoreRef storeRef = new StoreRef(store);
			String[] nodeIds = this.search(store, luceneQuery, permission);

			List<RepositoryNode> searchResults = new ArrayList<>();
			if (nodeIds != null) {
				MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();

				for (String nodeId : nodeIds) {
					NodeRef nodeRef = new NodeRef(storeRef, nodeId);

					RepositoryNode rn = new RepositoryNode();
					rn.setNodeId(nodeId);
					List<KeyValue> resultProps = new ArrayList<>();

					if (propertiesToReturn != null && propertiesToReturn.length > 0) {

						for (String propertyToReturn : propertiesToReturn) {
							Serializable propValue = nodeService.getProperty(nodeRef, QName.createQName(propertyToReturn));
							if (propValue != null) {
								KeyValue keyValue = new KeyValue();
								keyValue.setKey(propertyToReturn);
								keyValue.setValue(propValue.toString());
								resultProps.add(keyValue);
							}
						}
					} else {
						Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
						for (Map.Entry<QName, Serializable> entry : properties.entrySet()) {
							if (entry.getValue() != null) {
								KeyValue keyValue = new KeyValue();
								keyValue.setKey(entry.getKey().toString());
								keyValue.setValue(entry.getValue().toString());
								resultProps.add(keyValue);
							}
						}
					}
					rn.setProperties(resultProps.toArray(new KeyValue[resultProps.size()]));
					searchResults.add(rn);

				}

			}

			return searchResults.toArray(new RepositoryNode[searchResults.size()]);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new java.rmi.RemoteException(e.getMessage(), e);
		}
	}

	public java.util.HashMap hasPermissions(java.lang.String userId, java.lang.String[] permissions, java.lang.String nodeId) throws java.rmi.RemoteException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		OwnableService ownableService = serviceRegistry.getOwnableService();
		PermissionService permissionService = serviceRegistry.getPermissionService();
		
		if (userId.equals(PermissionService.OWNER_AUTHORITY)) {
			userId = ownableService.getOwner(new NodeRef(storeRef, nodeId));
			logger.info(PermissionService.OWNER_AUTHORITY + " mapping on userId:" + userId);
		}

		return AuthenticationUtil.runAs(new HasPermissionsWork(permissionService, userId, permissions, nodeId), userId);
	}
	
	@Override
	public void removeNode(String nodeId, String fromId) throws RemoteException {
		try {
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.removeNode(nodeId, fromId);	
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public boolean isAdmin(String username) throws RemoteException {
		try {
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.isAdmin(username);	
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}

	@Override
	public SearchResult searchSolr(String query, int startIdx, int nrOfresults, String[] facettes,
			int facettesMinCount, int facettesLimit) throws RemoteException {
		try {
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			
			org.edu_sharing.repository.client.rpc.SearchResult nativeResult =  mcAlfrescoAPIClient.searchSolr(query, startIdx, nrOfresults, Arrays.asList(facettes), facettesMinCount, facettesLimit);
			
			if(nativeResult != null){
				SearchResult remoteResult = new SearchResult();
				Map<String, HashMap<>> nodes = nativeResult.getData();
				RepositoryNode[] remoteNodes = new RepositoryNode[nodes.size()];
				int i = 0;
				for(Map<String, Object> localProps : nodes.values() ){
					String nodeId = (String)localProps.get(CCConstants.SYS_PROP_NODE_UID);
					KeyValue[] remoteProps = new KeyValue[localProps.size()];
					int j = 0;
					for(String propKey : localProps.keySet()){
						remoteProps[j] = new KeyValue(propKey,(String)localProps.get(propKey));
						j++;
					} 
					remoteNodes[i] = new RepositoryNode(nodeId,remoteProps);
					i++;
				}
				remoteResult.setData(remoteNodes);
				remoteResult.setStartIDX(startIdx);
				remoteResult.setNodeCount(nativeResult.getNodeCount());
				
				Map<String, Map<String, Integer>> nativeFacettes = nativeResult.getCountedProps();
				
				if(nativeFacettes != null){
				
					List<Facette> remoteFacettes = new ArrayList<>();
					
					for(Map.Entry<String, Map<String, Integer>> entry : nativeFacettes.entrySet()){
						Facette remoteFacette = new Facette();
						remoteFacette.setProperty(entry.getKey());
						FacettePair[] facettePairs = new FacettePair[entry.getValue().size()];
						int k = 0;
						for(Map.Entry<String, Integer> valueCount : entry.getValue().entrySet()){
							FacettePair facettePair = new FacettePair(valueCount.getValue(), valueCount.getKey());
							facettePairs[k] = facettePair;
							k++;
						}
						remoteFacette.setFacettePairs(facettePairs);
						remoteFacettes.add(remoteFacette);
					}
					remoteResult.setFacettes(remoteFacettes.toArray(new Facette[remoteFacettes.size()]));
				}
				return remoteResult;
			}
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	
		return null;
	}
	
	@Override
	public String validateTicket(String ticket) throws RemoteException {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		
		try {
			AuthenticationService authService = serviceRegistry.getAuthenticationService();
			authService.validate(ticket);
			String currentUser = authService.getCurrentUserName();
			
			//for security remove from current thread
			authService.clearCurrentSecurityContext();
			return currentUser;
			
		}catch(AuthenticationException e){
			return null;
		}
	}
	
	public void invalideTicket(String ticket) throws RemoteException {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		
		try {
			AuthenticationService authService = serviceRegistry.getAuthenticationService();
			authService.invalidateTicket(ticket);	
			
			//for security remove from current thread
			authService.clearCurrentSecurityContext();	
	
		}catch(AuthenticationException e){
			logger.error(e.getMessage(), e);
		}
	};
	
	@Override
	public RepositoryNode[] getVersionHistory(String nodeId) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			Map<String,Map<String,Object>> versionHistory = mcAlfrescoAPIClient.getVersionHistory(nodeId);
			
			List<RepositoryNode> result = new ArrayList<>();
			if(versionHistory != null){
				for(Map.Entry<String, Map<String,Object>> entry : versionHistory.entrySet()){
					List<KeyValue> keyValueList = new ArrayList<>();
					for(Object propKey : entry.getValue().keySet()){
						keyValueList.add(new KeyValue((String)propKey, (String)entry.getValue().get(propKey)));
					}
					RepositoryNode repNode = new RepositoryNode(entry.getKey(), keyValueList.toArray(new KeyValue[keyValueList.size()]));
					result.add(repNode);
				}
			}
			return result.toArray(new RepositoryNode[result.size()]);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public String getType(String nodeId) throws RemoteException {
		MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
		return mcAlfrescoAPIClient.getNodeType(nodeId);
	}
	
	@Override
	public void setProperty(String nodeId, String property, String value) throws RemoteException {
		MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
		mcAlfrescoAPIClient.setProperty(nodeId, property, value);
	}
	
	@Override
	public GetPreviewResult getPreviewUrl(String storeProtocol, String storeIdentifier, String nodeId) throws RemoteException {
		MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
		return mcAlfrescoAPIClient.getPreviewUrl(storeProtocol, storeIdentifier, nodeId);
	}
	
	public String getProperty(String storeProtocol, String storeIdentifier, String nodeId, String property){
		MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
		return mcAlfrescoAPIClient.getProperty(storeProtocol, storeIdentifier, nodeId, property);
	}
	
	@Override
	public void copyNode(String nodeId, String toNodeId,  boolean copyChildren) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.copyNode(nodeId, toNodeId, copyChildren);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void createShare(String nodeId, String[] emails, long expiryDate) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.createShare(nodeId, emails, expiryDate);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public Share[] getShares(String nodeId) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getShares(nodeId);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public boolean isOwner(String nodeId, String user) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.isOwner(nodeId, user);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public String[] getMetadataSets() throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getMetadataSets();
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public SearchResult findGroups(String searchWord, String eduGroupNodeId, int from, int nrOfResults) throws RemoteException {
		try {
			
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			Result<List<Group>> apiResult =  permissionService.findGroups(searchWord, true, from, nrOfResults);
			
			SearchResult sr = new SearchResult();
			sr.setStartIDX(apiResult.getStartIDX());
			sr.setNodeCount(apiResult.getNodeCount());
			List<RepositoryNode> repoNodes = new ArrayList<>();
			sr.setData(repoNodes.toArray(new RepositoryNode[repoNodes.size()]));
			for(Group group: apiResult.getData()){
				RepositoryNode repoNode = new RepositoryNode();
				repoNode.setNodeId(group.getNodeId());
				List<KeyValue> props = new ArrayList<>();
				props.add(new KeyValue(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME, group.getDisplayName()));
				props.add(new KeyValue(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME, group.getName()));
				props.add(new KeyValue(CCConstants.SYS_PROP_NODE_UID, group.getNodeId()));
				props.add(new KeyValue(CCConstants.REPOSITORY_ID, group.getRepositoryId()));
				props.add(new KeyValue(CCConstants.PERM_AUTHORITYTYPE_KEY, group.getAuthorityType()));
				repoNode.setProperties(props.toArray(new KeyValue[props.size()]));
				repoNodes.add(repoNode);
			}
			return sr;
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public SearchResult findUsers(KeyValue[] searchProps, String eduGroupNodeId, int from, int nrOfResults) throws RemoteException {

		// mapping to new findUsers method signature
		Map<String, Double> searchFields = new HashMap<>();
		for(KeyValue kv:searchProps){
			searchFields.put(kv.getKey(), 1.);
		}
		
		try {
			
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			Result<List<User>> apiResult =  permissionService.findUsers(searchProps[0].getValue(),searchFields, true, from, nrOfResults);
			
			SearchResult sr = new SearchResult();
			sr.setStartIDX(apiResult.getStartIDX());
			sr.setNodeCount(apiResult.getNodeCount());
			List<RepositoryNode> repoNodes = new ArrayList<>();
			sr.setData(repoNodes.toArray(new RepositoryNode[repoNodes.size()]));
			for(User user: apiResult.getData()){
				RepositoryNode repoNode = new RepositoryNode();
				repoNode.setNodeId(user.getNodeId());
				List<KeyValue> props = new ArrayList<>();
				props.add(new KeyValue(CCConstants.CM_PROP_PERSON_EMAIL, user.getEmail()));
				props.add(new KeyValue(CCConstants.CM_PROP_PERSON_FIRSTNAME, user.getGivenName()));
				props.add(new KeyValue(CCConstants.CM_PROP_PERSON_LASTNAME, user.getSurname()));
				props.add(new KeyValue(CCConstants.CM_PROP_PERSON_USERNAME, user.getUsername()));
				props.add(new KeyValue(CCConstants.SYS_PROP_NODE_UID, user.getNodeId()));
				props.add(new KeyValue(CCConstants.REPOSITORY_ID, user.getRepositoryId()));
				
				repoNode.setProperties(props.toArray(new KeyValue[props.size()]));
				repoNodes.add(repoNode);
			}
			return sr;
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public KeyValue[] getEduGroupContextOfNode(String nodeId) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			Group group = mcAlfrescoAPIClient.getEduGroupContextOfNode(nodeId);
			List<KeyValue> props = new ArrayList<>();
			if(group != null){
				props.add(new KeyValue(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME, group.getDisplayName()));
				props.add(new KeyValue(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME, group.getName()));
				props.add(new KeyValue(CCConstants.SYS_PROP_NODE_UID, group.getNodeId()));
				props.add(new KeyValue(CCConstants.REPOSITORY_ID, group.getRepositoryId()));
				props.add(new KeyValue(CCConstants.PERM_AUTHORITYTYPE_KEY, group.getAuthorityType()));
			}
			return props.toArray(new KeyValue[props.size()]);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public boolean hasToolPermission(String toolPermission)
			throws RemoteException {
		try {
			
			ToolPermissionService tps = ToolPermissionServiceFactory.getInstance();
			return tps.hasToolPermission(toolPermission);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void setOwner(String nodeId, String username) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.setOwner(nodeId, username);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
		
	}
	
	@Override
	public void setUserDefinedPreview(String nodeId, byte[] content, String fileName) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.setUserDefinedPreview(nodeId, content, fileName);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
		
	}
	
	
	public void removeUserDefinedPreview(String nodeId) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.removeUserDefinedPreview(nodeId);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	};
	
	@Override
	public String guessMimetype(String filename) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.guessMimetype(filename);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public HashMap getChildrenCheckPermissions(String parentID, String[] permissionsOnChild) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getChildren(parentID, permissionsOnChild);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public String[] searchNodeIdsLimit(String luceneString, int limit) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.searchNodeIds(luceneString, limit);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void removeAspect(String nodeId, String aspect) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.removeAspect(nodeId, aspect);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
		
	}
	
	@Override
	public void removeGlobalAspectFromGroup(String groupNodeId) throws RemoteException {
			
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.removeGlobalAspectFromGroup(groupNodeId);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
		
	}
	@Override
	public Notify[] getNotifyList(String nodeId) throws RemoteException {
		try {
			
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			List<Notify> result = permissionService.getNotifyList(nodeId);
			return result.toArray(new Notify[result.size()]);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void setPermissions(String nodeId, ACE[] aces) throws RemoteException {
		try {
			
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			permissionService.setPermissions(nodeId, Arrays.asList(aces));
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void revertVersion(String nodeId, String verLbl)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.revertVersion(nodeId, verLbl);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void createVersion(String nodeId, HashMap properties)
			throws RemoteException {
		try {
					
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.createVersion(nodeId);
		
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
		
	}
	
	@Override
	public boolean hasPermissionsSimple(String nodeId, String[] permissions)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.hasPermissions(nodeId, permissions);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public HashMap hasAllPermissions(String nodeId, String[] permissions)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.hasAllPermissions(nodeId, permissions);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public HashMap hasAllPermissionsExt(String storeProtocol, String storeId, String nodeId, String[] permissions)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.hasAllPermissions(storeProtocol, storeId, nodeId, permissions);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public String getHomeFolderID(String username) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getHomeFolderID(username);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public ACL getPermissions(String nodeId) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getPermissions(nodeId);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void setPermissionsBasic(String nodeId, String _authority,
			String[] permissions, boolean changeInherit, boolean inheritPermission)
			throws RemoteException {
		try {
			
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			Boolean inheritPermissionsAsObject = new Boolean(inheritPermission);
			if (!changeInherit) {inheritPermissionsAsObject = null;}
			permissionService.setPermissions(nodeId, _authority, permissions, inheritPermissionsAsObject);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void removePermissions(String nodeId, String _authority,
			String[] _permissions) throws RemoteException {
		try {
			
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			permissionService.removePermissions(nodeId, _authority, _permissions);
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public void executeAction(String nodeId, String actionName,
			String actionId, HashMap parameters, boolean async)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.executeAction(nodeId, actionName, actionId, parameters, async);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void createAssociation(String fromID, String toID, String association)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.createAssociation(fromID, toID, association);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void createChildAssociation(String from, String to,
			String assocType, String assocName) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.createChildAssociation(from, to, assocType, assocName);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void moveNode(String newParentId, String childAssocType,
			String nodeId) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.moveNode(newParentId, childAssocType, nodeId);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}
	
	@Override
	public void removeAssociation(String fromID, String toID, String association)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.removeAssociation(fromID, toID, association);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
		
	}
	
	@Override
	public void removeChild(String parentID, String childID, String association)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.removeChild(parentID, childID, association);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}		
	}
	
	@Override
	public void removeRelationsForNode(String nodeId, String nodeParentId)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.removeRelationsForNode(nodeId, nodeParentId);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
		
	}
	
	@Override
	public void removeRelations(String parentID) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.removeRelations(parentID);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
		
	}
	
	@Override
	public void addAspect(String nodeId, String aspect) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.addAspect(nodeId, aspect);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
		
	}
	
	@Override
	public String getGroupFolderId() throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getGroupFolderId();
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public String getRepositoryRoot() throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getRepositoryRoot();
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	
	@Override
	public HashMap getAssocNode(String nodeID, String association)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getAssocNode(nodeID, association);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public HashMap getChild(String parentId, String type, String property,
			String value) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getChild(parentId, type, property, value);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public HashMap getChildenByProps(String parentId, String type, HashMap props)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getChilden(parentId, type, props);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public HashMap getChildrenByType(String nodeId, String type)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getChildrenByType(nodeId, type);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public HashMap getChildrenByAssociation(String storeString, String nodeId,
			String association) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getChildrenByAssociation(storeString, nodeId, association);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public HashMap getParents(String nodeID, boolean primary)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getParents(nodeID, primary);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public HashMap getChildRecursive(String parentId, String type, HashMap props)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getChildRecursive(parentId, type, props);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	@Override
	public HashMap getChildrenRecursive(String parentId, String type)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getChildrenRecursive(parentId, type);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public String[] getAssociationNodeIds(String nodeID, String association)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			List<String> list = mcAlfrescoAPIClient.getAssociationNodeIds(nodeID,association);
			return (list != null) ? list.toArray(new String[list.size()]) : null;
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public HashMap getUserInfo(String userName) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			String currentUser = mcAlfrescoAPIClient.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME);
			return    (currentUser.equals(userName) || mcAlfrescoAPIClient.isAdmin())
					? mcAlfrescoAPIClient.getUserInfo(userName)
					: null;
					
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}

	@Override
	public SearchResult search(SearchCriteria[] searchCriteria, String s, int i, int i1, String[] strings) throws RemoteException {
		return null;
	}

	@Override
	public String[] getUserNames() throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return 	  (mcAlfrescoAPIClient.isAdmin()) 
					? mcAlfrescoAPIClient.getUserNames()
					: null;
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}

	@Override
	public boolean isSubOf(String type, String parentType)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.isSubOf(type, parentType);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public String getPath(String nodeID) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getPath(nodeID);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public boolean hasContent(String nodeId, String contentProp) throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.hasContent(nodeId, contentProp);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public void writeContent(String nodeID, byte[] content, String mimetype,
			String encoding, String property) throws RemoteException {
		
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.writeContent(nodeID, content, mimetype, encoding, property);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public void setUserPassword(String userName, String password)
			throws RemoteException {
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.setUserPassword(userName, password);
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}

	@Override
	public UserDetails[] getUserDetails(String[] userNames) 
			throws RemoteException {
		
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			List<UserDetails> result = new ArrayList<>();
			for (String userName : userNames) {
				if (userName == null) {
					continue;
				}
				Map<String, String> userInfo = mcAlfrescoAPIClient.getUserInfo(userName);
				if (userInfo == null) {
					continue;
				}
				UserDetails ud = new UserDetails();
				ud.setUserName(userInfo.get(CCConstants.PROP_USERNAME));
				ud.setFirstName(userInfo.get(CCConstants.PROP_USER_FIRSTNAME));
				ud.setLastName(userInfo.get(CCConstants.PROP_USER_LASTNAME));
				ud.setEmail(userInfo.get(CCConstants.PROP_USER_EMAIL));
				result.add(ud);
			}
			return result.toArray(new UserDetails[0]);
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}

	@Override
	public void setUserDetails(UserDetails[] userDetails) throws RemoteException {
		
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			for (UserDetails userDetail : userDetails) {
				if (userDetail == null) {
					continue;
				}
				Map<String, String> userInfo = new HashMap<>();
				userInfo.put(CCConstants.PROP_USERNAME, userDetail.getUserName());
				userInfo.put(CCConstants.PROP_USER_FIRSTNAME, userDetail.getFirstName());
				userInfo.put(CCConstants.PROP_USER_LASTNAME, userDetail.getLastName());
				userInfo.put(CCConstants.PROP_USER_EMAIL, userDetail.getEmail());
				mcAlfrescoAPIClient.createOrUpdateUser(userInfo);
			}
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}

	@Override
	public void deleteUser(String[] userNames) throws RemoteException {

		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			for (String userName : userNames) {
				if (userName == null) {
					continue;
				}
				mcAlfrescoAPIClient.deleteUser(userName);
			}
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}		
	}

	@Override
	public String[] getGroupNames() throws RemoteException {
		try {
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getGroupNames();
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}

	@Override
	public GroupDetails[] getGroupDetails(String[] groupNames)
			throws RemoteException {
		
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();		
			List<GroupDetails> result = new ArrayList<>();
			for (String groupName : groupNames) {
				if (groupName == null) {
					continue;
				}
				GroupDetails gd = new GroupDetails();
				gd.setGroupName(groupName);
				gd.setDisplayName(mcAlfrescoAPIClient.getGroupDisplayName(groupName));
				gd.setHomeFolderId(mcAlfrescoAPIClient.getEduGroupFolder(groupName));
				gd.setNodeId(mcAlfrescoAPIClient.getGroupNodeId(groupName));
				result.add(gd);
			}
			return result.toArray(new GroupDetails[0]);
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}

	@Override
	public void deleteGroup(String[] groupNames) throws RemoteException {

		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			for (String groupName : groupNames) {
				if (groupName == null) {
					continue;
				}
				mcAlfrescoAPIClient.deleteGroup(groupName);
			}
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}		
		
	}

	@Override
	public String[] getMemberships(String groupName) throws RemoteException {

		try {
			return AuthorityServiceFactory.getLocalService().getMembershipsOfGroup(groupName);
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}

	@Override
	public void addMemberships(String groupName, String[] members)
			throws RemoteException {

		try {
			AuthorityServiceFactory.getLocalService().addMemberships(groupName, members);
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
		
	}

	@Override
	public void removeMemberships(String groupName, String[] members)
			throws RemoteException {

		try {
			AuthorityServiceFactory.getLocalService().removeMemberships(groupName, members);
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
		
	}

	@Override
	public void removeAllMemberships(String[] groupNames)
			throws RemoteException {
		
		try {
			
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			for (String groupName : groupNames) {
				if (groupName == null) {
					continue;
				}
				mcAlfrescoAPIClient.removeAllMemberships(groupName);
			}
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}				
	}

	@Override
	public void addPermissionACEs(String nodeId, ACE[] aces)
			throws RemoteException {
		
		try {
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			permissionService.addPermissions(nodeId, aces);
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}

	@Override
	public void removePermissionACEs(String nodeId, ACE[] aces)
			throws RemoteException {
		
		try {
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			permissionService.removePermissions(nodeId, aces);
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}

	@Override
	public void bindEduGroupFolder(String groupName, String folderId)
			throws RemoteException {
		
		try {
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.bindEduGroupFolder(groupName, folderId);
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}

	@Override
	public String findNodeByPath(String path) throws RemoteException {
		
		try {
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.findNodeByPath(path);
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}

	@Override
	public GroupDetails setGroupDetails(GroupDetails groupDetails)
			throws RemoteException {
		
		try {
			
			String groupName = groupDetails.getGroupName();
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			mcAlfrescoAPIClient.createOrUpdateGroup(
					groupName, 
					groupDetails.getDisplayName());
			
			GroupDetails gd = new GroupDetails();
			gd.setGroupName(groupName);
			gd.setDisplayName(mcAlfrescoAPIClient.getGroupDisplayName(groupName));
			gd.setHomeFolderId(mcAlfrescoAPIClient.getEduGroupFolder(groupName));
			gd.setNodeId(mcAlfrescoAPIClient.getGroupNodeId(groupName));
			
			return gd;
			
		} catch (Throwable e) {
			
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}	
	}
	
	@Override
	public String[] getAspects(String storeProtocol, String storeId, String nodeId) throws RemoteException {
		try {
			MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient();
			return mcAlfrescoAPIClient.getAspects(storeProtocol, storeId, nodeId);
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage(), e);
		}
	}

}
