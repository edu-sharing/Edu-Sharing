package org.edu_sharing.restservices.login.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;;

@Data
public class LoginCredentials {

	@JsonProperty(required = true)
	String userName;

	@JsonProperty(required = true)
	String password;

	@JsonProperty(required = true)
	String scope;
}
