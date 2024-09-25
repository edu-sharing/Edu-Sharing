package org.edu_sharing.lightbend;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ConfigurationPropertyClassFactory {

    private final BeanFactory beanFactory;

    public <T> T getConfiguration(Class<T> clazz) {
        return beanFactory.getBean(clazz);
    }

    public <T> T getConfiguration(Class<T> clazz, ConfigParam configParam) {
        return beanFactory.getBean(clazz, clazz, configParam);
    }
}
