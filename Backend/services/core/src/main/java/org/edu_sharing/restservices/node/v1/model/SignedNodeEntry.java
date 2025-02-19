package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.restservices.shared.Node;


@Data
public class SignedNodeEntry {

    @JsonProperty(required = true)
    private Node node;
    @JsonProperty(required = true)
    private String jwt;
    @JsonProperty(required = true)
    private String signedNode;
    @JsonProperty(required = true)
    private String signature;
}
