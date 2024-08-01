package org.edu_sharing.repository.tools;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.RequestHelper;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class URLHelper {

    private static Log logger = LogFactory.getLog(URLHelper.class);

    public static String getBaseUrl(boolean dynamic){
        ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
        if(dynamic && homeRepository.getBoolean(ApplicationInfo.KEY_URL_DYNAMIC,false)) {
            try {
                HttpServletRequest req = Context.getCurrentInstance().getRequest();
                return getBaseUrlFromRequest(req);
            }
            catch(Throwable t){
                logger.debug("Failed to get dynamic base url, will use the one defined in homeApp");
            }
        }

        return getBaseUrl(homeRepository.getAppId());
    }

    public static String getBaseUrlFromRequest(HttpServletRequest req) {
        ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
        String path = req.getScheme() + "://" + new RequestHelper(req).getServerName();
        int port = req.getServerPort();
        if (port != 80 && port != 443) {
            path += ":" + port;
        }
        path += "/" + homeRepository.getWebappname();
        return path;
    }

    public static String getNgRenderNodeUrl(String nodeId, String version) {
        return getNgRenderNodeUrl(nodeId, version, false);
    }

    public static String getNgRenderNodeUrl(String nodeId, String version, boolean dynamic) {
        return getNgRenderNodeUrl(nodeId, version, dynamic, null);
    }

    /**
     * Get the url to the angular rendering component
     * @param nodeId
     * @param version may be null to use the latest
     * @return
     */
    public static String getNgRenderNodeUrl(String nodeId,String version,boolean dynamic, String repository) {
        String ngComponentsUrl =  getNgComponentsUrl(dynamic)+"render/"+nodeId+(version!=null && !version.equals("-1") && !version.trim().isEmpty() ? "/"+version : "");
        if(repository != null) {
            ngComponentsUrl+="?repository="+repository;
        }
        return ngComponentsUrl;
    }

    public static String getBaseUrl(String repositoryId){
        ApplicationInfo repository = ApplicationInfoList.getRepositoryInfoById(repositoryId);
        String hostOrDomain = (repository.getDomain() == null || repository.getDomain().trim().equals(""))? repository.getHost() : repository.getDomain();

        String host = hostOrDomain;
        logger.debug("host:"+host);
        String port = repository.getClientport();
        logger.debug("port:"+port);
        String edusharingcontext = repository.getWebappname();
        logger.debug("edusharingcontext:"+edusharingcontext);

        String protocol = repository.getClientprotocol();


        String baseUrl = null;
        if(port.equals("80") || port.equals("443")){
            baseUrl = protocol+"://" + host + "/"+edusharingcontext;
        }else{
            baseUrl = protocol+"://" + host + ":" + port + "/"+ edusharingcontext;
        }
        return baseUrl;
    }

    public static String getNgComponentsUrl(){
        return getNgComponentsUrl(true);
    }

    public static String getNgComponentsUrl(boolean dynamic){
        return getBaseUrl(dynamic)+"/components/";
    }
}
