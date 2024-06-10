package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.List;

public class ConfigThemeColors implements Serializable {
    @JsonPropertyDescription("Colors for the global branding")
    public @XmlElement List<ConfigThemeColor> color;
    @JsonPropertyDescription("Colors for the global branding when in safe scope")
    public @XmlElement List<ConfigThemeColor> colorSafe;
}
