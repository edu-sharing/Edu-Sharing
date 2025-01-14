package org.edu_sharing.service.oai.core;

import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.Set;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an item identifier in the OAI (Open Archives Initiative) metadata repository.
 * This class implements the {@link ItemIdentifier} interface from the XOAI library and is used
 * to encapsulate the identifier and datestamp (modified date) of an item within an OAI-PMH
 * (Open Archives Initiative Protocol for Metadata Harvesting) context.
 *
 * The class provides a way to manage the unique identifier for an item, its modified date (datestamp),
 * and other metadata-related information required for OAI-PMH operations. It also includes a list of sets
 * (although this implementation returns an empty list) and an indication of whether the item is deleted.
 *
 * This class is typically used for identifying and tracking items in repositories that support OAI-PMH
 * harvesting protocols.
 */
public class EduItemIdentifier implements ItemIdentifier {
    private final String identifier;
    private final Instant modifiedDate;

    public EduItemIdentifier(String identifier, Instant modifiedDate) {
        this.identifier=identifier;
        this.modifiedDate=modifiedDate;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Instant getDatestamp() {
        return modifiedDate;
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
