package org.edu_sharing.repository.server.jobs.quartz;

import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.xml.XmlWriter;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.oai.core.EduMetadataFormatRegistry;
import org.edu_sharing.service.oai.core.EduSharingItemRepository;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileOutputStream;
import java.util.Map;

@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class ExporterJob extends AbstractJobMapAnnotationParams {

    public static final String PARAM_LUCENE_FILTER = "luceneFilter";

    public static final String PARAM_OUTPUT_DIR = "outputDirectory";
    public static final String PARAM_FORMAT = "format";


    @JobFieldDescription(description = "Export format", sampleValue = "lom")
    public String format;

    @JobFieldDescription
    public String luceneFilter;

    @JobFieldDescription
    public String outputDirectory;


    @Autowired
    private EduMetadataFormatRegistry eduMetadataFormatRegistry;

    @Autowired
    private EduSharingItemRepository eduSharingItemRepository;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (luceneFilter == null || outputDirectory == null) {
            return;
        }

        try {
            MetadataFormat formatWriter = eduMetadataFormatRegistry.getMetadataFormat(format);

            ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();

            Map<String, String> authInfo = new AuthenticationToolAPI().createNewSession(appInfo.getUsername(), appInfo.getPassword());
            MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient(authInfo);
            String[] nodeIds = apiClient.searchNodeIds(luceneFilter);

            if (nodeIds != null) {
                log.info("found {} to export with " + PARAM_LUCENE_FILTER + ": {}", nodeIds.length, luceneFilter);


                for (String nodeId : nodeIds) {
                    try(FileOutputStream os = new FileOutputStream(outputDirectory + "/" + nodeId + ".xml")) {
                        Item item = eduSharingItemRepository.getItem(nodeId, formatWriter);
                        try (XmlWriter writer = new XmlWriter(os)) {
                            item.getMetadata().write(writer);
                        }
                    }
                }

            } else {
                log.info("found nothing with " + PARAM_LUCENE_FILTER + ": {}", luceneFilter);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }

    }
}
