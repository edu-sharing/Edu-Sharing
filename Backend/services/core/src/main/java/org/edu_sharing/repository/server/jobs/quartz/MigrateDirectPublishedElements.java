package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.HandleMode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@JobDescription(description = "Migrate previously directly published element to published copies")
public class MigrateDirectPublishedElements extends AbstractJobMapAnnotationParams{

	protected Logger logger = Logger.getLogger(MigrateDirectPublishedElements.class);


	@JobFieldDescription(description = "Single node to migrate")
	private String nodeId;
	private NodeService nodeService;
	private BehaviourFilter policyBehaviourFilter;
	private ServiceRegistry serviceRegistry;

	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY, ServiceRegistry.class);
		nodeService = NodeServiceFactory.getLocalService();
		policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");
		if(!StringUtils.isBlank(nodeId)) {
			AuthenticationUtil.runAsSystem(() -> {
                migrate(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
				return null;
            });
			return;
		}
		NodeRunner runner = new NodeRunner();
		runner.setTask(this::migrate);
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setLucene("ISNOTNULL:\"ccm:published_handle_id\" AND ISNULL:\"ccm:published_original\" AND NOT ASPECT:\"ccm:collection_io_reference\"");
		runner.setKeepModifiedDate(false);
		runner.setTransaction(NodeRunner.TransactionMode.None);
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}

	private void migrate(NodeRef ref) {
		Serializable handleId = NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID);
		if(handleId == null) {
			logger.warn("Can not migrate node " + ref + " since it has no handle id");
			return;
		}
		if(NodeServiceHelper.hasAspect(ref, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
			logger.warn("Can not migrate node " + ref + " since it is a ref");
			return;
		}
		if(NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL) != null) {
			logger.warn("Can not migrate node " + ref + " since it is a published copy");
			return;
		}
		// copy the old publish date
		Serializable date = NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_IO_PUBLISHED_DATE);
		if (date != null) {
			logger.info("Keeping old published date " + date);
		} else {
			logger.warn("Old node had no published date! Will use cm:modified as fallback");
			date = NodeServiceHelper.getPropertyNative(ref, CCConstants.CM_PROP_C_MODIFIED);
		}
		try {
			// do not do anything with the handle for now!
			logger.info("Creating published copy of " + ref);
			NodeRef copy = serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(() -> {
				policyBehaviourFilter.disableBehaviour(ref);
				NodeRef copyInternal = new NodeRef(
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
						nodeService.publishCopy(ref.getId(), null));
				policyBehaviourFilter.enableBehaviour(ref);
				return copyInternal;
			});

			if(!NodeServiceHelper.exists(copy)) {
				logger.error("Copy failed for node: " + ref + ", missing node id: " + copy);
				return;
			}
			logger.info("Created copy: " + copy);
			Serializable finalDate = date;
			serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
				policyBehaviourFilter.disableBehaviour(copy);
				NodeServiceHelper.setProperty(copy, CCConstants.CM_PROP_C_CREATED, NodeServiceHelper.getPropertyNative(ref, CCConstants.CM_PROP_C_CREATED), true);
				NodeServiceHelper.setProperty(copy, CCConstants.CM_PROP_C_MODIFIED, NodeServiceHelper.getPropertyNative(ref, CCConstants.CM_PROP_C_MODIFIED), true);
				// now, fake the current history of copies to the directly published element so its handle id gets the update
				logger.info("Update old handle " + handleId + " from " + ref + " to " + copy);
				nodeService.createHandle(copy, Collections.singletonList(ref.getId()), HandleMode.update);

				// copy the old publish date
				if (finalDate != null) {
					NodeServiceHelper.setProperty(copy, CCConstants.CCM_PROP_IO_PUBLISHED_DATE, finalDate, true);
				}
				policyBehaviourFilter.enableBehaviour(copy);
				return null;
			});
			serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(() -> {
						policyBehaviourFilter.disableBehaviour(ref);
						Set<AccessPermission> perm = serviceRegistry.getPermissionService().getAllSetPermissions(ref).
								stream().filter(p -> p.getAccessStatus().equals(AccessStatus.ALLOWED) && p.getAuthority().equals(CCConstants.AUTHORITY_GROUP_EVERYONE)).collect(Collectors.toSet());
						logger.info("cleaning up permissions");
						if (perm.stream().anyMatch(p -> p.getPermission().equals(CCConstants.PERMISSION_CONSUMER))) {
							serviceRegistry.getPermissionService().deletePermission(ref, CCConstants.AUTHORITY_GROUP_EVERYONE, CCConstants.PERMISSION_CONSUMER);
						}
						if (perm.stream().anyMatch(p -> p.getPermission().equals(CCConstants.PERMISSION_CC_PUBLISH))) {
							serviceRegistry.getPermissionService().deletePermission(ref, CCConstants.AUTHORITY_GROUP_EVERYONE, CCConstants.PERMISSION_CC_PUBLISH);
						}
						NodeServiceHelper.removeProperty(ref, CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID);
						policyBehaviourFilter.enableBehaviour(ref);
						return null;
					});
			logger.info("done for node: " + ref + ", new copy: " + copy);
			new RepositoryCache().remove(ref.getId());

		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}
}
