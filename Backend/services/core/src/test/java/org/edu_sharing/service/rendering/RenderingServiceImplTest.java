package org.edu_sharing.service.rendering;

import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfresco.service.connector.Connector;
import org.edu_sharing.alfresco.service.connector.ConnectorFileType;
import org.edu_sharing.alfresco.service.connector.ConnectorList;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.service.connector.ConnectorServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.permission.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)

class RenderingServiceImplTest {
    @Mock
    private NodeService nodeService;
    @Mock
    private PermissionService permissionService;
    private RenderingServiceImpl underTest;
    private MockedStatic<ConnectorServiceFactory> connectorServiceFactoryMockedStatic;
    private MockedStatic<I18nAngular> i18nAngularMockedStatic;

    @BeforeEach
    void setUp() {
        underTest = new RenderingServiceImpl(nodeService, permissionService);

        ConnectorList connectorList = Mockito.mock(ConnectorList.class);
        connectorServiceFactoryMockedStatic = Mockito.mockStatic(ConnectorServiceFactory.class);
        connectorServiceFactoryMockedStatic.when(ConnectorServiceFactory::getConnectorList).thenReturn(connectorList);
        i18nAngularMockedStatic = Mockito.mockStatic(I18nAngular.class);
        i18nAngularMockedStatic.when(() -> I18nAngular.getTranslationAngular(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
                )
        ).thenAnswer(invocation -> invocation.getArguments()[1]);

        ConnectorFileType fileTypeNotEditable = new ConnectorFileType();
        fileTypeNotEditable.setMimetype("application/test_basic");
        fileTypeNotEditable.setEditable(false);
        ConnectorFileType fileTypeBasic = new ConnectorFileType();
        fileTypeBasic.setMimetype("application/test_basic");
        ConnectorFileType fileTypeZip = new ConnectorFileType();
        fileTypeZip.setMimetype("application/zip");
        ConnectorFileType fileTypeZipSpecific = new ConnectorFileType();
        fileTypeZipSpecific.setMimetype("application/zip");
        fileTypeZipSpecific.setCcressourcetype(CCConstants.CCM_PROP_CCRESSOURCETYPE);
        fileTypeZipSpecific.setCcressourceversion(CCConstants.CCM_PROP_CCRESSOURCEVERSION);
        fileTypeZipSpecific.setCcresourcesubtype(CCConstants.CCM_PROP_CCRESSOURCESUBTYPE);
        List<Connector> connectors = new ArrayList<>();
        Connector connector = new Connector();
        connector.setId("test_not_editable");
        connector.setFiletypes(List.of(
                fileTypeNotEditable
        ));
        connector.setHasViewMode(true);
        connectors.add(connector);

        connector = new Connector();
        connector.setId("test_mime_basic_view");
        connector.setFiletypes(List.of(
                fileTypeBasic
        ));
        connector.setHasViewMode(true);
        connectors.add(connector);

        connector = new Connector();
        connector.setId("test_mime_basic");
        connector.setFiletypes(Collections.singletonList(
                fileTypeBasic
        ));
        connectors.add(connector);

        connector = new Connector();
        connector.setId("test_mime_zip_all");
        connector.setFiletypes(Collections.singletonList(
                fileTypeZip
        ));
        connectors.add(connector);

        connector = new Connector();
        connector.setId("test_mime_zip_specific");
        connector.setFiletypes(Collections.singletonList(
                fileTypeZipSpecific
        ));
        connectors.add(connector);
        Mockito.lenient().when(connectorList.getConnectors()).thenReturn(connectors);
    }

    @AfterEach
    void teardown() {
        connectorServiceFactoryMockedStatic.close();
        i18nAngularMockedStatic.close();
    }

