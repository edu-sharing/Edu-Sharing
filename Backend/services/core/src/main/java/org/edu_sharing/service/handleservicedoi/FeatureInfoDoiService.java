package org.edu_sharing.service.handleservicedoi;

import org.edu_sharing.restservices.about.v1.model.FeatureInfo;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "repository.doiservice.enabled", havingValue = "true")
public class FeatureInfoDoiService implements FeatureInfo {
    @Override
    public Features getId() {
        return Features.doiService;
    }
}
