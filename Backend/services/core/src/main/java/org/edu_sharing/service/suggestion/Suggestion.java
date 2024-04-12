package org.edu_sharing.service.suggestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Suggestion {
    String id;
    String nodeId;

    String propertyId;
    Object value;

    SuggestionType type;
    SuggestionStatus status;
    String description;
    double confidence = 0;

    Date created;
    String createdBy;
    Date modified;
    String modifiedBy;
}
