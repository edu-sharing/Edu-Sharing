package org.edu_sharing.restservices.shared;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserQuota implements Serializable  {
    private boolean enabled;
    private long sizeCurrent;
    private long sizeQuota;
}
