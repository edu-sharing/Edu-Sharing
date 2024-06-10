package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Schema(description = "")
public class NodeRelation {
    Node node; // TODO we can delete this cause it's unused by the frontend
    @Singular
    List<RelationData> relations;
}
