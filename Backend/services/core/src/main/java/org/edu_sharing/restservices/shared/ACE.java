package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.User;

import java.util.ArrayList;
import java.util.List;

@Data
public class ACE  {

	@JsonProperty(required = true)
	private Authority authority = null;

	@JsonProperty("user")
	private UserProfile userProfile = null;

	@JsonProperty("group")
	private GroupProfile groupProfile = null;

	@JsonProperty(required = true)
	private List<String> permissions = null;

	private Long from = null;
	private Long to = null;
	
	boolean isEditable = true;
	
	public ACE(Authority authority,UserProfile user,GroupProfile group) {
		setAuthority(authority);
		setUserProfile(user);
		setGroupProfile(group);
	}
	public ACE(){}
	public ACE(org.edu_sharing.repository.client.rpc.ACE ace) {
		authority=new Authority(ace.getAuthority(),ace.getAuthorityType());
		if(ace.getGroup()!=null)
			groupProfile=new GroupProfile(ace.getGroup());
		if(ace.getUser()!=null)
			userProfile=new UserProfile(ace.getUser());
			
		permissions=new ArrayList<>();
		permissions.add(ace.getPermission());		
	}

	@JsonIgnore
	public void setUser(User user) {
		if(user==null)
			return;
		this.userProfile = new UserProfile();
		this.userProfile.setFirstName(user.getGivenName());
		this.userProfile.setLastName(user.getSurname());
		this.userProfile.setEmail(user.getEmail());
	}
	@JsonIgnore
	public void setGroup(Group group) {
		if(group==null)
			return;
		this.groupProfile=new GroupProfile();
		this.groupProfile.setDisplayName(group.getDisplayName());
	}
}
