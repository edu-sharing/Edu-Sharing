package org.edu_sharing.service.util;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

public class AlfrescoDaoHelper {

	public static NodeRef marshall(org.edu_sharing.service.model.NodeRef nodeRef){
		return new NodeRef(new StoreRef(nodeRef.getStoreProtocol(),nodeRef.getStoreId()),nodeRef.getNodeId());
	}
	
	public static List<NodeRef> marshall(List<org.edu_sharing.service.model.NodeRef> list){
		List<NodeRef> nodeRefs = new ArrayList<>();
		for(org.edu_sharing.service.model.NodeRef nr :  list){
			nodeRefs.add(marshall(nr));
		}
		return nodeRefs;
	}
	
	
	public static List<org.edu_sharing.service.model.NodeRef> unmarshall(List<NodeRef> list, String repositoryId){
		List<org.edu_sharing.service.model.NodeRef> nrs = new ArrayList<org.edu_sharing.service.model.NodeRef>();
		for(NodeRef nodeRef : list){
			nrs.add(unmarshall(nodeRef,repositoryId));
		}
		return nrs;
	}
	
	public static org.edu_sharing.service.model.NodeRef unmarshall(NodeRef nodeRef, String repositoryId){
		org.edu_sharing.service.model.NodeRef eduNR = new org.edu_sharing.service.model.NodeRefImpl();
		eduNR.setRepositoryId(repositoryId);
		eduNR.setStoreProtocol(nodeRef.getStoreRef().getProtocol());
		eduNR.setStoreId(nodeRef.getStoreRef().getIdentifier());
		eduNR.setNodeId(nodeRef.getId());
		return eduNR;
	}
}
