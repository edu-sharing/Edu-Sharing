package org.edu_sharing.repository.client.rpc;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class ACE implements java.io.Serializable {

    private String authority = null;
	private String permission = null;
	private String accessStatus = null;
    private String authorityType = null;
    private User user = null;
    private Group group = null;
	private Long from = null;
	private Long to = null;
	int id;
	boolean inherited = false;
	boolean edited = false;
	
	public ACE() {
	}

	public ACE(String permission, String authority) {
		this.permission = permission;
		this.authority = authority;
	}

	@Override
	public int hashCode() {
		final int prime = 3;
	      int result = 1;
	      result = prime * result + ((accessStatus == null) ? 0 : accessStatus.hashCode());
	      result = prime * result + ((authority == null) ? 0 : authority.hashCode());
	      result = prime * result + ((permission == null) ? 0 : permission.hashCode());
	      result = prime * result + Boolean.valueOf(inherited).toString().hashCode();
	      result = prime * result + ((from == null) ? 0 : from.toString().hashCode());
	      result = prime * result + ((to == null) ? 0 : to.toString().hashCode());
	      return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if( !(obj instanceof ACE) ) return false;
		
		ACE ace = (ACE)obj;
		
		if(ace.getAuthority().equals(this.getAuthority()) &&
				ace.getPermission().equals(this.getPermission()) &&
				ace.inherited == this.inherited &&
				Objects.equals(from, ace.from) &&
				Objects.equals(to, ace.to)
		){
			
			return true;
		
		}
		
		return false;
	}
}
