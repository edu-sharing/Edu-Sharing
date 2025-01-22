package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.repository.client.rpc.ACE;

import java.io.Serializable;
import java.util.Map;

@Data
public class Authority implements Serializable {

    public enum Type {USER, GROUP, OWNER, EVERYONE, GUEST}

    @JsonProperty(required = true)
    private String authorityName;
    private Type authorityType;
    private Map<String, String[]> properties;
    boolean editable;

    public Authority() {
    }

    public Authority(ACE ace) {
        authorityName = ace.getAuthority();
        authorityType = Authority.Type.valueOf(ace.getAuthorityType());
        if (ace.getUser() != null) {
            editable = ace.getUser().isEditable();
        }
    }

    public Authority(String authorityName, String authorityType) {
        this.authorityName = authorityName;
        switch (authorityType) {
            case "USER":
                this.authorityType = Type.USER;
                break;
            case "GROUP":
                this.authorityType = Type.GROUP;
                break;
            case "OWNER":
                this.authorityType = Type.OWNER;
                break;
            case "GUEST":
                this.authorityType = Type.GUEST;
                break;
            default:
                this.authorityType = Type.EVERYONE;
        }
    }

    public Authority(String authorityName, Type authorityType) {
        this.authorityName = authorityName;
        this.authorityType = authorityType;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Authority)) {
            return false;
        }

        Authority toCompare = (Authority) obj;
        return this.authorityName.equals(toCompare.authorityName) &&
                this.authorityType.equals(toCompare.authorityType);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (null == authorityName ? 0 : authorityName.hashCode());
        hash = 31 * hash + (null == authorityType ? 0 : authorityType.hashCode());
        return hash;
    }
}
