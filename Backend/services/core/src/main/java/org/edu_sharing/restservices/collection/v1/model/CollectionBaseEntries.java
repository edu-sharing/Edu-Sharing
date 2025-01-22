package org.edu_sharing.restservices.collection.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;

import java.util.ArrayList;
import java.util.List;

@Data
public class CollectionBaseEntries {

    private List<Node> entries = new ArrayList<>();
    private Pagination pagination;
}
