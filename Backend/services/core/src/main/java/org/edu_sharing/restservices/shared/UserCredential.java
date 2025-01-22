package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.service.password.ValidPassword;;

@Data
public class UserCredential {

	private String oldPassword = null;

	@ValidPassword
	@JsonProperty(required = true)
	private String newPassword = null;
}
