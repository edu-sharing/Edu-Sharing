package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.restservices.shared.Pagination;

import java.util.ArrayList;
import java.util.List;

@Data
public class AbstractEntries<T>  {

    @JsonProperty(required = true)
    private List<T> nodes = new ArrayList<>();
    @JsonProperty(required = true)
    private Pagination pagination;

    public AbstractEntries(){
    }
}
