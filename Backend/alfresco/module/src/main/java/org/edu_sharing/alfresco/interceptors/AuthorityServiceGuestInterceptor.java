package org.edu_sharing.alfresco.interceptors;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

public class AuthorityServiceGuestInterceptor implements MethodInterceptor {

    public static final String REPOSITORY_GUEST_USERNAME = "repository.guest.username";
    Logger logger = Logger.getLogger(AuthorityServiceGuestInterceptor.class);



    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        String methodName = methodInvocation.getMethod().getName();
        if(!methodName.equals("addAuthority")){
            return methodInvocation.proceed();
        }


        if(LightbendConfigLoader.get().hasPath(REPOSITORY_GUEST_USERNAME)) {
            return methodInvocation.proceed();
        }

        String childName = (String)methodInvocation.getArguments()[1];
        String guestUsername = LightbendConfigLoader.get().getString(REPOSITORY_GUEST_USERNAME);
        if(childName == null || !childName.equals(guestUsername)) {
            return methodInvocation.proceed();
        }

        logger.error("someone tried to add "+guestUsername+" to a group. this is not allowed");
        return null;

    }
}
