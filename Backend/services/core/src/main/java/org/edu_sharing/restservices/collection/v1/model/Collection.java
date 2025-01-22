package org.edu_sharing.restservices.collection.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

@Data
public class Collection implements Serializable {

	@JsonProperty(required = true)
	boolean level0;

	@JsonProperty(required = true)
	String title;

	String description;

	@JsonProperty(required = true)
	String type;

	@JsonProperty(required = true)
	String viewtype;

	String orderMode;
	
	int x;
	int y;
	int z;
	
	String color;
	User owner;

	@JsonProperty(required = true)
	boolean fromUser;

	boolean pinned;
	int childCollectionsCount;
	int childReferencesCount;


	private String scope;
	private String authorFreetext;
	private Boolean orderAscending;
}
