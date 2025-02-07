package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.when;

class SearchServiceElasticTest {

    private SearchServiceElastic underTest;
    private MockedStatic<AlfAppContextGate> alfAppContextGateMockedStatic;
    private MockedConstruction<MCAlfrescoAPIClient> mcAlfrescoApiClientMockedStatic;
    private MockedStatic<ToolPermissionServiceFactory> toolPermissionServiceMockedStatic;
    private ToolPermissionService toolPermissionService;
    private ServiceRegistry serviceRegistry;
    private MutableAuthenticationService authenticationService;
    private AuthorityService authorityService;
    private MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic;

    @BeforeEach() void beforeEach() {
        toolPermissionService = Mockito.mock(ToolPermissionService.class);
        serviceRegistry = Mockito.mock(ServiceRegistry.class);
        authenticationService = Mockito.mock(MutableAuthenticationService.class);
        when(authenticationService.getCurrentUserName()).thenReturn("tester");
        authorityService = Mockito.mock(AuthorityService.class);
        when(authorityService.getAuthorities()).thenReturn(new HashSet<>(Set.of("test_group1", "test_group2")));
        when(serviceRegistry.getAuthenticationService()).thenReturn(authenticationService);
        when(serviceRegistry.getAuthorityService()).thenReturn(authorityService);
        mcAlfrescoApiClientMockedStatic = Mockito.mockConstruction(MCAlfrescoAPIClient.class);
        alfAppContextGateMockedStatic = Mockito.mockStatic(AlfAppContextGate.class);
        toolPermissionServiceMockedStatic = Mockito.mockStatic(ToolPermissionServiceFactory.class);
        authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("tester");
        toolPermissionServiceMockedStatic.when(ToolPermissionServiceFactory::getInstance).thenReturn(toolPermissionService);
        ApplicationContext applicationContextMock = Mockito.mock(ApplicationContext.class);
        when(applicationContextMock.getBean(ServiceRegistry.SERVICE_REGISTRY)).thenReturn(serviceRegistry);
        alfAppContextGateMockedStatic.when(() -> AlfAppContextGate.getApplicationContext()).thenReturn(applicationContextMock);

        underTest = new SearchServiceElastic("home");
    }
    @AfterEach() void afterEach() {
        alfAppContextGateMockedStatic.close();
        mcAlfrescoApiClientMockedStatic.close();
        toolPermissionServiceMockedStatic.close();
        authenticationUtilMockedStatic.close();
    }


    @Test
    void getGlobalConditions() {
        BoolQuery.Builder conditions = underTest.getGlobalConditions(Collections.singletonList("scope"), Collections.singletonList("read"), new MetadataQuery());

        SearchServiceElasticTestUtils.assertQuery(
                "{\n" +
                        "  \"bool\": {\n" +
                        "    \"minimum_should_match\": \"1\",\n" +
                        "    \"must\": [\n" +
                        "      {\n" +
                        "        \"bool\": {\n" +
                        "          \"minimum_should_match\": \"1\",\n" +
                        "          \"must\": [\n" +
                        "            {\n" +
                        "              \"match\": {\n" +
                        "                \"nodeRef.storeRef.protocol\": {\n" +
                        "                  \"query\": \"workspace\"\n" +
                        "                }\n" +
                        "              }\n" +
                        "            }\n" +
                        "          ],\n" +
                        "          \"should\": [\n" +
                        "            {\n" +
                        "              \"match\": {\n" +
                        "                \"permissions.read\": {\n" +
                        "                  \"query\": \"scope\"\n" +
                        "                }\n" +
                        "              }\n" +
                        "            }\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"must_not\": [\n" +
                        "      {\n" +
                        "        \"exists\": {\n" +
                        "          \"field\": \"properties.ccm:eduscopename\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"should\": [\n" +
                        "      {\n" +
                        "        \"match\": {\n" +
                        "          \"owner\": {\n" +
                        "            \"query\": \"tester\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"bool\": {\n" +
                        "          \"minimum_should_match\": \"1\",\n" +
                        "          \"should\": [\n" +
                        "            {\n" +
                        "              \"match\": {\n" +
                        "                \"permissions.read\": {\n" +
                        "                  \"query\": \"test_group1\"\n" +
                        "                }\n" +
                        "              }\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"match\": {\n" +
                        "                \"permissions.read\": {\n" +
                        "                  \"query\": \"GROUP_EVERYONE\"\n" +
                        "                }\n" +
                        "              }\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"match\": {\n" +
                        "                \"permissions.read\": {\n" +
                        "                  \"query\": \"tester\"\n" +
                        "                }\n" +
                        "              }\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"match\": {\n" +
                        "                \"permissions.read\": {\n" +
                        "                  \"query\": \"test_group2\"\n" +
                        "                }\n" +
                        "              }\n" +
                        "            }\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}",
                conditions
        );
    }
}