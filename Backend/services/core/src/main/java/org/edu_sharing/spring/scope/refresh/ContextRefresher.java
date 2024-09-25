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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ContextRefresher {

    private final RefreshScope scope;

    protected RefreshScope getScope() {
        return this.scope;
    }

    public synchronized void refresh(){
        refresh(true);
    }

    /**
     * Fires a ContextRefreshEvent to enforce a context reload of Refresh scoped beans.
     * @param isCaller true if this event was called by the same cluster instance otherwise false
     */
    public synchronized void refresh(boolean isCaller) {
        this.scope.refreshAll(isCaller);
    }
}
