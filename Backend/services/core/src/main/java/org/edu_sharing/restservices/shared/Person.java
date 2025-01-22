package org.edu_sharing.restservices.shared;

import lombok.Data;

import java.io.Serializable;

@Data
public class Person implements Serializable {

	private String firstName = null;
	private String lastName = null;
	private String mailbox = null;
	private UserProfile profile = null;
}
