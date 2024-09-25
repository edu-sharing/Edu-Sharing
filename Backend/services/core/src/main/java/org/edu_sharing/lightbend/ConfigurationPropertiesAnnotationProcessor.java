package org.edu_sharing.lightbend;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

@Slf4j
@Component
public class ConfigurationPropertiesAnnotationProcessor implements BeanDefinitionRegistryPostProcessor {
    ExpressionParser parser = new SpelExpressionParser();

    public Object createConfigurationProperties(Class<?> beanClazz, @Nullable ConfigParam param) {
        String prefix = getPrefix(beanClazz);

        Config config;
        try {
            Expression expression = parser.parseExpression(prefix, ParserContext.TEMPLATE_EXPRESSION);
            EvaluationContext context = new StandardEvaluationContext(param);
            String configPath = expression.getValue(context, String.class);
            config = LightbendConfigLoader.get().getConfig(configPath);
        } catch (ConfigException e) {
            try {
                log.debug("Could not find config path '{}' for type {}: {}", prefix, beanClazz.getName(), e.getMessage());
                return beanClazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException ex) {
                throw new BeanInstantiationException(beanClazz, ex.getMessage(), ex);
            }
        }

        try {
            if (config != null) {
                return ConfigBeanFactory.create(config, beanClazz);
            } else {
                return beanClazz.getDeclaredConstructor().newInstance();
            }
        } catch (ConfigException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException ex) {
            throw new BeanInstantiationException(beanClazz, ex.getMessage(), ex);
        }
    }

    private String getPrefix(final Class<?> beanClazz) {
        ConfigurationProperties configurationProperties = beanClazz.getAnnotation(ConfigurationProperties.class);
        if(configurationProperties != null) {
            return configurationProperties.prefix();
        }

        ParameterizedConfigurationProperties parameterizedConfigurationProperties = beanClazz.getAnnotation(ParameterizedConfigurationProperties.class);
        if(parameterizedConfigurationProperties != null) {
            return parameterizedConfigurationProperties.prefix();
        }

        throw new NotImplementedException("Missing implementation for ConfigurationProperties annotation of class: " + beanClazz.getName());
    }

    @SneakyThrows
    @Override
    public void postProcessBeanDefinitionRegistry(@NotNull BeanDefinitionRegistry registry) throws BeansException {

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(ParameterizedConfigurationProperties.class));

        for (BeanDefinition beanDefinition : scanner.findCandidateComponents("org.edu_sharing")) {
            Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());

            RootBeanDefinition rootBeanDefinition = new RootBeanDefinition(clazz);
            rootBeanDefinition.setTargetType(clazz);
            rootBeanDefinition.setFactoryBeanName("configurationPropertiesAnnotationProcessor");
            rootBeanDefinition.setFactoryMethodName("createConfigurationProperties");

            ConstructorArgumentValues properties = new ConstructorArgumentValues();
            properties.addGenericArgumentValue(clazz);
            //properties.addGenericArgumentValue((Object)null);
            rootBeanDefinition.setConstructorArgumentValues(properties);
            rootBeanDefinition.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
            rootBeanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            rootBeanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
            if (beanDefinition instanceof AnnotatedBeanDefinition) {
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
    public void postProcessBeanFactory(@NotNull ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
