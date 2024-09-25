package org.edu_sharing.alfresco.fixes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class VirtualEduGroupFolderTool {

	Logger logger = Logger.getLogger(VirtualEduGroupFolderTool.class);

	ServiceRegistry serviceRegistry;
	NodeService nodeService;

	public VirtualEduGroupFolderTool(ServiceRegistry serviceRegistry, NodeService nodeService) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = nodeService;
	}

	public List<ChildAssociationRef> getGroupMapChildren(NodeRef nodeRef) {
		if (QName.createQName(CCConstants.CCM_TYPE_MAP).equals(nodeService.getType(nodeRef))) {
			String mapType = (String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
			if (mapType != null && mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP)) {
				logger.debug("its a map with typ edugroup");
				String user = serviceRegistry.getOwnableService().getOwner(nodeRef);
				AuthorityService authorityService = serviceRegistry.getAuthorityService();
				NodeService nodeService = serviceRegistry.getNodeService();
				Set<String> authorities = authorityService.getContainingAuthorities(AuthorityType.GROUP, user, true);

				List<ChildAssociationRef> children = new ArrayList<>();
				for (String authority : authorities) {

					NodeRef nodeRefAuthority = authorityService.getAuthorityNodeRef(authority);
					if (nodeService.hasAspect(nodeRefAuthority, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) {

						NodeRef eduGroupHomeDir = (NodeRef) nodeService.getProperty(nodeRefAuthority, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
						try {
							String eduGroupHomeDirName = (String) nodeService.getProperty(eduGroupHomeDir, ContentModel.PROP_NAME);
							children.add(new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, nodeRef, QName.createQName(eduGroupHomeDirName), eduGroupHomeDir));
						} catch (InvalidNodeRefException e) {
							logger.warn("eduGroupHomeDir:" + eduGroupHomeDir + " does not longer exist.");
						}catch(IllegalArgumentException e) {
							logger.warn("eduGroupHomeDir:" + eduGroupHomeDir + " " + e.getMessage());
						}catch(AccessDeniedException e) {
							logger.warn("eduGroupHomeDir:" + eduGroupHomeDir + " " + e.getMessage());
						}catch(Throwable e) {
							logger.error(e.getMessage(), e);
						}
					}

				}
				return children;
			}
		}

		return null;
	}

}
