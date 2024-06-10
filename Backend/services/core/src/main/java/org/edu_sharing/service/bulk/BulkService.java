package org.edu_sharing.service.bulk;


import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.restservices.DAOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface BulkService {

    NodeRef getPrimaryFolder();

    NodeRef sync(String group, List<String> match, List<String> groupBy, String type, List<String> aspects, Map<String, String[]> properties, boolean forceUpdate) throws Throwable;

    NodeRef find(Map<String, String[]> properties) throws Exception;

    List<BulkRun> runs(String replicationsource, BulkRun.RunState filterByState) throws DAOException;
}
