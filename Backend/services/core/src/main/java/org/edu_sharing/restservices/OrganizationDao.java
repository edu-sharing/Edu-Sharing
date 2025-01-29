package org.edu_sharing.restservices;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.restservices.organization.v1.model.OrganizationUserDeprovisioning;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.lifecycle.PersonDeleteOptions;
import org.edu_sharing.service.lifecycle.PersonLifecycleService;
import org.edu_sharing.service.organization.OrganizationService;
import org.edu_sharing.service.organization.OrganizationServiceFactory;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class OrganizationDao {

	private static final Logger log = LoggerFactory.getLogger(OrganizationDao.class);

	public static List<EduGroup> getOrganizations(RepositoryDao repoDao) throws DAOException {
		try{
			return SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId()).searchOrganizations("", 0, Integer.MAX_VALUE, null,false,false).getData();
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	public static String create(RepositoryDao repoDao, String groupName, String folderId,String scope) throws DAOException {

		try {

			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();

			if (!repoDao.getBaseClient().isAdmin(currentUser)) {

				throw new AccessDeniedException(currentUser);
			}

			((MCAlfrescoAPIClient)repoDao.getBaseClient()).bindEduGroupFolder(groupName, folderId);
			return groupName;
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}

	}

	public static OrganizationDao create(RepositoryDao repoDao, String orgName,String scope) throws DAOException {
		GroupProfile profile=new GroupProfile();
		profile.setDisplayName(orgName);
		String authorityName=create(repoDao,orgName,profile,scope);
		return getInstant(repoDao, PermissionService.GROUP_PREFIX + authorityName);
	}
	/**
	 * returns Groupname
	 * @param repoDao
	 * @param orgName
	 * @param profile
	 * @return
	 * @throws DAOException
	 */
	public static String create(RepositoryDao repoDao, String orgName, GroupProfile profile,String scope) throws DAOException {
		try {
			OrganizationService organizationService = OrganizationServiceFactory.getOrganizationService(repoDao.getApplicationInfo().getAppId());
			return organizationService.createOrganization(orgName, profile.getDisplayName(),HttpContext.getCurrentMetadataSet(),scope);
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}
	public static OrganizationDao getInstant(RepositoryDao repoDao, String groupName) throws DAOException {

		try {
			return new OrganizationDao(repoDao,AuthorityServiceFactory.getAuthorityService(repoDao.getId()).getEduGroup(groupName));
		} catch (Throwable t) {
			if(t instanceof NullPointerException) {
				throw new DAOMissingException(t);
			}
			throw DAOException.mapping(t);
		}

	}
	public static OrganizationDao get(RepositoryDao repoDao, String groupName) throws DAOException {

		try {

			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();

			List<EduGroup> groups = getOrganizations(repoDao);
			for (EduGroup eduGroup : groups) {

				String eduGroupName = generateGroupName(eduGroup);

				if (! eduGroupName.equals(groupName)) {

					continue;
				}
				OrganizationDao org=new OrganizationDao(repoDao, eduGroup);

				return org;
			}

			throw new DAOMissingException(
					new IllegalArgumentException(groupName));

		} catch (Throwable e) {

			throw DAOException.mapping(e);
		}

	}

	private RepositoryDao repoDao;
	private final EduGroup eduGroup;

	private final String authorityName;
	private final String groupName;
	private org.alfresco.service.cmr.repository.NodeRef ref;

	public OrganizationDao(RepositoryDao repoDao, EduGroup eduGroup) {

		this.repoDao = repoDao;
		this.eduGroup = eduGroup;

		this.authorityName = generateAuthorityName(eduGroup);
		this.groupName = generateGroupName(eduGroup);
		this.ref = AuthorityServiceFactory.getAuthorityService(repoDao.getId()).getAuthorityNodeRef(this.authorityName);
	}

	public static List<Organization> mapOrganizations(List<EduGroup> parentOrganizations) {
		if(parentOrganizations != null && parentOrganizations.size() > 0){
			return AuthenticationUtil.runAsSystem(() -> parentOrganizations.stream().map((org) -> {
				try {
					return OrganizationDao.getInstant(RepositoryDao.getHomeRepository(), org.getGroupname()).asOrganization();
				} catch (DAOException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList()));
		}
		return null;
	}

	/**
	 * returns true if the user is allowed to administer this org
	 * @return
	 */
	public boolean hasAdministrationAccess(){
		return AuthorityServiceFactory.getAuthorityService(repoDao.getId()).hasAdminAccessToOrganization(groupName);
	}

	public Organization asOrganization() {

		Organization data = new Organization();

		data.setRef(getRef());
		data.setAuthorityName(authorityName);
		data.setAuthorityType(Authority.Type.GROUP);
		data.setGroupName(groupName);
		data.setAdministrationAccess(hasAdministrationAccess());

		try {
			Group group = GroupDao.getGroup(repoDao, authorityName).asGroup();
			data.setSignupMethod(group.getSignupMethod());
			data.setProfile(group.getProfile());
		}catch(Throwable t){
			throw new RuntimeException("Error getting profile for organization "+authorityName,t);
		}

		NodeRef ref = new NodeRef();
		ref.setRepo(repoDao.getId());
		ref.setId(eduGroup.getFolderId());
		data.setSharedFolder(ref);

		return data;
	}

	private NodeRef getRef() {
		return NodeDao.createNodeRef(repoDao, this.ref.getId());
	}

	public void delete() throws DAOException {

		try {

			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();

			if (!repoDao.getBaseClient().isAdmin(currentUser)) {

				throw new AccessDeniedException(currentUser);
			}

			OrganisationService organisationService = (OrganisationService) ApplicationContextFactory.getApplicationContext().getBean("organisationService");
			organisationService.unbindEduGroupFolder(groupName,eduGroup.getFolderId());

		} catch (Exception e) {

			throw DAOException.mapping(e);
		}

	}

	private static String generateAuthorityName(EduGroup eduGroup) {

		String groupName = eduGroup.getGroupname();

		return    groupName.startsWith(PermissionService.GROUP_PREFIX)
				? groupName
				: PermissionService.GROUP_PREFIX + groupName;

	}

	private static String generateGroupName(EduGroup eduGroup) {

		String groupName = eduGroup.getGroupname();

		return    groupName.startsWith(PermissionService.GROUP_PREFIX)
				? groupName.substring(PermissionService.GROUP_PREFIX.length())
				: groupName;

	}
	public void removeMember(String member, OrganizationUserDeprovisioning deprovisioning) throws DAOException{
		if(!hasAdministrationAccess())
			throw new DAOSecurityException(new Throwable("no administration access for org "+groupName));
		if(deprovisioning != null && deprovisioning.getMode() == OrganizationUserDeprovisioning.Mode.assign) {
			if(!AuthorityServiceHelper.isAdmin()){
				throw new NotAnAdminException();
			}
		}
		AuthenticationUtil.runAsSystem((RunAsWork<Void>) () -> {
            // will throw if member is invalid user
            repoDao.getBaseClient().getUserInfo(member).get(CCConstants.CM_PROP_PERSON_USERNAME);

            if(deprovisioning != null) {
                if(deprovisioning.getMode() == OrganizationUserDeprovisioning.Mode.assign) {
                    if(StringUtils.isBlank(deprovisioning.getReceiver())) {
                        throw new RuntimeException("invalid parameters. Missing receiver for mode == assign");
                    }
                    Map<String, Object> filter = new HashMap<>() {{
						put("cmis:createdBy", member);
                    }};
                    List<org.alfresco.service.cmr.repository.NodeRef> refs = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO, filter);
                    refs.addAll(CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_MAP, filter));
                    refs = CMISSearchHelper.filterCMISResult(refs, new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, eduGroup.getFolderId()));
                    log.info("Deprovisioning " + member + " from " + authorityName + ": " + refs.size() + " objects to process");
                    PersonLifecycleService pls = new PersonLifecycleService();
                    PersonDeleteOptions options = new PersonDeleteOptions();
                    options.cleanupMetadata = false;
                    options.receiver = deprovisioning.getReceiver();
                    pls.setOwnerAndPermissions(refs, member, options);
                }
            }

            removeMember(groupName, member);

            return null;
        });
	}

	private String getAdminGroup() {
		return PermissionService.GROUP_PREFIX + org.edu_sharing.service.authority.AuthorityService.getGroupName(
				org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP, authorityName
		);
	}

	private void removeMember(String groupName,String authorityName) throws DAOException {
		try {
			String[] members = repoDao.getAuthorityService().getMembershipsOfGroup(groupName);
			for (String auth : members) {
				if (auth.startsWith(PermissionService.GROUP_PREFIX)) {
					removeMember(auth.substring(PermissionService.GROUP_PREFIX.length()), authorityName);
				} else if (auth.equals(authorityName)) {
					repoDao.getAuthorityService().removeMemberships(groupName, new String[]{authorityName});
				}
			}
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}

}
