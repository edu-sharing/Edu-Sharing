package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserRender extends UserSimple implements Serializable {
    private String primaryAffiliation;
    private List<String> remoteRoles;
    private boolean isGuest;

}
