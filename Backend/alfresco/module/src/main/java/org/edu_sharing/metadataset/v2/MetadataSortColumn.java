package org.edu_sharing.metadataset.v2;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class MetadataSortColumn implements Serializable {
	private String id, mode;
}
