package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;
import org.edu_sharing.restservices.UserStatus;

@Schema(description = "")
public class User extends UserSimple {

	
	private NodeRef homeFolder = null;
	private List<NodeRef> sharedFolders = new ArrayList<>();
	private UserQuota quota;

	public User(){super();}
	public User(org.edu_sharing.repository.client.rpc.User user) {
		super(user);
		homeFolder=new NodeRef(user.getRepositoryId(),user.getNodeId());
	}

	@Schema(required = true, description = "")
	@JsonProperty("homeFolder")
	public NodeRef getHomeFolder() {
		return homeFolder;
	}

	public void setHomeFolder(NodeRef homeFolder) {
		this.homeFolder = homeFolder;
	}

	@Schema(description = "")
	@JsonProperty("sharedFolders")
	public List<NodeRef> getSharedFolders() {
		return sharedFolders;
	}

	public void setSharedFolders(List<NodeRef> sharedFolders) {
		this.sharedFolders = sharedFolders;
	}

	@JsonProperty
	public UserQuota getQuota() {
		return quota;
	}

	public void setQuota(UserQuota quota) {
		this.quota = quota;
	}


}
