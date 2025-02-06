package org.edu_sharing.alfresco.action;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.lightbend.LightbendConfigCache;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.UUID;

import static org.mockito.Mockito.times;

class RessourceInfoExecuterTest {

    private RessourceInfoExecuter underTest;

    private final NodeService mockedNodeService = Mockito.mock(NodeService.class);
    private final LightbendConfigLoader mockedLightbendConfigLoader = Mockito.mock(LightbendConfigLoader.class);
    @BeforeEach
    void setUp() {
        underTest = new RessourceInfoExecuter(mockedLightbendConfigLoader);
        underTest.setNodeService(mockedNodeService);
    }

    @Test
    void processGeogebraTest() {
        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString());
        underTest.processGeogebra(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<geogebra format=\"5.0\" version=\"5.0.749.0\" app=\"notes\" platform=\"w\" id=\"a010b22b-922e-4ceb-b74d-439165947b4e\"  xsi:noNamespaceSchemaLocation=\"http://www.geogebra.org/apps/xsd/ggb.xsd\" xmlns=\"\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ></geogebra>".getBytes()), nodeRef);
        Mockito.verify(mockedNodeService, times(1)).addAspect(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_ASPECT_RESSOURCEINFO), null);
        Mockito.verify(mockedNodeService, times(1)).setProperty(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCETYPE), RessourceInfoExecuter.CCM_RESSOURCETYPE_GEOGEBRA);
        Mockito.verify(mockedNodeService, times(1)).setProperty(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCEVERSION), "5.0.749.0");
        Mockito.reset(mockedNodeService);

        underTest.processGeogebra(new ByteArrayInputStream("some invalid xml".getBytes()), nodeRef);
        Mockito.verify(mockedNodeService, times(0)).addAspect(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_ASPECT_RESSOURCEINFO), null);
    }
    @Test
    void processSerloTest() {
        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString());
        Assertions.assertTrue(underTest.processSerlo(new ByteArrayInputStream("{\"type\":\"https://serlo.org/editor\",\"variant\":\"https://github.com/serlo/serlo-editor-for-edusharing\",\"version\":0,\"dateModified\":\"2024-01-04T08:22:23.941Z\"}".getBytes()), nodeRef));
        Mockito.verify(mockedNodeService, times(1)).addAspect(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_ASPECT_RESSOURCEINFO), null);
        Mockito.verify(mockedNodeService, times(1)).setProperty(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCETYPE), RessourceInfoExecuter.CCM_RESSOURCETYPE_SERLO);
        Mockito.verify(mockedNodeService, times(1)).setProperty(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESOURCESUBTYPE), "https://github.com/serlo/serlo-editor-for-edusharing");
        Mockito.verify(mockedNodeService, times(1)).setProperty(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCEVERSION), "0");
        Mockito.reset(mockedNodeService);

        Assertions.assertTrue(underTest.processSerlo(new ByteArrayInputStream("{\"type\":\"https://serlo.org/editor\"}".getBytes()), nodeRef));
        Mockito.verify(mockedNodeService, times(1)).addAspect(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_ASPECT_RESSOURCEINFO), null);
        Mockito.verify(mockedNodeService, times(1)).setProperty(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCETYPE), RessourceInfoExecuter.CCM_RESSOURCETYPE_SERLO);
        Mockito.verify(mockedNodeService, times(1)).setProperty(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESOURCESUBTYPE), null);
        Mockito.verify(mockedNodeService, times(1)).setProperty(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCEVERSION), null);
        Mockito.reset(mockedNodeService);

        Assertions.assertFalse(underTest.processSerlo(new ByteArrayInputStream("{\"test\":\"https://serlo.org/editor\"}".getBytes()), nodeRef));
        Mockito.verify(mockedNodeService, times(0)).addAspect(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_ASPECT_RESSOURCEINFO), null);
        Mockito.reset(mockedNodeService);

        Assertions.assertFalse(underTest.processSerlo(new ByteArrayInputStream("invalid json".getBytes()), nodeRef));
        Mockito.verify(mockedNodeService, times(0)).addAspect(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_ASPECT_RESSOURCEINFO), null);
        Mockito.reset(mockedNodeService);

    }
}