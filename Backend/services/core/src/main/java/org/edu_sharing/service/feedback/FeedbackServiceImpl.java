package org.edu_sharing.service.feedback;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.typesafe.config.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.feedback.model.FeedbackData;
import org.edu_sharing.service.feedback.model.FeedbackResult;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.annotation.NodeManipulation;
import org.edu_sharing.service.nodeservice.annotation.NodeOriginal;
import org.edu_sharing.service.permission.annotation.NodePermission;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import java.util.*;
import java.util.stream.Collectors;

@Log4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FeedbackServiceImpl implements FeedbackService, ApplicationListener<RefreshScopeRefreshedEvent> {
    private final NodeService nodeService;
    UserMode userMode;
    boolean allowMultiple;

    @Override
    @Permission({CCConstants.CCM_VALUE_TOOLPERMISSION_MATERIAL_FEEDBACK})
    @NodeManipulation
    public List<FeedbackData> getFeedback(
            @NotNull
            @NodeOriginal
            @NodePermission({CCConstants.PERMISSION_FEEDBACK, CCConstants.PERMISSION_COORDINATOR})
            String nodeId
    ) throws InsufficientPermissionException {
        Objects.requireNonNull(nodeId);
        return AuthenticationUtil.runAsSystem(() ->
                nodeService.getChildrenChildAssociationRefType(
                                nodeId,
                                CCConstants.CCM_TYPE_MATERIAL_FEEDBACK
                        ).
                        stream().
                        map((ref) -> {
                            try {
                                return toFeedback(ref.getChildRef());
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }).sorted((a, b) ->
                                b.getModifiedAt().compareTo(a.getModifiedAt())
                        ).collect(Collectors.toList())
        );

    }

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        refresh();
    }

    @Override
    public void refresh() {
        Config config = LightbendConfigLoader.get().getConfig("repository.feedback");
        userMode = config.getEnum(UserMode.class, "userMode");
        allowMultiple = config.getBoolean("allowMultiple");
        if (!allowMultiple && !Arrays.asList(UserMode.full, UserMode.obfuscate).contains(userMode)) {
            throw new IllegalArgumentException("Mode allowMultiple == false is not supported in current userMode");
        }
    }

    private FeedbackData toFeedback(NodeRef nodeRef) throws Throwable {
        FeedbackData feedbackData = new FeedbackData();
        feedbackData.setAuthority((String) nodeService.getPropertyNative(
                nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(),
                CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY
        ));
        feedbackData.setCreatedAt((Date) nodeService.getPropertyNative(
                nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(),
                CCConstants.CM_PROP_C_CREATED
        ));
        feedbackData.setModifiedAt((Date) nodeService.getPropertyNative(
                nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(),
                CCConstants.CM_PROP_C_MODIFIED
        ));
        String data = (String) nodeService.getPropertyNative(
                nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(),
                CCConstants.CCM_PROP_MATERIAL_FEEDBACK_DATA
        );
        Map<String, List<String>> mapData = new Gson().fromJson(data, new TypeToken<Map<String,Object>>() {
        }.getType());
        feedbackData.setData(mapData);
        return feedbackData;
    }

    @Override
    @Permission({CCConstants.CCM_VALUE_TOOLPERMISSION_MATERIAL_FEEDBACK})
    @NodeManipulation
    public FeedbackResult addFeedback(
            @NotNull
            @NodeOriginal
            @NodePermission({CCConstants.PERMISSION_FEEDBACK})
            String nodeId,
            Map<String, List<String>> feedbackData
    ) {
        String userId = AuthenticationUtil.getFullyAuthenticatedUser();
        if (AuthorityServiceFactory.getLocalService().isGuest() && !userMode.equals(UserMode.session)) {
            throw new IllegalArgumentException("Guest feedback is only supported when userMode == session");
        }
        return AuthenticationUtil.runAsSystem(() -> {
            try {
                // will reset after runAs automatically
                AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);
                Map<String, Object> props = new HashMap<>();
                props.put(
                        CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY,
                        getHashedAuthority(userId)
                );
                props.put(
                        CCConstants.CCM_PROP_MATERIAL_FEEDBACK_DATA,
                        new Gson().toJson(feedbackData, new TypeToken<HashMap>() {
                        }.getType())
                );
                NodeRef existing = null;
                if (!allowMultiple) {
                    existing = nodeService.getChild(
                            StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
                            nodeId,
                            CCConstants.CCM_TYPE_MATERIAL_FEEDBACK,
                            CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY,
                            getHashedAuthority(userId)
                    );
                }
                if (existing != null) {
                    nodeService.updateNodeNative(existing.getId(), props);
                    return new FeedbackResult(
                            existing.getId(),
                            true
                    );
                } else {
                    return new FeedbackResult(nodeService.createNodeBasic(
                            StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
                            nodeId,
                            CCConstants.CCM_TYPE_MATERIAL_FEEDBACK, CCConstants.CCM_ASSOC_MATERIAL_FEEDBACK,
                            props),
                            false
                    );
                }
            } catch (Throwable t) {
                log.warn(t.getMessage(), t);
                throw t;
            }
        });
    }

    @Override
    public void deleteUserData(String userName) {
        if (Arrays.asList(UserMode.full, UserMode.obfuscate).contains(userMode)) {
            List<NodeRef> nodes = getUsersFeedback(userName);
            nodes.forEach(n -> NodeServiceHelper.removeNode(n, false));
        }
    }

    @Override
    public void changeUserData(String userName, String deletedName) {
        if (Arrays.asList(UserMode.full, UserMode.obfuscate).contains(userMode)) {
            List<NodeRef> nodes = getUsersFeedback(userName);
            nodes.forEach(n ->
                    NodeServiceHelper.setProperty(n, CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY, DigestUtils.sha1Hex((deletedName)), false)
            );
        }
    }

    @NotNull
    private List<NodeRef> getUsersFeedback(String userName) {
        Map<String, Object> filters = new HashMap<>();
        filters.put(CCConstants.CCM_PROP_MATERIAL_FEEDBACK_AUTHORITY, getHashedAuthority(userName));
        return CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_MATERIAL_FEEDBACK, filters);
    }

    private String getHashedAuthority(String authorityName) {
        String esuid = (String) NodeServiceHelper.getPropertyNative(AuthorityServiceHelper.getAuthorityNodeRef(authorityName), CCConstants.PROP_USER_ESUID);
        if (userMode.equals(UserMode.obfuscate)) {
            return DigestUtils.sha1Hex(authorityName + esuid);
        } else if (userMode.equals(UserMode.full)) {
            return authorityName;
        } else if (userMode.equals(UserMode.session)) {
            return DigestUtils.sha1Hex(Context.getCurrentInstance().getSessionId() + esuid);
        } else if (userMode.equals(UserMode.external)) {
            throw new NotImplementedException("TODO");
        } else {
            throw new IllegalArgumentException("Invalid userMode");
        }
    }

}
