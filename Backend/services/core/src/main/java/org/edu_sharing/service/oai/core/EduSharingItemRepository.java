package org.edu_sharing.service.oai.core;

import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.exceptions.handler.IdDoesNotExistException;
import io.gdcc.xoai.dataprovider.filter.ScopedFilter;
import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.repository.ItemRepository;
import io.gdcc.xoai.dataprovider.repository.ResultsPage;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.oai.formats.OaiMetadataFormatWriter;
import org.edu_sharing.service.oai.legacy.OAILOMExporterHSOER;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.edu_sharing.util.CheckedFunction;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Lazy
@Component
@RefreshScope
@RequiredArgsConstructor
public class EduSharingItemRepository implements ItemRepository {

    @Value("${exporter.oai.identifierPrefix:}")
    private String identifierPrefix;

    private final EduMetadataFormatRegistry eduMetadataFormatRegistry;

    @NotNull
    private String getIdentifier(NodeRef nodeRef) {
        return identifierPrefix + nodeRef.getNodeId();
    }

    @NotNull
    private String getNodeId(String identifier) {
        return identifier.substring(identifierPrefix.length());
    }

    @Override
    public ItemIdentifier getItemIdentifier(String identifier) throws IdDoesNotExistException {
        if (!identifier.startsWith(identifierPrefix)) {
            throw new IdDoesNotExistException("Invalid id, identifierPrefix does not match " + identifierPrefix);
        }

        String nodeId = getNodeId(identifier);
        NodeRef nodeRef = searchFor(nodeId);
        Instant date = ((Date) nodeRef.getProperties().get(CCConstants.CM_PROP_C_MODIFIED)).toInstant();
        return new EduItemIdentifier(identifier, date);
    }


    private ItemIdentifier getItemIdentifier(NodeRef nodeRef) {
        Instant date = ((Date) nodeRef.getProperties().get(CCConstants.CM_PROP_C_MODIFIED)).toInstant();
        return new EduItemIdentifier(getIdentifier(nodeRef), date);
    }

    @Override
    public Item getItem(String identifier, MetadataFormat format) throws HandlerException {
        // TODO alles neu!
        if (!identifier.startsWith(identifierPrefix)) {
            throw new IdDoesNotExistException("Invalid id, identifierPrefix does not match " + identifierPrefix);
        }
        String nodeId = getNodeId(identifier);
        NodeRef nodeRef = searchFor(nodeId);
        return getItem(nodeRef, format);
    }

    private Item getItem(NodeRef nodeRef, MetadataFormat format) throws HandlerException {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            OaiMetadataFormatWriter formatWriter = eduMetadataFormatRegistry.getFormatWriter(format.getPrefix());

            // todo compare results
            boolean useNew= true;
            if(useNew) {
                formatWriter.write(os, new org.alfresco.service.cmr.repository.NodeRef(new StoreRef(nodeRef.getStoreProtocol(), nodeRef.getStoreId()), nodeRef.getNodeId()), ValueTool.getMultivalue(nodeRef.getProperties()));
            } else {
                new OAILOMExporterHSOER().write(os, nodeRef.getNodeId());
            }


            return new EduItem(getIdentifier(nodeRef), os.toString());
        } catch (InvalidNodeRefException e) {
            log.error(e.getMessage(), e);
            throw new IdDoesNotExistException(e);
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
            throw new IdDoesNotExistException(new Exception(t));
        }
    }


    @Override
    public ResultsPage<ItemIdentifier> getItemIdentifiers(List<ScopedFilter> filters, MetadataFormat metadataFormat, int maxResponseLength, ResumptionToken.Value resumptionToken) throws HandlerException {
        ResultsPage<NodeRef> nodeRefResultsPage = searchFor(maxResponseLength, resumptionToken);
        return resultsPage(nodeRefResultsPage, this::getItemIdentifier);
    }

    private <S, T> ResultsPage<T> resultsPage(ResultsPage<S> value, CheckedFunction<S, T, HandlerException> mapper) {
        return new ResultsPage<>(
                value.getRequestTokenValue(),
                value.hasMore(),
                value.getList().stream().map(CheckedFunction.wrap(mapper)).collect(Collectors.toList()),
                value.getTotal());
    }

    @Override
    public ResultsPage<Item> getItems(List<ScopedFilter> filters, MetadataFormat metadataFormat, int maxResponseLength, ResumptionToken.Value resumptionToken) throws HandlerException {
        ResultsPage<NodeRef> nodeRefResultsPage = searchFor(maxResponseLength, resumptionToken);
        return resultsPage(nodeRefResultsPage, ref -> getItem(ref, metadataFormat));
    }

    private NodeRef searchFor(String nodeId) throws IdDoesNotExistException{
        try {
            Map<String, String[]> searchCriteria = new HashMap<>();
            searchCriteria.put("nodeId", new String[]{nodeId});

            SearchToken token = new SearchToken();
            token.setMaxResult(1);
            SortDefinition sort = new SortDefinition();
            sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.CM_PROP_C_CREATED, true));
            token.setSortDefinition(sort);
            token.setContentType(SearchService.ContentType.FILES);
            SearchResultNodeRef result = SearchServiceFactory.getLocalService().search(
                    MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id),
                    "oai",
                    searchCriteria,
                    token);

            log.info("{}", result.getNodeCount());
            return result.getData().get(0);

        } catch (Throwable t) {
            log.warn(t.getMessage(), t);
            throw new IdDoesNotExistException(new Exception(t));
        }

    }

    private ResultsPage<NodeRef> searchFor(int maxResponseLength, ResumptionToken.Value resumptionToken) throws IdDoesNotExistException {
        try {
            Map<String, String[]> searchCriteria = new HashMap<>();
            if (resumptionToken.hasFrom()) {
                searchCriteria.put("from", new String[]{convertDate(resumptionToken.getFrom())});
            }

            if (resumptionToken.hasUntil()) {
                searchCriteria.put("until", new String[]{convertDate(resumptionToken.getUntil())});
            }

            SearchToken token = new SearchToken();
            token.setFrom((int) resumptionToken.getOffset());
            token.setMaxResult(maxResponseLength);
            SortDefinition sort = new SortDefinition();
            sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.CM_PROP_C_CREATED, true));
            token.setSortDefinition(sort);
            token.setContentType(SearchService.ContentType.FILES);
            SearchResultNodeRef result = SearchServiceFactory.getLocalService().search(
                    MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id),
                    (!resumptionToken.hasSetSpec() || resumptionToken.getSetSpec().equals("default") ? "oai" : "oai_" + resumptionToken.getSetSpec()),
                    searchCriteria,
                    token);

            log.info("{}", result.getNodeCount());

            long delivered = resumptionToken.getOffset() + result.getData().size();
            return new ResultsPage<>(resumptionToken, delivered < result.getNodeCount(), result.getData(), result.getNodeCount());
        } catch (Throwable t) {
            log.warn(t.getMessage(), t);
            throw new IdDoesNotExistException(new Exception(t));
        }
    }

    private String convertDate(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
