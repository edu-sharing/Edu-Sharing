package org.edu_sharing.service.model;

public interface CollectionRef extends NodeRef{
    /**
     * this node contains the relationship, e.g. it can be a proposal or an usage
     */
    NodeRef getRelationNode();

    /**
     * get information about the relation, e.g. is this a real reference or just a proposal
     */
    RelationType getRelationType();

    enum RelationType {
        Usage,
        Proposal,
        Original
    }
}
