package org.edu_sharing.restservices.shared;

import lombok.Data;

@Data
public class Contributor{
    String property;
    String firstname;
    String lastname;
    String email;
    String vcard;
    String org;
}
