package org.edu_sharing.repository.server.jobs.quartz;

import com.opencsv.CSVWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.NodeTool;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.mediacenter.MediacenterService;
import org.edu_sharing.service.mediacenter.MediacenterServiceFactory;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import javax.mail.Store;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@JobDescription(description = "Creates reports for all mediacenters on the 1st of each month for the last month")
public class MediacenterMonthlyReportsJob extends AbstractJobMapAnnotationParams {

	public enum ReportMode {
		@JobFieldDescription(description = "Use the tracked mediacenter user data. Elements accessed from users assigned to more than one mediacenter are ignored")
		TrackingMediacenterData,
		@JobFieldDescription(description = "Use the current licensed node data. Elements not licensed anymore will not be visible. Elements accessed from users assigned to more than one MZ are counted as well (legacy)")
		AlfrescoPermissionData
	}
	@JobFieldDescription(description = "Mode to use for processing")
	private ReportMode mode;

	protected Logger logger = Logger.getLogger(MediacenterMonthlyReportsJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;
	private NodeService nodeServiceEdu;

	@JobFieldDescription(description = "List of properties to include in the export file, first value is the property, second value (only for vcard) is the vcard field id", sampleValue = "[\"cm:name\"]")
	private List<List<String>> columns = new ArrayList<List<String>>() {{
		add(Collections.singletonList("cclom:title"));
		add(Collections.singletonList("ccm:replicationsourceid"));
		add(Collections.singletonList("ccm:lifecyclecontributer_publisher"));
		add(Arrays.asList("ccm:lifecyclecontributer_publisher","X-ES-LOM-CONTRIBUTE-DATE"));
		add(Collections.singletonList("sys:node-uuid"));
	}};
	@JobFieldDescription(description = "List of additional (custom) fields to be fetched from the tracking data", sampleValue = "field1")
	private List<String> additionalFields = Collections.emptyList();

	static List<TrackingService.EventType> STAT_FIELDS = Arrays.asList(
			TrackingService.EventType.VIEW_MATERIAL,
			TrackingService.EventType.VIEW_MATERIAL_EMBEDDED,
			TrackingService.EventType.VIEW_MATERIAL_PLAY_MEDIA,
			TrackingService.EventType.DOWNLOAD_MATERIAL
	);
	@JobFieldDescription(description = "Optional, set a list of mediacenters to apply, otherwise it will run for all", sampleValue = "GROUP_MEDIA_CENTER_1")
	private List<String> mediacenters;
	@JobFieldDescription(description = "force run, even if the date is currently not the 1st")
	private boolean force = false;

	@JobFieldDescription(description = "use a custom date (month) to run the job for. Note: The job will run the month BEFORE the given date!", sampleValue = "YYYY-MM-DD")
	private Date customDate = null;


	@JobFieldDescription(description = "Delete stats of the month if they're already existing")
	private boolean delete = false;
	private ContentService contentService;


	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		contentService = serviceRegistry.getContentService();
		nodeService = serviceRegistry.getNodeService();
		nodeServiceEdu = NodeServiceFactory.getLocalService();

		Date date = new Date();
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		if (!force && localDate.getDayOfMonth() != 1) {
			logger.info("Job not running because of date: " + localDate.getDayOfMonth());
			return;
		}
		if(mode == null) {
			mode = ReportMode.TrackingMediacenterData;
		}
		AuthenticationUtil.runAsSystem(this::createStats);
	}

