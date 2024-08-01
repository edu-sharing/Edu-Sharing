package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import net.sourceforge.cardme.engine.VCardEngine;
import net.sourceforge.cardme.vcard.VCard;
import net.sourceforge.cardme.vcard.types.ExtendedType;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.impl.model.PermissionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.metadataset.v2.tools.MetadataElasticSearchHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.LogTime;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.restservices.shared.Contributor;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.service.admin.SystemFolder;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.model.CollectionRef;
import org.edu_sharing.service.model.CollectionRefImpl;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.nodeservice.PropertiesGetInterceptor;
import org.edu_sharing.service.nodeservice.PropertiesInterceptorFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SearchVCard;
import org.edu_sharing.service.search.model.SharedToMeType;
import org.edu_sharing.service.search.model.SortDefinition;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchServiceElastic extends SearchServiceImpl {
    public static final String WORKSPACE_INDEX = "workspace_9.0";
    static RestClient restClient;
    static ElasticsearchClient client;

    public SearchServiceElastic(String applicationId) {
        super(applicationId);
    }

    Logger logger = Logger.getLogger(SearchServiceElastic.class);

    ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
    Repository repositoryHelper = (Repository) alfApplicationContext.getBean("repositoryHelper");

    ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    PermissionModel permissionModel = (PermissionModel) alfApplicationContext.getBean("permissionsModelDAO");

    public static int MAX_RESPONSE_ENTITY_SIZE = -1;

    public static HttpHost[] getConfiguredHosts() {
        try {
            List<String> servers = LightbendConfigLoader.get().getStringList("elasticsearch.servers");
            List<HttpHost> hosts = new ArrayList<>();
            for (String server : servers) {
                hosts.add(new HttpHost(server.split(":")[0], Integer.parseInt(server.split(":")[1])));
            }
            return hosts.toArray(new HttpHost[0]);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static BoolQuery getFilesSharedToMeQuery(MetadataQueries queries, SharedToMeType type) {
        String username = AuthenticationUtil.getFullyAuthenticatedUser();
        Set<String> memberships = getAllMemberships(username);
        String basequery = queries.findQuery("sharedToMe").getBasequery().get(null);

        BoolQuery.Builder builder = QueryBuilders.bool()
                .must(b -> b.bool(b2 -> b2.mustNot(
                                b3 -> b3.match(m -> m.field("properties.ccm:ph_users.keyword").query(username))
                        ).mustNot(
                                b3 -> b3.match(m -> m.field("properties.cm:creator.keyword").query(username))
                        )
                ));
        if(StringUtils.isNotBlank(basequery)) {
            builder.must(b -> b.wrapper(new ReadableWrapperQueryBuilder(basequery).build()));
        }

        builder.minimumShouldMatch("1");
        if (type.equals(SharedToMeType.All)) {
            memberships.forEach(m ->
                    builder.should(QueryBuilders.match().field("properties.ccm:ph_invited.keyword").query(m).build()._toQuery())
            );
        } else if (type.equals(SharedToMeType.Private)) {
            builder.should(QueryBuilders.match().field("properties.ccm:ph_invited.keyword").query(username).build()._toQuery());
        }
        return builder.build();
    }

    @NotNull
    private static Set<String> getAllMemberships(String username) {
        ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
        Set<String> memberships = new HashSet<>();
        memberships.add(username);
        memberships.addAll(serviceRegistry.getAuthorityService().getAuthorities());
        memberships.remove(CCConstants.AUTHORITY_GROUP_EVERYONE);
        return memberships;
    }

    public static BoolQuery getFilesSharedByMeQuery(MetadataQueries queries) {
        String username = AuthenticationUtil.getFullyAuthenticatedUser();
        String basequery = queries.findQuery("sharedByMe").getBasequery().get(null);
        BoolQuery.Builder builder = QueryBuilders.bool()
                .must(b -> b.bool(b2 -> b2.must(
                        b3 -> b3.match(m -> m.field("properties.ccm:ph_users.keyword").query(username))
                )));
        if(StringUtils.isNotBlank(basequery)) {
            builder.must(b -> b.wrapper(new ReadableWrapperQueryBuilder(basequery).build()));
        }
        return builder.build();
    }

    public SearchResultNodeRefElastic searchDSL(String dsl) throws Throwable {
        checkClient();
        Request request = new Request("GET", WORKSPACE_INDEX + "/_search");
        request.setJsonEntity(dsl);
        JSONObject response = new JSONObject(EntityUtils.toString(restClient.performRequest(request).getEntity()));
        SearchResultNodeRefElastic sr = new SearchResultNodeRefElastic();
        List<NodeRef> data = new ArrayList<>();
        sr.setData(data);
        sr.setElasticResponse(response);
        JSONObject hits = response.getJSONObject("hits");
        int total = hits.getJSONObject("total").getInt("value");
        logger.info("result count: " + total);
        sr.setNodeCount(total);
        JSONArray hitsList = hits.getJSONArray("hits");
        Set<String> authorities = getUserAuthorities();
        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();
        for (int i = 0; i < hitsList.length(); i++) {
            Map hit = new ObjectMapper().readValue(hitsList.getJSONObject(i).getJSONObject("_source").toString(), Map.class);
            data.add(transformSearchHit(authorities, user, hit, false));
        }
        return sr;
    }

    private TransportOptions.Builder getRequestOptions(TransportOptions.Builder bld) {
        // add trace headers to elastic request
        Context context = Context.getCurrentInstance();
        if (context != null) {
            for (Map.Entry<String, String> header : context.getB3().getX3Headers().entrySet()) {
                bld.addHeader(header.getKey(), header.getValue());
            }
        }
        return bld;
    }

    public BoolQuery.Builder getPermissionsQuery(BoolQuery.Builder builder, String field) {
        Set<String> authorities = getUserAuthorities();
        return getPermissionsQuery(builder, field, authorities);
    }

    public BoolQuery.Builder getPermissionsQuery(BoolQuery.Builder audienceQueryBuilder, String field, Set<String> authorities) {
        audienceQueryBuilder.minimumShouldMatch("1");
        for (String a : authorities) {
            audienceQueryBuilder.should(should -> should.match(match -> match.field(field).query(a)));
        }
        return audienceQueryBuilder;
    }

    public BoolQuery.Builder getReadPermissionsQuery(BoolQuery.Builder builder) {
        if (AuthorityServiceHelper.isAdmin() || AuthenticationUtil.isRunAsUserTheSystemUser()) {
            return new BoolQuery.Builder().must(q -> q.matchAll(all -> all));
        }

        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();

        //enhance to collection permissions
        // @TODO: FIX after DESP-840
        BoolQuery collectionPermissions = getPermissionsQuery(QueryBuilders.bool(), "collections.permissions.read")
                .should(s -> s.match(m -> m.field("collections.owner").query(user)))
                .build();

        BoolQuery proposalPermissions = getPermissionsQuery(QueryBuilders.bool(), "collections.permissions.Coordinator", getUserAuthorities().stream().filter(a -> !a.equals(CCConstants.AUTHORITY_GROUP_EVERYONE)).collect(Collectors.toSet()))
                .should(s -> s.match(m -> m.field("collections.owner").query(user)))
                .must(must -> must.match(match -> match.field("collections.relation.type").query("ccm:collection_proposal")))
                .build();


        return getPermissionsQuery(builder, "permissions.read")
                .should(q -> q.match(m -> m.field("owner").query(user)))
                .should(audienceQueryBuilderCollections -> audienceQueryBuilderCollections
                        .bool(b -> b
                                .minimumShouldMatch("1")
                                .should(bs -> bs.bool(bb -> bb
                                        // restricted access is true -> do not inherit rights
                                        .mustNot(m -> m.term(t -> t.field("properties.ccm:restricted_access.keyword").value(true)))
                                        .must(m -> m.bool(subPermission -> subPermission
                                                .minimumShouldMatch("1")
                                                .should(q -> q.nested(nested->nested.path("collections").query(nq->nq.bool(collectionPermissions))))
                                                .should(q -> q.nested(nested->nested.path("collections").query(nq->nq.bool(proposalPermissions))))
                                        )))
                                ).should(bs -> bs.bool(bb -> bb
                                        // restricted access is "true" BUT "ReadAll" is given -> so behave like it would be false
                                        .must(m -> m.term(t -> t.field("properties.ccm:restricted_access.keyword").value(true)))
                                        .must(m -> m.term(t -> t.field("properties.ccm:restricted_access_permissions.keyword").value(CCConstants.PERMISSION_READ_ALL)))
                                        .must(m -> m.bool(subPermission -> subPermission
                                                .minimumShouldMatch("1")
                                                .should(q -> q.nested(nested->nested.path("collections").query(nq->nq.bool(collectionPermissions))))
                                                .should(q -> q.nested(nested->nested.path("collections").query(nq->nq.bool(proposalPermissions))))
                                        )))
                                )
                        ));
    }

    public SearchResultNodeRef searchFacets(MetadataSet mds, String query, Map<String, String[]> criterias, SearchToken searchToken) throws Throwable {

        MetadataQuery queryData = mds.findQuery(query, MetadataReader.QUERY_SYNTAX_DSL);
        Query globalConditions = new Query.Builder()
                .bool(getGlobalConditions(searchToken.getAuthorityScope(), searchToken.getPermissions(), queryData).build())
                .build();

        Set<MetadataQueryParameter> excludeOwnFacets = MetadataElasticSearchHelper.getExcludeOwnFacets(queryData, new HashMap<>(), searchToken.getFacets());
        Map<String, Aggregation> aggregations = MetadataElasticSearchHelper.getAggregations(
                mds,
                queryData,
                criterias,
                searchToken.getFacets(),
                excludeOwnFacets,
                globalConditions,
                searchToken);

        SearchResultNodeRef result = parseAggregations(searchToken, aggregations);
        result.setFacets(result.getFacets().stream().map(facet -> {
            facet.setValues(facet.getValues().stream().filter(s -> {
                // if one document has i.e. multiple keywords, they will be shown in the facet
                // so, we filter for values which actually contain the given string
                {
                    try {
                        String value = s.getValue();
                        try {
                            // map to i18n value if available
                            value = mds.findWidget(facet.getProperty()).getValuesAsMap().get(value).getCaption();
                        } catch (Throwable ignored) {
                        }
                        return value.toLowerCase().contains(searchToken.getQueryString().toLowerCase());
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).distinct().limit(searchToken.getFacetLimit()).collect(Collectors.toList()));
            return facet;
        }).collect(Collectors.toList()));
        return result;
    }

    @NotNull
    private SearchResultNodeRef parseAggregations(SearchToken searchToken, Map<String, Aggregation> aggregations) throws Exception {
//        logger.info("query aggs: "+searchSourceBuilderAggs.toString());
        SearchResponse<Map> resp = LogTime.log("Searching elastic for facets", () -> client.search(req -> req
                        .index(WORKSPACE_INDEX)
                        .from(0)
                        .size(0)
                        .aggregations(aggregations)
                , Map.class));

        List<NodeSearch.Facet> facetsResult = new ArrayList<>();
        for (Map.Entry<String, Aggregate> a : resp.aggregations().entrySet()) {
            if (a.getValue().isFilter()) {
                FilterAggregate pf = a.getValue().filter();
                for (Map.Entry<String, Aggregate> aggregation : pf.aggregations().entrySet()) {
                    if (aggregation.getValue().isSterms()) {
                        Aggregation definition = aggregations.get(a.getKey());
                        StringTermsAggregate sterms = aggregation.getValue().sterms();
                        facetsResult.add(getFacet(aggregation.getKey(), sterms, definition));
                    } else if (aggregation.getValue().isNested()) {
                        Aggregation definition = aggregations.get(a.getKey());
                        NestedAggregate nested = aggregation.getValue().nested();
                        StringTermsAggregate sterms = nested.aggregations().values().stream().findFirst().get().sterms();
                        facetsResult.add(getFacet(aggregation.getKey(), sterms, definition));
                    } else if(aggregation.getValue().isMultiTerms()) {
                        Aggregation definition = aggregations.get(a.getKey());
                        MultiTermsAggregate multiTerm = aggregation.getValue().multiTerms();
                        facetsResult.add(getMultitermFacet(aggregation.getKey(), multiTerm, definition));
                    }
                }
            } else {
                logger.error("non supported aggregation " + a.getKey());
            }
        }

        SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
        searchResultNodeRef.setData(new ArrayList<>());
        searchResultNodeRef.setFacets(facetsResult);
        searchResultNodeRef.setStartIDX(searchToken.getFrom());
        searchResultNodeRef.setNodeCount(0);

        return searchResultNodeRef;
    }

    private NodeSearch.Facet getMultitermFacet(String name, MultiTermsAggregate mta, Aggregation definition) {
        NodeSearch.Facet facet = new NodeSearch.Facet();
        facet.setProperty(name);
        List<NodeSearch.Facet.Value> values = new ArrayList<>();
        facet.setValues(values);

        for (MultiTermsBucket b : mta.buckets().array()) {
            for (FieldValue fv : b.key()) {
                long count = b.docCount();
                NodeSearch.Facet.Value value = new NodeSearch.Facet.Value();
                // skip duplicate entries
                if(values.stream().anyMatch(v -> v.getValue().equals(fv.stringValue()))) {
                    continue;
                }
                value.setValue(fv.stringValue());
                value.setCount((int) count);
                values.add(value);
            }
        }
        facet.setSumOtherDocCount(mta.sumOtherDocCount());
        return facet;
    }

    /**
     * fetches all nodes with the given query using the scroll api
     * ignores maxCount & skipCount set!
     * Does not evaluate any suggestions or facettes, only returns nodes
     *
     * @param mds
     * @param query
     * @param criterias
     * @param searchToken
     * @return
     * @throws Throwable
     */
    public List<NodeRef> searchAll(MetadataSet mds, String query, Map<String, String[]> criterias,
                                   SearchToken searchToken) throws Throwable {
        checkClient();
        MetadataQuery queryData = mds.findQuery(query, MetadataReader.QUERY_SYNTAX_DSL);

        Set<String> authorities = getUserAuthorities();
        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();


        SearchResultNodeRef sr = new SearchResultNodeRef();
        List<NodeRef> data = new ArrayList<>();
        sr.setData(data);


        BoolQuery.Builder metadataQueryBuilderFilter = MetadataElasticSearchHelper.getElasticSearchQuery(searchToken, mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, criterias, true);
        BoolQuery.Builder metadataQueryBuilderAsQuery = MetadataElasticSearchHelper.getElasticSearchQuery(searchToken, mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, criterias, false);
        BoolQuery.Builder queryBuilderGlobalConditions = getGlobalConditions(searchToken.getAuthorityScope(), searchToken.getPermissions(), queryData);

        SearchRequest.Builder searchRequest = new SearchRequest.Builder()
                .index(WORKSPACE_INDEX)
                .scroll(Time.of(time -> time.time("60s")))
                .source(src -> src
                        .filter(filter -> filter.excludes(appendDefaultExcludes(searchToken.getExcludes())))
                )
                .size(100)
                .query(q->q
                        .bool(b->b
                                .filter(filter->filter
                                        .bool(fb->fb
                                                .must(fq->fq.bool(metadataQueryBuilderFilter.build()))
                                                .must(fq->fq.bool(queryBuilderGlobalConditions.build()))))
                                .must(must->must.bool(metadataQueryBuilderAsQuery.build()))));

        if (searchToken.getSortDefinition() != null) {
            searchToken.getSortDefinition().applyToSearchSourceBuilder(searchRequest);
        }

        try {
            String scrollId = null;
            while (true) {
                ResponseBody<Map> searchResponse;
                if (scrollId == null) {
                    searchResponse = client
                            .withTransportOptions(this::getRequestOptions)
                            .search(searchRequest.build(), Map.class);
                } else {
                    final String usedScrollId = scrollId;
                    searchResponse = client
                            .withTransportOptions(this::getRequestOptions)
                            .scroll(scroll -> scroll.scrollId(usedScrollId).scroll(t -> t.time("60s")), Map.class);
                }
                scrollId = searchResponse.scrollId();

                HitsMetadata<Map> hits = searchResponse.hits();
                for (Hit<Map> hit : hits.hits()) {
                    data.add(transformSearchHit(authorities, user, hit.source(), searchToken.isResolveCollections()));
                }
                if (hits.hits().isEmpty()) {
                    break;
                }
            }
        } catch (ElasticsearchException e) {
            logger.error("Error running query. The query is logged below for debugging reasons");
            logger.error(e.getMessage(), e);
            logger.error(searchRequest.toString());
            throw e;
        }
        logger.info("result count: " + data.size());
        return data;
    }

    private List<String> appendDefaultExcludes(List<String> excludes){
        if(excludes == null) excludes = new ArrayList<>();
        else excludes = new ArrayList<>(excludes);

        if(!excludes.contains("content.fulltext")){
            excludes.add("content.fulltext");
        }
        return excludes;
    }

    @Override
    public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criterias,
                                      SearchToken searchToken) throws Throwable {

        checkClient();
        MetadataQuery queryData;
        try {
            queryData = mds.findQuery(query, MetadataReader.QUERY_SYNTAX_DSL);
        } catch (IllegalArgumentException e) {
            logger.info("Query " + query + " is not defined within dsl language, switching to lucene...");
            return super.search(mds, query, criterias, searchToken);
        }

        Set<String> authorities = getUserAuthorities();
        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();


        SearchResultNodeRef sr = new SearchResultNodeRef();
        List<NodeRef> data = new ArrayList<>();
        sr.setData(data);
        try {

            BoolQuery metadataQueryBuilderFilter = MetadataElasticSearchHelper.getElasticSearchQuery(searchToken, mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, criterias, true).build();
            BoolQuery metadataQueryBuilderAsQuery = MetadataElasticSearchHelper.getElasticSearchQuery(searchToken, mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, criterias, false).build();
            BoolQuery queryBuilderGlobalConditions = getGlobalConditions(searchToken.getAuthorityScope(), searchToken.getPermissions(), queryData).build();

            // add collapse builder
            // CollapseBuilder collapseBuilder = new CollapseBuilder("properties.ccm:original");
            // searchSourceBuilder.collapse(collapseBuilder);
            // cardinality aggregation to get correct total count
            // https://github.com/elastic/elasticsearch/issues/24130
            // CardinalityAggregationBuilder original_count = AggregationBuilders.cardinality("original_count").field("properties.ccm:original");
            // searchSourceBuilder.aggregation(original_count);


            SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                    .index(WORKSPACE_INDEX)
                    .source(src -> src.filter(filter -> filter.excludes(appendDefaultExcludes(searchToken.getExcludes()))));

            SearchResponse<Map> searchResponseAggregations = null;
            if (searchToken.getFacets() != null) {
                Set<MetadataQueryParameter> excludeOwnFacets = MetadataElasticSearchHelper.getExcludeOwnFacets(queryData, criterias, searchToken.getFacets());
                if (!excludeOwnFacets.isEmpty()) {
                    Map<String, Aggregation> aggregations = MetadataElasticSearchHelper.getAggregations(
                            mds,
                            queryData,
                            criterias,
                            searchToken.getFacets(),
                            excludeOwnFacets,
                            queryBuilderGlobalConditions._toQuery(),
                            searchToken);

                    SearchRequest searchSourceAggs = SearchRequest.of(req -> req
                            .index(WORKSPACE_INDEX)
                            .from(0)
                            .size(0)
                            .aggregations(aggregations));

                    logger.info("query aggs: " + JsonpUtils.toJsonString(searchSourceAggs, new JacksonJsonpMapper()));
                    searchResponseAggregations = LogTime.log("Searching elastic for facets", () -> client.search(searchSourceAggs, Map.class));
                } else {
                    for (String facet : searchToken.getFacets()) {
                        // we use a higher facet limit since the facets will be filtered for the containing string!
                        searchRequestBuilder.aggregations(facet, b -> b.terms(t -> t
                                .field("properties." + facet + ".keyword")
                                .size(searchToken.getFacetLimit() * MetadataElasticSearchHelper.FACET_LIMIT_MULTIPLIER)
                                .minDocCount(searchToken.getFacetsMinCount())));
                    }
                }
            }

            if (searchToken.isReturnSuggestion()) {
                String[] ngsearches = criterias.get("ngsearchword");
                if (ngsearches != null) {
                    searchRequestBuilder.suggest(suggest -> suggest
                            .text(ngsearches[0])
                            .suggesters("ngsearchword", s -> s
                                    .phrase(p -> p
                                            .text("properties.cclom:title.trigram")
                                            .gramSize(3)
                                            .confidence(0.9)
                                            .highlight(high -> high.preTag("<em>").postTag("</em>"))
                                            .directGenerator(x -> x
                                                    .field("properties.cclom:title.trigram")
                                                    .suggestMode(SuggestMode.Popular))
                                            .smoothing(smooth -> smooth.laplace(l -> l.alpha(0.5))))));
                }
            }


            searchRequestBuilder.query(q -> q
                    .bool(b -> b
                            .filter(filter -> filter
                                    .bool(bool -> bool
                                            .must(must -> must.bool(metadataQueryBuilderFilter))
                                            .must(must -> must.bool(queryBuilderGlobalConditions))))
                            .must(must -> must.bool(metadataQueryBuilderAsQuery))));

            searchRequestBuilder.from(searchToken.getFrom());
            searchRequestBuilder.size(searchToken.getMaxResult());
            searchRequestBuilder.trackTotalHits(new TrackHits.Builder().enabled(true).build());
            if (searchToken.getSortDefinition() != null) {
                searchToken.getSortDefinition().applyToSearchSourceBuilder(searchRequestBuilder);
            }

            // logger.info("query: "+searchSourceBuilder.toString());
            SearchRequest searchRequest = searchRequestBuilder.build();
            try {
                logger.info("query: " + JsonpUtils.toJsonString(searchRequest, new JacksonJsonpMapper()));
                SearchResponse<Map> searchResponse = LogTime.log("Searching elastic", () -> client.search(searchRequest, Map.class));

                HitsMetadata<Map> hits = searchResponse.hits();
                logger.info("result count: " + hits.total());

                long millisPerm = System.currentTimeMillis();
                for (Hit<Map> hit : hits.hits()) {
                    data.add(transformSearchHit(authorities, user, hit.source(), searchToken.isResolveCollections()));
                }
                logger.info("permission stuff took:" + (System.currentTimeMillis() - millisPerm));

                List<NodeSearch.Facet> facetsResult = new ArrayList<>();
                List<NodeSearch.Facet> facetsResultSelected = new ArrayList<>();

                Long total = null;

                Map<String, Aggregate> aggregations = new HashMap<>();
                if (searchResponseAggregations != null) {
                    aggregations.putAll(searchResponseAggregations.aggregations());
                }
                if (searchResponse.aggregations() != null) aggregations.putAll(searchResponse.aggregations());

                for (Map.Entry<String, Aggregate> a : aggregations.entrySet()) {
                    if (a.getValue().isSterms()) {
                        facetsResult.add(getFacet(a.getKey(), a.getValue().sterms(), null));
                    } else if (a.getValue()._kind().equals(Aggregate.Kind.Cardinality)) {
                        if (a.getKey().equals("original_count")) {
                            total = a.getValue().cardinality().value();
                        } else {
                            logger.error("unknown cardinality aggregation");
                        }

                    } else if (a.getValue().isFilter()) {
                        FilterAggregate filter = a.getValue().filter();
                        for (Map.Entry<String, Aggregate> aggregationEntry : filter.aggregations().entrySet()) {
                            if (aggregationEntry.getValue().isSterms()) {
                                if (a.getKey().endsWith("_selected")) {
                                    if (a.getValue().isFilter()) {
                                        Map<String, Aggregate> agg = a.getValue().filter().aggregations();
                                        facetsResultSelected.add(getFacet(a.getKey(), agg.entrySet().iterator().next().getValue().sterms(), null));
                                    } else {
                                        facetsResultSelected.add(getFacet(a.getKey(), a.getValue().sterms(), null));
                                    }
                                } else {
                                    if (a.getValue().isFilter()) {
                                        Map<String, Aggregate> agg = a.getValue().filter().aggregations();
                                        facetsResult.add(getFacet(a.getKey(), agg.entrySet().iterator().next().getValue().sterms(), null));
                                    } else {
                                        facetsResult.add(getFacet(a.getKey(), a.getValue().sterms(), null));
                                    }
                                }
                            }
                        }
                    } else {
                        logger.error("non supported aggreagtion " + a.getKey());
                    }
                }
                // add selected when missing
                if (searchToken.getFacets() != null && !searchToken.getFacets().isEmpty()) {
                    for (String facet : searchToken.getFacets()) {
                        if (!criterias.containsKey(facet)) {
                            continue;
                        }
                        for (String value : criterias.get(facet)) {
                            Optional<NodeSearch.Facet> facetResult = facetsResult.stream()
                                    .filter(f -> f.getProperty().equals(facet)).findFirst();
                            Optional<NodeSearch.Facet> selected = facetsResultSelected
                                    .stream()
                                    .filter(f ->
                                            f.getProperty().equals(facet))
                                    .findFirst();
                            if (selected.isPresent()) {
                                if (facetResult.isPresent()) {
                                    if (facetResult.get().getValues().stream().noneMatch(v -> v.getValue().equals(value))) {
                                        if (selected.get().getValues().stream().anyMatch(v -> value.equals(v.getValue()))) {
                                            facetResult.get().getValues().add(selected.get().getValues().stream()
                                                    .filter(v -> value.equals(v.getValue()))
                                                    .findFirst()
                                                    .get()
                                            );
                                        }
                                    }
                                } else {
                                    if (selected.get().getValues().stream().anyMatch(v -> value.equals(v.getValue()))) {
                                        facetsResult.add(selected.get());
                                    }
                                }
                            }
                        }
                    }
                }

                if (!searchResponse.suggest().isEmpty()) {
                    List<co.elastic.clients.elasticsearch.core.search.Suggestion<Map>> phraseSuggestion = searchResponse.suggest().get("ngsearchword");
                    if (!phraseSuggestion.isEmpty()) {
                        List<NodeSearch.Suggest> suggests = phraseSuggestion.stream()
                                .filter(co.elastic.clients.elasticsearch.core.search.Suggestion::isPhrase)
                                .map(co.elastic.clients.elasticsearch.core.search.Suggestion::phrase)
                                .map(PhraseSuggest::options)
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream)
                                .map(x -> {
                                    NodeSearch.Suggest suggest = new NodeSearch.Suggest();
                                    suggest.setText(x.text());
                                    suggest.setHighlighted(x.highlighted());
                                    suggest.setScore(x.score());
                                    return suggest;
                                })
                                .collect(Collectors.toList());
                        suggests.forEach(x -> logger.info("SUGGEST:" + x.getText() + " " + x.getScore() + " " + x.getHighlighted()));
                        sr.setSuggests(suggests);
                    }
                }

                if (total == null) {
                    total = Optional.of(hits).map(HitsMetadata::total).map(TotalHits::value).orElse(0L);
                }

                sr.setFacets(facetsResult);
                sr.setStartIDX(searchToken.getFrom());
                sr.setNodeCount((int) total.longValue());
                //client.close();
            } catch (ElasticsearchException e) {
                logger.error("Error running query. The query is logged below for debugging reasons");
                logger.error(e.getMessage(), e);
                logger.error(JsonpUtils.toJsonString(searchRequest, new JacksonJsonpMapper()));
                throw e;
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }


        logger.info("returning");
        return sr;
    }

    private NodeSearch.Facet getFacet(String name, StringTermsAggregate pst, Aggregation builder) {

        NodeSearch.Facet facet = new NodeSearch.Facet();
        facet.setProperty(name);
        List<NodeSearch.Facet.Value> values = new ArrayList<>();
        facet.setValues(values);

        for (StringTermsBucket b : pst.buckets().array()) {
            if (builder != null && Aggregation.Kind.MultiTerms == builder._kind()) {
                String[] key = b.key().stringValue().split("\\|");
                for (String k : key) {
                    long count = b.docCount();
                    NodeSearch.Facet.Value value = new NodeSearch.Facet.Value();
                    // skip duplicate entries
                    if(values.stream().anyMatch(v -> v.getValue().equals(k))) {
                        continue;
                    }
                    value.setValue(k);
                    value.setCount((int) count);
                    values.add(value);
                }
            } else {
                String key = b.key().stringValue();
                long count = b.docCount();
                NodeSearch.Facet.Value value = new NodeSearch.Facet.Value();
                value.setValue(key);
                value.setCount((int) count);
                values.add(value);
            }
        }

        facet.setSumOtherDocCount(pst.sumOtherDocCount());
        return facet;
    }

    /**
     * permissions, scope ...
     *
     * @param authorityScope
     * @param permissions
     * @param query
     * @return
     */
    BoolQuery.Builder getGlobalConditions(List<String> authorityScope, List<String> permissions, MetadataQuery query) {

        Function<BoolQuery.Builder, BoolQuery.Builder> queryGlobalConditionsFactory = (builder) ->
                ((authorityScope != null && !authorityScope.isEmpty())
                        ? getPermissionsQuery(builder, "permissions.read", new HashSet<>(authorityScope))
                        : getReadPermissionsQuery(builder))
                        .must(must -> must
                                .match(match -> match
                                        .field("nodeRef.storeRef.protocol")
                                        .query("workspace")));


        BoolQuery.Builder queryBuilderGlobalConditions = queryGlobalConditionsFactory.apply(QueryBuilders.bool());
        if (permissions != null) {
            BoolQuery.Builder permissionsFilter = QueryBuilders.bool().must(must -> must.bool(queryGlobalConditionsFactory::apply));
            String user = serviceRegistry.getAuthenticationService().getCurrentUserName();
            permissionsFilter.should(should -> should.match(match -> match.field("owner").query(user)));
            for (String permission : permissions) {
                permissionsFilter.should(s -> s.bool(bool -> getPermissionsQuery(bool, "permissions." + permission)));
            }
            queryBuilderGlobalConditions = permissionsFilter;
        }

        if (NodeServiceInterceptor.getEduSharingScope() == null) {
            queryBuilderGlobalConditions.mustNot(mustNot -> mustNot.exists(exist -> exist.field("properties.ccm:eduscopename")));
        } else {
            queryBuilderGlobalConditions.must(must -> must.term(term -> term.field("properties.ccm:eduscopename.keyword").value(NodeServiceInterceptor.getEduSharingScope())));
        }
        // mds specialFilter processing on per-query basis
        if (query != null) {
            for (MetadataQuery.SpecialFilter filter : query.getSpecialFilter()) {
                if (MetadataQuery.SpecialFilter.exclude_system_folder.equals(filter)) {
                    queryBuilderGlobalConditions.mustNot(mustNot -> mustNot.wildcard(wild -> wild.field("fullpath").value("*/" + SystemFolder.getSystemFolderBase().getId() + "*")));
                } else if (MetadataQuery.SpecialFilter.exclude_sites_folder.equals(filter)) {
                    queryBuilderGlobalConditions.mustNot(mustNot -> mustNot.wildcard(wild -> wild.field("fullpath").value("*/" + SystemFolder.getSitesFolder().getId() + "*")));
                } else if (MetadataQuery.SpecialFilter.exclude_people_folder.equals(filter)) {
                    org.alfresco.service.cmr.repository.NodeRef personFolder = SystemFolder.getPersonFolder();
                    if (personFolder != null) {
                        queryBuilderGlobalConditions.mustNot(mustNot -> mustNot.wildcard(wild -> wild.field("fullpath").value("*/" + SystemFolder.getPersonFolder().getId() + "*")));
                    } else {
                        logger.warn("People folder unknown, elastic query is skipping special filter");
                    }
                }
            }
        }
        return queryBuilderGlobalConditions;
    }

    public Set<String> getUserAuthorities() {
        Set<String> authorities = serviceRegistry.getAuthorityService().getAuthorities();
        authorities.add(CCConstants.AUTHORITY_GROUP_EVERYONE);
        if (!AuthenticationUtil.isRunAsUserTheSystemUser()) {
            authorities.add(AuthenticationUtil.getFullyAuthenticatedUser());
        }
        return authorities;
    }

    public boolean isAllowedToRead(String nodeId) {
        boolean result = hasReadPermissionOnNode(nodeId);
        if (result) return true;

//        BoolQueryBuilder checkIsChildObjectQuery = QueryBuilders.boolQuery()
//                .must(QueryBuilders.termQuery("properties.sys:node-uuid", nodeId))
//                .must(QueryBuilders.termQuery("aspects", "ccm:io_childobject"));
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        searchSourceBuilder.query(checkIsChildObjectQuery);
//        SearchRequest request = new SearchRequest("workspace");
//        request.source(searchSourceBuilder);
        try {
            SearchResponse<Map> searchResult = client
                    .withTransportOptions(this::getRequestOptions)
                    .search(req -> req
                            .index(WORKSPACE_INDEX)
                            .trackTotalHits(t -> t.enabled(true))
                            .query(query -> query
                                    .bool(checkIsChildObjectQuery -> checkIsChildObjectQuery
                                            .must(must -> must.term(term -> term.field("properties.sys:node-uuid").value(nodeId)))
                                            .must(must -> must.term(term -> term.field("aspects").value("ccm:io_childobject"))))), Map.class);

            if (searchResult.hits().total().value() == 0) {
                return false;
            }

            Map source = searchResult.hits().hits().get(0).source();
            if (source == null) {
                return false;
            }

            Map parentRef = (Map) source.get("parentRef");
            String parentId = (String) parentRef.get("id");
            return hasReadPermissionOnNode(parentId);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean hasReadPermissionOnNode(String nodeId) {
        try {
            checkClient();
            SearchResponse<Map> searchResult = client
                    .withTransportOptions(this::getRequestOptions)
                    .search(request->request
                            .index(WORKSPACE_INDEX)
                            .size(0)
                            .trackTotalHits(t->t.enabled(true))
                            .query(query->query
                                    .bool(bool->bool
                                            .must(must -> must.bool(this::getReadPermissionsQuery))
                                            .must(must -> must.term(term->term.field("properties.sys:node-uuid").value(nodeId))))), Map.class);
            return searchResult.hits().total().value() != 0;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return false;
    }

    public NodeRef transformSearchHit(Set<String> authorities, String user, Map hit, boolean resolveCollections) {
        try {
            return this.transform(NodeRefImpl.class, authorities, user, hit, resolveCollections);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends NodeRefImpl> T transform(Class<T> clazz, Set<String> authorities, String user, Map<String, Object> sourceAsMap, boolean resolveCollections) throws IllegalAccessException, InstantiationException {
        boolean isAdmin = AuthorityServiceHelper.isAdmin();
        Map<String, MetadataSet> mdsCache = new HashMap<>();
        String currentLocale = new AuthenticationToolAPI().getCurrentLocale();

        Map<String, Serializable> properties = (Map) sourceAsMap.get("properties");

        Map nodeRef = (Map) sourceAsMap.get("nodeRef");
        String nodeId = (String) nodeRef.get("id");

        Map parentRef = (Map) sourceAsMap.get("parentRef");
        String parentId = (parentRef != null) ? (String) parentRef.get("id") : null;


        Map storeRef = (Map) nodeRef.get("storeRef");
        String protocol = (String) storeRef.get("protocol");
        String identifier = (String) storeRef.get("identifier");

        Map<String, Object> props = new HashMap<>();

        for (Map.Entry<String, Serializable> entry : properties.entrySet()) {

            Serializable value = null;
            /**
             * @TODO: transform to ValueTool.toMultivalue
             */
            if (entry.getValue() instanceof ArrayList) {
                ArrayList<?> list = (ArrayList<?>) entry.getValue();
                if (list.size() > 1 && list.get(0) instanceof String) {
                    value = ValueTool.toMultivalue(list.toArray(new String[0]));
                } else if (list.size() == 1) {
                    value = (Serializable) ((ArrayList<?>) entry.getValue()).get(0);
                }
            } else {
                value = entry.getValue();
            }
            if (entry.getKey().equals("ccm:mediacenter")) {
                List<Map<String, Object>> mediacenterStatus = (List<Map<String, Object>>) entry.getValue();
                ArrayList<String> result = new ArrayList<>();
                for (Map<String, Object> mcSt : mediacenterStatus) {
                    Gson gson = new Gson();
                    String json = gson.toJson(mcSt);
                    result.add(json);
                }
                value = ValueTool.toMultivalue(result.toArray(new String[result.size()]));
            }
            if (entry.getKey().equals("cm:created") || entry.getKey().equals("cm:modified") && value != null) {
                props.put(CCConstants.getValidGlobalName(entry.getKey()) + CCConstants.LONG_DATE_SUFFIX, ((Long) value).toString());
            }
            props.put(CCConstants.getValidGlobalName(entry.getKey()), value);

            /**
             * metadataset translation
             */
            Map<String, Serializable> i18n = (Map<String, Serializable>) sourceAsMap.get("i18n");
            if (i18n != null) {
                Map<String, Serializable> i18nProps = (Map<String, Serializable>) i18n.get(currentLocale);
                if (i18nProps != null) {
                    List<String> displayNames = (List<String>) i18nProps.get(entry.getKey());
                    if (displayNames != null) {
                        props.put(CCConstants.getValidGlobalName(entry.getKey()) + CCConstants.DISPLAYNAME_SUFFIX, StringUtils.join(displayNames, CCConstants.MULTIVALUE_SEPARATOR));
                    }
                }
            } else {
                try {
                    String mdsId = (String) properties.getOrDefault(
                            CCConstants.getValidLocalName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET),
                            CCConstants.metadatasetdefault_id);
                    MetadataSet mds = mdsCache.get(mdsId);
                    if (mds == null) {
                        mds = MetadataHelper.getMetadataset(
                                ApplicationInfoList.getHomeRepository(),
                                mdsId
                        );
                        mdsCache.put(mdsId, mds);
                    }

                    MetadataHelper.addVirtualDisplaynameProperties(mds, props);
                } catch (Throwable t) {
                    logger.info("Could not resolve displaynames: " + t.getMessage());
                }
            }
        }
        props.put(CCConstants.NODETYPE, sourceAsMap.get("type"));

        List<Map<String, Serializable>> children = (List) sourceAsMap.get("children");
        int childIOCount = 0;
        int usageCount = 0;
        int commentCount = 0;
        if (children != null) {
            for (Map<String, Serializable> child : children) {
                String type = (String) child.get("type");
                List<String> aspects = (List<String>) child.get("aspects");
                if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO).equals(type)
                        && aspects.contains(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT))) {
                    childIOCount++;
                }
                if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_USAGE).equals(type)) {
                    usageCount++;
                }
                if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_COMMENT).equals(type)) {
                    commentCount++;
                }
            }
        }
        if (childIOCount > 0) {
            props.put(CCConstants.VIRT_PROP_CHILDOBJECTCOUNT, childIOCount);
        }
        if (usageCount > 0) {
            props.put(CCConstants.VIRT_PROP_USAGECOUNT, usageCount);
        }
        if (commentCount > 0) {
            props.put(CCConstants.VIRT_PROP_COMMENTCOUNT, commentCount);
        }


        org.alfresco.service.cmr.repository.NodeRef alfNodeRef = new org.alfresco.service.cmr.repository.NodeRef(new StoreRef(protocol, identifier), nodeId);
        String contentUrl = URLHelper.getNgRenderNodeUrl(nodeId, null);
        contentUrl = URLTool.addOAuthAccessToken(contentUrl);
        props.put(CCConstants.CONTENTURL, contentUrl);

        if (sourceAsMap.get("content") != null) {
            props.put(CCConstants.DOWNLOADURL, URLTool.getDownloadServletUrl(alfNodeRef.getId(), null, true));
        }

        if (parentId != null) {
            props.put(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID, parentId);
        }

        T eduNodeRef = clazz.newInstance();
        eduNodeRef.setRepositoryId(ApplicationInfoList.getHomeRepository().getAppId());
        ;
        eduNodeRef.setStoreProtocol(protocol);
        eduNodeRef.setStoreId(identifier);
        eduNodeRef.setNodeId(nodeId);

        eduNodeRef.setAspects(((List<String>) sourceAsMap.get("aspects")).
                stream().map(CCConstants::getValidGlobalName).filter(Objects::nonNull).collect(Collectors.toList()));

        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put(CCConstants.PERMISSION_READ, true);
        String guestUser = ApplicationInfoList.getHomeRepository().getGuest_username();
        long millis = System.currentTimeMillis();
        eduNodeRef.setPublic(false);
        Map<String, List<String>> permissionsElastic = (Map) sourceAsMap.get("permissions");
        String owner = (String) sourceAsMap.get("owner");
        for (Map.Entry<String, List<String>> entry : permissionsElastic.entrySet()) {
            if ("read".equals(entry.getKey())) {
                continue;
            }
            if (!eduNodeRef.getPublic() && guestUser != null && entry.getValue().contains(CCConstants.AUTHORITY_GROUP_EVERYONE)) {
                PermissionReference pr = permissionModel.getPermissionReference(null, entry.getKey());
                Set<PermissionReference> granteePermissions = permissionModel.getGranteePermissions(pr);
                eduNodeRef.setPublic(granteePermissions.stream().anyMatch(p -> p.getName().equals(CCConstants.PERMISSION_READ_ALL)));
            }
            if (authorities.stream().anyMatch(s -> entry.getValue().contains(s))
                    || entry.getValue().contains(user)) {
                //get fine grained permissions
                PermissionReference pr = permissionModel.getPermissionReference(null, entry.getKey());
                Set<PermissionReference> granteePermissions = permissionModel.getGranteePermissions(pr);
                for (String perm : PermissionServiceHelper.PERMISSIONS) {
                    for (PermissionReference pRef : granteePermissions) {
                        if (pRef.getName().equals(perm)) {
                            permissions.put(perm, true);
                        }
                    }
                }
            }
        }

        // @TODO: remove all of this from/to multivalue
        ValueTool.getMultivalue(props);
        PropertiesGetInterceptor.PropertiesContext propertiesContext = PropertiesInterceptorFactory.getPropertiesContext(
                alfNodeRef, props, eduNodeRef.getAspects(),
                permissions,
                sourceAsMap
        );
        for (PropertiesGetInterceptor i : PropertiesInterceptorFactory.getPropertiesGetInterceptors()) {
            props = new HashMap<>(i.beforeDeliverProperties(propertiesContext));
        }
        // @TODO: remove all of this from/to multivalue
        ValueTool.toMultivalue(props);
        eduNodeRef.setProperties(props);

        eduNodeRef.setOwner((String) sourceAsMap.get("owner"));

        Map preview = (Map) sourceAsMap.get("preview");
        if (preview != null && preview.get("small") != null) {
            eduNodeRef.setPreview(
                    new NodeRefImpl.PreviewImpl(
                            (String) preview.get("mimetype"),
                            Base64.getDecoder().decode((String) preview.get("small")),
                            (String) preview.get("type"),
                            (Boolean) preview.get("icon")
                    )
            );
        }

        List<Contributor> contributorsResult = new ArrayList<>();
        List contributors = (List) sourceAsMap.get("contributor");
        if (contributors != null) {
            for (Object contributor : contributors) {
                Map c = (Map) contributor;
                Contributor contributorResult = new Contributor();
                contributorResult.setProperty((String) c.get("property"));
                contributorResult.setEmail((String) c.get("email"));
                contributorResult.setFirstname((String) c.get("firstname"));
                contributorResult.setLastname((String) c.get("lastname"));
                contributorResult.setOrg((String) c.get("org"));
                contributorResult.setVcard((String) c.get("vcard"));
                contributorsResult.add(contributorResult);
            }
        }
        eduNodeRef.setContributors(contributorsResult);


        if (isAdmin || user.equals(owner)) {
            permissions.put(CCConstants.PERMISSION_CC_PUBLISH, true);
            PermissionReference pr = permissionModel.getPermissionReference(null, "FullControl");
            Set<PermissionReference> granteePermissions = permissionModel.getGranteePermissions(pr);
            for (String perm : PermissionServiceHelper.PERMISSIONS) {
                for (PermissionReference pRef : granteePermissions) {
                    if (pRef.getName().equals(perm)) {
                        permissions.put(perm, true);
                    }
                }
            }

            //Set<PermissionReference> granteePermissions = permissionModel.getGranteePermissions(pr);
            //Set<PermissionReference> immediateGranteePermissions = permissionModel.getImmediateGranteePermissions(pr);

        }
        // check if user has access via any collection and grant him all usage permissions
        processCollectionUsagePermissions(authorities, user, sourceAsMap, permissions);

        eduNodeRef.setPermissions(permissions);
        boolean isProposal = sourceAsMap.get("type").equals(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_COLLECTION_PROPOSAL));
        if (resolveCollections) {
            List<Map<String, Object>> collections = (List) sourceAsMap.get("collections");
            if (collections != null) {
                for (Map<String, Object> collection : collections) {
                    String colOwner = (String) collection.get("owner");
                    boolean hasPermission = user.equals(colOwner) || isAdmin;
                    if (!hasPermission) {
                        Map<String, List<String>> colPermissionsElastic = (Map) collection.get("permissions");
                        for (Map.Entry<String, List<String>> entry : colPermissionsElastic.entrySet()) {
                            if ("read".equals(entry.getKey())) {
                                hasPermission = entry.getValue().stream().anyMatch(s -> authorities.contains(s) || s.equals(user));
                                break;
                            }
                        }
                    }
                    if (hasPermission) {
                        CollectionRefImpl transform = transform(CollectionRefImpl.class, authorities, user, collection, false);
                        if (isProposal) {
                            transform.setRelationType(CollectionRef.RelationType.Proposal);
                        }
                        eduNodeRef.getUsedInCollections().add(transform);
                    }
                }
            }
        }
        if (isProposal && sourceAsMap.containsKey("original")) {
            eduNodeRef.getRelations().put(
                    NodeRefImpl.Relation.Original,
                    transform(NodeRefImpl.class, authorities, user, (Map) sourceAsMap.get("original"), false)
            );
        }
        if (eduNodeRef instanceof CollectionRefImpl) {
            CollectionRefImpl collectionRef = (CollectionRefImpl) eduNodeRef;
            Map<String, Object> relation = (Map) sourceAsMap.get("relation");
            if (relation != null) {
                // @TODO: transform relation type
                Map<String, Object> relationProps = (Map) relation.get("properties");
                if (relationProps.containsKey(CCConstants.getValidLocalName(CCConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS))) {
                    collectionRef.setRelationType(CollectionRef.RelationType.Proposal);
                } else {
                    collectionRef.setRelationType(CollectionRef.RelationType.Usage);
                }
                collectionRef.setRelationNode(transform(NodeRefImpl.class, authorities, user, relation, false));
            }
        }
        long permMillisSingle = (System.currentTimeMillis() - millis);
        return eduNodeRef;
    }

    /**
     * check if the user has permissions on this element via a collection and give him all permissions as it is an usage access
     */
    private static void processCollectionUsagePermissions(Set<String> authorities, String user, Map<String, Object> sourceAsMap, Map<String, Boolean> permissions) {
        if (permissions.size() == 1) {
            List<Map<String, Object>> collections = (List<Map<String, Object>>) sourceAsMap.get("collections");
            for (Map<String, Object> collection : Optional.ofNullable(collections).orElse(Collections.emptyList())) {
                Map<String, List<String>> collectionPermissions = (Map<String, List<String>>) collection.get("permissions");
                if (Optional.ofNullable(collectionPermissions).orElse(Collections.emptyMap()).entrySet().stream().filter(p ->
                        // check the consumer, collaborator or coordinator lists
                        Arrays.asList(CCConstants.PERMISSION_CONSUMER, CCConstants.PERMISSION_COLLABORATOR, CCConstants.PERMISSION_COORDINATOR).contains(p.getKey())
                ).anyMatch(
                        // and if the user has one of this rights
                        (entry) ->
                                authorities.stream().anyMatch(s -> entry.getValue().contains(s))
                                        || entry.getValue().contains(user))) {
                    permissions.putAll(
                            CCConstants.getUsagePermissions().stream().collect(
                                    Collectors.toMap(o -> o, (o) -> true)
                            ));
                    break;
                }
            }
        }
    }

    enum CONTRIBUTOR_PROP {firstname, lastname, email, url, uid}

    ;

    @Override
    public Set<SearchVCard> searchContributors(String suggest, List<String> fields, List<String> contributorProperties, ContributorKind contributorKind) throws IOException {
        checkClient();

        List<String> searchFields = new ArrayList<>();
        if (fields == null || fields.size() == 0) {
            for (CONTRIBUTOR_PROP att : CONTRIBUTOR_PROP.values()) {
                searchFields.add("contributor." + att.name());
            }
        } else {
            for (String f : fields) {
                if (Stream.of(CONTRIBUTOR_PROP.values()).anyMatch(v -> v.name().equals(f))) {
                    searchFields.add("contributor." + f);
                }
            }
        }
        final BoolQuery.Builder contributorQuery = QueryBuilders.bool();
        for (String searchField : searchFields) {
            final String search = suggest.contains("*") ? suggest : String.format("*%s*", suggest);
            contributorQuery.should(should -> should.wildcard(wc -> wc.field(searchField).value(search)));
        }

        if (!contributorProperties.isEmpty()) {
            contributorQuery.must(must -> must.bool(bool -> bool
                    .minimumShouldMatch("1")
                    .should(should -> {
                        contributorProperties.forEach(prop -> should.term(term -> term.field("contributor.property").value(prop)));
                        return should;
                    })));
        }

        if (contributorKind == ContributorKind.ORGANIZATION) {
            contributorQuery.must(must -> must.bool(bool -> bool
                    .should(should -> should.exists(exists -> exists.field("contributor.X-ROR")))
                    .should(should -> should.exists(exists -> exists.field("contributor.X-Wikidata")))
                    .minimumShouldMatch("1")));
        } else {
            contributorQuery.must(must -> must.bool(bool -> bool
                    .should(should -> should.exists(exists -> exists.field("contributor.X-ORCID")))
                    .should(should -> should.exists(exists -> exists.field("contributor.X-GND-URI")))
                    .minimumShouldMatch("1")));
        }

        SearchRequest searchRequest = SearchRequest.of(req -> req
                .index(WORKSPACE_INDEX)
                .from(0)
                .size(0)
                .trackTotalHits(track -> track.enabled(true))
                .sort(sort -> sort.score(score -> score.order(SortOrder.Desc)))
                .aggregations("contributor", aggr -> aggr
                        .nested(nes -> nes.path("contributor"))
                        .aggregations("vcard", vcardAggr -> vcardAggr
                                .terms(term -> term
                                        .field("contributor.vcard")
                                        .size(100))))
                .query(query -> query
                        .nested(nested -> nested
                                .path("contributor")
                                .query(nq -> nq
                                        .bool(contributorQuery.build())))));

        SearchResponse<Map> searchResponse = client
                .withTransportOptions(this::getRequestOptions)
                .search(searchRequest, Map.class);

        Aggregate aggregation = searchResponse.aggregations()
                .get("contributor")
                .nested()
                .aggregations()
                .get("vcard");

        VCardEngine engine = new VCardEngine();
        return aggregation.sterms().buckets().array().stream().
                map(StringTermsBucket::key)
                // this would be nicer via elastic "include" feature, however, it seems to be a pain with the java library
                .filter(k -> Arrays.stream(suggest.toLowerCase().split(" ")).allMatch(t -> k.stringValue().toLowerCase().contains(t)))
                .filter(k -> {
                    try {
                        VCard vcard = engine.parse(k.stringValue());
                        if (contributorKind == ContributorKind.ORGANIZATION) {
                            return vcard.getExtendedTypes().stream().map(ExtendedType::getExtendedName).anyMatch(
                                    (e) -> e.equals("X-ROR") || e.equals("X-Wikidata")
                            );
                        } else {
                            return vcard.getExtendedTypes().stream().map(ExtendedType::getExtendedName).anyMatch(
                                    (e) -> e.equals("X-ORCID") || e.equals("X-GND-URI")
                            );
                        }
                    } catch (Exception ignored) {
                        return false;
                    }
                })
                .map((k) -> new SearchVCard(k.stringValue())).
                collect(Collectors.toCollection(HashSet::new));
    }

    //
//    public RestHighLevelClient getClient() throws IOException {
//        checkClient();
//        return client;
//    }

    public DeleteResponse deleteNative(DeleteRequest deleteRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).delete(deleteRequest);
    }

    public SearchResponse<Map> searchNative(SearchRequest searchRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).search(searchRequest, Map.class);
    }

    public UpdateResponse<Map> updateNative(UpdateRequest updateRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).update(updateRequest, Map.class);
    }

    public ScrollResponse<Map> scrollNative(ScrollRequest searchScrollRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).scroll(searchScrollRequest, Map.class);
    }

    public ClearScrollResponse clearScrollNative(ClearScrollRequest clearScrollRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).clearScroll(clearScrollRequest);
    }

    public void checkClient() throws IOException {
        if (client == null || !client.ping().value()) {
            if (client != null) {
                try {
                    restClient.close();
                } catch (Exception e) {
                    logger.error("ping failed, close failed:" + e.getMessage() + " creating new");
                }
            }
            if(MAX_RESPONSE_ENTITY_SIZE == -1){
                if(LightbendConfigLoader.get().hasPath("elasticsearch.max_response_entity_size")){
                    MAX_RESPONSE_ENTITY_SIZE = LightbendConfigLoader.get().getInt("elasticsearch.max_response_entity_size");
                }
                else{
                    //100 MB
                    MAX_RESPONSE_ENTITY_SIZE = 100 * 1048576;
                }
            }

            // Create the low-level client
            restClient = RestClient
                    .builder(getConfiguredHosts())
                    .setDefaultHeaders(new Header[]{
                            // new BasicHeader("Authorization", "ApiKey " + apiKey)
                    })
                    .build();

            RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
            options.setHttpAsyncResponseConsumerFactory(
                    new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(MAX_RESPONSE_ENTITY_SIZE)
            );
            ElasticsearchTransport transport = new RestClientTransport(
                    restClient, new JacksonJsonpMapper(), new RestClientOptions.Builder(options).build());
            client = new ElasticsearchClient(transport);
        }
    }


    public SearchResultNodeRef getMetadata(List<String> nodeIds) throws IOException {

        SearchResultNodeRef sr = new SearchResultNodeRef();
        List<NodeRef> data = new ArrayList<>();
        sr.setData(data);

        SearchRequest searchRequest = SearchRequest.of(req -> req
                .index(WORKSPACE_INDEX)
                .from(0)
                .size(nodeIds.size())
                .trackTotalHits(track -> track.enabled(true))
                .query(root -> root
                        .bool(getGlobalConditions(null, null, null)
                                .must(must -> must
                                        .bool(queryNodeIds -> queryNodeIds.minimumShouldMatch("1")
                                                .should(should -> {
                                                    nodeIds.forEach(x -> should.term(t -> t.field("nodeRef.id").value(x)));
                                                    return should;
                                                })))
                                .build())));
        SearchResponse<Map> searchResponse = client.search(searchRequest, Map.class);

        logger.info("query: " + JsonpUtils.toJsonString(searchRequest, new JacksonJsonpMapper()));
        HitsMetadata<Map> hits = searchResponse.hits();
        logger.info("result count: " + hits.total().value());

        for (Hit<Map> hit : hits.hits()) {
            data.add(transformSearchHit(getUserAuthorities(), AuthenticationUtil.getFullyAuthenticatedUser(), hit.source(), true));
        }
        sr.setStartIDX(0);
        sr.setNodeCount((int) hits.total().value());
        return sr;
    }

    @Override
    public List<? extends Suggestion> getSuggestions(MetadataSet mds, String queryId, String parameterId, String value, List<MdsQueryCriteria> criterias) {
        Map<String, String[]> criteriasMap = MetadataSearchHelper.convertCriterias(criterias);
        SearchToken token = new SearchToken();
        token.setFacets(Collections.singletonList(parameterId));
        token.setFrom(0);
        token.setMaxResult(0);
        token.setFacetLimit(50);
        token.setFacetsMinCount(1);
        token.setQueryString(value);
        try {
            Map<String, MetadataKey> captions = mds.findWidget(parameterId).getValuesAsMap();
            SearchResultNodeRef search = searchFacets(
                    mds, queryId, criteriasMap, token
            );
            if (search.getFacets().size() != 1) {
                return Collections.emptyList();
            }
            return search.getFacets().get(0).getValues().stream().filter(s ->
                    // if one document has i.e. multiple keywords, they will be shown in the facet
                    // so, we filter for values which actually contain the given string
                    s.getValue().toLowerCase().contains(value.toLowerCase())
            ).map(s -> {
                Suggestion suggestion = new Suggestion();
                suggestion.setKey(s.getValue());
                suggestion.setDisplayString(
                        captions.containsKey(s.getValue()) ? captions.get(s.getValue()).getCaption() : s.getValue()
                );
                return suggestion;
            }).distinct().limit(token.getFacetLimit()).collect(Collectors.toList());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchResultNodeRef getFilesSharedByMe(SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
        BoolQuery.Builder query = QueryBuilders.bool()
                .must(
                        SearchServiceElastic.getFilesSharedByMeQuery(MetadataHelper.getLocalDefaultMetadataset().getQueries(MetadataReader.QUERY_SYNTAX_DSL))._toQuery()
                ).must(
                        getContentTypeQuery(contentType)
                );
        return searchByQuery(query.build(), skipCount, maxItems, sortDefinition);
    }
    @Override
    public SearchResultNodeRef getFilesSharedToMe(SharedToMeType type, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
        BoolQuery.Builder query = QueryBuilders.bool()
                .must(
                        SearchServiceElastic.getFilesSharedToMeQuery(MetadataHelper.getLocalDefaultMetadataset().getQueries(MetadataReader.QUERY_SYNTAX_DSL), type)._toQuery()
                ).must(
                        getContentTypeQuery(contentType)
                );
        return searchByQuery(query.build(), skipCount, maxItems, sortDefinition);
    }

    @Override
    public SearchResultNodeRef getWorkflowReceive(String user, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception{
        BoolQuery.Builder builder = QueryBuilders.bool();
        builder.minimumShouldMatch("1");
        getAllMemberships(user).forEach(authority -> builder.should(b -> b.match(m -> m.field(
                "properties.ccm:wf_receiver.keyword").query(authority)))
        );
        builder.mustNot(b -> b.exists(e -> e.field(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL))));
        builder.mustNot(b -> b.match(m -> m.field("aspects").query(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))));
        builder.must(getContentTypeQuery(contentType));
        MetadataQueries queries = MetadataHelper.getLocalDefaultMetadataset().getQueries(MetadataReader.QUERY_SYNTAX_DSL);
        String basequery = queries.findQuery("workflowReceive").getBasequery().get(null);
        if(StringUtils.isNotBlank(basequery)) {
            builder.must(b -> b.wrapper(new ReadableWrapperQueryBuilder(basequery).build()));
        }
        return searchByQuery(builder.build(), skipCount, maxItems, sortDefinition);
    }

    private Query getContentTypeQuery(ContentType contentType) {
        if(contentType == null || contentType.equals(ContentType.ALL)) {
            return QueryBuilders.matchAll().build()._toQuery();
        }
        SearchToken token = new SearchToken();
        token.setContentType(contentType);
        BoolQuery.Builder builder = QueryBuilders.bool();

        builder.minimumShouldMatch("1");
        Arrays.stream(token.getSearchCriterias().getContentkind()).forEach(
                type -> builder.should(s -> s.match(m -> m.field("type").query(CCConstants.getValidLocalName(
                                type
                        )
                ))));
        return builder.build()._toQuery();
    }

    private SearchResultNodeRef searchByQuery(BoolQuery query, int skipCount, int maxItems, SortDefinition sortDefinition) throws IOException {
        checkClient();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder().index(WORKSPACE_INDEX);
        searchRequestBuilder.query(query._toQuery());
        searchRequestBuilder.from(skipCount);
        searchRequestBuilder.size(maxItems);
        searchRequestBuilder.trackTotalHits(new TrackHits.Builder().enabled(true).build());
        searchRequestBuilder.source(src -> src
                .filter(filter -> filter.excludes(appendDefaultExcludes(new ArrayList<>())))
        );
        if (sortDefinition != null) {
            sortDefinition.applyToSearchSourceBuilder(searchRequestBuilder);
        }
        SearchRequest searchRequest = searchRequestBuilder.build();
        SearchResponse<Map> searchResponse = client.search(searchRequest, Map.class);
        HitsMetadata<Map> hits = searchResponse.hits();
        Set<String> authorities = getUserAuthorities();
        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();
        SearchResultNodeRef sr = new SearchResultNodeRef();
        sr.setData(hits.hits().stream().map(h -> transformSearchHit(authorities, user, h.source(), false)).collect(Collectors.toList()));
        sr.setStartIDX(skipCount);
        sr.setNodeCount((int) hits.total().value());
        return sr;
    }



}
