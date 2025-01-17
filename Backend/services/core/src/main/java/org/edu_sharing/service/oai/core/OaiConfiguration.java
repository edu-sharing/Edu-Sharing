package org.edu_sharing.service.oai.core;

import io.gdcc.xoai.dataprovider.DataProvider;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.dataprovider.repository.ItemRepository;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.dataprovider.repository.SetRepository;
import io.gdcc.xoai.model.oaipmh.DeletedRecord;
import io.gdcc.xoai.model.oaipmh.Granularity;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.repository.server.oai.OaiIdentifier;
import org.edu_sharing.repository.server.oai.OaiSettings;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.xml.transform.TransformerConfigurationException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration class for setting up the OAI-PMH (Open Archives Initiative Protocol for Metadata Harvesting)
 * provider in the EduSharing system. This class is responsible for configuring the repository settings,
 * item repository, and set repository for OAI metadata harvesting.
 *
 * The OAI-PMH configuration allows EduSharing to provide metadata records to external harvesters in accordance
 * with the OAI-PMH standard. This class sets up essential components for OAI-PMH operations, including the
 * repository configuration, metadata format support, and the handling of sets and items.
 *
 * Key functionalities include:
 * - Configuring the OAI-PMH repository settings, such as granularity, identifier prefix, and repository metadata.
 * - Defining the item repository used to fetch metadata records.
 * - Defining the set repository, which manages the sets of items that can be harvested in OAI-PMH requests.
 * - Providing a data provider that integrates the repository, set repository, and item repository, enabling
 *   the OAI-PMH data harvesting functionality.
 *
 * This configuration class ensures that the EduSharing OAI provider is correctly set up, with support for metadata
 * formats, sets, and item retrieval in line with OAI-PMH standards.
 */
@Lazy
@Configuration
@RequiredArgsConstructor
public class OaiConfiguration {


    @Lazy
    @Bean
    @RefreshScope
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    RepositoryConfiguration repositoryConfiguration(OaiSettings settings) {
        OaiIdentifier identifier = settings.getIdentify();
        int itemsPerPage = settings.getItemsPerPage();
        return new RepositoryConfiguration.RepositoryConfigurationBuilder()
                .withMaxListIdentifiers(itemsPerPage).withMaxListSets(itemsPerPage)
                .withMaxListRecords(itemsPerPage).withMaxListSets(itemsPerPage)
                .withAdminEmail(identifier.getAdminEmail())
                .withBaseUrl(URLTool.getBaseUrl() + "/eduservlet/oai/provider")
                .withGranularity(Granularity.valueOf(identifier.getGranularity()))
                .withDeleteMethod(DeletedRecord.fromValue(identifier.getDelete()))
                .withEarliestDate(ISODateTimeFormat.dateTimeNoMillis().parseDateTime(identifier.getEarliestDate()).toDate().toInstant())
                .withRepositoryName(identifier.getName() != null ? identifier.getName() : ApplicationInfoList.getHomeRepository().getAppCaption())
                .withDescription(identifier.getDescription())
                .build();
    }

    @Lazy
    @Bean
    @RefreshScope
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    SetRepository setRepository(OaiSettings settings) throws TransformerConfigurationException {
        return new SetRepository() {
            @Override
            public boolean supportSets() {
                return true;
            }

            @Override
            public List<Set> getSets() {
                return settings.getSets().stream().map(Set::new).collect(Collectors.toList());
            }

            @Override
            public boolean exists(String setSpec) {
                return getSets().stream().anyMatch((set) -> set.getSpec().equals(setSpec));
            }
        };
    }

    @Lazy
    @Bean
    @RefreshScope
    DataProvider oaiDataProvider(RepositoryConfiguration configuration, SetRepository setRepository, ItemRepository itemRepository, EduMetadataFormatRegistry eduMetadataFormatRegistry) {
        Context context = Context.context();
        eduMetadataFormatRegistry.getSupportedFormats().forEach(context::withMetadataFormat);
        return new DataProvider(context, new Repository(configuration)
                .withSetRepository(setRepository)
                .withItemRepository(itemRepository));
    }
}