	private Void createStats() {
		try {
			Date date = new Date();
			LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			if(customDate != null) {
				localDate = customDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			}
			LocalDate from = LocalDate.of(localDate.getYear(), localDate.getMonthValue() - 1, 1);
			YearMonth month = YearMonth.from(from);
			LocalDate to = month.atEndOfMonth();
			TrackingService trackingService = TrackingServiceFactory.getTrackingService();
			MediacenterService mediacenterService = MediacenterServiceFactory.getLocalService();
			String baseFolder = new UserEnvironmentTool().getEdu_SharingMediacenterFolder();
			for (String mediacenter : mediacenters == null ? SearchServiceFactory.getLocalService().getAllMediacenters() : mediacenters) {
				if(isInterrupted()) {
					return null;
				}
				logger.info("Building stats for mediacenter " + mediacenter);
				Date startDate = Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC));
				Date endDate = Date.from(to.atTime(23, 59).toInstant(ZoneOffset.UTC));
				Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> data = null;
				if(mode.equals(ReportMode.AlfrescoPermissionData)) {
					List<NodeRef> nodes = mediacenterService.getAllLicensedNodes(mediacenter);
					data = trackingService.getListNodeData(
							nodes.stream().map(
									ref -> new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, ref.getNodeId())
							).collect(Collectors.toList()),
							startDate,
							endDate,
							additionalFields,
							mediacenter
					);
				} else if (mode.equals(ReportMode.TrackingMediacenterData)) {
					data = trackingService.getListNodeDataByMediacenter(
							mediacenter,
							startDate,
							endDate,
							additionalFields
					);
					List<NodeRef> nodes = mediacenterService.getAllLicensedNodes(mediacenter);
					for (NodeRef n : nodes) {
						org.alfresco.service.cmr.repository.NodeRef mappedRef = new org.alfresco.service.cmr.repository.NodeRef(new StoreRef(n.getStoreProtocol(), n.getStoreId()), n.getNodeId());
						if (data.containsKey(mappedRef)) {
							continue;
						}
						data.put(mappedRef, new StatisticEntry());
					}
					data = filterNonMediacenterMedia(data);
				}
				String filename = from.format(DateTimeFormatter.ISO_DATE) + " - " + to.format(DateTimeFormatter.ISO_DATE) + ".csv";
				String parent = new NodeTool().createOrGetNodeByName(new MCAlfrescoAPIClient(), baseFolder, new String[]{mediacenter});
				PermissionServiceFactory.getLocalService().setPermission(parent, mediacenterService.getMediacenterAdminGroup(mediacenter), CCConstants.PERMISSION_CONSUMER);
				if(delete) {
					String node = NodeServiceFactory.getLocalService().findNodeByName(parent, filename);
					if(node != null) {
						NodeServiceFactory.getLocalService().removeNode(node, null, false);
					}
				}
				String nodeId = NodeServiceFactory.getLocalService().createNode(parent, CCConstants.CCM_TYPE_IO, NodeServiceFactory.getLocalService().getNameProperty(filename));
				NodeServiceHelper.addAspect(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), CCConstants.CCM_MEDIACENTER_STATISTICS);
				NodeServiceHelper.setProperty(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), CCConstants.CCM_PROP_MEDIACENTER_ID, mediacenter, false);
				try {
					writeCSVFile(data, nodeId);
				} catch(Throwable t) {
					logger.warn("Error writing csv data for mediacenter " + mediacenter, t);
					NodeServiceHelper.removeNode(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), false);
				}
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
		return null;
	}

	private Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> filterNonMediacenterMedia(Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> data) {
		return data.entrySet().stream().filter(
				e -> "restricted_mz".equals(NodeServiceHelper.getPropertyNative(e.getKey(), CCConstants.CCM_PROP_IO_EDITORIAL_STATE))
		).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private void writeCSVFile(Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> data, String nodeId) throws Exception {
		ContentWriter contentWriter = contentService.getWriter(
				new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),
				ContentModel.PROP_CONTENT,
				true);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(bos);
		CSVWriter writer = new CSVWriter(osw);
		List<String> header = columns.stream().map(c ->
				I18nAngular.getTranslationAngular("common", (
						c.size() == 1 ? "NODE." + c.get(0) : "VCARD." + c.get(1))
				)
		).collect(Collectors.toList());
		header.add(I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.HEADERS.count"));
		header.addAll(STAT_FIELDS.stream().map(e ->  I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.ACTIONS." + e)).collect(Collectors.toList()));
		Map<String, Set<String>> additionalFieldValues = new HashMap<>();
		additionalFields.forEach(f -> {
			additionalFieldValues.put(f,
					data.values().stream().map(
							d -> STAT_FIELDS.stream().map(
									event -> d.getGroups().getOrDefault(event, Collections.emptyMap()).getOrDefault(f, Collections.emptyMap()).keySet()
							).collect(Collectors.toSet())
					).flatMap(Set::stream).flatMap(Set::stream).collect(Collectors.toSet())
			);
			additionalFieldValues.forEach((key, value) -> header.addAll(
					value.stream().map(
							// concat field name with possibile field value
							// i.e. role: teacher
							subField -> I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.HEADERS." + key) + ": " +
									(StringUtils.isBlank(subField) ?
											I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.UNKNOWN_VALUE") :
											I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.CUSTOM." + key + "." + subField)
									)
					).map(
							subField -> I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.HEADERS.count") + " (" + subField + ")"
					).collect(Collectors.toList())
			));
		});
		writer.writeNext(header.toArray(new String[0]));
		ArrayList<ReportEntry> entries = new ArrayList<ReportEntry>();
		for (Map.Entry<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> entry : data.entrySet()) {
			if(isInterrupted()) {
				return;
			}
			List<String> csvEntry = columns.stream().map(
					e -> {
						try {
							String prop = NodeServiceHelper.getProperty(
									new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, entry.getKey().getId()),
									CCConstants.getValidGlobalName(e.get(0))
							);
							if (VCardConverter.isVCardProp(CCConstants.getValidGlobalName(e.get(0)))) {
								if(e.size() == 1) {
									prop = VCardConverter.getNameForVCardString(prop);
								} else {
									ArrayList<HashMap<String, Object>> vcard = VCardConverter.vcardToHashMap(null, prop);
									if (vcard.size() == 0) {
										return "";
									}
									return (String) vcard.get(0).getOrDefault(e.get(1), "");
								}
							}
							if (StringUtils.isEmpty(prop)) {
								return "";
							}
							return prop;
						}catch(Throwable t) {
							logger.debug(t.getMessage(), t);
							return entry.getKey().getId();
						}
					}
			).collect(Collectors.toList());
			// add total sum count
			int totalSum =
					STAT_FIELDS.stream().map(
							event -> entry.getValue().getCounts().getOrDefault(event, 0)
					).reduce(Integer::sum).orElse(0);
			csvEntry.add(String.valueOf(totalSum));
			// add counts per stat field
			csvEntry.addAll(STAT_FIELDS.stream().map(
					event -> entry.getValue().getCounts().getOrDefault(event, 0)
			).map(String::valueOf).collect(Collectors.toList()));
			// add additional, custom mapped field accounts (i.e. for role)
			additionalFieldValues.forEach((key, value) -> value.forEach(
					field -> csvEntry.add(
							String.valueOf(
									STAT_FIELDS.stream().map(
											event -> entry.getValue().getGroups().getOrDefault(event, Collections.emptyMap())
													.getOrDefault(key, Collections.emptyMap()).getOrDefault(field, 0L)
									).reduce(Long::sum).orElse(0L))
					)
			));
			entries.add(new ReportEntry(csvEntry,  totalSum));
		}
		Collections.sort(entries);
		writer.writeAll(entries.stream().map(ReportEntry::getEntry).map(l -> l.toArray(new String[0])).collect(Collectors.toList()));
		osw.close();
		new MCAlfrescoAPIClient().writeContent(nodeId, bos.toByteArray(), "text/csv", String.valueOf(StandardCharsets.UTF_8), CCConstants.CM_PROP_CONTENT);
	}

	@Getter
	@Setter
	@AllArgsConstructor
	private class ReportEntry implements Comparable<ReportEntry>{
		private List<String> entry;
		private int totalCount;


		@Override
		public int compareTo(@NotNull ReportEntry reportEntry) {
			return Long.compare(reportEntry.totalCount, this.totalCount);
		}
	}
}
