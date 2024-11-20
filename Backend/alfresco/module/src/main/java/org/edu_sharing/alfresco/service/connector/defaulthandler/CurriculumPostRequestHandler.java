package org.edu_sharing.alfresco.service.connector.defaulthandler;

import com.hazelcast.shaded.org.snakeyaml.engine.v2.api.lowlevel.Serialize;
import org.edu_sharing.alfresco.service.connector.SimpleConnector;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CurriculumPostRequestHandler implements SimpleConnector.PostRequestHandler {
    @Override
    public Map<String, Serializable> handleRequest(SimpleConnector.ConnectorRequest request, JSONObject apiResult) {
        try {
            String uri = getApiRoot(request) + "kanbans/" + apiResult.get("id").toString();
            setPermissions(request, apiResult);
            HashMap<String, Serializable> result = new HashMap<>();
            result.put(CCConstants.CCM_PROP_IO_WWWURL, uri);
            return result;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static URL getApiRoot(SimpleConnector.ConnectorRequest request) throws MalformedURLException {
        return URI.create(request.getSimpleConnector().getApi().getUrl()).resolve("/").toURL();
    }

    private void setPermissions(SimpleConnector.ConnectorRequest request, JSONObject apiResult) {
        /**
         * Die folgenden Informationen müssen dabei übermittelt werden:
         *
         *     'kanban_id',
         *     'subscribable_type', [Organization, Group, User]
         *     'subscribable_id', [hier würde ich aus den CNs die ID in Curriculum ermitteln]
         *
         *     'editable', [Boolean, default: false] ,
         *
         *     'owner_id'  [CNs => Ermittlung ID in Curriculum]
         *
         *
         */

    }
}
