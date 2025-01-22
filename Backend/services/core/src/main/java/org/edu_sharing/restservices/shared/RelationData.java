package org.edu_sharing.restservices.shared;

import lombok.Builder;
import lombok.Value;
import org.edu_sharing.service.relations.OutputRelationType;

import java.util.Date;

@Value
@Builder
public class RelationData {
    Node node;
    User creator;
    Date timestamp;
    OutputRelationType type;
}
