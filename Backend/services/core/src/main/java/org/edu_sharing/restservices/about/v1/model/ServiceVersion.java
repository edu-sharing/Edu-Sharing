package org.edu_sharing.restservices.about.v1.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ServiceVersion {

    private String repository;
    private String renderservice;

    @JsonProperty(required = true)
    private int major = 0;

    @JsonProperty(required = true)
    private int minor = 0;
}