    @RepeatedTest(value = 5, name = RepeatedTest.LONG_DISPLAY_NAME)
    void getAvailableEditorsWithVersion() {
        List<RenderingServiceData.Editor> editors = underTest.getAvailableEditors(UUID.randomUUID().toString(),
                String.valueOf(Integer.valueOf((int) (Math.random() * 10000))),
                UUID.randomUUID().toString()
        );
        assertEquals(0, editors.size());
    }
    @Test
    void getAvailableEditors() {
        String nodeId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        Mockito.when(
                permissionService
                        .hasPermission(
                        ArgumentMatchers.eq(StoreRef.PROTOCOL_WORKSPACE),
                        ArgumentMatchers.eq(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier()),
                        ArgumentMatchers.eq(nodeId),
                        ArgumentMatchers.eq(userId),
                        ArgumentMatchers.eq(CCConstants.PERMISSION_WRITE)
                )).thenReturn(true);

        Mockito.when(
                nodeService.getContentMimetype(
                        ArgumentMatchers.eq(StoreRef.PROTOCOL_WORKSPACE),
                        ArgumentMatchers.eq(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier()),
                        ArgumentMatchers.eq(nodeId)
                )).thenReturn("application/test_no_connector");
        assertEquals(0, underTest.getAvailableEditors(nodeId, null, userId).size());
        Mockito.when(
                nodeService.getContentMimetype(
                        ArgumentMatchers.eq(StoreRef.PROTOCOL_WORKSPACE),
                        ArgumentMatchers.eq(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier()),
                        ArgumentMatchers.eq(nodeId)
                )).thenReturn("application/test_basic");
        List<RenderingServiceData.Editor> actual = underTest.getAvailableEditors(nodeId, null, userId);
        assertEquals(2, actual.size());
        assertEquals("test_mime_basic_view", actual.get(0).getId());
        assertEquals("test_mime_basic", actual.get(1).getId());


        // read only test
        Mockito.when(
                permissionService
                        .hasPermission(
                                ArgumentMatchers.eq(StoreRef.PROTOCOL_WORKSPACE),
                                ArgumentMatchers.eq(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier()),
                                ArgumentMatchers.eq(nodeId),
                                ArgumentMatchers.eq(userId),
                                ArgumentMatchers.eq(CCConstants.PERMISSION_WRITE)
                        )).thenReturn(false);
        actual = underTest.getAvailableEditors(nodeId, null, userId);
        assertEquals(1, actual.size());
        assertEquals("test_mime_basic_view", actual.get(0).getId());

        // zip editor test
        Mockito.when(
                permissionService
                        .hasPermission(
                                ArgumentMatchers.eq(StoreRef.PROTOCOL_WORKSPACE),
                                ArgumentMatchers.eq(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier()),
                                ArgumentMatchers.eq(nodeId),
                                ArgumentMatchers.eq(userId),
                                ArgumentMatchers.eq(CCConstants.PERMISSION_WRITE)
                        )).thenReturn(true);
        Mockito.when(
                nodeService.getContentMimetype(
                        ArgumentMatchers.eq(StoreRef.PROTOCOL_WORKSPACE),
                        ArgumentMatchers.eq(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier()),
                        ArgumentMatchers.eq(nodeId)
                )).thenReturn("application/zip");
        actual = underTest.getAvailableEditors(nodeId, null, userId);
        assertEquals(1, actual.size());
        assertEquals("test_mime_zip_all", actual.get(0).getId());

        Mockito.when(
                nodeService.getProperty(
                        ArgumentMatchers.eq(StoreRef.PROTOCOL_WORKSPACE),
                        ArgumentMatchers.eq(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier()),
                        ArgumentMatchers.eq(nodeId),
                        ArgumentMatchers.anyString()
                )).thenAnswer((props) -> props.getArguments()[3]);
        actual = underTest.getAvailableEditors(nodeId, null, userId);
        assertEquals(2, actual.size());
        assertEquals("test_mime_zip_all", actual.get(0).getId());
        assertEquals("test_mime_zip_specific", actual.get(1).getId());
    }

}
