package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.repository.client.rpc.ACE;

import java.io.Serializable;
import java.util.Map;

;

@Schema(description = "")
public class Authority implements Serializable {

	public static enum Type {USER, GROUP, OWNER, EVERYONE, GUEST;};

	private String authorityName;
	private Type authorityType;
	private Map<String, String[]> properties;
	boolean editable;

	public Authority(){}
	public Authority(ACE ace) {
		authorityName = ace.getAuthority();
		authorityType = Authority.Type.valueOf(ace.getAuthorityType());
		if(ace.getUser() != null){
		    editable = ace.getUser().isEditable();
		}
	}

	public Authority(String authorityName, String authorityType) {
		this.authorityName=authorityName;
		switch(authorityType){
			case "USER":
				this.authorityType=Type.USER;
				break;
			case "GROUP":
				this.authorityType=Type.GROUP;
				break;
			case "OWNER":
				this.authorityType=Type.OWNER;
				break;
			case "GUEST":
				this.authorityType=Type.GUEST;
				break;
			default:
				this.authorityType=Type.EVERYONE;
		}
	}
	public Authority(String authorityName, Type authorityType) {
		this.authorityName=authorityName;
		this.authorityType=authorityType;
	}
	@Schema(required = true, description = "")
	@JsonProperty("authorityName")
	public String getAuthorityName() {
		return authorityName;
	}

	public void setAuthorityName(String groupName) {
		this.authorityName = groupName;
	}

	/**
	 **/
	@Schema(description = "")
	@JsonProperty("authorityType")
	public Type getAuthorityType() {
		return authorityType;
	}

	public void setAuthorityType(Type authorityType) {
		this.authorityType = authorityType;
	}


	@JsonProperty
	public Map<String, String[]> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String[]> properties) {
		this.properties = properties;
	}

	@JsonProperty
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public boolean isEditable() {
		return editable;
	}


	@Override
	public boolean equals(Object obj) {

		if(!(obj instanceof Authority)){
			return false;
		}

		if(obj == null) return false;

		Authority toCompare = (Authority)obj;
		if(this.getAuthorityName().equals(toCompare.getAuthorityName()) &&
				this.getAuthorityType().equals(toCompare.getAuthorityType())){
			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (null == authorityName ? 0 : authorityName.hashCode());
		hash = 31 * hash + (null == authorityType ? 0 : authorityType.hashCode());
		return hash;
	}
}
