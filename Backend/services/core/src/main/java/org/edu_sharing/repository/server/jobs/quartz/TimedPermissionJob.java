package org.edu_sharing.repository.server.jobs.quartz;


import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.service.permission.TimedPermission;
import org.edu_sharing.service.permission.TimedPermissionMapper;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

@JobDescription(description = "")
public class TimedPermissionJob extends AbstractInterruptableJob {

    @Autowired
    private TimedPermissionMapper timedPermissionMapper;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RetryingTransactionHelper retryingTransactionHelper;

    @Override
    protected void executeInterruptable(JobExecutionContext jobExecutionContext) {

        List<TimedPermission> permissionsToAdd = timedPermissionMapper.findAllByFromAfterAndNotActivated(new Date());
        List<TimedPermission> permissionsToRemove = timedPermissionMapper.findAllByToBefore(new Date());


            for (TimedPermission timedPermission : permissionsToAdd) {
            retryingTransactionHelper.doInTransaction(() ->
                    AuthenticationUtil.runAsSystem(() -> {
                        permissionService.setPermission(
                                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, timedPermission.getNode_id()),
                                timedPermission.getAuthority(),
                                timedPermission.getPermission(),
                                true);

                        if(timedPermission.getTo() == null){
                            timedPermissionMapper.delete(timedPermission);
                        }else {
                            timedPermission.setActivated(true);
                            timedPermissionMapper.save(timedPermission);
                        }
                        return null;
                    }));
        }

        for (TimedPermission timedPermission : permissionsToRemove) {
            retryingTransactionHelper.doInTransaction(() ->
                    AuthenticationUtil.runAsSystem(() -> {
                        permissionService.deletePermission(
                                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, timedPermission.getNode_id()),
                                timedPermission.getAuthority(),
                                timedPermission.getPermission());

                        timedPermissionMapper.delete(timedPermission);
                        return null;
                    }));
        }


    }
}
