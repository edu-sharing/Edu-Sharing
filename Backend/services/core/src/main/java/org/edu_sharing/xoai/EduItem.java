package org.edu_sharing.xoai;

import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.model.oaipmh.results.record.About;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EduItem implements Item {
    private final String identifier;
    private Metadata metadata;

    public EduItem(String identifier,String lomMetadata) {
        this.identifier = identifier;
        this.metadata=new Metadata(lomMetadata);
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
