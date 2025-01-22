package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.admin.model.GlobalGroup;
import org.edu_sharing.service.organization.GroupSignupMethod;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
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
}
