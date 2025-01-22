package org.edu_sharing.restservices.comment.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.UserSimple;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class Comment {
	private NodeRef ref;
	private NodeRef replyTo;
	private UserSimple creator;
	private long created;
	private String comment;
}
