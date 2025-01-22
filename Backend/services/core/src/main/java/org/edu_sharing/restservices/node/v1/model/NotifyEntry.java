package org.edu_sharing.restservices.node.v1.model;

import lombok.Data;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.restservices.shared.ACL;
import org.edu_sharing.restservices.shared.User;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class NotifyEntry {

    @JsonProperty(required = true)
    private long date;
    
    @JsonProperty(required = true)
    private ACL permissions;

    @JsonProperty(required = true)
    private User user;

    @JsonProperty(required = true)
    private String action;

    public NotifyEntry() {
    }

    public NotifyEntry(Notify notify) {
        date = notify.getCreated().getTime();
        action = notify.getNotifyAction();
        user = new User(notify.getUser());
        permissions = new ACL(notify.getAcl());
    }
}
