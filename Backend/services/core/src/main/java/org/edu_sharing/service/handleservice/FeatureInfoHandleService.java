package org.edu_sharing.service.handleservice;

import org.edu_sharing.restservices.about.v1.model.FeatureInfo;;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(name = "repository.handleservice.enabled", havingValue = "true")
public class FeatureInfoHandleService implements FeatureInfo{

    @Override
    public Features getId() {
        return Features.handleService;
    }

}
