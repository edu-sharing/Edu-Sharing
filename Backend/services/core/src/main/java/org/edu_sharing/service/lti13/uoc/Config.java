package org.edu_sharing.service.lti13.uoc;

import edu.uoc.elc.lti.tool.Tool;
import edu.uoc.elc.spring.lti.tool.BasicToolDefinition;
import edu.uoc.elc.spring.lti.tool.ToolDefinitionBean;
import edu.uoc.lti.accesstoken.AccessTokenRequestBuilder;
import edu.uoc.lti.accesstoken.JSONAccessTokenRequestBuilderImpl;
import edu.uoc.lti.claims.ClaimAccessor;
import edu.uoc.lti.clientcredentials.ClientCredentialsTokenBuilder;
import edu.uoc.lti.deeplink.DeepLinkingTokenBuilder;
import edu.uoc.lti.jwt.claims.JWSClaimAccessor;
import edu.uoc.lti.jwt.client.JWSClientCredentialsTokenBuilder;
import edu.uoc.lti.jwt.deeplink.JWSTokenBuilder;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

import jakarta.servlet.http.HttpServletRequest;
import org.edu_sharing.service.lti13.uoc.elc.spring.lti.tool.ToolFactory;

public class Config {
    private static BasicToolDefinition getBasicToolDefinition(ApplicationInfo platform){
        BasicToolDefinition basicToolDefinition = new BasicToolDefinition();
        basicToolDefinition.setAccessTokenUrl(platform.getLtiAuthTokenEndpoint());
        basicToolDefinition.setDeploymentId(platform.getLtiDeploymentId());
        basicToolDefinition.setClientId(platform.getLtiClientId());
        basicToolDefinition.setKeySetUrl(platform.getLtiKeysetUrl());
        basicToolDefinition.setName(platform.getAppCaption());
        basicToolDefinition.setOidcAuthUrl(platform.getLtiOidc());
        basicToolDefinition.setPlatform(platform.getLtiIss());
        basicToolDefinition.setPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey());
        basicToolDefinition.setPublicKey(ApplicationInfoList.getHomeRepository().getPublicKey());
        return basicToolDefinition;
    }

    private static ClaimAccessor claimAccessor(BasicToolDefinition basicToolDefinition) {
        return new JWSClaimAccessor(basicToolDefinition.getKeySetUrl());
    }

    private static DeepLinkingTokenBuilder deepLinkingTokenBuilder(BasicToolDefinition basicToolDefinition) {
        return new JWSTokenBuilder(basicToolDefinition.getPublicKey(), basicToolDefinition.getPrivateKey());
    }

    private static ClientCredentialsTokenBuilder clientCredentialsTokenBuilder(BasicToolDefinition basicToolDefinition) {
        return new JWSClientCredentialsTokenBuilder(basicToolDefinition.getPublicKey(), basicToolDefinition.getPrivateKey());
    }

    private static AccessTokenRequestBuilder accessTokenRequestBuilder() {
        return new JSONAccessTokenRequestBuilderImpl();
    }

    private static ToolDefinitionBean getToolDefinitionBean(ApplicationInfo platform){
        BasicToolDefinition btd = getBasicToolDefinition(platform);

        ToolDefinitionBean tdb = new ToolDefinitionBean(btd,
                claimAccessor(btd),
                deepLinkingTokenBuilder(btd),
                clientCredentialsTokenBuilder(btd),
                accessTokenRequestBuilder());
        return tdb;
    }

    public static Tool getTool(ApplicationInfo platform, HttpServletRequest request, boolean clearSession){
        return new ToolFactory().from(getToolDefinitionBean(platform), request, clearSession);
    }
}
