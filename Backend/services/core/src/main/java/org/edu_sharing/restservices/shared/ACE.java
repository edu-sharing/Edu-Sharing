package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.User;

import java.util.ArrayList;
import java.util.List;

;

@Schema(description = "")
public class ACE  {
	
	private Authority authority = null;
	private UserProfile userProfile = null;
	private GroupProfile groupProfile = null;	
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
	
	@Schema(required = true, description = "")
	@JsonProperty("authority")
	public Authority getAuthority() {
		return authority;
	}
	public void setAuthority(Authority authority) {
		this.authority = authority;
	}
	@Schema(required = false, description = "")
	@JsonProperty("user")
	public UserProfile getUserProfile() {
		return userProfile;
	}
	public void setUserProfile(UserProfile user){
		this.userProfile=user;
	}
	@Schema(required = false, description = "")
	@JsonProperty("group")
	public GroupProfile getGroupProfile() {
		return groupProfile;
	}
	public void setGroupProfile(GroupProfile group){
		this.groupProfile=group;
	}


	@Schema(required = true, description = "")
	@JsonProperty("permissions")
	public List<String> getPermissions() {
		return permissions;
	}
	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;
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
	
	public void setEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}
	
	public boolean isEditable() {
		return isEditable;
	}


	public Long getFrom() {
		return from;
	}

	public void setFrom(Long from) {
		this.from = from;
	}

	public Long getTo() {
		return to;
	}

	public void setTo(Long to) {
		this.to = to;
	}


}
