package org.edu_sharing.alfresco.service.connector;

import com.typesafe.config.Optional;

import java.io.Serializable;
import java.util.List;

public class Connector implements Serializable {
	
	private String id;

	private String icon;

	@Optional private String connectorId;

	@Optional private boolean showNew=true;

	@Optional private boolean onlyDesktop=false;

	@Optional private boolean hasViewMode=false;

	@Optional private String url;
	
	/**
	 * optional, create element if an empty node is created
	 * Currently only used for h5p connector
	 */
	@Optional private String defaultCreateElement;

	@Optional private List<String> parameters;
	
	private List<ConnectorFileType> filetypes;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getConnectorId() {
		return connectorId;
	}

	public void setConnectorId(String connectorId) {
		this.connectorId = connectorId;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public boolean isShowNew() {
		return showNew;
	}

	public void setShowNew(boolean showNew) {
		this.showNew = showNew;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public void setParameters(List<String> parameters) {
		this.parameters = parameters;
	}

	public List<ConnectorFileType> getFiletypes() {
		return filetypes;
	}

	public void setFiletypes(List<ConnectorFileType> filetypes) {
		this.filetypes = filetypes;
	}

	public String getDefaultCreateElement() {
		return defaultCreateElement;
	}

	public void setDefaultCreateElement(String defaultCreateElement) {
		this.defaultCreateElement = defaultCreateElement;
	}

	public boolean isOnlyDesktop() {
		return onlyDesktop;
	}

	public void setOnlyDesktop(boolean onlyDesktop) {
		this.onlyDesktop = onlyDesktop;
	}

	public boolean isHasViewMode() {
		return hasViewMode;
	}

	public void setHasViewMode(boolean hasViewMode) {
		this.hasViewMode = hasViewMode;
	}
}
