package org.edu_sharing.alfresco.service.guest;

import com.typesafe.config.Optional;
import lombok.Data;

import java.util.List;

@Data
public class GuestConfig {
    private boolean enabled;
    private String username;
    private List<String> groups;
}
