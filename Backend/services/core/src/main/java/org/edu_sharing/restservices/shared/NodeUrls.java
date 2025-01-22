package org.edu_sharing.restservices.shared;

import lombok.Data;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.lti13.LTIJWTUtil;

import java.security.GeneralSecurityException;

@Data
public class NodeUrls {

    private String repositoryBaseUrl;
    private String generateLtiResourceLink;

    public NodeUrls(Node node, String requestedVersion) {
        repositoryBaseUrl = URLHelper.getBaseUrl(true);
        if(node.getAspects().contains(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_LTITOOL_NODE))){
            generateLtiResourceLink = repositoryBaseUrl + "/rest/ltiplatform/v13/generateLoginInitiationFormResourceLink?nodeId=" + node.getRef().getId();
            if(Context.getCurrentInstance() != null){
                if(Context.getCurrentInstance().isSingleUseNodeId(node.getRef().getId())){
                    //generate short living jwt
                    try {
                        String jwt = LTIJWTUtil.getShortAccessJwt(node.getRef().getId(),60);
                        generateLtiResourceLink +="&jwt="+jwt;
                    } catch (GeneralSecurityException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
            if(requestedVersion != null && !requestedVersion.equals("-1")){
                generateLtiResourceLink +="&version="+requestedVersion;
            }
        }
    }

}
