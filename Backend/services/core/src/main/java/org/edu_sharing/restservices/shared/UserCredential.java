package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.edu_sharing.service.password.ValidPassword;;

@Data
@Schema(description = "")
public class UserCredential {

	@Schema(description = "")
	@JsonProperty("oldPassword")
	private String oldPassword = null;

	@ValidPassword
	@Schema(required = true, description = "")
	@JsonProperty("newPassword")
	private String newPassword = null;
}
