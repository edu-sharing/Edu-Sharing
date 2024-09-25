package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;

public class FixOrganisationAdminGroup extends AbstractJob{

    ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    AuthorityService authorityService = serviceRegistry.getAuthorityService();

    NodeService nodeService = serviceRegistry.getNodeService();

    PermissionService permissionService = serviceRegistry.getPermissionService();

    OrganisationService eduOrganisationService = (OrganisationService)alfApplicationContext.getBean("eduOrganisationService");

    public static String PARAM_ORG_FILTER = "ORG_FILTER";


    Logger logger = Logger.getLogger(FixOrganisationAdminGroup.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String orgFilterString = jobExecutionContext.getJobDetail().getJobDataMap().getString(PARAM_ORG_FILTER);
        List<String> organisationFilter = new ArrayList<>();
        if(orgFilterString != null && !orgFilterString.trim().isEmpty()){
            organisationFilter.addAll(Arrays.asList(orgFilterString.split(",")));
        }

        AuthenticationUtil.runAsSystem(() ->{
            //get all organisations
            Set<String> allGroups = authorityService.getAllAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);
            Set<String> allOrganisations = allGroups.stream()
                    .filter(g ->
                            nodeService.hasAspect(authorityService.getAuthorityNodeRef(g), OrganisationService.ASPECT_EDUGROUP)
                                    && (organisationFilter.size() == 0 || organisationFilter.contains(g)) )
                    .collect(Collectors.toSet());

            logger.info("found "+allOrganisations.size()+" organisations");
            for(String org : allOrganisations){
                //create org admin group
                try {
                    logger.info("org:" + org);
                    String orgAdminGroup = eduOrganisationService.getOrganisationAdminGroup(org);
                    if (orgAdminGroup == null) {
                        orgAdminGroup = eduOrganisationService.createOrganizationAdminGroup(org);
                        logger.info("created admin group " + orgAdminGroup + " for org " + org);
                    }
                    //set permissions for org admin group on folder
                    NodeRef orgNodeRef = authorityService.getAuthorityNodeRef(org);
                    NodeRef orgFolder = (NodeRef) nodeService.getProperty(orgNodeRef, QName.createQName(OrganisationService.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
                    if (orgFolder != null) {
                        //checks before set
                        String orgAdminAuthorityName = (!orgAdminGroup.startsWith(PermissionService.GROUP_PREFIX))
                                ? PermissionService.GROUP_PREFIX + orgAdminGroup
                                : orgAdminGroup;
                        eduOrganisationService.setOrgAdminPermissionsOnNode(orgAdminAuthorityName, true, orgFolder);
                    } else logger.error("no organisation folder found for " + org);
                }catch (IllegalArgumentException e){
                    logger.error(e.getMessage());
                }
            }
            return null;
        });

    }
}
