package org.edu_sharing.restservices.stream.v1.model;

import java.util.List;
import java.util.Map;

import lombok.Data;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Person;
import org.edu_sharing.restservices.shared.UserSimple;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class StreamEntry {
	private String id;
	private String description;
	private List<Node> nodes;
	private Map<String,Object> properties;
	private int priority;
	private UserSimple author;
	private long created;
	private long modified;
}
