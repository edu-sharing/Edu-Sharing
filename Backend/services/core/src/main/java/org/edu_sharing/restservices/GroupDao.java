package org.edu_sharing.restservices;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.alfresco.tools.EduSharingNodeHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.cache.PersonCache;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.restservices.iam.v1.model.GroupSignupResult;
import org.edu_sharing.restservices.organization.v1.model.GroupSignupDetails;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.GroupProfile;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.notification.NotificationServiceFactoryUtility;
import org.edu_sharing.service.organization.GroupSignupMethod;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.springframework.context.ApplicationContext;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;

import java.util.*;
import java.util.stream.Collectors;

public class GroupDao {

    static Logger logger = Logger.getLogger(GroupDao.class);
    private final Map<String, Object> properties;
    private final String[] aspects;
    private final ArrayList<EduGroup> parentOrganizations;
    private String userEmail;
    private NotificationService notificationService;

    public static GroupDao getGroup(RepositoryDao repoDao, String groupName) throws DAOException {

        try {

            return new GroupDao(repoDao, groupName);

        } catch (Exception e) {

            throw DAOException.mapping(e);
        }
    }

    public static GroupDao createGroup(RepositoryDao repoDao, String groupName, GroupProfile profile, String parentGroup) throws DAOException {
        try {
            AuthorityService authorityService = AuthorityServiceFactory.getAuthorityService(repoDao.getApplicationInfo().getAppId());
            String result = authorityService.createGroup(groupName, profile.getDisplayName(), parentGroup);
            GroupDao groupDao = GroupDao.getGroup(repoDao, result);
            if (result != null) {
                // permission check was done already, so run as system to allow org admin to set properties
                AuthenticationUtil.runAsSystem(() -> {
                    groupDao.setGroupEmail(profile);
                    groupDao.setGroupType(profile);
                    groupDao.setScopeType(profile);
                    return null;
                });
            }
            // reload data after it was changed
            return GroupDao.getGroup(repoDao, result);
        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    public static List<GroupDao> search(RepositoryDao repoDao, String pattern) throws DAOException {

        try {

            List<GroupDao> resultset = new ArrayList<>();
            for (String groupName : ((MCAlfrescoAPIClient) repoDao.getBaseClient()).searchGroupNames(pattern)) {

                resultset.add(new GroupDao(repoDao, groupName));
            }

            return resultset;

        } catch (Exception e) {

            throw DAOException.mapping(e);
        }
    }

    private final MCAlfrescoBaseClient baseClient;

    private final RepositoryDao repoDao;

    private final String authorityName;
    private final String groupName;
    private final String displayName;

    private AuthorityService authorityService;
    private SearchService searchService;

    private String groupType;
    private String scopeType;

    private String groupEmail;

    private NodeRef ref;

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    org.edu_sharing.alfresco.service.OrganisationService eduOrganisationService = (org.edu_sharing.alfresco.service.OrganisationService) applicationContext
            .getBean("eduOrganisationService");

    public GroupDao(RepositoryDao repoDao, String groupName) throws DAOException {

        try {

            this.baseClient = repoDao.getBaseClient();
            this.authorityService = AuthorityServiceFactory.getAuthorityService(repoDao.getApplicationInfo().getAppId());
            this.searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
            this.notificationService = NotificationServiceFactoryUtility.getLocalService();
            this.repoDao = repoDao;

            this.authorityName =
                    groupName.startsWith(PermissionService.GROUP_PREFIX)
                            ? groupName
                            : PermissionService.GROUP_PREFIX + groupName;

            this.groupName =
                    groupName.startsWith(PermissionService.GROUP_PREFIX)
                            ? groupName.substring(PermissionService.GROUP_PREFIX.length())
                            : groupName;

            this.displayName = ((MCAlfrescoAPIClient) baseClient).getGroupDisplayName(this.groupName);
            if (displayName == null) {

                throw new DAOMissingException(
                        new IllegalArgumentException("Group does not exist: " + groupName));

            }
            this.ref = authorityService.getAuthorityNodeRef(this.authorityName);
            this.properties = NodeServiceHelper.getProperties(ref);
            this.aspects = NodeServiceHelper.getAspects(ref);
            this.groupType = (String) properties.get(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);
            this.scopeType = (String) properties.get(CCConstants.CCM_PROP_SCOPE_TYPE);
            this.groupEmail = (String) properties.get(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPEMAIL);


            // may causes performance penalties!
            this.parentOrganizations = AuthenticationUtil.runAsSystem(() ->
                    authorityService.getEduGroups(this.authorityName, NodeServiceInterceptor.getEduSharingScope())
            );
        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }
    }

    public void changeProfile(GroupProfile profile) throws DAOException {

        try {
            checkModifyAccess();

            AuthenticationUtil.RunAsWork<Void> runAs = new RunAsWork<Void>() {
                @Override
                public Void doWork() throws Exception {
                    ((MCAlfrescoAPIClient) repoDao.getBaseClient()).createOrUpdateGroup(groupName, profile.getDisplayName());
                    setGroupType(profile);
                    setGroupEmail(profile);
                    setScopeType(profile);

                    // rename admin group
                    renameSubGroup(profile, org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP, org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX);
                    renameSubGroup(profile, org.edu_sharing.alfresco.service.AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP, org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX);
                    renameOrganisationFolder();
                    return null;
                }
            };
            try {
                AuthenticationUtil.runAsSystem(runAs);
            } catch (DuplicateChildNodeNameException e) {
                String displayName = EduSharingNodeHelper.makeUniqueName(profile.getDisplayName());
                profile.setDisplayName(displayName);
                AuthenticationUtil.runAsSystem(runAs);
            }

        } catch (Throwable t) {

            throw DAOException.mapping(t);
        }

    }

    private void renameOrganisationFolder() {
        this.eduOrganisationService.syncOrganisationFolder(this.authorityName);
    }

    private void renameSubGroup(GroupProfile profile, String subgroup, String postfix) {
        String authorityName = PermissionService.GROUP_PREFIX + org.edu_sharing.alfresco.service.AuthorityService.getGroupName(
                subgroup, groupName);
        if (authorityService.authorityExists(authorityName)) {
            String newDisplayName = profile.getDisplayName() + postfix;
            authorityService.setAuthorityProperty(authorityName,
                    CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME,
                    newDisplayName);
        }
    }

    protected void setGroupType(GroupProfile profile) {
        if (profile.getGroupType() != null) {
            authorityService.addAuthorityAspect(PermissionService.GROUP_PREFIX + groupName, CCConstants.CCM_ASPECT_GROUPEXTENSION);
        }
        authorityService.setAuthorityProperty(PermissionService.GROUP_PREFIX + groupName, CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE, profile.getGroupType());
    }

    protected void setGroupEmail(GroupProfile profile) {
        authorityService.setAuthorityProperty(PermissionService.GROUP_PREFIX + groupName, CCConstants.CCM_PROP_GROUPEXTENSION_GROUPEMAIL, profile.getGroupEmail());

    }

    protected void setScopeType(GroupProfile profile) {
        if (profile.getScopeType() != null) {
            authorityService.addAuthorityAspect(PermissionService.GROUP_PREFIX + groupName, CCConstants.CCM_ASPECT_SCOPE);
        }
        authorityService.setAuthorityProperty(PermissionService.GROUP_PREFIX + groupName, CCConstants.CCM_PROP_SCOPE_TYPE, profile.getScopeType());
    }

    public void delete() throws DAOException {

        try {
            checkModifyAccess();
            AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
                @Override
                public Void doWork() throws Exception {
                    authorityService.deleteAuthority(PermissionService.GROUP_PREFIX + groupName);
                    return null;
                }
            });

        } catch (Exception e) {

            throw DAOException.mapping(e);
        }
    }

