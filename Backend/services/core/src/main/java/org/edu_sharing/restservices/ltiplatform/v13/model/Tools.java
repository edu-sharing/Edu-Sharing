package org.edu_sharing.restservices.ltiplatform.v13.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
public class Tools {
    private List<Tool> tools = new ArrayList<>();

}
