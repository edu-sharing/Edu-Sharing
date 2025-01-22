package org.edu_sharing.restservices.login.v1.model;

import lombok.Data;

@Data
public class AuthenticationToken {
    String userId;
    String ticket;
}
