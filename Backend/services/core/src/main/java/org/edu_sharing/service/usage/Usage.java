package org.edu_sharing.service.usage;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.edu_sharing.repository.client.tools.CCConstants;

public class Usage implements Serializable {
	// type of the usage. Default is Direct (directly associated to the node)
	// Indirect = The usage is from an other node which is related to this
	// (e.g. a collection reference which has an usage)
    public enum Type{
    	DIRECT, INDIRECT
	}
	private Type type=Type.DIRECT;

	private String appUser;

    private String appUserMail;

    private String courseId;

    private Integer distinctPersons;

    private Calendar fromUsed;

    private String lmsId;

    private String nodeId;

    private Calendar toUsed;

    private Integer usageCounter;

    private String parentNodeId;

    private String usageVersion;

    private String usageXmlParams;
    

    private String resourceId;
    
    private String guid;
    
    private Date created;
    
    private Date modified;



	public void setType(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public String getAppUser() {
		return appUser;
	}

	public void setAppUser(String appUser) {
		this.appUser = appUser;
	}

	public String getAppUserMail() {
		return appUserMail;
	}

	public void setAppUserMail(String appUserMail) {
		this.appUserMail = appUserMail;
	}

	public String getCourseId() {
		return courseId;
	}

	public void setCourseId(String courseId) {
		this.courseId = courseId;
	}

	public Integer getDistinctPersons() {
		return distinctPersons;
	}

	public void setDistinctPersons(Integer distinctPersons) {
		this.distinctPersons = distinctPersons;
	}

	public Calendar getFromUsed() {
		return fromUsed;
	}

	public void setFromUsed(Calendar fromUsed) {
		this.fromUsed = fromUsed;
	}

	public String getLmsId() {
		return lmsId;
	}

	public void setLmsId(String lmsId) {
		this.lmsId = lmsId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public Calendar getToUsed() {
		return toUsed;
	}

	public void setToUsed(Calendar toUsed) {
		this.toUsed = toUsed;
	}

	public Integer getUsageCounter() {
		return usageCounter;
	}

	public void setUsageCounter(Integer usageCounter) {
		this.usageCounter = usageCounter;
	}

	public String getParentNodeId() {
		return parentNodeId;
	}

	public void setParentNodeId(String parentNodeId) {
		this.parentNodeId = parentNodeId;
	}

	public String getUsageVersion() {
		return usageVersion;
	}

	public void setUsageVersion(String usageVersion) {
		this.usageVersion = usageVersion;
	}

	public String getUsageXmlParams() {
		return usageXmlParams;
	}

	public void setUsageXmlParams(String usageXmlParams) {
		this.usageXmlParams = usageXmlParams;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
	
	public Date getCreated() {
		return created;
	}
	
	public Date getModified() {
		return modified;
	}
	
	public void setCreated(Date created) {
		this.created = created;
	}
	
	public void setModified(Date modified) {
		this.modified = modified;
	}
    
    
	public Map<String,String> toMap(){
		Map<String,String> result = new HashMap<>();
		result.put(CCConstants.CCM_PROP_USAGE_APPID, this.getLmsId());
		result.put(CCConstants.CCM_PROP_USAGE_APPUSER, this.getAppUser());
		result.put(CCConstants.CCM_PROP_USAGE_APPUSERMAIL, this.getAppUserMail());
		if(this.getUsageCounter() != null) result.put(CCConstants.CCM_PROP_USAGE_COUNTER, this.getUsageCounter().toString());
		result.put(CCConstants.CCM_PROP_USAGE_COURSEID, this.getCourseId());
		if( this.getFromUsed() != null)	result.put(CCConstants.CCM_PROP_USAGE_FROM, this.getFromUsed().toString());
		result.put(CCConstants.CCM_PROP_USAGE_GUID, this.getGuid());
		if(this.getDistinctPersons() != null) result.put(CCConstants.CCM_PROP_USAGE_MAXPERSONS, this.getDistinctPersons().toString());
		result.put(CCConstants.CCM_PROP_USAGE_PARENTNODEID, this.getParentNodeId());
		result.put(CCConstants.CCM_PROP_USAGE_RESSOURCEID, this.getResourceId());
		if(this.getToUsed() != null) result.put(CCConstants.CCM_PROP_USAGE_TO, this.getToUsed().toString());
		result.put(CCConstants.CCM_PROP_USAGE_VERSION, this.getUsageVersion());
		result.put(CCConstants.CCM_PROP_USAGE_XMLPARAMS, this.getUsageXmlParams());
		
		return result;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}
	
}
