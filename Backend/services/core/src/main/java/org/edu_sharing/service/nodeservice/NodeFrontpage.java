package org.edu_sharing.service.nodeservice;


import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.json.JsonData;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.QueryUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.admin.RepositoryConfigFactory;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.ReadableWrapperQueryBuilder;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.edu_sharing.service.search.SearchServiceElastic.WORKSPACE_INDEX;

public class NodeFrontpage {
    private Logger logger= Logger.getLogger(NodeFrontpage.class);
    private static final String INDEX_NAME = "frontpage_cache";
    private static final String TYPE_NAME = "_doc";
    private SearchService searchService= SearchServiceFactory.getLocalService();
    private NodeService nodeService=NodeServiceFactory.getLocalService();
    private PermissionService permissionService= PermissionServiceFactory.getLocalService();
    private Map<String, Date> APPLY_DATES;

    SearchServiceElastic searchServiceElastic = new SearchServiceElastic(ApplicationInfoList.getHomeRepository().getAppId());

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");

    public NodeFrontpage(){
    }


    public Collection<NodeRef> getNodesForCurrentUserAndConfig() throws Throwable {

        RepositoryConfig.Frontpage config = RepositoryConfigFactory.getConfig().frontpage;
        if(config.mode.equals(RepositoryConfig.Frontpage.Mode.collection)){
            if(config.collection==null){
                throw new RuntimeException("Frontpage mode "+RepositoryConfig.Frontpage.Mode.collection+" requires a collection id to be defined");
            }
            // only return io's
            SortDefinition sortDefinition=new SortDefinition();
            sortDefinition.addSortDefinitionEntry(
                    new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CCM_PROP_COLLECTION_ORDERED_POSITION),true),0);

            Collection<org.alfresco.service.cmr.repository.NodeRef> alfNodeRef = CollectionServiceFactory.getLocalService().getChildren(config.collection, null,sortDefinition, Collections.singletonList("files"));
            Collection<NodeRef> result = new ArrayList<>();
            alfNodeRef.stream().forEach((n)->{
                NodeRef nodeRef = new NodeRefImpl();
                nodeRef.setNodeId(n.getId());
                nodeRef.setStoreId(n.getStoreRef().getIdentifier());
                nodeRef.setStoreProtocol(n.getStoreRef().getProtocol());
                result.add(nodeRef);
            });

            return result;
        }

        BoolQuery.Builder query = new BoolQuery.Builder()
                .must(
                        m -> m.bool(b -> searchServiceElastic.getReadPermissionsQuery(b)))
                .must(
                        m -> m.term(t -> t.field("type").value("ccm:io"))
                )
                .must(
                        m -> m.term(t -> t.field("nodeRef.storeRef.protocol").value("workspace"))
                )
                .mustNot(
                        m -> m.term(t -> t.field("aspects").value("ccm:collection_io_reference"))
                );

        if(config.queries!=null && !config.queries.isEmpty()) {
            // filter all queries with matching toolpermissions, than concat them via "must"
            config.queries.stream().filter((q)->{
                if(q.condition.type.equals(RepositoryConfig.Condition.Type.TOOLPERMISSION)){
                    // should return true if query is launching
                    // so toolpermission == true && negate ? false : true -> toolpermission!=negate
                    return ToolPermissionServiceFactory.getInstance().hasToolPermission(q.condition.value)!=q.condition.negate;
                }
                return false;
            }).forEach((q)-> {
                //@TODO check config queries in extensions and fit for new index
                String queryString = QueryUtils.replaceCommonQueryParams(q.query,QueryUtils.replacerFromSyntax(MetadataReader.QUERY_SYNTAX_DSL));
                query.must(must->must.wrapper(new ReadableWrapperQueryBuilder(queryString).build()));
            });
        }


        //InputStream is = NodeFrontpage.class.getClassLoader().getResourceAsStream("frontpage-ratings.properties");
        InputStream is = NodeFrontpage.class.getClassLoader().getResource("frontpage-ratings.properties").openStream();
        String sortingScript = IOUtils.toString(is, StandardCharsets.UTF_8.name());

        Script sortingScriptInline = new Script.Builder().inline(
                i -> i.lang("painless").source(sortingScript).params("fields", getFieldNames(config))
        ).build();

        SearchRequest searchRequest = SearchRequest.of(req->req
                .index(WORKSPACE_INDEX)
                .from(0)
                // fetch more because we might need buffer for invalid permissions
                .size(config.totalCount)
                .trackTotalHits(track->track.enabled(true))
                .query(q -> q.bool(query.build()))
                .sort(
                        SortOptions.of(so -> so.script(
                                s -> s.mode(SortMode.Max).type(ScriptSortType.Number).order(SortOrder.Desc).script(sortingScriptInline))
                        )
                )
        );

        SearchResponse<Map> searchResult = searchServiceElastic.searchNative(searchRequest);
        List<NodeRef> result=new ArrayList<>();
        Set<String> authorities = searchServiceElastic.getUserAuthorities();
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        for(Hit<Map> hit : searchResult.hits().hits()){
            result.add(searchServiceElastic.transformSearchHit(authorities, user,hit.source(),false));
        }
        result = result.subList(0, result.size() > config.totalCount ? config.totalCount : result.size());
        if(config.displayCount<config.totalCount) {
            Set<NodeRef> randoms = new HashSet<>();
            // grab a random count of elements (equals displayCount) of the whole array
            while (randoms.size() < config.displayCount && randoms.size()<result.size()) {
                randoms.add(result.get(new Random().nextInt(result.size())));
            }
            return randoms;
        }
        return result;
    }

    private JsonData getFieldNames(RepositoryConfig.Frontpage config){
        List<String> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        String prefix = "";
        if(RepositoryConfig.Frontpage.Mode.rating.equals(config.mode) ){
            prefix = "statistic_RATING_";
        }else if(RepositoryConfig.Frontpage.Mode.views.equals(config.mode)){
            prefix = "statistic_VIEW_MATERIAL_";
        }else if(RepositoryConfig.Frontpage.Mode.downloads.equals(config.mode)){
            prefix = "statistic_DOWNLOAD_MATERIAL_";
        }

        if(config.timespanAll == true){
            String fieldName = prefix + "null";
            result.add(fieldName);
        }else {
            for (int i = 0; i < config.timespan; i++) {
                if(i > 0){
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                }
                String dateStr = sdfDate.format(cal.getTime());
                String fieldName = prefix + dateStr;
                result.add(fieldName);
            }
        }
        return JsonData.of(result);
    }
}
