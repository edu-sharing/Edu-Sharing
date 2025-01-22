package org.edu_sharing.restservices.comment.v1.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Comments {
	private List<Comment> comments;
}
