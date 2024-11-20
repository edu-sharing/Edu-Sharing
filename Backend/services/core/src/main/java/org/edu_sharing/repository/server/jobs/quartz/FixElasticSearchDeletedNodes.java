package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JobDescription(description = "checks for nodes removed in repository but still exist in elasticsearch. please check that tracker is 100% finished and tracker is disabled before running ths job.")
public class FixElasticSearchDeletedNodes extends AbstractJobMapAnnotationParams{

    @JobFieldDescription(description = "if false (default) no changes will be done.")
    boolean execute;

    @JobFieldDescription(description = "query that delivers a result of nodes that have to be checked. optional. if not set all nodes will be searched.",sampleValue = "{\"query\":\"{\\\"term\\\":{\\\"type\\\":\\\"ccm:io\\\"}}\"}")
    String query;


    @JobFieldDescription(description = "Try to check the nested arrays like children or collections and clean them up as well",sampleValue = "true")
    boolean cleanupChildren = true;

    SearchServiceElastic searchServiceElastic = new SearchServiceElastic(ApplicationInfoList.getHomeRepository().getAppId());

    private static int pageSize = 1000;

    Logger logger = Logger.getLogger(FixElasticSearchDeletedNodes.class);

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();
    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

            AuthenticationUtil.runAsSystem(()->{
                try {
                    ObjectBuilder<Query> queryBuilder = (query == null) ? new Query.Builder().matchAll(x -> x) : new Query.Builder().wrapper(wrapper -> wrapper.query(query));
                    search(queryBuilder, new DeletedNodesHandler());
                } catch (IOException e) {
                    logger.error(e.getMessage(),e);
                }
                return null;
            });
    }

    private void search(ObjectBuilder<Query> queryBuilder, SearchResultHandler searchResultHandler) throws IOException{
        logger.info("search with handler: "+searchResultHandler.getClass().getName());

        Time scroll = Time.of(time->time.time("4h"));
        ResponseBody<Map> response = null;
        int page = 0;
        do{
            if(response == null) {
                response = search(SearchServiceElastic.WORKSPACE_INDEX, queryBuilder, scroll);
            }else {
                response = scroll(scroll,response.scrollId());
            }
            HitsMetadata<Map> searchHits = response.hits();
            logger.info("page:" + page + " with result size:" + searchHits.hits().size() + " of:" + searchHits.total().value());
            for(Hit<Map> searchHit : searchHits.hits()){
                searchResultHandler.handleSearchHit(searchHit);
            }
            page++;
        }while(response.hits() != null && !response.hits().hits().isEmpty());

        boolean clearSuccess = clearScroll(response.scrollId());
        if(clearSuccess) logger.info("cleared scroll successfully");
        else logger.error("clear of scroll "+ response.scrollId() +" failed");
    }




    private SearchResponse<Map>  search(String index, ObjectBuilder<Query> queryBuilder, Time scroll) throws IOException {
        return searchServiceElastic.searchNative(SearchRequest.of(req->req
                .index(index)
                .size(pageSize)
                .source(src->src.filter(filter->filter.excludes("preview", "content")))
                .scroll(scroll)));
    }

    private ScrollResponse<Map> scroll(Time scroll, String scrollId) throws IOException {
        return searchServiceElastic.scrollNative(ScrollRequest.of(sq->sq
                .scrollId(scrollId)
                .scroll(scroll)));
    }

    private boolean clearScroll(String scrollId) throws IOException {
        ClearScrollResponse clearScrollResponse = searchServiceElastic.clearScrollNative(ClearScrollRequest.of(req -> req.scrollId(scrollId)));
        return clearScrollResponse.succeeded();
    }

    public interface SearchResultHandler{
        public void handleSearchHit(Hit<Map> searchHit) throws IOException;
    }

    public class DeletedNodesHandler implements SearchResultHandler{
        @Override
        public void handleSearchHit(Hit<Map> searchHit) throws IOException {

            Map nodeRef = (Map) searchHit.source().get("nodeRef");
            String nodeId = (String) nodeRef.get("id");
            String dbid = searchHit.id();

            Map properties = (Map)searchHit.source().get("properties");
            String name = (String)properties.get("cm:name");
            String type = (String)searchHit.source().get("type");
            List<String> aspects = (List<String>)searchHit.source().get("aspects");

            Map storeRef = (Map) nodeRef.get("storeRef");
            String protocol = (String) storeRef.get("protocol");
            String identifier = (String) storeRef.get("identifier");

            NodeRef alfNodeRef = new NodeRef(new StoreRef(protocol,identifier),nodeId);
            if(!nodeService.exists(alfNodeRef)){
                logger.info(alfNodeRef +";dbid:"+dbid+ ";type:"+ type + ";name:"+name + ";does not longer exist in repo. will remove.");
                //tracker gets 2 events when a node is deleted from workspace: delete and create for archive store
                //so we can safely remove it here without checking for archive store

                //cleanup replicated collections on ios
                if(cleanupChildren && type.equals("ccm:map") && aspects.contains("ccm:collection") ){
                    syncNestedCollections(dbid);
                }

                if(execute){
                    searchServiceElastic.deleteNative(DeleteRequest.of(req->req
                            .index(SearchServiceElastic.WORKSPACE_INDEX)
                            .id(dbid)));
                }
            }

        }
    }

    /**
     * remove nested collection of ccm:io when ccm:collection is remove
     * @TODO test: io with one collection, io with more collections/diff before job
     * @param dbid from collection
     * @throws IOException
     */
    private void syncNestedCollections(String dbid) throws IOException {
        ObjectBuilder<Query> ioCollectionQuery = new Query.Builder().term(term -> term.field("collections.dbid").value(dbid));

        search(ioCollectionQuery, searchHit -> {
            List<Map<String, Object>> collections = (List<Map<String, Object>>) searchHit.source().get("collections");

            /**
             * cleanup collections
             */
            logger.info("will remove replicated collection with id:"+dbid
                    +" name:"+searchHit.source().get("properties.cm:name")
                    +" from ccm:io with id:"+searchHit.id());
            cleanupSubArray(dbid, searchHit, collections, "collections");

            /**
             * cleanup usages
             */
            List<Map<String, Object>> children = (List<Map<String, Object>>) searchHit.source().get("children");
            //find usagedbid from collection object
            Long usageDbId = collections.stream()
                    .filter(m -> ((Number) m.get("dbid")).longValue() == Long.parseLong(dbid))
                    .map(m -> ((Number) m.get("usagedbid")).longValue()).findFirst().get();
            //cleanup usage
            logger.info("will remove replicated usage with id:"+usageDbId
                    +" from ccm:io with id:"+searchHit.id());
            cleanupSubArray(usageDbId.toString(), searchHit, children,"children");
        });
    }

    private void cleanupSubArray(String dbid, Hit<Map> searchHit, List<Map<String, Object>> nestedObjectsArray, String subArrayName) throws IOException {
        if(nestedObjectsArray == null) {
            return;
        }
        if(nestedObjectsArray.size() == 1) {
            /**
             * remove all collections so that there is no empty collection object left over
             */
            Map<String, JsonData> params = new HashMap<>() {{
                put("value", JsonData.of(subArrayName));
            }};

            if(execute) {
                update(UpdateRequest.of(req->req
                        .index(SearchServiceElastic.WORKSPACE_INDEX)
                        .id(searchHit.id())
                        .script(src->src.lang("painless").source("ctx._source.remove(params.get('value'))").params(params))));
            }
        }else {

            /**
             * the other collections
             */
            Map<String, Object> data = new HashMap<>();
            List<Object> list = new ArrayList<>();
            data.put(subArrayName, list);
            for (Map<String, Object> nestedObject : nestedObjectsArray) {
                if(!dbid.equals(nestedObject.get("dbid").toString())){
                    Map<String,Object> obj = new HashMap<>();
                    obj.putAll(nestedObject);
                    list.add(obj);
                }else{
                    logger.info("excluded " + dbid + " from subarray: " + subArrayName);
                }
            }

            if(execute) {
                update(UpdateRequest.of(req->req
                        .index(SearchServiceElastic.WORKSPACE_INDEX)
                        .id(searchHit.id())
                        .doc(data)));
            }
        }
    }

    private void update(UpdateRequest request) throws IOException{
        UpdateResponse<Map> updateResponse = searchServiceElastic.updateNative(
                request
        );
        String index = updateResponse.index();
        String id = updateResponse.id();
        long version = updateResponse.version();
        if (updateResponse.result() == Result.Created) {
            logger.error("object did not exist");
        } else if (updateResponse.result() == Result.Updated) {

        } else if (updateResponse.result() == Result.Deleted) {

        } else if (updateResponse.result() == Result.NoOp) {

        }
    }

}
