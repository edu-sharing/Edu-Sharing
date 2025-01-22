package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ACL {

	@JsonProperty(required = true)
	private boolean inherited = false;

	@JsonProperty(required = true)
	private List<ACE> permissions = null;
	
	public ACL(){}
	public ACL(org.edu_sharing.repository.client.rpc.ACL acl) {
		inherited=acl.isInherited();
		permissions=new ArrayList<>();
		if(acl.getAces()==null){
			return;
		}
		
		for(org.edu_sharing.repository.client.rpc.ACE ace : acl.getAces()){
			boolean added=false;
			for(ACE check : permissions){
				if(check.getAuthority().getAuthorityName().equals(ace.getAuthority())){
					check.getPermissions().add(ace.getPermission());
					added=true;
					break;
				}
			}
			if(added)
				continue;
			
			permissions.add(new ACE(ace));
		}
	}

}
