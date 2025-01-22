package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;;

@Data
@EqualsAndHashCode(callSuper = true)
public class ManagableGroup extends Group {

	private boolean administrationAccess;

}
