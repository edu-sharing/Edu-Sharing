package org.edu_sharing.alfresco.workspace_administration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class NodeServiceInterceptor implements MethodInterceptor {

	transient private DictionaryService dictionaryService;

	transient private NodeService nodeService;

	transient private AuthorityService authorityService;

	boolean debugMode = false;

	static Logger logger = Logger.getLogger(NodeServiceInterceptor.class);

	/**
	 * thread local to be independent of user session (oauth, jsession) allow to use
	 * scope information i.e. in runas of alfresco
	 */
	static ThreadLocal<String> eduSharingScope = new ThreadLocal<String>();
	
	static List<QName> toFilter = Arrays.asList(new QName[]{QName.createQName(CCConstants.CM_TYPE_FOLDER),QName.createQName(CCConstants.CM_TYPE_CONTENT),
			QName.createQName(CCConstants.CCM_TYPE_MAP),QName.createQName(CCConstants.CCM_TYPE_IO),
			QName.createQName(CCConstants.CM_TYPE_THUMBNAIL)});

	public void init() {
		PropertyCheck.mandatory(this, "dictionaryService", dictionaryService);
		PropertyCheck.mandatory(this, "nodeService", nodeService);
	}
	
	public static boolean filterNodeType(NodeService nodeService,NodeRef nodeRef) {
		QName type = nodeService.getType(nodeRef);
		
		//only filter specific type
		return toFilter.contains(type);
	}
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		String methodName = invocation.getMethod().getName();

		String runAsUser = AuthenticationUtil.getRunAsUser();

		if (runAsUser != null && runAsUser.equals(AuthenticationUtil.SYSTEM_USER_NAME)) {
			return invocation.proceed();
		}
		
		if (runAsUser != null && runAsUser.equals(ApplicationInfoList.getHomeRepository().getUsername())) {
			return invocation.proceed();
		}

		if (methodName.equals("getChildAssocs")) {

			String currentScope = NodeServiceInterceptor.eduSharingScope.get();
			logger.info("runAsUser:" + runAsUser + " currentScope:" + currentScope);
			List<ChildAssociationRef> childAssocs = (List<ChildAssociationRef>) invocation.proceed();
			List<ChildAssociationRef> childAssocsResult = new ArrayList<>();
			for (ChildAssociationRef childRef : childAssocs) {
				NodeRef nodeRef = childRef.getChildRef();
				
				//only filter specific type
				if(!filterNodeType(nodeService,nodeRef)) {
					childAssocsResult.add(childRef);
					continue;
				}

				String scope = (String) nodeService.getProperty(nodeRef,
						QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME));
				logger.info("child scope:" + scope + " id:" + nodeRef.getId());
				if (scope != null && !scope.equals("")) {
					// System.out.println("getChildAssocs currentScope:"+currentScope);
					if (scope.equals(currentScope)) {
						childAssocsResult.add(childRef);
					} else {
						logger.debug("filtered node:" + childRef.getChildRef().getId());
					}
				}
				// don't show items which are not scoped but the user is inside a scope
				else if (currentScope == null) {
					childAssocsResult.add(childRef);
				}else {
					logger.debug("filtered node:" + childRef.getChildRef().getId() + " currentScope:"+currentScope);
				}
			}
			return childAssocsResult;
		}

		return invocation.proceed();
	}
	public static void throwIfWrongScope(NodeService nodeService, NodeRef node) throws Throwable {
		
		String runAsUser = AuthenticationUtil.getRunAsUser();
		if (runAsUser != null && runAsUser.equals(AuthenticationUtil.SYSTEM_USER_NAME)) {
			return;
		}
		if (runAsUser != null && runAsUser.equals(ApplicationInfoList.getHomeRepository().getUsername())) {
			return;
		}
		
		if(!filterNodeType(nodeService,node))
			return;
		
		String scope=(String)nodeService.getProperty(node, QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME));
		String currentScope = NodeServiceInterceptor.eduSharingScope.get();
		
		logger.debug("scope:" + scope +" currentScope:" + currentScope);
		boolean scopeIsWrong=currentScope==null && scope!=null || currentScope!=null && scope==null || currentScope!=null && !currentScope.equals(scope);
		if(scopeIsWrong) {
			String info="Trying to fetch properties or content of a node with an other scope than the current user scope";
			Logger.getLogger(NodeServiceInterceptor.class).warn(info);
			throw new RuntimeException(info);
		}
	}

	public void setDictionaryService(DictionaryService dictionaryService) {
		this.dictionaryService = dictionaryService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public static void setEduSharingScope(String eduSharingScope) {
		NodeServiceInterceptor.eduSharingScope.set(eduSharingScope);
	}

	public static String getEduSharingScope() {
		return NodeServiceInterceptor.eduSharingScope.get();
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}
}
