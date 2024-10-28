import { CardDialogConfig } from '../../card-dialog/card-dialog-config';
import { Node } from 'ngx-edu-sharing-api';

export class CheckboxDialogData<P extends string = string> extends CardDialogConfig {
    nodes: Node[];
    /** Message to show in the dialog body. Will be translated. */
    message: string;
    /** Translation parameters for the given message text. */
    messageParameters?: { [key in P]: string };
    /** Label of the checkbox field. Will be translated. */
    label: string;
    /** state of the checkbox **/
    state?: boolean;
}

export type CheckboxDialogResult = boolean | null;
