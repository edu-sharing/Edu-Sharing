package org.edu_sharing.service.handleservicedoi.model;

import org.edu_sharing.service.handleservicedoi.model.xml.Resource;

public interface Helper {

    public void addCreator(String value, String affiliation);

    public void addTitle(String value);

    public void setPublisher(String value);

    public void setResourceType(String value);

    public void setPublicationYear(int value);

    public DOI getDoi();
}
