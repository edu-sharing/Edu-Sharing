package org.edu_sharing.restservices.shared;

import lombok.Data;

import java.io.Serializable;

@Data
public class Content implements Serializable {
    private String url;
    private String hash;
    private String version;

}
