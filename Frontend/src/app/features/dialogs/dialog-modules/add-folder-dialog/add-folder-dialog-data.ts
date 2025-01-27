import { Node } from 'ngx-edu-sharing-api';

export class AddFolderDialogData {
    parent: Node;
    /** Folder name to be pre-filled. */
    name?: string;
}

export interface AddFolderDialogResult {
    /** The folder name chosen by the user. */
    name: string;
    metadataSet?: string;
}
