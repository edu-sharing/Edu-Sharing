package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class User extends UserSimple {

	@JsonProperty(required = true)
	private NodeRef homeFolder = null;
	private List<NodeRef> sharedFolders = new ArrayList<>();
	private UserQuota quota;

	public User(){super();}
	public User(org.edu_sharing.repository.client.rpc.User user) {
		super(user);
		homeFolder=new NodeRef(user.getRepositoryId(),user.getNodeId());
	}
}
