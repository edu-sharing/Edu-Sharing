package org.edu_sharing.restservices.lti.v13.model;


import lombok.Data;

import java.util.List;

@Data
public class JWKSResult {
    private List<JWKResult> keys;
}
