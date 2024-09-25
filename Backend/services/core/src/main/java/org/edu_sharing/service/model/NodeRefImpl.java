package org.edu_sharing.service.model;

import java.util.*;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.Contributor;

public class NodeRefImpl implements NodeRef {

	String repositoryId;

	List<CollectionRef> usedInCollections = new ArrayList<>();

	Map<Relation, NodeRef> relations = new HashMap<>();

	public static class PreviewImpl implements Preview {
		private final String type;
		private final Boolean icon;
		String mimetype;
		byte[] data;

		public PreviewImpl(String mimetype, byte[] data, String type, Boolean icon) {
			this.mimetype = mimetype;
			this.data = data;
			this.type = type;
			this.icon = icon;
		}

		@Override
		public String getMimetype() {
			return mimetype;
		}

		public void setMimetype(String mimetype) {
			this.mimetype = mimetype;
		}

		@Override
		public byte[] getData() {
			return data;
		}

		public void setData(byte[] data) {
			this.data = data;
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public Boolean getIcon() {
			return icon;
		}
	}

	String storeProtocol;
	
	String storeId;
	
	String nodeId;

	Preview preview;
	Map<String, Object> properties;
	Map<String, Boolean> permissions;

	/**
	 * is this node ref publicly accessible, e.g. shared with "GROUP_EVERYONE"
	 */
	Boolean isPublic;
	private List<String> aspects;

	String owner;

	List<Contributor> contributors;

	public NodeRefImpl(){
		
	}
	public NodeRefImpl(String nodeId){
		this.nodeId = nodeId;
	}
	public NodeRefImpl(String repositoryId, String storeProtocol, String storeId, String nodeId){
		this.repositoryId = repositoryId;
		this.storeId = storeId;
		this.storeProtocol = storeProtocol;
		this.nodeId = nodeId;
	}
	public NodeRefImpl(String repositoryId, String storeProtocol, String storeId, Map<String, Object> properties ){
		this.repositoryId = repositoryId;
		this.storeId = storeId;
		this.storeProtocol = storeProtocol;
		this.nodeId = (String)properties.get(CCConstants.SYS_PROP_NODE_UID);
		this.properties = properties;
	}

	@Override
	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	@Override
	public String getStoreId() {
		return storeId;
	}

	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}

	@Override
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}
	
	@Override
	public String getStoreProtocol() {
		return this.storeProtocol;
	}
	
	public void setStoreProtocol(String storeProtocol) {
		this.storeProtocol = storeProtocol;
	}

	@Override
	public Map<String, Boolean> getPermissions() {
		return permissions;
	}

	@Override
	public void setPermissions(Map<String, Boolean> permissions) {
		this.permissions = permissions;
	}

	public Boolean getPublic() {
		return isPublic;
	}

	public void setPublic(Boolean aPublic) {
		isPublic = aPublic;
	}

	@Override
	public void setAspects(List<String> aspects) {
		this.aspects = aspects;
	}

	@Override
	public List<String> getAspects() {
		return aspects;
	}

	@Override
	public void setUsedInCollections(List<CollectionRef> usedInCollections) {
		this.usedInCollections = usedInCollections;
	}

	@Override
	public List<CollectionRef> getUsedInCollections() {
		return usedInCollections;
	}

	@Override
	public void setPreview(Preview preview) {
		this.preview = preview;
	}

	@Override
	public Preview getPreview() {
		return preview;
	}

	@Override
	public String getOwner() { return owner;}

	@Override
	public void setOwner(String owner) { this.owner = owner; }

	public enum Relation {
		Original
	}

	@Override
	public Map<Relation, NodeRef> getRelations() {
		return relations;
	}

	@Override
	public void setRelations(Map<Relation, NodeRef> relations) {
		this.relations = relations;
	}

	@Override
	public void setContributors(List<Contributor> contributors) {
		this.contributors = contributors;
	}

	@Override
	public List<Contributor> getContributors() {
		return contributors;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NodeRefImpl nodeRef = (NodeRefImpl) o;
		return Objects.equals(storeProtocol, nodeRef.storeProtocol) && Objects.equals(storeId, nodeRef.storeId) && Objects.equals(nodeId, nodeRef.nodeId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(storeProtocol, storeId, nodeId);
	}
}
