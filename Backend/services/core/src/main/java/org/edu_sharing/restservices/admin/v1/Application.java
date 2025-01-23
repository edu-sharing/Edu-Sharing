package org.edu_sharing.restservices.admin.v1;

import lombok.Data;

@Data
public class Application {
	private String id;
	private String title;
	private String webserverUrl;
	private String clientBaseUrl;
	private String type;
	private String subtype;
	private String repositoryType;
	private String xml;
	private String file;
	private String contentUrl;
	private String configUrl;
}
