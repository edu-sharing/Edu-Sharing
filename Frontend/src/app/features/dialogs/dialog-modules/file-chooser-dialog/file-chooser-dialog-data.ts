import { Node } from 'ngx-edu-sharing-api';

export class FileChooserDialogData {
    /**
     * The caption of the dialog, will be translated automatically.
     */
    title: string;
    /**
     * The subtitle of the dialog. Will be auto-filled if left empty.
     */
    subtitle: string;
    /**
     * Set true if the user needs write permissions to the target file.
     */
    writeRequired = false;
    /**
     * Set true if the user should pick a collection, not a regular node.
     */
    collections = false;
    /**
     * Set to true if the user should pick a directory.
     */
    pickDirectory = false;
    /**
     * Filter for individual file types, please see @RestNodeService.getChildren().
     */
    filter: string[] = [];
}

export type FileChooserDialogResult = Node[] | null;
