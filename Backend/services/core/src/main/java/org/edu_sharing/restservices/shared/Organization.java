package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Organization extends ManagableGroup {
	private NodeRef sharedFolder;
}
