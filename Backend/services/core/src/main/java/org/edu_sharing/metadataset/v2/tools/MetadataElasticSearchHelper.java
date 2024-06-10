package org.edu_sharing.metadataset.v2.tools;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermLookup;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.restservices.mds.v1.model.MdsWidget;
import org.edu_sharing.service.search.ReadableWrapperQueryBuilder;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SharedToMeType;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

public class MetadataElasticSearchHelper extends MetadataSearchHelper {
    /**
     *  the given count will be multiplied by this value since facets are filtered for the containing string afterwards and we need some overhead
     */
    public static final int FACET_LIMIT_MULTIPLIER = 5;
    static Logger logger = Logger.getLogger(MetadataElasticSearchHelper.class);
    private static MetadataQueryPreprocessor preprocessor = new MetadataQueryPreprocessor(MetadataReader.QUERY_SYNTAX_DSL);

    public static BoolQuery.Builder getElasticSearchQuery(SearchToken searchToken, MetadataQueries queries,MetadataQuery query, Map<String,String[]> parameters) throws IllegalArgumentException {
        return getElasticSearchQuery(searchToken, queries,query,parameters,true);
    }

    public static BoolQuery.Builder getElasticSearchQuery(SearchToken searchToken, MetadataQueries queries,MetadataQuery query, Map<String,String[]> parameters, Boolean asFilter) throws IllegalArgumentException {


        BoolQuery.Builder result = new BoolQuery.Builder();
        if(asFilter == null || (asFilter == query.getBasequeryAsFilter())) {
            String baseQuery = replaceCommonQueryVariables(query.getBasequery().get(null));
            String baseQueryConditional = replaceCommonQueryVariables(query.findBasequery(parameters == null ? null : parameters.keySet()));

            if(Objects.equals(baseQuery,baseQueryConditional)) {
                if(baseQuery != null) {
                    result.must(must -> must.wrapper(new ReadableWrapperQueryBuilder(baseQuery).build()));
                }
            } else {
                result.must(must->must.wrapper(new ReadableWrapperQueryBuilder(baseQuery).build()))
                        .must(must->must.wrapper(new ReadableWrapperQueryBuilder(baseQueryConditional).build()));
            }
        }

        if(parameters != null && parameters.isEmpty()) {
            if(query.isApplyBasequery()){
                String baseQueryParam = queries.findBasequery(parameters.keySet());
                if(!StringUtils.isEmpty(baseQueryParam)) {
                    result.must(must->must.wrapper(new ReadableWrapperQueryBuilder(baseQueryParam).build()));
                }
                applyCondition(queries, result);
            }
            applyCondition(query, result);
        }

        for (String name : parameters.keySet()) {
            MetadataQueryParameter parameter = query.findParameterByName(name);
            if (parameter == null)
                throw new IllegalArgumentException("Could not find parameter " + name + " in the query " + query.getId());

            String[] values = parameters.get(parameter.getName());
            /**
             * @TODO MetadataSearchHelper check if ignoreable is needed
             */
            if ((values == null || values.length == 0)) {
                //if(parameter.getIgnorable()==0)
                continue;
            }

            if(asFilter != null && parameter.isAsFilter() != asFilter){
                continue;
            }

            Query queryBuilderParam = null;
            if(query.isApplyBasequery()){
                String baseQueryParam = queries.findBasequery(parameters.keySet());
                if(!StringUtils.isEmpty(baseQueryParam)) {
                    result.must(must->must.wrapper(new ReadableWrapperQueryBuilder(baseQueryParam).build()));
                }
                applyCondition(queries, result);
            }

            applyCondition(query, result);
            BoolQuery sharedFilesSearch;
            if(query.getId().equals("workspace") && parameter.getName().equals("parent") && parameter.getPreprocessor().equals("node_path")) {
                sharedFilesSearch = handleSharedFilesSearch(queries, values);
            } else {
                sharedFilesSearch = null;
            }
            if(sharedFilesSearch != null) {
                queryBuilderParam = Query.of(q-> q.bool(sharedFilesSearch));
            } else if (parameter.isMultiple()) {

                String multipleJoin = parameter.getMultiplejoin();
                BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
                if (multipleJoin.equals("AND")) {
                    for(String value : values){
                        boolQueryBuilder.must(must->must.wrapper(new ReadableWrapperQueryBuilder(replaceCommonQueryVariables(getStatmentForValue(parameter,value)), parameter).build()));
                    }
                } else {
                    for(String value : values){
                        boolQueryBuilder.should(should->should.wrapper(new ReadableWrapperQueryBuilder(replaceCommonQueryVariables(getStatmentForValue(parameter,value)), parameter).build()));
                    }
                }

                queryBuilderParam = Query.of(q->q.bool(boolQueryBuilder.build()));
            }else{
                if(values.length>1){
                    throw new InvalidParameterException("Trying to search for multiple values of a non-multivalue field "+parameter.getName());
                }

                queryBuilderParam = Query.of(q->q.wrapper(new ReadableWrapperQueryBuilder(replaceCommonQueryVariables(getStatmentForValue(parameter,values[0])), parameter).build()));
            }

            if(query.getJoin().equals("AND")){
                result.must(queryBuilderParam);
            }else{
                result.should(queryBuilderParam);
            }

        }

        if(asFilter
                && searchToken != null
                && searchToken.getSearchCriterias() != null
                && searchToken.getSearchCriterias().getContentkind() != null
                && searchToken.getSearchCriterias().getContentkind().length > 0){

            result.filter( filter->filter.bool(criteriasBool->{
                Arrays.stream(searchToken.getSearchCriterias().getContentkind())
                        .forEach((content) -> criteriasBool.should(should->should
                                .match(match->match
                                        .field("type")
                                        .query(CCConstants.getValidLocalName(content)))));
                return criteriasBool;
            }));
        }

        return result;
    }

