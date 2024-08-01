import { Component, Inject, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
    LocalEventsService,
    WORKFLOW_STATUS_UNCHECKED,
    WorkflowDefinition,
} from 'ngx-edu-sharing-ui';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
    Authority,
    ConfigurationService,
    DialogButton,
    Node,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
    WorkflowEntry,
} from '../../../../core-module/core.module';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { Toast } from '../../../../services/toast';
import { AuthorityNamePipe } from '../../../../shared/pipes/authority-name.pipe';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { DialogsService } from '../../dialogs.service';
import { WorkflowDialogData, WorkflowDialogResult } from './workflow-dialog-data';
import { MatSelect } from '@angular/material/select';
import { AuthoritySearchInputComponent } from '../../../../shared/components/authority-search-input/authority-search-input.component';

type WorkflowReceiver = Authority;

@Component({
    selector: 'es-workflow-dialog',
    templateUrl: './workflow-dialog.component.html',
    styleUrls: ['./workflow-dialog.component.scss'],
})
export class WorkflowDialogComponent {
    @ViewChild('statusSelect') statusSelectRef: MatSelect;
    @ViewChild(AuthoritySearchInputComponent)
    authoritySearchInputComponentRef: AuthoritySearchInputComponent;
    readonly TYPE_EDITORIAL = RestConstants.GROUP_TYPE_EDITORIAL;

    comment: string;
    globalAllowed: boolean;
    history: WorkflowEntry[];
    receivers: WorkflowReceiver[] = [];
    status = WORKFLOW_STATUS_UNCHECKED;
    validStatus: WorkflowDefinition[];

