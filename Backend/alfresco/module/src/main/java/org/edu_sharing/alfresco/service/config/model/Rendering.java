package org.edu_sharing.alfresco.service.config.model;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Rendering implements Serializable {
	@XmlElement public Boolean showPreview;
	@XmlElement public Boolean showDownloadButton;
	@XmlElement public Boolean prerender;
	@XmlElement public RenderingGdpr[] gdpr;

	public static class RenderingGdpr implements Serializable {
		@XmlElement public String matcher;
		@XmlElement public String name;
		@XmlElement public String privacyInformationUrl;
	}
}
