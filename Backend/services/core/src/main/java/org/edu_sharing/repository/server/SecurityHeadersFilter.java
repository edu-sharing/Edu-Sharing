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
        Config headers = LightbendConfigLoader.get().getConfig("angular.headers");
        resp.setHeader("X-XSS-Protection", headers.getString("X-XSS-Protection"));
        resp.setHeader("X-Frame-Options", headers.getString("X-Frame-Options"));
        Config securityConfigs = headers.getConfig("Content-Security-Policy");
        StringBuilder joined = new StringBuilder();
        securityConfigs.entrySet().forEach((e) ->
                joined.append(e.getKey()).append(" ").append(
                        e.getValue().unwrapped().toString().replace("{{ngCspNonce}}", ngCspNonce.get())
                ).append("; ")
        );
        resp.setHeader("Content-Security-Policy", joined.toString());
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

}
