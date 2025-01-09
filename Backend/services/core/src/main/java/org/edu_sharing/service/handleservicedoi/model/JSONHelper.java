package org.edu_sharing.service.handleservicedoi.model;

import java.util.ArrayList;

public class JSONHelper implements Helper {

    DOI doi;

    public JSONHelper(){
        doi = DOI.builder()
                .data(Data.builder()
                        .type("dois")
                        .attributes(Data.Attributes.builder()
                                .creators(new ArrayList<>())
                                .titles(new ArrayList<>())
                                .event("publish")
                                .build())
                        .build())
                .build();
    }

    @Override
    public void addCreator(String value, String affiliation) {
        doi.getData().getAttributes().getCreators()
                .add(Data.Creator.builder().name(value).build());
    }

    @Override
    public void addTitle(String value) {
        doi.getData().getAttributes().getTitles().add(Data.Title.builder().title(value).build());
    }

    @Override
    public void setPublisher(String value) {
        doi.getData().getAttributes().setPublisher(value);
    }

    @Override
    public void setResourceType(String value) {
        doi.getData().getAttributes().setTypes(Data.Types.builder().resourceTypeGeneral(value).build());
    }

    @Override
    public void setPublicationYear(int value) {
        doi.getData().getAttributes().setPublicationYear(value);
    }

    @Override
    public DOI getDoi() {
        return doi;
    }
}
