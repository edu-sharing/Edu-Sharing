package org.edu_sharing.restservices.sharing.v1.model;

import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Person;
import org.edu_sharing.service.share.ShareService;
import org.edu_sharing.service.share.ShareServiceImpl;

import java.util.Date;

public class SharingInfo {
    private boolean passwordMatches;
    private boolean password;
    private boolean expired;
    private Person invitedBy = null;
    private Node node;
    public SharingInfo(Share share, NodeDao nodeDao, String passwordCheck) throws DAOException {
        this.password=share.getPassword()!=null;
        setInvitedBy(convertToPerson(share, nodeDao));
        if(passwordCheck!=null && !passwordCheck.isEmpty()){
            this.passwordMatches=ShareServiceImpl.encryptPassword(passwordCheck).equals(share.getPassword());
        }
        this.expired=share.getExpiryDate() != ShareService.EXPIRY_DATE_UNLIMITED && new Date(System.currentTimeMillis()).after(new Date(share.getExpiryDate()));
        if(!this.expired)
            this.node=nodeDao.asNode();
    }

    private Person convertToPerson(Share share, NodeDao nodeDao) {
        Person ref = new Person();
        ref.setFirstName((String) share.getProperties()
                .get(CCConstants.NODECREATOR_FIRSTNAME));
        ref.setLastName((String) share.getProperties()
                .get(CCConstants.NODECREATOR_LASTNAME));
        if(nodeDao.checkUserHasPermissionToSeeMail((String) share.getProperties().get(CCConstants.CM_PROP_C_CREATOR))) {
            ref.setMailbox((String) share.getProperties().get(CCConstants.NODECREATOR_EMAIL));
        }
        return ref;
    }

    public Person getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Person invitedBy) {
        this.invitedBy = invitedBy;
    }

    public boolean isPasswordMatches() {
        return passwordMatches;
    }

    public void setPasswordMatches(boolean passwordMatches) {
        this.passwordMatches = passwordMatches;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}
