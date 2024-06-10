package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.jobs.helper.NodeHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ProtocolDiffDBSolrJob extends AbstractJob{

    public static final String PARAM_START_FOLDER = "START_FOLDER";
    public static final String DESCRIPTION = "logs diff solr and db";
    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    Logger logger = Logger.getLogger(ProtocolDiffDBSolrJob.class);

    int PAGE_SIZE = 1000;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        String startFolder = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_START_FOLDER);

        if(startFolder == null){
            logger.error("no "+PARAM_START_FOLDER+ " provided");
            return;
        }
        AuthenticationUtil.runAsSystem(() -> {
            run(startFolder);
            return null;
        });
    }

    private void run(String startFolder){
        Path path = serviceRegistry.getNodeService().getPath(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,startFolder));

        String pathPrefixString = path.toPrefixString(serviceRegistry.getNamespaceService());
        logger.info("pathPrefixString:"+pathPrefixString);
        String query = "PATH:\""+pathPrefixString+"//*\" AND TYPE:\"ccm:io\"";


        logger.info("collect solr nodes " + query);
        ResultSet rs = null;
        int skipCount = 0;
        List<NodeRef> nodesInSolr = new ArrayList<>();
        do{
            SearchParameters sp = new SearchParameters();
            sp.setQuery(query);
            sp.setLanguage(SearchService.LANGUAGE_LUCENE);
            sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            sp.setMaxItems(PAGE_SIZE);
            sp.setSkipCount(skipCount);
            rs = serviceRegistry.getSearchService().query(sp);
            logger.info("found "+ rs.getNumberFound() +" in solr index. skipCount:" + skipCount+". currentPageSize: "+rs.length());
            nodesInSolr.addAll(rs.getNodeRefs());
            skipCount = skipCount + PAGE_SIZE;
        }while( rs.getNumberFound() > skipCount);

        logger.info("collection db nodes");
        List<NodeRef> nodesInDb = new NodeHelper().getNodes(startFolder);

        List<NodeRef> diff = new ArrayList<>(nodesInDb);
        diff.removeAll(nodesInSolr);
        if(diff.size() == 0){
            logger.info("no diff between database and solr in this folder " + startFolder);
        }
        for(NodeRef node : diff){
            logger.info("in db not in solr:"+node);
        }
    }
}