    public void addMember(String member) throws DAOException {

        try {
            checkAdminAccess();
            AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

                @Override
                public Void doWork() throws Exception {
                    authorityService.addMemberships(groupName, new String[]{member});
                    PersonCache.reset(member);
                    return null;
                }
            });

        } catch (Exception e) {

            throw DAOException.mapping(e);
        }
    }

    public void checkAdminAccess() {
        if (!authorityService.hasAdminAccessToGroup(groupName)) {
            throw new AccessDeniedException("User does not have permissions to manage this group");
        }
    }

    public void checkModifyAccess() {
        if (!authorityService.hasModifyAccessToGroup(groupName)) {
            throw new AccessDeniedException("User does not have permissions to modify this group");
        }
    }

    public void deleteMember(String member) throws DAOException {

        try {
            checkAdminAccess();
            AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

                @Override
                public Void doWork() throws Exception {
                    authorityService.removeMemberships(groupName, new String[]{member});
                    PersonCache.reset(member);
                    return null;
                }
            });

        } catch (Exception e) {

            throw DAOException.mapping(e);
        }
    }
    public Group asGroup() {
		return asGroup(true);
	}
	public Group asGroup(boolean resolveOrganizations) {

        Group data = new Group();

        data.setRef(getRef());
        data.setAuthorityName(getAuthorityName());
        data.setAuthorityType(Authority.Type.GROUP);
		if(resolveOrganizations) {
        data.setOrganizations(OrganizationDao.mapOrganizations(parentOrganizations));
		}

        data.setGroupName(getGroupName());

        GroupProfile profile = new GroupProfile();
        profile.setDisplayName(getDisplayName());
        profile.setGroupType(getGroupType());
        profile.setScopeType(getScopeType());
        profile.setGroupEmail(getGoupEmail());
        data.setProfile(profile);
        data.setProperties(getProperties());
        data.setAspects(getAspects());
        data.setSignupMethod(getSignupMethod(ref));

        return data;
    }

    public static GroupSignupMethod getSignupMethod(NodeRef ref) {
        String method = NodeServiceHelper.getProperty(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_METHOD);
        if (method == null) {
            return null;
        }
        return GroupSignupMethod.valueOf(method);
    }

    private List<String> getAspects() {
        return Arrays.stream(aspects).map((a) -> CCConstants.getValidLocalName(a)).collect(Collectors.toList());
    }

    private Map<String, String[]> getProperties() {
        return NodeServiceHelper.getPropertiesMultivalue(
                NodeServiceHelper.transformLongToShortProperties(properties)
        );
    }

    private String getScopeType() {
        return this.scopeType;
    }

    public String getGroupType() {
        return this.groupType;
    }

    private String getGoupEmail() {
        return this.groupEmail;
    }

    public org.edu_sharing.restservices.shared.NodeRef getRef() {

        return NodeDao.createNodeRef(repoDao, this.ref.getId());
    }

    public String getAuthorityName() {

        return this.authorityName;
    }

    public String getGroupName() {

        return this.groupName;
    }

    public String getDisplayName() {

        return this.displayName;
    }

    public GroupDao getSubgroupByType(String type) throws DAOException {
        String authority = authorityService.getSubgroupByType(authorityName, type);
        if (authority != null) {
            return new GroupDao(repoDao, authority);
        }
        throw new IllegalArgumentException("Group does not contain sub group of type " + type);
    }

    public void setSignup(GroupSignupDetails details) {
        checkAdminAccess();
        AuthenticationUtil.runAsSystem(() -> {
            if (details.getSignupMethod() == null) {
                NodeServiceHelper.removeProperty(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_METHOD);
                NodeServiceHelper.removeAspect(ref, CCConstants.CCM_ASPECT_GROUP_SIGNUP);
            } else {
                if (authorityName.equals(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)) {
                    throw new DAOSecurityException(new AccessDeniedException("not allowed for group " + authorityName));
                }
                NodeServiceHelper.addAspect(ref, CCConstants.CCM_ASPECT_GROUP_SIGNUP);
                NodeServiceHelper.setProperty(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_METHOD, details.getSignupMethod().toString(), false);
                if (details.getSignupPassword() != null && !details.getSignupPassword().isEmpty()) {
                    NodeServiceHelper.setProperty(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_PASSWORD,
                            DigestUtils.sha1Hex(details.getSignupPassword()),
                            false);
                }
            }
            return null;
        });
    }

    public GroupSignupResult signupUser(String password) throws DAOException {
        try {
            ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_SIGNUP_GROUP);
            return AuthenticationUtil.runAsSystem(() -> {
                GroupSignupMethod method = getSignupMethod(ref);
                boolean addMember = false;
                NodeRef userRef = authorityService.getAuthorityNodeRef(AuthenticationUtil.getFullyAuthenticatedUser());

                if (GroupSignupMethod.simple.equals(method)) {
                    addMember = true;
                } else if (GroupSignupMethod.password.equals(method)) {
                    if (!NodeServiceHelper.getProperty(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_PASSWORD).equals(DigestUtils.sha1Hex(password))) {
                        return GroupSignupResult.InvalidPassword;
                    }
                    addMember = true;
                } else if (GroupSignupMethod.list.equals(method)) {
                    ArrayList<NodeRef> list = (ArrayList<NodeRef>) NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_LIST);
                    if (list == null)
                        list = new ArrayList<>();
                    if (list.contains(userRef)) {
                        return GroupSignupResult.AlreadyInList;
                    }
                    list.add(userRef);
                    NodeServiceHelper.setProperty(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_LIST, list, false);

                    if (StringUtils.isNotBlank(groupEmail)) {
                        notifyGroupSignup(getDisplayName(), userRef, groupEmail, "groupSignupList");
                    }

                } else {
                    throw new IllegalArgumentException("The group " + authorityName + " is not allowed for signup");
                }

                if (addMember) {
                    if (authorityService.getMemberships(AuthenticationUtil.getFullyAuthenticatedUser()).contains(authorityName)) {
                        return GroupSignupResult.AlreadyMember;
                    }
                    addMember(AuthenticationUtil.getFullyAuthenticatedUser());
                    userEmail = NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_EMAIL);
                    if (StringUtils.isNotBlank(userEmail)) {
                        notifyGroupSignup(getDisplayName(), userRef, userEmail, "groupSignupUser");
                    }
                    if (StringUtils.isNotBlank(groupEmail)) {
                        notifyGroupSignup(getDisplayName(), userRef, groupEmail, "groupSignupAdmin");
                    }
                }
                return GroupSignupResult.Ok;
            });
        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    public List<PersonDao> signupUserList() throws DAOException {
        try {
            checkAdminAccess();
            if (!NodeServiceHelper.getProperty(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_METHOD).equals(GroupSignupMethod.list.toString())) {
                throw new IllegalArgumentException("Group " + groupName + " is not configured with the proper signup mode");
            }
            ArrayList<NodeRef> userRefs =
                    (ArrayList<NodeRef>) NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_LIST);
            if (userRefs == null) {
                return new ArrayList<>();
            }
            return userRefs.stream().map((userRef) -> {
                        try {
                            return PersonDao.getPerson(repoDao, NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_USERNAME));
                        } catch (DAOException e) {
                            logger.info("Could not handle person ref: " + userRef, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw DAOException.mapping(e);
        }
    }

    private void handleSignup(String user, boolean add) {
        checkAdminAccess();
        AuthenticationUtil.runAsSystem(() -> {
            ArrayList<NodeRef> userRefs = (ArrayList<NodeRef>) NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_LIST);
            NodeRef userRef = userRefs.stream().filter((r) -> NodeServiceHelper.getProperty(r, CCConstants.CM_PROP_PERSON_USERNAME).equals(user))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("User " + user + " is not on the waiting list of " + groupName));

            // filter the user
            userRefs = userRefs.stream().
                    filter((r) -> !r.equals(userRef)).
                    collect(Collectors.toCollection(ArrayList::new));
            NodeServiceHelper.setProperty(ref, CCConstants.CCM_PROP_GROUP_SIGNUP_LIST, userRefs, false);
            if (add) {
                addMember(user);
            }

            String receiver = NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_EMAIL);
            if (StringUtils.isBlank(receiver)) {
                return null;
            }

            String template = "groupSignupConfirmed";
            if (!add) {
                template = "groupSignupRejected";
            }
            notifyGroupSignup(groupName, userRef, receiver, template);
            return null;
        });
    }

    public void confirmSignup(String user) throws DAOException {
        try {
            handleSignup(user, true);
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    public void rejectSignup(String user) throws DAOException {
        try {
            handleSignup(user, false);
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    private void notifyGroupSignup(String groupName, NodeRef userRef, String receiver, String messageType) throws Exception {
        Map<String, String> replace =  new HashMap<>();
        replace.put("group", groupName);
        replace.put("firstName", NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_FIRSTNAME));
        replace.put("lastName", NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_LASTNAME));
        MailTemplate.sendMail(receiver, messageType, replace);
    }

}
