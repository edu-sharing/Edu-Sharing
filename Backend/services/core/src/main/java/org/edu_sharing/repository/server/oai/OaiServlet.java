package org.edu_sharing.repository.server.oai;

import com.typesafe.config.Config;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.dspace.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import org.dspace.xoai.dataprovider.model.ItemIdentifier;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;
import org.dspace.xoai.model.oaipmh.OAIPMH;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
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
import org.edu_sharing.xoai.EduDataHandler;
import org.edu_sharing.xoai.EduItem;
import org.edu_sharing.xoai.EduItemIdentifier;
import org.edu_sharing.xoai.EduOai;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


public class OaiServlet extends HttpServlet{
    private static final int MAX_ITEMS_PER_PAGE = 300;
    private static Logger logger = Logger.getLogger(OaiServlet.class);
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Config config = LightbendConfigLoader.get().getConfig("exporter.oai.identify");
            if(!LightbendConfigLoader.get().getBoolean("exporter.oai.enabled")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            int itemsPerPage =  LightbendConfigLoader.get().getInt("exporter.oai.itemsPerPage");
            //oai/provider?verb=GetRecord&metadataPrefix=lom&identifier=3410648a-465e-47ff-87fe-706b89cecd65
            RepositoryConfiguration configuration = new RepositoryConfiguration().
                    withMaxListIdentifiers(itemsPerPage).withMaxListSets(itemsPerPage).
                    withMaxListRecords(itemsPerPage).withMaxListSets(itemsPerPage).
                    withAdminEmail(config.getString("adminEmail")).
                    withBaseUrl(URLTool.getBaseUrl() + "/eduservlet/oai/provider").
                    withGranularity(Granularity.valueOf(config.getString("granularity"))).
                    withDeleteMethod(DeletedRecord.fromValue(config.getString("delete"))).
                    withEarliestDate(ISODateTimeFormat.dateTimeNoMillis().parseDateTime(config.getString("earliestDate")).toDate()).
                    withRepositoryName(config.hasPath("name") ? config.getString("name") : ApplicationInfoList.getHomeRepository().getAppCaption()).
                    withDescription(config.getString("description"));

            EduOai oai = new EduOai(configuration,
                    LightbendConfigLoader.get().getString("exporter.oai.metadataPrefix"),
                    new Handler());
            Map<String, List<String>> request = mapRequest(req);
            OAIPMH response = oai.handleRequest(request);
            String responseXML = EduOai.responseToXML(response);
            responseXML = OAIExporterFactory.getOAILOMExporter().postProcessResponse(request, responseXML);
            resp.setHeader("Content-Type","application/xml");
            resp.getOutputStream().write(responseXML.getBytes());
        }
        catch(Throwable t){
            logger.warn(t.getMessage(),t);
            resp.sendError(500,t.getMessage());
        }
    }

    private Map<String, List<String>> mapRequest(HttpServletRequest req) {
        Map<String, List<String>> request=new HashMap<>();
        for(Object r : req.getParameterMap().entrySet()){
            Map.Entry<String,String[]> data= (Map.Entry<String, String[]>) r;
            request.put(data.getKey(), Arrays.asList(data.getValue()));
        }
        return request;
    }

    private class Handler implements EduDataHandler {
        private final ServiceRegistry serviceRegistry;
        private String identifierPrefix;

        public Handler(){

            ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
            serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
            identifierPrefix = LightbendConfigLoader.get().getString("exporter.oai.identiferPrefix");
            if(identifierPrefix == null){
                identifierPrefix = "";
            }
        }
        @Override
        public ListItemIdentifiersResult getIdentifiers(int from, int length, String set) throws OAIException {
            return getIdentifiersSolr(from, length,new HashMap<>(), set);
        }

        private ListItemIdentifiersResult getIdentifiersSolr(int from, int length,Map<String,String[]> searchCriterias, String set) throws OAIException{
            try {
                SearchToken token=new SearchToken();
                token.setFrom(from);
                token.setMaxResult(length);
                SortDefinition sort=new SortDefinition();
                sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.CM_PROP_C_CREATED,true));
                token.setSortDefinition(sort);
                token.setContentType(SearchService.ContentType.FILES);
                SearchResultNodeRef result = SearchServiceFactory.getLocalService().search(
                        MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id),
                        (set == null || set.equals("default") ? "oai" : "oai_" + set),
                        searchCriterias,
                        token);
                List<ItemIdentifier> refs = result.getData().stream().map((ref) -> new EduItemIdentifier(identifierPrefix + ref.getNodeId(),getDate(ref))).collect(Collectors.toList());
                logger.info(result.getNodeCount());
                int delivered=from+result.getData().size();
                return new ListItemIdentifiersResult(delivered<result.getNodeCount(),refs,result.getNodeCount());
            } catch (Throwable t) {
                logger.warn(t.getMessage(),t);
                throw new OAIException(new Exception(t));
            }
        }

        @Override
        public List<String> getSets() {
            return LightbendConfigLoader.get().getStringList("exporter.oai.sets");
        }

        @Override
        public ListItemIdentifiersResult getIdentifiersFrom(int from, int length, Date date, String set) throws OAIException {
            Map<String, String[]> criterias = new HashMap<>();
            criterias.put("from",new String[]{convertDateSolr(date)});
            return getIdentifiersSolr(from, length,criterias, set);
        }

        private String convertDateSolr(Date date) {
            return DateTimeFormatter.ISO_INSTANT.format(date.toInstant());
    }

        @Override
        public ListItemIdentifiersResult getIdentifiersUntil(int from, int length, Date date, String set) throws OAIException {
            Map<String, String[]> criterias = new HashMap<>();
            criterias.put("until",new String[]{convertDateSolr(date)});
            return getIdentifiersSolr(from, length,criterias, set);        }

        @Override
        public ListItemIdentifiersResult getIdentifiersFromUntil(int from, int length, Date fromDate, Date untilDate, String set) throws OAIException {
            Map<String, String[]> criterias = new HashMap<>();
            criterias.put("from",new String[]{convertDateSolr(fromDate)});
            criterias.put("until",new String[]{convertDateSolr(untilDate)});
            return getIdentifiersSolr(from, length, criterias, set);
        }
        private Date getDate(NodeRef ref) {
            return (Date) serviceRegistry.getNodeService().getProperty(new org.alfresco.service.cmr.repository.NodeRef(ref.getStoreProtocol(),ref.getStoreId(),ref.getNodeId()),QName.createQName(CCConstants.CM_PROP_C_MODIFIED));
        }

        @Override
        public EduItem getItem(String id) throws IdDoesNotExistException, OAIException{
            if(!id.startsWith(identifierPrefix)){
                throw new IdDoesNotExistException("Invalid id, identifierPrefix does not match " + identifierPrefix);
            }
            try {
                ByteArrayOutputStream os=new ByteArrayOutputStream();
                String nodeId = id.substring(identifierPrefix.length());
                OAIExporterFactory.getOAILOMExporter().write(os, nodeId);
                return new EduItem(id, os.toString());
            }catch(InvalidNodeRefException e){
                throw new IdDoesNotExistException(e);
            }catch(Throwable t){
                throw new OAIException(new Exception(t));
            }
        }
    }
}
