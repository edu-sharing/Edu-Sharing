package org.edu_sharing.restservices.shared;


import lombok.Data;


@Data
public class NodeAccess  {
  
  private String permission = null;
  private Boolean hasRight = null;



  @Override
  public String toString()  {
      return "class NodeAccess {\n" +
              "  permission: " + permission + "\n" +
              "  hasRight: " + hasRight + "\n" +
              "}\n";
  }
}
