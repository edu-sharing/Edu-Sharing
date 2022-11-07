package org.edu_sharing.restservices.shared;

import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;

public class NodeUrls {


    public NodeUrls(Node node) {
        repositoryBaseUrl = URLTool.getBaseUrl(true);
        if(node.getAspects().contains(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_LTITOOL_NODE))){
            generateLtiResourceLink = repositoryBaseUrl + "/rest/ltiplatform/v13/generateLoginInitiationFormResourceLink?nodeId=" + node.getRef().getId();
        }
    }

    String repositoryBaseUrl;

    String generateLtiResourceLink;
}
