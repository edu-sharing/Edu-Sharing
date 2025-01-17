package org.edu_sharing.service.handleservicedoi.model;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.edu_sharing.service.handleservicedoi.model.xml.Affiliation;
import org.edu_sharing.service.handleservicedoi.model.xml.ObjectFactory;
import org.edu_sharing.service.handleservicedoi.model.xml.Resource;

import java.io.StringWriter;

/**
 * https://support.datacite.org/docs/api-create-dois#create-findable-doi-using-datacite-xml
 */
public class XMLHelper implements Helper {

    ObjectFactory objectFactory = new ObjectFactory();

    Resource resource;

    DOI doi;

    public XMLHelper(){
        doi = DOI.builder()
                .data(Data.builder()
                        .type("dois")
                        .attributes(Data.Attributes.builder()
                                .event("publish")
                                .build())
                        .build())
                .build();

        resource = getInitialized();
        doi.setXmlRepresentation(resource);
    }


    private Resource getInitialized(){
        Resource resource = objectFactory.createResource();
        resource.setCreators(objectFactory.createResourceCreators());
        resource.setTitles(objectFactory.createResourceTitles());
        resource.setIdentifier(objectFactory.createResourceIdentifier());
        resource.setPublisher(objectFactory.createResourcePublisher());
        resource.setResourceType(objectFactory.createResourceResourceType());
        return resource;
    }

    @Override
    public void addCreator(String value, String affiliation){
        Resource.Creators.Creator creator = objectFactory.createResourceCreatorsCreator();
        Resource.Creators.Creator.CreatorName creatorName = objectFactory.createResourceCreatorsCreatorCreatorName();
        creator.setCreatorName(creatorName);
        creatorName.setValue(value);
        if(affiliation != null){
            Affiliation affiliationObj = objectFactory.createAffiliation();
            affiliationObj.setValue(affiliation);
            creator.getAffiliation().add(affiliationObj);
        }
        resource.getCreators().getCreator().add(creator);
    }

    @Override
    public void addTitle(String value){
        Resource.Titles.Title title = objectFactory.createResourceTitlesTitle();
        title.setValue(value);
        resource.getTitles().getTitle().add(title);
    }

    public void setIdentifier(Resource resource, String value){
        Resource.Identifier resourceIdentifier = objectFactory.createResourceIdentifier();
        resourceIdentifier.setIdentifierType("DOI");
        resourceIdentifier.setValue(value);
        resource.setIdentifier(resourceIdentifier);
    }

    @Override
    public void setPublisher(String value){
        Resource.Publisher resourcePublisher = objectFactory.createResourcePublisher();
        resourcePublisher.setValue(value);
        resource.setPublisher(resourcePublisher);
    }

    @Override
    public void setResourceType(String value){
        Resource.ResourceType resourceResourceType = objectFactory.createResourceResourceType();
        resourceResourceType.setResourceTypeGeneral(org.edu_sharing.service.handleservicedoi.model.xml.ResourceType.fromValue(value));
        resource.setResourceType(resourceResourceType);
    }

    @Override
    public void setPublicationYear(int value) {
        resource.setPublicationYear(Integer.valueOf(value).toString());
    }

    @Override
    public DOI getDoi() {
        return doi;
    }

    public Resource getResource(){
        return resource;
    }

    public String marshal(Resource resource) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Resource.class);
        Marshaller marshaller = context.createMarshaller();
        // Optional: Pretty print the XML
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd");

        //remove standalone="yes"
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        //marshaller.setProperty("com.sun.xml.bind.xmlHeaders","<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        // Write the XML to a StringWriter
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(resource, stringWriter);
        return stringWriter.toString();
    }
}
