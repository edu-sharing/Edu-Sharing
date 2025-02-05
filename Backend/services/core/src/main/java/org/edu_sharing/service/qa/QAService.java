package org.edu_sharing.service.qa;

import org.edu_sharing.restservices.qa.v1.domain.CreateQANodeRequestDTO;
import org.edu_sharing.restservices.qa.v1.domain.UpdateQAEntriesRequestDTO;
import org.edu_sharing.service.qa.domain.QAEntry;
import org.edu_sharing.service.qa.domain.QANode;

import java.util.List;

public interface QAService {
    void createQANode(String sourceId, String nodeId, CreateQANodeRequestDTO requestData);

    void updateQANode(String sourceId, String nodeId, UpdateQAEntriesRequestDTO requestData);

    QANode getQANode(String sourceId, String nodeId);

    List<QANode> getAllQANode(String nodeId);

    List<QAEntry> getAllQAEntriesOf(String nodeId);

    List<QAEntry> getAllQAEntriesOf(String sourceId, String nodeId);

    void delete(String nodeId);

    void delete(String sourceId, String nodeId);
}
