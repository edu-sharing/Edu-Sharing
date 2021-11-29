package org.edu_sharing.service.tracking;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.service.tracking.ibatis.NodeData;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TrackingService {



    enum GroupingType {
        None,
        Daily,
        Monthly,
        Yearly,
        Node,
    }
    enum EventType {
        DOWNLOAD_MATERIAL,
        VIEW_MATERIAL,
        VIEW_MATERIAL_EMBEDDED,
        VIEW_MATERIAL_PLAY_MEDIA, // When a video or audio file is actually started playing
        LOGIN_USER_SESSION,
        LOGIN_USER_OAUTH_PASSWORD,
        LOGIN_USER_OAUTH_REFRESH_TOKEN,
        LOGOUT_USER_TIMEOUT,
        LOGOUT_USER_REGULAR
    }
    List<String> getAlteredNodes(java.util.Date from);
    List<NodeData> getNodeData(String nodeId, java.util.Date from);
    boolean trackActivityOnUser(String authorityName,EventType type);
    boolean trackActivityOnNode(NodeRef nodeRef,NodeTrackingDetails details,EventType type);
    List<StatisticEntryNode> getNodeStatisics(GroupingType type, Date dateFrom, Date dateTo, String mediacenter, StatisticsFetchConfig fetchConfig) throws Throwable;
    List<StatisticEntry> getUserStatistics(GroupingType type, java.util.Date dateFrom, java.util.Date dateTo, String mediacenter, StatisticsFetchConfig fetchConfig) throws Throwable;
    StatisticEntry getSingleNodeData(NodeRef nodeRef,java.util.Date dateFrom,java.util.Date dateTo) throws Throwable;

    /**
     * delete all tracked data for a given user
     * This method can do nothing, depending on the configured @UserTrackingMode
     * It will only do something if the mode is NOT set to "none"
     */
    void deleteUserData(String username) throws Throwable;
    /**
     * reassign all tracked data for a given user to a new user (usually a dummy)
     * This method can do nothing, depending on the configured @UserTrackingMode
     * It will only do something if the mode is NOT set to "none"
     */
    void reassignUserData(String oldUsername, String newUsername);

    enum UserTrackingMode{
        none,
        obfuscate,
        session,
        full
    }


    public static class StatisticsFetchConfig {
        private List<String> additionalFields,groupFields;
        private Map<String, String> filters;
        private boolean fetchAll;

        public StatisticsFetchConfig(List<String> additionalFields, List<String> groupFields, Map<String, String> filters) {
            this.additionalFields = additionalFields;
            this.groupFields = groupFields;
            this.filters = filters;
        }

        public StatisticsFetchConfig(List<String> additionalFields, List<String> groupFields, Map<String, String> filters, boolean fetchAll) {
            this.additionalFields = additionalFields;
            this.groupFields = groupFields;
            this.filters = filters;
            this.fetchAll = fetchAll;
        }

        public List<String> getAdditionalFields() {
            return additionalFields;
        }

        public void setAdditionalFields(List<String> additionalFields) {
            this.additionalFields = additionalFields;
        }

        public List<String> getGroupFields() {
            return groupFields;
        }

        public void setGroupFields(List<String> groupFields) {
            this.groupFields = groupFields;
        }

        public Map<String, String> getFilters() {
            return filters;
        }

        public void setFilters(Map<String, String> filters) {
            this.filters = filters;
        }

        public boolean isFetchAll() {
            return fetchAll;
        }

        public void setFetchAll(boolean fetchAll) {
            this.fetchAll = fetchAll;
        }
    }
}
