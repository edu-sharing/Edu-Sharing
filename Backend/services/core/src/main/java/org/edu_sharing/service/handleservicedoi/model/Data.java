package org.edu_sharing.service.handleservicedoi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Data {

    private String id;
    private String type;
    private Attributes attributes;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attributes {
        private String doi;
        private String prefix;
        private String suffix;
        private List<Object> identifiers;
        private List<Object> alternateIdentifiers;
        private List<Creator> creators;
        private List<Title> titles;
        private String publisher;
        private Container container;
        private int publicationYear;
        private List<Object> subjects;
        private List<Contributor> contributors;
        private List<Date> dates;
        private String language;
        private Types types;
        private List<Object> relatedIdentifiers;
        private List<Object> relatedItems;
        private List<Object> sizes;
        private List<Object> formats;
        private String version;
        private List<Object> rightsList;
        private List<Object> descriptions;
        private List<Object> geoLocations;
        private List<Object> fundingReferences;
        private String xml;
        private String url;
        private String state;
        private boolean isActive;
        private String event;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Creator {
        private String name;
        private List<Object> affiliation;
        private List<Object> nameIdentifiers;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Title {
        private String title;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @NoArgsConstructor
    @Builder
    public static class Container {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Contributor {
        private String name;
        private String givenName;
        private String familyName;
        private List<String> affiliation;
        private String contributorType;
        private List<NameIdentifier> nameIdentifiers;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NameIdentifier {
        private String schemeUri;
        private String nameIdentifier;
        private String nameIdentifierScheme;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Date {
        private String date;
        private String dateType;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Types {
        private String ris;
        private String bibtex;
        private String citeproc;
        private String schemaOrg;
        private String resourceType;
        private String resourceTypeGeneral;
    }
}
