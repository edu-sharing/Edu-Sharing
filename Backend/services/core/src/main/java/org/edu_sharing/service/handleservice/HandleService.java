package org.edu_sharing.service.handleservice;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.tools.URLHelper;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Map;

public interface HandleService {

    public boolean enabled();

    public boolean available();

    public String generateId() throws Exception;

    public String create(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception;

    public String update(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception;

    default boolean updateState(String nodeId, String eventState) throws Exception {
        Logger.getLogger(HandleService.class).debug("update state is not implemented for this service");
        return false;
    }

    public String delete(String handleId, String nodeId) throws Exception;


    public default String getContentLink(Map<QName, Serializable> properties) throws Exception{
        return URLHelper.getNgRenderNodeUrl((String)properties.get(ContentModel.PROP_NODE_UUID), null, false);
    }

    public String getHandleIdProperty();

}
