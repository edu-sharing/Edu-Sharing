package org.edu_sharing.service.nodeservice;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.provider.ElasticSearchProvider;
import org.edu_sharing.service.provider.Provider;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.stream.StreamServiceFactory;
import org.edu_sharing.service.stream.StreamServiceHelper;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class NodeServiceInterceptor implements MethodInterceptor {
    static ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    static ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    static Logger logger = Logger.getLogger(NodeServiceInterceptor.class);

    List<NodeServiceInterceptorPermissions> customizations;

    public void init(){
        
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName=invocation.getMethod().getName();
        int argumentId=-1;
        if(methodName.equals("getProperty") ||
                methodName.equals("getContent") ||
                methodName.equals("getContentHash") ||
                methodName.equals("getContentMimetype") ||
                methodName.equals("getPreview") ||
                methodName.equals("getProperties") ||
                methodName.equals("getVersion") ||
                methodName.equals("getAspects") ||
                methodName.equals("hasAspect") ||
                methodName.equals("getOwner")) {
            argumentId=2;
        }
        if (methodName.equals("getChild")) {
            argumentId=1;
        }
        if(methodName.equals("getChildrenChildAssociationRefAssoc") ||
                methodName.equals("getChildrenChildAssociationRefType") ||
                methodName.equals("getType") ||
                methodName.equals("getPublishedCopies") ||
                methodName.equals("getOriginalNode") ||
                methodName.equals("getPrimaryParent") ||
                methodName.equals("getVersionHistory")){
            argumentId=0;
        }
        if(methodName.equals("writeContent")){
            return checkIgnoreQuota(invocation);
        }
        if(argumentId==-1)
            return invocation.proceed();
        String nodeId = (String) invocation.getArguments()[argumentId];
        if(Arrays.asList("getProperty", "getProperties").contains(methodName)) {
            checkReadMetadataPermissions(nodeId, invocation);
        }
        return handleInvocation(nodeId, invocation,true);
    }

    private void checkReadMetadataPermissions(String nodeId, MethodInvocation invocation) {
        try {
            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
            if(serviceRegistry.getNodeService().hasAspect(nodeRef, QName.createQName(
                    CCConstants.CCM_ASPECT_REMOTEREPOSITORY
            ))) {
                String remoteId = (String) serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(
                        CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID
                ));
                if(remoteId != null) {
                    String tpId = CCConstants.CCM_VALUE_TOOLPERMISSION_REPOSITORY_PREFIX + remoteId;
                    ToolPermissionHelper.throwIfToolpermissionMissing(tpId);
                }
            }
        } catch(ToolPermissionException e) {
            throw e;
        } catch(Throwable t) {
            logger.info("Unexpected error while verifying if object is remote object: " + t.getMessage());
        }
    }

    /**
     * When onlyOnError is set, make sure that the called method will not cause any side effects since it may be called twice
     * @param nodeId
     * @param invocation
     * @param onlyOnError
     * @return
     * @throws Throwable
     */
    public static Object handleInvocation(String nodeId, MethodInvocation invocation, boolean onlyOnError) throws Throwable {
        if(onlyOnError){
            try{
                return invocation.proceed();
            }
            catch(AccessDeniedException|InsufficientPermissionException|AuthenticationCredentialsNotFoundException t)
            {
                // catch exception, check
                logger.debug("Method threw "+t.getMessage()+" for node "+nodeId+", will check signature");
                return runAsSystem(nodeId,invocation);
            }
        }
        else{
            return runAsSystem(nodeId,invocation);
        }


    }

    /**
     * checks if the quota must be ignored (basically if the request was initiated by a connector)
     * and run the task accordingly
     */
    private static Object checkIgnoreQuota(MethodInvocation invocation) throws Throwable {
        if(ContextManagementFilter.accessTool.get() != null &&
                ApplicationInfo.TYPE_CONNECTOR.equals(ContextManagementFilter.accessTool.get().getApplicationInfo().getType())
        ){
            return ignoreQuota(() -> {
                try {
                    return invocation.proceed();
                } catch (Throwable t) {
                    logger.error(t.getMessage(),t);
                    return null;
                }
            });
        }
        return invocation.proceed();
    }

    /**
     * Ignores any user quotas for the given callback context
     * Basically, this temporarily disables the quota for the user
     */
    public static <T> T ignoreQuota(Callable<T> callable) throws Exception {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        RetryingTransactionHelper rth = serviceRegistry.getTransactionService().getRetryingTransactionHelper();

        return rth.doInTransaction(() -> {
            Serializable quota= (Serializable) AuthorityServiceFactory.getLocalService().getAuthorityProperty(AuthenticationUtil.getFullyAuthenticatedUser(),CCConstants.CM_PROP_PERSON_SIZE_QUOTA);
            AuthenticationUtil.runAsSystem(()-> {
                AuthorityServiceFactory.getLocalService().setAuthorityProperty(AuthenticationUtil.getFullyAuthenticatedUser(), CCConstants.CM_PROP_PERSON_SIZE_QUOTA, null);
                return null;
            });

            T result=callable.call();

            AuthenticationUtil.runAsSystem(()-> {
                AuthorityServiceFactory.getLocalService().setAuthorityProperty(AuthenticationUtil.getFullyAuthenticatedUser(), CCConstants.CM_PROP_PERSON_SIZE_QUOTA, quota);
                return null;
            });
            return result;
        });
    }

    /**
     * returns true if the current user has access on the given node via usages, collections or other indirect permissions
     */
    public static boolean hasReadAccess(String nodeId) {
        int i = 0;
        while (nodeId!=null) {
            if (
                    hasPermissions(nodeId, i)
            ) {
                return true;
            }
            // only one parent at the moment
            if(i++ >= 1) {
                break;
            }
            nodeId = fetchParentId(nodeId);
        }
        return false;
    }

    private static Object runAsSystem(String nodeId,MethodInvocation invocation) throws Throwable {
        int i = 0;
        while(nodeId!=null) {
            if (
                    hasPermissions(nodeId, i)
            ) {
                logger.debug("Node "+nodeId+" -> will run as system");
                return AuthenticationUtil.runAsSystem(() -> {
                    try {
                        return invocation.proceed();
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                });
            }

            // only one parent at the moment
            if(i++>=1) {
                break;
            }
            // we'll check if any of the nodes in the parent hierarchy may has an usage -> so it is allowed as well
            nodeId = fetchParentId(nodeId);
        }
        return invocation.proceed();
    }

    private static String fetchParentId(String nodeIdFinal) {
        return AuthenticationUtil.runAsSystem(() -> {
            try {
                return serviceRegistry.getNodeService().getPrimaryParent(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeIdFinal)).getParentRef().getId();
            } catch (Throwable t2) {
                return null;
            }
        });
    }

    private static boolean hasPermissions(String nodeId, int recursionDepth) {
        return (hasSignature(nodeId) || hasUsage(nodeId)) ||
                // direct permissions only valid for current node, NOT for parent!
                (accessibleViaStream(nodeId) || accessableViaCustomization(nodeId, recursionDepth) || hasCollectionPermissions(nodeId) && recursionDepth == 0);
    }

    private static boolean accessableViaCustomization(String nodeId, int recursionDepth) {
        for (NodeServiceInterceptorPermissions customization : PropertiesInterceptorFactory.getNodeServiceInterceptorPermissions()){
            if (customization.accessable(nodeId, recursionDepth)) return true;
        }
        return false;
    }

    private static boolean accessibleViaStream(String nodeId) {
        try {
            return StreamServiceHelper.canCurrentAuthorityAccessNode(StreamServiceFactory.getStreamService(), nodeId);
        }catch(Throwable t){
            logger.warn(t.getMessage());
        }
        return false;
    }

    private static boolean hasSignature(String nodeId) {
        if(Context.getCurrentInstance()==null)
            return false;
        String authSingleUseNodeId = Context.getCurrentInstance().getSessionAttribute(CCConstants.AUTH_SINGLE_USE_NODEID);
        if(authSingleUseNodeId==null)
            return false;
        return authSingleUseNodeId.equals(nodeId);
    }

    private static boolean hasUsage(String nodeId) {
        /*List<ChildAssociationRef> usages = nodeService.getChildAssocs(nodeRef, Collections.singleton(QName.createQName(CCConstants.CCM_ASSOC_USAGEASPECT_USAGES)));
        for(ChildAssociationRef usage : usages){
            Map<QName, Serializable> props = nodeService.getProperties(usage.getChildRef());
        }
        return false;*/
        return false;
    }

    public static boolean hasCollectionPermissions(String nodeId){
        long test = System.currentTimeMillis();
        Provider providerByApp = ProviderHelper.getProviderByApp(ApplicationInfoList.getHomeRepository());
        if(!(providerByApp instanceof ElasticSearchProvider)){
            logger.debug("Skipping collection permission check cause no elastic provider present");
            return false;
        }
        if(
                !Arrays.asList(
                        CallSourceHelper.CallSource.Render, CallSourceHelper.CallSource.Preview,
                        CallSourceHelper.CallSource.Sitemap, CallSourceHelper.CallSource.ToolConnector,
                        CallSourceHelper.CallSource.RatingApi
                ).contains(CallSourceHelper.getCallSource())
        ){
            logger.debug("Skipping collection permission check for call source " + CallSourceHelper.getCallSource());
            return false;
        }
        boolean result = ((SearchServiceElastic)providerByApp.getSearchService()).isAllowedToRead(nodeId);
        logger.debug("collection permission check took:"+(System.currentTimeMillis() - test) +"ms");
        return result;
    }
}
