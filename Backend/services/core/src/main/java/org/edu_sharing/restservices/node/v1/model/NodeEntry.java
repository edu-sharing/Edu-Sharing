package org.edu_sharing.restservices.node.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;


@Data
public class NodeEntry {

    @JsonProperty(required = true)
    private Node node = null;
}
