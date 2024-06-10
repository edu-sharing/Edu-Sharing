package org.edu_sharing.service.suggestion;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Setter
@Component
public class SuggestionServiceFactory implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public SuggestionService getServiceByAppId(String appId) {
        return applicationContext.getBean(SuggestionService.class);
    }

    public SuggestionService getLocalService() {
        return applicationContext.getBean(SuggestionService.class);
    }
}
