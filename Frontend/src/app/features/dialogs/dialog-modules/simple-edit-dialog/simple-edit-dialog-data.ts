import { Node } from 'ngx-edu-sharing-api';

export class SimpleEditDialogData {
    nodes: Node[];
    fromUpload: boolean;
}

export type SimpleEditDialogResult = Node[] | null;
