package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.admin.model.GlobalGroup;
import org.edu_sharing.service.organization.GroupSignupMethod;

import java.util.List;

;

@Schema(description = "")
public class Group extends Authority {

	private GroupSignupMethod signupMethod;
	private String groupName = null;
	private GroupProfile profile = null;
	private NodeRef ref = null;

	private List<String> aspects;

	private List<Organization> organizations;

	public Group(){
	}

	public Group(ACE ace){
		super(ace);
		profile = new GroupProfile(ace.getGroup());
		setEditable(ace.getGroup().isEditable());
	}

	public static Group getEveryone(){
		Group group=new Group();
		group.setAuthorityName(CCConstants.AUTHORITY_GROUP_EVERYONE);
		group.setAuthorityType(Type.EVERYONE);
		return group;
	}
	public Group(GlobalGroup group) {
		groupName=group.getName();
		setAuthorityType(Type.GROUP);
		setGroupName(group.getName());

    	GroupProfile profile = new GroupProfile();
    	profile.setDisplayName(group.getDisplayName());
    	profile.setGroupType(group.getGroupType());
    	setProfile(profile);
    	
	}

	@Schema(description = "")
	@JsonProperty("groupName")
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	/**
	 **/
	@Schema(description = "")
	@JsonProperty("profile")
	public GroupProfile getProfile() {
		return profile;
	}

	public void setProfile(GroupProfile profile) {
		this.profile = profile;
	}

	@JsonProperty
	public NodeRef getRef() {
		return ref;
	}
	public void setRef(NodeRef ref) {
		this.ref = ref;
	}


    public void setAspects(List<String> aspects) {
        this.aspects = aspects;
    }

    public List<String> getAspects() {
        return aspects;
    }
	@JsonProperty
	public List<Organization> getOrganizations() {
		return organizations;
	}

	public void setOrganizations(List<Organization> organizations) {
		this.organizations = organizations;
	}

	@JsonProperty
	public GroupSignupMethod getSignupMethod() {
		return signupMethod;
	}

	public void setSignupMethod(GroupSignupMethod signupMethod) {
		this.signupMethod = signupMethod;
	}

}
