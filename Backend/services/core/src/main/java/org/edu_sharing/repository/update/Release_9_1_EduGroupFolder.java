package org.edu_sharing.repository.update;


import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Slf4j
@UpdateService
public class Release_9_1_EduGroupFolder {

    private final OrganisationService organisationService;


    @Autowired
    public Release_9_1_EduGroupFolder(OrganisationService organisationService){
        this.organisationService = organisationService;
    }

    @UpdateRoutine(
            id = "Release_9_1_EduGroupFolder",
            description = "add ccm:edugroup_folder aspect to the organisation folders",
            order = 0,
            auto = true)
    public void execute() {
        List<Map<QName, Serializable>> organisations = organisationService.getOrganisations();
        for(Map<QName, Serializable> orgProps : organisations){
            NodeRef homeFolderRef = (NodeRef)orgProps.get(OrganisationService.PROP_EDUGROUP_EDU_HOMEDIR);
            String authorityName = (String)orgProps.get(ContentModel.PROP_AUTHORITY_NAME);
            try {
                organisationService.bindEduGroupToFolder(authorityName, homeFolderRef);
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
        }

    }
}
