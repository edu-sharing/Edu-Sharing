package org.edu_sharing.service.collection;

import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.util.*;

public class CollectionServiceElastic extends CollectionServiceImpl {

    private final SearchServiceElastic searchServiceElastic;

    public static CollectionServiceElastic build(String appId) {
        CollectionServiceConfig config = (CollectionServiceConfig) ApplicationContextFactory.getApplicationContext().getBean("collectionServiceConfig");
        return new CollectionServiceElastic(appId, config.getPattern(), config.getPath());
    }

    public CollectionServiceElastic(String appId, String pattern, String path) {
        super(appId, pattern, path);
        this.searchServiceElastic = new SearchServiceElastic(appId);
    }

    @Override
    protected void addCollectionCountProperties(NodeRef nodeRef, Collection collection, BoolQuery readPermissionsQuery) {
        try {
            SearchRequest searchRequest = SearchRequest.of(req -> req
                    .index(SearchServiceElastic.WORKSPACE_INDEX)
                    .size(0)
                    .aggregations("type", agg -> agg.terms(t -> t.field("type")))
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> readPermissionsQuery != null ? m.bool(readPermissionsQuery) : m.bool(searchServiceElastic::getReadPermissionsQuery))
                                    .must(m -> m.match(match -> match.field("nodeRef.storeRef.protocol").query("workspace")))
                                    .must(m -> m.wildcard(w -> w.field("fullpath").wildcard("*/" + nodeRef.getId() + "*")))
                                    .mustNot(m -> m.match(match -> match.field("aspects").query(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT)))))));

            SearchResponse<Map> result = searchServiceElastic.searchNative(searchRequest);
            StringTermsAggregate types = result.aggregations().get("type").sterms();
            for (StringTermsBucket bucket : types.buckets().array()) {
                if (bucket.key().stringValue().equals("ccm:io")) {
                    collection.setChildReferencesCount((int) bucket.docCount());
                } else if (bucket.key().stringValue().equals("ccm:map")) {
                    collection.setChildCollectionsCount((int) bucket.docCount());
                }
            }
        } catch (Exception e) {
            logger.warn("Error fetching collection counts for " + nodeRef, e);
        }
    }

    @Override
    public CollectionProposalInfo getCollectionsContainingProposals(CCConstants.PROPOSAL_STATUS status, Integer skipCount, Integer maxItems, SortDefinition sortDefinition) throws Throwable {

        SearchRequest searchRequest = SearchRequest.of(req->req
                .index(SearchServiceElastic.WORKSPACE_INDEX)
                .size(maxItems)
                .from(skipCount)
                .source(src->src.fetch(true))
                .trackTotalHits(t->t.enabled(true))
                .scriptFields("proposals", sf->sf.script(src->src.inline(il->il
                        .lang("painless")
                        .source("Map m = new HashMap();for(def proposal: params._source.children) { if(proposal['type'] == 'ccm:collection_proposal') {def value = proposal['properties']['ccm:collection_proposal_status']; if(m.containsKey(value)) { m.put(value, m.get(value) + 1); } else { m.put(value, 1); } } } return m;"))))
                .query(q -> q.scriptScore(ssq -> ssq
                        .minScore(1f)
                        .script(scr->scr.inline(il->il
                                .lang("painless")
                                .source("if(!params._source.containsKey('children')) return 0; for(def proposal: params._source.children) { if(proposal['type'] == 'ccm:collection_proposal' && proposal.containsKey('properties') && proposal['properties'].containsKey('ccm:collection_proposal_status') && proposal['properties']['ccm:collection_proposal_status'] == params.status) return 1; } return 0;")
                                .params("status", JsonData.of(status))))
                        .query(iq -> iq.bool(b -> b
                                .must(m -> m.match(match -> match.field("nodeRef.storeRef.protocol").query("workspace")))
                                .must(m -> m.match(match -> match.field("type").query(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_MAP))))
                                .must(m -> m.match(match -> match.field("aspects").query(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION))))
                                .must(m -> m.bool(bool -> searchServiceElastic.getPermissionsQuery(bool, "permissions.Write")))))
                ))
        );

        SearchResponse<Map> result = searchServiceElastic.searchNative(searchRequest);
        List<CollectionProposalInfo.CollectionProposalData> dataList = new ArrayList<>();
        Set<String> authorities = searchServiceElastic.getUserAuthorities();
        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();
        boolean isAdmin = AuthorityServiceHelper.isAdmin();

        for (Hit<Map> hit : result.hits().hits()) {
            CollectionProposalInfo.CollectionProposalData data = new CollectionProposalInfo.CollectionProposalData();
            data.setNodeRef(searchServiceElastic.transformSearchHit(isAdmin, authorities, user, hit.source(), false));
            for (Object v : hit.fields().get("proposals").to(List.class)) {
                Map al = (Map) v;
                for (Object e : al.entrySet()) {
                    Map.Entry entry = (Map.Entry) e;
                    data.getProposalCount().put(CCConstants.PROPOSAL_STATUS.valueOf((String) entry.getKey()), (Integer) entry.getValue());
                }
                logger.info(v);
            }
            dataList.add(data);
        }
        return new CollectionProposalInfo(dataList, result.hits().total().value());
    }

    @Override
    public List<AssociationRef> getChildrenProposal(String parentId) throws Exception {
        return super.getChildrenProposal(parentId);
    }

    @Override
    public void proposeForCollection(String collectionId, String originalNodeId, String sourceRepositoryId) throws DuplicateNodeException, Throwable {
        super.proposeForCollection(collectionId, originalNodeId, sourceRepositoryId);
    }

    @Override
    protected SearchResultNodeRef searchChildren(String scope, SortDefinition sortDefinition, int skipCount, int maxItems) throws Throwable {

        MetadataSet mds = MetadataHelper.getMetadataset(appInfo, CCConstants.metadatasetdefault_id);

        String queryId = getQueryForScope(scope);
        /**
         * @TODO owner + inherit off -> node will be found even if search is done in edu-group context
         */

        List<org.edu_sharing.service.model.NodeRef> returnVal = new ArrayList<>();
        SearchToken token = new SearchToken();
        token.setContentType(SearchService.ContentType.COLLECTIONS);
        token.setSortDefinition(sortDefinition);
        token.setFrom(skipCount);
        token.setMaxResult(maxItems);
        SearchResultNodeRef nodeRefs = SearchServiceFactory.getLocalService().search(mds, queryId, Collections.emptyMap(), token);
        for (org.edu_sharing.service.model.NodeRef nodeRef : nodeRefs.getData()) {
            if (isSubCollection(nodeRef)) {
                continue;
            }
            returnVal.add(nodeRef);
        }
        nodeRefs.setData(returnVal);
        return nodeRefs;
    }
}

