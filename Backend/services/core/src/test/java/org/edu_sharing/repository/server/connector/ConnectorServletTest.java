package org.edu_sharing.repository.server.connector;

import com.github.davidmoten.guavamini.Maps;
import com.lyncode.test.check.Assert;
import org.edu_sharing.alfresco.service.connector.SimpleConnector;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class ConnectorServletTest {

    private ConnectorServlet underTest;

    @BeforeEach
    void setUp() {
        underTest = new ConnectorServlet();
    }

    @Test
    @Disabled
    void handleSimpleConnectorTestOauthFormCurriculum() throws UnsupportedEncodingException {
        SimpleConnector connector = new SimpleConnector();
        SimpleConnector.SimpleConnectorApi api = new SimpleConnector.SimpleConnectorApi();
        connector.setApi(api);
        api.setUrl("https://curriculum-dev.schulcampus-rlp.de/api/v1/kanbans");
        api.setMethod(SimpleConnector.SimpleConnectorApi.Method.Post);
        api.setPostRequestHandler("org.edu_sharing.alfresco.service.connector.defaulthandler.CurriculumPostRequestHandler");
        api.setBodyType(SimpleConnector.SimpleConnectorApi.BodyType.Form);
        api.setBody(Map.ofEntries(
                        Map.entry("title", "Sample-Kanban " + UUID.randomUUID())
                        // Map.entry("owner_cn", "${user.cm:authorityName}")
                )
        );
        SimpleConnector.SimpleConnectorAuthentication authentication = new SimpleConnector.SimpleConnectorAuthentication();
        api.setAuthentication(authentication);
        authentication.setUrl("https://curriculum-dev.schulcampus-rlp.de/oauth/token");
        authentication.setMethod(SimpleConnector.SimpleConnectorApi.Method.Post);
        authentication.setBodyType(SimpleConnector.SimpleConnectorApi.BodyType.Form);
        authentication.setType(SimpleConnector.SimpleConnectorAuthentication.AuthenticationType.OAuth);
        authentication.setBody(Map.ofEntries(
                        Map.entry("grant_type", "client_credentials"),
                        Map.entry("client_id", "xxx"),
                        Map.entry("client_secret", "xxx")
                )
        );
        connector.setId("test");
        HashMap<String, Serializable> result = underTest.handleSimpleConnector(Maps.empty(), connector, null);
        Assert.assertThat(result.get(CCConstants.CCM_PROP_IO_WWWURL).toString(), CoreMatchers.containsString("https://curriculum-dev.schulcampus-rlp.de/kanbans/"));
    }
}