package org.edu_sharing.service.handleservicedoi;

import com.typesafe.config.Optional;
import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "repository.doiservice")
public class DoiConfig {
    private boolean enabled;
    @Optional
    private String baseUrl;
    @Optional
    private String accountId;
    @Optional
    private String prefix;
    @Optional
    private String password;
    @Optional
    private String repoUrl;

}
