package org.edu_sharing.spring.context;

import com.typesafe.config.Config;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.PropertySource;


public class TypesafeConfigPropertySource extends PropertySource<Config> {
    public TypesafeConfigPropertySource(String name) {
        super(name, LightbendConfigLoader.get());
    }

    @NotNull
    @Override
    public Config getSource() {
        return LightbendConfigLoader.get();
    }

    @Override
    public Object getProperty(String path) {
        Config config = getSource();
        if (path.contains("["))
            return null;
        if (path.contains(":"))
            return null;
        if (config.hasPath(path)) {
            return config.getAnyRef(path);
        }
        return null;
    }

}
