package org.edu_sharing.service.tracking;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;
import org.json.JSONObject;
import org.postgresql.util.PGobject;
import org.springframework.context.ApplicationContext;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackingServiceImpl extends TrackingServiceDefault{
    public static Logger logger = Logger.getLogger(TrackingServiceImpl.class);

    public static String TRACKING_NODE_TABLE_ID = "edu_tracking_node";
    public static String TRACKING_USER_TABLE_ID = "edu_tracking_user";
    public static String TRACKING_INSERT_NODE = "insert into " + TRACKING_NODE_TABLE_ID +" (node_id,node_uuid,node_version,authority,time,type,data) VALUES (?,?,?,?,?,?,?)";
    public static String TRACKING_INSERT_USER = "insert into " + TRACKING_USER_TABLE_ID +" VALUES (?,?,?,?)";
    public static String TRACKING_STATISTICS_NODE = "SELECT node_uuid as node,type,COUNT(*) :additional from edu_tracking_node as tracking" +
            //" LEFT JOIN alf_node_properties as props ON (tracking.node_id=props.node_id and props.qname_id=28)" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY node,type :grouping" +
            " ORDER BY count DESC";
    public static String TRACKING_STATISTICS_DAILY = "SELECT type,COUNT(*),TO_CHAR(time,'yyyy-mm-dd') as date :additional from :table as tracking" +
            //" LEFT JOIN alf_node_properties as props ON (tracking.node_id=props.node_id and props.qname_id=28)" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type,date :grouping" +
            " ORDER BY date";
    public static String TRACKING_STATISTICS_MONTHLY = "SELECT type,COUNT(*),TO_CHAR(time,'yyyy-mm') as date :additional from :table as tracking" +
            //" LEFT JOIN alf_node_properties as props ON (tracking.node_id=props.node_id and props.qname_id=28)" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type,date :grouping" +
            " ORDER BY date";
    public static String TRACKING_STATISTICS_YEARLY = "SELECT type,COUNT(*),TO_CHAR(time,'yyyy') as date :additional from :table as tracking" +
            //" LEFT JOIN alf_node_properties as props ON (tracking.node_id=props.node_id and props.qname_id=28)" +
            " WHERE time BETWEEN ? AND ? AND (:filter)" +
            " GROUP BY type,date :grouping" +
            " ORDER BY date";
    private final NodeService nodeService;

    public TrackingServiceImpl() {
        ApplicationContext appContext = AlfAppContextGate.getApplicationContext();

        ServiceRegistry serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        nodeService=serviceRegistry.getNodeService();
    }

    @Override
    public boolean trackActivityOnUser(String authorityName, EventType type) {
        super.trackActivityOnUser(authorityName,type);
        if(authorityName==null ||authorityName.equals(ApplicationInfoList.getHomeRepository().getGuest_username())){
            return false;
        }
        return AuthenticationUtil.runAsSystem(()-> {
            return addToDatabase(TRACKING_INSERT_USER, statement -> {
                statement.setString(1, super.getTrackedUsername(authorityName));
                statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                statement.setString(3, type.name());
                JSONObject json = buildJson(authorityName, type);
                PGobject obj = new PGobject();
                obj.setType("json");
                if (json != null)
                    obj.setValue(json.toString());
                statement.setObject(4, obj);

                return true;
            });
        });

    }

    @Override
    public boolean trackActivityOnNode(NodeRef nodeRef,String nodeVersion, EventType type) {
        super.trackActivityOnNode(nodeRef,nodeVersion,type);
        return AuthenticationUtil.runAsSystem(()-> {
            String version;
            if(nodeVersion==null || nodeVersion.isEmpty() || nodeVersion.equals("-1")){
                version=NodeServiceHelper.getProperty(nodeRef,CCConstants.CM_PROP_VERSIONABLELABEL);
            }
            else{
                version=nodeVersion;
            }
            return addToDatabase(TRACKING_INSERT_NODE, statement -> {
                // @Todo: track the node version
                statement.setLong(1, (Long) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.SYS_PROP_NODE_DBID)));
                statement.setString(2, nodeRef.getId());
                statement.setString(3, version);
                statement.setString(4, super.getTrackedUsername(null));
                statement.setDate(5, new Date(System.currentTimeMillis()));
                statement.setString(6, type.name());
                JSONObject json = buildJson(nodeRef, type);
                PGobject obj = new PGobject();
                obj.setType("json");
                if (json != null)
                    obj.setValue(json.toString());
                statement.setObject(7, obj);

                return true;
            });
        });
    }

    /**
     * overwrite this in a custom method to track additional data
     */
    protected JSONObject buildJson(NodeRef nodeRef, EventType type) {
        /*
        try {
            // sample object for testing purposes
            return new JSONObject().put("ref",nodeRef.getId()).put("boolean",true).put("int",1).put("string","text").put("double",1.0);
        } catch (JSONException e) {}
        */
        return null;
    }
    /**
     * overwrite this in a custom method to track additional data
     */
    protected JSONObject buildJson(String authorityName, EventType type) {
        return null;
    }
    @Override
    public List<StatisticEntry> getUserStatistics(GroupingType type, java.util.Date dateFrom, java.util.Date dateTo, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws Throwable {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            List<StatisticEntry> result = initList(StatisticEntry.class,type,dateFrom,dateTo);
            String query = getQuery(type, "edu_Tracking_user", additionalFields, groupFields, filters);
            statement = con.prepareStatement(query);
            statement.setTimestamp(1, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            statement.setTimestamp(2, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));

            if (filters != null && !filters.isEmpty()) {
                int i = 3;
                for (String value : filters.values()) {
                    statement.setString(i++, value);
                }
            }
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                StatisticEntry entry = new StatisticEntry();
                entry.setDate(resultSet.getString("date"));
                if (additionalFields != null && additionalFields.size() > 0) {
                    for (String field : additionalFields) {
                        entry.getData().put(field, resultSet.getString(field));
                    }
                }
                if (result.contains(entry)) {
                    entry = result.get(result.indexOf(entry));
                }
                entry.getCounts().put(EventType.valueOf(resultSet.getString("type")), resultSet.getInt("count"));
                if (!result.contains(entry)) {
                    result.add(entry);
                }
            }
            return result;
        }catch(Throwable t){
            throw t;
        }
        finally{
            dbAlf.cleanUp(con, statement);
        }
    }

    @Override
    public List<StatisticEntryNode> getNodeStatisics(GroupingType type, java.util.Date dateFrom, java.util.Date dateTo, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws Throwable {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            List<StatisticEntryNode> result = initList(StatisticEntryNode.class,type,dateFrom,dateTo);
            String query = getQuery(type, "edu_tracking_node", additionalFields, groupFields, filters);
            statement=con.prepareStatement(query);
            statement.setTimestamp(1, Timestamp.valueOf(dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            statement.setTimestamp(2, Timestamp.valueOf(dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));

            if (filters != null && !filters.isEmpty()) {
                int i = 3;
                for (String value : filters.values()) {
                    statement.setString(i++, value);
                }
            }
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                StatisticEntryNode entry = new StatisticEntryNode();
                if (type.equals(GroupingType.None)) {
                    entry.setNode(resultSet.getString("node"));
                } else
                    entry.setDate(resultSet.getString("date"));
                if (additionalFields != null && additionalFields.size() > 0) {
                    for (String field : additionalFields) {
                        entry.getData().put(field, resultSet.getString(field));
                    }
                }
                if (result.contains(entry)) {
                    entry = result.get(result.indexOf(entry));
                }
                entry.getCounts().put(EventType.valueOf(resultSet.getString("type")), resultSet.getInt("count"));
                if (!result.contains(entry)) {
                    result.add(entry);
                }
            }
            return result;
        }catch(Throwable t){
            throw t;
        }finally {
            dbAlf.cleanUp(con, statement);
        }
    }

    private <T extends StatisticEntry> List<T> initList(Class<T> clz,GroupingType type, java.util.Date dateFrom, java.util.Date dateTo) throws IllegalAccessException, InstantiationException {
        long DAY_DURATION=1000*60*60*24;

        List<T> list = new ArrayList<>();
        if(type.equals(GroupingType.None))
            return list;

        Date date=new Date(dateFrom.getTime());
        String pattern;
        if(type.equals(GroupingType.Daily)){
            pattern="yyyy-MM-dd";
        }
        else if(type.equals(GroupingType.Monthly)){
            pattern="yyyy-MM";
        }
        else{
            pattern="yyyy";
        }
        SimpleDateFormat dt = new SimpleDateFormat(pattern);
        while(date.getTime()<dateTo.getTime()){
            T obj=clz.newInstance();
            obj.setDate(dt.format(date));
            if(!list.contains(obj))
                list.add(obj);
            date.setTime(date.getTime()+DAY_DURATION);
        }
        return list;
    }

    private String getQuery(GroupingType type, String table, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws SQLException {
        try {
            String prepared;
            if (type.equals(GroupingType.Daily))
                prepared = TRACKING_STATISTICS_DAILY;
            else if (type.equals(GroupingType.Monthly))
                prepared = TRACKING_STATISTICS_MONTHLY;
            else if (type.equals(GroupingType.Yearly))
                prepared = TRACKING_STATISTICS_YEARLY;
            else if (type.equals(GroupingType.None) && table.equals("edu_tracking_node")) {
                prepared = TRACKING_STATISTICS_NODE;
            } else {
                throw new IllegalArgumentException("No statement found for tracking table " + table + " and mode " + type);
            }
            prepared = prepared.replace(":table", table);

            StringBuilder filter = new StringBuilder("true");
            StringBuilder grouping = new StringBuilder();
            StringBuilder additional = new StringBuilder();
            if (filters != null && !filters.isEmpty()) {
                filter = new StringBuilder();
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    if (filter.length() > 0)
                        filter.append(" AND ");
                    filter.append(makeDbField(entry.getKey())).append(" = ?");
                }
            }
            if (groupFields != null && groupFields.size() > 0) {
                for (String field : groupFields) {
                    grouping.append(",").append(makeDbField(field));
                }
            }
            if (additionalFields != null && additionalFields.size() > 0) {
                for (String field : additionalFields) {
                    additional.append(",").append(makeDbField(field) + " as " + field);
                }
            }
            prepared = prepared.replace(":additional", additional).replace(":filter", filter).replace(":grouping", grouping);

            return prepared;
        }catch(Throwable t) {
            logger.error(t.getMessage(), t);
            throw t;
        }
    }

    private String makeDbField(String field) {
        if(field.toLowerCase().matches("[a-z]*[0-9]*")){
            return "data ->> '"+field+"'";
        }
        else
            throw new IllegalArgumentException("Fields for filter and grouping should only contain numbers and letters");
    }

    private static boolean addToDatabase(String statementContent,FillStatement fillStatement){
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = dbAlf.getConnection();
            statement = con.prepareStatement(statementContent);
            if(fillStatement.onFillStatement(statement)) {
                statement.executeUpdate();
            }
            statement.close();
            con.commit();

        }catch(Throwable t){
            logger.error("Error tracking to database",t);
        }finally {
            dbAlf.cleanUp(con, statement);
        }
        return true;
    }
    private interface FillStatement{
        boolean onFillStatement(PreparedStatement statement) throws SQLException;
    }
}
