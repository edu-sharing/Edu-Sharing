package org.edu_sharing.restservices.mds.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.edu_sharing.metadataset.v2.MetadataColumn;
import org.edu_sharing.metadataset.v2.MetadataSortColumn;

@Data
public class MdsSortColumn {
    @JsonProperty(required = true)
    private String id;
    private String mode;

    public MdsSortColumn(MetadataSortColumn column) {
        this.id = column.getId();
        this.mode = column.getMode();
    }
}

