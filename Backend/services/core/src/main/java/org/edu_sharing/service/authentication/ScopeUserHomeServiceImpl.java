package org.edu_sharing.service.authentication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.person.RegexHomeFolderProvider;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.HomeFolderTool;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.springframework.context.ApplicationContext;

public class ScopeUserHomeServiceImpl implements ScopeUserHomeService{

	RegexHomeFolderProvider regexHomeFolderProvider;
	
	ServiceRegistry serviceRegistry;
	
	PersonService personService;
	
	NodeService nodeService;
	
	Repository repositoryHelper;
	
	OwnableService ownableService;
	
	public static String SCOPE_ROOT = "SCOPES";
	
	public static QName CHILD_ASSOC_PERSON_SCOPES = QName.createQName("{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}personScopes");
	
	public static QName TYPE_PERSON_SCOPE = QName.createQName("{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}personScope");
	
	public static QName PROP_PERSON_SCOPE_NAME = QName.createQName("{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}personScopeName");
	
	public static QName PROP_PERSON_SCOPE_HOMEFOLDER = QName.createQName("{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}personScopeHomeFolder");
	
	public static QName PROP_PERSON_SCOPE_ROLE = QName.createQName("{"+NamespaceService.CONTENT_MODEL_1_0_URI+"}personScopeRole");
	
	Logger logger = Logger.getLogger(ScopeUserHomeServiceImpl.class);
	
	boolean manageEduGroupFolders = true;
	
	public ScopeUserHomeServiceImpl() {
		
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		personService = serviceRegistry.getPersonService();
		regexHomeFolderProvider = (RegexHomeFolderProvider)applicationContext.getBean("largeHomeFolderProvider");
		nodeService = serviceRegistry.getNodeService();
		
		ownableService = serviceRegistry.getOwnableService();
		
		repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");
	}
	
