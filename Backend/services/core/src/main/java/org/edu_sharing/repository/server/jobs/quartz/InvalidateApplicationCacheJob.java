/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.spring.scope.refresh.ContextRefreshUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@JobDescription(description = "This job triggers a cache reload of the application caches and metadatasets")
public class InvalidateApplicationCacheJob extends AbstractJobMapAnnotationParams {
    public InvalidateApplicationCacheJob(){
    }
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ContextRefreshUtils.refreshContext();
    }
}
