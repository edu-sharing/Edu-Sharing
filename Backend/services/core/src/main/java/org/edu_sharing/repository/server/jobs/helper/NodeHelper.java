package org.edu_sharing.repository.server.jobs.helper;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class NodeHelper {

    Logger logger = Logger.getLogger(NodeHelper.class);

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    /**
     *
     * @param startFolder
     * @return map with ccm:replicationSourceId as key and nodeRef as value
     */
    public Map<String, NodeRef> getImportedNodes(String startFolder) {
        Map<String, NodeRef> result = new HashMap<>();
        int processNodes = runTask(startFolder,(ref) -> {
            String replicationSourceId = (String) serviceRegistry.getNodeService().getProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID));
            if (replicationSourceId != null) {
                result.put(replicationSourceId, ref);
            }});
        logger.info("processed nodes:" + processNodes + " size:" + result.size());
        return result;
    }

    /**
     *
     * @param startFolder
     * @return map with duplicates: ccm:replicationSourceId as key and list of nodeRef as value
     */
    public Map<String, List<NodeRef>> getDuplicatedImportedNodes(String startFolder){
        Map<String, List<NodeRef>> collect = new HashMap<>();
        int processNodes = runTask(startFolder,(ref)->{
            String replicationSourceId = (String) serviceRegistry.getNodeService().getProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID));
            if (replicationSourceId != null && !replicationSourceId.trim().equals("")) {
                List<NodeRef> list = collect.get(replicationSourceId);
                if(list == null){
                    list = new ArrayList<>();
                    collect.put(replicationSourceId,list);
                }
                if(!list.contains(ref)){
                    list.add(ref);
                }else{
                    logger.error(ref+ " is already in list for "+replicationSourceId);
                }
            }
        });

        //remove all unique entries from map
        Map<String, List<NodeRef>> result  = collect.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry<String, List<NodeRef>>::getKey,Map.Entry<String, List<NodeRef>>::getValue));
        logger.info("found:"+result.size()+" duplicates in: " + collect.size());
        return result;
    }

    public List<NodeRef> getNodes(String startFolder){
        List<NodeRef> result = new ArrayList<>();
        int processNodes = runTask(startFolder,(ref) -> { result.add(ref);});
        logger.info("processed nodes:" + processNodes + " size:" + result.size());
        return result;
    }

    private int runTask(String startFolder, Consumer<NodeRef> task) {

        NodeRunner runner = new NodeRunner();
        runner.setTask(task);
        runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
        runner.setStartFolder(startFolder);
        runner.setRunAsSystem(true);
        runner.setTransaction(NodeRunner.TransactionMode.Local);
        runner.setThreaded(false);

        int processNodes = runner.run();
        return processNodes;
    }
}
