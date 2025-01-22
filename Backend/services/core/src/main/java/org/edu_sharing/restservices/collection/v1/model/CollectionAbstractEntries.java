package org.edu_sharing.restservices.collection.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.restservices.shared.Pagination;

@Data
public class CollectionAbstractEntries<T> {

    @JsonProperty(required = true)
    private List<T> collections = new ArrayList<>();
    private Pagination pagination;
}
