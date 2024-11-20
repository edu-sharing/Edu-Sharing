package org.edu_sharing.alfresco.interceptors;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.edu_sharing.alfresco.service.guest.GuestService;

@Slf4j
public class AuthorityServiceGuestInterceptor implements MethodInterceptor {


    @Setter
    private GuestService guestService;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        String methodName = methodInvocation.getMethod().getName();
        if(!methodName.equals("addAuthority")){
            return methodInvocation.proceed();
        }

        String childName = (String)methodInvocation.getArguments()[1];
        if(childName == null || !guestService.isGuestUser(childName)) {
            return methodInvocation.proceed();
        }

        log.error("someone tried to add the guest user {} to a group. this is not allowed", childName);
        return null;

    }
}
