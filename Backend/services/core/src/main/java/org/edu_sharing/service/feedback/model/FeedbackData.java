package org.edu_sharing.service.feedback.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedbackData implements Serializable {
    /**
     * hashed authority who gave the feedback (not reversable)
     */
    private String authority;
    /**
     * custom (key value) data
     * will be stored on alfresco node
     */
    private Map<String, List<String>> data;
    private Date createdAt;
    private Date modifiedAt;

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public Map<String, List<String>> getData() {
        return data;
    }

    public void setData(Map<String, List<String>> data) {
        this.data = data;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }
}
