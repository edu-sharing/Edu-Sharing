import { Node } from '../models';

export type CollectionProposalStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';
export interface ProposalNode extends Node {
    proposal: Node;
    status: CollectionProposalStatus;
    // collection this proposal is for
    proposalCollection?: Node;
    accessible: boolean;
}
