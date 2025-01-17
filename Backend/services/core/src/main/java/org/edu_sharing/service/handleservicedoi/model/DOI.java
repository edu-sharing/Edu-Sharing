package org.edu_sharing.service.handleservicedoi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.edu_sharing.service.handleservicedoi.model.xml.Resource;

//@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DOI {
    private Data data;

    @JsonIgnore
    Resource xmlRepresentation;
}
