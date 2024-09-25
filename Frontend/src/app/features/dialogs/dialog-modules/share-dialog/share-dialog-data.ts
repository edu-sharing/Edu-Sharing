import { Acl } from 'ngx-edu-sharing-api';
import { Node } from '../../../../core-module/core.module';
import { ExtendedAcl } from './share-dialog.component';

export class ShareDialogData {
    /** Provide either a node objects or the node ids. */
    nodes: Node[] | string[];
    sendMessages? = true;
    sendToApi? = true;
    currentPermissions?: ExtendedAcl = null;
}

export interface ShareDialogResult {
    permissions: ExtendedAcl;
    notify: boolean;
    notifyMessage: string;
}
