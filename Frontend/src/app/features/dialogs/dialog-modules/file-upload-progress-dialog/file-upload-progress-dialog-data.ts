import { Node } from 'ngx-edu-sharing-api';

export class FileUploadProgressDialogData {
    parent: Node;
    files: FileList;
}

export type FileUploadProgressDialogResult = Node[] | null;
