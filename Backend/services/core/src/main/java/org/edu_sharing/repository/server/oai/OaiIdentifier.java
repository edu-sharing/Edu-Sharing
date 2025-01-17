package org.edu_sharing.repository.server.oai;

import com.typesafe.config.Optional;
import lombok.Data;

@Data
public class OaiIdentifier {
    @Optional
    private String name;
    private String adminEmail;
    private String description;
    private String delete;
    private String granularity;
    private String earliestDate;
}
