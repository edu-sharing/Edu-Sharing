package org.edu_sharing.restservices.iam.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.Pagination;

import com.fasterxml.jackson.annotation.JsonProperty;

;

@Data
public class AuthorityEntries {

	@JsonProperty(required = true)
	private List<Authority> authorities = null;

	@JsonProperty(required = true)
	private Pagination pagination = null;
}
