import { Node } from 'ngx-edu-sharing-api';

export interface NodeRelationsDialogData {
    node: Node;
}

/** `true` when relations were changed. */
export type NodeRelationsDialogResult = true | null;
