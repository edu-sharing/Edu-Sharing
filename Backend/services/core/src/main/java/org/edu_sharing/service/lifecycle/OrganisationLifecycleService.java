package org.edu_sharing.service.lifecycle;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceImpl;
import org.edu_sharing.service.collection.CollectionServiceConfig;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orgs:
 * - SafeOrg
 * Subgroups
 * OrgFolder
 * Collections mit safescope?
 * - DefaultOrg
 * Subgroups
 * OrgFolder
 * Persons (mit PersonLifecycle ohne Organisationsordner)
 * <p>
 * <p>
 * bei PersonLifeCycleService müsste personstatus auf todelete gesetzt werren
 */
public class OrganisationLifecycleService {

    Logger logger = Logger.getLogger(OrganisationLifecycleService.class);

    PersonLifecycleService personLifecycleService = new PersonLifecycleService();

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    OrganisationService organisationService = (OrganisationService) applicationContext.getBean("eduOrganisationService");

    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    AuthorityService authorityService = serviceRegistry.getAuthorityService();
    PersonService personService = serviceRegistry.getPersonService();
    NodeService nodeService = serviceRegistry.getNodeService();

    OrganisationDeleteProtocolService protocolService;

    String organisation;

    CollectionServiceConfig collectionServiceConfig = (CollectionServiceConfig) ApplicationContextFactory.getApplicationContext().getBean("collectionServiceConfig");

    public OrganisationLifecycleService(String organisation){
        this.organisation = organisation;
        protocolService = new OrganisationDeleteProtocolServiceCSV(organisation);
    }


    public boolean deleteOrganisation() {
        String authorityName = organisationService.getAuthorityName(this.organisation );

        logger.info("starting for organisation:" + authorityName);
        try {
            if(!authorityService.authorityExists(authorityName)){
                logger.error("authority does not exists " + authorityName);
                throw new Exception("authority does not exists " + authorityName);
            }

            return deleteOrganisationWithContext(this.organisation , authorityName);
        }catch(Throwable e){
            logger.error(e.getMessage(),e);
            protocolService.protocolError(this.organisation, this.organisation, e.getMessage() );
            return false;
        }finally {
            protocolService.writeProtocolToAlfrescoNode(OrganisationDeleteProtocolServiceCSV.MIMETYPE);
            protocolService.cleanUp();
        }
    }

    private boolean deleteOrganisationWithContext(String orga, String authorityName) {
        Set<String> users = authorityService.getContainedAuthorities(AuthorityType.USER, authorityName, true);

        for (String user : users) {
            try {
                PersonDeleteResult personDeleteResult = deleteUser(authorityName, user);
                if (!checkNodes(authorityName, user, CCConstants.CCM_TYPE_IO,null)) return false;
                if (!checkNodes(authorityName, user, CCConstants.CCM_TYPE_IO,CCConstants.CCM_VALUE_SCOPE_SAFE)) return false;
                if (!checkNodes(authorityName, user, CCConstants.CCM_TYPE_MAP,null)) return false;
                if (!checkNodes(authorityName, user, CCConstants.CCM_TYPE_MAP,CCConstants.CCM_VALUE_SCOPE_SAFE)) return false;
                protocolService.protocolPerson(orga, personDeleteResult);
            } catch (RuntimeException e) {
                logger.error(e.getMessage(),e);
                protocolService.protocolError(orga, user, e.getMessage());
                //skip for fixing problem by hand
                return false;
            }catch(Throwable e){
                logger.error(e.getMessage(),e);
                protocolService.protocolError(orga, user, e.getMessage());
                return false;
            }
        }

        /*
         * check if still users exist, also in subgroups (which is not edu-sharing default)
         */
        users = authorityService.getContainedAuthorities(AuthorityType.USER, authorityName, false);
        if(users != null && !users.isEmpty()){
            String userFailed = String.join(",", users);
            String message = "the following users could not be deleted:" +userFailed;
            logger.error(message);
            protocolService.protocolError(orga,orga,message);
            return false;
        }

        //delete safe org and subgroups
        String authorityNameSafe = authorityName + "_safe";
        deleteOrganisationGroup(orga, authorityNameSafe);


        //delete org and subgroups
        deleteOrganisationGroup(orga, authorityName);
        return true;
    }

