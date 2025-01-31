import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import {
    DialogButton,
    Node,
    RestConstants,
    RestNodeService,
} from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { MdsEditorWrapperComponent } from '../../../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { NodeTemplateDialogData, NodeTemplateDialogResult } from './node-template-dialog-data';

@Component({
    selector: 'es-node-template-dialog',
    templateUrl: './node-template-dialog.component.html',
    styleUrls: ['./node-template-dialog.component.scss'],
})
export class NodeTemplateDialogComponent implements OnInit {
    @ViewChild('mds') mdsRef: MdsEditorWrapperComponent;

    templateNode: Node;
    enabled: boolean;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: NodeTemplateDialogData,
        private dialogRef: CardDialogRef<NodeTemplateDialogData, NodeTemplateDialogResult>,
        private nodeService: RestNodeService,
        private toast: Toast,
    ) {
        this.dialogRef.patchState({ isLoading: true });
    }

    ngOnInit(): void {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close(null)),
                new DialogButton('SAVE', { color: 'primary' }, () => this.save()),
            ],
        });
        void this.initNode();
    }

    private async initNode() {
        const nodeId = this.data.node.ref.id;
        const template = await this.nodeService.getNodeTemplate(nodeId).toPromise();
        this.templateNode = template.node;
        this.enabled = template.enabled;

        if (!template.enabled) {
            // check if this is the first time opening -> activate it
            const aspects = (await this.nodeService.getNodeMetadata(nodeId).toPromise()).node
                .aspects;
            if (!aspects.includes(RestConstants.CCM_ASPECT_METADATA_PRESETTING)) {
                this.enabled = true;
            }
        }

        this.dialogRef.patchState({ isLoading: false });
        // @TODO this is only required for the legacy mds and might be removed as soon as the legacy
        // mds is obsolete.
        setTimeout(() => this.mdsRef.loadMds(true));
    }

    private async save() {
        const data = this.enabled ? await this.mdsRef.getValues() : {};
        this.dialogRef.patchState({ isLoading: true });
        this.nodeService.setNodeTemplate(this.data.node.ref.id, this.enabled, data).subscribe(
            () => {
                this.dialogRef.close(null);
                this.toast.toast('WORKSPACE.TOAST.METADATA_TEMPLATE_UPDATED');
            },
            (error) => {
                this.toast.error(error);
            },
        );
    }
}
