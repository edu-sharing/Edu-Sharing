package org.edu_sharing.service.permission;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceInterceptor;
import org.edu_sharing.service.nodeservice.NodeServiceInterceptorPermissions;
import org.edu_sharing.service.nodeservice.PropertiesInterceptorFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PermissionServiceInterceptor implements MethodInterceptor {

    public void init() {

    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        if (methodName.equals("hasPermission") || methodName.equals("hasAllPermissions")) {
            String nodeId = (String) invocation.getArguments()[2];
            Object data = invocation.getArguments()[3];
            if (methodName.equals("hasPermission")) {
                boolean result = (boolean) invocation.proceed();
                // to improve performance, if node seems to have already valid permission, return true
                if (result)
                    return result;

                for (NodeServiceInterceptorPermissions nodeServiceInterceptorPermission : PropertiesInterceptorFactory.getNodeServiceInterceptorPermissions()) {
                    if(nodeServiceInterceptorPermission.hasPermission(nodeId,(String) data)){
                        return true;
                    }
                }

                if (!CCConstants.getUsagePermissions().contains((String) data)) {
                    return false;
                }
                return NodeServiceInterceptor.handleInvocation(nodeId, invocation, false);
            } else if (methodName.equals("hasAllPermissions")) {
                Map<String, Boolean> result = (Map<String, Boolean>) invocation.proceed();
                // to improve performance, if node seems to have any valid permissions, return true
                if (result.values().stream().anyMatch((v) -> v)) {
                    return result;
                }
                // fetch all permissions but only allow the onces that are allowed for usages
                result = (Map<String, Boolean>) NodeServiceInterceptor.handleInvocation(nodeId, invocation, false);
                return new HashMap<>(result.entrySet().stream().map((e) -> {
                    e.setValue(e.getValue() && CCConstants.getUsagePermissions().contains(e.getKey()));
                    return e;
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }
        } else if (methodName.equals("getPermissionsForAuthority")) {
            String nodeId = (String) invocation.getArguments()[0];
            String authority = (String) invocation.getArguments()[1];
            // do only intercEept in case the current authority is requested
            if (AuthenticationUtil.getFullyAuthenticatedUser().equals(authority)) {
                List<String> result = (List<String>) invocation.proceed();
                if (!result.isEmpty()) {
                    return result;
                }
                if (NodeServiceInterceptor.hasReadAccess(nodeId)) {
                    // return all valid usage permissions because indirect access is available
                    return new ArrayList<>(CCConstants.getUsagePermissions());
                }
                return result.stream().filter(e -> CCConstants.getUsagePermissions().contains(e)).collect(Collectors.toList());
            }
        }
        return invocation.proceed();
    }
}
