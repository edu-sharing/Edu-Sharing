package org.edu_sharing.alfresco.fixes;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.copy.CopyServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetadataPresettingPolicy implements
		NodeServicePolicies.OnCreateNodePolicy,
		CopyServicePolicies.OnCopyCompletePolicy,
		NodeServicePolicies.OnMoveNodePolicy,
		NodeServicePolicies.BeforeDeleteNodePolicy {

	private static final Log logger = LogFactory
			.getLog(MetadataPresettingPolicy.class);

	private static final QName CONTENT_TYPE = QName
			.createQName(CCConstants.CCM_TYPE_IO);

	private static final QName ASPECT_TYPE = QName
			.createQName(CCConstants.CCM_ASPECT_METADATA_PRESETTING);

	private static final QName ASPECT_PROP = QName
			.createQName(CCConstants.CCM_PROP_METADATA_PRESETTING_PROPERTIES);

	private static final QName ASPECT_ASSOC = QName
			.createQName(CCConstants.CCM_ASSOC_METADATA_PRESETTING_TEMPLATE);

	private final List<QName> defaultQNames = new ArrayList<>();

	/**
	 * The common node service
	 */
	protected NodeService nodeService;

	/**
	 * Policy component
	 */
	protected PolicyComponent policyComponent;

	/**
	 * Default Properties
	 */
	protected List<String> defaultProperties;

	/**
	 * Flag for delete policy
	 */
	protected boolean protectTemplatesInUse;

	/**
	 * Spring bean init method
	 */
	public void init() {

		logger.info("called!");

		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME,
				CONTENT_TYPE, new JavaBehaviour(this, "onCreateNode"));

		policyComponent.bindClassBehaviour(NodeServicePolicies.OnMoveNodePolicy.QNAME,
				CONTENT_TYPE, new JavaBehaviour(this, "onMoveNode"));

		policyComponent.bindClassBehaviour(CopyServicePolicies.OnCopyCompletePolicy.QNAME,
				CONTENT_TYPE, new JavaBehaviour(this, "onCopyComplete"));

		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME,
				ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onCreateNode"));

		policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME,
				CONTENT_TYPE, new JavaBehaviour(this, "beforeDeleteNode"));

		defaultQNames.clear();
		if (defaultProperties != null) {
			for (String defaultProperty : defaultProperties) {
				defaultQNames.add(QName.createQName(defaultProperty));
			}
		}
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setDefaultProperties(List<String> defaultProperties) {
		this.defaultProperties = defaultProperties;
	}

	public void setProtectTemplatesInUse(boolean protectTemplatesInUse) {
		this.protectTemplatesInUse = protectTemplatesInUse;
	}

	@Override
	public void onCopyComplete(QName qName, NodeRef nodeRef, NodeRef copy, boolean b, Map<NodeRef, NodeRef> map) {
		inheritMetadata(copy,nodeService.getPrimaryParent(copy).getParentRef());
	}

	@Override
	public void onMoveNode(ChildAssociationRef source, ChildAssociationRef target) {
		inheritMetadata(target.getChildRef(),target.getParentRef());
	}

	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		inheritMetadata(childAssocRef.getChildRef(),childAssocRef.getParentRef());
	}

	private void inheritMetadata(NodeRef targetRef,NodeRef parentRef) {
		logger.info("Starting inherit metadata for "+targetRef+" inside folder "+parentRef);
		if (ContentModel.TYPE_CONTENT.equals(nodeService.getType(targetRef))) {
			nodeService.setType(targetRef, CONTENT_TYPE);
		}

		if (!CONTENT_TYPE.equals(nodeService.getType(targetRef))) {
			return;
		}
		Boolean status = (Boolean) nodeService.getProperty(parentRef, QName.createQName(CCConstants.CCM_PROP_METADATA_PRESETTING_STATUS));
		if (nodeService.hasAspect(parentRef, ASPECT_TYPE) && status!=null && status) {
			// assoc is a good idea but will always get lost when a node is moved to recycle
			//List<AssociationRef> templates = nodeService.getTargetAssocs(parentRef, ASPECT_ASSOC);
			NodeRef template = nodeService.getChildByName(parentRef,ContentModel.ASSOC_CONTAINS,CCConstants.TEMPLATE_NODE_NAME);

			if (template==null) {

				logger.error("metadataPresettingPolicy for folder(" + parentRef
						+ ") failed: there's no template specified .");
				return;
			}

			if (!nodeService.exists(template)) {
				logger.error("metadataPresettingPolicy for folder(" + parentRef
						+ ") failed: template (" + template
						+ ") doesn't exist.");
				return;
			}

			/*
			List<QName> props = (List<QName>) nodeService.getProperty(
					parentRef, ASPECT_PROP);

			if (props == null || props.size() < 1) {
				props = defaultQNames;
			}

			for (QName prop : props) {

				Serializable value = nodeService.getProperty(sourceRef, prop);

				if (value != null) {
					nodeService.setProperty(targetRef, prop, value);
				}

			}
			*/
			inheritMetadata(parentRef,template,targetRef);
		}
	}

	private void inheritMetadata(NodeRef parentRef,NodeRef templateRef,NodeRef targetRef){

		String mdsId=CCConstants.metadatasetdefault_id;
		String mdsProp = (String)nodeService.getProperty(
				parentRef, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
		if(mdsProp!=null && !mdsProp.trim().isEmpty()){
			mdsId=mdsProp;
		}
		try {
			MetadataSet mds = MetadataReader.getMetadataset(ApplicationInfoList.getHomeRepository(), mdsId,"default");
			for(MetadataWidget widget : mds.getWidgetsByTemplate("io_template")){
				QName prop = QName.createQName(CCConstants.getValidGlobalName(widget.getId()));
				Serializable value = nodeService.getProperty(templateRef, prop);
				Serializable current = nodeService.getProperty(targetRef, prop);
				if(value!=null) {
					// mutli value: try to merge the values
					if(widget.isMultivalue() && current!=null && current instanceof List && value instanceof List){
						List currentList = (List) current;
						List valueList = (List) value;
						for(Object v : valueList){
							if(!currentList.contains(v)){
								currentList.add(v);
							}
						}
						nodeService.setProperty(targetRef,prop, (Serializable) currentList);
					}
					else{
						nodeService.setProperty(targetRef, prop, value);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void beforeDeleteNode(NodeRef nodeRef) {

		if (protectTemplatesInUse
				&& nodeService.getSourceAssocs(nodeRef, ASPECT_ASSOC).size() > 0) {

			throw new NodeLockedException(nodeRef);

		}

	}

}
