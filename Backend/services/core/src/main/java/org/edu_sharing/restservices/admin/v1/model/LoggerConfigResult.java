package org.edu_sharing.restservices.admin.v1.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class LoggerConfigResult {
    String name;
    String level;
    List<String> appender;
    boolean config = false;
}
