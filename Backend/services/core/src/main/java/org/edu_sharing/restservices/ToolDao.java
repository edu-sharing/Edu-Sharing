package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.edu_sharing.service.permission.HandleMode;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.NameSpaceTool;
import org.edu_sharing.restservices.node.v1.NodeApi;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceFactory;

public class ToolDao {
	
	

	public NodeDao createToolDefinition(String repository, Boolean renameIfExists, String versionComment,
			HashMap<String, String[]> properties) throws DAOException  {
		return create(repository, getToolFolder(repository), renameIfExists, versionComment, properties, CCConstants.CCM_TYPE_IO,
				CCConstants.CCM_ASPECT_TOOL_DEFINITION, null);
	}
	
	private String getToolFolder(String repositoryId) throws DAOException {
		
		RepositoryDao repoDao = RepositoryDao.getRepository(repositoryId);
		NodeService nodeService = NodeServiceFactory.getNodeService(repoDao.getApplicationInfo().getAppId());
		String companyHomeId = nodeService.getCompanyHome();
		String toolHomeFolder = nodeService.findNodeByName(companyHomeId, CCConstants.TOOL_HOMEFOLDER);
		if(toolHomeFolder==null){
			
			
			RunAsWork<String> runAsWork = new RunAsWork<String>() {
				@Override
				public String doWork() throws Exception {
					// TODO Auto-generated method stub
					try {
						NodeService nodeService = NodeServiceFactory.getNodeService(repoDao.getApplicationInfo().getAppId());
						String nodeId = nodeService.createNode(companyHomeId, CCConstants.CCM_TYPE_MAP, nodeService.getNameProperty( CCConstants.TOOL_HOMEFOLDER));
						PermissionServiceFactory.getPermissionService(null).setPermissions(nodeId, new ArrayList<>(), false, null,null, false);
						return nodeId;
		}catch(Throwable e) {
			throw new Exception(e);
		}
	}
};
			
			toolHomeFolder =  AuthenticationUtil.runAsSystem(runAsWork);
		}
		return toolHomeFolder;
	}
	
	public NodeDao createToolInstance(String repository, String toolDefinition, Boolean renameIfExists, String versionComment, HashMap<String, String[]> properties) throws DAOException {
		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		NodeDao nodeDao = NodeDao.getNode(repoDao, toolDefinition);

		List<String> aspects = nodeDao.asNode().getAspects();
		if (aspects == null
				|| !aspects.contains(NameSpaceTool.transformToShortQName(CCConstants.CCM_ASPECT_TOOL_DEFINITION))) {
			throw new DAOException(new Exception("parent must have a tool_definition aspect"),toolDefinition);
		}
		
		NodeDao child = new ToolDao().create(repository, toolDefinition, renameIfExists, versionComment, properties,
				CCConstants.CCM_TYPE_TOOL_INSTANCE, null, CCConstants.getValidLocalName(CCConstants.CCM_ASSOC_TOOL_INSTANCES));
		
		return child;
	}
	
	public NodeDao create(String repository, String node, Boolean renameIfExists, String versionComment,
			HashMap<String, String[]> properties, String type, String aspect, String childAssoc) throws DAOException {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			if ("-userhome-".equals(node)) {
				node = repoDao.getUserHome();
			}
			if ("-inbox-".equals(node)) {
				node = repoDao.getUserInbox(true);
			}
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			new NodeApi().resolveURLTitle(properties);

			List<String> aspects = new ArrayList<>();
			if (aspect != null && !aspect.trim().equals("")) {
				aspects.add(aspect);
			}

			NodeDao child = nodeDao.createChild(type, aspects, properties,
					renameIfExists == null ? false : renameIfExists.booleanValue(), childAssoc);

			if (versionComment != null && !versionComment.isEmpty()) {
				child.createVersion(versionComment);
			}

			return child;
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}

	}
	
	public List<Node> getInstances(String repository, String toolDefinition) throws DAOException{
		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		NodeDao nodeDao = NodeDao.getNode(repoDao, toolDefinition);

		List<String> aspects = nodeDao.asNode().getAspects();
		if (aspects == null
				|| !aspects.contains(NameSpaceTool.transformToShortQName(CCConstants.CCM_ASPECT_TOOL_DEFINITION))) {
			throw new DAOException(new Exception("parent must have a tool_definition aspect"),toolDefinition) ;
		}

		List<NodeRef> childRefs = nodeDao.getChildren();
		List<Node> result = new ArrayList<>();
		for (NodeRef nodeRef : childRefs) {
			Node node = NodeDao.getNode(repoDao, nodeRef.getId()).asNode();
			if (node.getType().equals(NameSpaceTool.transformToShortQName(CCConstants.CCM_TYPE_TOOL_INSTANCE))) {
				result.add(node);
			}

		}
		
		return result;
	}
	
	public List<Node> getAllToolDefinitions(String repositoryId) throws DAOException{
		
		RepositoryDao repoDao = RepositoryDao.getRepository(repositoryId);
		NodeService nodeService = NodeServiceFactory.getNodeService(repoDao.getApplicationInfo().getAppId());
		
		String toolFolderId = getToolFolder(repositoryId);
		List<ChildAssociationRef> children = nodeService.getChildrenChildAssociationRef(toolFolderId);
		
		List<Node> result = new ArrayList<>();
		
		for(ChildAssociationRef child : children) {
			String[] aspects = nodeService.getAspects(child.getChildRef().getStoreRef().getProtocol(), child.getChildRef().getStoreRef().getIdentifier(), child.getChildRef().getId());
			if(Arrays.asList(aspects).contains(CCConstants.CCM_ASPECT_TOOL_DEFINITION)) {
				result.add(NodeDao.getNode(repoDao, child.getChildRef().getId()).asNode());
			}
		}
		
		return result;
		
	}
	
	public Node getInstance(String repository, String toolObject) throws DAOException{
		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		NodeDao nodeDao = NodeDao.getNode(repoDao, toolObject);

		Node node = nodeDao.asNode();
		List<String> aspects = node.getAspects();
		if (aspects == null
				|| !aspects.contains(NameSpaceTool.transformToShortQName(CCConstants.CCM_ASPECT_TOOL_OBJECT))) {
			throw new DAOException(new Exception("toolObject must have a tool_object aspect"),toolObject) ;
		}
		
		NodeService nodeService = NodeServiceFactory.getNodeService(repository);
		//nodeService.get
		String[] instanceRef = node.getProperties().get(CCConstants.CCM_PROP_TOOL_OBJECT_TOOLINSTANCEREF);
		return null;
	}

}
