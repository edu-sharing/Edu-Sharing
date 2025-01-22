package org.edu_sharing.restservices.network.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.Repo;

import com.fasterxml.jackson.annotation.JsonProperty;


@Data
public class RepoEntries  {

  @JsonProperty(required = true)
  private List<Repo> repositories = null;
}
