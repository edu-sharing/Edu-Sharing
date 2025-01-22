package org.edu_sharing.restservices.register.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class RegisterInformation implements Serializable{
    private String vcard;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String organization;
    // general flag for indicating if user may receive mail notifications
    private boolean allowNotifications;
    private String authorityName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterInformation that = (RegisterInformation) o;
        return Objects.equals(email, that.email);
    }
}
