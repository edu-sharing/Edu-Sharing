package org.edu_sharing.restservices.node.v1.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NodeTemplateEntry extends NodeEntry {
    private boolean enabled;
}
