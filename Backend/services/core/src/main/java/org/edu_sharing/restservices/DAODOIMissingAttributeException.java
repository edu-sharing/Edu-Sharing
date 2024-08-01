package org.edu_sharing.restservices;

import org.edu_sharing.service.handleservicedoi.DOIServiceException;
import org.edu_sharing.service.handleservicedoi.DOIServiceMissingAttributeException;
import org.edu_sharing.service.handleservicedoi.DOIServiceNotConfiguredException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DAODOIMissingAttributeException extends DAODOIException{

    public DAODOIMissingAttributeException(DOIServiceMissingAttributeException t, String nodeId) {
        super(t, nodeId);
        cause = t;
    }

    @Override
    public Map<String, Serializable> getDetails() {
        return new HashMap<>() {{
            put("property", ((DOIServiceMissingAttributeException)cause).getProperty());
            put("schemaField", ((DOIServiceMissingAttributeException)cause).getSchemaField());
            put("cause",cause.getClass().getSimpleName());
        }};
    }
}
