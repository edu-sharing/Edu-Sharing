import { Node } from 'ngx-edu-sharing-api';

export interface DeleteNodesDialogData {
    nodes: Node[];
}

/**
 * The data indicating which nodes were changed in what way in case the user confirms the dialog.
 *
 * `null` in case the user cancels the dialog or no nodes have been modified due to an error.
 */
export type DeleteNodesDialogResult = DeleteNodesDialogResultData | null;

/**
 * An authorized user has the choice to not delete the nodes but to modify their permissions
 * instead. In this case, `nodes` holds the updated nodes and `action` is set to "changed".
 */
interface DeleteNodesDialogResultData {
    /** The (updated) nodes, that have been processed. */
    nodes: Node[];
    /** Whether the nodes were deleted or changed. */
    action: 'deleted' | 'changed';
}
