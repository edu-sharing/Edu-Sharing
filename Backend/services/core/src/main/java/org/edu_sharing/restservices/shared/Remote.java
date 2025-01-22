package org.edu_sharing.restservices.shared;

import lombok.Data;
import java.io.Serializable;

@Data
public class Remote implements Serializable {
    private Repo repository;
    private String id;
}