    private boolean checkNodes(String authorityName, String user, String type, String scope) {
        List<NodeRef> allNodeRefs = personLifecycleService.getAllNodeRefs(user, type, scope);
        if(allNodeRefs != null && !allNodeRefs.isEmpty()){
            if(CCConstants.CCM_TYPE_MAP.equals(type)) {

                //ignore folders of shared area, which will not be deleted by personlifecycleservice, but removed when org is deleted
                String fullOrgName = authorityName + ((scope != null) ? "_" + scope : "");
                Map<QName, Serializable> orgProps = organisationService.getOrganisation(organisationService.getCleanName(fullOrgName));
                if(orgProps == null){
                    String message = "no  org found:"+fullOrgName;
                    if(CCConstants.CCM_VALUE_SCOPE_SAFE.equals(scope)){
                        message = "no safe org found:"+fullOrgName;
                    }
                    logger.info(message);
                    protocolService.protocolError(authorityName, user, message);
                }else{
                    NodeRef orgHomeFolderRef = (NodeRef) orgProps.get(QName.createQName(OrganisationService.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
                    String orgHomeFolderPath = nodeService.getPath(orgHomeFolderRef).toPrefixString(serviceRegistry.getNamespaceService());
                    allNodeRefs = allNodeRefs.stream().filter(nodeRef -> !nodeService.getPath(nodeRef)
                                    .toPrefixString(serviceRegistry.getNamespaceService())
                                    .contains(orgHomeFolderPath))
                            .collect(Collectors.toList());
                }


                //ignore collection path folders, that need to stay there cause it can be that another user saved an collection there
                if(!allNodeRefs.isEmpty()){
                    allNodeRefs = allNodeRefs.stream().filter(nodeRef ->
                            !(
                                    !nodeService.hasAspect(nodeRef,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))
                                    && nodeService.getPath(nodeRef).toPrefixString(serviceRegistry.getNamespaceService()).contains(collectionServiceConfig.getPath())
                            )
                    ).collect(Collectors.toList());
                }

                //ignore intermediate folders for safe home
                if(!allNodeRefs.isEmpty() && CCConstants.CCM_VALUE_SCOPE_SAFE.equals(scope)){
                    NodeRef safeHomerFolderRoot = ((ScopeUserHomeServiceImpl) ScopeUserHomeServiceFactory.getScopeUserHomeService()).getRootNodeRef(scope);
                    allNodeRefs = allNodeRefs.stream()
                            .filter(nodeRef -> !safeHomerFolderRoot.equals(nodeService.getPrimaryParent(nodeRef).getParentRef()))
                            .collect(Collectors.toList());
                }
            }

            if(!allNodeRefs.isEmpty()) {
                String message = "the following nodes(type=" + type + ") for " + user + " scope:" + scope + " could not be deleted:" + allNodeRefs.stream()
                        .map(NodeRef::toString)
                        .collect(Collectors.joining(","));
                logger.error(message);
                protocolService.protocolError(authorityName, user, message);
                return false;
            }
        }
        return true;
    }

    protected void deleteOrganisationGroup(String orga, String authorityName){
        if(authorityService.authorityExists(authorityName)) {
            //delete subgroups
            deleteSubGroups(orga, authorityName);

            //delete org folders @TDOD: check why personlifecycle service does not remove folders, can be a perfomance issue for large folder structures
            Map<QName, Serializable> orgProps = organisationService.getOrganisation(organisationService.getCleanName(authorityName));
            if(orgProps == null){
                String message = "this group is no organiation:"+authorityName+"."+" if it formerly was an edugroup you have to manually delete groupfolder and group";
                logger.error(message);
                protocolService.protocolError(orga,authorityName,message);
                //don't throw here cause it wouldn be easy to fix this for customer
                return;
            }

            NodeRef orgFolder = (NodeRef) orgProps.get(QName.createQName(OrganisationService.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
            if (nodeService.exists(orgFolder)) {
                nodeService.addAspect(orgFolder, ContentModel.ASPECT_TEMPORARY, null);
                nodeService.deleteNode(orgFolder);
            } else {
                logger.warn("no organisation folder found for org:" + authorityName + " ref:" + orgFolder);
            }

            //delete organisation group
            authorityService.deleteAuthority(authorityName);

            protocolService.protocolEntry(OrganisationDeleteProtocolServiceCSV.OrganisationDeleteProtocol.instance(orga, authorityName));
        } else{
           logger.info("authority does not exist:" + authorityName);
        }
    }

    protected void deleteSubGroups(String orga, String authorityName){
        Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, authorityName, false);
        List<String> result = new ArrayList<>();
        for(String authority : containedAuthorities){

            checkAuthorityOnlyExistsInOneOrg(authorityName,authority);

            authorityService.deleteAuthority(authority);
            result.add(authority);
        }
        String scope = null;
        if(authorityName.endsWith("_safe")) scope = CCConstants.CCM_VALUE_SCOPE_SAFE;
        protocolService.protocolSubGroups(orga,result,scope);

        Set<String> containedAuthoritiesRemained = authorityService.getContainedAuthorities(AuthorityType.GROUP, authorityName, false);
        if(containedAuthoritiesRemained != null && !containedAuthoritiesRemained.isEmpty()){
            throw new RuntimeException("the following subgroups of "+ authorityName + " could not be deleted:"
                    + String.join(",", containedAuthoritiesRemained));
        }
    }

    protected PersonDeleteResult deleteUser(String orgAuthorityName, String userName) {
        logger.info("deleting user:" + userName);

        checkAuthorityOnlyExistsInOneOrg(orgAuthorityName, userName);


        NodeRef nodeRef = personService.getPerson(userName);
        nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS), PersonLifecycleService.PersonStatus.todelete.name());

        return personLifecycleService.deletePersons(Collections.singletonList(userName), getPersonDeleteOptions()).results.get(0);
    }

