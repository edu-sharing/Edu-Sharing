package org.edu_sharing.alfresco.service.search;

import java.util.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.PermissionEvaluationMode;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CMISSearchHelper {

    private static Logger logger= Logger.getLogger(CMISSearchHelper.class);

    public static ResultSet fetchNodesByTypeAndFilters(String nodeType, Map<String,Object> filters,List<String> aspects, CMISSearchData data, int from, int pageSize, int maxPermissionChecks, StoreRef storeRef){
    	logger.debug("from: "+from+ " pageSize:"+ pageSize);
    	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

        SearchParameters params=new SearchParameters();
        params.addStore((storeRef == null) ? StoreRef.STORE_REF_WORKSPACE_SPACESSTORE : storeRef);
        // will use the database
        params.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);

        params.setSkipCount(from);
        params.setMaxItems(pageSize);
        params.setMaxPermissionChecks(maxPermissionChecks);
        String tableName=CCConstants.getValidLocalName(nodeType);
        String tableNameAlias=tableName.split(":")[1];
        StringBuilder join= new StringBuilder();
        StringBuilder where= new StringBuilder();
        List<String> joinedTable = new ArrayList<>();
        if(filters!=null && filters.size()>0){
            for(Map.Entry<String,Object> filter : filters.entrySet()){
                if(filter.getKey().startsWith("cmis")){
                    prepareWhere(where);
                    where.append(tableNameAlias).append(".").append(filter.getKey()).append(" = ").append(escape(filter.getValue().toString()));
                }
                else{
                    // join the needed aspect, and access this ones value
                    PropertyDefinition property = serviceRegistry.getDictionaryService().getProperty(QName.createQName(filter.getKey()));
                    if(property == null){
                        throw new RuntimeException("Property " + filter.getKey() +" was not found in the alfresco dicitionary. Please check the spelling");
                    }
                    String aspectTable=CCConstants.getValidLocalName(property.getContainerClass().getName().toString());
                    String aspectTableAlias = property.getContainerClass().getName().getLocalName();
                    if(!joinedTable.contains(aspectTable) && !tableName.equals(aspectTable)) {
                        join.append("JOIN ").append(aspectTable).append(" AS ").append(aspectTableAlias)
                                .append(" ON ").append(aspectTableAlias).append(".cmis:objectId = ").append(tableNameAlias).append(".cmis:objectId ");
                        joinedTable.add(aspectTable);
                    }
                    prepareWhere(where);
                    where.append(aspectTableAlias).append(".").append(CCConstants.getValidLocalName(filter.getKey()));
                    if(filter.getValue()==null) {
                        where.append(" IS NULL");
                    }else{
                        where.append(" = ").append(escape(filter.getValue().toString()));
                    }
                }
            }
            if(aspects != null && aspects.size() > 0){
                for(String aspect : new HashSet<String>(aspects)){
                   AspectDefinition ad = serviceRegistry.getDictionaryService().getAspect(QName.createQName(aspect));
                   if(ad == null){
                       throw new RuntimeException("aspect not found for:"+aspect);
                   }
                   String aspectTable = CCConstants.getValidLocalName(aspect);
                   String aspectTableAlias = ad.getName().getLocalName();
                   //check if aspect filter was already through property filter added
                   if(!joinedTable.contains(aspectTable) && !tableName.equals(aspectTable)) {
                       join.append("JOIN ").append(aspectTable).append(" AS ").append(aspectTableAlias)
                               .append(" ON ").append(aspectTableAlias).append(".cmis:objectId = ").append(tableNameAlias).append(".cmis:objectId ");
                   }
                }
            }
        }
        if(data!=null){
            if(data.inFolder != null){
                prepareWhere(where);
                where.append("IN_FOLDER(").append(tableNameAlias).append(", ").append(escape(data.inFolder)).append(")");
            }
            if(data.inTree != null){
                prepareWhere(where);
                where.append("IN_TREE(").append(tableNameAlias).append(", ").append(escape(data.inTree)).append(")");
            }
        }
        String query="SELECT "+tableNameAlias+".cmis:name FROM "+ tableName + " AS " + tableNameAlias + " " + join + where;
        params.setQuery(query);
        ResultSet result = serviceRegistry.getSearchService().query(params);
        logger.debug(query+": "+result.getNumberFound() +" "+ result.length() +" "+ result.getClass().getName() +" getBulkFetchSize: "+ result.getBulkFetchSize()+" "+result);
        return result;
    }

    private static void prepareWhere(StringBuilder where) {
        if(where.length() > 0) {
            where.append(" AND ");
        }else {
            where.append(" WHERE ");
        }
    }

    public static List<NodeRef> fetchNodesByTypeAndFilters(String nodeType, Map<String,Object> filters, List<String> aspects, CMISSearchData data, int maxPermissionChecks, StoreRef storeRef){
    	List<NodeRef> result = new ArrayList<>();

        int from = 0;

        int pageSize = 1000;

        ResultSet resultSet = null;
        do {
     	   resultSet = fetchNodesByTypeAndFilters(nodeType, filters,aspects, data, from, pageSize, maxPermissionChecks, storeRef);
     	   result.addAll(resultSet.getNodeRefs());
     	   from += pageSize;
        }while(resultSet.length() > 0);

        logger.debug("result:" + result.size());
        return result;
    }

    public static List<NodeRef> fetchNodesByTypeAndFilters(String nodeType, Map<String,Object> filters, CMISSearchData data){
       return fetchNodesByTypeAndFilters(nodeType,filters,null, data,1000, null);
    }

    public static List<NodeRef> fetchNodesByTypeAndFilters(String nodeType, Map<String,Object> filters){
        return fetchNodesByTypeAndFilters(nodeType,filters,(StoreRef)null);
    }

    public static List<NodeRef> fetchNodesByTypeAndFilters(String nodeType, Map<String,Object> filters, StoreRef storeRef){
        return fetchNodesByTypeAndFilters(nodeType,filters,null, null,1000,storeRef);
    }

    /**
     * from QueryStatementImpl
     */
    private static String escape(String str) {
        StringBuilder sb = new StringBuilder(str.length() + 16);
        sb.append('\'');

        for(int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c == '\'' || c == '\\') {
                sb.append('\\');
            }

            sb.append(c);
        }

        sb.append('\'');
        return sb.toString();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CMISSearchData {
        /**
         * The folder in which the elements are located
         * When used, the query is still processed via the DB
         */
        public String inFolder;
        /**
         * One folder of the tree in which the elements are located
         * WARNING: When active, SOLR instead of the database will be used!
         * https://docs.alfresco.com/5.2/concepts/query-lang-support.html
         */
        public String inTree;
    }

    public static List<NodeRef> getLevel0Collections(String username){
        Map<String,Object> filter = new HashMap<>();
        filter.put(CCConstants.CM_PROP_OWNER,username);
        filter.put(CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0,"true");
        return fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_MAP,filter);
    }
}
