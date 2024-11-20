import { Node } from '../../../../core-module/core.module';

export class RevocationDialogData {
    node: Node;
}

export interface RevocationDialogResult {
    reason: string;
    node: Node;
}
