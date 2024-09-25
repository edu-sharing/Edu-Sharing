package org.edu_sharing.service.nodeservice;

import com.typesafe.config.ConfigObject;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PropertiesInterceptorFactory {
    static Logger logger = Logger.getLogger(PropertiesInterceptorFactory.class);


    static List<PropertiesGetInterceptor> propertiesGetInterceptors = null;
    static List<PropertiesSetInterceptor> propertiesSetInterceptors = null;
    static List<NodeServiceInterceptorPermissions> nodeServiceInterceptorPermissions = null;

    public static List<?> getInterceptors(List<String> className) {
        synchronized (PropertiesInterceptorFactory.class) {
            try {
                ArrayList<Class<?>> clazz = (ArrayList<Class<?>>)className.stream().map((String className1) -> {
                    try {
                        return Class.forName(className1);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toCollection((Supplier<ArrayList<Class<?>>>) ArrayList::new));
                return clazz.stream().map((c) -> {
                    try {
                        return c.newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
    public static List<? extends PropertiesSetInterceptor> getPropertiesSetInterceptors(){
        if(propertiesSetInterceptors == null){
            synchronized (PropertiesInterceptorFactory.class){
                try{
                    List<String> className = new ArrayList<>(LightbendConfigLoader.get().getStringList("repository.interceptors.properties.set"));
                    List<PropertiesSetInterceptor> clazz =
                            (List<PropertiesSetInterceptor>) getInterceptors(className);
                    if(clazz.size() == 0) {
                        logger.info("No interceptors for set properties defined, will use default handling");
                    }
                    propertiesSetInterceptors = clazz;
                }catch(Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }
        return propertiesSetInterceptors;
    }
    public static List<? extends PropertiesGetInterceptor> getPropertiesGetInterceptors(){
        if(propertiesGetInterceptors == null){
            synchronized (PropertiesInterceptorFactory.class){
                try{
                    List<String> className = new ArrayList<>(LightbendConfigLoader.get().getStringList("repository.interceptors.properties.get"));
                    List<PropertiesGetInterceptor> clazz =
                            (List<PropertiesGetInterceptor>) getInterceptors(className);
                    if(clazz.size() == 0) {
                        logger.info("No interceptors for get properties defined, will use default handling");
                    }
                    clazz.add(0, new PropertiesGetInterceptorPermissions());

                    propertiesGetInterceptors = clazz;
                }catch(Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }
        return propertiesGetInterceptors;
    }

    public static List<? extends NodeServiceInterceptorPermissions> getNodeServiceInterceptorPermissions(){
        if(nodeServiceInterceptorPermissions == null){
            synchronized (PropertiesInterceptorFactory.class){
                try{
                    List<String> classes = LightbendConfigLoader.get().getObjectList("repository.interceptors.permissions")
                            .stream()
                            .map(co -> co.toConfig())
                            .map(c -> c.getString("clazz"))
                            .collect(Collectors.toList());
                    List<NodeServiceInterceptorPermissions> clazz =
                            (List<NodeServiceInterceptorPermissions>) getInterceptors(classes);
                    if(clazz.size() == 0) {
                        logger.info("No interceptors for get properties defined, will use default handling");
                    }

                    nodeServiceInterceptorPermissions = clazz;
                }catch(Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }
        return nodeServiceInterceptorPermissions;
    }

    public static void refresh(){
        propertiesGetInterceptors = null;
        propertiesSetInterceptors = null;
        nodeServiceInterceptorPermissions = null;
    }

    public static PropertiesGetInterceptor.PropertiesContext getPropertiesContext(NodeRef nodeRef, Map<String,Object> properties, List<String> aspects, Map<String, Boolean> permissions, Map<String, Object> elasticsearchSource){
        PropertiesGetInterceptor.PropertiesContext propertiesContext = new PropertiesGetInterceptor.PropertiesContext();
        propertiesContext.setProperties(properties);
        propertiesContext.setAspects(aspects);
        propertiesContext.setNodeRef(nodeRef);
        propertiesContext.setPermissions(permissions);
        propertiesContext.setElasticsearchSource(elasticsearchSource);
        propertiesContext.setSource(CallSourceHelper.getCallSource());
        return propertiesContext;
    }
}
