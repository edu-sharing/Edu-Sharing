package org.edu_sharing.repository.server.jobs.quartz;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.person.PersonServiceImpl;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class MigrationJob extends AbstractJob {

    public final static String PARAM_IDM_URL = "PARAM_IDM_URL";
    public final static String PARAM_TESTMODE = "PARAM_TESTMODE";

    public final static String PARAM_AUTHORITIES = "AUTHORITIES";

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    org.edu_sharing.alfresco.service.OrganisationService eduOrganisationService = (org.edu_sharing.alfresco.service.OrganisationService) applicationContext
            .getBean("eduOrganisationService");

    BehaviourFilter behaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");


    String[] protectedGroups = new String[] { "GROUP_EVERYONE", "GROUP_site_swsdp_SiteManager",
            "GROUP_site_swsdp_SiteCollaborator", "GROUP_site_swsdp_SiteContributor", "GROUP_site_swsdp_SiteConsumer",
            "GROUP_ToolPermission_Sharing", "GROUP_ToolPermission_Licensing", "GROUP_ToolPermission_UncheckedContent",
            "GROUP_ALFRESCO_ADMINISTRATORS", "GROUP_EMAIL_CONTRIBUTORS", "GROUP_SITE_ADMINISTRATORS",
            "GROUP_ALFRESCO_SEARCH_ADMINISTRATORS" };

    Logger logger = Logger.getLogger(MigrationJob.class);

    Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");

    String fileUpdateProtocol = "LogineoMigration";

    AuthorityService authorityService = serviceRegistry.getAuthorityService();

    NodeService nodeService = serviceRegistry.getNodeService();

    QName protocolProp = QName.createQName(CCConstants.LOM_PROP_GENERAL_KEYWORD);

    String tempDir = System.getProperty("java.io.tmpdir");
    String protocolFile = tempDir + "/" + "MigrationJob.txt";
    private Client client;
    private String url;

    List<String> cleanAuthorityDisplayname = LightbendConfigLoader.get().hasPath("migrationjob.cleanAuthorityDisplayname")
            ? LightbendConfigLoader.get().getStringList("migrationjob.cleanAuthorityDisplayname")
            : new ArrayList<>();

    boolean useIdForSchoolMapping = LightbendConfigLoader.get().hasPath("migrationjob.useIdForSchoolMapping")
            ? LightbendConfigLoader.get().getBoolean("migrationjob.useIdForSchoolMapping")
            : true;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        RunAsWork<Void> runAs = new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {

                try {

                    NodeRef companyHomeNodeRef = repositoryHelper.getCompanyHome();




                    url = context.getJobDetail().getJobDataMap().getString(PARAM_IDM_URL);
                    String ptestmode = context.getJobDetail().getJobDataMap().getString(PARAM_TESTMODE);

                    boolean testMode = (ptestmode == null) ? true : new Boolean(ptestmode);

                    String paramAuthorities = context.getJobDetail().getJobDataMap().getString(PARAM_AUTHORITIES);

                    client = ClientBuilder.newClient();

                    // http://localhost:8085/migration/mapping?groupId=wedwedwedw&url=http%3A%2F%2Feee.de%2Feee

                    Set<String> authorities = (paramAuthorities == null || paramAuthorities.trim().equals(""))
                            ? authorityService.getAllAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP)
                            : new HashSet<>(Arrays.asList(paramAuthorities.split(",")));
                    logger.info(";authorities:" + authorities.size());
                    for (String authority : authorities) {
                        logger.info(";authority;"+authority);
                        NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authority);

                        if (authorityNodeRef == null) {
                            logger.error(";error;" + authority + ";authorityNodeRef is null");
                            continue;
                        }

                        String authorityName = (String) nodeService.getProperty(authorityNodeRef,
                                ContentModel.PROP_AUTHORITY_NAME);
                        String authorityNameDN = (String) nodeService.getProperty(authorityNodeRef,
                                ContentModel.PROP_AUTHORITY_DISPLAY_NAME);

                        boolean isEduGroup = nodeService.hasAspect(authorityNodeRef,
                                QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP));
                        String groupType = (String) serviceRegistry.getNodeService().getProperty(authorityNodeRef,
                                QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
                        boolean authorityWasUsed = authorityIsUsedInACE(authority);
                        if(Arrays.asList(protectedGroups).contains(authority)) {
                            logger.info(";protected_skip;" + authority);
                        }
                        else if (CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(groupType)) {
                            logger.info(";admingroup_skip;" + authority);
                            // we migrate them with the real org later, so do nothing here
                            /**
                             * groupName: ORG_ADMINISTRATORS parentGroup: ORG_id_1
                             *
                             *
                             * public static String getGroupName(String groupName, String parentGroup) {
                             * String prefix = ""; if (parentGroup != null) { prefix =
                             * MD5.Digest(parentGroup.getBytes()) + "_"; } String name = prefix + groupName;
                             *
                             * return name; }
                             *
                             * --> Vorraussetzung ist, dass die Organisation schon migriert wurde -->
                             * nachgelagerte Aufgabe
                             *
                             */
                        }
                        else if(!authorityWasUsed && !isEduGroup){
                            //logger.info(";unused_skip;" + authorityNodeRef + ";" + authorityName + ";" + authorityNameDN);
                            // boolean isGroupExtension =
                            // serviceRegistry.getNodeService().hasAspect(authorityNodeRef,
                            // QName.createQName(CCConstants.CCM_ASPECT_GROUPEXTENSION));

                            logger.info(";would_delete;" + authorityName + ";" + authorityNameDN + ";" + authorityNodeRef);
                            if (!testMode) {
                                //prevent mapped nodes being deleted by the second run
                                if (!protocolAlreadyProcessed(authorityNodeRef)) {
                                    authorityService.deleteAuthority(authority);
                                    logger.info(";delete;" + authorityName + ";" + authorityNameDN + ";" + authorityNodeRef);
                                }

                            }
                        }
                        else if (!isEduGroup){
                            String result=getIdmMapping(authority);
                            if (result != null && !result.trim().equals("")) {

                                logger.info(";would_migrate;" + authorityNodeRef + ";" + authorityName + ";" + authorityNameDN
                                        + ";" + result);
                                if (!testMode) {

                                    serviceRegistry.getTransactionService().getRetryingTransactionHelper()
                                            .doInTransaction(new RetryingTransactionCallback<Void>() {
                                                @Override
                                                public Void execute() throws Throwable {

                                                    if(protocolAlreadyProcessed(authorityNodeRef)) {
                                                        return null;
                                                    }

                                                    AlfrescoTransactionSupport.bindResource(
                                                            PersonServiceImpl.KEY_ALLOW_UID_UPDATE, Boolean.TRUE);

                                                    nodeService.setProperty(authorityNodeRef,
                                                            ContentModel.PROP_AUTHORITY_NAME, "GROUP_" + result.trim());
                                                    logger.info(";migrate;" + authorityNodeRef + ";" + authorityName + ";" + authorityNameDN
                                                            + ";" + result);
                                                    protocolNode(authorityNodeRef);
                                                    return null;
                                                }
                                            });

                                }
                            } else {
                                logger.error(";error_missing_mapping;" + authorityNodeRef + ";" + authorityName + ";"
                                        + authorityNameDN + ";result from idm is null");
                            }
                        }
                        else if (isEduGroup) {

                            String resultEduGroup = getIdmMappingEduGroup(authorityName, authorityNameDN, MigrationJob.this.useIdForSchoolMapping);

                            if (resultEduGroup != null && !resultEduGroup.trim().equals("")) {
                                logger.info(";would_migrate_school;" + authorityNodeRef + ";" + authorityName + ";"
                                        + authorityNameDN + ";" + resultEduGroup);

                                String organisationName = "GROUP_ORG_" + resultEduGroup.trim();
                                String adminGroupName = org.edu_sharing.alfresco.service.AuthorityService
                                        .getGroupName(
                                                org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP,
                                                organisationName.replace("GROUP_", ""));

                                if (!testMode) {
                                    String plainOrgName = resultEduGroup;
                                    serviceRegistry.getTransactionService().getRetryingTransactionHelper()
                                            .doInTransaction(new RetryingTransactionCallback<Void>() {
                                                @Override
                                                public Void execute() throws Throwable {
                                                    try {

                                                        if(protocolAlreadyProcessed(authorityNodeRef)) {
                                                            return null;
                                                        }

                                                        logger.info(";migrate_school;" + authorityNodeRef + ";" + authorityName + ";"
                                                                + authorityNameDN + ";" + plainOrgName);


                                                        AlfrescoTransactionSupport.bindResource(
                                                                PersonServiceImpl.KEY_ALLOW_UID_UPDATE,
                                                                Boolean.TRUE);

                                                        /**
                                                         * rename organisation
                                                         */
                                                        nodeService.setProperty(
                                                                authorityNodeRef, ContentModel.PROP_AUTHORITY_NAME,
                                                                organisationName);

                                                        protocolNode(authorityNodeRef);

                                                        /**
                                                         * rename adminstrator group
                                                         */
                                                        List<ChildAssociationRef> childsOfOrg = nodeService.getChildAssocs(authorityNodeRef);
                                                        for (ChildAssociationRef childRef : childsOfOrg) {

                                                            String groupType = (String) nodeService
                                                                    .getProperty(childRef.getChildRef(),
                                                                            QName.createQName(
                                                                                    CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
                                                            if (CCConstants.ADMINISTRATORS_GROUP_TYPE
                                                                    .equals(groupType)) {

                                                                if(protocolAlreadyProcessed(childRef.getChildRef())) {
                                                                    continue;
                                                                }
                                                                nodeService.setProperty(
                                                                        childRef.getChildRef(),
                                                                        ContentModel.PROP_AUTHORITY_NAME,
                                                                        "GROUP_" + adminGroupName);

                                                                protocolNode(childRef.getChildRef());
                                                            }
                                                        }

                                                        /**
                                                         * create safe organisation
                                                         */

                                                        NodeRef homeDirNodeRef = (NodeRef)nodeService.getProperty(authorityNodeRef,QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));

                                                        String nameHomeDir = (String)nodeService.getProperty(homeDirNodeRef, ContentModel.PROP_NAME);

                                                        Map<QName, Serializable> safeOrgProps = eduOrganisationService
                                                                .getOrganisation(plainOrgName + "_"
                                                                        + CCConstants.CCM_VALUE_SCOPE_SAFE);
                                                        if (safeOrgProps == null) {

                                                            //first use folderName as displayname so that the homefolder of safe is the same name with safe suffix
                                                            eduOrganisationService.createOrganization(
                                                                    plainOrgName.trim(), nameHomeDir, null,
                                                                    CCConstants.CCM_VALUE_SCOPE_SAFE);


                                                            //second set correctly displayName for safe org
                                                            String authorityNameSafeOrg ="GROUP_ORG_" + plainOrgName.trim() + "_" + CCConstants.CCM_VALUE_SCOPE_SAFE;
                                                            String authorityDisplayNameSafeOrg = authorityNameDN + "_" + CCConstants.CCM_VALUE_SCOPE_SAFE;
                                                            authorityService.setAuthorityDisplayName(authorityNameSafeOrg, authorityDisplayNameSafeOrg);



                                                            //third set correctly displayName for safe org_administrators
                                                            NodeRef authorityNodeRefSafe = authorityService.getAuthorityNodeRef(authorityNameSafeOrg);
                                                            List<ChildAssociationRef> childsOfOrgSafe = nodeService.getChildAssocs(authorityNodeRefSafe);
                                                            for (ChildAssociationRef childRef : childsOfOrgSafe) {

                                                                String groupType = (String) nodeService
                                                                        .getProperty(childRef.getChildRef(),
                                                                                QName.createQName(
                                                                                        CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
                                                                if (CCConstants.ADMINISTRATORS_GROUP_TYPE
                                                                        .equals(groupType)) {

                                                                    if(protocolAlreadyProcessed(childRef.getChildRef())) {
                                                                        continue;
                                                                    }
                                                                    nodeService.setProperty(
                                                                            childRef.getChildRef(),
                                                                            ContentModel.PROP_AUTHORITY_DISPLAY_NAME,
                                                                            authorityDisplayNameSafeOrg + " (Admin)");

                                                                    protocolNode(childRef.getChildRef());
                                                                }
                                                            }
                                                        } else {
                                                            logger.error(";error_safe_exists;" + plainOrgName + "_"
                                                                    + CCConstants.CCM_VALUE_SCOPE_SAFE
                                                                    + " already exists");
                                                        }
                                                    } catch (Throwable e) {
                                                        logger.error(e.getMessage(), e);
                                                    }
                                                    return null;

                                                }

                                            });

                                }
                            }

                        }

                    }

                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
                return null;

            }
        };
        AuthenticationUtil.runAsSystem(runAs);

    }

    private String getIdmMappingEduGroup(String authorityName, String authorityNameDN, boolean useAuthorityName) throws UnsupportedEncodingException {

        //String cleanedAuthorityName = authorityNameDN.replace("Hamburg - ", "");
        //cleanedAuthorityName = cleanedAuthorityName.replace("BSB - ", "");
        WebTarget webTargetEduGroup = null;
        if(useAuthorityName){
            String id = authorityName.replace("GROUP_","");
            logger.info("asking for school with id:" + id);
            webTargetEduGroup = client
                    .target("http://localhost:8085/migration/mappingSchoolById?esLegacyId="
                            + URLEncoder.encode(id, "UTF-8") + "&url=" + url);
        }else{

            String cleanedAuthorityName = new String(authorityNameDN);
            for(String clean : cleanAuthorityDisplayname){
                cleanedAuthorityName = cleanedAuthorityName.replace(clean,"");
            }


            webTargetEduGroup = client
                    .target("http://localhost:8085/migration/mappingSchool?schoolName="
                            + URLEncoder.encode(cleanedAuthorityName, "UTF-8") + "&url=" + url);
        }

        String resultEduGroup = null;
        try {
            resultEduGroup = webTargetEduGroup.request(MediaType.APPLICATION_JSON).buildGet()
                    .invoke(String.class);
        } catch (NotFoundException e) {
            logger.error(
                    ";error_missing_mapping_school" + authorityName + ";" + authorityNameDN + ";" + e.getMessage()+";"+useAuthorityName);
        }
        if(resultEduGroup==null || resultEduGroup.trim().isEmpty()){
            logger.error(
                    ";error_missing_mapping_school" + authorityName + ";" + authorityNameDN + ";result from idm is null;"+useAuthorityName);
        }
        return resultEduGroup;
    }


    private String getIdmMapping(String authority) {

        String authorityPur = authority.replace("GROUP_", "");
        WebTarget webTarget = client.target(
                "http://localhost:8085/migration/mapping?groupId=" + authorityPur + "&url=" + url);

        return webTarget.request(MediaType.APPLICATION_JSON).buildGet().invoke(String.class);
    }

    private void protocolNode(NodeRef authority) {
        logger.debug("called");

        List<String> nodeEntries = null;

        try {
            nodeEntries = FileUtils.readLines(new File(protocolFile));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        if(nodeEntries == null) {
            nodeEntries = new ArrayList<>();
        }

        nodeEntries.add(authority.getId());

        try {
            FileUtils.writeLines(new File(protocolFile), Arrays.asList(new String[] {authority.getId()}), true);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        logger.debug("ends");
    }

    private boolean protocolAlreadyProcessed(NodeRef authority) {
        logger.debug("called");

        List<String> nodeEntries = null;

        try {
            nodeEntries = FileUtils.readLines(new File(protocolFile));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if(nodeEntries == null) return false;


        boolean alreadyProcessed = nodeEntries.contains(authority.getId());

        if(alreadyProcessed) {
            logger.info(";alreadyProcessed;"+ authority +";" + nodeService.getProperty(authority, ContentModel.PROP_AUTHORITY_DISPLAY_NAME));
        }
        logger.debug("ends");
        return alreadyProcessed;
    }

    private static String sql = "select authority from alf_authority where authority=?";

    boolean authorityIsUsedInACE(String authorityName) throws SQLException {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection connection = dbAlf.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, authorityName);
        ResultSet rs = preparedStatement.executeQuery();

        boolean result = true;

        if (rs.next()) {
            String authorityDB = rs.getString("authority");
            if (authorityName.equals(authorityDB)) {
                result = true;
                // this will not happen:
            } else {
                result = false;
            }
        } else {
            result = false;
        }

        dbAlf.cleanUp(connection);
        return result;
    }

    @Override
    public Class[] getJobClasses() {
        this.addJobClass(MigrationJob.class);
        return super.allJobs;
    }
}
