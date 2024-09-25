package org.edu_sharing.lightbend;

import org.springframework.context.annotation.Scope;

import java.lang.annotation.*;

/**
 * ParameterizedConfigurationProperties is a specialized type of {@link ConfigurationProperties} with implicit {@link Scope} of "prototype"
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Scope("prototype")
public @interface ParameterizedConfigurationProperties {

    /**
     * The prefix of the properties that are valid to bind to this object. A valid prefix is
     * defined by one or more words separated with dots (e.g. "repository.system.feature"). You can also use Spring EL to define a path.
     */
    String prefix();
}
