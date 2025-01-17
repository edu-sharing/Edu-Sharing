package org.edu_sharing.restservices.lti.v13;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.Encryption;
import org.edu_sharing.service.lti13.model.LTISessionObject;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Service;
import org.edu_sharing.service.usage.Usage2Exception;
import org.springframework.extensions.surf.util.URLEncoder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;


public class ApiTool {

    static Logger logger = Logger.getLogger(ApiTool.class);

    public static Response processError(HttpServletRequest req, Throwable e, String errorType){
        if(e != null) logger.error(e.getMessage(),e);
        try {
            return Response.seeOther(new URI(req.getScheme() +"://"
                            + req.getServerName()
                            +":"+req.getServerPort()
                            + "/edu-sharing/components/messages/"+errorType+"/"+ URLEncoder.encode(e.getMessage())))
                    .build();
        } catch (URISyntaxException ex) {
            return Response.status(Response.Status.OK).entity(getHTML(null,null,"error:" + ex.getMessage())).build();
        }
    }

    public static String getHTML(String formTargetUrl, Map<String,String> params){
        return getHTML(formTargetUrl,params,null,null);
    }

    public static String getHTML(String formTargetUrl, Map<String,String> params, String errorMessage){
        return getHTML(formTargetUrl,params,errorMessage,null);
    }
    /**
     * @TODO use template engine?
     * @param formTargetUrl
     * @param params
     * @return
     */
    public static String getHTML(String formTargetUrl, Map<String,String> params, String message, String javascript){
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        if(javascript != null){
            sb.append("<script type=\"text/javascript\">"+javascript+"</script>");
        }
        if(message == null) {
            String FORMNAME = "ltiform";
            sb.append("<script type=\"text/javascript\">window.onload=function(){document.forms[\""+FORMNAME+"\"].submit();}</script>");
            sb.append("<form action=\"" + formTargetUrl + "\" method=\"post\" name=\"" + FORMNAME + "\">");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append("<input type=\"hidden\" id=\"" + entry.getKey() + "\" name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" class=\"form-control\"/>");
            }
            sb.append("<input type=\"submit\" value=\"Submit POST\" style=\"display:none\">")
                    .append("</form>");
        }else {
            sb.append(message);
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    public static String encrpt(String toEncrypt){
        ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
        String encrpted = null;
        try {
            Encryption encryptionTool = new Encryption("RSA");
            byte[] userEncryptedBytes = encryptionTool.encrypt(toEncrypt.getBytes(), encryptionTool.getPemPublicKey(homeRepository.getPublicKey()));
            encrpted = java.util.Base64.getEncoder().encodeToString(userEncryptedBytes);
        }catch (Exception e){

        }
        return encrpted;
    }

    public static String decrpt(String toDecrypt){
        Encryption encryptionTool = new Encryption("RSA");

        try {
            return encryptionTool.decrypt(
                    Base64.decodeBase64(toDecrypt.getBytes()),
                    encryptionTool.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey()));
        }catch (Exception e){
            return null;
        }
    }

    public static void handleUsagePermissions(String node, HttpSession session, String appId, String contextId, Usage2Service usageService) throws Usage2Exception {
        if(ApplicationInfoList.getRepositoryInfoById(appId).isLtiUsagesEnabled()){
            Usage usage = usageService.getUsage(appId, contextId, node, null);
            if(usage != null){
                Context.getCurrentInstance().addSingleUseNode(node);
            }
        }
    }
}
