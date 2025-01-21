package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.tika.utils.StringUtils;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.util.CSVTool;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

@JobDescription(description = "Set time restricted/temporary permissions for a given list of elements")
public class BulkSetTemporaryPermissionsJob extends AbstractJobMapAnnotationParams{


	@JobFieldDescription(description = "folder id to look for elements (defaults to IMP_OBJ folder)")
	private String startFolder;

	@JobFieldDescription(description = "Authority name to set permissions for", sampleValue = "GROUP_EVERYONE")
	private String authorityName;

	@JobFieldDescription(description = "Property to match the csv list against", sampleValue = "ccm:replicationsourceid")
	private String property;

	@JobFieldDescription(description = "Only run for for restricted access nodes?", sampleValue = "true")
	private boolean onlyRestrictedAccess = true;

	@JobFieldDescription(description = "Override permissions if the authority is already invited?", sampleValue = "false")
	private boolean overridePermissions = false;

	@JobFieldDescription(description = "Permissions to set, i.e. Consumer, CCPublish", sampleValue = "Consumer")
	private List<String> permissions;

	@JobFieldDescription(description = "The date/time on which the permission will end", sampleValue = "2035-12-31TT00:00:00Z")
	private Date until;

	@JobFieldDescription(description = "csv identifier list matching the property, one header \"identifier\" is required", file = true)
	private String data;






	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {

		AuthenticationUtil.runAsSystem(() -> {
			PermissionService permissionService = PermissionServiceFactory.getLocalService();
			PersistentHandlerEdusharing phe;
			try {
				phe = new PersistentHandlerEdusharing(this, null, false);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
			if (!StringUtils.isBlank(startFolder)) {
				phe.setImportFolderId(startFolder);
			}
			CSVTool.CSVResult csv = CSVTool.readCSV(new BufferedReader(new StringReader(data)), ',');
			assert csv != null;
			if (!csv.getHeaders().contains("identifier")) {
				throw new IllegalArgumentException("CSV file requires \"identifier\" header");
			}
			NodeRunner runner = new NodeRunner();
			runner.setNodesList(csv.getLines().stream().map(line -> {
				if (isInterrupted()) {
					return null;
				}
				String id = line.get("identifier");
				NodeRef nodeRef = phe.getNodeIfExists(
						new HashMap<>() {{
							put(CCConstants.getValidGlobalName(property), id);
						}}
				);
				if (nodeRef == null) {
					logger.warn("No node found for identifier " + property + ": " + id);
				}
				return nodeRef;
			}).filter(Objects::nonNull).collect(Collectors.toList()));

			runner.setTask((ref) -> {
				if (isInterrupted()) {
					return;
				}
				if (onlyRestrictedAccess) {
					if (!((Boolean) NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_RESTRICTED_ACCESS))) {
						logger.warn("Node " + ref + " is not restrictedAccess, skipping");
						return;
					}
				}

				try {
					logger.debug("processing node:" + ref.getId());
					ACE[] aces = permissionService.getPermissions(ref.getId()).getAces();
					if(!overridePermissions) {
						if(Arrays.stream(aces).anyMatch(ace -> ace.getAuthority().equals(authorityName))) {
							logger.info("Authority " + authorityName + " is already invited for node " + ref.getId());
							return;
						}
					}
					List<ACE> list = Arrays.stream(permissionService.getPermissions(ref.getId()).getAces()).filter(ace -> !ace.getAuthority().equals(authorityName)).collect(Collectors.toList());
					list.addAll(permissions.stream().map(p -> {
						ACE ace = new ACE(p, authorityName);
						ace.setTo(until.getTime());
						logger.info("Setting permission " + p + " for authority " + authorityName + " on " + ref.getId());
						return ace;
					}).collect(Collectors.toList()));
					permissionService.setPermissions(ref.getId(), list, null, null, false, false);

				} catch (Throwable t) {
					logger.error(t.getMessage(), t);
				}
			});
			runner.setRunAsSystem(true);
			runner.setThreaded(false);
			runner.setKeepModifiedDate(true);
			runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
			int count = runner.run();
			logger.info("Processed " + count + " nodes");
			return null;
		});
	}

	public void run() {

	}

	public enum Mode {
		@JobFieldDescription(description = "remove all local set permissions")
		Remove,
		@JobFieldDescription(description = "remove a given authority from the list (will do nothing if this authority was not invited on the node)")
		RemoveAuthority,
		@JobFieldDescription(description = "Add the CCPublish permission for all users that already have at least Consumer/Read permissions")
		AddCCPublish,
		@JobFieldDescription(description = "add a given authority from the list (will do nothing if this authority is already invited on the node)")
		ReplaceAuthority,
	}
}
