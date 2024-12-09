package org.edu_sharing.metadataset.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class ValuespaceInfo implements Serializable {
    public enum ValuespaceType {
        SKOS
    }
    private String value;
    private ValuespaceType type;
    /**
     * when set true, the mds should continue loading even if the valuespace was not read properly
     * This might help for valuespaces which are external and not always available
     */
    private boolean lenient;

}
