package org.edu_sharing.service.qa.domain;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class QANode {
    String id;
    String sourceId;
    String nodeId;
    Date generatedDate;
    String usedText;
    List<QAEntry> entries;

    public QANode() {
    }

    public QANode(String sourceId, String nodeId, Date generatedDate, String usedText, List<QAEntry> entries) {
        this.sourceId = sourceId;
        this.nodeId = nodeId;
        this.generatedDate = generatedDate;
        this.usedText = usedText;
        this.entries = entries;
    }
}
