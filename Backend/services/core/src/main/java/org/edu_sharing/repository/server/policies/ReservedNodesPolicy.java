package org.edu_sharing.repository.server.policies;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.CopyServicePolicies;
import org.alfresco.repo.copy.DefaultCopyBehaviourCallback;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.collection.DuplicateNodeException;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservedNodesPolicy implements NodeServicePolicies.OnCreateNodePolicy, CopyServicePolicies.OnCopyNodePolicy, NodeServicePolicies.OnUpdatePropertiesPolicy {

    private final PolicyComponent policyComponent;
    private final NodeService nodeService;

    private final List<String> reservedNodeNames = List.of("inbox", "savedsearch");

    private final List<String> types = List.of(CCConstants.CCM_TYPE_IO, CCConstants.CM_TYPE_FOLDER);

    @PostConstruct
    public void init() {
        types.forEach(type -> {
            policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, QName.createQName(type), new JavaBehaviour(this, "onCreateNode"));
            policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdatePropertiesPolicy.QNAME, QName.createQName(type), new JavaBehaviour(this, "onUpdateProperties"));
            policyComponent.bindClassBehaviour(CopyServicePolicies.OnCopyNodePolicy.QNAME, QName.createQName(type), new JavaBehaviour(this, "getCopyCallback"));
        });
    }

    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef) {
        checkForResveredNames(childAssocRef.getParentRef(), childAssocRef.getChildRef());
    }

    @Override
    public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
        if(Objects.equals(before.get(ContentModel.PROP_NAME), after.get(ContentModel.PROP_NAME))){
            return;
        }
        ChildAssociationRef primaryParent = nodeService.getPrimaryParent(nodeRef);
        checkForResveredNames(primaryParent.getParentRef(),  nodeRef);
    }

    @Override
    public CopyBehaviourCallback getCopyCallback(QName classRef, CopyDetails copyDetails) {
        checkForResveredNames(copyDetails.getTargetParentNodeRef(), copyDetails.getSourceNodeRef());
        return new DefaultCopyBehaviourCallback();
    }

    private void checkForResveredNames(NodeRef parentRef, NodeRef childRef) {
        try {

            String userName = new AuthenticationToolAPI().getAuthentication(Context.getCurrentInstance().getRequest().getSession()).get(CCConstants.AUTH_USERNAME);
            MCAlfrescoAPIClient baseClient = new MCAlfrescoAPIClient();
            String homeFolderID = baseClient.getHomeFolderID(userName);
            if (!parentRef.getId().equals(homeFolderID)) {
                return;
            }

            Map<QName, Serializable> props = nodeService.getProperties(childRef);
            String name = (String) props.get(ContentModel.PROP_NAME);

            if (reservedNodeNames.contains(name.trim().toLowerCase())) {
                throw new DuplicateNodeException(CCConstants.getValidLocalName(CCConstants.CM_NAME) + ": " + name + " is a reserved folder name");
            }

        } catch (DuplicateNodeException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
}
