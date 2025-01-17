package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class LogoutInfo implements Serializable {
	/**
	 * url to navigate to
	 */
	@XmlElement public String url;
	/**
	 * url for locale users
	 * if not set, url will be used
	 */
	@XmlElement public String localUrl;
	/**
	 * url for shibboleth or sso users
	 * if not set, url will be used
	 */
	@XmlElement public String ssoUrl;
	/**
	 * destroy the local session?
	 */
	@XmlElement public Boolean destroySession;
	/**
	 * call the given url via ajax (true) or navigate via browser (false)
	 */
	@XmlElement public Boolean ajax;
	/**
	 * only if ajax: url to navigate the browser to after triggering "url" via ajax.
	 */
	@XmlElement	public String next;
}
