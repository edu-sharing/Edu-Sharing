package org.edu_sharing.metadataset.v2.tools;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElasticTestUtils;
import org.edu_sharing.service.search.model.SearchToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class MetadataElasticSearchHelperTest {

    private MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic;
    private String basequery;
    private MetadataQuery query;
    private MetadataQueries queries;
    private MetadataSet mds;
    private AuthenticationToolAPI authenticationToolApi;
    private MockedConstruction<AuthenticationToolAPI> authenticationToolApiConstruction;


    @BeforeEach
    void beforeEach() {

        authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("user");
        authenticationToolApi = Mockito.mock(AuthenticationToolAPI.class);
        authenticationToolApiConstruction = Mockito.mockConstruction(AuthenticationToolAPI.class);
        when(authenticationToolApi.getCurrentLocale()).thenReturn("en");

        query = new MetadataQuery();
        basequery = "{\"exists\":{\"field\": \"type\"}}";
        query.setBasequery(new HashMap<>() {{
            put(null, basequery);
        }});
        query.setId("ngsearch");
        query.setJoin("AND");
        query.setSyntax(MetadataReader.QUERY_SYNTAX_DSL);
        queries = new MetadataQueries();
        queries.setQueries(Collections.singletonList(query));
        mds = new MetadataSet();
        mds.setQueries(new HashMap<>(){{
            put(MetadataReader.QUERY_SYNTAX_DSL, queries);
        }});
    }
    @AfterEach
    void afterEach() {
        authenticationUtilMockedStatic.close();
        authenticationToolApiConstruction.close();
    }

    @Test
    void getElasticSearchQueryBasic() throws JsonProcessingException {
        SearchToken token = new SearchToken();
        BoolQuery.Builder result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, Collections.emptyMap());

        SearchServiceElasticTestUtils.assertQuery(String.format(
                "{\"bool\":{\"must\":[{\"wrapper\":{\"query\": \"%s\"}}]}}",
                Base64.getEncoder().encodeToString(basequery.getBytes())
        ), result);

        token.setContentType(SearchService.ContentType.FILES);
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, Collections.emptyMap());
        String expected = "{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"match\":{\"type\":{\"query\":\"ccm:io\"}}}]}}],\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"}}]}}";
        SearchServiceElasticTestUtils.assertQuery(expected,result);
    }


    @Test
    void getElasticSearchQueryMultipleParameter() {
        SearchToken token = new SearchToken();
        List<MetadataQueryParameter> parameters = new ArrayList<>();
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax());
        parameter.setMultiple(true);
        parameter.setMultiplejoin("AND");
        parameter.setName("parameter");
        parameter.setStatements(new HashMap<>() {{
            put(null, "{\"match\":{\"some_field\":\"{$value}\"}}");
        }});
        parameters.add(parameter);
        query.setParameters(parameters);
        BoolQuery.Builder result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
        }});
        SearchServiceElasticTestUtils.assertQuery(
                "{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"\n        }\n      },\n      {\n        \"bool\" : {\n          \"must\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            }\n          ]}\n      }\n    ]}\n}",
                result
        );

        // OR JOIN
        parameter.setMultiplejoin("OR");
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
        }});
        SearchServiceElasticTestUtils.assertQuery(
                "{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"\n        }\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            }\n          ]}\n      }\n    ]}\n}",
                result
        );


        // 2 Parameters AND combined
        MetadataQueryParameter parameter2 = new MetadataQueryParameter(query.getSyntax());
        parameter2.setMultiple(true);
        parameter2.setMultiplejoin("AND");
        parameter2.setName("parameter2");
        parameter2.setMultiplejoin("OR");
        parameters.add(parameter2);
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
            put("parameter2", new String[]{"a", "b"});
        }});
        SearchServiceElasticTestUtils.assertQuery(
                "{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"\n        }\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            }\n          ]}\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IiphKiJ9fX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IipiKiJ9fX0=\"\n              }\n            }\n          ]}\n      }\n    ]}\n}",
                result
        );
        // 2 Parameters or combined
        query.setJoin("OR");
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
            put("parameter2", new String[]{"a", "b"});
        }});
        SearchServiceElasticTestUtils.assertQuery(
                "{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"\n        }\n      }\n    ],\n    \"should\" : [\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            }\n          ]}\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IiphKiJ9fX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IipiKiJ9fX0=\"\n              }\n            }\n          ]}\n      }\n    ]}\n}",
                result
        )
        ;
    }
    @Test
    void getAggregations() {
        SearchToken token = new SearchToken();
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax());
        parameter.setName("test_facet");

        query.setParameters(Collections.singletonList(parameter));

        Map<String, Aggregation> result = MetadataElasticSearchHelper.getAggregations(mds, query, Collections.emptyMap(),
                Collections.singletonList("test_facet"), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(1, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"}}]}},{\"bool\":{}},{\"bool\":{}}]}}}",
                result.get("test_facet")
        );

        // 2 facets
        MetadataQueryParameter parameter2 = new MetadataQueryParameter(query.getSyntax());
        parameter2.setName("test_facet2");
        query.setParameters(Arrays.asList(parameter, parameter2));

        result = MetadataElasticSearchHelper.getAggregations(mds, query, Collections.emptyMap(),
                Arrays.asList("test_facet", "test_facet2"), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(2, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"}}]}},{\"bool\":{}},{\"bool\":{}}]}}" +
                        "}",
                result.get("test_facet"));
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet2\":{\"terms\":{\"field\":\"properties.test_facet2.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"}}]}},{\"bool\":{}},{\"bool\":{}}]}}" +
                        "}",
                result.get("test_facet2")
        );

        // multi term facet
        parameter.setFacets(Arrays.asList("facet1", "facet2"));
        query.setParameters(Collections.singletonList(parameter));

        result = MetadataElasticSearchHelper.getAggregations(mds, query, Collections.emptyMap(),
                Collections.singletonList("test_facet"), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(1, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"multi_terms\":{\"min_doc_count\":4,\"size\":250,\"terms\":[{\"field\":\"facet1\",\"missing\":\"\"},{\"field\":\"facet2\",\"missing\":\"\"}]}}}," +
                        "\"meta\":{\"type\":\"multi_terms\"}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"}}]}},{\"bool\":{}},{\"bool\":{}}]}}" +
                        "}",
                result.get("test_facet")
        );
    }


    @Test
    void getAggregationsSearchToken() {
        SearchToken token = new SearchToken();
        token.setQueryString("A B C");
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax());
        parameter.setName("test_facet");
        MetadataWidget widget = new MetadataWidget();
        widget.setId("test_facet");
        mds.setWidgets(Collections.singletonList(widget));

        query.setParameters(Collections.singletonList(parameter));

        Map<String, Aggregation> result = MetadataElasticSearchHelper.getAggregations(mds, query, Collections.emptyMap(),
                Collections.singletonList("test_facet"), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(1, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"}}]}},{\"bool\":{}},{\"bool\":{}},{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"properties.test_facet\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}},{\"wildcard\":{\"properties.test_facet.keyword\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}}]}}" +
                        "}",
                result.get("test_facet")
        );

        // with param
        query.setParameters(Collections.singletonList(parameter));
        result = MetadataElasticSearchHelper.getAggregations(mds, query, new HashMap<>() {{
                    put("test_facet", new String[]{"a"});
                }},
                Collections.singletonList("test_facet"), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(2, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"}},{\"wrapper\":{\"query\":\"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnRlc3RfZmFjZXQua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IiphKiJ9fX0=\"}}]}},{\"bool\":{}},{\"bool\":{}},{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"properties.test_facet\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}},{\"wildcard\":{\"properties.test_facet.keyword\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}}]}}" +
                        "}",
                result.get("test_facet")
        );
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"include\":[\"a\"],\"min_doc_count\":1,\"size\":1}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"}},{\"wrapper\":{\"query\":\"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnRlc3RfZmFjZXQua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IiphKiJ9fX0=\"}}]}},{\"bool\":{}},{\"bool\":{}},{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"properties.test_facet\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}},{\"wildcard\":{\"properties.test_facet.keyword\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}}]}}" +
                        "}",
                result.get("test_facet_selected")
        );

        // multi term facet
        parameter.setFacets(Arrays.asList("facet1", "facet2"));
        query.setParameters(Collections.singletonList(parameter));

        result = MetadataElasticSearchHelper.getAggregations(mds, query, Collections.emptyMap(),
                Collections.singletonList("test_facet"), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(1, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"multi_terms\":{\"min_doc_count\":4,\"size\":250,\"terms\":[{\"field\":\"facet1\",\"missing\":\"\"},{\"field\":\"facet2\",\"missing\":\"\"}]}}}," +
                        "\"meta\":{\"type\":\"multi_terms\"}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"}}]}},{\"bool\":{}},{\"bool\":{}},{\"bool\":{\"should\":[{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"facet1\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}},{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"facet2\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}}]}}]}}" +
                        "}",
                result.get("test_facet")
        );


    }


}