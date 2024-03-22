package org.edu_sharing.metadataset.v2;

import lombok.AllArgsConstructor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class MetadataQueryPreprocessor {
    private final String syntax;
    static Logger logger = Logger.getLogger(MetadataQueryPreprocessor.class);
    /**
     * runs a preprocessor defined in the paramter for a given value
     * if the parameter does not require any preoprocessor, the value will be returned
     */
    public String run(MetadataQueryParameter parameter,String valueIn) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if(parameter.getPreprocessor()==null)
            return valueIn;
        Method method=this.getClass().getDeclaredMethod(parameter.getPreprocessor(), MetadataQueryParameter.class, String.class);
        return (String) method.invoke(this, parameter, valueIn);
    }


    private String node_path(MetadataQueryParameter parameter,String value){
        return AuthenticationUtil.runAsSystem(()-> {
            ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
            ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, value);
            if(!serviceRegistry.getNodeService().exists(nodeRef)){
                return null;
            }
            if(syntax.equals(MetadataReader.QUERY_SYNTAX_DSL)) {
                Path path = serviceRegistry.getNodeService().getPath(nodeRef);
                List<String> result = new ArrayList<>(path.size());
                for(int i=0; i<path.size(); i++) {
                    if(path.get(i) instanceof Path.ChildAssocElement) {
                        result.add(((Path.ChildAssocElement) path.get(i)).getRef().getChildRef().getId());
                    } else {
                        logger.warn("Invalid path type: " + path.get(i).getClass().getName());
                        return null;
                    }
                }
                return StringUtils.join(result, "/");
            }
            return serviceRegistry.getNodeService().getPath(nodeRef).toPrefixString(serviceRegistry.getNamespaceService());
        });
    }
    // convert values like YYYY-MM-DD to a unix millis string (e.g. for elastic)
    private String date_millis(MetadataQueryParameter parameter,String value){
        try {
            return String.valueOf(new SimpleDateFormat("yyyy-MM-dd").parse(value).getTime());
        } catch (ParseException e) {
            logger.warn("Mds could not parse date: " + value, e);
            return value;
        }
    }
}
