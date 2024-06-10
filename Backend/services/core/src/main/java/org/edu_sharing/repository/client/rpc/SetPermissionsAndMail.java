/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SetPermissionsAndMail implements Serializable {
	
	String nodeId = null;
	
	//<authorityname,permissions[]>
	Map<String,String[]> authPerm = null;
	
	Boolean inheritPermissions = null;
	
	String mailSubject = null;
	
	String mailText = null;
	
	Boolean sendMail = false;
	
	Boolean sendCopy = false;
	
	String license = null;
	
	public SetPermissionsAndMail() {
	}
	
	public SetPermissionsAndMail(String _nodeId,HashMap<String,String[]> _authPerm, Boolean _inheritPermissions, String _mailSubject,String  _mailText,Boolean _sendMail, Boolean _sendCopy, String _license){
		nodeId = _nodeId;
		authPerm = _authPerm;
		inheritPermissions = _inheritPermissions;
		mailSubject = _mailSubject;
		mailText = _mailText;
		sendMail = _sendMail;
		sendCopy = _sendCopy;
		license = _license;
	}
	
	/**
	 * @return the nodeId
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * @param nodeId the nodeId to set
	 */
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @return the authPerm
	 */
	public Map<String, String[]> getAuthPerm() {
		return authPerm;
	}

	/**
	 * @param authPerm the authPerm to set
	 */
	public void setAuthPerm(Map<String, String[]> authPerm) {
		this.authPerm = authPerm;
	}

	/**
	 * @return the inheritPermissions
	 */
	public Boolean getInheritPermissions() {
		return inheritPermissions;
	}

	/**
	 * @param inheritPermissions the inheritPermissions to set
	 */
	public void setInheritPermissions(Boolean inheritPermissions) {
		this.inheritPermissions = inheritPermissions;
	}

	/**
	 * @return the mailSubject
	 */
	public String getMailSubject() {
		return mailSubject;
	}

	/**
	 * @param mailSubject the mailSubject to set
	 */
	public void setMailSubject(String mailSubject) {
		this.mailSubject = mailSubject;
	}

	/**
	 * @return the mailText
	 */
	public String getMailText() {
		return mailText;
	}

	/**
	 * @param mailText the mailText to set
	 */
	public void setMailText(String mailText) {
		this.mailText = mailText;
	}

	/**
	 * @return the sendMail
	 */
	public Boolean getSendMail() {
		return sendMail;
	}

	/**
	 * @param sendMail the sendMail to set
	 */
	public void setSendMail(Boolean sendMail) {
		this.sendMail = sendMail;
	}

	/**
	 * @return the sendCopy
	 */
	public Boolean getSendCopy() {
		return sendCopy;
	}

	/**
	 * @param sendCopy the sendCopy to set
	 */
	public void setSendCopy(Boolean sendCopy) {
		this.sendCopy = sendCopy;
	}

	/**
	 * @return the license
	 */
	public String getLicense() {
		return license;
	}

	/**
	 * @param license the license to set
	 */
	public void setLicense(String license) {
		this.license = license;
	}

}
