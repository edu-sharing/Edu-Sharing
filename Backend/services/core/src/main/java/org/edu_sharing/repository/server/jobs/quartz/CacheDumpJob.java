package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.admin.AdminService;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.Serializable;
import java.util.Map;


@JobDescription(description = "Dump Entries of alfresco ticket cache in log")
public class CacheDumpJob extends AbstractJobMapAnnotationParams{

    Logger log = Logger.getLogger(CacheDumpJob.class);

    @JobFieldDescription(description = "cache that should be dumped. defaults to \"ticketsCache\"",sampleValue = "ticketsCache")
    String cache;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if(cache == null) cache = "ticketsCache";
        AuthenticationUtil.runAsSystem(() -> {
            AdminService adminService = AdminServiceFactory.getInstance();
            log.info("################# START DUMP " + cache +" ###########################");
            Map<Serializable, Serializable> cacheEntries = adminService.getCacheEntries(cache);
            for (Map.Entry<Serializable, Serializable> entry : cacheEntries.entrySet()) {
                Serializable key = entry.getKey();
                Serializable value = entry.getValue();
                log.info("key: " + key + " value: " + value);
            }
            log.info("################# FINISHED DUMP " + cache +" ###########################");
            return null;
        });

    }
}
