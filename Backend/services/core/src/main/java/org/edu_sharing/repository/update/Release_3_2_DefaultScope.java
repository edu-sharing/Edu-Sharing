package org.edu_sharing.repository.update;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.edu_sharing.service.lti13.RepoTools.logger;

@Slf4j
@UpdateService
public class Release_3_2_DefaultScope {

    private final List<NodeRef> processedEduGroups = new ArrayList<>();
    private final RetryingTransactionHelper retryingTransactionHelper;
    private final NodeService nodeService;
    private final BehaviourFilter policyBehaviourFilter;

    private int processedNodeCounter = 0;

    @Autowired
    public Release_3_2_DefaultScope(RetryingTransactionHelper retryingTransactionHelper, @Qualifier("alfrescoDefaultDbNodeService") NodeService nodeService, @Qualifier("policyBehaviourFilter") BehaviourFilter policyBehaviourFilter) {
        this.retryingTransactionHelper = retryingTransactionHelper;

        this.nodeService = nodeService;
        this.policyBehaviourFilter = policyBehaviourFilter;
    }


    @UpdateRoutine(
            id = "Release_3_2_DefaultScope",
            description = "sets default scope for Maps and IO's",
            order = 3200,
            auto = true
    )
    public void execute(boolean test) {
        /**
         * disable policies for this node to prevent that beforeUpdateNode
         * checks the scope which will be there after update
         */
        setDefautScope(nodeService.getRootNode(MCAlfrescoAPIClient.storeRef), test);
    }

    void setDefautScope(NodeRef parent, boolean test) {
        List<ChildAssociationRef> childAssocRef = nodeService.getChildAssocs(parent);
        for (ChildAssociationRef child : childAssocRef) {
            NodeRef noderef = child.getChildRef();
            String name = (String) nodeService.getProperty(child.getChildRef(), ContentModel.PROP_NAME);
            String nodeType = nodeService.getType(child.getChildRef()).toString();

            if (nodeService.hasAspect(child.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE))) {
                continue;
            }

            //serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(cb)


            if (CCConstants.CCM_TYPE_IO.equals(nodeType)
                    || CCConstants.CCM_TYPE_TOOLPERMISSION.equals(nodeType)
                    || CCConstants.CCM_TYPE_NOTIFY.equals(nodeType)) {
                log.info("updateing node:" + noderef + " in " + nodeService.getPath(child.getChildRef()) + " processedNodeCounter:" + processedNodeCounter);
                if (!test) {

                    Map<QName, Serializable> aspectProps = new HashMap<>();
                    aspectProps.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME), null);


                    retryingTransactionHelper.doInTransaction((RetryingTransactionCallback<Void>) () -> {
                        policyBehaviourFilter.disableBehaviour(child.getChildRef());
                        nodeService.addAspect(child.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE), aspectProps);
                        policyBehaviourFilter.enableBehaviour(child.getChildRef());
                        return null;
                    });

                }
            } else if (CCConstants.CCM_TYPE_MAP.equals(nodeType) || CCConstants.CM_TYPE_FOLDER.equals(nodeType)) {
                log.info("updateing Map:" + noderef + " in " + nodeService.getPath(child.getChildRef()));

                if (!test) {
                    Map<QName, Serializable> aspectProps = new HashMap<>();
                    aspectProps.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME), null);
                    retryingTransactionHelper.doInTransaction((RetryingTransactionCallback<Void>) () -> {
                        policyBehaviourFilter.disableBehaviour(child.getChildRef());
                        nodeService.addAspect(child.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE), aspectProps);
                        policyBehaviourFilter.enableBehaviour(child.getChildRef());
                        return null;
                    });
                }

                if (processedEduGroups.contains(noderef)) {
                    log.info("already processed edugroup: " + name);
                    continue;
                } else {
                    log.info("remembering edugroup: " + name + " " + noderef);
                    processedEduGroups.add(noderef);
                }

                setDefautScope(noderef, test);
            }
            processedNodeCounter++;

        }
    }
}
