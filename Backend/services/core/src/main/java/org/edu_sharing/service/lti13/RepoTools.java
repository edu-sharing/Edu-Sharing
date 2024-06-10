package org.edu_sharing.service.lti13;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.authentication.EduAuthentication;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.springframework.context.ApplicationContext;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;


public class RepoTools {
    public static Logger logger = Logger.getLogger(RepoTools.class);

    public ApplicationInfo getApplicationInfo(String iss, String clientId, String ltiDeploymentId) throws LTIException {

        /**
         * can we reuse our ApplicationInfo ?, even we got only one appId?
         * we could take as appId:
         * - client_id + deployment_id OR
         * - iss + client_id  OR
         * - iss + deployment_id OR
         * - iss
         */
        ApplicationInfo applicationInfo = null;
        String applicationId = null;
        if(clientId != null && ltiDeploymentId != null){
            applicationId = clientId + ltiDeploymentId;
            applicationInfo = ApplicationInfoList.getApplicationInfos().get(applicationId);
            if(applicationInfo == null){
               throw new LTIException(LTIConstants.ERROR_DEPLOYMENT_NOT_FOUND + LTIConstants.LTI_PARAM_CLIENT_ID+":"+clientId +" "+LTIConstants.LTI_PARAM_DEPLOYMENT_ID+":"+ltiDeploymentId +" found.");
            }
        }else if(clientId != null){
            applicationId = clientId + iss;
            applicationInfo = ApplicationInfoList.getApplicationInfos().get(applicationId);
            if(applicationInfo == null){
                throw new LTIException(LTIConstants.ERROR_DEPLOYMENT_NOT_FOUND + LTIConstants.LTI_PARAM_CLIENT_ID+":"+clientId +" "+LTIConstants.LTI_PARAM_ISS+":"+iss +" found.");
            }
        }else if(ltiDeploymentId != null){
            applicationId = iss + ltiDeploymentId;
            applicationInfo = ApplicationInfoList.getApplicationInfos().get(applicationId);
            if(applicationInfo == null){
                throw new LTIException(LTIConstants.ERROR_DEPLOYMENT_NOT_FOUND + LTIConstants.LTI_PARAM_ISS+":"+iss +" "+LTIConstants.LTI_PARAM_DEPLOYMENT_ID+":"+ltiDeploymentId +" found.");
            }
        }else{
            applicationId = iss;
            applicationInfo = ApplicationInfoList.getApplicationInfos().get(applicationId);
            if(applicationInfo == null){
                throw new LTIException(LTIConstants.ERROR_DEPLOYMENT_NOT_FOUND + LTIConstants.LTI_PARAM_ISS+":"+iss +" "+LTIConstants.LTI_PARAM_DEPLOYMENT_ID+":"+ltiDeploymentId +" found.");
            }
        }
        return applicationInfo;

    }

    public String getAppId(String iss, String clientId, String ltiDeploymentId){
        String applicationId = null;
        if(clientId != null && ltiDeploymentId != null) {
            applicationId = clientId + ltiDeploymentId;
        }else if(clientId != null){
            applicationId = clientId + iss;
        }else if(ltiDeploymentId != null){
            applicationId = iss + ltiDeploymentId;
        }else{
            applicationId = iss;
        }
        return applicationId;
    }

    public static String authenticate(HttpServletRequest req, Map<String,String> ssoMap){
        ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
        SSOAuthorityMapper ssoMapper = (SSOAuthorityMapper)eduApplicationContext.getBean("ssoAuthorityMapper");
        AuthenticationToolAPI authTool = new AuthenticationToolAPI();
        Map<String,String> validAuthInfo = authTool.validateAuthentication(req.getSession());

        String userName = ssoMap.get(ssoMapper.getSSOUsernameProp());
        if (validAuthInfo != null ) {
            if (validAuthInfo.get(CCConstants.AUTH_USERNAME).equals(userName)) {
                logger.info("got valid ticket from session for user:"+userName);
                return userName;
            } else {

                /**
                 * this can make problems if lti data that is needed later will be destroyed with session ending
                 */
                logger.error("end session for user:" + validAuthInfo.get(CCConstants.AUTH_USERNAME)
                        + " this can make problems if lti data that is needed later will be destroyed with session invalidating");
                authTool.logout(validAuthInfo.get(CCConstants.AUTH_TICKET));
                if(req.getSession(false) != null) {
                    req.getSession(false).invalidate();
                }
                req.getSession(true);
            }
        }

        EduAuthentication authService =  (EduAuthentication)eduApplicationContext.getBean("authenticationService");
        authService.authenticateBySSO(SSOAuthorityMapper.SSO_TYPE_LTI,ssoMap);
        String ticket = authService.getCurrentTicket();
        authTool.storeAuthInfoInSession(userName, ticket,CCConstants.AUTH_TYPE_LTI, req.getSession());
        return userName;
    }

    public static Map<String,String> mapToSSOMap(String username, String firstName, String lastName, String email){
        ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
        SSOAuthorityMapper ssoMapper = (SSOAuthorityMapper)eduApplicationContext.getBean("ssoAuthorityMapper");
        Map<String,String> result = new HashMap<>();
        if(firstName != null) result.put(ssoMapper.getUserAttribute(CCConstants.CM_PROP_PERSON_FIRSTNAME),firstName);
        if(username != null) result.put(ssoMapper.getUserAttribute(CCConstants.CM_PROP_PERSON_USERNAME),username);
        if(lastName != null) result.put(ssoMapper.getUserAttribute(CCConstants.CM_PROP_PERSON_LASTNAME),lastName);
        if(email != null) result.put(ssoMapper.getUserAttribute(CCConstants.CM_PROP_PERSON_EMAIL),email);
        return result;
    }
}
