package org.edu_sharing.service.lifecycle;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.edu_sharing.service.collection.Collection;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganisationLifecycleServiceTestSetup {


    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    OrganisationService organisationService = (OrganisationService) applicationContext.getBean("eduOrganisationService");

    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    AuthorityService authorityService = serviceRegistry.getAuthorityService();
    PersonService personService = serviceRegistry.getPersonService();
    NodeService nodeService = serviceRegistry.getNodeService();


    int numberOfPersons = 10;

    int numberOfHomeDocs = 12;

    //+ 3 (dokuments,images,shared)
    int numberOfHomeFolders = 9;

    int numberOfSharedDocs = 10;
    int numberOfSharedFolders = 4;

    int numberOfCollections = 3;
    int numberOfCollectionRefs = 7;

    List<NodeRef> persons = new ArrayList<>();
    String orgAuthorityName;


    public String createTestSetup() {
        String orgAuthorityName = AuthenticationUtil.runAsSystem(this::testSetup);
        testSetupPersons();
        return orgAuthorityName;
    }


    private String testSetup(){
        try {
            orgAuthorityName = "GROUP_" + organisationService.createOrganization("testSchool", "Test School");

            //create persons
            //TODO safe
            for(int i = 0; i < numberOfPersons; i++){
                Map<QName, Serializable> personProps = new HashMap<>();
                personProps.put(ContentModel.PROP_FIRSTNAME, RandomStringUtils.random(6, true, false));
                personProps.put(ContentModel.PROP_LASTNAME, RandomStringUtils.random(10, true, false));
                personProps.put(ContentModel.PROP_EMAIL, RandomStringUtils.random(6, true, false) +"@test.de");
                personProps.put(ContentModel.PROP_USERNAME,RandomStringUtils.random(6, true, false));
                persons.add(personService.createPerson(personProps));
                authorityService.addAuthority(orgAuthorityName, (String)personProps.get(ContentModel.PROP_USERNAME));
                String organisationAdminGroup = organisationService.getOrganisationAdminGroup(orgAuthorityName);
                authorityService.addAuthority(organisationAdminGroup, (String)personProps.get(ContentModel.PROP_USERNAME));
            }
            return orgAuthorityName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testSetupPersons(){
        for(NodeRef person : persons){
            String user = AuthenticationUtil.runAsSystem(() -> (String)nodeService.getProperty(person,ContentModel.PROP_USERNAME));

            //home
            NodeRef rootFolder = AuthenticationUtil.runAsSystem(() ->(NodeRef) nodeService.getProperty(person, ContentModel.PROP_HOMEFOLDER));
            createFoldersFiles(user, numberOfHomeFolders, numberOfHomeDocs, rootFolder);

            //shared
            Map<QName, Serializable> orgProps = AuthenticationUtil.runAsSystem(() -> organisationService.getOrganisation(organisationService.getCleanName(orgAuthorityName)));
            NodeRef orgFolder = (NodeRef) orgProps.get(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
            createFoldersFiles(user,numberOfSharedFolders,numberOfSharedDocs,orgFolder);


            //safe
            boolean safeFoldersManaged = ScopeUserHomeServiceFactory.getScopeUserHomeService().isManageEduGroupFolders();
            try {
                NodeServiceInterceptor.setEduSharingScope(CCConstants.CCM_VALUE_SCOPE_SAFE);

                if(!safeFoldersManaged) {
                    ScopeUserHomeServiceFactory.getScopeUserHomeService().setManageEduGroupFolders(true);
                }

                NodeRef ref  = AuthenticationUtil.runAsSystem(() -> {
                            AuthenticationUtil.setFullyAuthenticatedUser(user);
                            AuthenticationUtil.setRunAsUserSystem();
                            return  ScopeUserHomeServiceFactory.getScopeUserHomeService().getUserHome(user, CCConstants.CCM_VALUE_SCOPE_SAFE, true);
                        }
                );
                createFoldersFiles(user, numberOfHomeFolders, numberOfHomeDocs, ref);

                //create/get safe org

                String authorityNameOrgSafe = orgAuthorityName + "_safe";
                Map<QName, Serializable> orgSafeProps = AuthenticationUtil.runAsSystem(() -> organisationService.getOrganisation(organisationService.getCleanName(authorityNameOrgSafe)));
                NodeRef orgSafeFolder = (NodeRef) orgSafeProps.get(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
                String organisationAdminGroup = AuthenticationUtil.runAsSystem(() -> organisationService.getOrganisationAdminGroup(authorityNameOrgSafe));
                AuthenticationUtil.runAsSystem(() -> {
                    authorityService.addAuthority(organisationAdminGroup, user);
                    return null;
                });
                createFoldersFiles(user, numberOfSharedFolders, numberOfSharedDocs, orgSafeFolder);

            }finally {
                NodeServiceInterceptor.setEduSharingScope(null);
                if(!safeFoldersManaged) {
                    ScopeUserHomeServiceFactory.getScopeUserHomeService().setManageEduGroupFolders(false);
                }
            }
        }
    }

    private void createFoldersFiles(String user, int nrOfFolders, int nrOfDocs, NodeRef parent){
        AuthenticationUtil.runAs(() -> {
            List<NodeRef> folderList = new ArrayList<>();
            for(int i = 0; i  < nrOfFolders; i++){
                Map<QName,Serializable> folderProps = new HashMap<>();
                folderProps.put(ContentModel.PROP_NAME,RandomStringUtils.random(6, true, false));
                folderList.add(createNode(parent, CCConstants.CCM_TYPE_MAP, folderProps));
            }

            List<NodeRef> fileList = new ArrayList<>();
            for(int i = 0; i < nrOfDocs; i++){
                int parentFolderIdx = RandomUtils.nextInt(0,folderList.size() -1);
                Map<QName,Serializable> props = new HashMap<>();
                props.put(ContentModel.PROP_NAME, RandomStringUtils.random(6, true, false)+"."+RandomStringUtils.random(3, true, false));
                fileList.add(createNode(folderList.get(parentFolderIdx),CCConstants.CCM_TYPE_IO,props));
            }

            NodeRef personHomeFolder = (NodeRef) nodeService.getProperty(personService.getPerson(user),ContentModel.PROP_HOMEFOLDER);
            if(personHomeFolder.equals(parent) ){
                createCollectionsAndRefs(user, fileList);
            }

            return null;
        },user);
    }

    private void createCollectionsAndRefs(String user, List<NodeRef> fileList) {
        CollectionService collectionService = CollectionServiceFactory.getLocalService();
        Collection level0Col = null;
        List<Collection> collectionList = new ArrayList<>();
        for(int i = 0; i < numberOfCollections; i++){
            try {
                Collection col = new Collection();
                if (level0Col != null) {
                    col.setTitle(RandomStringUtils.random(6, true, false));
                    col = collectionService.create(level0Col.getNodeId(), col);
                }

                if (level0Col == null) {
                    col.setTitle(user + RandomStringUtils.random(6, true, false));
                    col.setLevel0(true);
                    col = collectionService.create(null, col);
                    level0Col = col;
                }

                collectionList.add(col);
            }catch (Throwable e){
                throw new RuntimeException(e);
            }
        }

        for(int i = 0; i < numberOfCollectionRefs;i++){
            int fileIdx = RandomUtils.nextInt(0, fileList.size() -1);
            int collectionIdx = RandomUtils.nextInt(0,collectionList.size() -1);
            try {
                collectionService.addToCollection(collectionList.get(collectionIdx).getNodeId(), fileList.get(fileIdx).getId(),null,true);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private NodeRef createNode(NodeRef parent,String type, Map<QName,Serializable> props){
        try {
            return nodeService.createNode(parent,
                    ContentModel.ASSOC_CONTAINS,
                    QName.createQName("{"+CCConstants.NAMESPACE_CCM+"}"+props.get(ContentModel.PROP_NAME)),
                    QName.createQName(type),
                    props).getChildRef();
        }catch (DuplicateChildNodeNameException e){
            props.put(ContentModel.PROP_NAME,props.get(ContentModel.PROP_NAME) + RandomStringUtils.randomAscii(10));
            return nodeService.createNode(parent,
                    ContentModel.ASSOC_CONTAINS,
                    QName.createQName("{"+CCConstants.NAMESPACE_CCM+"}"+props.get(ContentModel.PROP_NAME)),
                    QName.createQName(type),
                    props).getChildRef();
        }
    }

}
