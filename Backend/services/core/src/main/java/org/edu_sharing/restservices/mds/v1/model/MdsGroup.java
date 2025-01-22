package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.metadataset.v2.MetadataGroup;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class MdsGroup {
	private String id;
	private List<String> views;
	private MetadataGroup.Rendering rendering;

	public MdsGroup(){}
	public MdsGroup(MetadataGroup group) {
		this.id=group.getId();
		this.rendering=group.getRendering();
		this.views=group.getViews();
	}
}

