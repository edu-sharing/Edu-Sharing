package org.edu_sharing.service.nodeservice;

import org.apache.http.HttpHeaders;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;

import java.net.URI;
import java.net.URISyntaxException;

public class CallSourceHelper {
    public static final String WEBAPP_BASE_PATH = "/edu-sharing";

    public enum CallSource {
        Search,
        Render,
        Preview,
        Sitemap,
        Workspace,
        // access from external edu connector tool
        ToolConnector,
        RatingApi,
        Unknown
    }

    public static CallSource getCallSource(){
        if(Context.getCurrentInstance() == null || Context.getCurrentInstance().getRequest() == null){
            return CallSource.Unknown;
        }

        String requestPath = "";
        String refererPath = "";
        if(Context.getCurrentInstance() != null) {
            String requestURI = Context.getCurrentInstance().getRequest().getRequestURI();
            String referer =  Context.getCurrentInstance().getRequest().getHeader(HttpHeaders.REFERER);

            try {
                requestPath = new URI(requestURI).getPath();
                if(referer != null){
                    refererPath = new URI(referer).getPath();
                }
            } catch (URISyntaxException e) {
               //
            }

        }
        if(isToolConnector()) {
            return CallSource.ToolConnector;
        }else if(isSearch(requestPath)){
            return CallSource.Search;
        }else if(isRender(requestPath) || (isRender(refererPath))){
            return CallSource.Render;
        }else if(isPreview(requestPath)){
            return CallSource.Preview;
        }else if(isSitemap(requestPath)){
            return CallSource.Sitemap;
        }else if(isRatingApi(requestPath)){
            return CallSource.RatingApi;
        }else{
            return CallSource.Workspace;
        }
    }

    private static boolean isRatingApi(String requestPath) {
        return requestPath.startsWith(WEBAPP_BASE_PATH + "/rest/rating");
    }

    private static boolean isToolConnector() {
        return ContextManagementFilter.accessTool.get() != null &&
                ApplicationInfo.TYPE_CONNECTOR.equals(ContextManagementFilter.accessTool.get().getApplicationInfo().getType());
    }

    private static boolean isSearch(String path){
        if(path.startsWith(WEBAPP_BASE_PATH + "/rest/search")){
            return true;
        }
        return false;
    }

    private static boolean isRender(String path){
        if(path.startsWith(WEBAPP_BASE_PATH + "/components/render")
                || path.startsWith(WEBAPP_BASE_PATH + "/rest/rendering")
                || path.startsWith(WEBAPP_BASE_PATH + "/eduservlet/render")
                || path.startsWith(WEBAPP_BASE_PATH + "/content")){
            return true;
        }
        return false;
    }

    private static boolean isPreview(String path){
        if(path.startsWith(WEBAPP_BASE_PATH + "/preview")){
            return true;
        }
        return false;
    }

    private static boolean isSitemap(String path){
        if(path.startsWith(WEBAPP_BASE_PATH + "/eduservlet/sitemap")){
            return true;
        }
        return false;
    }
}
