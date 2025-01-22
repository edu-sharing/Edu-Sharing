package org.edu_sharing.restservices.collection.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.Node;

import java.util.Map;


@Data
@EqualsAndHashCode(callSuper = true)
public class NodeCollectionProposalCount extends Node {
    private Map<CCConstants.PROPOSAL_STATUS, Integer> proposalCounts;
}
