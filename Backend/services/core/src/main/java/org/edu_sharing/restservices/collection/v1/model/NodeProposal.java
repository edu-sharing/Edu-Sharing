package org.edu_sharing.restservices.collection.v1.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.Node;

@Data
@EqualsAndHashCode(callSuper = true)
public class NodeProposal extends Node {
    /**
     * if this proposal is accessible by the current user
     */
    private boolean isAccessible;
    /**
     * status of this proposal
     */
    private CCConstants.PROPOSAL_STATUS status;

    /**
     * ref of the proposal node
     */
    private Node proposal;

    public NodeProposal() {
    }
}