    private void checkAuthorityOnlyExistsInOneOrg(String orgAuthorityName, String authority) {
        //check user exists only in organisation orgAuthorityName
        Set<String> containedAuthorities = authorityService.getContainingAuthorities(AuthorityType.GROUP, authority, false);
        List<String> orgMemberships = containedAuthorities.stream()
                .filter(g -> g.startsWith("GROUP_ORG_"))
                .collect(Collectors.toList());
        if(!orgMemberships.contains(orgAuthorityName)){
            throw new RuntimeException("authority " + authority + " is not member of organisation " + orgAuthorityName);
        }

        String orgAuthorityNameSafe = orgAuthorityName + "_" + CCConstants.CCM_VALUE_SCOPE_SAFE;
        orgMemberships = orgMemberships.stream()
                .filter(a -> (!a.equals(orgAuthorityName) && !a.equals(orgAuthorityNameSafe)))
                .collect(Collectors.toList());
        if(!orgMemberships.isEmpty()){
            throw new RuntimeException("authority " + authority + " is member of more than one organisation");
        }
    }

    private static @NotNull PersonDeleteOptions getPersonDeleteOptions() {
        PersonDeleteOptions options = new PersonDeleteOptions();
        options.cleanupMetadata = false;
        options.collectionFeedback = new PersonDeleteOptions.DeleteOption(true);
        options.ratings = new PersonDeleteOptions.DeleteOption(true);
        options.comments = new PersonDeleteOptions.DeleteOption(true);
        options.statistics = new PersonDeleteOptions.DeleteOption(true);
        options.stream = new PersonDeleteOptions.DeleteOption(true);

        options.collections = new PersonDeleteOptions.CollectionOptions();
        options.collections.privateCollections = PersonDeleteOptions.DeleteMode.delete;
        options.collections.publicCollections = PersonDeleteOptions.DeleteMode.delete;

        options.homeFolder = new PersonDeleteOptions.HomeFolderOptions();
        options.homeFolder.folders = PersonDeleteOptions.FolderDeleteMode.delete;
        options.homeFolder.ccFiles = PersonDeleteOptions.DeleteMode.delete;
        options.homeFolder.privateFiles = PersonDeleteOptions.DeleteMode.delete;
        options.homeFolder.keepFolderStructure = false;

        options.sharedFolders = new PersonDeleteOptions.SharedFolderOptions();
        options.sharedFolders.folders = PersonDeleteOptions.FolderDeleteMode.delete;
        options.sharedFolders.ccFiles = PersonDeleteOptions.DeleteMode.delete;
        options.sharedFolders.privateFiles = PersonDeleteOptions.DeleteMode.delete;
        options.sharedFolders.move = false;
        return options;
    }


}
