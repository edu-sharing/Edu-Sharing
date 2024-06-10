package org.edu_sharing.alfresco.policy;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

public class HomeFolderTool {
	
	private final NodeService nodeService;
	private final PersonService personService;
	private final OwnableService ownableService;
	
	Logger logger = Logger.getLogger(HomeFolderTool.class);

	public HomeFolderTool(ServiceRegistry serviceRegistry) {
		this.nodeService = serviceRegistry.getNodeService();
		this.personService = serviceRegistry.getPersonService();
		this.ownableService = serviceRegistry.getOwnableService();
	}
	
	public void constructPersonFolders(NodeRef personNodeRef){
		
		QName type = nodeService.getType(personNodeRef);
		
		if(ContentModel.TYPE_PERSON.equals(type)){
			String userName = (String)nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);
			if(userName != null){
				logger.debug("creating edu folders in user home for:"+userName);
				NodeRef homefolder = (NodeRef) nodeService.getProperty(personNodeRef, ContentModel.PROP_HOMEFOLDER);
				constructPersonFoldersInUserHome(userName, homefolder);
			}else{
				logger.error("username is null for person ref"+personNodeRef);
			}
		}
	}
	
	
	public void constructPersonFoldersInUserHome(String userName, NodeRef homefolder){
		if(homefolder == null){
			logger.error("can not create edu folders in user home for user:"+userName+" cause no homefolder exists!");
			return;
		}
		
		//this folders can not be deleted cause it will be prevented by BeforeMapDeletePolicy
		//set owner so that user can see his folder(read permission for all authorities will be prevented by uncheck inherit permission see: NodeCustomizationPolicy
		NodeRef docsFolder = createMap(homefolder, CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS, CCConstants.I18n_USERFOLDER_DOCUMENTS,userName);
		if(docsFolder != null){
			ownableService.setOwner(docsFolder, userName);
		}
		NodeRef groupFolder = createMap(homefolder, CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP, CCConstants.I18n_USERFOLDER_GROUPS, userName);
		if(groupFolder != null){
			ownableService.setOwner(groupFolder, userName);
		}
		NodeRef favoriteFolder = createMap(homefolder, CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE, CCConstants.I18n_USERFOLDER_FAVORITES, userName);
		if(favoriteFolder != null){
			ownableService.setOwner(favoriteFolder, userName);
		}
		
		NodeRef imageFolder = createMap(homefolder, CCConstants.CCM_VALUE_MAP_TYPE_IMAGES, CCConstants.I18n_USERFOLDER_IMAGES, userName);
		if(imageFolder != null){
			ownableService.setOwner(imageFolder, userName);
		}
		
		if(HttpContext.getCurrentMetadataSet() != null) {
			nodeService.setProperty(docsFolder, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET), HttpContext.getCurrentMetadataSet());
			nodeService.setProperty(favoriteFolder, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET), HttpContext.getCurrentMetadataSet());
			nodeService.setProperty(imageFolder, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET), HttpContext.getCurrentMetadataSet());
		}
	}
	
	private NodeRef createMap(NodeRef parent, String mapType, String nameI18nKey, String userName){
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocsByPropertyValue(parent, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE), mapType);
		if(childAssocs == null || childAssocs.size() == 0){
			String userDataFolderName = I18nServer.getTranslationDefaultResourcebundle(nameI18nKey);
			Map<QName,Serializable> props = new HashMap<>();
			props.put(ContentModel.PROP_NAME,userDataFolderName);
			MLText mlTitle = new MLText();
			mlTitle.addValue(new Locale("de","DE"),I18nServer.getTranslationDefaultResourcebundle(nameI18nKey, "de_DE"));
			mlTitle.addValue(new Locale("en","US"),I18nServer.getTranslationDefaultResourcebundle(nameI18nKey, "en_US"));
			mlTitle.addValue(new Locale("en","EN"),I18nServer.getTranslationDefaultResourcebundle(nameI18nKey, "en_EN"));
			props.put(ContentModel.PROP_TITLE, mlTitle);
			props.put(ContentModel.PROP_CREATOR, userName);
			props.put(QName.createQName(CCConstants.CCM_PROP_MAP_TYPE),  mapType);
			
			return nodeService.createNode(parent, ContentModel.ASSOC_CONTAINS, QName.createQName(userDataFolderName), QName.createQName(CCConstants.CCM_TYPE_MAP), props).getChildRef();
			
		}else{
			logger.debug("map with maptype:"+mapType+" already exsist in "+parent);
			return null;
		}
	}
	
	
	/**
	 * mount edugroup folders for all persons that are member of this group
	 * the group must have the edugroup aspecst with a valid eduGroupHomeDirRef
	 * 
	 * @param groupNodeRef the noderef to this group
	 */
	public void mountEduGroupFoldersInUserHomes(NodeRef groupNodeRef){
		if(nodeService.hasAspect(groupNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))){
			NodeRef eduGroupHomeDirRef = (NodeRef)nodeService.getProperty(groupNodeRef, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
			
			String groupname =(String) nodeService.getProperty(groupNodeRef, ContentModel.PROP_NAME);
			
			if(eduGroupHomeDirRef == null || !nodeService.exists(eduGroupHomeDirRef)){
				logger.warn("eduGroupHomeDirRef of group "+groupname+" does not exist");
				return;
			}
			
			
			if(eduGroupHomeDirRef != null){
				
				String eduGroupDirName = (String)nodeService.getProperty(eduGroupHomeDirRef, ContentModel.PROP_NAME);
				
				List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(groupNodeRef, new HashSet(Arrays.asList(new QName[]{ContentModel.TYPE_PERSON})));
				logger.debug("mounting groupfolder for group "+groupname+" for "+childAssocRefs.size()+" persons");
				for(ChildAssociationRef personChildRef : childAssocRefs){
					NodeRef personHomeFolderNodeRef = (NodeRef)nodeService.getProperty(personChildRef.getChildRef() , ContentModel.PROP_HOMEFOLDER);
					logger.debug("mounting groupfolder for group "+groupname+" for user:"+nodeService.getProperty(personChildRef.getChildRef(),ContentModel.PROP_USERNAME ));
					List<ChildAssociationRef>  childAssocMapTypes = nodeService.getChildAssocsByPropertyValue(personHomeFolderNodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE),CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP);
					if(childAssocMapTypes != null && childAssocMapTypes.size() == 1){
						ChildAssociationRef childAssocRefGroupFolder = childAssocMapTypes.get(0);
						try{
							nodeService.addChild(childAssocRefGroupFolder.getChildRef(), eduGroupHomeDirRef, ContentModel.ASSOC_CONTAINS, QName.createQName(eduGroupDirName));
						}catch(DuplicateChildNodeNameException e){
							logger.warn("group folder already mounted for person "+personChildRef.getChildRef());
						}
					}else{
						logger.warn("can not mount group Folder for person:"+personChildRef.getChildRef()+" cause of missing or invalid "+CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP+" Folder");
					}
				}
			}
		}
	}
	
	
	public void mountEduGroupFoldersInUserHomes(){
		NodeRef rootNode = nodeService.getRootNode(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"));
		List<ChildAssociationRef> rootChildAssocs = nodeService.getChildAssocs(rootNode);
		for(ChildAssociationRef childAssocRef : rootChildAssocs){
			
			if(childAssocRef.getQName().equals(QName.createQName("{"+NamespaceService.SYSTEM_MODEL_1_0_URI+"}system"))){
				
				List<ChildAssociationRef> systemChildren =  nodeService.getChildAssocs(childAssocRef.getChildRef());
				
				for(ChildAssociationRef sysChild : systemChildren){
					if(sysChild.getQName().equals(QName.createQName("{"+NamespaceService.SYSTEM_MODEL_1_0_URI+"}authorities"))){
						List<ChildAssociationRef> authorities = nodeService.getChildAssocs(sysChild.getChildRef());
						
						for(ChildAssociationRef authorityChild :authorities ){
							logger.info("found authority"+authorityChild.getQName());
							this.mountEduGroupFoldersInUserHomes(authorityChild.getChildRef());
						}
						
						
					}
				}
				
				
			}
		}
	}
	
	
	public void constructEduFoldersInUserHomes(){
		Set<NodeRef> allPeople = personService.getAllPeople();
		for(NodeRef personRef : allPeople){
			logger.debug("creating person folders for:"+nodeService.getProperty(personRef, ContentModel.PROP_USERNAME));
			constructPersonFolders(personRef);
		}
		
	}
	

}
