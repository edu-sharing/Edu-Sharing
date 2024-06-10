package org.edu_sharing.service.tracking;

import jakarta.servlet.http.HttpSession;
import lombok.Value;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.GuestCagePolicy;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.service.mediacenter.MediacenterService;
import org.edu_sharing.service.mediacenter.MediacenterServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.tracking.ibatis.EduTrackingMapper;
import org.edu_sharing.service.tracking.ibatis.NodeData;
import org.edu_sharing.service.tracking.ibatis.NodeResult;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.postgresql.util.PGobject;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TrackingServiceImpl extends TrackingServiceDefault {
    private static final Map<String, FieldDescription> EXISTING_FIELDS = Stream.of(
            new FieldDescription("node_id", false, false),
            new FieldDescription("node_uuid", false, false),
            new FieldDescription("authority", false, false),
            new FieldDescription("time", false, false),
            new FieldDescription("type", false, false),
            new FieldDescription("data", false, true),
            new FieldDescription("node_version", false, false),
            new FieldDescription("authority_organization", true, false),
            new FieldDescription("authority_mediacenter", true, false),
            new FieldDescription("original_node_uuid", false, false),
            new FieldDescription("license", false, false),
            new FieldDescription("shared_with_mediacenters", true, false)
    ).collect(Collectors.toMap(FieldDescription::getName, x -> x));


    @Value
    private static class FieldDescription {
        String name;
        boolean array;
        boolean json;
    }

    private static final String SESSION_AUTHORITY_MEDIACENTERS = "SESSION_AUTHORITY_MEDIACENTERS";
    private static final String SESSION_AUTHORITY_ORGANIZATIONS = "SESSION_AUTHORITY_ORGANIZATIONS";
    public static Logger logger = Logger.getLogger(TrackingServiceImpl.class);

    public static String TRACKING_NODE_TABLE_ID = "edu_tracking_node";
    public static String TRACKING_USER_TABLE_ID = "edu_tracking_user";

    public static String TRACKING_DELETE_NODE = "DELETE FROM " + TRACKING_NODE_TABLE_ID + " WHERE authority = ?";
    public static String TRACKING_DELETE_USER = "DELETE FROM " + TRACKING_USER_TABLE_ID + " WHERE authority = ?";

    public static String TRACKING_UPDATE_NODE = "UPDATE " + TRACKING_NODE_TABLE_ID + " SET authority = ? WHERE authority = ?";
    public static String TRACKING_UPDATE_USER = "UPDATE " + TRACKING_USER_TABLE_ID + " SET authority = ? WHERE authority = ?";

    public static String TRACKING_INSERT_NODE = "insert into " + TRACKING_NODE_TABLE_ID + " (node_id,node_uuid,original_node_uuid,node_version,authority,authority_organization,authority_mediacenter,time,type,data,license, shared_with_mediacenters) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    public static String TRACKING_INSERT_USER = "insert into " + TRACKING_USER_TABLE_ID + " (authority,authority_organization,authority_mediacenter,time,type,data) VALUES (?,?,?,?,?,?)";
    public static String TRACKING_STATISTICS_CUSTOM_GROUPING = "SELECT type,COUNT(*) :fields from :table as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type :grouping" +
            " ORDER BY count DESC";
    public static String TRACKING_STATISTICS_USER = "SELECT authority,authority_organization,authority_mediacenter,time as date,type,COUNT(*) :fields from edu_tracking_user as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY authority,authority_organization,authority_mediacenter,time,type :grouping" +
            " ORDER BY date" +
            " LIMIT 100";
    public static String TRACKING_STATISTICS_NODE = "SELECT node_uuid as node,authority,authority_organization,authority_mediacenter,time as date,type,COUNT(*) :fields from edu_tracking_node as tracking" +
            //" LEFT JOIN alf_node_properties as props ON (tracking.node_id=props.node_id and props.qname_id=28)" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY node,authority,authority_organization,authority_mediacenter,time,type :grouping" +
            " ORDER BY date" +
            " LIMIT 100";
    public static String TRACKING_STATISTICS_NODE_GROUPED = "SELECT COALESCE(original_node_uuid, node_uuid) as node,type,COUNT(*) :fields from edu_tracking_node as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY node,type :grouping" +
            " ORDER BY count DESC";
    public static String TRACKING_STATISTICS_NODE_SINGLE = "SELECT type,COUNT(*) from edu_tracking_node as tracking" +
            " WHERE node_uuid = ? AND time BETWEEN ? AND ?" +
            " GROUP BY type" +
            " ORDER BY count DESC";

    public static String TRACKING_STATISTICS_NODE_ARRAY = "SELECT COALESCE(original_node_uuid, node_uuid) as node_uuid_final, type,COUNT(*) :fields from edu_tracking_node as tracking" +
            " WHERE time BETWEEN ? AND ? AND (ARRAY[?] <@ authority_mediacenter)" +
            " GROUP BY node_uuid_final, type" +
            " HAVING COALESCE(original_node_uuid, node_uuid) = ANY(?) " +
            " ORDER BY count DESC";

    public static String TRACKING_STATISTICS_NODE_MEDIACENTER = "SELECT COALESCE(original_node_uuid, node_uuid) as node_uuid_final, type,COUNT(*) :fields from edu_tracking_node as tracking" +
            " WHERE (ARRAY[?] <@ authority_mediacenter) AND time BETWEEN ? AND ? AND ARRAY_LENGTH(authority_mediacenter, 1) = 1" +
            " GROUP BY node_uuid_final, type" +
            " ORDER BY count DESC";
    public static String TRACKING_STATISTICS_DAILY = "SELECT type,COUNT(*),TO_CHAR(time,'yyyy-mm-dd') as date :fields from :table as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type,date :grouping" +
            " ORDER BY date";
    public static String TRACKING_STATISTICS_MONTHLY = "SELECT type,COUNT(*),TO_CHAR(time,'yyyy-mm') as date :fields from :table as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type,date :grouping" +
            " ORDER BY date";
    public static String TRACKING_STATISTICS_YEARLY = "SELECT type,COUNT(*),TO_CHAR(time,'yyyy') as date :fields from :table as tracking" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type,date :grouping" +
            " ORDER BY date";
    private final TrackingServiceCustomInterface customTrackingService;

    public TrackingServiceImpl(TrackingServiceFactory trackingServiceFactory, TransactionService transactionService, @Qualifier("policyBehaviourFilter") BehaviourFilter policyBehaviourFilter) {
        super(transactionService, policyBehaviourFilter);
        customTrackingService = trackingServiceFactory.getTrackingServiceCustom();
        try {
            new ConnectionDBAlfresco().getSqlSessionFactoryBean().getConfiguration().addMapper(EduTrackingMapper.class);
        } catch (BindingException ignored) {
        }
    }

    @Override
    public List<String> getAlteredNodes(java.util.Date from) {
        try (SqlSession session = new ConnectionDBAlfresco().getSqlSessionFactoryBean().openSession()) {
            return session.getMapper(EduTrackingMapper.class).eduAlteredNodes(from).stream().
                    map(NodeResult::getNodeid).collect(Collectors.toList());
        }
    }

    @Override
    public List<NodeData> getNodeData(String nodeId, java.util.Date from) {
        try (SqlSession session = new ConnectionDBAlfresco().getSqlSessionFactoryBean().openSession()) {
            return session.getMapper(EduTrackingMapper.class).
                    eduNodeData(nodeId, "YYYY-MM-DD", from);
        }
    }

    @Override
    public boolean trackActivityOnUser(String authorityName, EventType type) {
        super.trackActivityOnUser(authorityName, type);
        if (authorityName == null
                || GuestCagePolicy.getGuestUsers().contains(authorityName)
                || authorityName.equals(AuthenticationUtil.getSystemUserName())) {
            return false;
        }
        return AuthenticationUtil.runAs(() -> execDatabaseQuery(TRACKING_INSERT_USER, statement -> {
            statement.setString(1, super.getTrackedUsername(authorityName));
            try {
                statement.setArray(2, statement.getConnection().createArrayOf("VARCHAR", getAuthorityOrganizations()));
            } catch (Exception e) {
                statement.setArray(2, null);
                logger.info("Failed to track organizations of user", e);
            }
            try {
                statement.setArray(3, statement.getConnection().createArrayOf("VARCHAR", getAuthorityMediacenters()));
            } catch (Exception e) {
                statement.setArray(3, null);
                logger.info("Failed to track mediacenter of user", e);
            }
            statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            statement.setString(5, type.name());
            JSONObject json = buildJson(authorityName, type);
            PGobject obj = new PGobject();
            obj.setType("json");
            if (json != null)
                obj.setValue(json.toString());
            statement.setObject(6, obj);

            return true;
        }), authorityName);

    }

    @Override
    public boolean trackActivityOnNode(NodeRef nodeRef, NodeTrackingDetails details, EventType type, String authorityName) {
        super.trackActivityOnNode(nodeRef, details, type, authorityName);

        String version;
        String nodeVersion = details == null ? null : details.getNodeVersion();
        if (nodeVersion == null || nodeVersion.isEmpty() || nodeVersion.equals("-1")) {
            version = NodeServiceHelper.getProperty(nodeRef, CCConstants.CM_PROP_VERSIONABLELABEL);
        } else {
            version = nodeVersion;
        }
        String originalNodeRef = null;
        try {
            if (NodeServiceHelper.hasAspect(nodeRef, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
                originalNodeRef = NodeServiceHelper.getProperty(nodeRef, CCConstants.CCM_PROP_IO_ORIGINAL);
            } else if (NodeServiceHelper.hasAspect(nodeRef, CCConstants.CCM_ASPECT_PUBLISHED)) {
                originalNodeRef = ((NodeRef) NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL)).getId();
            }
        } catch (Throwable ignored) {
        }
        String finalOriginalNodeRef = originalNodeRef;
        return execDatabaseQuery(TRACKING_INSERT_NODE, statement -> {
            statement.setLong(1, (Long) NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.SYS_PROP_NODE_DBID));
            statement.setString(2, nodeRef.getId());
            statement.setString(3, finalOriginalNodeRef);
            statement.setString(4, version);
            statement.setString(5, super.getTrackedUsername(authorityName));
            try {
                statement.setArray(6, statement.getConnection().createArrayOf("VARCHAR", getAuthorityOrganizations()));
            } catch (Exception e) {
                logger.info("Failed to track organizations of user", e);
            }
            try {
                statement.setArray(7, statement.getConnection().createArrayOf("VARCHAR", getAuthorityMediacenters()));
            } catch (Exception e) {
                logger.info("Failed to track mediacenter of user", e);
            }
            statement.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            statement.setString(9, type.name());
            JSONObject json = buildJson(nodeRef, details, type);
            PGobject obj = new PGobject();
            obj.setType("json");
            if (json != null)
                obj.setValue(json.toString());
            statement.setObject(10, obj);

            String license = NodeServiceHelper.getProperty(nodeRef, CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
            statement.setString(11, license);

            if (LightbendConfigLoader.get().getBoolean("repository.tracking.sharedWithMediacenter")) {
                MediacenterService mediacenterService = MediacenterServiceFactory.getLocalService();
                statement.setArray(12, statement.getConnection().createArrayOf("VARCHAR", mediacenterService.getMediacenterAuthoritiesByNode(nodeRef.getId()).toArray()));
            } else {
                statement.setArray(12, null);
            }

            return true;
        });
    }

    private static HttpSession getSession() {
        if (Context.getCurrentInstance() != null && Context.getCurrentInstance().getRequest() != null) {
            return Context.getCurrentInstance().getRequest().getSession();
        }
        return null;
    }

    @NotNull
    private static Object[] getAuthorityMediacenters() throws Exception {
        if (ContextManagementFilter.accessTool.get() == null || ContextManagementFilter.accessTool.get().getUserId() == null) {
            // use the fully authenticated user since the current runAs user might be system
            HttpSession session = getSession();
            Object[] result;
            if (session != null) {
                result = (Object[]) session.getAttribute(SESSION_AUTHORITY_MEDIACENTERS);
                if (result != null) {
                    return result;
                }
            }
            result = AuthenticationUtil.runAs(
                    () -> SearchServiceFactory.getLocalService().getAllMediacenters(true).toArray(),
                    AuthenticationUtil.getFullyAuthenticatedUser()
            );
            if (session != null) {
                session.setAttribute(SESSION_AUTHORITY_MEDIACENTERS, result);
            }
            return result;
        } else {
            return AuthenticationUtil.runAs(
                    () -> SearchServiceFactory.getLocalService().getAllMediacenters(true).toArray(),
                    ContextManagementFilter.accessTool.get().getUserId()
            );
        }
    }

    @NotNull
    private static Object[] getAuthorityOrganizations() throws Exception {
        if (ContextManagementFilter.accessTool.get() == null || ContextManagementFilter.accessTool.get().getUserId() == null) {
            // use the fully authenticated user since the current runAs user might be system
            HttpSession session = getSession();
            Object[] result;
            if (session != null) {
                result = (Object[]) session.getAttribute(SESSION_AUTHORITY_ORGANIZATIONS);
                if (result != null) {
                    return result;
                }
            }
            result = AuthenticationUtil.runAs(
                    () -> SearchServiceFactory.getLocalService().getAllOrganizations(true).getData().stream().map(EduGroup::getGroupname).toArray(),
                    AuthenticationUtil.getFullyAuthenticatedUser()
            );
            if (session != null) {
                session.setAttribute(SESSION_AUTHORITY_ORGANIZATIONS, result);
            }
            return result;
        } else {
            return AuthenticationUtil.runAs(
                    () -> SearchServiceFactory.getLocalService().getAllOrganizations(true).getData().stream().map(EduGroup::getGroupname).toArray(),
                    ContextManagementFilter.accessTool.get().getUserId()
            );
        }
    }

    /**
     * overwrite this in a custom method to track additional data
     */
    protected JSONObject buildJson(NodeRef nodeRef, NodeTrackingDetails details, EventType type) {
        if (customTrackingService != null) {
            return customTrackingService.buildJson(nodeRef, details, type);
        }
        return null;
    }

    /**
     * overwrite this in a custom method to track additional data
     */
    protected JSONObject buildJson(String authorityName, EventType type) {
        if (customTrackingService != null) {
            return customTrackingService.buildJson(authorityName, type);
        }
        return null;
    }

    @Override
    public List<StatisticEntry> getUserStatistics(GroupingType type, java.util.Date dateFrom, java.util.Date dateTo, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws Throwable {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            List<StatisticEntry> result = initList(StatisticEntry.class, type, dateFrom, dateTo);
            String query = getQuery(type, "edu_tracking_user", mediacenter, additionalFields, groupFields, filters);
            statement = con.prepareStatement(query);
            int index = 1;
            statement.setTimestamp(index++, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            statement.setTimestamp(index++, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            if (mediacenter != null && !mediacenter.isEmpty()) {
                statement.setString(index++, mediacenter);
            }
            if (filters != null && !filters.isEmpty()) {
                for (String value : filters.values()) {
                    statement.setString(index++, value);
                }
            }
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                StatisticEntry entry = new StatisticEntry();
                boolean grouping = !type.equals(GroupingType.None);
                mapResult(EventType.valueOf(resultSet.getString("type")), additionalFields, groupFields, resultSet, entry, mediacenter);

                if (result.contains(entry) && grouping) {
                    entry = result.get(result.indexOf(entry));
                }
                entry.getCounts().put(EventType.valueOf(resultSet.getString("type")), resultSet.getInt("count"));
                if (!result.contains(entry) || !grouping) {
                    result.add(entry);
                }
            }
            Collections.sort(result);
            return result;
        } finally {
            dbAlf.cleanUp(con, statement);
        }
    }

    private void mapResult(EventType type, List<String> additionalFields, List<String> groupFields, ResultSet resultSet, StatisticEntry entry, String mediacenter) throws SQLException {
        setAuthorityFromResult(resultSet, entry, mediacenter);
        if (additionalFields != null && !additionalFields.isEmpty()) {
            mapAdditionalFields(type, additionalFields, resultSet, entry, mediacenter);
        }
        try {
            entry.setDate(resultSet.getString("date"));
        } catch (PSQLException e) {
            // ignore, some queries don't have a date field
        }
        if (groupFields != null && !groupFields.isEmpty()) {
            for (String field : groupFields) {
                entry.getFields().put(field, resultSet.getString(field));
            }
        }
    }

    private static void mapAdditionalFields(EventType type, List<String> additionalFields, ResultSet resultSet, StatisticEntry entry, String mediacenter) throws SQLException {
        for (String field : additionalFields) {

            Map<EventType, Map<String, Map<String, Long>>> groups = entry.getGroups();
            Map<String, Map<String, Long>> current = groups.get(type);
            if (current == null) {
                current = new HashMap<>();
            }
            Map<String, Long> counted = getArrayAggToCounts((String[]) resultSet.getArray(field).getArray());
            if (field.equals("authority_mediacenter") && mediacenter != null) {
                counted = counted.entrySet().stream().filter(
                        e -> mediacenter.equals(e.getKey())
                ).collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
            }
            current.put(field, counted);
            groups.put(type, current);
        }
    }

    @NotNull
    private static Map<String, Long> getArrayAggToCounts(String[] arrayAgg) throws SQLException {
        // the sql field will add each property to an array like 1,2,1,3
        // we will map it to {1:2,2:1,3:1}
        return new HashMap<>(Arrays.stream(arrayAgg).map((a) -> a == null ? "" : a)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())));
    }

    @Override
    public List<StatisticEntryNode> getNodeStatisics(GroupingType type, java.util.Date dateFrom, java.util.Date dateTo, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws Throwable {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            List<StatisticEntryNode> result = initList(StatisticEntryNode.class, type, dateFrom, dateTo);
            String query = getQuery(type, "edu_tracking_node", mediacenter, additionalFields, groupFields, filters);
            statement = con.prepareStatement(query);
            int index = 1;
            statement.setTimestamp(index++, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            statement.setTimestamp(index++, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            if (mediacenter != null && !mediacenter.isEmpty()) {
                statement.setString(index++, mediacenter);
            }
            if (filters != null && !filters.isEmpty()) {
                for (String value : filters.values()) {
                    statement.setString(index++, value);
                }
            }
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                StatisticEntryNode entry = new StatisticEntryNode();
                boolean grouping = !type.equals(GroupingType.None) || groupFields != null && !groupFields.isEmpty();
                if (type.equals(GroupingType.Node)) {
                    entry.setNode(resultSet.getString("node"));
                } else {
                    if (!grouping) {
                        entry.setNode(resultSet.getString("node"));
                    }
                }
                // filter nodes which are not associated with this media center
                if (entry.getNode() != null && mediacenter != null && !mediacenter.isEmpty()) {
                    if (!isPartOfMediacenter(mediacenter, entry.getNode())) {
                        continue;
                    }
                }

                mapResult(EventType.valueOf(resultSet.getString("type")), additionalFields, groupFields, resultSet, entry, mediacenter);

                if (result.contains(entry) && grouping) {
                    entry = result.get(result.indexOf(entry));
                }
                entry.getCounts().put(EventType.valueOf(resultSet.getString("type")), resultSet.getInt("count"));
                if (!result.contains(entry) || !grouping) {
                    result.add(entry);
                }
            }
            Collections.sort(result);
            return result;
        } finally {
            dbAlf.cleanUp(con, statement);
        }
    }

    private boolean isPartOfMediacenter(String mediacenter, String nodeId) {
        List<String> readPermissions = Arrays.asList(
                CCConstants.PERMISSION_READ,
                CCConstants.PERMISSION_READ_ALL,
                CCConstants.PERMISSION_CONSUMER
        );
        return AuthenticationUtil.runAsSystem(
                () -> PermissionServiceFactory.getLocalService().
                        getExplicitPermissionsForAuthority(nodeId,
                                MediacenterServiceFactory.getLocalService().getMediacenterProxyGroup(mediacenter))
                        .stream().anyMatch(readPermissions::contains)
        );
    }

    /**
     * fechtes the counts for each event type for the given node, while the date must be between the given dates
     * If both dates are null, it will take the values across all dates
     *
     * @return
     * @throws Throwable
     */
    @Override
    public StatisticEntry getSingleNodeData(NodeRef node, java.util.Date dateFrom, java.util.Date dateTo) throws Throwable {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            String query = TRACKING_STATISTICS_NODE_SINGLE;
            statement = con.prepareStatement(query);
            int index = 1;
            statement.setString(index++, node.getId());
            if (dateFrom == null)
                dateFrom = new java.util.Date(0);
            statement.setTimestamp(index++, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            if (dateTo == null)
                dateTo = new java.util.Date();
            statement.setTimestamp(index++, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            ResultSet resultSet = statement.executeQuery();
            StatisticEntry entry = new StatisticEntry();
            while (resultSet.next()) {
                entry.getCounts().put(EventType.valueOf(resultSet.getString("type")), resultSet.getInt("count"));
            }
            return entry;
        } catch (Throwable t) {
            throw t;
        } finally {
            dbAlf.cleanUp(con, statement);
        }
    }

    @Override
    public Map<NodeRef, StatisticEntry> getListNodeData(List<NodeRef> nodes, java.util.Date dateFrom, java.util.Date dateTo, List<String> additionalFields, String mediacenter) throws Throwable {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        Map<NodeRef, StatisticEntry> data = new HashMap<>();
        nodes.forEach(ref -> {
            data.put(ref, new StatisticEntry());
        });
        try {
            con = dbAlf.getConnection();
            String query = TRACKING_STATISTICS_NODE_ARRAY;
            String fields = "";
            if (!additionalFields.isEmpty()) {
                fields = "," + StringUtils.join(additionalFields.stream().map(f -> "ARRAY_AGG(" + makeDbField(f, true) + ") as " + f).collect(Collectors.toList()), ",");
            }
            query = query.replace(":fields", fields);
            statement = con.prepareStatement(query);
            int index = 1;
            if (dateFrom == null) {
                dateFrom = new java.util.Date(0);
            }
            statement.setTimestamp(index++, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            if (dateTo == null) {
                dateTo = new java.util.Date();
            }
            statement.setTimestamp(index++, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            statement.setString(index++, mediacenter);
            statement.setArray(index++, con.createArrayOf("text", nodes.stream().map(NodeRef::getId).toArray()));

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, resultSet.getString("node_uuid_final"));
                EventType event = EventType.valueOf(resultSet.getString("type"));
                data.get(nodeRef).getCounts().put(event, resultSet.getInt("count"));
                if (!additionalFields.isEmpty()) {
                    mapAdditionalFields(event, additionalFields, resultSet, data.get(nodeRef), mediacenter);
                }
            }
            return data;
        } finally {
            dbAlf.cleanUp(con, statement);
        }
    }

    @Override
    public Map<NodeRef, StatisticEntry> getListNodeDataByMediacenter(String mediacenter, java.util.Date dateFrom, java.util.Date dateTo, List<String> additionalFields) throws Throwable {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        Map<NodeRef, StatisticEntry> data = new HashMap<>();
        try {
            con = dbAlf.getConnection();
            String query = TRACKING_STATISTICS_NODE_MEDIACENTER;
            String fields = "";
            if (!additionalFields.isEmpty()) {
                fields = "," + StringUtils.join(additionalFields.stream().map(f -> "ARRAY_AGG(" + makeDbField(f, true) + ") as " + f).collect(Collectors.toList()), ",");
            }
            query = query.replace(":fields", fields);
            statement = con.prepareStatement(query);
            int index = 1;
            statement.setString(index++, mediacenter);
            if (dateFrom == null) {
                dateFrom = new java.util.Date(0);
            }
            statement.setTimestamp(index++, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            if (dateTo == null) {
                dateTo = new java.util.Date();
            }
            statement.setTimestamp(index++, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, resultSet.getString("node_uuid_final"));
                EventType event = EventType.valueOf(resultSet.getString("type"));
                if (!data.containsKey(nodeRef)) {
                    data.put(nodeRef, new StatisticEntry());
                }
                data.get(nodeRef).getCounts().put(event, resultSet.getInt("count"));
                if (!additionalFields.isEmpty()) {
                    mapAdditionalFields(event, additionalFields, resultSet, data.get(nodeRef), mediacenter);
                }
            }
            return data;
        } finally {
            dbAlf.cleanUp(con, statement);
        }
    }

    @Override
    public void deleteUserData(String username) throws Throwable {
        if (getUserTrackingMode().equals(UserTrackingMode.none)) {
            logger.info("User tracking is set to none, deleteUserData will do nothing");
            return;
        }
        execDatabaseQuery(TRACKING_DELETE_NODE, (statement) -> {
            statement.setString(1, getTrackedUsername(username));
            return true;
        });
        execDatabaseQuery(TRACKING_DELETE_USER, (statement) -> {
            statement.setString(1, getTrackedUsername(username));
            return true;
        });
    }

    @Override
    public void reassignUserData(String oldUsername, String newUsername) {
        if (getUserTrackingMode().equals(UserTrackingMode.none)) {
            logger.info("User tracking is set to none, reassignUserData will do nothing");
            return;
        }
        execDatabaseQuery(TRACKING_UPDATE_NODE, (statement) -> {
            statement.setString(1, getTrackedUsername(newUsername));
            statement.setString(2, getTrackedUsername(oldUsername));
            return true;
        });
        execDatabaseQuery(TRACKING_UPDATE_USER, (statement) -> {
            statement.setString(1, getTrackedUsername(newUsername));
            statement.setString(2, getTrackedUsername(oldUsername));
            return true;
        });
    }

    private void setAuthorityFromResult(ResultSet resultSet, StatisticEntry entry, String mediacenter) throws SQLException {
        try {
            entry.getAuthorityInfo().setAuthority(resultSet.getString("authority"));
        } catch (PSQLException ignored) {
        }
        try {
            Array orgs = resultSet.getArray("authority_organization");
            if (orgs != null) {
                entry.getAuthorityInfo().setOrganizations((String[]) orgs.getArray());
            }
        } catch (PSQLException ignored) {
        }
        try {
            Array mediacenters = resultSet.getArray("authority_mediacenter");
            if (mediacenters != null) {
                if (!StringUtils.isEmpty(mediacenter)) {
                    // filter only for the current mediacenter
                    entry.getAuthorityInfo().setMediacenters(Arrays.stream((String[]) mediacenters.getArray()).filter(mediacenter::equals).toArray(String[]::new));
                } else {
                    entry.getAuthorityInfo().setMediacenters((String[]) mediacenters.getArray());
                }
            }
        } catch (PSQLException ignored) {
        }
    }

    private <T extends StatisticEntry> List<T> initList(Class<T> clz, GroupingType type, java.util.Date dateFrom, java.util.Date dateTo) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        long DAY_DURATION = 1000 * 60 * 60 * 24;

        List<T> list = new ArrayList<>();
        if (type.equals(GroupingType.None) || type.equals(GroupingType.Node))
            return list;

        Date date = new Date(dateFrom.getTime());
        String pattern;
        if (type.equals(GroupingType.Daily)) {
            pattern = "yyyy-MM-dd";
        } else if (type.equals(GroupingType.Monthly)) {
            pattern = "yyyy-MM";
        } else {
            pattern = "yyyy";
        }
        SimpleDateFormat dt = new SimpleDateFormat(pattern);
        while (date.getTime() < dateTo.getTime() - DAY_DURATION) {
            T obj = clz.getDeclaredConstructor().newInstance();
            obj.setDate(dt.format(date));
            if (!list.contains(obj))
                list.add(obj);
            date.setTime(date.getTime() + DAY_DURATION);
        }
        return list;
    }

    private String getQuery(GroupingType type, String table, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws SQLException {
        try {
            String prepared = null;
            if (type.equals(GroupingType.Daily))
                prepared = TRACKING_STATISTICS_DAILY;
            else if (type.equals(GroupingType.Monthly))
                prepared = TRACKING_STATISTICS_MONTHLY;
            else if (type.equals(GroupingType.Yearly))
                prepared = TRACKING_STATISTICS_YEARLY;
            else if (type.equals(GroupingType.Node)) {
                prepared = TRACKING_STATISTICS_NODE_GROUPED;
            } else if (type.equals(GroupingType.None)) {
                if (groupFields != null && !groupFields.isEmpty()) {
                    prepared = TRACKING_STATISTICS_CUSTOM_GROUPING;
                } else if (table.equals("edu_tracking_node")) {
                    prepared = TRACKING_STATISTICS_NODE;
                } else if (table.equals("edu_tracking_user")) {
                    prepared = TRACKING_STATISTICS_USER;
                }
            }
            if (prepared == null)
                throw new IllegalArgumentException("No statement found for tracking table " + table + " and mode " + type);

            prepared = prepared.replace(":table", table);

            StringBuilder filter = new StringBuilder("true");
            StringBuilder grouping = new StringBuilder();
            StringBuilder fields = new StringBuilder();
            if (mediacenter != null && !mediacenter.isEmpty()) {
                filter.append(" AND (ARRAY[?] <@ authority_mediacenter)");
            }
            if (filters != null && !filters.isEmpty()) {
                filter.append(" AND (");
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    filter.append(" AND ");
                    filter.append(makeDbField(entry.getKey(), false)).append(" = ?");
                }
                filter.append(")");
            }
            if (groupFields != null && !groupFields.isEmpty()) {
                for (String field : groupFields) {
                    grouping.append(",").append(makeDbField(field, false));
                    fields.append(",").append(makeDbField(field, false)).append(" as ").append(field);
                }
            }
            if (additionalFields != null && !additionalFields.isEmpty()) {
                for (String field : additionalFields) {
                    fields.append(",ARRAY_AGG(").append(makeDbField(field, true)).append(") as ").append(field);
                    // if additional fields and no grouping is provided, add them to grouping otherwise there will be a postgres exception when fetching
                    // nope: it is now using ARRAY_AGG
                    //if(groupFields==null || groupFields.isEmpty()){
                    //    grouping.append(",").append(makeDbField(field));
                    //}
                }
            }
            prepared = prepared.replace(":fields", fields).replace(":filter", filter).replace(":grouping", grouping);
            logger.info(prepared);
            return prepared;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw t;
        }
    }

    private String makeDbField(String field, boolean useFirstIndex) {
        FieldDescription fieldDescription = EXISTING_FIELDS.get(field);
        if (fieldDescription == null) {
            if (field.toLowerCase().matches("[a-z]*[0-9]*")) {
                return "data ->> '" + field + "'";
            }
            throw new IllegalArgumentException("Fields for filter and grouping should only contain numbers and letters");
        }

        if (fieldDescription.isArray() && useFirstIndex) {
            return field + "[1]";
        }

        return field;
    }

    private static boolean execDatabaseQuery(String statementContent, FillStatement fillStatement) {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            statement = con.prepareStatement(statementContent);
            if (fillStatement.onFillStatement(statement)) {
                statement.executeUpdate();
            }
            statement.close();
            con.commit();

        } catch (Throwable t) {
            logger.error("Error tracking to database", t);
        } finally {
            dbAlf.cleanUp(con, statement);
        }
        return true;
    }

    private interface FillStatement {
        boolean onFillStatement(PreparedStatement statement) throws Exception;
    }
}
