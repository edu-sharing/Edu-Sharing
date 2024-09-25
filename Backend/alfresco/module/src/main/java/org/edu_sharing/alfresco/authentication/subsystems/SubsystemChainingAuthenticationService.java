package org.edu_sharing.alfresco.authentication.subsystems;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.dao.ConcurrencyFailureException;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class SubsystemChainingAuthenticationService extends org.alfresco.repo.security.authentication.subsystems.SubsystemChainingAuthenticationService {

    Logger logger = Logger.getLogger(SubsystemChainingAuthenticationService.class);


    static ThreadLocal<String> successFullAuthenticationMethod = new ThreadLocal<String>();
    static ThreadLocal<String> currentPath = new ThreadLocal<>();

    private NodeService nodeService;
    private PersonService personService;
    private TransactionService transactionService;
    private GuestService guestService;

    /**
     * {@inheritDoc}
     */
    public void authenticate(String userName, char[] password) throws AuthenticationException {
        preAuthenticationCheck(userName);
        List<AuthenticationService> usableAuthenticationServices = getUsableAuthenticationServices();
        int counter = usableAuthenticationServices.size();
        for (AuthenticationService authService : usableAuthenticationServices) {
            try {
                counter--;
                authService.authenticate(userName, password);
                if (logger.isDebugEnabled()) {
                    logger.debug("authenticate " + userName + " with " + getId(authService) + " SUCCEEDED");
                }
                successFullAuthenticationMethod.set(getId(authService));
                setLoginTimestampToNow(userName, CCConstants.PROP_USER_ESFIRSTLOGIN);
                setLoginTimestampToNow(userName, CCConstants.PROP_USER_ESLASTLOGIN);

                return;
            } catch (AuthenticationException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("authenticate " + userName + " with " + getId(authService) + (counter == 0 ? " FAILED (end of chain)" : " failed (try next in chain)"));
                }
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Failed to authenticate");

    }

    public void setLoginTimestampToNow(String userName, String property) {
        NodeRef nodeRefPerson = personService.getPerson(userName, false);
        String currentUser = ApplicationInfoList.getHomeRepository().getUsername();

        // we won't do this for the guest
        if (userName != null && (guestService.isGuestUser(userName) || Objects.equals(currentUser, userName))) {
            return;
        }

        RunAsWork<Void> runAs = () -> {
            //alfresco share login is in readOnlyMode, so check to prevent exception
            if (AlfrescoTransactionSupport.getTransactionReadState() == TxnReadState.TXN_READ_ONLY) {
                logger.debug("unable to set cm:esLastLogin  for user " + userName + " cause of " + AlfrescoTransactionSupport.getTransactionReadState());
                return null;
            }

            RetryingTransactionCallback<Void> txnWork = () -> {
                try {
                    if(property.equals(CCConstants.PROP_USER_ESFIRSTLOGIN)) {
                        Serializable value = nodeService.getProperty(nodeRefPerson, QName.createQName(property));
                        if(value != null) {
                            return null;
                        } else {
                            // try to copy the last login and use this as the first known login
                            value = nodeService.getProperty(nodeRefPerson, QName.createQName(CCConstants.PROP_USER_ESLASTLOGIN));
                            if(value != null) {
                                nodeService.setProperty(nodeRefPerson, QName.createQName(property), value);
                                return null;
                            }
                        }
                    }
                    nodeService.setProperty(nodeRefPerson, QName.createQName(property), new Date());
                } catch (ConcurrencyFailureException e) {
                    logger.info("failed to set cm:esLastLogin for user " + userName + " cause of " + e.getClass().getSimpleName());
                }
                return null;
            };
            return transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false);
        };
        AuthenticationUtil.runAsSystem(runAs);
    }

    public static void setSuccessFullAuthenticationMethod(String successFullAuthenticationMethod) {
        SubsystemChainingAuthenticationService.successFullAuthenticationMethod.set(successFullAuthenticationMethod);
    }

    public static String getSuccessFullAuthenticationMethod() {
        return successFullAuthenticationMethod.get();
    }

    public static void setCurrentPath(String currentPath) {
        SubsystemChainingAuthenticationService.currentPath.set(currentPath);
    }

    public static String getCurrentPath() {
        return currentPath.get();
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setGuestService(GuestService guestService) {
        this.guestService = guestService;
    }
}
