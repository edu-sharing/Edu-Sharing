package org.edu_sharing.repository.server.jobs.quartz;

import com.opencsv.CSVWriter;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.NodeTool;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "Creates reports for all NodeObjects on the 1st of each month for the last month")
public class NodeObjectReportJob extends AbstractJobMapAnnotationParams {
    private static final List<TrackingService.EventType> STATISTIC_EVENTS = List.of(
            TrackingService.EventType.VIEW_MATERIAL,
            TrackingService.EventType.VIEW_MATERIAL_EMBEDDED,
            TrackingService.EventType.VIEW_MATERIAL_PLAY_MEDIA,
            TrackingService.EventType.OPEN_EXTERNAL_LINK,
            TrackingService.EventType.DOWNLOAD_MATERIAL
    );

    @Value
    private static class AddtionalFileds {
        String field;
        TrackingMode mode;
    }

    enum TrackingMode {
        latest,
        accumulated
    }

    //TODO default systemfolder/reports
    @JobFieldDescription(description = "Used to group reports under this folder name")
    private String reportName;

    @JobFieldDescription(description = "filename of the report.", sampleValue = "monthly node report")
    private String filename;

    @JobFieldDescription(description = "Will override existing reports", sampleValue = "false")
    private boolean overrideExisting;

    @JobFieldDescription(description = "The date of the report will be attached at the end of the filename", sampleValue = "true")
    private boolean appendDate;

    @JobFieldDescription(description = "List of additional (custom) fields to be fetched from the tracking data", sampleValue = "field1")
    private List<String> additionalFields = Collections.emptyList();

    @JobFieldDescription(description = "List of grouped (custom) fields to be fetched from the tracking data", sampleValue = "field1")
    private List<String> groupFields = Collections.emptyList();

    @JobFieldDescription(description = "use a custom date (month) to run the job for. Note: The job will run the month BEFORE the given date!", sampleValue = "YYYY-MM-DD")
    private Date customDate = null;

    @JobFieldDescription(description = "When set to true, the job will generate a yearly report")
    private boolean generateYearly = false;

    @JobFieldDescription(description = "List of properties to include in the export file. First value is the property, second value (only for vcard) is the vcard field id", sampleValue = "[\"cclom:title\"],[\"ccm:lifecyclecontributer_publisher\"],[\"ccm:lifecyclecontributer_publisher\", \"X-ES-LOM-CONTRIBUTE-DATE\"],[\"sys:node-uuid\"]")
    private List<List<String>> columns = List.of(
            Collections.singletonList("cclom:title"),
            Collections.singletonList("ccm:lifecyclecontributer_publisher"),
            Arrays.asList("ccm:lifecyclecontributer_publisher", "X-ES-LOM-CONTRIBUTE-DATE"),
            Collections.singletonList("sys:node-uuid")
    );

    @JobFieldDescription(description = "List of properties to filter by")
    private Map<String, String> filters;

    @JobFieldDescription(description = "List of aspects to filter by", sampleValue = "[]")
    private List<String> aspectFilters = Collections.emptyList();

    @Autowired
    private TrackingService trackingService;

    @Autowired
    @Qualifier("nodeService")
    private NodeService nodeService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LocalDate now = LocalDate.now();
        if (customDate == null && now.getDayOfMonth() != 1) {
            logger.error("Job not running because of date: " + now.getDayOfMonth());
            return;
        }

