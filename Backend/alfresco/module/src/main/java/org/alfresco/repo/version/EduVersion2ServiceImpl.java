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
package org.alfresco.repo.version;

import java.io.Serializable;
import java.util.*;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.VersionRevertCallback.RevertAspectAction;
import org.alfresco.repo.version.VersionRevertCallback.RevertAssocAction;
import org.alfresco.repo.version.common.VersionUtil;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionServiceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.springframework.extensions.surf.util.ParameterCheck;

/**
 * Version2 Service - implements version2Store (a lighter implementation of the
 * lightWeightVersionStore)
 */
public class EduVersion2ServiceImpl extends org.alfresco.repo.version.Version2ServiceImpl {

	private static Log logger = LogFactory.getLog(Version2ServiceImpl.class);

    private static Collection<String> typesToKeep = Arrays.asList(
            CCConstants.CCM_TYPE_USAGE,
            CCConstants.CCM_TYPE_COMMENT,
            CCConstants.CCM_TYPE_ASSIGNED_LICENSE);
    ActionService actionService;

	
	/**
     * @see org.alfresco.service.cmr.version.VersionService#revert(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.version.Version, boolean)
     */
    public void revert(NodeRef nodeRef, Version version, boolean deep)
    {
       
    	
    	if (logger.isDebugEnabled())
        {
        	logger.debug("Run as user " + AuthenticationUtil.getRunAsUser());
        	logger.debug("Fully authenticated " + AuthenticationUtil.getFullyAuthenticatedUser());
        }
        
    	if(logger.isDebugEnabled())
    	{
    	     logger.debug("revert nodeRef:" + nodeRef);
    	}
    	
        // Check the mandatory parameters
        ParameterCheck.mandatory("nodeRef", nodeRef);
        ParameterCheck.mandatory("version", version);

        // Cross check that the version provided relates to the node reference provided
        if (nodeRef.getId().equals(((NodeRef)version.getVersionProperty(Version2Model.PROP_FROZEN_NODE_REF)).getId()) == false)
        {
            // Error since the version provided does not correspond to the node reference provided
            throw new VersionServiceException(MSGID_ERR_REVERT_MISMATCH);
        }

        QName nodeType = this.nodeService.getType(nodeRef);

        // Turn off any auto-version policy behaviours
        this.policyBehaviourFilter.disableBehaviour(nodeRef, ContentModel.ASPECT_VERSIONABLE);
        try
        {                
            // The current (old) values
            Map<QName, Serializable> oldProps = this.nodeService.getProperties(nodeRef);
            Set<QName> oldAspectQNames = this.nodeService.getAspects(nodeRef);
            QName oldNodeTypeQName = nodeService.getType(nodeRef);
            // Store the current version label
            String currentVersionLabel = (String)this.nodeService.getProperty(nodeRef, ContentModel.PROP_VERSION_LABEL);

            // The frozen (which will become new) values
            // Get the node that represents the frozen state
            NodeRef versionNodeRef = version.getFrozenStateNodeRef();
                boolean needToRestoreDiscussion = !this.nodeService.hasAspect(versionNodeRef, ForumModel.ASPECT_DISCUSSABLE) 
                        && this.nodeService.hasAspect(nodeRef, ForumModel.ASPECT_DISCUSSABLE);
    
                Map<QName, Serializable> forumProps = null;
                
                // Collect forum properties
                // only if previous version hasn't discussable aspect
                if (needToRestoreDiscussion)
                {
                    Map<QName, Serializable> currentVersionProp = this.nodeService.getProperties(nodeRef);
                    forumProps = new HashMap<>();
                    for (QName key : currentVersionProp.keySet())
                    {
                        if (key.getNamespaceURI().equals(NamespaceService.FORUMS_MODEL_1_0_URI))
                        {
                            forumProps.put(key, currentVersionProp.get(key));
                        }
                    }
                }
                    
            Map<QName, Serializable> newProps = this.nodeService.getProperties(versionNodeRef);
            VersionUtil.convertFrozenToOriginalProps(newProps);
            Set<QName> newAspectQNames = this.nodeService.getAspects(versionNodeRef);
            
            // RevertDetails - given to policy behaviours
            VersionRevertDetailsImpl revertDetails = new VersionRevertDetailsImpl();
            revertDetails.setNodeRef(nodeRef);
            revertDetails.setNodeType(oldNodeTypeQName);
            
            //  Do we want to maintain any existing property values?
            Collection<QName> propsToLeaveAlone = new ArrayList<>();
            Collection<QName> assocsToLeaveAlone = new ArrayList<>();
            
            TypeDefinition typeDef = dictionaryService.getType(oldNodeTypeQName);
            if(typeDef != null)
            {
            	for(QName assocName : typeDef.getAssociations().keySet())
            	{
    		    	if(getRevertAssocAction(oldNodeTypeQName, assocName, revertDetails) == RevertAssocAction.IGNORE)
    		    	{
    		            assocsToLeaveAlone.add(assocName);
    		    	}                		
            	}
            }
            
        	for (QName aspect : oldAspectQNames)
        	{
        		AspectDefinition aspectDef = dictionaryService.getAspect(aspect);
        		if(aspectDef != null)
        		{
        		    if (getRevertAspectAction(aspect, revertDetails) == RevertAspectAction.IGNORE)
        		    {
        			     propsToLeaveAlone.addAll(aspectDef.getProperties().keySet());
        			}
        		    for(QName assocName : aspectDef.getAssociations().keySet())
        		    {
        		    	if(getRevertAssocAction(aspect, assocName, revertDetails) == RevertAssocAction.IGNORE)
        		    	{
        		            assocsToLeaveAlone.addAll(aspectDef.getAssociations().keySet());
        		    	}
        		    }
        		}
        	}
        	
        	/**
        	 * edu-sharing: keep PermissionHistory
        	 */
        	newAspectQNames.add(QName.createQName(CCConstants.CCM_ASPECT_PERMISSION_HISTORY));
        	newAspectQNames.add(QName.createQName(CCConstants.CCM_ASPECT_TRACKING));

            /**
             * edu-sharing fix old nodes that do not got mandatory educontext aspect
             */
        	if(QName.createQName(CCConstants.CCM_TYPE_IO).equals(nodeType)
                    || QName.createQName(CCConstants.CCM_TYPE_MAP).equals(nodeType)){
        	    QName aspectEduContext = QName.createQName(CCConstants.CCM_ASPECT_EDUCONTEXT);
        	    if(!oldAspectQNames.contains(aspectEduContext)){
                    newAspectQNames.add(aspectEduContext);
                }
            }

        	propsToLeaveAlone.add(QName.createQName(CCConstants.CCM_PROP_PH_ACTION));
        	propsToLeaveAlone.add(QName.createQName(CCConstants.CCM_PROP_PH_HISTORY));
        	propsToLeaveAlone.add(QName.createQName(CCConstants.CCM_PROP_PH_INVITED));
        	propsToLeaveAlone.add(QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED));
        	propsToLeaveAlone.add(QName.createQName(CCConstants.CCM_PROP_PH_USERS));
        	propsToLeaveAlone.add(QName.createQName(CCConstants.CCM_PROP_TRACKING_VIEWS));
        	propsToLeaveAlone.add(QName.createQName(CCConstants.CCM_PROP_TRACKING_DOWNLOADS));


		    for(QName prop : propsToLeaveAlone)
		    {
			    if(oldProps.containsKey(prop))
			    {
			        newProps.put(prop, oldProps.get(prop));
			        System.out.println("keeping propery:" + prop.getLocalName() + " " + oldProps.get(prop));
			    }
		    }
            
            this.nodeService.setProperties(nodeRef, newProps);
            /**
             * edu-sharing FIX: many properties with null value after revert
             */
            for(Map.Entry<QName,Serializable> entry : newProps.entrySet()){
                if(entry.getValue() == null){
                    this.nodeService.removeProperty(nodeRef,entry.getKey());
                }
            }
                
            //Restore forum properties
            if (needToRestoreDiscussion)
            {
                this.nodeService.addProperties(nodeRef, forumProps);
            }

            Set<QName> aspectsToRemove = new HashSet<QName>(oldAspectQNames);
        	aspectsToRemove.removeAll(newAspectQNames);
        	
        	Set<QName> aspectsToAdd = new HashSet<QName>(newAspectQNames);
        	aspectsToAdd.removeAll(oldAspectQNames);
        	
        	// add aspects that are not on the current node
        	for (QName aspect : aspectsToAdd)
        	{
        		if (getRevertAspectAction(aspect, revertDetails) != RevertAspectAction.IGNORE)
        		{
        	        this.nodeService.addAspect(nodeRef, aspect, null);
        		}
                }
                // Don't remove forum aspects
                if (needToRestoreDiscussion)
                {
                    Set<QName> ignoredAspects = new HashSet<QName>();
                    for (QName aspectForCheck : aspectsToRemove)
                    {
                        if (aspectForCheck.getNamespaceURI().equals(NamespaceService.FORUMS_MODEL_1_0_URI))
                        {
                            ignoredAspects.add(aspectForCheck);
                        }
                    }
                    aspectsToRemove.removeAll(ignoredAspects);
        	}
        	
            // remove aspects that are not on the frozen node
            for (QName aspect : aspectsToRemove)
            {
            	if (getRevertAspectAction(aspect, revertDetails) != RevertAspectAction.IGNORE)
            	{
            		this.nodeService.removeAspect(nodeRef, aspect);
            	}
            }
        	  
            // Re-add the versionable aspect to the reverted node
            if (this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE) == false)
            {
                this.nodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, null);
            }

            // Re-set the version label property (since it should not be modified from the original)
            this.nodeService.setProperty(nodeRef, ContentModel.PROP_VERSION_LABEL, currentVersionLabel);

            // Add/remove the child nodes
            List<ChildAssociationRef> children = new ArrayList<>(this.nodeService.getChildAssocs(nodeRef));
            List<ChildAssociationRef> versionedChildren = this.nodeService.getChildAssocs(versionNodeRef);
            for (ChildAssociationRef versionedChild : versionedChildren)
            {
                if (children.contains(versionedChild) == false)
                {
                    NodeRef childRef = null;
                    ChildAssociationRef assocToKeep = null;
                    if (this.nodeService.exists(versionedChild.getChildRef()) == true)
                    {
                        // The node was a primary child of the parent, but that is no longer the case.  Despite this
                        // the node still exits so this means it has been moved.
                        // The best thing to do in this situation will be to re-add the node as a child, but it will not
                        // be a primary child
                    	String childRefName = (String) this.nodeService.getProperty(versionedChild.getChildRef(), ContentModel.PROP_NAME);
                        childRef = this.nodeService.getChildByName(nodeRef, versionedChild.getTypeQName(), childRefName);
                        // we can faced with association that allow duplicate names
                        if (childRef == null)
                        {
                            List<ChildAssociationRef> allAssocs = nodeService.getParentAssocs(versionedChild.getChildRef(), versionedChild.getTypeQName(), RegexQNamePattern.MATCH_ALL);
                            for (ChildAssociationRef assocToCheck : allAssocs)
                            {
                                if (children.contains(assocToCheck))
                                {
                                    childRef = assocToCheck.getChildRef();
                                    assocToKeep = assocToCheck;
                                    break;
                                }
                            }
                        }
                        if (childRef == null )
                        {
                            childRef = this.nodeService.addChild(nodeRef, versionedChild.getChildRef(), versionedChild.getTypeQName(), versionedChild.getQName()).getChildRef();
                         }
                    }
                    else
                    {
                        if (versionedChild.isPrimary() == true)
                        {
                            // Only try to restore missing children if we are doing a deep revert
                            // Look and see if we have a version history for the child node
                            if (deep == true && getVersionHistoryNodeRef(versionedChild.getChildRef()) != null)
                            {
                                // We're going to try and restore the missing child node and recreate the assoc
                                childRef = restore(
                                   versionedChild.getChildRef(),
                                   nodeRef,
                                   versionedChild.getTypeQName(),
                                   versionedChild.getQName());
                            }
                            // else the deleted child did not have a version history so we can't restore the child
                            // and so we can't revert the association
                        }
                        
                        // else
                        // Since this was never a primary assoc and the child has been deleted we won't recreate
                        // the missing node as it was never owned by the node and we wouldn't know where to put it.
                    }
                    if (childRef != null)
                    {
                        if (assocToKeep != null)
                        {
                            children.remove(assocToKeep);
                        }
                        else
                        {
                            children.remove(nodeService.getPrimaryParent(childRef));
                        }
                    }
                }
                else
                {
                    children.remove(versionedChild);
                } 
            }
                // Don't remove forum children
                if (needToRestoreDiscussion)
                {
                    List<ChildAssociationRef> ignoredChildren = new ArrayList<>();
                    for (ChildAssociationRef childForCheck : children)
                    {
                        if (childForCheck.getTypeQName().getNamespaceURI().equals(NamespaceService.FORUMS_MODEL_1_0_URI))
                        {
                            ignoredChildren.add(childForCheck);
                        }
                    }
                    children.removeAll(ignoredChildren);
                }
            for (ChildAssociationRef ref : children)
            {
            	if (!assocsToLeaveAlone.contains(ref.getTypeQName()))
            	{
                    //this.nodeService.removeChild(nodeRef, ref.getChildRef());


                    /**
                	 * edu-sharing FIX don't remove edu-sharing children
                	 */
                	if(this.nodeService.exists(ref.getChildRef())
                            && !typesToKeep.contains(this.nodeService.getType(ref.getChildRef()).toString())
                            && !this.nodeService.hasAspect(ref.getChildRef(),QName.createQName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT))){ // childobjects in >= 4.2
                		this.nodeService.removeChild(nodeRef, ref.getChildRef());
                	}
            	}
            }
            
            // Add/remove the target associations
            for (AssociationRef assocRef : this.nodeService.getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL))
            {
            	if (!assocsToLeaveAlone.contains(assocRef.getTypeQName()))
            	{
            		this.nodeService.removeAssociation(assocRef.getSourceRef(), assocRef.getTargetRef(), assocRef.getTypeQName());
            	}
            }
            for (AssociationRef versionedAssoc : this.nodeService.getTargetAssocs(versionNodeRef, RegexQNamePattern.MATCH_ALL))
            {
            	if (!assocsToLeaveAlone.contains(versionedAssoc.getTypeQName()))
            	{

                    if (this.nodeService.exists(versionedAssoc.getTargetRef()) == true)
                    {
                        this.nodeService.createAssociation(nodeRef, versionedAssoc.getTargetRef(), versionedAssoc.getTypeQName());
                    }
            	}
                
                // else
                // Since the target of the assoc no longer exists we can't recreate the assoc
            }
        }
        finally
        {
            // Turn auto-version policies back on
            this.policyBehaviourFilter.enableBehaviour(nodeRef, ContentModel.ASPECT_VERSIONABLE);
        }
        
        invokeAfterVersionRevert(nodeRef, version);
        
        /**
         * edu-sharing FIX
		 * when we revert, the alfresco version label does not change to reverted version.  
		 * 
		 * i.e. when we revert from version 1.3 to version 1.0 the content and the properties get reverted to 1.0
		 * BUT the versionLabel remains on 1.3
		 * 
		 * see https://forums.alfresco.com/en/viewtopic.php?t=1390
		 * 
		 * This lead to problems when a node is reverted and is than published into an LMS. 
		 * Because this causes an usage entry to be created and the current version label to be remembered there.
		 * So user would get another version rendered than he has choosen.
		 * 
		 * to prevent this we remember the reverted version in LOM Version property
		 * 
		 * 
		 * also we recreate the preview again
		 */
		if(nodeType.toString().equals(CCConstants.CCM_TYPE_IO)){
			
			this.nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION), version.getVersionLabel());
			
			
			Action action = actionService.createAction("create-thumbnail");
            action.setExecuteAsynchronously(true);
			action.setParameterValue("thumbnail-name", CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
            actionService.executeAction(action, nodeRef);
            new RepositoryCache().remove(nodeRef.getId());
		}
            
        
    }


	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}
}
