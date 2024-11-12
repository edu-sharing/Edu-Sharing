package org.edu_sharing.service.oai.core;

import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.Set;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
