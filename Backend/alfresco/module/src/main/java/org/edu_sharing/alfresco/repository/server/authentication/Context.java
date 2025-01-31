package org.edu_sharing.alfresco.repository.server.authentication;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Context {

    /**
     * max number of nodes that are stored in the session for a temporary access via usage
     */
    private static final int MAX_SINGLE_USE_NODEIDS = 25;
    private static final ThreadLocal<Context> instance = new ThreadLocal<>();
    private static ServletContext globalContext;

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private B3 b3 = new B3() { };


    private Context(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Nullable
    public static Context getCurrentInstance() {
        return instance.get();
    }

    @Nullable
    public static ServletContext getGlobalContext() {
        return globalContext;
    }

    @Nullable
    public static Context setInstance(Context context){
        instance.set(context);
        return context;
    }

    @NotNull
    public static Context newInstance(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        Context context = new Context(request,response);
        if(globalContext==null)
            globalContext=servletContext;
        context.init();
        instance.set(context);
        return context;
    }

    private void init() {
        b3 = new B3() {

            @Override
            public String getTraceId() {
                return request.getHeader("X-B3-TraceId");
            }

            @Override
            public String getClientTraceId() {
                return request.getHeader("X-Client-Trace-Id");
            }

            @Override
            public String getSpanId() {
                return request.getHeader("X-B3-SpanId");
            }

            @Override
            public boolean isSampled() {
                return "1".equals(request.getHeader("X-B3-Sampled"));
            }

            @Override
            public String toString() {
                if (getTraceId() != null) {
                    return "TraceId: " + getTraceId();
                }
                return "";
            }

            private boolean isX3Header(String header) {
                return header.toUpperCase().startsWith("X-B3-") ||
                        header.toUpperCase().startsWith("X-OT-") ||
                        header.equalsIgnoreCase("X-Request-Id") ||
                        header.equalsIgnoreCase("X-Client-Trace-Id");
            }

            @Override
            public Map<String, String> getX3Headers() {
                try {
                    return Collections.list(request.getHeaderNames()).stream()
                            .filter(this::isX3Header)
                            .collect(Collectors.toMap(
                                    k -> k,
                                    request::getHeader
                            ));
                } catch (Throwable t) {
                    Logger.getLogger(Context.class).warn("Unexpected error while fetching x3 headers", t);
                    return Collections.emptyMap();
                }
            }
        };

        if(b3.getTraceId() != null) {
            MDC.put("TraceId", b3.getTraceId());
        }
        if(b3.getClientTraceId() != null) {
            MDC.put("ClientTraceId", b3.getClientTraceId());
        }
        if(b3.getSpanId() != null) {
            MDC.put("SpanId", b3.getSpanId());
        }
    }

    public static void release() {
        instance.remove();
    }

    @Nullable
    public HttpServletRequest getRequest() {
        return request;
    }

    @Nullable
    public B3 getB3() {
        return b3;
    }

    @Nullable
    public HttpServletResponse getResponse() {
        return response;
    }

    @Nullable
    public String getSessionAttribute(String key){
        String sessionAtt = null;
        if(this.getRequest() != null
                && this.getRequest().getSession() != null){
            sessionAtt = (String)this.getRequest().getSession().getAttribute(key);
        }
        return sessionAtt;
    }

    /**
     * is the given nodeId in the current context of an lms access?
     * @param nodeId
     * @return
     */
    public boolean isSingleUseNodeId(String nodeId) {
        List<String> list = null;
        if(this.getRequest() != null && this.getRequest().getSession() != null){
            list = (List<String>) this.getRequest().getSession().getAttribute(CCConstants.AUTH_SINGLE_USE_NODEIDS);
        }
        if(list == null) {
            return false;
        }
        return list.contains(nodeId);
    }

    /**
     * add the given node to the context of an lms access
     * @param nodeId
     */
    public synchronized void addSingleUseNode(String nodeId) {
        List<String> list = null;
        if(this.getRequest() != null && this.getRequest().getSession() != null){
            list = (List<String>) this.getRequest().getSession().getAttribute(CCConstants.AUTH_SINGLE_USE_NODEIDS);
        }
        if(list == null) {
            list = new ArrayList<>();
        }
        if(list.contains(nodeId)){
            return;
        }
        list.add(nodeId);
        while(list.size() > MAX_SINGLE_USE_NODEIDS) {
            list.remove(0);
        }
        this.getRequest().getSession().setAttribute(CCConstants.AUTH_SINGLE_USE_NODEIDS, list);
    }

    @Nullable
    public String getSessionId(){
        if(this.getRequest() != null
                && this.getRequest().getSession() != null){
            return this.request.getSession().getId();
        }
        return null;
    }

    @Nullable
    public String getLocale(){
        return getSessionAttribute(CCConstants.AUTH_LOCALE);
    }

    @Nullable
    public String getAuthType(){
        return getSessionAttribute(CCConstants.AUTH_TYPE);
    }

    @Nullable
    public String getAccessToken(){
        return getSessionAttribute(CCConstants.AUTH_ACCESS_TOKEN);
    }

    public interface B3 {

        @Nullable
        default String getTraceId() {
            return null;
        }

        @Nullable
        default String getClientTraceId() {
            return null;
        }

        @Nullable
        default String getSpanId() {
            return null;
        }

        default boolean isSampled() {
            return false;
        }

        default void addToRequest(RequestBuilder request) {
            for (Map.Entry<String, String> header : getX3Headers().entrySet()) {
                request.setHeader(header.getKey(), header.getValue());
            }
        }
        default void addToRequest(HttpMethodBase get) {
            for (Map.Entry<String, String> header : getX3Headers().entrySet()) {
                get.addRequestHeader(header.getKey(), header.getValue());
            }
        }

        default Map<String, String> getX3Headers() {
            return Collections.emptyMap();
        };
    }
}

