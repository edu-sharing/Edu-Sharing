package org.edu_sharing.restservices.iam.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.shared.UserSimple;

import com.fasterxml.jackson.annotation.JsonProperty;

;

@Data
public class UserEntries {
    @JsonProperty(required = true)
    private List<UserSimple> users = null;

    @JsonProperty(required = true)
    private Pagination pagination = null;
}
