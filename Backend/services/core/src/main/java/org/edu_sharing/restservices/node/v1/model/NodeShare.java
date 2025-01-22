package org.edu_sharing.restservices.node.v1.model;

import lombok.Data;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.server.tools.URLTool;
import com.fasterxml.jackson.annotation.JsonProperty;


@Data
public class NodeShare  {
	private boolean password;
	private String token;
	private String email;
	private Long expiryDate;
	private long invitedAt;
	private int downloadCount;
	private String url;
	private String shareId;


	public NodeShare(){}
	public NodeShare(NodeRef node,Share share){
		shareId=share.getNodeId();
		token=share.getToken();
		email=share.getEmail();
		password=share.getPassword()!=null;
		expiryDate=share.getExpiryDate();
		invitedAt=share.getInvitedAt().getTime();
		downloadCount=share.getDownloadCount();
		url=URLTool.getShareServletUrl(node, share.getToken());
	}
}
