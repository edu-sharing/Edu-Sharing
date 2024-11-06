package org.edu_sharing.repository.server.oai;

import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.exceptions.handler.IdDoesNotExistException;
import io.gdcc.xoai.dataprovider.filter.ScopedFilter;
import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.dataprovider.repository.ResultsPage;
import io.gdcc.xoai.model.oaipmh.DeletedRecord;
import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.model.oaipmh.OAIPMH;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.hc.core5.http.ContentType;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.oai.OAIExporterFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.edu_sharing.util.CheckedFunction;
import org.edu_sharing.xoai.EduDataHandler;
import org.edu_sharing.xoai.EduItem;
import org.edu_sharing.xoai.EduItemIdentifier;
import org.edu_sharing.xoai.EduOai;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
public class OaiServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            OaiSettings settings = ApplicationContextFactory.getApplicationContext().getBean(OaiSettings.class);
            OaiIdentifier identifier = settings.getIdentify();

            if (!settings.isEnabled()) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            int itemsPerPage = settings.getItemsPerPage();
            //oai/provider?verb=GetRecord&metadataPrefix=lom&identifier=3410648a-465e-47ff-87fe-706b89cecd65
            RepositoryConfiguration configuration = new RepositoryConfiguration.RepositoryConfigurationBuilder()
                    .withMaxListIdentifiers(itemsPerPage).withMaxListSets(itemsPerPage)
                    .withMaxListRecords(itemsPerPage).withMaxListSets(itemsPerPage)
                    .withAdminEmail(identifier.getAdminEmail())
                    .withBaseUrl(URLTool.getBaseUrl() + "/eduservlet/oai/provider")
                    .withGranularity(Granularity.valueOf(identifier.getGranularity()))
                    .withDeleteMethod(DeletedRecord.fromValue(identifier.getDelete()))
                    .withEarliestDate(ISODateTimeFormat.dateTimeNoMillis().parseDateTime(identifier.getEarliestDate()).toDate().toInstant())
                    .withRepositoryName(identifier.getName() != null ? identifier.getName() : ApplicationInfoList.getHomeRepository().getAppCaption())
                    .withDescription(identifier.getDescription())
                    .build();

            EduOai oai = new EduOai(configuration,
                    settings.getMetadataPrefix(),
                    new Handler(settings));
            OAIPMH response = oai.handleRequest(req.getParameterMap());

            // TODO anders!!!
            String responseXML = EduOai.responseToXML(response);

            // TODO anders!!!
            responseXML = OAIExporterFactory.getOAILOMExporter().postProcessResponse(req.getParameterMap(), responseXML);

            resp.setHeader("Content-Type", ContentType.APPLICATION_XML.getMimeType());
            resp.getOutputStream().write(responseXML.getBytes());
        } catch (Throwable t) {
            log.warn(t.getMessage(), t);
            resp.sendError(500, t.getMessage());
        }
    }

    private static class Handler implements EduDataHandler {
        private final ServiceRegistry serviceRegistry;
        @org.jetbrains.annotations.NotNull
        private final OaiSettings settings;
        private String identifierPrefix;

        public Handler(OaiSettings settings) {
            this.settings = settings;

            ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
            serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
            identifierPrefix = settings.getIdentiferPrefix();
            if (identifierPrefix == null) {
                identifierPrefix = "";
            }
        }

        @Override
        public List<String> getSets() {
            return settings.getSets();
        }

        @Override
        public ItemIdentifier getItemIdentifier(String identifier) throws IdDoesNotExistException {
            if (!identifier.startsWith(identifierPrefix)) {
                throw new IdDoesNotExistException("Invalid id, identifierPrefix does not match " + identifierPrefix);
            }

            String nodeId = identifier.substring(identifierPrefix.length());
            Instant date = ((Date) serviceRegistry.getNodeService().getProperty(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), QName.createQName(CCConstants.CM_PROP_C_MODIFIED))).toInstant();
            return new EduItemIdentifier(identifierPrefix, date);
        }

        @Override
        public Item getItem(String identifier, MetadataFormat format) throws HandlerException {
            // TODO alles neu!
            if (!identifier.startsWith(identifierPrefix)) {
                throw new IdDoesNotExistException("Invalid id, identifierPrefix does not match " + identifierPrefix);
            }
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                String nodeId = identifier.substring(identifierPrefix.length());
                OAIExporterFactory.getOAILOMExporter().write(os, nodeId);
                return new EduItem(identifier, os.toString());
            } catch (InvalidNodeRefException e) {
                throw new IdDoesNotExistException(e);
            } catch (Throwable t) {
                throw new IdDoesNotExistException(new Exception(t));
            }
        }

        @Override
        public ResultsPage<ItemIdentifier> getItemIdentifiers(List<ScopedFilter> filters, MetadataFormat metadataFormat, int maxResponseLength, ResumptionToken.Value resumptionToken) throws HandlerException {
            ResultsPage<NodeRef> nodeRefResultsPage = searchFor(maxResponseLength, resumptionToken);
            return resultsPage(nodeRefResultsPage, ref -> getItemIdentifier(identifierPrefix + ref.getNodeId()));
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
            return resultsPage(nodeRefResultsPage, ref -> getItem(ref.getNodeId(), metadataFormat));
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
                        (resumptionToken.hasSetSpec() || resumptionToken.getSetSpec().equals("default") ? "oai" : "oai_" + resumptionToken.getSetSpec()),
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
}
