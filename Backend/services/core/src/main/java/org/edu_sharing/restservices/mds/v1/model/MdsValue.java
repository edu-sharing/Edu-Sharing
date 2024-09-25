package org.edu_sharing.restservices.mds.v1.model;

import lombok.Getter;
import lombok.Setter;
import org.edu_sharing.metadataset.v2.MetadataKey;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "")
public class MdsValue {
	private String id,caption,description,parent,url;
	private List<String> alternativeIds;
	@Getter
	@Setter
	@JsonProperty
	private String abbreviation;
	public MdsValue(){};
	public MdsValue(MetadataKey key) {
		id=key.getKey();
		caption=key.getCaption();
		alternativeIds = key.getAlternativeKeys();
		abbreviation = key.getAbbreviation();
		url = key.getUrl();
		description=key.getDescription();
		parent=key.getParent();
	}

	@JsonProperty(required = false)
	public List<String> getAlternativeIds() {
		return alternativeIds;
	}

	public void setAlternativeIds(List<String> alternativeIds) {
		this.alternativeIds = alternativeIds;
	}

	@JsonProperty(required = false)
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@JsonProperty
	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	@JsonProperty(required = true)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	@JsonProperty
	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}
	
	@JsonProperty
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
}