    private initialStatus = WORKFLOW_STATUS_UNCHECKED;
    private nodes: Node[] = this.data.nodes;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: WorkflowDialogData,
        private dialogRef: CardDialogRef<WorkflowDialogData, WorkflowDialogResult>,
        private config: ConfigurationService,
        private connector: RestConnectorService,
        private dialogs: DialogsService,
        private iam: RestIamService,
        private localEvents: LocalEventsService,
        private nodeHelper: NodeHelperService,
        private nodeService: RestNodeService,
        private toast: Toast,
        private translate: TranslateService,
    ) {
        this.updateButtons();
        this.connector
            .hasToolPermission(RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH)
            .subscribe((has: boolean) => (this.globalAllowed = has));
        this.config.getAll().subscribe(async () => {
            this.validStatus = this.nodeHelper.getWorkflows();
            const receiver = this.config.instant('workflow.defaultReceiver');
            if (receiver) {
                try {
                    this.receivers = [(await this.iam.getGroup(receiver).toPromise()).group];
                    this.statusSelectRef.focus();
                } catch (e) {
                    toast.clientConfigError('workflow.defaultReceiver', 'group not found');
                }
            }
        });
        void this.initNodes(this.data.nodes);
    }

    isAllowedAsNext(status: WorkflowDefinition) {
        if (!this.initialStatus.next) {
            return true;
        }
        if (this.initialStatus.id === status.id) {
            return true;
        }
        return this.initialStatus.next.indexOf(status.id) !== -1;
    }

    setStatus(status: WorkflowDefinition) {
        if (!this.isAllowedAsNext(status)) {
            return;
        }
        this.status = status;
        this.updateButtons();
    }

    addSuggestion(data: Authority) {
        this.receivers = [data];
        this.updateButtons();
    }

    removeReceiver(data: WorkflowReceiver) {
        const pos = this.receivers.indexOf(data);
        if (pos !== -1) {
            this.receivers.splice(pos, 1);
        }
        this.updateButtons();
    }

    private hasChanges() {
        return this.statusChanged() || this.receiversChanged();
    }

    private async saveWorkflow() {
        if (
            !this.comment &&
            this.receiversChanged() &&
            this.config.instant('workflow.commentRequired', true)
        ) {
            this.toast.error(null, 'WORKSPACE.WORKFLOW.NO_COMMENT');
            return;
        }
        const receivers = this.status.hasReceiver ? this.receivers : [];
        if (receivers.length) {
            const hasPermission = await this.requestReceiverPermissionIfNeeded(this.receivers[0]);
            if (!hasPermission) {
                return;
            }
        } else if (this.status.hasReceiver) {
            this.toast.error(null, 'WORKSPACE.WORKFLOW.NO_RECEIVER');
            return;
        }
        this.saveWorkflowFinal(receivers);
    }

    private cancel() {
        this.dialogRef.close(null);
    }

    private async initNodes(nodes: Node[]): Promise<void> {
        if (!nodes || nodes.length === 0) {
            return;
        }
        this.dialogRef.patchState({ isLoading: true });
        try {
            await this.initNodesInner(nodes);
        } catch (error) {
            this.toast.error(error);
            this.cancel();
        } finally {
            this.dialogRef.patchState({ isLoading: false });
            this.updateButtons();
        }
    }

    private async initNodesInner(nodes: Node[]): Promise<void> {
        this.nodes = await this.fetchCompleteNodes(nodes).toPromise();
        const histories = await forkJoin(
            nodes.map((node) => this.nodeService.getWorkflowHistory(node.ref.id)),
        ).toPromise();
        if (nodes.length > 1) {
            if (histories.some((history) => history.length > 0)) {
                this.toast.error(null, 'WORKSPACE.WORKFLOW.BULK_WORKFLOWS_EXIST');
                this.cancel();
                return;
            } else {
                this.history = [];
                this.receivers = [];
                ({ current: this.status, initial: this.initialStatus } =
                    this.nodeHelper.getDefaultWorkflowStatus(true));
            }
        } else {
            this.history = histories[0];
            if (this.history.length) {
                this.receivers = this.history[0].receiver as Authority[];
            }
            if (!this.receivers || (this.receivers.length === 1 && !this.receivers[0])) {
                this.receivers = [];
                this.authoritySearchInputComponentRef.inputElement.nativeElement.focus();
            } else {
                this.statusSelectRef.focus();
            }
            ({ current: this.status, initial: this.initialStatus } =
                this.nodeHelper.getWorkflowStatus(this.nodes[0], true));
        }
    }

    private fetchCompleteNodes(nodes: Node[]): Observable<Node[]> {
        return forkJoin(
            nodes.map((node) =>
                this.nodeService
                    .getNodeMetadata(node.ref.id, [RestConstants.ALL])
                    .pipe(map((nodeWrapper) => nodeWrapper.node)),
            ),
        );
    }

    private saveWorkflowFinal(receivers: WorkflowReceiver[]) {
        const entry = new WorkflowEntry();
        const receiversClean: any[] = [];
        for (const r of receivers) {
            receiversClean.push({ authorityName: r.authorityName });
        }
        entry.receiver = receiversClean;
        entry.comment = this.comment;
        entry.status = this.status.id;
        this.dialogRef.patchState({ isLoading: true });
        forkJoin(
            this.nodes.map((node) => this.nodeService.addWorkflow(node.ref.id, entry)),
        ).subscribe(
            () => {
                this.toast.toast('WORKSPACE.TOAST.WORKFLOW_UPDATED');
                this.fetchCompleteNodes(this.nodes).subscribe(
                    (nodes) => {
                        this.afterSaved(nodes);
                    },
                    (error) => {
                        this.toast.error(error);
                        this.dialogRef.patchState({ isLoading: false });
                    },
                );
            },
            (error) => {
                this.toast.error(error);
                this.dialogRef.patchState({ isLoading: false });
            },
        );
    }

    private afterSaved(nodes: Node[]): void {
        this.localEvents.nodesChanged.emit(nodes);
        this.dialogRef.close(null);
    }

    private receiversChanged() {
        if (this.nodes.length > 1) {
            return this.receivers.length !== 0;
        } else {
            const prop: string[] = this.nodes[0].properties[RestConstants.CCM_PROP_WF_RECEIVER];
            if (prop) {
                if (prop.length !== this.receivers.length) {
                    return true;
                }
                for (const receiver of this.receivers) {
                    if (prop.indexOf(receiver.authorityName) === -1) {
                        return true;
                    }
                }
                return false;
            }
            return this.receivers.length > 0;
        }
    }

    private statusChanged() {
        return this.status.id !== this.initialStatus.id;
    }

    private updateButtons() {
        const save = new DialogButton('WORKSPACE.WORKFLOW.SAVE', { color: 'primary' }, () =>
            this.saveWorkflow(),
        );
        save.disabled = !this.hasChanges();
        const buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            save,
        ];
        this.dialogRef.patchConfig({ buttons });
    }

    /**
     * Checks if the given receiver has the 'coordinator' permission and requests to grant it to
     * them if not.
     *
     * @returns `true` if the receiver had or was granted the permission
     */
    private async requestReceiverPermissionIfNeeded(receiver: WorkflowReceiver): Promise<boolean> {
        const permissionsList = await forkJoin(
            this.nodes.map((node) =>
                this.nodeService.getNodePermissionsForUser(node.ref.id, receiver.authorityName),
            ),
        ).toPromise();
        const nodesMissingPermission = this.nodes.filter(
            (_, index) => !permissionsList[index].includes(RestConstants.PERMISSION_COORDINATOR),
        );
        if (nodesMissingPermission.length === 0) {
            return true;
        } else {
            const shouldGrantPermission = await this.requestReceiverPermissionDialog(receiver);
            if (shouldGrantPermission) {
                await this.grantReceiverPermission(nodesMissingPermission, receiver);
            }
            return shouldGrantPermission;
        }
    }

    /**
     * Shows a dialog to the user, asking them whether to grant missing permissions to the receiver.
     *
     * @returns `true` if the user decided to grant the permissions.
     */
    private async requestReceiverPermissionDialog(receiver: WorkflowReceiver): Promise<boolean> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'WORKSPACE.WORKFLOW.USER_NO_PERMISSION',
            message: 'WORKSPACE.WORKFLOW.USER_NO_PERMISSION_INFO',
            messageParameters: {
                user: new AuthorityNamePipe(this.translate).transform(receiver, null),
            },
            messageMode: 'html',
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                { label: 'WORKSPACE.WORKFLOW.PROCEED', config: { color: 'primary' } },
            ],
        });
        const response = await dialogRef.afterClosed().toPromise();
        return response === 'WORKSPACE.WORKFLOW.PROCEED';
    }

    private async grantReceiverPermission(
        nodes: Node[],
        receiver: WorkflowReceiver,
    ): Promise<void> {
        this.dialogRef.patchState({ isLoading: true });
        try {
            await Promise.all(nodes.map((node) => this.addWritePermission(receiver, node)));
        } catch (error) {
            this.toast.error(error);
        }
    }

    private async addWritePermission(authority: WorkflowReceiver, node: Node): Promise<void> {
        const nodePermissions = await this.nodeService.getNodePermissions(node.ref.id).toPromise();
        const permissions = RestHelper.addCoordinatorPermission(nodePermissions, authority);
        await this.nodeService.setNodePermissions(node.ref.id, permissions, false).toPromise();
    }
}
