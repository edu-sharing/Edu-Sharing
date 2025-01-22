package org.edu_sharing.restservices.iam.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;;

@Data
public class ProfileSettings {
	@JsonProperty(required = true)
	private boolean showEmail; // show or hide email in profile
}
