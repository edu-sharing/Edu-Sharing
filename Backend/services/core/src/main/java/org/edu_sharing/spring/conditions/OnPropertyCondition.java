package org.edu_sharing.spring.conditions;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;
import java.util.stream.Collectors;

public class OnPropertyCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        List<AnnotationAttributes> allAnnotationAttributes = metadata.getAnnotations()
                .stream(ConditionalOnProperty.class.getName())
                .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
                .map(MergedAnnotation::asAnnotationAttributes).collect(Collectors.toList());
        if(allAnnotationAttributes.isEmpty()){
            return false;
        }
        AnnotationAttributes annotationAttributes = allAnnotationAttributes.get(0);
        String name = annotationAttributes.getString("name");
        String hasValue = annotationAttributes.getString("havingValue");
        String configuredValue = context.getEnvironment().getProperty(name);
        return hasValue.equals(configuredValue);
    }
}
