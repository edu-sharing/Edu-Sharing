package org.edu_sharing.restservices.shared;

import java.util.List;

import lombok.Data;
import org.edu_sharing.metadataset.v2.MetadataCreate;
import org.edu_sharing.restservices.mds.v1.model.*;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class Mds {

	@JsonProperty(required = true)
	private String name = null;

	private Create create = null;

	@JsonProperty(required = true)
	private List<MdsWidget> widgets = null;

	@JsonProperty(required = true)
	private List<MdsView> views;

	@JsonProperty(required = true)
	private List<MdsGroup> groups;

	@JsonProperty(required = true)
	private List<MdsList> lists;

	@JsonProperty(required = true)
	private List<MdsSort> sorts;

	@Data
	public static class Create {
		private boolean onlyMetadata;

		public Create(MetadataCreate create) {
			this.onlyMetadata=create.isOnlyMetadata();
		}
	}


}
