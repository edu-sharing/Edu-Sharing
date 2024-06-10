package org.edu_sharing.service.feedback;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.codec.digest.DigestUtils;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.authority.AuthorityServiceImpl;
import org.edu_sharing.service.feedback.model.FeedbackData;
import org.edu_sharing.service.feedback.model.FeedbackResult;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.jetbrains.annotations.NotNull;
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

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {
    private FeedbackServiceImpl underTest;

    @Mock
    private NodeService nodeService;
    private String userId;
    private String userEsId;
    private MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic;
    private MockedStatic<NodeServiceHelper> nodeServiceHelperMockedStatic;
    private MockedStatic<AuthorityServiceHelper> authorityServiceHelperMockedStatic;
    private MockedStatic<Context> contextMockedStatic;
    private String sessionId;
    private MockedStatic<AuthorityServiceFactory> authorityServiceFactoryMockedStatic;

    @BeforeEach
    void setUp() {
        underTest = new FeedbackServiceImpl(nodeService);
        underTest.allowMultiple = true;
        userId = "username" + Math.random();
        sessionId = "session" + Math.random();
        userEsId = UUID.randomUUID().toString();
        NodeRef userNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString());
        nodeServiceHelperMockedStatic = Mockito.mockStatic(NodeServiceHelper.class);
        authorityServiceHelperMockedStatic = Mockito.mockStatic(AuthorityServiceHelper.class);
        authorityServiceFactoryMockedStatic = Mockito.mockStatic(AuthorityServiceFactory.class);
        AuthorityServiceImpl authorityServiceMock = Mockito.mock(AuthorityServiceImpl.class);
        authorityServiceFactoryMockedStatic.when(AuthorityServiceFactory::getLocalService).thenReturn(authorityServiceMock);
        Mockito.lenient().when(authorityServiceMock.isGuest()).thenReturn(false);
        authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class);
        contextMockedStatic = Mockito.mockStatic(Context.class);
        Context context = Mockito.mock(Context.class);
        contextMockedStatic.when(Context::getCurrentInstance).thenReturn(context);
        Mockito.lenient().when(Context.getCurrentInstance().getSessionId()).thenReturn(sessionId);
        authorityServiceHelperMockedStatic.when(() -> AuthorityServiceHelper.getAuthorityNodeRef(userId)).thenReturn(userNodeRef);
        authorityServiceHelperMockedStatic.when(() -> AuthorityServiceHelper.getAuthorityNodeRef(userId)).thenReturn(userNodeRef);
        authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(userId);
        authenticationUtilMockedStatic.when(AuthenticationUtil.runAsSystem(any())).thenAnswer(invocation ->
                ((AuthenticationUtil.RunAsWork<?>) invocation.getArgument(0)).doWork()
        );
        authenticationUtilMockedStatic.when(
                (MockedStatic.Verification) AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME)
        ).thenReturn(null);
        nodeServiceHelperMockedStatic.when(() -> NodeServiceHelper.getPropertyNative(userNodeRef, CCConstants.PROP_USER_ESUID)).thenReturn(userEsId);
    }

    @AfterEach
    void teardown() {
        authenticationUtilMockedStatic.close();
        authorityServiceFactoryMockedStatic.close();
        authorityServiceHelperMockedStatic.close();
        nodeServiceHelperMockedStatic.close();
        contextMockedStatic.close();
    }

    @RepeatedTest(value = 5, name = RepeatedTest.LONG_DISPLAY_NAME)
    void getFeedback() throws Throwable {
        String nodeId = UUID.randomUUID().toString();
        Map<String, Map<String, Serializable>> expected = new HashMap<>();
        Map<String, List<String>> expectedData = getSampleData();
        for (int i = 0; i < 100; i++) {
            String childId = UUID.randomUUID().toString();
            expected.put(childId, new HashMap<>() {{
                put(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY, UUID.randomUUID().toString());
                put(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_DATA, new Gson().toJson(expectedData, new TypeToken<HashMap>(){}.getType()));
                put(CCConstants.CM_PROP_C_CREATED, new Date((long) (Math.random() * Long.MAX_VALUE)));
                put(CCConstants.CM_PROP_C_MODIFIED, new Date((long) (Math.random() * Long.MAX_VALUE)));
            }});
        }
        List<ChildAssociationRef> children = expected.keySet().stream().map(
                id -> new ChildAssociationRef(null, null, null,
                        new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, id))
        ).collect(Collectors.toList());
        Mockito.when(nodeService.getChildrenChildAssociationRefType(nodeId, CCConstants.CCM_TYPE_MATERIAL_FEEDBACK)).thenReturn(children);
        Mockito.when(nodeService.getPropertyNative(
                        eq(StoreRef.PROTOCOL_WORKSPACE),
                        eq(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier()),
                        anyString(),
                        anyString()
                )
        ).thenAnswer(
                (invocation) ->
                        expected.get(invocation.getArgument(2)).
                                get(invocation.getArgument(3))
        );

        List<FeedbackData> result = underTest.getFeedback(nodeId);
        assertEquals(expected.size(), result.size());
        List<Map<String, Serializable>> expectedSorted = expected.values().stream().sorted(
                (a, b) -> ((Date) b.get(CCConstants.CM_PROP_C_MODIFIED)).compareTo((Date) a.get(CCConstants.CM_PROP_C_MODIFIED))
        ).collect(Collectors.toList());
        for (int i = 0; i < result.size(); i++) {
            FeedbackData actual = result.get(i);
            Map<String, Serializable> expectedEntry = expectedSorted.get(i);
            assertEquals(expectedEntry.get(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY), actual.getAuthority());
            assertEquals(expectedEntry.get(CCConstants.CM_PROP_C_CREATED), actual.getCreatedAt());
            assertEquals(expectedEntry.get(CCConstants.CM_PROP_C_MODIFIED), actual.getModifiedAt());
            assertEquals(expectedData, actual.getData());
        }
    }

    private Map<String, List<String>> getSampleData() {
        Map<String, List<String>> expectedData = new HashMap<>();
        expectedData.put("key1", new ArrayList<>(Arrays.asList("value1", UUID.randomUUID().toString())));
        expectedData.put("key2", new ArrayList<>(Arrays.asList("value1", "value2", UUID.randomUUID().toString())));
        return expectedData;
    }

    @RepeatedTest(value = 5, name = RepeatedTest.LONG_DISPLAY_NAME)
    void addFeedbackMultiple() {
        String nodeId = UUID.randomUUID().toString();
        String newNodeId = UUID.randomUUID().toString();
        Map<String, List<String>> testData = getSampleData();

        Map<String, Serializable> expectedMap = new HashMap<>() {{
            put(
                    CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY,
                    DigestUtils.sha1Hex(userId + userEsId)
            );
            put(
                    CCConstants.CCM_PROP_MATERIAL_FEEDBACK_DATA,
                    new Gson().toJson(testData, new TypeToken<HashMap>() {
                    }.getType())
            );
        }};

        Mockito.when(nodeService.createNodeBasic(
                        ArgumentMatchers.eq(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE),
                        ArgumentMatchers.eq(nodeId),
                        ArgumentMatchers.eq(CCConstants.CCM_TYPE_MATERIAL_FEEDBACK),
                        ArgumentMatchers.eq(CCConstants.CCM_ASSOC_MATERIAL_FEEDBACK),
                        ArgumentMatchers.eq(expectedMap)
                )
        ).thenReturn(newNodeId);
        underTest.userMode = UserMode.obfuscate;
        assertEquals(new FeedbackResult(newNodeId, false), underTest.addFeedback(nodeId, testData));

        // full mode
        underTest.userMode = UserMode.full;
        expectedMap.put(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY, userId);
        assertEquals(new FeedbackResult(newNodeId, false), underTest.addFeedback(nodeId, testData));

        // session mode
        underTest.userMode = UserMode.session;
        expectedMap.put(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY, DigestUtils.sha1Hex(sessionId + userEsId));
        assertEquals(new FeedbackResult(newNodeId, false), underTest.addFeedback(nodeId, testData));
    }

    @RepeatedTest(value = 5, name = RepeatedTest.LONG_DISPLAY_NAME)
    void addFeedbackSingle() {
        String nodeId = UUID.randomUUID().toString();
        NodeRef updateNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString());
        Map<String, List<String>> testData = getSampleData();

        Map<String, Serializable> expectedMap = new HashMap<>() {{
            put(
                    CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY,
                    DigestUtils.sha1Hex(userId + userEsId)
            );
            put(
                    CCConstants.CCM_PROP_MATERIAL_FEEDBACK_DATA,
                    new Gson().toJson(testData, new TypeToken<HashMap>() {
                    }.getType())
            );
        }};


        // update
        Mockito.when(
                nodeService.getChild(
                        ArgumentMatchers.eq(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE),
                        ArgumentMatchers.eq(nodeId),
                        ArgumentMatchers.eq(CCConstants.CCM_TYPE_MATERIAL_FEEDBACK),
                        ArgumentMatchers.eq(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY),
                        ArgumentMatchers.eq(DigestUtils.sha1Hex(userId + userEsId))
                )).thenReturn(updateNodeRef);

        underTest.userMode = UserMode.obfuscate;
        underTest.allowMultiple = false;
        assertEquals(new FeedbackResult(updateNodeRef.getId(), true), underTest.addFeedback(nodeId, testData));
        Mockito.verify(nodeService, times(1)).updateNodeNative(
                ArgumentMatchers.eq(updateNodeRef.getId()),
                ArgumentMatchers.eq(expectedMap)
        );
    }

    @RepeatedTest(value = 5, name = RepeatedTest.LONG_DISPLAY_NAME)
    void deleteUserData() {
        try (MockedStatic<CMISSearchHelper> cmisSearchHelperMockedStatic = Mockito.mockStatic(CMISSearchHelper.class)) {
            Map<String, Object> filters = new HashMap<>() {{
                put(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY, DigestUtils.sha1Hex(userId + userEsId));
            }};
            List<NodeRef> list = new ArrayList<>();
            int randListSize = (int) (1 + Math.random() * 100);
            for (int i = 0; i < randListSize; i++) {
                list.add(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString()));
            }
            cmisSearchHelperMockedStatic.when(() -> CMISSearchHelper.fetchNodesByTypeAndFilters(eq(CCConstants.CCM_TYPE_MATERIAL_FEEDBACK), eq(filters))).thenReturn(
                    list
            );
            underTest.userMode = UserMode.obfuscate;
            underTest.deleteUserData(userId);
            nodeServiceHelperMockedStatic.verify(() -> NodeServiceHelper.removeNode(any(), eq(false)),
                    times(list.size())
            );


            nodeServiceHelperMockedStatic.reset();
            underTest.userMode = UserMode.session;
            underTest.deleteUserData(userId);
            nodeServiceHelperMockedStatic.verify(
                    () -> NodeServiceHelper.removeNode(any(), eq(false)),
                    times(0)
            );
        }
    }

    @Test
    void changeUserData() {
        try (MockedStatic<CMISSearchHelper> cmisSearchHelperMockedStatic = Mockito.mockStatic(CMISSearchHelper.class)) {
            String newUserId = UUID.randomUUID().toString();
            Map<String, Object> filtersObfuscate = new HashMap<>() {{
                put(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY, DigestUtils.sha1Hex(userId + userEsId));
            }};
            Map<String, Object> filtersFull = new HashMap<>() {{
                put(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY, userId);
            }};
            List<NodeRef> list = new ArrayList<>();
            int randListSize = (int) (1 + Math.random() * 100);
            for (int i = 0; i < randListSize; i++) {
                list.add(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString()));
            }
            cmisSearchHelperMockedStatic.when(() -> CMISSearchHelper.fetchNodesByTypeAndFilters(eq(CCConstants.CCM_TYPE_MATERIAL_FEEDBACK), eq(filtersFull))).thenReturn(
                    list
            );
            underTest.userMode = UserMode.full;
            underTest.changeUserData(userId, newUserId);
            nodeServiceHelperMockedStatic.verify(
                    () -> NodeServiceHelper.setProperty(any(), eq(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY), eq(
                            DigestUtils.sha1Hex(newUserId)
                    ), eq(false)),
                    times(list.size())
            );

            nodeServiceHelperMockedStatic.reset();
            cmisSearchHelperMockedStatic.when(() -> CMISSearchHelper.fetchNodesByTypeAndFilters(eq(CCConstants.CCM_TYPE_MATERIAL_FEEDBACK), eq(filtersObfuscate))).thenReturn(
                    list
            );
            underTest.userMode = UserMode.full;
            underTest.changeUserData(userId, newUserId);
            nodeServiceHelperMockedStatic.verify(
                    () -> NodeServiceHelper.setProperty(any(), eq(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY), eq(
                            DigestUtils.sha1Hex(newUserId)
                    ), eq(false)),
                    times(list.size())
            );

            nodeServiceHelperMockedStatic.reset();
            underTest.userMode = UserMode.session;
            underTest.changeUserData(userId, newUserId);
            nodeServiceHelperMockedStatic.verify(
                    () -> NodeServiceHelper.setProperty(any(), eq(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY), anyString(), eq(false)),
                    times(0)
            );

        }
    }
}
