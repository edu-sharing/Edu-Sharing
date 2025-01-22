package org.edu_sharing.restservices.mds.v1.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.edu_sharing.metadataset.v2.MetadataColumn;
import org.edu_sharing.metadataset.v2.MetadataList;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class MdsList {
    private String id;
    private List<MdsColumn> columns;

    public MdsList() {
    }

    public MdsList(MetadataList list) {
        this.id = list.getId();
        if (list.getColumns() != null) {
            columns = new ArrayList<>();
            for (MetadataColumn column : list.getColumns()) {
                columns.add(new MdsColumn(column));
            }
        }
    }
}

