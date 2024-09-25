package org.edu_sharing.service.nodeservice;

public interface NodeServiceInterceptorPermissions {
    public boolean accessable(String nodeId, int recursionDepth);

    public default boolean hasPermission(String nodeId, String permission){
        return false;
    }
}
