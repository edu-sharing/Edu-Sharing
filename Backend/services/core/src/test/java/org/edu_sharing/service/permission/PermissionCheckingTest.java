package org.edu_sharing.service.permission;

import com.google.common.collect.Lists;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfresco.policy.GuestCagePolicy;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.permission.annotation.NodePermission;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.*;

import static org.mockito.ArgumentMatchers.eq;


@ExtendWith(MockitoExtension.class)
class PermissionCheckingTest {
    @Mock
    AuthorityService authorityService;

    @Mock
    PermissionService permissionService;

    @Mock
    ToolPermissionService toolPermissionService;

    @Test
    void singleNode_SuccessPermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        String nodeA = "1";
        NodeRef nodeB = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "2");
        List<String> nodePermissions = Arrays.asList("someNodePermission", "someMoreNodePermission", "otherNodePermission");
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        Mockito.when(permissionService.getPermissionsForAuthority(eq(nodeA), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissions);
        Mockito.when(permissionService.getPermissionsForAuthority(eq(nodeB.getId()), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissions);


        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();
        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            proxy.permissionTestMethod(nodeA,null, nodeB);
        }
    }

    @Test
    void multiNode_Array_SuccessPermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        Object[] nodes = {
                "1",
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"2"),
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"3")};
        List<String> nodePermissions = Arrays.asList("someNodePermission", "someMoreNodePermission", "otherNodePermission");
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        for (Object node : nodes) {
            if(node instanceof String) {
                Mockito.when(permissionService.getPermissionsForAuthority(eq((String) node), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissions);
            }else {
                Mockito.when(permissionService.getPermissionsForAuthority(eq(((NodeRef)node).getId()), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissions);
            }
        }

        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();
        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            proxy.permissionTestMethod(nodes);
        }
    }

    @Test
    void multiNode_List_SuccessPermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        List<Object> nodes =Arrays.asList("1", new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"2"), new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"3"));
        List<String> nodePermissions = Arrays.asList("someNodePermission", "someMoreNodePermission", "otherNodePermission");
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        for (Object node : nodes) {
            if(node instanceof String) {
                Mockito.when(permissionService.getPermissionsForAuthority(eq((String)node), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissions);
            }else {
                Mockito.when(permissionService.getPermissionsForAuthority(eq(((NodeRef)node).getId()), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissions);
            }
        }

        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();
        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            proxy.permissionTestMethod(nodes);
        }
    }

    @Test
    void invalidArgumentException() {
        // given
        String authority = "Muster";
        Object nodeA = 102316;
        NodeRef nodeB = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"2");
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);


        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InvalidArgumentException.class , () -> proxy.permissionTestMethod(nodeA, null, nodeB));
        }
    }

    @Test
    void failedIsGuestTest() {
        // given
        String authority = "Muster";
        String nodeA = "1";
        NodeRef nodeB = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"2");

        Mockito.when(authorityService.isGuest()).thenReturn(true);

        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();


        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(GuestCagePolicy.GuestPermissionDeniedException.class , () -> proxy.permissionTestMethod(nodeA, null, nodeB));
        }
    }

    @Test
    void failedToolPermissionTest() {
        // given
        String authority = "Muster";
        String nodeA = "1";
        NodeRef nodeB = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"2");
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(false);


        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();


        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class , () -> proxy.permissionTestMethod(nodeA, null, nodeB));
        }
    }
    @Test
    void singleNodeRef_failedNodePermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        NodeRefImpl nodeA = new NodeRefImpl("1");
        nodeA.setPermissions(new HashMap<>(){{
            put("someNodePermission", false);
        }});

        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class , () -> proxy.nodeTestMethod(nodeA));
        }
    }

    @Test
    void singleNodeRef_successNodePermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        NodeRefImpl nodeA = new NodeRefImpl("1");
        nodeA.setPermissions(new HashMap<>(){{
            put("someNodePermission", true);
        }});

        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            proxy.nodeTestMethod(nodeA);
        }
    }
    @Test
    void singleNodeA_failedNodePermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        String nodeA = "1";
        NodeRef nodeB = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"2");
        List<String> nodePermissionsNodeA = Collections.singletonList("otherNodePermission");
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        Mockito.when(permissionService.getPermissionsForAuthority(eq(nodeA), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissionsNodeA);


        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class , () -> proxy.permissionTestMethod(nodeA, null, nodeB));
        }
    }

    @Test
    void singleNodeB_failedNodePermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        String nodeA = "1";
        NodeRef nodeB = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"2");
        List<String> nodePermissionsNodeA = Arrays.asList("someNodePermission", "someMoreNodePermission", "otherNodePermission");
        List<String> nodePermissionsNodeB = new ArrayList<>();
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        Mockito.when(permissionService.getPermissionsForAuthority(eq(nodeA), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissionsNodeA);
        Mockito.when(permissionService.getPermissionsForAuthority(eq(nodeB.getId()), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissionsNodeB);


        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class, () -> proxy.permissionTestMethod(nodeA, null, nodeB));
        }
    }

    @Test
    void multiNode1_List_failedNodePermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        List<Object> nodes =Arrays.asList("1",
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"2"),
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"3"));
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        Mockito.when(permissionService.getPermissionsForAuthority(eq(((String) nodes.get(0))), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(new ArrayList<>());

        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class, () -> proxy.permissionTestMethod(nodes));
        }
    }

    @Test
    void multiNode2_List_failedNodePermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        List<Object> nodes =Arrays.asList("1",
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"2"),
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"3"));
        List<String> nodePermissions = Arrays.asList("someNodePermission", "someMoreNodePermission", "otherNodePermission");
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        Mockito.when(permissionService.getPermissionsForAuthority(eq((String) nodes.get(0)), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissions);
        Mockito.when(permissionService.getPermissionsForAuthority(eq(((NodeRef) nodes.get(1)).getId()), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(new ArrayList<>());

        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class, () -> proxy.permissionTestMethod(nodes));
        }
    }

    @Test
    void multiNode1_Array_failedNodePermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        Object[] nodes = {
                "1",
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "2"),
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "3")
        };

        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        Mockito.when(permissionService.getPermissionsForAuthority(eq(((String) nodes[0])), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(new ArrayList<>());

        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class, () -> proxy.permissionTestMethod(nodes));
        }
    }

    @Test
    void multiNode2_StringList_failedNodePermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        Object[] nodes = {
                "1",
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"2"),
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,"3")
        };
        List<String> nodePermissions = Arrays.asList("someNodePermission", "someMoreNodePermission", "otherNodePermission");
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        Mockito.when(permissionService.getPermissionsForAuthority(eq((String) nodes[0]), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(nodePermissions);
        Mockito.when(permissionService.getPermissionsForAuthority(eq(((NodeRef) nodes[1]).getId()), eq(authority), ArgumentMatchers.anyCollection())).thenReturn(new ArrayList<>());

        TestClass target = new TestClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        TestClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class, () -> proxy.permissionTestMethod(nodes));
        }
    }
}

class TestClass {
    @Permission()
    public void nodeTestMethod(
            @NodePermission({"someNodePermission"}) Object node
    )
            throws InsufficientPermissionException {
        System.out.println("MyPermissionTest");
    }
    @Permission(requiresUser = true, value = {"someToolPermission"})
    public void permissionTestMethod(
            @NodePermission({"someNodePermission","otherNodePermission"}) Object nodeA,
            Object nodeB,
            @NodePermission({"someMoreNodePermission"}) Object nodeC)
            throws InsufficientPermissionException {
        System.out.println("MyPermissionTest");
    }

    @Permission(requiresUser = true, value = {"someToolPermission"})
    public void permissionTestMethod(
            @NodePermission({"someNodePermission","otherNodePermission"}) Object[] node)
            throws InsufficientPermissionException {
        System.out.println("MyPermissionTest");
    }

    @Permission(requiresUser = true, value = {"someToolPermission"})
    public void permissionTestMethod(
            @NodePermission({"someNodePermission","otherNodePermission"}) List<Object> nodes)
            throws InsufficientPermissionException {
        System.out.println("MyPermissionTest");
    }
}
