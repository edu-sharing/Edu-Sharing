package org.edu_sharing.restservices.stream.v1.model;

import java.util.List;

import lombok.Data;
import org.edu_sharing.restservices.shared.Pagination;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class StreamList {
	private List<StreamEntry> stream;
	private Pagination pagination;
}
