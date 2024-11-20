package org.edu_sharing.alfresco.service.connector;

import com.typesafe.config.Optional;

import java.io.Serializable;

public class ConnectorFileType implements Serializable {

	@Optional private String ccressourceversion;

	@Optional private String ccressourcetype;

	@Optional private String ccresourcesubtype;

	@Optional private String editorType;


	@Optional private String mimetype;

	@Optional private String filetype;

	@Optional private boolean createable=true;

	@Optional private boolean editable=true;

	public String getCcressourceversion() {
		return ccressourceversion;
	}

	public void setCcressourceversion(String ccressourceversion) {
		this.ccressourceversion = ccressourceversion;
	}

	public String getCcressourcetype() {
		return ccressourcetype;
	}

	public void setCcressourcetype(String ccressourcetype) {
		this.ccressourcetype = ccressourcetype;
	}

	public String getCcresourcesubtype() {
		return ccresourcesubtype;
	}

	public void setCcresourcesubtype(String ccresourcesubtype) {
		this.ccresourcesubtype = ccresourcesubtype;
	}

	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	public String getFiletype() {
		return filetype;
	}

	public void setFiletype(String filetype) {
		this.filetype = filetype;
	}

	public boolean isCreateable() {
		return createable;
	}
	
	public void setCreateable(boolean createable) {
		this.createable = createable;
	}

	public boolean isEditable() {
		return editable;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public String getEditorType() {
		return editorType;
	}

	public void setEditorType(String editorType) {
		this.editorType = editorType;
	}
	
	
	
	
}
