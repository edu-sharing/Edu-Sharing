package org.edu_sharing.repository.server.importer;

import org.alfresco.service.cmr.repository.NodeRef;

public class FactualTermBulkEditConsumer implements java.util.function.Consumer<NodeRef>{

    FactualTermDisplayUpdater updater;

    public FactualTermBulkEditConsumer(){
        try {
            updater = new FactualTermDisplayUpdater();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accept(NodeRef nodeRef) {
        updater.resetDisplayProperty(nodeRef);
    }
}
