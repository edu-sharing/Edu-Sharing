package org.edu_sharing.spring.validation;

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;


@Configuration
public class ValidationConfiguration {
    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }
}
