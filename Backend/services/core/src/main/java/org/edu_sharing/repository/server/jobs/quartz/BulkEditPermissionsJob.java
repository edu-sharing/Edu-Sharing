package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@JobDescription(description = "Bulk edit node permissions")
public class BulkEditPermissionsJob extends AbstractJobMapAnnotationParams{

	protected Logger logger = Logger.getLogger(BulkEditPermissionsJob.class);


	@JobFieldDescription(description = "folder id to start from")
	private String startFolder;
	@JobFieldDescription(description = "Lucene query to fetch the nodes that shall be processed. When used, the 'startFolder' parameter is ignored")
	private String lucene;

	@JobFieldDescription(description = "Mode to use")
	private Mode mode;
	@JobFieldDescription(description = "Authority name, only if mode == RemoveAuthority or mode == AddCCPublish")
	private String authorityName;

	@JobFieldDescription(description = "Element types to process, e.g. ccm:map,ccm:io")
	private List<String> types;




	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		PermissionService permissionService = serviceRegistry.getPermissionService();
		if(mode == null){
			throw new IllegalArgumentException("Missing required boolean parameter 'mode'");
		}
		if(startFolder==null || startFolder.isEmpty()){
			throw new IllegalArgumentException("Missing required parameter 'startFolder'");
		}
		if(types == null){
			throw new IllegalArgumentException("Missing required boolean parameter 'types'");
		}

		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			if(isInterrupted()) {
				return;
			}
			try {
				logger.debug("processing node:" + ref.getId());
				if(mode.equals(Mode.RemoveAuthority)) {
					Set<AccessPermission> permissions = permissionService.getAllSetPermissions(ref);
					List<AccessPermission> result = permissions.stream().filter(p -> p.getAuthority().equals(authorityName)).collect(Collectors.toList());
					if(!result.isEmpty()) {
						logger.info("Node " + ref.getId() + " has authority invited:" + StringUtils.join(result.stream().filter(p -> AccessStatus.ALLOWED.equals(p.getAccessStatus())).collect(Collectors.toList()), ", "));
						permissionService.clearPermission(ref, authorityName);
					}
				} else if(mode.equals(Mode.Remove)) {
					logger.info("Node " + ref.getId() + ": all local permissions will be removed");
					permissionService.deletePermissions(ref);
				} else if(mode.equals(Mode.AddCCPublish)) {
					Set<AccessPermission> permissions = permissionService.getAllSetPermissions(ref);
					permissions.stream().filter(p ->
							// authority name equals given authority or do for all
							(StringUtils.isBlank(authorityName) || p.getAuthority().equals(authorityName)) &&
									// only apply for explicit permissions
									!p.isInherited() &&
									p.getAccessStatus().equals(AccessStatus.ALLOWED) &&
									Arrays.asList(CCConstants.PERMISSION_READ, CCConstants.PERMISSION_CONSUMER).contains(p.getPermission())
					).forEach(p -> {
						// if CCPublish is already present for this user, do nothing
						if (permissions.stream().anyMatch(p2 -> p.getAuthority().equals(p2.getAuthority()) && p2.getPermission().equals(CCConstants.PERMISSION_CC_PUBLISH) && p2.getAccessStatus().equals(AccessStatus.ALLOWED))) {
							return;
						}
						permissionService.setPermission(ref, p.getAuthority(), CCConstants.PERMISSION_CC_PUBLISH, true);
						logger.info("Added CCPublish for user " + p.getAuthority() + " on node " + ref);
					});

				}
			}catch (Exception e){
				logger.error(e.getMessage(),e);
			}
		});
		runner.setTypes(types!=null && !types.isEmpty() ? types.stream().map(CCConstants::getValidGlobalName).collect(Collectors.toList()) : null);
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setStartFolder(startFolder);
		runner.setLucene(lucene);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}

	public void run() {

	}

	public enum Mode {
		@JobFieldDescription(description = "remove all local set permissions")
		Remove,
		@JobFieldDescription(description = "remove a given authority from the list (will do nothing if this authority was not invited on the node)")
		RemoveAuthority,
		@JobFieldDescription(description = "Add the CCPublish permission for all users that already have at least Consumer/Read permissions")
		AddCCPublish
	}
}
