package org.edu_sharing.service.mediacenter;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.AuthorityService;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.repository.server.importer.RecordHandlerInterfaceBase;
import org.edu_sharing.repository.server.jobs.helper.NodeHelper;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.edu_sharing.service.util.CSVTool;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class MediacenterServiceImpl implements MediacenterService {

    private PersistentHandlerEdusharing persistentHandlerEdusharing;
    Logger logger = Logger.getLogger(MediacenterServiceImpl.class);

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceregistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    org.alfresco.service.cmr.security.AuthorityService authorityService = serviceregistry.getAuthorityService();
    NodeService nodeService = serviceregistry.getNodeService();
    AuthorityService eduAuthorityService = (AuthorityService) applicationContext.getBean("eduAuthorityService");
    OrganisationService organisationService = (OrganisationService) applicationContext
            .getBean("eduOrganisationService");
    org.edu_sharing.service.authority.AuthorityService eduAuthorityService2 = AuthorityServiceFactory.getLocalService();
    SearchService searchService = serviceregistry.getSearchService();
    PermissionService permissionService = serviceregistry.getPermissionService();
    BehaviourFilter policyBehaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");

    public MediacenterServiceImpl() {

    }

    @NotNull
    public static String getAuthorityScope(String mediacenter) throws Exception {
        String authorityScope = MediacenterServiceFactory.getLocalService().getMediacenterProxyGroup(mediacenter);
        if (authorityScope == null) {
            throw new Exception("No mediacenter proxy group found.");
        }
        return authorityScope;
    }


    @Override
    public int importMediacenters(InputStream csv) {
        RunAsWork<Integer> runAs = new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                List<List<String>> records = new CSVTool().getRecords(csv, CSVTool.ENC_UTF8);

                int counter = 0;
                for (List<String> record : records) {

                    String mzId = record.get(0);
                    String mz = record.get(1);
                    String plz = record.get(2);
                    String ort = record.get(3);

                    try {

                        String authorityName = AuthorityService.MEDIA_CENTER_GROUP_TYPE + "_" + mzId;
                        logger.info("managing:" + authorityName);

                        if (authorityService.authorityExists("GROUP_" + authorityName)) {
                            logger.info("authority already exists:" + authorityName);
                            updateMediacenter("GROUP_" + authorityName, mz, plz, ort, null, null, null, true);
                            continue;
                        }

                        createMediacenter(mzId, mz, plz, ort);


                        counter++;
                    } catch (Exception e) {
                        logger.error("error in record: " + ((record == null || record.size() < 1) ? null : record.get(0)), e);
                        throw e;
                    }
                }
                return counter;
            }
        };

        return AuthenticationUtil.runAs(runAs, ApplicationInfoList.getHomeRepository().getUsername());
    }

    public void updateMediacenter(String authorityName, String displayName, String postalCode, String city,
                                  String districtAbbreviation, String mainUrl, String mediacenterCatalogs, boolean active) throws Exception {

        NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authorityName);
        String alfAuthorityName = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
        String currentDisplayName = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));

        if (displayName != null && !displayName.equals(currentDisplayName)) {
            authorityService.setAuthorityDisplayName(alfAuthorityName, displayName);
            String mcAdminGroup = getMediacenterAdminGroup(alfAuthorityName);
            if (mcAdminGroup != null) {
                authorityService.setAuthorityDisplayName(mcAdminGroup, displayName + AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX);
            }

            String mcProxyGroup = getMediacenterProxyGroup(alfAuthorityName);
            if (mcProxyGroup != null) {
                authorityService.setAuthorityDisplayName(mcProxyGroup, displayName + AuthorityService.MEDIA_CENTER_PROXY_DISPLAY_POSTFIX);
            }
        }

        updateProperty(authorityNodeRef, CCConstants.CCM_PROP_ADDRESS_POSTALCODE, postalCode);
        updateProperty(authorityNodeRef, CCConstants.CCM_PROP_ADDRESS_CITY, city);
        updateProperty(authorityNodeRef, CCConstants.CCM_PROP_MEDIACENTER_DISTRICT_ABBREVIATION, districtAbbreviation);
        updateProperty(authorityNodeRef, CCConstants.CCM_PROP_MEDIACENTER_MAIN_URL, mainUrl);
        updateProperty(authorityNodeRef, CCConstants.CCM_PROP_MEDIACENTER_CATALOGS, mediacenterCatalogs);

        if (active) {
            this.setActive(true, authorityName);
        } else {
            this.setActive(false, authorityName);
        }
    }

    private void updateProperty(NodeRef authorityNodeRef, String property, String newValue) {
        String oldValue = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(property));
        if (newValue != null && !newValue.equals(oldValue)) {
            nodeService.setProperty(authorityNodeRef, QName.createQName(property), newValue);
        }
    }

    public String createMediacenter(String id, String displayName, String postalCode, String city) throws Exception {

        String authorityName = AuthorityService.MEDIA_CENTER_GROUP_TYPE + "_" + id;

        /**
         * create mediacenter group
         */
        String alfAuthorityName = authorityService.createAuthority(AuthorityType.GROUP, authorityName);
        authorityService.setAuthorityDisplayName(alfAuthorityName, displayName);

        /**
         * create mediacenter admin group
         */
        createMediacenterAdminGroup(alfAuthorityName, displayName);

        /**
         * create mediacenter proxy group
         */
        createMediacenterProxyGroup(alfAuthorityName, displayName);

        /**
         * add mediacenter metadata
         */
        NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(alfAuthorityName);

        Map<QName, Serializable> groupExtProps = new HashMap<>();
        groupExtProps.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), AuthorityService.MEDIA_CENTER_GROUP_TYPE);
        nodeService.addAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_GROUPEXTENSION), groupExtProps);

        Map<QName, Serializable> groupAddressProps = new HashMap<>();
        if (postalCode != null)
            groupAddressProps.put(QName.createQName(CCConstants.CCM_PROP_ADDRESS_POSTALCODE), postalCode);
        if (city != null) groupAddressProps.put(QName.createQName(CCConstants.CCM_PROP_ADDRESS_CITY), city);
        nodeService.addAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_ADDRESS), groupAddressProps);

        Map<QName, Serializable> groupMZProps = new HashMap<>();
        groupMZProps.put(QName.createQName(CCConstants.CCM_PROP_MEDIACENTER_ID), id);
        if (city != null) groupMZProps.put(QName.createQName(CCConstants.CCM_PROP_ADDRESS_CITY), city);
        nodeService.addAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_MEDIACENTER), groupMZProps);

        return alfAuthorityName;
    }


    @Override
    public int importOrganisations(InputStream csv) {
        RunAsWork<Integer> runAs = new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                List<List<String>> records = new CSVTool().getRecords(csv, CSVTool.ENC_UTF8);

                int counter = 0;
                for (List<String> record : records) {
                    String schoolId = record.get(0);
                    String schoolName = record.get(1);
                    String plz = (record.size() > 2) ? record.get(2) : null;
                    String city = (record.size() > 3) ? record.get(3) : null;

                    try {
                        if (schoolId == null || schoolId.trim().length() == 0) {
                            logger.info("no schoolid provided:" + record);
                            continue;
                        }

                        if (authorityService.authorityExists("GROUP_ORG_" + schoolId)) {
                            logger.info("authority already exists:" + schoolId);
                            NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef("GROUP_ORG_" + schoolId);
                            String alfAuthorityName = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
                            String currentDisplayName = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
                            String currentCity = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_ADDRESS_CITY));
                            String currentPLZ = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_ADDRESS_POSTALCODE));

                            if (schoolName != null && !schoolName.equals(currentDisplayName)) {
                                authorityService.setAuthorityDisplayName(alfAuthorityName, schoolName);
                                String authorityNameOrgAdmin = organisationService.getOrganisationAdminGroup(alfAuthorityName);
                                if (authorityNameOrgAdmin != null) {
                                    authorityService.setAuthorityDisplayName(authorityNameOrgAdmin, schoolName + AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX);
                                }
                            }

                            if (city != null && !city.equals(currentCity)) {
                                nodeService.setProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_ADDRESS_CITY), currentCity);
                            }

                            if (plz != null && !plz.equals(currentPLZ)) {
                                nodeService.setProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_ADDRESS_POSTALCODE), plz);
                            }

                            continue;
                        }

                        logger.info("creating: " + schoolId + " " + schoolName);
                        String organisationName = organisationService.createOrganization(schoolId, schoolName);

                        String authorityName = PermissionService.GROUP_PREFIX + organisationName;

                        eduAuthorityService2.addAuthorityAspect(authorityName, CCConstants.CCM_ASPECT_ADDRESS);
                        eduAuthorityService2.setAuthorityProperty(authorityName, CCConstants.CCM_PROP_ADDRESS_POSTALCODE,
                                plz);
                        eduAuthorityService2.setAuthorityProperty(authorityName, CCConstants.CCM_PROP_ADDRESS_CITY, city);


                        counter++;
                    } catch (DuplicateChildNodeNameException e) {
                        logger.error("error in record: " + ((record == null || record.size() < 1) ? null : record.get(0)) + " Folder already exists", e);
                    } catch (Exception e) {
                        logger.error("error in record: " + ((record == null || record.size() < 1) ? null : record.get(0)), e);
                        throw e;
                    }

                }

                return counter;
            }
        };

        return AuthenticationUtil.runAs(runAs, ApplicationInfoList.getHomeRepository().getUsername());
    }

    @Override
    public int importOrgMcConnections(InputStream csv, boolean removeSchoolsFromMC) {
        RunAsWork<Integer> runAs = new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {

                List<List<String>> records = new CSVTool().getRecords(csv, CSVTool.ENC_UTF8);

                /**
                 * remove schools from mediacenter
                 */
                if (removeSchoolsFromMC) {

                    Map<String, List<String>> newMZsAndSchools = listToUniqueMap(records);

                    /**
                     * get existing mediacenters
                     */
                    {
                        SearchParameters sp = new SearchParameters();
                        sp.setQuery("ASPECT:\"ccm:mediacenter\"");
                        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
                        sp.setSkipCount(0);
                        sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

                        ResultSet rs = searchService.query(sp);
                        if (rs == null || rs.length() < 1) {
                            logger.error("no mediacenters found");
                        } else {
                            Map<String, List<String>> existingMZsAndSchools = new HashMap<>();
                            for (NodeRef mzNodeRef : rs.getNodeRefs()) {
                                String authorityName = (String) nodeService.getProperty(mzNodeRef, ContentModel.PROP_AUTHORITY_NAME);
                                String mzId = authorityName.replace("GROUP_MEDIA_CENTER_", "");
                                try {
                                    Integer.parseInt(mzId);
                                    Set<String> mzContains = authorityService.getContainedAuthorities(AuthorityType.GROUP, authorityName, true);

                                    for (String schoolAuthorityName : mzContains) {
                                        //"GROUP_ORG_" + schoolId
                                        NodeRef nodeRef = authorityService.getAuthorityNodeRef(schoolAuthorityName);

                                        if (nodeRef == null) {
                                            logger.info("authority does not exist:" + schoolAuthorityName);
                                            continue;
                                        }
                                        if (!nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) {
                                            logger.debug("authority is no edugroup:" + schoolAuthorityName);
                                            continue;
                                        }

                                        String schoolId = schoolAuthorityName.replace("GROUP_ORG_", "");

                                        List<String> schools = existingMZsAndSchools.get(mzId);
                                        if (schools == null) {
                                            schools = new ArrayList<>();
                                        }
                                        if (!schools.contains(schoolId)) {
                                            schools.add(schoolId);
                                        }
                                        existingMZsAndSchools.put(mzId, schools);
                                    }
                                } catch (NumberFormatException e) {
                                    logger.info("authorityName:" + authorityName + " mzId:" + mzId + " is no number");
                                }
                            }

                            for (Map.Entry<String, List<String>> mzAndSchools : existingMZsAndSchools.entrySet()) {
                                List<String> newSchools = newMZsAndSchools.get(mzAndSchools.getKey());
                                if (newSchools == null) {
                                    logger.info("existing mz:" + mzAndSchools.getKey() + " has a null school list in new sheet");
                                    newSchools = new ArrayList<>();
                                }

                                if (mzAndSchools.getValue() == null) {
                                    logger.info("existing mz:" + mzAndSchools.getKey() + " has a null school list");
                                    continue;
                                }


                                for (String existingSchoolId : mzAndSchools.getValue()) {
                                    if (!newSchools.contains(existingSchoolId)) {
                                        String mzAuthorityName = "GROUP_MEDIA_CENTER_" + mzAndSchools.getKey();
                                        String schoolAuthorityName = "GROUP_ORG_" + existingSchoolId;
                                        logger.info("removing school " + schoolAuthorityName + " from " + mzAuthorityName + " cause its not in imported list");
                                        authorityService.removeAuthority(mzAuthorityName, schoolAuthorityName);
                                    }
                                }
                            }
                        }
                    }
                }


                int counter = 0;
                for (List<String> record : records) {
                    String mzId = record.get(0);
                    String schoolId = record.get(1);

                    SearchParameters sp = getSearchParameterMZ(mzId);
                    ResultSet rs = searchService.query(sp);

                    if (rs == null || rs.length() < 1) {
                        logger.error("no mediacenter found for " + mzId);
                        continue;
                    }

                    NodeRef nodeRefAuthorityMediacenter = rs.getNodeRef(0);

                    String authorityNameSchool = "GROUP_ORG_" + schoolId;

                    sp = new SearchParameters();
                    sp.setQuery("@cm\\:authorityName:" + authorityNameSchool);
                    sp.setMaxItems(1);
                    sp.setLanguage(SearchService.LANGUAGE_LUCENE);
                    sp.setSkipCount(0);
                    sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
                    rs = searchService.query(sp);
                    if (rs == null || rs.length() < 1) {
                        logger.error("no school found for " + schoolId + " " + authorityNameSchool);
                        continue;
                    }

                    String authorityNameMZ = (String) nodeService.getProperty(nodeRefAuthorityMediacenter,
                            ContentModel.PROP_AUTHORITY_NAME);


                    Set<String> mzContains = authorityService.getContainedAuthorities(AuthorityType.GROUP, authorityNameMZ, true);

                    if (!mzContains.contains(authorityNameSchool)) {
                        logger.info("adding school" + authorityNameSchool + " to MZ " + authorityNameMZ);
                        authorityService.addAuthority(authorityNameMZ, authorityNameSchool);
                        counter++;
                    } else {
                        logger.info("mediacenter:" + authorityNameMZ + " already contains " + authorityNameSchool);
                    }

                }
                return counter;
            }
        };

        return AuthenticationUtil.runAs(runAs, ApplicationInfoList.getHomeRepository().getUsername());
    }

    Map<String, List<String>> listToUniqueMap(List<List<String>> records) {
        Map<String, List<String>> result = new HashMap<>();

        for (List<String> record : records) {
            if (!result.containsKey(record.get(0))) {
                List<String> list = new ArrayList<>();
                list.add(record.get(1));
                result.put(record.get(0), list);
            } else {
                result.get(record.get(0)).add(record.get(1));
            }
        }

        return result;
    }

    private SearchParameters getSearchParameterMZ(String mzId) {
        SearchParameters sp = new SearchParameters();
        sp.setQuery("ASPECT:\"ccm:mediacenter\" AND @ccm\\:mediacenterId:\"" + mzId + "\"");
        sp.setMaxItems(1);
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setSkipCount(0);
        sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        return sp;
    }

    /**
     * @return
     */
    public String getMediacenterAdminGroup(String authorityName) {

        NodeRef eduGroupNodeRef = authorityService.getAuthorityNodeRef(authorityName);
        List<ChildAssociationRef> childGroups = nodeService.getChildAssocs(eduGroupNodeRef);
        for (ChildAssociationRef childGroup : childGroups) {
            String grouptype = (String) nodeService.getProperty(childGroup.getChildRef(), QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
            if (AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP_TYPE.equals(grouptype)) {
                return (String) nodeService.getProperty(childGroup.getChildRef(), QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
            }
        }

        return null;
    }

    public void isAllowedToManage(String authorityName) {

        if (org.edu_sharing.service.authority.AuthorityServiceFactory.getLocalService().isGlobalAdmin()) {
            return;
        }

        ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_MEDIACENTER_MANAGE);
        String mediacenterAdminGroup = getMediacenterAdminGroup(authorityName);
        Set<String> mediacenterAdmins = authorityService.getContainedAuthorities(AuthorityType.USER, mediacenterAdminGroup, false);
        if (!mediacenterAdmins.contains(serviceregistry.getAuthenticationService().getCurrentUserName())) {
            throw new RuntimeException("current user is not part of mediacenter admin group");
        }
    }

    /**
     * create mediacenter proxy group and add mediacenter group to proxy group
     */
    public void createMediacenterProxyGroup(String alfAuthorityName, String displayName) throws Exception {

        String mediacenterId = getMediacenterId(alfAuthorityName);
        String mediacenterProxyName = AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE + "_" + mediacenterId;
        AuthorityServiceFactory.getLocalService().createGroupWithType(
                mediacenterProxyName,
                displayName + AuthorityService.MEDIA_CENTER_PROXY_DISPLAY_POSTFIX,
                null,
                AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE);
        authorityService.addAuthority("GROUP_" + mediacenterProxyName, alfAuthorityName);
        String mediacenterAdminGroup = getMediacenterAdminGroup(alfAuthorityName);
        authorityService.addAuthority("GROUP_" + mediacenterProxyName, mediacenterAdminGroup);
    }

    public void createMediacenterAdminGroup(String alfAuthorityName, String displayName) throws Exception {
        AuthorityServiceFactory.getLocalService().createGroupWithType(
                AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP,
                displayName + AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX,
                alfAuthorityName.replace("GROUP_", ""),
                AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP_TYPE);
    }

    public String getMediacenterProxyGroup(String authorityName) {

        String proxyAuthorityName = PermissionService.GROUP_PREFIX + AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE
                + "_"
                + getMediacenterId(authorityName);

        if (authorityService.authorityExists(proxyAuthorityName)) {
            NodeRef nodeRef = authorityService.getAuthorityNodeRef(proxyAuthorityName);
            String groupType = (String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
            if (AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE.equals(groupType)) {
                return proxyAuthorityName;
            }
        }

        return null;
    }

    @Override
    public List<String> getMediacenterAuthoritiesByNode(String nodeId) throws Exception {
        List<String> allMediacenterIds = getAllMediacenterIds();

        return Arrays.stream(PermissionServiceFactory.getLocalService().getPermissions(nodeId).getAces())
                .map(ACE::getAuthority)
                .filter(x -> x.startsWith(PermissionService.GROUP_PREFIX + AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE))
                .map(this::getMediacenterIdFromProxyGroup)
                .filter(allMediacenterIds::contains)
                .map(this::getMediacenterAuthority)
                .filter(this::isActive)
                .collect(Collectors.toList());
    }

    String getMediacenterId(String authorityName) {
        return authorityName.replace(PermissionService.GROUP_PREFIX + AuthorityService.MEDIA_CENTER_GROUP_TYPE + "_", "");
    }

    public String getMediacenterIdFromProxyGroup(String authorityName) {
        return authorityName.replace(PermissionService.GROUP_PREFIX + AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE + "_", "");
    }

    public String getMediacenterAuthority(String mediacenterId) {
        return PermissionService.GROUP_PREFIX + AuthorityService.MEDIA_CENTER_GROUP_TYPE + "_" + mediacenterId;
    }

    public boolean isActive(String authorityName) {
        String proxyGroup = getMediacenterProxyGroup(authorityName);
        if (proxyGroup == null) {
            return false;
        }

        Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, proxyGroup, false);
        if (containedAuthorities == null) {
            return false;
        }

        if (containedAuthorities.contains(authorityName)) {
            return true;
        } else {
            return false;
        }

    }

    public void setActive(boolean active, String authorityName) {
        if (active) {
            if (isActive(authorityName)) {
                return;
            }

            String proxyGroup = getMediacenterProxyGroup(authorityName);
            if (proxyGroup == null) {
                logger.error("no proxy group found for " + authorityName);
                return;
            }

            authorityService.addAuthority(proxyGroup, authorityName);
        } else {
            String proxyGroup = getMediacenterProxyGroup(authorityName);
            if (proxyGroup != null && authorityService.authorityExists(proxyGroup)) {
                authorityService.removeAuthority(proxyGroup, authorityName);
            }
        }
    }


    List<String> getAllMediacenterIds() {
        Set<String> allGroups = authorityService.getAllAuthoritiesInZone(org.alfresco.service.cmr.security.AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);

        List<String> result = new ArrayList<>();

        for (String group : allGroups) {
            NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(group);
            if(authorityNodeRef == null){
                logger.warn("no authority node found for " + group);
                continue;
            }
            if (nodeService.hasAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_MEDIACENTER))) {
                String mediacenterId = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_MEDIACENTER_ID));
                result.add(mediacenterId);
            }
        }
        return result;
    }

    boolean hasPermissionSet(NodeRef nodeRef, String authority, String permission) {
        boolean hasPermission = false;
        Set<AccessPermission> permissionsSet = permissionService.getAllSetPermissions(nodeRef);

        for (AccessPermission ap : permissionsSet) {
            if (authority.equals(ap.getAuthority())
                    && permission.equals(ap.getPermission())
                    && AccessStatus.ALLOWED.equals(ap.getAccessStatus())) {
                if (!ap.isInherited()) {
                    hasPermission = true;
                } else {
                    logger.warn(nodeRef + " permission" + ap.getPermission() + " is inherited");
                }
            }
        }
        return hasPermission;
    }

    boolean hasPermission(NodeRef nodeRef, String authority, String permission) {
        AuthenticationUtil.RunAsWork<Boolean> runAs = () -> {
            if (permissionService.hasPermission(nodeRef, permission) == AccessStatus.ALLOWED) {
                return true;
            } else {
                return false;
            }
        };
        return AuthenticationUtil.runAs(runAs, authority);
    }

    /**
     * @deprecated
     */
    public void manageNodeLicenses() {
        logger.info("cache mediacenterids");
        List<String> allMediacenterIds = getAllMediacenterIds();
        logger.info("cache mediacenter nodes");

        String impFolderId = getPersistentHandlerEdusharing().getImportFolderId();
        if (impFolderId == null) {
            logger.error("no imported objects folder found");
            return;
        }
        Map<String, NodeRef> importedNodes = new NodeHelper().getImportedNodes(impFolderId);


        Map<String, List<String>> sodisMediacenterIdNodes = new HashMap<>();
        for (String mediacenterId : allMediacenterIds) {
            logger.info("cache provider mediacenter nodes mediacenterId:" + mediacenterId + " already cached:" + sodisMediacenterIdNodes.size());
            List<String> nodes = MediacenterLicenseProviderFactory.getMediacenterLicenseProvider().getNodes(mediacenterId);
            sodisMediacenterIdNodes.put(mediacenterId, nodes);
        }


        Map<String, List<NodeRef>> addToMediacenterList = new HashMap<>();
        Map<String, List<NodeRef>> removeFromMediacenterList = new HashMap<>();

        for (String mediacenterId : allMediacenterIds) {
            logger.info("collect differences for " + mediacenterId);
            List<String> sodisLicensedNodes = sodisMediacenterIdNodes.get(mediacenterId);
            /**
             * @TODO check if correct:
             *  when LicenseProvider api does not deliver any datasets prevent
             *  all permissions will be removed
             */
            if (sodisLicensedNodes == null || sodisLicensedNodes.size() == 0) {
                logger.info("leave out mediacenter " + mediacenterId + " cause no licensed nodes found");
                continue;
            }
            String mediacenterName = "GROUP_" + AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE + "_" + mediacenterId;


            for (Map.Entry<String, NodeRef> entry : importedNodes.entrySet()) {

                boolean hasPublishPermission = hasPermissionSet(entry.getValue(), mediacenterName,
                        CCConstants.PERMISSION_CC_PUBLISH);
                boolean hasConsumerPermission = hasPermissionSet(entry.getValue(), mediacenterName,
                        CCConstants.PERMISSION_CONSUMER);

                if (sodisLicensedNodes.contains(entry.getKey()) && (!hasConsumerPermission || !hasPublishPermission)) {
                    List<NodeRef> nodeRefs = addToMediacenterList.get(mediacenterName);
                    if (nodeRefs == null) {
                        nodeRefs = new ArrayList<>();
                        addToMediacenterList.put(mediacenterName, nodeRefs);
                    }
                    nodeRefs.add(entry.getValue());
                } else if (!sodisLicensedNodes.contains(entry.getKey())
                        && (hasConsumerPermission || hasPublishPermission)) {
                    List<NodeRef> nodeRefs = removeFromMediacenterList.get(mediacenterName);
                    if (nodeRefs == null) {
                        nodeRefs = new ArrayList<>();
                        removeFromMediacenterList.put(mediacenterName, nodeRefs);
                    }
                    nodeRefs.add(entry.getValue());
                }
            }
        }

        serviceregistry.getRetryingTransactionHelper().doInTransaction(() -> {
            for (Map.Entry<String, List<NodeRef>> entry : addToMediacenterList.entrySet()) {
                String mediacenter = entry.getKey();
                logger.info("process add changes for " + mediacenter);
                for (NodeRef nodeRef : entry.getValue()) {
                    policyBehaviourFilter.disableBehaviour(nodeRef);
                    permissionService.setPermission(nodeRef, mediacenter, CCConstants.PERMISSION_CONSUMER, true);
                    permissionService.setPermission(nodeRef, mediacenter, CCConstants.PERMISSION_CC_PUBLISH, true);
                    policyBehaviourFilter.enableBehaviour(nodeRef);

                }
            }

            for (Map.Entry<String, List<NodeRef>> entry : removeFromMediacenterList.entrySet()) {
                String mediacenter = entry.getKey();
                logger.info("process remove changes for " + mediacenter);
                for (NodeRef nodeRef : entry.getValue()) {
                    policyBehaviourFilter.disableBehaviour(nodeRef);
                    permissionService.deletePermission(nodeRef, mediacenter, CCConstants.PERMISSION_CONSUMER);
                    permissionService.deletePermission(nodeRef, mediacenter, CCConstants.PERMISSION_CC_PUBLISH);
                    policyBehaviourFilter.enableBehaviour(nodeRef);
                }
            }
            return null;
        });
    }

    private PersistentHandlerEdusharing getPersistentHandlerEdusharing() {
        if (persistentHandlerEdusharing != null) {
            return persistentHandlerEdusharing;
        }
        persistentHandlerEdusharing = AuthenticationUtil.runAsSystem(() -> {
            try {
                return new PersistentHandlerEdusharing(null, null, false);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
        return persistentHandlerEdusharing;
    }

    @Override
    public void manageNodeLicenses(Date from, Date until) {
        logger.info("cache mediacenterids for period: " + from + " - " + until);
        List<String> allMediacenterIds = getAllMediacenterIds();

        for (String mediacenterId : allMediacenterIds) {
            String mediacenterProxyName = "GROUP_" + AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE + "_" + mediacenterId;
            String mediacenterGroupName = "GROUP_" + AuthorityService.MEDIA_CENTER_GROUP_TYPE + "_" + mediacenterId;
            String mediacenterAdminGroup = getMediacenterAdminGroup(mediacenterGroupName);
            List<String> nodesAdd = MediacenterLicenseProviderFactory.getMediacenterLicenseProvider().getNodes(mediacenterId, from, until);
            logger.info(mediacenterId + " found new nodes: " + nodesAdd.size() + " for period: " + from + " - " + until);

            for (String replicationsourceId : nodesAdd) {
                NodeRef nodeRef = getNodeRefByReplicationSourceId(replicationsourceId);
                if (nodeRef == null) {
                    logger.warn("no node found in repo for:" + replicationsourceId);
                    continue;
                }
                boolean hasPublishPermission = hasPermissionSet(nodeRef, mediacenterProxyName,
                        CCConstants.PERMISSION_CC_PUBLISH);
                boolean hasConsumerPermission = hasPermissionSet(nodeRef, mediacenterProxyName,
                        CCConstants.PERMISSION_CONSUMER);
                boolean hasPublishPermissionAdmin = hasPermissionSet(nodeRef, mediacenterAdminGroup,
                        CCConstants.PERMISSION_CC_PUBLISH);
                boolean hasConsumerPermissionAdmin = hasPermissionSet(nodeRef, mediacenterAdminGroup,
                        CCConstants.PERMISSION_CONSUMER);


                serviceregistry.getRetryingTransactionHelper().doInTransaction(() -> {
                    policyBehaviourFilter.disableBehaviour(nodeRef);
                    if (!hasPublishPermission) {
                        logger.info(mediacenterProxyName + " add publish permission for " + nodeRef);
                        permissionService.setPermission(nodeRef, mediacenterProxyName, CCConstants.PERMISSION_CONSUMER, true);
                    }
                    if (!hasConsumerPermission) {
                        logger.info(mediacenterProxyName + " add consumer permission for " + nodeRef);
                        permissionService.setPermission(nodeRef, mediacenterProxyName, CCConstants.PERMISSION_CC_PUBLISH, true);
                    }
                    if (!hasPublishPermissionAdmin) {
                        logger.info(mediacenterAdminGroup + " add publish permission for " + nodeRef);
                        permissionService.setPermission(nodeRef, mediacenterAdminGroup, CCConstants.PERMISSION_CC_PUBLISH, true);
                    }
                    if (!hasConsumerPermissionAdmin) {
                        logger.info(mediacenterAdminGroup + " add consumer permission for " + nodeRef);
                        permissionService.setPermission(nodeRef, mediacenterAdminGroup, CCConstants.PERMISSION_CONSUMER, true);
                    }
                    policyBehaviourFilter.enableBehaviour(nodeRef);
                    return null;
                });


                fixMediacenterStatus(nodeRef, mediacenterGroupName, true);
            }


            List<String> nodesRemove = MediacenterLicenseProviderFactory.getMediacenterLicenseProvider().getNodesLicenseRemoved(mediacenterId, from, until);
            logger.info(mediacenterId + " found nodes where to remove license: " + nodesRemove.size() + " for period: " + from + " - " + until);
            for (String replicationsourceId : nodesRemove) {
                NodeRef nodeRef = getNodeRefByReplicationSourceId(replicationsourceId);
                if (nodeRef == null) {
                    logger.warn("no node found in repo for:" + replicationsourceId);
                    continue;
                }
                boolean hasPublishPermission = hasPermissionSet(nodeRef, mediacenterProxyName,
                        CCConstants.PERMISSION_CC_PUBLISH);
                boolean hasConsumerPermission = hasPermissionSet(nodeRef, mediacenterProxyName,
                        CCConstants.PERMISSION_CONSUMER);

                boolean hasPublishPermissionAdmin = hasPermissionSet(nodeRef, mediacenterAdminGroup,
                        CCConstants.PERMISSION_CC_PUBLISH);
                boolean hasConsumerPermissionAdmin = hasPermissionSet(nodeRef, mediacenterAdminGroup,
                        CCConstants.PERMISSION_CONSUMER);

                if (hasPublishPermission) {
                    serviceregistry.getRetryingTransactionHelper().doInTransaction(() -> {
                        policyBehaviourFilter.disableBehaviour(nodeRef);
                        logger.info(mediacenterProxyName + " remove publish permission for " + nodeRef);
                        permissionService.deletePermission(nodeRef, mediacenterProxyName, CCConstants.PERMISSION_CC_PUBLISH);
                        policyBehaviourFilter.enableBehaviour(nodeRef);
                        return null;
                    });
                }
                if (hasConsumerPermission) {
                    serviceregistry.getRetryingTransactionHelper().doInTransaction(() -> {
                        policyBehaviourFilter.disableBehaviour(nodeRef);
                        logger.info(mediacenterProxyName + " remove consumer permission for " + nodeRef);
                        permissionService.deletePermission(nodeRef, mediacenterProxyName, CCConstants.PERMISSION_CONSUMER);
                        policyBehaviourFilter.enableBehaviour(nodeRef);
                        return null;
                    });
                }

                if (!hasPublishPermissionAdmin) {
                    logger.info(mediacenterAdminGroup + " add publish permission for " + nodeRef);
                    permissionService.setPermission(nodeRef, mediacenterAdminGroup, CCConstants.PERMISSION_CC_PUBLISH, true);
                }
                if (!hasConsumerPermissionAdmin) {
                    logger.info(mediacenterAdminGroup + " add consumer permission for " + nodeRef);
                    permissionService.setPermission(nodeRef, mediacenterAdminGroup, CCConstants.PERMISSION_CONSUMER, true);
                }

                fixMediacenterStatus(nodeRef, mediacenterGroupName, false);
            }
        }
    }

    private void fixMediacenterStatus(NodeRef nodeRef, String mediacenterGroupName, Boolean activated) {

        QName prop = QName.createQName(CCConstants.CCM_PROP_IO_MEDIACENTER);
        List<String> mcStatusList = (List<String>) nodeService.getProperty(nodeRef, prop);

        JSONObject jo = new JSONObject();
        jo.put("name", mediacenterGroupName);
        jo.put("activated", activated.toString());

        ArrayList<String> mcStatusListNew = new ArrayList<>();

        if (mcStatusList == null) {
            mcStatusListNew.add(jo.toJSONString());
        } else if (mcStatusList.stream().anyMatch(o -> o.contains(mediacenterGroupName))) {
            mcStatusListNew.addAll(mcStatusList.stream().map(o -> {
                try {
                    return ((JSONObject) new JSONParser().parse(o)).get("name").equals(mediacenterGroupName) ? jo.toJSONString() : o;
                } catch (ParseException e) {
                    logger.error(e.getMessage());
                    return o;
                }
            }).collect(Collectors.toList()));
        } else {
            mcStatusListNew.addAll(mcStatusList);
            mcStatusListNew.add(jo.toJSONString());
        }

        if (mcStatusList == null || (CollectionUtils.disjunction(mcStatusListNew, mcStatusList)).size() > 0) {
            serviceregistry.getRetryingTransactionHelper().doInTransaction(() -> {
                policyBehaviourFilter.disableBehaviour(nodeRef);
                logger.info("updateing mediacenter status for " + nodeRef + " mediacenter:" + mediacenterGroupName + " activated:" + activated);
                nodeService.setProperty(nodeRef, prop, mcStatusListNew);
                policyBehaviourFilter.enableBehaviour(nodeRef);
                return null;
            });
            new RepositoryCache().remove(nodeRef.getId());
        }

    }

    private NodeRef getNodeRefByReplicationSourceId(String replicationSourceId) {
        NodeRef nodeRef = getPersistentHandlerEdusharing().getNodeIfExists(new HashMap<>() {
                                                                               {
                                                                                   put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, replicationSourceId);
                                                                               }
                                                                           }
        );

        if (nodeRef == null) {
            logger.info("creating dummy object for:" + replicationSourceId);
            Map<String, Object> properties = new HashMap<>();
            properties.put(CCConstants.CM_NAME, replicationSourceId);
            properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP, "1900-01-01T00:00:00Z");
            properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, replicationSourceId);
            properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, MediacenterLicenseProviderFactory.getMediacenterLicenseProvider().getCatalogId());
            properties.put(CCConstants.CCM_PROP_IO_TECHNICAL_STATE, "problem_notAvailable");
            try {
                String nodeId = getPersistentHandlerEdusharing().safe((RecordHandlerInterfaceBase) () -> properties, null, MediacenterLicenseProviderFactory.getMediacenterLicenseProvider().getSet());
                if (nodeId != null) {
                    nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
                }
            } catch (Throwable throwable) {
                logger.error(throwable.getMessage(), throwable);
            }
        }
        return nodeRef;
    }

    @Override
    public void deleteMediacenter(String authorityName) {
        if (authorityService.authorityExists(authorityName)) {
            NodeRef nodeRef = authorityService.getAuthorityNodeRef(authorityName);
            if (!nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_MEDIACENTER))) {
                throw new RuntimeException(authorityName + " is no mediacenter.");
            }
            serviceregistry.getRetryingTransactionHelper().doInTransaction(() -> {

                String authorityNameAdmin = getMediacenterAdminGroup(authorityName);
                if (authorityNameAdmin != null) {
                    authorityService.deleteAuthority(authorityNameAdmin);
                }

                authorityService.deleteAuthority(authorityName);

                String authorityNameProxy = getMediacenterProxyGroup(authorityName);
                if (authorityNameProxy != null) {
                    authorityService.deleteAuthority(authorityNameProxy);
                }
                return null;
            });
        } else {
            throw new RuntimeException(authorityName + " does not exist.");
        }
    }

    @Override
    public List<org.edu_sharing.service.model.NodeRef> getAllLicensedNodes(String mediacenter, Map<String, String[]> criteria, SortDefinition sortDefinition) throws Throwable {
        List<org.edu_sharing.service.model.NodeRef> data = new ArrayList<>();
        SearchToken searchToken = new SearchToken();
        searchToken.setAuthorityScope(Collections.singletonList(getAuthorityScope(mediacenter)));
        searchToken.setFacets(new ArrayList<>());
        searchToken.setExcludes(Arrays.asList("preview", "collections", "children"));
        if (sortDefinition != null) {
            searchToken.setSortDefinition(sortDefinition);
        }
        if (SearchServiceFactory.getLocalService() instanceof SearchServiceElastic) {
            return ((SearchServiceElastic) SearchServiceFactory.getLocalService()).searchAll(MetadataHelper.getLocalDefaultMetadataset(), "mediacenter_filter", criteria, searchToken);
        } else {
            throw new RuntimeException("getAllLicensedNodes requires Elasticsearch");
        }
    }


}
