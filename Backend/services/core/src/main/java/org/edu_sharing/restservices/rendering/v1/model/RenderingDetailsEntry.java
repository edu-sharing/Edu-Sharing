package org.edu_sharing.restservices.rendering.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;


@Data
public class RenderingDetailsEntry {

    @JsonProperty(required = true)
    private String detailsSnippet;

    @JsonProperty(required = true)
    private String mimeType;

    @JsonProperty(required = true)
    private Node node;

}
