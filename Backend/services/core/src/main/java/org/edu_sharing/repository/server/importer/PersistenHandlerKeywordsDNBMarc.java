package org.edu_sharing.repository.server.importer;

import org.apache.commons.collections.CollectionUtils;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;


import java.sql.*;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class PersistenHandlerKeywordsDNBMarc implements PersistentHandlerInterface{

    static String STATEMENT_EXISTS = "SELECT * from edu_factual_term WHERE factual_term_ident=?";

    static String STATEMENT_INSERT = "INSERT INTO edu_factual_term(factual_term_ident, factual_term_value, factual_term_synonyms) VALUES(?, ?, ?)";

    static String STATEMENT_UPDATE = "UPDATE edu_factual_term SET factual_term_ident=?, factual_term_value=?, factual_term_synonyms=? WHERE factual_term_ident=?";

    static String STATEMENT_MATCHES = "select CASE WHEN factual_term_synonyms IS NULL THEN factual_term_value WHEN array_length(factual_term_synonyms,1) = 1 THEN format('%s (%s)',factual_term_value, factual_term_synonyms[1])  ELSE format('%s (%s, %s)',factual_term_value, factual_term_synonyms[1], factual_term_synonyms[2]) END from edu_factual_term where lower(factual_term_value) like ? order by char_length(factual_term_value) limit 10";

    static String STATEMENT_CHANGED = "select factual_term_ident from edu_factual_term where factual_term_modified IS NOT NULL";

    static String STATEMENT_RESET_MODIFIED = "UPDATE edu_factual_term SET factual_term_modified=NULL WHERE factual_term_ident=?";

    static String STATEMENT_DISABLE_TRIGGER = "ALTER TABLE edu_factual_term DISABLE TRIGGER update_edu_factual_term_modtime";

    static String STATEMENT_ENABLE_TRIGGER = "ALTER TABLE edu_factual_term ENABLE TRIGGER update_edu_factual_term_modtime";

    static String STATEMENT_TRIGGER_ENABLED = "select tgenabled from pg_trigger where tgname='update_edu_factual_term_modtime'";

    static String STATEMENT_COUNT = "SELECT count(*) FROM edu_factual_term";

    static String COL_ID = "factual_term_id";
    static String COL_IDENT = "factual_term_ident";
    static String COL_VALUE = "factual_term_value";
    static String COL_SYNONYMS = "factual_term_synonyms";

    Logger logger = Logger.getLogger(PersistenHandlerKeywordsDNBMarc.class);

    public PersistenHandlerKeywordsDNBMarc(){

    }

    @Override
    public String safe(RecordHandlerInterfaceBase recordHandler, String cursor, String set) throws Throwable {

        Map<String, Object> props = recordHandler.getProperties();
        String id = (String)props.get(RecordHandlerKeywordsDNBMarc.ID);
        if(id == null){
            logger.error("no id provided");
            return null;
        }

        String value = (String) props.get(RecordHandlerKeywordsDNBMarc.NAME);
        if(value == null){
            logger.error("no value provided");
            return null;
        }

        value = fixCombiningDiaresis(value);
        Set<String> synonymCollection = (Set<String>)props.get(RecordHandlerKeywordsDNBMarc.SYNONYMS);
        if(synonymCollection != null){
            synonymCollection = synonymCollection.stream().map(s -> fixCombiningDiaresis(s)).collect(Collectors.toSet());
        }
        String synonyms = null;
        if(synonymCollection.size() > 0) {
            synonymCollection = synonymCollection.stream().map(s -> s.replace("\"","\\\"")).collect(Collectors.toSet());
            synonyms = synonymCollection.stream().collect(Collectors.joining("\",\"", "\"", "\""));
            synonyms = "{"+synonyms+"}";
        }

        Map<String,Object> cols = get(id);
        if(cols == null){
            logger.info("creating:" + id);

            Connection con = null;
            PreparedStatement statement = null;
            ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
            try{
                con = dbAlf.getConnection();
                statement = con.prepareStatement(STATEMENT_INSERT);

                id = StringEscapeUtils.escapeSql(id);
                statement.setString(1, id);
                statement.setString(2, value);
               // statement.setString(3,jsArray.toString());

                statement.setObject(3,synonyms,java.sql.Types.OTHER);

                statement.executeUpdate();
                con.commit();
            }finally {
                dbAlf.cleanUp(con, statement);
            }

        }else{

            List<String> existingSyn = (cols.get(COL_SYNONYMS) == null) ? new ArrayList<>() : new ArrayList<>((List<String>)cols.get(COL_SYNONYMS));
            List<String> newSyn = (synonymCollection == null) ? new ArrayList<>() : new ArrayList<>(synonymCollection);
            if(value.equals(cols.get(COL_VALUE))
                    && CollectionUtils.isEqualCollection(existingSyn,newSyn) ){
                logger.info(id +" didn't change");
                return null;
            }

            String tmpOldSyns = (cols.get(COL_SYNONYMS) == null) ? "": String.join(",",existingSyn);
            String tmpNewSyns = (synonymCollection == null) ? "" :String.join(",",newSyn);
            logger.info("updating;" + id +";old;"+cols.get(COL_VALUE)+";"+tmpOldSyns+";new;"+value+";"+tmpNewSyns);

            String oldValueFormated = displayFormat((String)cols.get(COL_VALUE),existingSyn);
            String newValueFormated = displayFormat(value,newSyn);
            if(!oldValueFormated.trim().isEmpty() && !oldValueFormated.equals(newValueFormated)) {
                logger.info("csv," +  StringEscapeUtils.escapeCsv(oldValueFormated) + "," +  StringEscapeUtils.escapeCsv(newValueFormated));
            }

            Connection con = null;
            PreparedStatement statement = null;
            ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
            try{
                con = dbAlf.getConnection();
                statement = con.prepareStatement(STATEMENT_UPDATE);

                id = StringEscapeUtils.escapeSql(id);
                statement.setString(1, id);
                statement.setString(2, value);
                statement.setObject(3,synonyms,java.sql.Types.OTHER);
                statement.setString(4, id);

                statement.executeUpdate();
                con.commit();
            }finally {
                dbAlf.cleanUp(con, statement);
            }
        }

        return null;
    }

    private String displayFormat(String value, List<String> synonyms){
        String display = value;
        if(synonyms.size() > 0){
            display += " ("+synonyms.get(0);
            if(synonyms.size() > 1){
                display += ", "+ synonyms.get(1);
            }
            display +=")";
        }
        return display;
    }

    @Override
    public boolean mustBePersisted(String replId, String timeStamp) {
        return true;
    }

    @Override
    public boolean exists(String id) {
        Map<String,Object> cols = get(id);
        if(cols == null || cols.size() == 0) return false;
        else return true;
    }

    public Map<String,Object> get(String id){

        Map<String,Object> result = new HashMap<>();
        Connection con = null;
        PreparedStatement statement = null;
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        try{
            con = dbAlf.getConnection();
            statement = con.prepareStatement(STATEMENT_EXISTS);
            id = StringEscapeUtils.escapeSql(id);
            statement.setString(1, id);
            java.sql.ResultSet resultSet = statement.executeQuery();

            if(resultSet.next()){
                result.put(COL_ID,resultSet.getString(COL_ID));
                result.put(COL_IDENT,resultSet.getString(COL_IDENT));
                result.put(COL_VALUE,resultSet.getString(COL_VALUE));
                Array array = resultSet.getArray(COL_SYNONYMS);
                if(array != null){
                    List<String> synonyms = Arrays.asList((String[])array.getArray());
                    result.put(COL_SYNONYMS,synonyms);
                }
                return result;
            }

        } catch (SQLException e) {
            logger.error(e.getMessage(),e);
        } finally {
            dbAlf.cleanUp(con, statement);
        }


        return null;
    }

    public List<String> matches(String value){
        value = value.toLowerCase().trim();

        if(value.matches("[a-zA-Z \\-]* < [a-zA-ZäöüÄÖÜ, ]* >")){
            value = value.replaceAll("< ","<");//value.replaceAll(" < [a-zA-Z]* >","");
            value = value.replaceAll(" >",">");
        }

        List<String> result = new ArrayList<>();
        Connection con = null;
        PreparedStatement statement = null;
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        try{
            con = dbAlf.getConnection();
            statement = con.prepareStatement(STATEMENT_MATCHES);
            //statement.setString(1, value+"%");
            statement.setString(1, value);
            java.sql.ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                String s = resultSet.getString(1);
                if(s == null){
                    logger.error("null value found for:"+ value);
                    continue;
                }
                result.add(s);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(),e);
        } finally {
            dbAlf.cleanUp(con, statement);
        }
        return result;
    }

    public long countEntries(){
        Connection con = null;
        PreparedStatement statement = null;
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        try{
            con = dbAlf.getConnection();
            statement = con.prepareStatement(STATEMENT_COUNT);
            java.sql.ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()){
                Long result = resultSet.getLong(1);
                return result;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(),e);
        } finally {
            dbAlf.cleanUp(con, statement);
        }
        return -1;
    }


    public List<Map<String,Object>> getEntriesByDisplayValue(String value){
        value = value.trim();

        String[] splitted = value.split(" \\(");
        String termValue = splitted[0];
        String tempSyn = (splitted.length > 1) ? splitted[1].replaceAll("\\)","") : null;
        String[] termSyns = (tempSyn != null) ? tempSyn.split(", ") : null;

        List<Map<String,Object>> result = new ArrayList<>();
        Connection con = null;
        PreparedStatement statement = null;
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        try{
            String statementStr = "select factual_term_ident,factual_term_value from edu_factual_term where factual_term_value=?";
            if(termSyns != null && termSyns.length > 0){
                statementStr = "select distinct(factual_term_ident),factual_term_value from (select factual_term_ident,factual_term_value,unnest(factual_term_synonyms) as synonyms from edu_factual_term) s where factual_term_value=?";
                statementStr+=" AND (";
                for(int i = 0; i < termSyns.length; i++){
                    if(i > 0){
                        statementStr+=" OR ";
                    }
                    statementStr += "synonyms like ?";
                }
                statementStr+=")";
            }

            con = dbAlf.getConnection();
            statement = con.prepareStatement(statementStr);
            statement.setString(1, termValue);
            if(termSyns != null) {
                for(int i = 0; i < termSyns.length;i++){
                    statement.setString((i+2), "%" + termSyns[i] + "%");
                }
            }

            java.sql.ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                Map<String,Object> row = new HashMap<>();
                int colCount = resultSet.getMetaData().getColumnCount();
                for(int i = 1; i <= colCount; i++){
                    row.put(resultSet.getMetaData().getColumnName(i),resultSet.getObject(i));
                }
                result.add(row);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(),e);
        } finally {
            dbAlf.cleanUp(con, statement);
        }
        return result;
    }


    public String fixCombiningDiaresis(String value){
        if(!Normalizer.isNormalized(value,Normalizer.Form.NFC)){
            logger.warn("is not normalized:"+value);
        }
        String result = Normalizer.normalize(value, Normalizer.Form.NFC);
        return result;
    }


    public List<String> getChangedIdents(){
        Connection con = null;
        PreparedStatement statement = null;
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        List<String> changedIdents = new ArrayList<>();
        try{
            con = dbAlf.getConnection();
            statement = con.prepareStatement(STATEMENT_CHANGED);

            java.sql.ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                String s = resultSet.getString(1);
                changedIdents.add(s);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(),e);
        } finally {
            dbAlf.cleanUp(con, statement);
        }
        return changedIdents;
    }

    public void resetModified(String ident){
        Connection con = null;
        PreparedStatement statement = null;
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        List<String> changedIdents = new ArrayList<>();
        try{
            con = dbAlf.getConnection();
            con.createStatement().executeUpdate(STATEMENT_DISABLE_TRIGGER);
            ResultSet resultSet = con.createStatement().executeQuery(STATEMENT_TRIGGER_ENABLED);
            resultSet.next();
            statement = con.prepareStatement(STATEMENT_RESET_MODIFIED);
            statement.setString(1, ident);
            statement.executeUpdate();
            logger.info("reseted modified for:"+ident);
        } catch (SQLException e) {
            logger.error(e.getMessage(),e);
        } finally {
            try {
                con.createStatement().executeUpdate(STATEMENT_ENABLE_TRIGGER);
                con.commit();
            } catch (SQLException throwables) {
                logger.error(throwables.getMessage(),throwables);
            }
            dbAlf.cleanUp(con, statement);
        }
    }
}
