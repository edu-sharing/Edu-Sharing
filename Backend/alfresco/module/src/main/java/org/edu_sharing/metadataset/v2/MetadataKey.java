package org.edu_sharing.metadataset.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataKey extends MetadataTranslatable {

    @Getter
    public static class MetadataKeyRelated extends MetadataKey {
        public enum Relation {
            exactMatch,
            narrowMatch,
            relatedMatch,
            closeMatch,
            broadMatch,
        }

        private final Relation relation;

        public MetadataKeyRelated(Relation relation) {
            this.relation = relation;
        }

    }
    @JacksonXmlText
    private String key;
    @JacksonXmlProperty(isAttribute = true, localName = "cap")
    private String caption;
    private String icon;
    @JacksonXmlProperty(isAttribute = true)
    private String description;
    @JacksonXmlProperty(isAttribute = true)
    private String parent;
    @JacksonXmlProperty(isAttribute = true)
    private String locale;
    /**
     * List of other keys this child is a precedor of
     */
    private List<String> preceds;
    private final List<MetadataKeyRelated> related = new ArrayList<>();
    private List<String> alternativeKeys;
    private String url;

    public void setParent(String parent) {
        if (StringUtils.isBlank(parent)) {
            this.parent = null;
        } else {
            this.parent = parent;
        }
    }

    public void addRelated(MetadataKeyRelated related) {
        this.related.add(related);
    }
}
