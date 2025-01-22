package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.metadataset.v2.MetadataSetInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data

public class MdsEntries {
	@JsonProperty(required = true)
	private List<MetadataSetInfo> metadatasets;
}