    private static BoolQuery handleSharedFilesSearch(MetadataQueries queries, String[] values) {
        if(values[0].startsWith("-to_me_shared_files")){
            try {
                if("-to_me_shared_files_personal-".equals(values[0]) ){
                    return SearchServiceElastic.getFilesSharedToMeQuery(queries, SharedToMeType.Private);
                }else if("-to_me_shared_files-".equals(values[0])){
                    return SearchServiceElastic.getFilesSharedToMeQuery(queries, SharedToMeType.All);
                }else{
                    logger.error("unknown SharedToMeType:"+values[0]);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
        } else if(values[0].startsWith("-my_shared_files")) {
            try {
                return SearchServiceElastic.getFilesSharedByMeQuery(queries);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    private static BoolQuery.Builder applyCondition(MetadataQueryBase query, BoolQuery.Builder result) {
        for(MetadataQueryCondition condition : query.getConditions()){
            boolean conditionState = MetadataHelper.checkConditionTrue(condition.getCondition());
            if(conditionState && condition.getQueryTrue()!=null) {
                String conditionString = condition.getQueryTrue();
                conditionString = replaceCommonQueryVariables(conditionString);
                final String condString = conditionString;
                result.must(must->must.wrapper(new ReadableWrapperQueryBuilder(condString).build()));
            }
            if(!conditionState && condition.getQueryFalse()!=null) {
                String conditionString = condition.getQueryFalse();
                conditionString = replaceCommonQueryVariables(conditionString);
                final String condString = conditionString;
                result.must(must->must.wrapper(new ReadableWrapperQueryBuilder(condString).build()));
            }
        }
        return result;
    }
    private static String getStatmentForValue(MetadataQueryParameter parameter, String value) {
        if (value == null && parameter.isMandatory()) {
            throw new java.lang.IllegalArgumentException("null value for mandatory parameter " + parameter.getName() + " given, null values are not allowed if mandatory is set to true");
        }
        if (value == null)
            return "";

        // invoke any preprocessors for this value
        try {
            value = preprocessor.run(parameter, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (value.startsWith("\"") && value.endsWith("\"") || parameter.isExactMatching()) {
            // clear value's '"'
            if(value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            //String statement = parameter.getStatement(value).replace("${value}", QueryParser.escape(value));
            String statement = QueryUtils.replacerFromSyntax(parameter.getSyntax()).replaceString(
                    parameter.getStatement(value),
                    "${value}", value);
            statement = QueryUtils.replacerFromSyntax(parameter.getSyntax(),true).replaceString(
                    statement,
                    "${valueRaw}", value);
            return statement;
        }

        String[] words = value.split(" ");
        final BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        for (String word : words) {
            //String statement = parameter.getStatement(value).replace("${value}", QueryParser.escape(word));
            String statement = QueryUtils.replacerFromSyntax(parameter.getSyntax()).replaceString(
                    parameter.getStatement(word),
                    "${value}", word);
            statement = QueryUtils.replacerFromSyntax(parameter.getSyntax(),true).replaceString(
                    statement,
                    "${valueRaw}", word);

            final String finStatement = statement;
            boolQuery.must(must->must.wrapper(new ReadableWrapperQueryBuilder(finStatement).build()));

        }
        return JsonpUtils.toJsonString(Query.of(q->q.bool(boolQuery.build())), new JacksonJsonpMapper());
    }

    public static Set<MetadataQueryParameter> getExcludeOwnFacets(MetadataQuery query, Map<String,String[]> parameters, List<String> facets){
        Set<MetadataQueryParameter> excludeOwn = new HashSet<>();
        for (String name : facets) {
            MetadataQueryParameter parameter = query.findParameterByName(name);
            if(parameter == null) continue;
            if((parameter.getMultiplejoin() != null && parameter.getMultiplejoin().equals("OR"))) excludeOwn.add(parameter);
        }
        return excludeOwn;
    }

    /**
     * returns FilterAggregations to be used in a separate call
     * @param mds
     * @param query
     * @param parameters
     * @param facets
     * @param excludeOwn
     * @param globalConditions
     * @param searchToken
     * @return
     * @throws IllegalArgumentException
     */
    public static Map<String, Aggregation> getAggregations(MetadataSet mds, MetadataQuery query, Map<String,String[]> parameters, List<String> facets, Set<MetadataQueryParameter> excludeOwn, Query globalConditions, SearchToken searchToken) throws IllegalArgumentException {
        MetadataQueries queries = mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL);
        Map<String, Aggregation> result = new HashMap<>();
        String currentLocale = new AuthenticationToolAPI().getCurrentLocale();
        for (String facet : facets) {

            Map<String, String[]> tmp = new HashMap<>(parameters);
            if (excludeOwn.stream().anyMatch(mdqp -> mdqp.getName().equals(facet))) {
                tmp.remove(facet);
            }

            BoolQuery.Builder qbFilter = getElasticSearchQuery(searchToken, queries, query, tmp, true);
            BoolQuery.Builder qbNoFilter = getElasticSearchQuery(searchToken, queries, query, tmp, false);
            BoolQuery.Builder  bqb = new BoolQuery.Builder()
                .must(must->must.bool(qbFilter.build()))
                    .must(must->must.bool(qbNoFilter.build()))
                    .must(globalConditions);

            List<String> fieldName = Collections.singletonList("properties." + facet+".keyword");
            MetadataQueryParameter parameter = query.findParameterByName(facet);
            if(parameter != null && parameter.getFacets() != null) {
                if(parameter.getFacets().size() != 1) {
                    logger.warn("Using more than one facet parameter is not recommended when using elasticsearch");
                }
                fieldName = parameter.getFacets();
            }
            if(searchToken.getQueryString() != null && !searchToken.getQueryString().trim().isEmpty()){

                boolean isi18nProp = false;
                MetadataWidget mdw = mds.findWidget(facet);
                if(mdw != null && new MdsWidget(mdw).isHasValues()){
                    isi18nProp = true;
                }

                Query mmqb;
                if(parameter != null && parameter.getFacets() != null) {
                    if(parameter.getFacets().size() > 1) {
                        BoolQuery.Builder facetQuery = new BoolQuery.Builder();
                        for (String parameterFacet : parameter.getFacets()) {
                            facetQuery.should(getFacetFilter(searchToken.getQueryString(), parameterFacet));
                        }
                        mmqb = Query.of(q->q.bool(facetQuery.build()));
                    } else {
                        mmqb = getFacetFilter(searchToken.getQueryString(),parameter.getFacets().get(0));
                    }
                } else if(isi18nProp){
                    mmqb = getFacetFilter(searchToken.getQueryString(),"i18n."+currentLocale+"."+facet, "collections.i18n."+currentLocale+"."+facet);
                }else{
                    mmqb = getFacetFilter(searchToken.getQueryString(),"properties."+facet, "properties."+facet+".keyword");
                }
                bqb.must(mmqb);
            }

            // https://discuss.elastic.co/t/sub-aggregation-in-new-java-api-client/313447
            Query bqbQuery = bqb.build()._toQuery();
            if(fieldName.size() == 1) {
                result.put(
                        facet,
                        new Aggregation.Builder().filter(bqbQuery)
                                .aggregations(facet, AggregationBuilders.terms()
                                .field(fieldName.get(0))
                                .size(searchToken.getFacetLimit() * FACET_LIMIT_MULTIPLIER)
                                .minDocCount(searchToken.getFacetsMinCount())
                                .build()._toAggregation()
                        ).build()
                );
            } else {
                result.put(
                        facet,
                        new Aggregation.Builder().filter(
                                        bqbQuery
                                ).aggregations(facet, AggregationBuilders.multiTerms()
                                        .terms(fieldName.stream().map(f -> MultiTermLookup.of(t->t.field(f).missing(""))).collect(Collectors.toList()))
                                        .size(searchToken.getFacetLimit() * FACET_LIMIT_MULTIPLIER)
                                        .minDocCount((long) searchToken.getFacetsMinCount())
                                        .build()
                                        ._toAggregation()
                                )
                                // @TODO: Check if this is really necessary
                                .meta("type", JsonData.of("multi_terms")).build()
                );
            }

            if(parameters.get(facet) != null && parameters.get(facet).length > 0) {
                List<String> facetDetails = query.findParameterByName(facet).getFacets();
                result.put(
                        facet + "_selected",
                        new Aggregation.Builder().filter(
                                bqbQuery
                        ).aggregations(facet, agg->agg
                                .terms(term->term
                                .field(facetDetails == null || facetDetails.isEmpty() ? "properties." + facet + ".keyword" : facetDetails.get(0))
                                .size(parameters.get(facet).length)
                                .minDocCount(1)
                                .include(ti->ti.terms(Arrays.asList(parameters.get(facet)))))
                        ).build()
                );

            }

        }

        return result;
    }

    private static Query getFacetFilter(String queryString, String... fieldName) throws IllegalArgumentException {
        return Query.of(q->q.bool(bool-> {
            bool.minimumShouldMatch("1");
            Arrays.stream(fieldName).forEach(
                    field -> {
                        bool.should(should -> should.wildcard(wc -> wc.field(field).value("*" + queryString + "*").caseInsensitive(true)));
                    }
            );
            return bool;
        }));
    }
}