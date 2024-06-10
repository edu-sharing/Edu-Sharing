package org.edu_sharing.repository.tomcat;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.jobs.quartz.AbstractJob;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassHelper {

    static Logger logger = Logger.getLogger(ClassHelper.class);

    public static List<Class> getSubclasses(Class clazz){
        return getSubclasses(clazz, null);
    }
    public static List<Class> getSubclasses(Class clazz, String packageName){
        if(packageName == null) {
            packageName = "org.edu_sharing";
        }

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new org.springframework.core.type.filter.AssignableTypeFilter(clazz));

        List<Class> result = new ArrayList<>();
        for (BeanDefinition beanDefinition : scanner.findCandidateComponents(packageName)) {
            try {
                result.add(Class.forName(beanDefinition.getBeanClassName()));
            } catch (ClassNotFoundException ignored) { }
        }
        return result;
    }

    public static List<Field> getStaticFields(Class clazz){
        Field[] declaredFields = clazz.getDeclaredFields();
        List<Field> staticFields = new ArrayList<>();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                staticFields.add(field);
            }
        }
        return staticFields;
    }
}
