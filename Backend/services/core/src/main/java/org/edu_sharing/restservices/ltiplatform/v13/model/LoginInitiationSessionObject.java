package org.edu_sharing.restservices.ltiplatform.v13.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginInitiationSessionObject implements Serializable {

    public enum MessageType {resourcelink, deeplink}

    private String contextId;
    private String clientId;
    private String appId;
    private String resourceLinkNodeId;
    private String contentUrlNodeId;
    private String version;
    private String token;
    private String toolNonce;
    private String launchPresentation;
    private String user;
    private boolean resourceLinkEditMode = true;
    private MessageType messageType;
    private Long lastAccessed;

}
