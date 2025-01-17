package org.edu_sharing.service.handleservice;

import com.typesafe.config.Optional;
import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "repository.handleservice")
public class HandleConfig {
    private boolean enabled;
    @Optional
    private String prefix;
    @Optional
    private String privkey;
    @Optional
    private String repoid;
    @Optional
    private String email;
    @Optional
    private String configDir;
}
