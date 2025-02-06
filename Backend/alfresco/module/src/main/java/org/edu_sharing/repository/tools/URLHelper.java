package org.edu_sharing.repository.tools;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.RequestHelper;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

@Slf4j
public class URLHelper {

    public static String getBaseUrl(boolean dynamic) {
        ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
        if (dynamic && homeRepository.getBoolean(ApplicationInfo.KEY_URL_DYNAMIC, false)) {
            try {
                HttpServletRequest req = Context.getCurrentInstance().getRequest();
                return getBaseUrlFromRequest(req);
            } catch (Throwable t) {
                log.debug("Failed to get dynamic base url, will use the one defined in homeApp");
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
     *
     * @param nodeId
     * @param version may be null to use the latest
     * @return
     */
    public static String getNgRenderNodeUrl(String nodeId, String version, boolean dynamic, String repository) {
        return getNgRenderNodeUrl(getNgComponentsUrl(dynamic), nodeId, version, repository);
    }

    public static String getNgRenderNodeUrl(String domain, String nodeId, String version) {
        return getNgRenderNodeUrl(getNgComponentsUrl(domain), nodeId, version, null);
    }

    private static String getNgRenderNodeUrl(@NonNull String domain, @NonNull String nodeId, String version, String repository) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append(domain);
        sb.append("render/");
        sb.append(nodeId);

        if(StringUtils.isNotBlank(version) && !version.equals("-1")) {
            sb.append("/");
            sb.append(version);
        }

        if (StringUtils.isNotBlank(repository)) {
            sb.append("?repository=");
            sb.append(repository);
        }

        return sb.toString();
    }


    public static String getBaseUrl(String repositoryId) {
        ApplicationInfo repository = ApplicationInfoList.getRepositoryInfoById(repositoryId);

        String host = StringUtils.isBlank(repository.getDomain())
                ? repository.getHost()
                : repository.getDomain();
        log.debug("host:{}", host);
        String port = repository.getClientport();
        log.debug("port:{}", port);
        String edusharingcontext = repository.getWebappname();
        log.debug("edusharingcontext:{}", edusharingcontext);

        String protocol = repository.getClientprotocol();


        String baseUrl;
        if (port.equals("80") || port.equals("443")) {
            baseUrl = protocol + "://" + host + "/" + edusharingcontext;
        } else {
            baseUrl = protocol + "://" + host + ":" + port + "/" + edusharingcontext;
        }
        return baseUrl;
    }

    public static String getNgComponentsUrl() {
        return getNgComponentsUrl(true);
    }

    public static String getNgComponentsUrl(String baseUrl) {
        return baseUrl.replaceAll("/$","") + "/components/";
    }

    public static String getNgComponentsUrl(boolean dynamic) {
        return getNgComponentsUrl(getBaseUrl(dynamic));
    }
}
