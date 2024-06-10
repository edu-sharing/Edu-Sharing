package org.edu_sharing.spring.scope.refresh;

/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <a href="https://github.com/spring-cloud/spring-cloud-commons">Source by spring cloud</a>
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.edu_sharing.lightbend.LightbendConfigHelper;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.PropertiesInterceptorFactory;
import org.edu_sharing.service.provider.ProviderHelper;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.support.StandardServletEnvironment;

@Slf4j
@RequiredArgsConstructor
public class ContextRefresher {

    private final RefreshScope scope;

    protected RefreshScope getScope() {
        return this.scope;
    }

    public synchronized void refresh() {
        RepoFactory.refresh();
        ApplicationInfoList.refresh();
        LightbendConfigHelper.refresh();
        MetadataReader.refresh();
        PropertiesInterceptorFactory.refresh();
        ProviderHelper.clearCache();

        this.scope.refreshAll();
    }
}