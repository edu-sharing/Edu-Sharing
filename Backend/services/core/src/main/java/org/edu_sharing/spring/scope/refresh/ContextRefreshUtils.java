package org.edu_sharing.spring.scope.refresh;

import org.edu_sharing.spring.ApplicationContextFactory;

public class ContextRefreshUtils {

    public static void refreshContext() {
        ContextRefresher contextRefresher = ApplicationContextFactory.getApplicationContext().getBean(ContextRefresher.class);
        contextRefresher.refresh();
    }
}
