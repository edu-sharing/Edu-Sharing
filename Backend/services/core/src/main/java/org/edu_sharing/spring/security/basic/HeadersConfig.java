package org.edu_sharing.spring.security.basic;

import org.edu_sharing.repository.server.SecurityHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import java.util.Map;

public class HeadersConfig {
    public static void config(HttpSecurity http) throws Exception {
        //disable frame options here so that header config is used
        http.headers(h -> h.frameOptions(fo -> fo.disable()));
        for (Map.Entry<String, String> h : SecurityHeadersFilter.getConfiguredHeaders()
                .entrySet()) {
            http.headers(c -> c.addHeaderWriter(new StaticHeadersWriter(h.getKey(), h.getValue())));
        }
    }

}
