package org.edu_sharing.alfresco.policy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.copy.CopyServicePolicies.OnCopyCompletePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class OnCopyTinymcePolicy implements OnCopyCompletePolicy {
	PolicyComponent policyComponent;

	NodeService nodeService;

	VersionService versionService;

	Logger logger = Logger.getLogger(OnCopyCollectionRefPolicy.class);

	public void init() {
		
		this.policyComponent.bindClassBehaviour(OnCopyCompletePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCopyComplete"));
		
	}

	@Override
	public void onCopyComplete(QName classRef, NodeRef sourceNodeRef, NodeRef targetNodeRef, boolean copyToNewNode,
			Map<NodeRef, NodeRef> copyMap) {

		for (Map.Entry<NodeRef, NodeRef> copyMapEntry : copyMap.entrySet()) {
			String editoryType = (String) nodeService.getProperty(copyMapEntry.getValue(),
					QName.createQName(CCConstants.CCM_PROP_EDITOR_TYPE));

			if ("tinymce".equals(editoryType)) {
				if (versionService.getVersionHistory(copyMapEntry.getValue()) == null) {
					Map<String, Serializable> transFormedProps = transformQNameKeyToString(
							nodeService.getProperties(copyMapEntry.getValue()));

					// see https://issues.alfresco.com/jira/browse/ALF-12815
					// alfresco-4.0.d fix version should start with 1.0 not with
					// 0.1
					transFormedProps.put(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR);
					transFormedProps.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, "MAIN_FILE_UPLOAD");
					
					versionService.createVersion(copyMapEntry.getValue(), transFormedProps);
				}
			}

		}

	}

	Map<String, Serializable> transformQNameKeyToString(Map<QName, Serializable> props) {
		Map<String, Serializable> result = new HashMap<>();
		for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
			result.put(entry.getKey().toString(), entry.getValue());
		}
		return result;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}
}
