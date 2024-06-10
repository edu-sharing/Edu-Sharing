package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.*;

public class AssignedLicenseManagerJob  extends AbstractJob {
	
	Logger logger = Logger.getLogger(AssignedLicenseManagerJob.class);
	
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	SearchService searchService =serviceRegistry.getSearchService();
	NodeService nodeService = serviceRegistry.getNodeService();
	AuthorityService authorityService = serviceRegistry.getAuthorityService();
	PermissionService permissionService = serviceRegistry.getPermissionService();
	
	int fetchSize = 10;
	
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		try{
			ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
			authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());
			
			
			int startIdx = 0;
			manageExpiryDate(fetch(startIdx,fetchSize));
						
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}
	
	void manageExpiryDate(ResultSet rs){
		for(int i = 0; i < rs.length(); i++){
			NodeRef nodeRef = rs.getNodeRef(i);
			Date date = (Date)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE_EXPIRY));
			/**
			 * only handle assignedLicneses that got an expiry date
			 */
			if(date == null){
				continue;
			}
			
			
			String authority = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_AUTHORITY));
			ChildAssociationRef parentAssocRef = nodeService.getPrimaryParent(nodeRef);
			Set<AccessPermission> accessPermissions = permissionService.getAllSetPermissions(parentAssocRef.getParentRef());
			
			/**
			 * create authorities
			 */
			if(!authorityService.authorityExists(authority)){
				authorityService.createAuthority(AuthorityType.GROUP, authority.replace(AuthorityType.GROUP.getPrefixString(), ""));
			}
			
			/**
			 * set permissions for authority if no access
			 */
			boolean authorityCanRead = false;
			for(AccessPermission accessPermission : accessPermissions){
				if(accessPermission.getAuthority().equals(authority)){
					authorityCanRead = true;
				}
			}
			if(!authorityCanRead && date.getTime() > System.currentTimeMillis()){
				permissionService.setPermission(parentAssocRef.getParentRef(), authority, PermissionService.READ, true);
				permissionService.setPermission(parentAssocRef.getParentRef(), authority, CCConstants.PERMISSION_CC_PUBLISH, true);
			}
			
			/**
			 * remove expired
			 */
			if(date.getTime() < System.currentTimeMillis()){
				
			
				
				for(AccessPermission perm : accessPermissions){
					
					//logger.info("authority:" + authority + " perm:"+perm.getAuthority() +"" + perm.getPermission());
					
					if(perm.isInherited()) continue;
					if(perm.getAuthority().equals(authority)){
						logger.info("delete permission for " + parentAssocRef.getParentRef()+ " authority:"+perm.getAuthority() + " " + perm.getPermission());
						permissionService.deletePermission(parentAssocRef.getParentRef(), perm.getAuthority(), perm.getPermission());
					}
				}
				List<String> mediaCentres = (List<String>)nodeService.getProperty(parentAssocRef.getParentRef(),QName.createQName(CCConstants.CCM_PROP_IO_MEDIACENTER));
				ArrayList<String> newMediacentres = new ArrayList<>();
				if(mediaCentres != null){
					for(String mediaCentre : mediaCentres){
						if(!authority.replace(AuthorityType.GROUP.getPrefixString(), "").equals(mediaCentre)){
							newMediacentres.add(mediaCentre);
						}
					}
				}
				nodeService.setProperty(parentAssocRef.getParentRef(), QName.createQName(CCConstants.CCM_PROP_IO_MEDIACENTER), newMediacentres);
			}
			
			
		}
		
		if(rs.hasMore()){
			manageExpiryDate(fetch(rs.getStart() + fetchSize, fetchSize));
		}
	}
	
	ResultSet fetch(int skipCount, int maxItems){
		
		
		SearchParameters sp = new SearchParameters();
		sp.setQuery("TYPE:\"" + CCConstants.CCM_TYPE_ASSIGNED_LICENSE +"\"");
		sp.setMaxItems(maxItems);
		sp.addStore(MCAlfrescoAPIClient.storeRef);
		sp.setMaxItems(maxItems);
		sp.setSkipCount(skipCount);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.setBulkFetchEnabled(true);
		
		ResultSet rs = searchService.query(sp);
		
		return rs;
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		List<Class> allJobs = new ArrayList<>(Arrays.asList(super.allJobs));
		allJobs.add(AssignedLicenseManagerJob.class);
		return allJobs.toArray(new Class[0]);
	}
}
