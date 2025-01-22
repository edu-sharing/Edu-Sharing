package org.edu_sharing.restservices.about.v1.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class AboutPlugins {
    private final List<PluginInfo> plugins;

    public AboutPlugins(@Autowired(required = false) List<PluginInfo> plugins) {
        this.plugins = plugins;
    }
}
