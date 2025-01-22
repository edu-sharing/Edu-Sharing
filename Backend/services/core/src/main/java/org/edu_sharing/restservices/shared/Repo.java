package org.edu_sharing.restservices.shared;

import lombok.Data;
import java.io.Serializable;

@Data
public class Repo implements Serializable {
  private String id = null;
  private Boolean isHomeRepo = null;
  private String title = null;
  private String repositoryType = null;
  private String icon = null;
  private String logo = null;
  private boolean renderingSupported;
}
