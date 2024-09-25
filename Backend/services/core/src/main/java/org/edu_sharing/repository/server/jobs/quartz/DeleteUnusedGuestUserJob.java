package org.edu_sharing.repository.server.jobs.quartz;

import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@JobDescription(description = "Deletes all people with primary affiliation set to Guest that are no longer referred in the guest configuration")
public class DeleteUnusedGuestUserJob  extends AbstractJobMapAnnotationParams {

    @Autowired
    private GuestService guestService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        guestService.deleteUnusedGuests();
    }
}
