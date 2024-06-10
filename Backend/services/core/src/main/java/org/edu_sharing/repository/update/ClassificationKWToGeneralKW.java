/**
 *
 */
package org.edu_sharing.repository.update;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.MCBaseClient;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@UpdateService
public class ClassificationKWToGeneralKW {

    private final static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    private final NodeService nodeService;
    private final Set<String> updatedIOs = new HashSet<>();

    @Autowired
    public ClassificationKWToGeneralKW(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @UpdateRoutine(
            id = "CKW_TO_GKW",
            description = "UPDATE AUF 1.4.0. speichert alle lom classification keywords nach general keywords",
            order = 1400
    )
    public void execute(boolean test) {
        log.info("starting with test:" + test);
        try {
            MCBaseClient mcBaseClient = new MCAlfrescoAPIClient();
            MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) mcBaseClient;
            String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();

            runFromLevel(test, companyHomeId);
            log.info("updated ios:" + updatedIOs.size() + " test:" + test);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void runFromLevel(boolean test, String parentId) {

        log.info("starting with parentId:" + parentId);

        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(new NodeRef(storeRef, parentId));
        for (ChildAssociationRef objectAssocRef : childAssocs) {

            // process io
            QName typeQName = nodeService.getType(objectAssocRef.getChildRef());
            if (typeQName.isMatch(QName.createQName(CCConstants.CCM_TYPE_IO))) {

                Set<QName> childSet = new HashSet<QName>();
                childSet.add(QName.createQName(CCConstants.LOM_TYPE_CLASSIFICATION));

                List<ChildAssociationRef> classificationRefs = nodeService.getChildAssocs(objectAssocRef.getChildRef(), childSet);
                for (ChildAssociationRef classRef : classificationRefs) {
                    List<MLText> classKeywordList = (List<MLText>) nodeService.getProperty(classRef.getChildRef(), QName.createQName(CCConstants.LOM_PROP_CLASSIFICATION_KEYWORD));
                    if (classKeywordList != null && classKeywordList.size() > 0) {
                        List<MLText> generalKeyWordList = (List<MLText>) nodeService.getProperty(objectAssocRef.getChildRef(), QName.createQName(CCConstants.LOM_PROP_GENERAL_KEYWORD));
                        writeToGeneralKeyword(objectAssocRef.getChildRef(), generalKeyWordList, classKeywordList, test);
                    }
                }
            }

            // process child
            if (typeQName.isMatch(QName.createQName(CCConstants.CCM_TYPE_MAP)) || typeQName.isMatch(QName.createQName(CCConstants.CM_TYPE_FOLDER))) {
                runFromLevel(test, objectAssocRef.getChildRef().getId());
            }
        }
    }

    private void writeToGeneralKeyword(NodeRef nodeRef, List<MLText> generalKeyWordList, List<MLText> classificationKeywordList, boolean test) {
        ArrayList<MLText> newKeywordsList = new ArrayList<>();

        if (generalKeyWordList != null) {
            newKeywordsList.addAll(generalKeyWordList);
        }

        for (MLText classKW : classificationKeywordList) {

            log.info(" classKW.getValues().size(): " + classKW.getValues().size() + " classKW.getDefaultValue():" + classKW.getDefaultValue());

            if (classKW.getValues().size() > 0 && !classKW.getDefaultValue().trim().equals("") && !mlTextListContainsValue(newKeywordsList, classKW)) {
                newKeywordsList.add(classKW);
            }
        }

        if ((generalKeyWordList == null && newKeywordsList.size() > 0) || (newKeywordsList.size() > generalKeyWordList.size())) {

            log.info("updating node IO: " + nodeRef.getId());

            if (!test) {
                nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_GENERAL_KEYWORD), newKeywordsList);
            }

            updatedIOs.add(nodeRef.getId());
        }

    }

    boolean mlTextListContainsValue(List<MLText> list, MLText value) {

        for (MLText tmpMlText : list) {
            if (tmpMlText.getDefaultValue() != null && tmpMlText.getDefaultValue().trim().equals(value.getDefaultValue().trim())) {
                return true;
            }
        }

        return false;
    }

}
