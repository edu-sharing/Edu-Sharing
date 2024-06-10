package org.edu_sharing.repository.server.tools.cache;

import lombok.Setter;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Setter
public class UserCache {
    private NodeService nodeService;
    private PersonService personService;


    final SimpleCache<String, User> userCache =
            (SimpleCache<String, User>) AlfAppContextGate.getApplicationContext().getBean("eduSharingUserCache");

    public final User get(String username){
        if(username == null) return null;
        return userCache.get(username);
    }

    public final void put(String username, User user){
        if(username == null) return;
        userCache.put(username,user);
    }

    public final void refresh(String username){
        if(username == null) return;
        userCache.remove(username);
    }

    public User getUser(String username){
        User user = get(username);
        if(user != null){
            return user;
        }

        user = new User();
        user.setUsername(username);
        NodeRef persNoderef = null;
        try {
            persNoderef = personService.getPerson(username,false);
        } catch(NoSuchPersonException e) {
            //ie the system user
        }
        if(persNoderef != null){
            Map<QName, Serializable> props = nodeService.getProperties(persNoderef);
            user.setEmail((String)props.get(QName.createQName(CCConstants.CM_PROP_PERSON_EMAIL)));
            user.setGivenName((String)props.get(QName.createQName(CCConstants.CM_PROP_PERSON_FIRSTNAME)));
            user.setSurname(((String)props.get(QName.createQName(CCConstants.CM_PROP_PERSON_LASTNAME))));
            user.setNodeId(persNoderef.getId());
            Map<String, Serializable> userProperties = new HashMap<>();
            for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
                Serializable value = entry.getValue();
                userProperties.put(
                        entry.getKey().toString(),
                        value);
            }
            user.setProperties(userProperties);
        }
        put(username,user);
        return user;
    }
}
