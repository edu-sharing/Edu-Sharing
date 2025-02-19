package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigRating implements Serializable {
	enum RatingMode {
		none,
		likes,
		stars,
	}
	@XmlElement
	public RatingMode mode;
}
