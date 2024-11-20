package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevokeDetails {
    @JsonProperty
    private String reason;
}
