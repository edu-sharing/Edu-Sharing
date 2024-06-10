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
package org.edu_sharing.repository.server.tools;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

/**
 * Tools for working the edu group aspect on normal alfresco user group.
 * @author Christian
 */
public class EduGroupTool {

	public static final QName	QNAME_EDUGROUP	= QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP );

	public static final String TYPE_HOMEDIR 	= CCConstants.CCM_TYPE_MAP;
	public static final QName  QNAME_HOMEDIR 	= QName.createQName(TYPE_HOMEDIR);
	
	public static final String CCM_PROP_EDUGROUP_EDU_HOMEDIR = "{http://www.campuscontent.de/model/1.0}edu_homedir";
	public static final String CCM_PROP_EDUGROUP_EDU_UNIQUENAME = "{http://www.campuscontent.de/model/1.0}edu_uniquename";
	
	private static final String	NAME_SYSTEM	 		= "{http://www.alfresco.org/model/system/1.0}system";
	private static final String	NAME_AUTHORITIES	= "{http://www.alfresco.org/model/system/1.0}authorities";	
	
	private static final String PROP_AUTHORITYNAME	= "{http://www.alfresco.org/model/user/1.0}authorityName";
				
	private static boolean	doneInit = false;
	private static NodeRef	authorityRootNodeRef = null;
	
	private static long  lastMapRefresh	= 0;
	
	// Map [PROP_AUTHORITYNAME] --> [NODEREF OF EDUGROUP]
	// Important: [NAME OF A GROUP] is unique on an Alfresco node  
	private static Map<String, NodeRef>	cachedGroupNameMap = null;
	
	// Map [NODEREF OF EDUGROUP (String)] --> [PARENT NODEREF OF EDUGROUP (String)]
	private static Map<String, String>	cachedGroupParentMap = null;
	
	// use this object for synchronized code blocks that are working stateful on
	// cachedGroupNameMap OR cachedGroupParentMap
	public static Boolean syncAccessLock =  new Boolean(true); 
		
	/**
	 * Prepare to work with the authorities/groups stored in userstore
	 * @throws Exception
	 */
	private static synchronized void init()throws Exception {
		
		if (doneInit) return;
		
		// get services to work with
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		NodeService nodeService = serviceRegistry.getNodeService();		

		// start in store root and get base node of all authorities
		NodeRef userStoreRoot = nodeService.getRootNode(new StoreRef("user://alfrescoUserStore"));
		NodeRef systemRoot = getNodeRefWithQName(nodeService.getChildAssocs(userStoreRoot), NAME_SYSTEM);
		authorityRootNodeRef = getNodeRefWithQName(nodeService.getChildAssocs(systemRoot), NAME_AUTHORITIES);
		
		doneInit = true;
	}

	/**
	 * Loads a cashed Map of all edugroups from userStore.
	 * Use if a lot requests are performed on Map and its no problem if Map is less than 20 secs old
	 * 
	 * KEY 		-> Groupname
	 * VALUE   	-> NodeRef 
	 */
	private static Map<String,NodeRef> getCashedEduGroupMap() throws Exception {
		
		// check if map needs refresh
		synchronized(syncAccessLock) {
				long actualTime = new Date().getTime();
				if ((cachedGroupNameMap==null) || ((actualTime-(20*1000))>lastMapRefresh)) {
					
					// get services to work with
					ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
					ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
					NodeService nodeService = serviceRegistry.getNodeService();	
					
					cachedGroupNameMap = getEduGroupMap(nodeService);
					cachedGroupParentMap = createCachedGroupParentMap(nodeService, cachedGroupNameMap);
					lastMapRefresh = actualTime;
				}
		}
				
		return cachedGroupNameMap;
	}	
	
	/**
	 * Builds from the group map a parent map.
	 * @param nodeService
	 * @param groupMap
	 * @return
	 * @throws Exception
	 */
	private static Map<String, String> createCachedGroupParentMap(NodeService nodeService, Map <String,NodeRef> groupMap) throws Exception {
		Map<String, String> result = new HashMap<>();
		for (String groupName : groupMap.keySet()) {
			NodeRef nodeRef = groupMap.get(groupName);			
			if (nodeService.getParentAssocs(nodeRef).size()==1) {
				String parentNodeRef = nodeService.getParentAssocs(nodeRef).get(0).getParentRef().toString();
				result.put(nodeRef.toString(), parentNodeRef);
			} else {
				throw new Exception("Node '"+nodeRef.toString()+"' has '"+nodeService.getParentAssocs(nodeRef).size()+"' Parents !! not ==1 !!");
			}
		}
		return result;
	}
	
	/**
	 * Loads a fresh Map of all edugroups from userStore.
	 * KEY 		-> Groupname
	 * VALUE   	-> NodeRef 
	 */
	public static Map<String,NodeRef> getEduGroupMap(final NodeService nodeService) throws Exception {
				
		AuthenticationUtil.RunAsWork<Map<String,NodeRef>> runAs = () -> {
            if (!doneInit) init();
            return getEduGroupMapFromNodeContainer(nodeService, authorityRootNodeRef, 1);
        };
		
		return AuthenticationUtil.runAs(runAs, ApplicationInfoList.getHomeRepository().getUsername());		
	}
	
	/**
	 * Checks if a given group is local, which means that the user is in that group or the group is
	 * a sub group of a group he is in.
	 * @param listOfGroups_NodeRefStr_UserIsIn All the groups the user is in / List of NodeRefs in String format
	 * @param group_NodeRefStr_UserIsIn The group to test in NodeRef String format
	 * @return
	 * @throws Exception 
	 */
	public static boolean isEduGroupLocal(Set<String> listOfGroups_NodeRefStr_UserIsIn, String group_NodeRefStr_UserIsIn) throws Exception {
		
		if (!doneInit) init();
		
		if (cachedGroupParentMap==null) {
			System.err.println("WARNING: isEduGroupLocal() called on EduGroupTools but cachedGroupParentMap is NULL (no init done)");
			return false;
		}
		
		String actualNodeRefStr = group_NodeRefStr_UserIsIn;
		while (actualNodeRefStr!=null) {
			if (listOfGroups_NodeRefStr_UserIsIn.contains(actualNodeRefStr)) return true;
			actualNodeRefStr = cachedGroupParentMap.get(actualNodeRefStr);
		}
		
		return false;
	}
	
	/**
	 * Returns the path of eduGroups the actual eduGroup is a sub group of 
	 * @param group_NodeRefStr_UserIsIn
	 * @return
	 */
	public static String getParentPathForEduGroup(String group_NodeRefStr_UserIsIn) {
		String parentPath = "/";
		String parentNodeRef = cachedGroupParentMap.get(group_NodeRefStr_UserIsIn);
		while (parentNodeRef!=null) {
			// if parent is a eduGroup
			if (cachedGroupNameMap.containsValue(new NodeRef(parentNodeRef))) {
				// get name from reverse lookup of groupNameMap
				String groupName = null;
				synchronized(syncAccessLock) {
					Iterator<String> keys = cachedGroupNameMap.keySet().iterator();
					while ((groupName==null) && (keys.hasNext())) {
						String name = keys.next();
						NodeRef toTest = cachedGroupNameMap.get(name);
						if ((toTest!=null) && (toTest.toString().equals(parentNodeRef))) {
							if (name.startsWith("GROUP_")) name = name.substring(6);
							groupName = name;
						}
					}
				}
				if (groupName!=null) parentPath = parentPath+groupName+"/";
			}
			// get next parent
			parentNodeRef = cachedGroupParentMap.get(parentNodeRef);
		}
		return parentPath;
	}

	/*
	 * Recursive traversal of userstore ...
	 */
	private static Map<String,NodeRef> getEduGroupMapFromNodeContainer(NodeService nodeService, NodeRef node, int recDepth) {
		
		Map<String,NodeRef> results = new HashMap<>();
		
		// to prevent possible infintiv loops in a node graph ... stop at a recursive level of 5 
		if (recDepth>5) return results;
			
		// check current node for a edugroup
		if (nodeService.hasAspect(node, QNAME_EDUGROUP)) {
			String groupName = (String)nodeService.getProperty(node, QName.createQName(PROP_AUTHORITYNAME));
			if (groupName!=null) {
	 			results.put(groupName, node);
			}
		} 
		
		// check children recursive
		List<ChildAssociationRef> childs = nodeService.getChildAssocs(node);
		for (ChildAssociationRef childAssociationRef : childs) {
			results.putAll(getEduGroupMapFromNodeContainer(nodeService, childAssociationRef.getChildRef(), (recDepth+1)));
		}
		
		return results;
	}
	
	public static NodeRef getNodeRefWithQName(List<ChildAssociationRef> childs, String qNameStr) throws Exception {
		
		if ((childs==null) || (childs.size()==0)) throw new Exception("list of childs is empty ... cannot find child with qName '"+qNameStr+"'");
		
		for (ChildAssociationRef childAssociationRef : childs) {
			if (childAssociationRef.getQName().isMatch(QName.createQName(qNameStr))) return childAssociationRef.getChildRef();
		}
		
		throw new Exception("cannot find child with QName '"+qNameStr+"' in userstore");
	}
	
	/**
	 * Returns all NodeRefs (String format) of edu groups a user is assigned to
	 * @param username
	 * @return
	 */
	public static Set<String> getAllEduGroupsOfUserAsNodeRefStrings(String username) throws Exception {
		
		Set<String> preresults =  getAllEduGroupsOfUserAsName(username);
		Set<String> results = new HashSet<String>(preresults.size());
		for (String string : preresults) {
			results.add(getNodeRefFromEduGroupUniqueName(string));
		}
		return results;
	}
	
	/**
	 * Returns all GroupNames (alfresco identifier) of edu groups a user is assigned to
	 * @param username
	 * @return
	 */
	public static Set<String> getAllEduGroupsOfUserAsName(String username) throws Exception {
						
		Set<String> resultList = new HashSet<String>();
		
		// get services to work with
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);	
				
		// all alfresco groups the user is in 
		Set<String> authorityList = null;
		try {
			authorityList = serviceRegistry.getAuthorityService().getContainingAuthorities(AuthorityType.GROUP, username, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (authorityList==null) {
			System.err.println("[ERROR] Not able to get groups for username '"+username+"' ... check if Exception above");			
			return resultList;
		}

		// filter out just the edu groups
		for (String groupname : authorityList) {
			if (getCashedEduGroupMap().containsKey(groupname)) {
				resultList.add(groupname);		
			}
		}
	
		return resultList;
		
	} 
	
	public static String getNodeRefFromEduGroupUniqueName(String groupName) throws Exception {

		if (!doneInit) init();
		NodeRef nodeRef = getCashedEduGroupMap().get(groupName);
		
		if (nodeRef!=null) {
			return nodeRef.toString(); 
		} else {
			return null;		
		}

	}
	
	/**
	 * Returns the username of the actual context 
	 * @return
	 */
	public static String getTheCurrentUsername() {
		
		// get services to work with
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);			
		
		// get the current username
		return serviceRegistry.getAuthenticationService().getCurrentUserName();
	}

	/**
	 * Checks if two sets are containing the same string item (group node ref)
	 * @param one
	 * @param two
	 * @return
	 */
	public static boolean gotAtleastOneSameGroupInSets(Set<String> one, Set<String> two) {
		for (String itemInListOne : one) {
			if (two.contains(itemInListOne)) return true;
		}
		return false;
	}
	
	/**
	 * Processing Mini Command Line for adding and deleting the EduGroup Aspect to or from an Alfresco group
	 * 
	 * Attention: expects that the current thread is authenticated
	 * @param propName
	 * @return (can be ignored)
	 * @throws Exception
	 */
	public static void processEduGroupMicroCommand(String propName) throws Exception {
		
		String syntaxHelp = "Use Syntax: COMMAND [ADD|REMOVE] [GROUPNODEREF] [GROUPHOMEFOLDERNODEREF(just needed on ADD command)]";
		
		// get services to work with
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		NodeService nodeService = serviceRegistry.getNodeService();		
		
		// parse command line
		String commandLine = "";
		if (propName.startsWith("COMMAND ")) {
			commandLine = propName.substring(8);
		}
		
		// parse command ...
		String groupNode = "";
		String homeNode	 = "";
		
		// ADD ASPECT TO GROUP
		if (commandLine.startsWith("ADD")) {
			
			commandLine = commandLine.substring(3).trim();
			
			// get groupnode param
			int spaceIndex = commandLine.indexOf(' ');
			if (spaceIndex<=0) throw new Exception(syntaxHelp);
			groupNode = commandLine.substring(0,spaceIndex).trim();
			commandLine = commandLine.substring(spaceIndex).trim();
			
			//alfresco34E
			//if ((!NodeRef.isNodeRef(groupNode)) || (!groupNode.startsWith("user"))) throw new Exception("Group Node Refrence needs to be like this format 'user://alfrescoUserStore/1c64cabc-97bd-4628-86a8-4860cdc73342' and NOT like '"+groupNode+"'");					

			if ((!NodeRef.isNodeRef(groupNode)) || (!groupNode.startsWith("workspace"))) throw new Exception("Group Node Refrence needs to be like this format 'workspace://SpacesStore/1c64cabc-97bd-4628-86a8-4860cdc73342' and NOT like '"+groupNode+"'");					
			
			if(!nodeService.getType(new NodeRef(groupNode)).equals(QName.createQName(CCConstants.CM_TYPE_AUTHORITY_CONTAINER))){
				throw new Exception(groupNode+" is no Group");					
			}
			
			// get homenode param
			if (commandLine.length()<=0) throw new Exception(syntaxHelp);
			homeNode = commandLine;
			if (!NodeRef.isNodeRef(homeNode)) throw new Exception("Home Node Refrence needs to be like this format 'workspace://SpacesStore/93303a1f-187e-4f49-9fef-7b80e982da2c' and NOT like '"+homeNode+"'");		
			
			if( !(nodeService.getType(new NodeRef(homeNode)).equals(QName.createQName(CCConstants.CM_TYPE_FOLDER)) || nodeService.getType(new NodeRef(homeNode)).equals(QName.createQName(CCConstants.CCM_TYPE_MAP)) )){
				throw new Exception(homeNode+" is no Folder");					
			}
			
			NodeRef groupRef = new NodeRef(groupNode);
			NodeRef homeRef = new NodeRef(homeNode);
			
			String linkName = (String) nodeService.getProperty(
					groupRef
					, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));  

			// typ must be set, because needed for request 
			nodeService.setType(homeRef, QNAME_HOMEDIR);
					
			// add aspect to group
			Map<QName, Serializable> params = new HashMap<>();
			params.put(QName.createQName(CCM_PROP_EDUGROUP_EDU_HOMEDIR), homeRef);
			params.put(QName.createQName(CCM_PROP_EDUGROUP_EDU_UNIQUENAME), linkName);
			nodeService.addAspect(groupRef, QNAME_EDUGROUP, params);
									
		} else if (commandLine.startsWith("REMOVE")) {
			
			commandLine = commandLine.substring(6).trim();
			
			// get groupnode param
			if (commandLine.length()<=0) throw new Exception(syntaxHelp);			
			int spaceIndex = commandLine.indexOf(' ');
			if (spaceIndex<=0) spaceIndex = commandLine.length();
			groupNode = commandLine.substring(0,spaceIndex).trim();
			
			if(!nodeService.getType(new NodeRef(groupNode)).equals(QName.createQName(CCConstants.CM_TYPE_AUTHORITY_CONTAINER))){
				throw new Exception(groupNode+" is no Group");					
			}
			
			// remove aspect
			nodeService.removeAspect(new NodeRef(groupNode), QNAME_EDUGROUP);	
			
		} else {
			throw new Exception(syntaxHelp);
		}
					
	}		
	
}
