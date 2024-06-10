package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import net.sf.acegisecurity.providers.dao.UsernameNotFoundException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.edu_sharing.repository.server.tools.KeyTool;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import jakarta.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@UpdateService
public class FixMissingUserstoreNode {


    private final MutableAuthenticationDao authenticationDao;
    private final PersonService personService;
    private final NodeService nodeService;
    private final TransactionService transactionService;

    @Autowired
    public FixMissingUserstoreNode(@Qualifier("authenticationDao") MutableAuthenticationDao authenticationDao, PersonService personService, NodeService nodeService, TransactionService transactionService) {
        this.authenticationDao = authenticationDao;
        this.personService = personService;
        this.nodeService = nodeService;
        this.transactionService = transactionService;
    }


    @UpdateRoutine(
            id = "FixMissingUserstoreNode",
            description = "finds users that got no entry in userStore",
            order = 1801
    )
    public void execute(boolean test) {
        NodeRef peopleContainer = personService.getPeopleContainer();

        log.info("peopleContainer:" + peopleContainer);

        List<ChildAssociationRef> children = nodeService.getChildAssocs(peopleContainer);

        ArrayList<String> missingUsers = new ArrayList<>();

        for (ChildAssociationRef childref : children) {

            if (!ContentModel.TYPE_PERSON.equals(nodeService.getType(childref.getChildRef()))) {
                log.error(childref.getChildRef() + "is no person");
                continue;
            }

            String userName = (String) nodeService.getProperty(childref.getChildRef(), ContentModel.PROP_USERNAME);

            if ("guest".equals(userName)) {
                log.info("ignoring guest");
                continue;
            }

            if ("System".equals(userName)) {
                log.info("ignoring System");
                continue;
            }

            if (userName == null || userName.trim().equals("")) {
                log.error("no username for " + childref.getChildRef());
                continue;
            }

            try {
                authenticationDao.loadUserByUsername(userName);
            } catch (UsernameNotFoundException e) {
                log.info("username " + userName + " exsists as person but not as user in userstore");
                missingUsers.add(userName);
            }
        }

        if (!test) {
            UserTransaction userTransaction = transactionService.getNonPropagatingUserTransaction();
            try {

                userTransaction.begin();

                for (String missingUser : missingUsers) {
                    authenticationDao.createUser(missingUser, new KeyTool().getRandomPassword().toCharArray());
                }

                userTransaction.commit();

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                try {
                    log.error("trying rollback");
                    userTransaction.rollback();
                } catch (Exception rollBackException) {
                    log.error(rollBackException.getMessage(), rollBackException);
                }
            }
        }
    }

}
