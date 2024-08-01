package org.edu_sharing.restservices;

import org.edu_sharing.service.handleservicedoi.DOIServiceException;
import org.edu_sharing.service.handleservicedoi.DOIServiceMissingAttributeException;
import org.edu_sharing.service.handleservicedoi.DOIServiceNotConfiguredException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DAODOIException extends DAOException{

    DOIServiceException cause;

    public DAODOIException(DOIServiceException t, String nodeId) {
        super(t, nodeId);
        cause = (DOIServiceException) t;
    }

    @Override
    public Map<String, Serializable> getDetails() {
        if(cause instanceof DOIServiceNotConfiguredException){
            return new HashMap<>() {{
                put("message", "doi service not configured");
                put("cause",cause.getClass().getSimpleName());
            }};
        }
        return new HashMap<>() {{
            put("message", "doi service method failed");
            put("cause",cause.getClass().getSimpleName());
        }};
    }

    public static DAODOIException instance(DOIServiceException t, String nodeId) {

        if(t instanceof DOIServiceMissingAttributeException){
            return new DAODOIMissingAttributeException((DOIServiceMissingAttributeException)t,nodeId);
        }

        return new DAODOIException(t, nodeId);
    }
}
