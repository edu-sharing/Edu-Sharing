package org.edu_sharing.service.oai.core;

import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.model.oaipmh.results.record.About;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an item in the OAI (Open Archives Initiative) metadata repository.
 * This class implements the {@link Item} interface from the XOAI library, providing
 * metadata, identifier, and other required information for an item in the repository.
 *
 * The class encapsulates an item's identifier and metadata, offering methods to
 * retrieve this data in accordance with the OAI-PMH (Open Archives Initiative Protocol for Metadata Harvesting).
 * It is a simplified implementation designed to provide basic functionality such as returning the metadata,
 * sets, and datestamp associated with the item.
 */
public class EduItem implements Item {
    private final String identifier;
    private Metadata metadata;

    public EduItem(String identifier,String metadata) {
        this.identifier = identifier;
        this.metadata=new Metadata(metadata);
    }

    @Override
    public List<About> getAbout() {
        return List.of();
    }

    @Override
    public Metadata getMetadata() {
        return this.metadata;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public Instant getDatestamp() {
        return Instant.now();
    }

    @Override
    public List<Set> getSets() {
        return new ArrayList<>();
    }

    @Override
    public boolean isDeleted() {
        return false;
    }
}
