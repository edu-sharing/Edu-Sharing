package org.edu_sharing.restservices.shared;

import lombok.Data;

@Data
public class UserStats  {
	private int nodeCount = 0;
	private int nodeCountCC = 0;
	private int collectionCount = 0;
}
