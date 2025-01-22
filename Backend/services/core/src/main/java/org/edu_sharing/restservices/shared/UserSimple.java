package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.restservices.UserStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserSimple extends Authority{
	private String userName;
	private UserProfile profile = null;
    private UserStatus status;
	private List<Organization> organizations;

	public UserSimple(){super();}
	public UserSimple(org.edu_sharing.repository.client.rpc.User user) {
		super(user.getAuthorityName(),user.getAuthorityType());
		userName=user.getAuthorityDisplayName();
		profile=new UserProfile(user);
	}

	public static UserSimple getDummy(String name) {
		UserSimple userSimple = new UserSimple();
		userSimple.setAuthorityName(name);
		userSimple.setAuthorityType(Type.USER);
		return userSimple;
	}
}
