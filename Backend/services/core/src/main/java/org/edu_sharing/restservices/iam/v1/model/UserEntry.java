package org.edu_sharing.restservices.iam.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.User;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class UserEntry  {

  @JsonProperty(required = true)
  private User person;
  private Boolean editProfile;
}
