package org.edu_sharing.restservices.node.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.UserSimple;

@Data
public class WorkflowHistory {
	private long time;
	private UserSimple editor;
	private Authority[] receiver;
	private String status;
	private String comment;
}
