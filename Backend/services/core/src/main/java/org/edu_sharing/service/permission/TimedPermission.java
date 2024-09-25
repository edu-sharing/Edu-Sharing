package org.edu_sharing.service.permission;

import lombok.Data;

import java.util.Date;

@Data
public class TimedPermission {
    private String node_id;
    private String permission;
    private String authority;
    private Date from;
    private Date to;
    private boolean activated;
}
