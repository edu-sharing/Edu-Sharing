package org.edu_sharing.xoai;

import io.gdcc.xoai.dataprovider.DataProvider;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.dataprovider.repository.ItemRepository;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.dataprovider.repository.SetRepository;
import io.gdcc.xoai.model.oaipmh.OAIPMH;
import io.gdcc.xoai.xml.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO should be a bean config that instantiate a OAIPMH bean
public class EduOai {

    private String metadataPrefix;
    private RepositoryConfiguration configuration;
    private ItemRepository itemRepository;
    private DataProvider provider;


    public EduOai(RepositoryConfiguration configuration, String metadataPrefix, EduDataHandler handler) throws TransformerConfigurationException {
        this.metadataPrefix = metadataPrefix;
        itemRepository = handler;
        SetRepository setRepository = new SetRepository() {
            @Override
            public boolean supportSets() {
                return true;
            }

            @Override
            public List<Set> getSets() {
                return handler.getSets().stream().map(Set::new).collect(Collectors.toList());
            }

            @Override
            public boolean exists(String setSpec) {
                return getSets().stream().anyMatch((set) -> set.getSpec().equals(setSpec));
            }
        };

        Repository repository = new Repository(configuration)
                .withSetRepository(setRepository)
                .withItemRepository(itemRepository);

        Context context = Context.context()
                .withMetadataFormat(metadataPrefix, MetadataFormat.identity());

        provider = new DataProvider(context, repository);
    }

    public static String responseToXML(OAIPMH response) throws XMLStreamException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlWriter writer = new XmlWriter(os);
        response.write(writer);
        writer.close();
        return os.toString();
    }

    public OAIPMH handleRequest(Map<String, String[]> requests) {
        return provider.handle(requests);
    }
}
