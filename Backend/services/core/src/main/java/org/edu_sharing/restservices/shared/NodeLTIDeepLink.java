package org.edu_sharing.restservices.shared;

import lombok.Data;

@Data
public class NodeLTIDeepLink {
    String ltiDeepLinkReturnUrl;
    String jwtDeepLinkResponse;

    public NodeLTIDeepLink(String ltiDeepLinkReturnUrl, String jwtDeepLinkResponse){
        this.ltiDeepLinkReturnUrl = ltiDeepLinkReturnUrl;
        this.jwtDeepLinkResponse = jwtDeepLinkResponse;
    }
}
