package org.edu_sharing.restservices.stream.v1.model;

import java.util.List;
import java.util.Map;

import lombok.Data;
import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class StreamEntryInput {
	private String id;
	private String title;
	private String description;
	private List<String> nodes;
	private Map<String,Object> properties;
	private int priority;
	
}
