package org.edu_sharing.restservices.mds.v1.model;

import lombok.Data;
import org.edu_sharing.metadataset.v2.MetadataColumn;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class MdsColumn {
    private String id;
    private String format;
    private boolean showDefault;

    public MdsColumn(MetadataColumn column) {
        this.id = column.getId();
        this.showDefault = column.isShowDefault();
        this.format = column.getFormat();
    }
}

