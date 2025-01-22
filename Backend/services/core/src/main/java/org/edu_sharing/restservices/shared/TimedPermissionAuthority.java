package org.edu_sharing.restservices.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections.ListUtils;

import java.util.Objects;
import java.util.Set;

/**
 * An Authority + from + to permission timing
 * Used to prevent duplicate authorities when resolving permissions
 */
@Data
@AllArgsConstructor
public class TimedPermissionAuthority {
    private Authority authority;
    private Long from, to;
    private Set<String> permissions;

    public boolean isTimed() {
        return from != null || to != null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TimedPermissionAuthority)) {
            return false;
        }

        TimedPermissionAuthority that = (TimedPermissionAuthority) o;
        if (!authority.equals(that.authority) || !ListUtils.isEqualList(permissions, that.permissions)) {
            return false;
        }

        if (!this.isTimed()) {
            return !that.isTimed();
        }

        return that.isTimed();
    }

    @Override
    public int hashCode() {
        return Objects.hash(authority.hashCode(), permissions, from != null, to != null);
    }

    public boolean equalsIgnorePermissions(TimedPermissionAuthority that) {
        if (!authority.equals(that.authority)) {
            return false;
        }

        if (!this.isTimed()) {
            return !that.isTimed();
        }

        return that.isTimed();
    }

    public boolean equalsIgnoreFromTo(TimedPermissionAuthority that) {
        return authority.equals(that.authority)
                && (permissions.containsAll(that.permissions) || that.permissions.containsAll(permissions));
    }
}
