package org.edu_sharing.restservices.mds.v1.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.edu_sharing.metadataset.v2.MetadataKey;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Data
public class MdsValue {
	@JsonProperty(required = true)
	private String id;
	private String caption;
	private String description;
	private String parent;
	private String url;
	private List<String> alternativeIds;
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
}
