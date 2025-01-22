package org.edu_sharing.repository.server.jobs.quartz;


import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.lifecycle.OrganisationLifecycleService;
import org.edu_sharing.service.lifecycle.OrganisationLifecycleServiceTestSetup;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@JobDescription(description = "deletes organisation with all members (users and subgroups) and files,folders,collections")
public class OrganisationLifecycleJob extends AbstractInterruptableJob {

    @JobFieldDescription(description = "list of organisations to cleanup. is required.")
    List<String> organisations;

    @JobFieldDescription(description = "if true an test org with users and file/folder structure will be created")
    boolean testSetup = false;

    @Override
    protected void executeInterruptable(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (testSetup) {
            try {
                String organisationGroup = new OrganisationLifecycleServiceTestSetup().createTestSetup();
                log.info("Test setup completed: Organisation group created: " + organisationGroup);
            }catch (Exception ex){
                log.error("Test setup failed with: " + ex.getMessage(), ex);
            }
        } else {
            AtomicBoolean hasErrors = new AtomicBoolean(false);
            AuthenticationUtil.runAsSystem(() -> {
                for (String organisation : organisations) {
                    if (!new OrganisationLifecycleService(organisation).deleteOrganisation()) {
                        hasErrors.set(true);
                    }
                }
                return null;
            });
            if (hasErrors.get()) {
                log.error("Errors occurred while cleaning up organisation with all members. For more information show into the ");
            } else {
                log.info("Successfully cleaned up organisation with all members.");
            }
        }

    }
}
