package org.edu_sharing.restservices.connector.v1.model;

import lombok.Data;
import org.edu_sharing.repository.client.tools.CCConstants;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class ConnectorFileType {
	private String ccressourceversion;
	private String ccressourcetype;
	private String ccresourcesubtype;
	private String editorType;
	private String mimetype;
	private String filetype;
	private boolean creatable;
	private boolean editable;
}
