package org.edu_sharing.repository.server;

import com.typesafe.config.Config;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import lombok.SneakyThrows;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class SecurityHeadersFilter implements Filter {

    public static ThreadLocal<String> ngCspNonce = new ThreadLocal<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)servletRequest;
        HttpServletResponse res = (HttpServletResponse)servletResponse;

        generateNonceHash();

        if(HttpMethod.GET.equals(req.getMethod())){
            addResponseHeaders(res);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * generate a - for this request - unique nonce hash that can/will be used to secure the angular injected style tags
     */
    private static void generateNonceHash() throws NoSuchAlgorithmException {
        SecureRandom instance = SecureRandom.getInstance("SHA1PRNG");
        byte[] nonce = new byte[16];
        instance.nextBytes(nonce);
        ngCspNonce.set(String.valueOf(Base64.getEncoder().encodeToString(nonce)));
    }

    private void addResponseHeaders(HttpServletResponse resp) {
        getConfiguredHeaders().entrySet().forEach(h -> resp.setHeader(h.getKey(), h.getValue()));
    }

    public static Map<String,String> getConfiguredHeaders(){
        Config headers = LightbendConfigLoader.get().getConfig("angular.headers");
        Config securityConfigs = headers.getConfig("Content-Security-Policy");
        StringBuilder joined = new StringBuilder();
        String ngCspNonceVal = ngCspNonce.get() == null ? "" : ngCspNonce.get();
        securityConfigs.entrySet().forEach((e) ->
                joined.append(e.getKey()).append(" ").append(
                        e.getValue().unwrapped().toString().replace("{{ngCspNonce}}", ngCspNonceVal)
                ).append("; ")
        );
        Map<String, String> headerList = new java.util.HashMap<>(Map.of("X-XSS-Protection", headers.getString("X-XSS-Protection"),
                "X-Frame-Options", headers.getString("X-Frame-Options"),
                "Content-Security-Policy", joined.toString()));
        for(String entry: List.of("Referrer-Policy")) {
            if (headers.hasPath(entry) && headers.getString(entry) != null) {
                headerList.put(entry, headers.getString(entry));
            }
        }
        return headerList;
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

}
