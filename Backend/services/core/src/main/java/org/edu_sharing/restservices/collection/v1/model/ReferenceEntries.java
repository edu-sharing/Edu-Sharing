package org.edu_sharing.restservices.collection.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;;
import lombok.Data;
import org.edu_sharing.restservices.shared.Pagination;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReferenceEntries {
    @JsonProperty(required = true)
    private List<CollectionReference> references = new ArrayList<>();
    private Pagination pagination;
}
