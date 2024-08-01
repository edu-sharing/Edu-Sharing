package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;

public class ConfigReportProblem implements Serializable {
	@XmlElement(name = "toolPermission")
	public String[] toolPermissions;
}
