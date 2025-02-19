package org.edu_sharing.restservices.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
public class SignedNode {
	private String node;
	private String signature;
}
