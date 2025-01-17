package org.edu_sharing.service.lti13.uoc.elc.spring.lti.tool;

import edu.uoc.elc.lti.tool.Tool;
import edu.uoc.elc.lti.tool.ToolDefinition;



import edu.uoc.elc.spring.lti.tool.ToolDefinitionBean;
import edu.uoc.elc.spring.lti.tool.ToolDefinitionFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.edu_sharing.service.lti13.uoc.elc.spring.lti.security.openid.HttpSessionOIDCLaunchSession;
import org.edu_sharing.service.lti13.uoc.elc.spring.lti.security.utils.TokenFactory;

/**
 * @author xaracil@uoc.edu
 */
public class ToolFactory {
    HttpSessionOIDCLaunchSession oidcLaunchSession;

    public ToolFactory() {
    }

    public Tool from(ToolDefinitionBean toolDefinitionBean, HttpServletRequest request) {
        return this.from(toolDefinitionBean, request, false);
    }

    public Tool from(ToolDefinitionBean toolDefinitionBean, HttpServletRequest request, boolean clearSession) {
        this.oidcLaunchSession = new HttpSessionOIDCLaunchSession(request);
        if (clearSession) {
            this.oidcLaunchSession.clear();
        }

        ToolDefinition toolDefinition = ToolDefinitionFactory.from(toolDefinitionBean);
        Tool tool = new Tool(toolDefinition, toolDefinitionBean.getClaimAccessor(), this.oidcLaunchSession, toolDefinitionBean.getBuilders());
        String token = TokenFactory.from(request);
        String state = request.getParameter("state");
        tool.validate(token, state);
        return tool;
    }
}
