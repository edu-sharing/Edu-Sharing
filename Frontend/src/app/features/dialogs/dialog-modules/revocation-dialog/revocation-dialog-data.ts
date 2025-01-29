import { Node } from 'ngx-edu-sharing-api';

export class RevocationDialogData {
    node: Node;
}

export interface RevocationDialogResult {
    reason: string;
    node: Node;
}
