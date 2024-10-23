package org.edu_sharing.repository.server.jobs.quartz;


import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.service.permission.PermissionService;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

@JobDescription(description = "Updates permission of timed permissions")
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class TimedPermissionJob extends AbstractInterruptableJob {

    @Autowired
    private PermissionService permissionService;


    @Override
    protected void executeInterruptable(JobExecutionContext jobExecutionContext) {
        permissionService.updateTimedPermissions();
    }
}
