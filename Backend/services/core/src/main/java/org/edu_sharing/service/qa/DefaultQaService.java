package org.edu_sharing.service.qa;

import org.edu_sharing.restservices.qa.v1.domain.CreateQANodeRequestDTO;
import org.edu_sharing.restservices.qa.v1.domain.UpdateQAEntriesRequestDTO;
import org.edu_sharing.service.qa.domain.QAEntry;
import org.edu_sharing.service.qa.domain.QANode;

import java.util.List;

public class DefaultQaService implements QAService {
    @Override
    public void createQANode(String sourceId, String nodeId, CreateQANodeRequestDTO requestData) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateQANode(String sourceId, String nodeId, UpdateQAEntriesRequestDTO requestData) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public QANode getQANode(String sourceId, String nodeId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<QANode> getAllQANode(String nodeId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<QAEntry> getAllQAEntriesOf(String nodeId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<QAEntry> getAllQAEntriesOf(String sourceId, String nodeId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(String nodeId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(String sourceId, String nodeId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
