package org.edu_sharing.restservices.collection.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.model.CollectionRef;

@Data
@EqualsAndHashCode(callSuper = true)
public class CollectionRelationReference extends Node {
    private CollectionRef.RelationType relationType;
}
