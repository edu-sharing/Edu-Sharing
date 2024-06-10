package org.edu_sharing.repository.server.importer;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.search.Suggestion;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FactualTermDisplayUpdater {

    Logger logger = Logger.getLogger(FactualTermDisplayUpdater.class);
    String appId = ApplicationInfoList.getHomeRepository().getAppId();
    MetadataSet mds = null;

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    BehaviourFilter policyBehaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");
    NodeService nodeService = serviceRegistry.getNodeService();

    public FactualTermDisplayUpdater() throws Exception {
        mds = MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(),"-default-");
    }


    public void updateDisplayStrings(String key) throws IllegalArgumentException {
        AuthenticationUtil.runAsSystem(()->{
            runforStore(key, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            runforStore(key, StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
            runforStore(key, new StoreRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),Version2Model.STORE_ID));
            return null;
        });
    }

    private void runforStore(String key, StoreRef storeRef) throws IllegalArgumentException {
        Map<String,Object> filter = new HashMap<>();
        filter.put(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD, key);
        List<NodeRef> nodeRefs = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO,filter,storeRef);
        logger.info("found "+nodeRefs.size() +" io's with classification_keyword:"+ key + " in store:" + storeRef);
        if(storeRef.equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE)
                || storeRef.equals(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE)){
            List<NodeRef> nodeRefsMap = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_MAP,filter,storeRef);
            nodeRefs.addAll(nodeRefsMap);
            logger.info("found "+nodeRefsMap.size() +" map's with classification_keyword:"+ key + " in store:" + storeRef);
        }

        for(NodeRef nodeRef : nodeRefs){
            List<String> keys = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD));
            ArrayList<String> displays = new ArrayList<>();
            for(String k : keys){
                List<? extends Suggestion> suggestions = MetadataSearchHelper.getSuggestions(appId, mds, "ngsearch",
                        CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD), k, null);
                displays.add(suggestions.get(0).getDisplayString());
            }
            logger.info("updateing;"+nodeRef+";"+ key);
            serviceRegistry.getRetryingTransactionHelper().doInTransaction(()->{
                try {

                    policyBehaviourFilter.disableBehaviour(nodeRef);
                    setProperty(nodeRef,QName.createQName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD_DISPLAY),displays);
                    new RepositoryCache().remove(nodeRef.getId());
                }finally {
                    policyBehaviourFilter.enableBehaviour(nodeRef);
                }
                return null;
            });
        }
    }

    private void setProperty(NodeRef nodeRef, QName qName, Serializable serializable){
        nodeService.setProperty(nodeRef, qName, serializable);
        if(nodeRef.getStoreRef().getIdentifier().equals(Version2Model.STORE_ID)){
            QName qnameV = QName.createQName(Version2Model.NAMESPACE_URI, Version2Model.PROP_METADATA_PREFIX + qName.toString());
            nodeService.setProperty(nodeRef, qnameV, serializable);
        }
    }

}
