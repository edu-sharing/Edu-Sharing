package org.edu_sharing.restservices.lti.v13.model;

import lombok.Data;

@Data
public class JWKResult {
    private String kty;
    private String alg;
    private String kid;
    private String e;
    private String n;
    private String use;
}
