package org.edu_sharing.lightbend;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.*;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Component("configurationPropertiesAnnotationProcessor")
public class ConfigurationPropertiesAnnotationProcessor implements BeanDefinitionRegistryPostProcessor {

    public Object createConfigurationProperties(Class<?> beanClazz) {
        ConfigurationProperties annotation = beanClazz.getAnnotation(ConfigurationProperties.class);
        Config config = null;
        try {
            config = LightbendConfigLoader.get().getConfig(annotation.prefix());
        } catch (ConfigException e) {
            try {
                log.debug("Could not find config path '{}' for type {}: {}", annotation.prefix(), beanClazz.getName(), e.getMessage());
                return beanClazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new BeanInstantiationException(beanClazz, ex.getMessage(), ex);
            }
        }

        try {
            if (config != null) {
                return ConfigBeanFactory.create(config, beanClazz);
            } else {
                return beanClazz.newInstance();
            }
        } catch (ConfigException | InstantiationException | IllegalAccessException ex) {
            throw new BeanInstantiationException(beanClazz, ex.getMessage(), ex);
        }
    }

    @SneakyThrows
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));

        for (BeanDefinition beanDefinition : scanner.findCandidateComponents("org.edu_sharing")) {
            Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
            RootBeanDefinition rootBeanDefinition = new RootBeanDefinition(clazz);
            rootBeanDefinition.setTargetType(clazz);
            rootBeanDefinition.setFactoryBeanName("configurationPropertiesAnnotationProcessor");
            rootBeanDefinition.setFactoryMethodName("createConfigurationProperties");

            ConstructorArgumentValues properties = new ConstructorArgumentValues();
            properties.addGenericArgumentValue(clazz);
            rootBeanDefinition.setConstructorArgumentValues(properties);

            rootBeanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            rootBeanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
            if(beanDefinition instanceof AnnotatedBeanDefinition){
                AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
                String scope = annotatedBeanDefinition
                        .getMetadata()
                        .getAnnotations()
                        .get(Scope.class)
                        .getValue("scopeName", String.class)
                        .orElse(BeanDefinition.SCOPE_SINGLETON);
                rootBeanDefinition.setScope(scope);
            }

            registry.registerBeanDefinition(beanDefinition.getBeanClassName(), rootBeanDefinition);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