        AuthenticationUtil.runAsSystem(this::createStats);
    }

    private Void createStats() {
        LocalDate to = LocalDate.now();
        if (this.customDate != null) {
            to = customDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        to = to.minusDays(1);

        LocalDate from = generateYearly
                ? to.minusYears(1).minusMonths(1).plusDays(1)
                : to.minusMonths(1).plusDays(1);
        String nodeId = null;
        try {
            List<StatisticEntryNode> nodeStatisics = trackingService.getNodeStatisics(
                    TrackingService.GroupingType.Node,
                    Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    Date.from(to.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    "",
                    additionalFields,
                    groupFields,
                    filters);

            nodeId = generateCSVNode(from, to);
            writeCsvFile(nodeId, nodeStatisics);
        } catch (Throwable e) {
            try {
                if (nodeId != null) {
                    nodeService.removeNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
                }
            } catch (Exception ignored) {
            }
            throw new RuntimeException(e);
        }

        return null;
    }


    private String generateCSVNode(LocalDate from, LocalDate to) throws Throwable {
        String baseFolder = new UserEnvironmentTool().getEdu_SharingReportsFolder();
        String parent = NodeTool.createOrGetNodeByName(baseFolder, new String[]{reportName, String.valueOf(from.getYear())});
        String filename = generateFilename(from, to);

        if (overrideExisting) {
            String node = nodeService.findNodeByName(parent, filename);
            if (!StringUtils.isEmpty(node)) {
                nodeService.removeNode(node, null, false);
            }
        }

        return nodeService.createNode(parent, CCConstants.CCM_TYPE_IO, nodeService.getNameProperty(filename));
    }

    private String generateFilename(LocalDate from, LocalDate to) {
        StringBuilder stringBuilder = new StringBuilder(filename);
        if (appendDate) {
            String datePattern = from.getMonth() == to.getMonth() ? "yyyy-MM" : "yyyy";
            stringBuilder.append("_");
            stringBuilder.append(from.format(DateTimeFormatter.ofPattern(datePattern)));
        }
        stringBuilder.append(".csv");
        return stringBuilder.toString();
    }


    private void writeCsvFile(String nodeId, List<StatisticEntryNode> dataSet) throws Exception {
        List<String> headers = Stream.concat(
                        columns.stream().map(x -> x.size() == 1 ? "NODE." + x.get(0) : "VCARD." + x.get(1)).map(x -> I18nAngular.getTranslationAngular("common", x)),
                        STATISTIC_EVENTS.stream().map(x -> "ADMIN.STATISTICS.ACTIONS." + x).map(x -> I18nAngular.getTranslationAngular("admin", x)))
                .collect(Collectors.toList());

        Map<String, Set<String>> additionalFieldValues = extractAdditionalFields(dataSet);
        for (Map.Entry<String, Set<String>> entry : additionalFieldValues.entrySet()) {
            List<String> additionalHeaders = entry.getValue().stream()
                    .map(subField -> I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.HEADERS." + entry.getKey()) + ": "
                            + (org.apache.commons.lang.StringUtils.isBlank(subField)
                            ? I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.UNKNOWN_VALUE")
                            : I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.CUSTOM." + entry.getKey() + "." + subField)))
                    .map(subField -> I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.HEADERS.count") + " (" + subField + ")")
                    .collect(Collectors.toList());

            headers.addAll(additionalHeaders);
        }

        List<ReportEntry> csvData = new ArrayList<>();
        for (StatisticEntryNode entry : dataSet) {
            if (isInterrupted()) {
                return;
            }

            if(!aspectFilters.isEmpty()) {
                try {
                    String[] apsects = nodeService.getAspects(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), entry.getNode());
                    if (Arrays.stream(apsects).map(CCConstants::getValidLocalName).noneMatch(x -> aspectFilters.contains(x))) {
                        continue;
                    }
                }catch (InvalidNodeRefException e){
                    log.warn("Node {} does not exist", entry.getNode());
                    continue;
                }
            }

            List<String> csvRow = columns.stream()
                    .map(x -> getProperty(entry, x))
                    .collect(Collectors.toList());

            // add total sum count
            Map<TrackingService.EventType, Integer> eventCountMapping = entry.getCounts();
            int totalSum = STATISTIC_EVENTS.stream()
                    .map(event -> eventCountMapping.getOrDefault(event, 0))
                    .reduce(Integer::sum)
                    .orElse(0);

            // add eventCountMapping per stat field
            csvRow.addAll(STATISTIC_EVENTS.stream()
                    .map(event -> eventCountMapping.getOrDefault(event, 0))
                    .map(String::valueOf)
                    .collect(Collectors.toList()));

            // add additional, custom mapped field accounts (i.e. for role)
            for (Map.Entry<String, Set<String>> additionalFieldValue : additionalFieldValues.entrySet()) {
                String fieldName = additionalFieldValue.getKey();
                Set<String> values = additionalFieldValue.getValue();

                // group and aum up statistic events based on field type
                List<String> counts = values.stream()
                        .map(value -> STATISTIC_EVENTS.stream()
                                .map(eventType -> entry.getGroups().getOrDefault(eventType, Collections.emptyMap()))
                                .map(eventData -> eventData.getOrDefault(fieldName, Collections.emptyMap()))
                                .map(eventDataValues -> eventDataValues.getOrDefault(value, 0L))
                                .reduce(Long::sum)
                                .orElse(0L))
                        .map(String::valueOf)
                        .collect(Collectors.toList());

                csvRow.addAll(counts);
            }

            csvData.add(new ReportEntry(csvRow, totalSum));
        }
        Collections.sort(csvData);
        writeCsvFile(nodeId, headers, csvData.stream().map(ReportEntry::getEntry).map(x -> x.toArray(new String[0])).collect(Collectors.toList()));
    }

    private void writeCsvFile(String nodeId, List<String> headers, List<String[]> data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (OutputStreamWriter osw = new OutputStreamWriter(bos)) {
            CSVWriter writer = new CSVWriter(osw);
            writer.writeNext(headers.toArray(new String[0]));
            writer.writeAll(data);
        }

        new MCAlfrescoAPIClient().writeContent(nodeId, bos.toByteArray(), "text/csv", String.valueOf(StandardCharsets.UTF_8), CCConstants.CM_PROP_CONTENT);
    }


    /**
     * "groups": {
     * "VIEW_MATERIAL": {
     * "license": {
     * "": 20,
     * "PDM": 1,
     * "CC_0": 10
     * }
     * }
     * },
     * Extracts all additional fields (e.g., license) and groups them by their possible values.
     *
     * @return Group by Field and the resulting values
     */
    private Map<String, Set<String>> extractAdditionalFields(List<StatisticEntryNode> dataSet) {
        Map<String, Set<String>> additionalFieldGroup = new HashMap<>();
        for (String field : additionalFields) {
            Set<String> values = dataSet.stream()
                    .map(d -> STATISTIC_EVENTS.stream()
                            .map(event -> d.getGroups().getOrDefault(event, Collections.emptyMap()))
                            .map(x -> x.getOrDefault(field, Collections.emptyMap()))
                            .map(Map::keySet)
                            .collect(Collectors.toSet()))
                    .flatMap(Set::stream)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

            additionalFieldGroup.put(field, values);
        }

        return additionalFieldGroup;
    }

    private String getProperty(StatisticEntryNode entry, List<String> propsToFetch) {
        try {
            String propertyName = CCConstants.getValidGlobalName(propsToFetch.get(0));
            String prop = nodeService.getProperty(
                    StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
                    StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
                    entry.getNode(),
                    propertyName);

            if (VCardConverter.isVCardProp(propertyName)) {
                if (propsToFetch.size() == 1) {
                    prop = VCardConverter.getNameForVCardString(prop);
                } else {
                    return VCardConverter.vcardToMap(null, prop)
                            .stream()
                            .findFirst().map(x -> (String) x.get(propsToFetch.get(1)))
                            .orElse("");
                }
            }
            return StringUtils.isEmpty(prop) ? "" : prop;
        } catch (Throwable t) {
            log.debug(t.getMessage(), t);
            return entry.getNode();
        }
    }

    @Value
    @AllArgsConstructor
    private static class ReportEntry implements Comparable<ReportEntry> {
        List<String> entry;
        int totalCount;


        @Override
        public int compareTo(@NotNull ReportEntry reportEntry) {
            return Long.compare(reportEntry.totalCount, this.totalCount);
        }
    }
}
