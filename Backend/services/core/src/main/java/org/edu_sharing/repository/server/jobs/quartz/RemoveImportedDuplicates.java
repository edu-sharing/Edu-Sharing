package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.jobs.helper.NodeHelper;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.alfresco.service.cmr.version.VersionService;

import java.util.*;
import java.util.stream.Collectors;

public class RemoveImportedDuplicates extends AbstractJob{

    public static final String DESCRIPTION = "Find and remove Imported duplicates (got the same replicationsourceid";

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();

    VersionService versionService = serviceRegistry.getVersionService();
    BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");

    Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");

    Logger logger = Logger.getLogger(RemoveImportedDuplicates.class);

    @JobFieldDescription(description = "startfolder", sampleValue = "cm:name")
    String startFolder;

    @JobFieldDescription(description = "weather to start the job in persistent (true) or protocol mode (false). default is false", sampleValue = "false")
    Boolean execute;

    @JobFieldDescription(description = "username i.e. import job user. nodes where the current modifier equals this user, the versioncheck is skipped.")
    String ignoreModifierVersionCheck;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        startFolder = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("startFolder");
        execute = (Boolean)jobExecutionContext.getJobDetail().getJobDataMap().get("execute");
        if(execute == null) execute = false;
        ignoreModifierVersionCheck = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("ignoreModifierVersionCheck");
        AuthenticationUtil.runAsSystem(() -> {
                    excecute(startFolder,execute,ignoreModifierVersionCheck);
                    return null;
                }
        );
    }

    private void excecute(String startFolder, boolean execute, String ignoreModifierVersionCheck){
        if(startFolder == null || startFolder.trim().equals("")){
            for(ChildAssociationRef ref : nodeService.getChildAssocs(repositoryHelper.getCompanyHome())){
                if(OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS.equals(nodeService.getProperty(ref.getChildRef(), ContentModel.PROP_NAME))){
                    startFolder = ref.getChildRef().getId();
                }
            }
            if(startFolder == null){
                logger.error("no imported objects folder found");
                return;
            }
        }

        Map<String, List<NodeRef>> duplicates = new NodeHelper().getDuplicatedImportedNodes(startFolder);
        logger.info("found "+ duplicates.size() +" duplicates in import folder  " + startFolder );

        for(Map.Entry<String, List<NodeRef>> entry : duplicates.entrySet()){
            logger.info(
                    entry.getKey() + ": found duplicates: "  +
                    nodeService.getProperty(
                            entry.getValue().get(0),
                            QName.createQName(CCConstants.CM_NAME)
                    ) + " " + entry.getValue().stream().map(NodeRef::getId).collect(Collectors.joining(","))
            );
            Map<NodeRef, Integer> result = new HashMap<>();
            for(NodeRef nodeRef : entry.getValue()){
                int isInUse = 0;
                List<ChildAssociationRef> children = nodeService.getChildAssocs(nodeRef, new HashSet(Arrays.asList(new QName[]{
                        QName.createQName(CCConstants.CCM_TYPE_USAGE)})));
                if(children != null && children.size() > 0){
                    // logger.error(entry.getKey() + ": can not remove " + nodeRef + " cause of usages " + children);
                    isInUse += children.size();
                    logger.info(" " + entry.getKey() +" " + nodeRef +": has usage");
                }
                List<String> curriculum = (List<String>)nodeService.getProperty(nodeRef,QName.createQName(CCConstants.getValidGlobalName("ccm:curriculum")));
                if(curriculum != null && curriculum.size() > 0){
                    // logger.error(entry.getKey() + ": can not remove " + nodeRef + " cause of ccm:curriculum: " + String.join(",",curriculum));
                    isInUse++;
                    logger.info(" " + entry.getKey()+" " + nodeRef +": has curriculum");
                }
                String modifier = (String)nodeService.getProperty(nodeRef,ContentModel.PROP_MODIFIER);
                if(ignoreModifierVersionCheck == null || (!ignoreModifierVersionCheck.equals(modifier) && !"admin".equals(modifier))) {
                    VersionHistory versionHistory = versionService.getVersionHistory(nodeRef);
                    if (versionHistory != null && versionHistory.getAllVersions() != null && versionHistory.getAllVersions().size() > 1) {
                        isInUse++;
                        logger.info(" " + entry.getKey() + " " + nodeRef + ": has > 1 versions");
                    }
                }else{
                    logger.info(" " + entry.getKey() + " " + nodeRef + ":version check ignored");
                }

                result.put(nodeRef, isInUse);
            }
            List<NodeRef> toDelete = result.entrySet().stream().filter((e) -> e.getValue() == 0).
                    map((e) -> e.getKey()).collect(Collectors.toList());
            if(toDelete.size() == result.size()) {
                logger.info(entry.getKey() + ": None of all " + toDelete.size() + " nodes have usages, will delete all but latest");
                toDelete.sort((a, b) ->
                        ((Date)nodeService.getProperty(b, QName.createQName(CCConstants.CM_PROP_C_MODIFIED))).
                        compareTo((Date)nodeService.getProperty(a, QName.createQName(CCConstants.CM_PROP_C_MODIFIED)))
                );
                toDelete.remove(0);
            } else if (toDelete.size() == result.size() - 1) {
                logger.info(entry.getKey() + ": Exactly one node has usages, will delete all but this one");
            } else {
                logger.error(entry.getKey() + ": " + (result.size() - toDelete.size()) + " nodes have usages, won't delete anything. Please manually check:");
                result.forEach((key, value) -> logger.log(value > 0 ? Level.WARN : Level.INFO, entry.getKey() + ": Node id " + key + ", hasUsages: " + value));
                toDelete.clear();
            }

            toDelete.forEach((ref) -> {
                logger.info(entry.getKey()+": delete node:" + ref);
                if(execute) {
                    nodeService.addAspect(ref, ContentModel.ASPECT_TEMPORARY, null);
                    nodeService.deleteNode(ref);
                }
            });
        }
    }
}
