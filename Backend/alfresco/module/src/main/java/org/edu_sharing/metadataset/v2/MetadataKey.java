package org.edu_sharing.metadataset.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataKey extends MetadataTranslatable {

    @Getter
    @Setter
    public static class MetadataKeyRelated extends MetadataKey {
        public enum Relation {
            exactMatch,
            narrowMatch,
            relatedMatch,
            closeMatch,
            broadMatch,
        }

        private Relation relation;
        /**
         * optional target id of the valuespace/widget id it is targeting
         */
        private String target;

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
    private String abbreviation;
    @JacksonXmlProperty(isAttribute = true)
    private String parent;

    // Hint if the given key shall be removed. Use in conjunction with @MetadataWidget.ValuespaceMerge
    private Boolean delete = false;
    @JacksonXmlProperty(isAttribute = true)
    private String locale;
    /**
     * List of other keys this child is a precedor of
     */
    private List<String> preceds;
    private final List<MetadataKeyRelated> related = new ArrayList<>();
    private List<String> alternativeKeys;
    private String url;
    private Source source = Source.Internal;

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


    /**
     * @return an uri or literal value that identifies the subject.
     * This is mainly used for oai dublin core export and its customizations,
     * to provide an identifiable resource, in case of a skos values or other internal value spaces.
     * </br>
     * </br>
     * e.g. for OaiDublinCoreMetadataFormatWriter customizations:
     *
     * <pre>
     *     {@code
     *     MetadataWidget generalKeyWordWidget = context.getMetadataSet().findWidget(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_KEYWORD));
     *     context.createAndAppendElement("dc:subject", root, propertyMapper.getStringList(CCConstants.LOM_PROP_GENERAL_KEYWORD)
     *         .stream()
     *         .map(x-> generalKeyWordWidget.getValuesAsMap().get(x))
     *         .filter(Objects::nonNull)
     *         .map(MetadataKey::getIdentifiableValue)
     *         .collect(Collectors.toList()));
     *     }
     * </pre>
     */
    public String getIdentifiableValue() {
        switch (source) {
            case Internal:
                return getCaption();
            case SKOS:
                return getKey();
            default:
                throw new NotImplementedException(source.name());
        }
    }


}
