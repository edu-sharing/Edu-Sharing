package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.xml.bind.annotation.XmlElement;

public class ConfigTutorial {
    @JsonPropertyDescription("should the tutorial be triggered at all (default true)")
    @XmlElement
    public boolean enabled;
}