	@Override
	public NodeRef getUserHome(String username, String scope,boolean createIfNotExists) {
		NodeRef nodeRefUserHome = null;
		NodeRef personNodeRef = personService.getPerson(username);
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocsByPropertyValue(personNodeRef, PROP_PERSON_SCOPE_NAME, scope);
		
		if((childAssocs == null || childAssocs.size() == 0) && createIfNotExists){
			
			
			RunAsWork<NodeRef> runAs = new RunAsWork<NodeRef>() {
				@Override
				public NodeRef doWork() throws Exception {
					
					List<String> homeFolderPath = regexHomeFolderProvider.getHomeFolderPath(personNodeRef);
					
					NodeRef parentNodeRef = getRootNodeRef(scope);
					
					for(String homeFolderPart : homeFolderPath){
						parentNodeRef = getOrCreateMap(parentNodeRef, homeFolderPart, null);
					}
					
					//set owner to username
					ownableService.setOwner(parentNodeRef, username);
					serviceRegistry.getPermissionService().setInheritParentPermissions(parentNodeRef, false);

					//set ROLE_OWNER authority to ALL like alfresco is doing
					// (watch out for alfresco bean "personServicePermissionsManager"
					//  and class org.alfresco.repo.security.person.PermissionsManagerImpl)
					serviceRegistry.getPermissionService().setPermission(parentNodeRef, PermissionService.OWNER_AUTHORITY, PermissionService.ALL_PERMISSIONS, true);
					serviceRegistry.getPermissionService().setPermission(parentNodeRef, username, PermissionService.ALL_PERMISSIONS, true);
					
					Map<QName,Serializable> personScopeProps = new HashMap<>();
					personScopeProps.put(PROP_PERSON_SCOPE_HOMEFOLDER, parentNodeRef);
					personScopeProps.put(PROP_PERSON_SCOPE_NAME, scope);
					ChildAssociationRef childRef = nodeService.createNode(personNodeRef, 
							CHILD_ASSOC_PERSON_SCOPES, 
							QName.createQName("{" + NamespaceService.CONTENT_MODEL_1_0_URI+"}"+scope), 
							TYPE_PERSON_SCOPE, 
							personScopeProps);
					
					new HomeFolderTool(serviceRegistry).constructPersonFoldersInUserHome(username, parentNodeRef);
					
					return parentNodeRef;
				}
			};
			
			
			nodeRefUserHome = AuthenticationUtil.runAsSystem(runAs);
			
			
		}else if(!(childAssocs == null || childAssocs.size() == 0)){
			NodeRef personScope = childAssocs.iterator().next().getChildRef();
			nodeRefUserHome = (NodeRef)nodeService.getProperty(personScope, PROP_PERSON_SCOPE_HOMEFOLDER);
		}
		
		if(nodeRefUserHome == null){
			logger.error("could not find userhome for user " + username +" and scope " + scope );
		}else{
			
			if(manageEduGroupFolders){
				final NodeRef nodeRefUserHomeFinal=nodeRefUserHome;
				AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

					@Override
					public Void doWork() throws Exception {
						manageEduGroupFolders(username,scope, nodeRefUserHomeFinal);
						return null;
					}
				});
			}
		}
		return nodeRefUserHome;
	}
	
	public NodeRef getRootNodeRef(String scope){
		NodeRef companyHome = repositoryHelper.getCompanyHome();
		
		//clear scope for root node the moment cause of the policy that sets the scope prop
		String currentScope = NodeServiceInterceptor.getEduSharingScope();
		NodeServiceInterceptor.setEduSharingScope(null);
		NodeRef scopeRootNodeRef = getOrCreateMap(companyHome,SCOPE_ROOT,null);
		NodeServiceInterceptor.setEduSharingScope(currentScope);
		
		Map<QName,Serializable> scopeProperties = new HashMap<>();
		scopeProperties.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME), scope);
		scopeProperties.put(ContentModel.PROP_NAME, scope);
		NodeRef scopeNodeRef = 	getOrCreateMap(scopeRootNodeRef,scope,scopeProperties);
		
		return scopeNodeRef;
	}
	
	private NodeRef getOrCreateMap(NodeRef parent, String name, Map<QName,Serializable> properties){
		NodeRef nodeRef = nodeService.getChildByName(parent, ContentModel.ASSOC_CONTAINS, name);
		if(nodeRef == null){
			String assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + name;
			
			if(properties == null){ 
				properties = new HashMap<>();
				properties.put(ContentModel.PROP_NAME, name);
			}
			
			ChildAssociationRef childAssocRef = nodeService.createNode(
					parent, 
					ContentModel.ASSOC_CONTAINS, 
					QName.createQName(assocName), 
					QName.createQName(CCConstants.CCM_TYPE_MAP), 
					properties);
	
			nodeRef = childAssocRef.getChildRef();
		}
		return nodeRef;
	}

	@Override
	public EduGroup getOrCreateScopedEduGroup(String authority, String scope){
		AuthorityService authorityService = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
		EduGroup check = authorityService.getEduGroup(authority + "_" + scope);
		if(check == null) {
			EduGroup eduGroup = authorityService.getEduGroup(authority);
			EduGroup tmEduGroup = new EduGroup();
			tmEduGroup.setGroupname(eduGroup.getGroupname() + "_" + scope);
			tmEduGroup.setGroupDisplayName(eduGroup.getGroupDisplayName() + "_" + scope);
			tmEduGroup.setFolderName(tmEduGroup.getGroupDisplayName().replaceAll("GROUP_",""));
			tmEduGroup.setScope(scope);
			//try to get root folder of edugroups
			ChildAssociationRef primaryParent = nodeService.getPrimaryParent(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,eduGroup.getFolderId()));
			return authorityService.getOrCreateEduGroup(tmEduGroup,eduGroup, primaryParent.getParentRef().getId());
		}
		return check;
	}
	
	public void manageEduGroupFolders(String userName, String scope, NodeRef userHome ){
		
		AuthorityService authorityService = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
		
		
		if(userHome != null){
			List<EduGroup> noneScopedEduGroups = authorityService.getEduGroups(null);
			List<EduGroup> scopedEduGroups = authorityService.getEduGroups();
			
			for(EduGroup eduGroup : noneScopedEduGroups){
				EduGroup tmEduGroup = new EduGroup();
				tmEduGroup.setGroupname(eduGroup.getGroupname() + "_" + scope);
				tmEduGroup.setGroupDisplayName(eduGroup.getGroupDisplayName() + "_" + scope);
				tmEduGroup.setFolderName(tmEduGroup.getGroupDisplayName().replaceAll("GROUP_",""));
				tmEduGroup.setScope(scope);
				if(!scopedEduGroups.contains(tmEduGroup)){
					
					//try to get root folder of edugroups
					ChildAssociationRef primaryParent = nodeService.getPrimaryParent(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,eduGroup.getFolderId()));
					
					EduGroup scopedEduGroup = authorityService.getOrCreateEduGroup(tmEduGroup,eduGroup, primaryParent.getParentRef().getId());
				
					
					RunAsWork<Void> runAs = new RunAsWork<Void>() {
						@Override
						public Void doWork() throws Exception {
							Set<String> members = serviceRegistry.getAuthorityService().getContainedAuthorities(AuthorityType.USER, scopedEduGroup.getGroupname(), false);
							if(!members.contains(userName)){
								serviceRegistry.getAuthorityService().addAuthority(scopedEduGroup.getGroupname(), userName);
							}
							return null;
						}
					};
					AuthenticationUtil.runAs(runAs, ApplicationInfoList.getHomeRepository().getUsername());
					
				}
			}
			
		}
	}
	
	public void setManageEduGroupFolders(boolean manageEduGroupFolders) {
		this.manageEduGroupFolders = manageEduGroupFolders;
	}
}
