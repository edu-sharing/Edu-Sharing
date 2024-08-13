package org.edu_sharing.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.rest.notification.data.StatusDTO;
import org.edu_sharing.rest.notification.event.NotificationEventDTO;
import org.edu_sharing.restservices.mds.v1.model.MdsValue;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.rating.RatingDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface NotificationService {
    @Getter
    @Setter
    @AllArgsConstructor
    class NodeContext {
        private String nodeId;
        private List<String> aspects;
        private Map<String, Object> properties;
    }
    interface NodeIssueMapping {
        String getTemplateId(NodeContext context);
        List<String> getReceivers(NodeContext context);
    }

    void notifyNodeIssue(String nodeId, String reason, String nodeType, List<String> aspects, Map<String, Object> properties, String userEmail, String userComment) throws Throwable;

    void notifyWorkflowChanged(String nodeId, String nodeType, List<String> aspects, Map<String, Object> nodeProperties, String receiver, String comment, String status);

    void notifyPersonStatusChanged(String receiver, String firstname, String lastName, String oldStatus, String newStatus);

    void notifyPermissionChanged(String senderAuthority, String receiverAuthority, String nodeId, String nodeType, List<String> aspects, Map<String, Object> props, String[] permissions, String mailText) throws Throwable;

    void notifyMetadataSetSuggestion(MdsValue mdsValue, MetadataWidget widgetDefinition, List<String> nodeIds, List<String> nodeType, List<List<String>> aspects, List<Map<String, Object>> nodeProperties) throws Throwable;

    void notifyComment(String node, String comment, String commentReference, String nodeType, List<String> aspects, Map<String, Object> nodeProperties, Status status);

    void notifyAddCollection(String collectionId, String nodeId, String collectionNodeType, List<String> collectionAspects, Map<String, Object> collectionProperties, String nodeType, List<String> nodeAspects, Map<String, Object> nodeProperties, Status status);
    void notifyProposeForCollection(String collectionId, String nodeId, String collectionNodeType, List<String> collectionAspects, Map<String, Object> collectionProperties, String nodeType, List<String> nodeAspects, Map<String, Object> nodeProperties, Status status);

    void notifyRatingChanged(String nodeId, String nodeType, List<String> aspects, Map<String, Object> nodeProps, Double rating, RatingDetails accumulatedRatings, Status removed);

    @Permission(requiresUser = true)
    default NotificationConfig getConfig() throws Exception {
        Map<String, String> info = new MCAlfrescoAPIClient().getUserInfo(AuthenticationUtil.getFullyAuthenticatedUser());
        if (!StringUtils.isEmpty(info.get(CCConstants.CCM_PROP_PERSON_NOTIFICATION_PREFERENCES))) {
            return new ObjectMapper().readValue(info.get(CCConstants.CCM_PROP_PERSON_NOTIFICATION_PREFERENCES), NotificationConfig.class);
        }
        return new NotificationConfig();
    }

    @Permission(requiresUser = true)
    default void setConfig(NotificationConfig config) throws Exception {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put(CCConstants.PROP_USERNAME, AuthenticationUtil.getFullyAuthenticatedUser());
        userInfo.put(CCConstants.CCM_PROP_PERSON_NOTIFICATION_PREFERENCES, new ObjectMapper().writeValueAsString(config));
        new MCAlfrescoAPIClient().updateUser(userInfo);
    }

    @Permission(requiresUser = true)
    Page<NotificationEventDTO> getNotifications(String receiverId, List<StatusDTO> status, Pageable pageable) throws IOException, InsufficientPermissionException;

    @Permission(requiresUser = true)
    NotificationEventDTO setNotificationStatusByNotificationId(String id, StatusDTO status) throws IOException, InsufficientPermissionException;

    @Permission(requiresUser = true)
    void setNotificationStatusByReceiverId(String receiverId, List<StatusDTO> oldStatusList, StatusDTO newStatus) throws IOException, InsufficientPermissionException;

    @Permission(requiresUser = true)
    void deleteNotification(String id) throws IOException, InsufficientPermissionException;

}
