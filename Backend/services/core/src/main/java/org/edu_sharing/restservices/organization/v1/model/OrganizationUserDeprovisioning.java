package org.edu_sharing.restservices.organization.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

;


@Data
public class OrganizationUserDeprovisioning {
    @JsonProperty()
    @Schema(description = "Shall the user data within this organization be cleaned up")
    private Mode mode;
    @Schema(description = "Receiver authority if mode == assign")
    private String receiver;
    public enum Mode {
        @Schema(description = "Do nothing, the files will still belong to the owner")
        none,
        @Schema(description = "Assign the files to a new authority; The group admin group will also get coordinator permissions (requires admin rights)")
        assign
    }
}
