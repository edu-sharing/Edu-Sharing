package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigPublish implements Serializable {
    @XmlElement public Boolean licenseMandatory;
    @XmlElement public Boolean authorMandatory;
}
