package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class VersionedNode extends Node{
    private Version version;


    @Data
    public static class Version implements Serializable {
        private String comment;
    }
}
