package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Admin implements Serializable {
    @XmlElement public Statistics statistics;
    @JsonProperty
    @JsonPropertyDescription("editor to use by the ui when editing config files")
    @XmlElement
    public EditorType editorType;

    public enum EditorType {
        Textarea,
        Monaco
    }

}
