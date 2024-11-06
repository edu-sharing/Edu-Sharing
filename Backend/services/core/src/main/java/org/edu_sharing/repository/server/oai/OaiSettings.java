package org.edu_sharing.repository.server.oai;

import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;

import java.util.List;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "exporter.oai")
public class OaiSettings {
    private boolean enabled;
    private OaiIdentifier identify;
    private String identiferPrefix;
    private int itemsPerPage;
    private String metadataPrefix;
    private List<String> sets;
}

