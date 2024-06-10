package org.edu_sharing.repository.server.importer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * dummy class to wrap a property list into an record handler
 */
public class RecordHandlerStatic implements RecordHandlerInterfaceBase {

    private final Map<String, Object> props;

    public RecordHandlerStatic(Map<String, Object> props) {
        this.props=props;
    }

    @Override
    public Map<String, Object> getProperties() {
        return props;
    }
}
