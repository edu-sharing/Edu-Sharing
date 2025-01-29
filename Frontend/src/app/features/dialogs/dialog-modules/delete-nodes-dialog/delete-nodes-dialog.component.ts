import { Component, Inject, OnInit } from '@angular/core';
import {
    Node,
    NodePermissions,
    NodeService,
    SessionStorageService,
    Store,
} from 'ngx-edu-sharing-api';
import { Observable } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import {
    DialogButton,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestUsageService,
    TemporaryStorageService,
} from '../../../../core-module/core.module';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { Toast } from '../../../../services/toast';
import { ClipboardObject, LocalEventsService } from 'ngx-edu-sharing-ui';
import { forkJoinWithErrors } from '../../../../util/rxjs/forkJoinWithErrors';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { DeleteNodesDialogData, DeleteNodesDialogResult } from './delete-nodes-dialog-data';

@Component({
    selector: 'es-delete-nodes-dialog',
    templateUrl: './delete-nodes-dialog.component.html',
    styleUrls: ['./delete-nodes-dialog.component.scss'],
})
export class DeleteNodesDialogComponent implements OnInit {
    /** Message shown to the user in the dialog body. */
    message: string;
    /** Translation parameters for the message. */
    messageParams: { [key: string]: string };
    /** Whether the user is given the option to block further imports. */
    canBlockImport: boolean;
    /** Whether the user selected to block further imports. */
    shouldBlockImport: boolean;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: DeleteNodesDialogData,
        private dialogRef: CardDialogRef<DeleteNodesDialogData, DeleteNodesDialogResult>,
        private connector: RestConnectorService,
        private localEvents: LocalEventsService,
        private nodeHelper: NodeHelperService,
        private nodeService: NodeService,
        private temporaryStorage: TemporaryStorageService,
        private sessionStorageService: SessionStorageService,
        private toast: Toast,
        private usageService: RestUsageService,
    ) {
        this.dialogRef.patchState({ isLoading: true });
    }

    ngOnInit(): void {
        this.updateTitleAndMessage();
        this.canBlockImport = this.getCanBlockImports();
        this.shouldBlockImport = this.canBlockImport;
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.dialogRef.close()),
                new DialogButton('YES_DELETE', { color: 'danger' }, () => this.onConfirm()),
            ],
        });
    }

    /**
     * Updates the dialog title and sets `message` and `messageParams`.
     *
     * Sets the dialog's `isLoading` state to `false` when done.
     */
    private updateTitleAndMessage(): void {
        let waitingForResponse = false;
        let title: string;
        title = 'WORKSPACE.DELETE_TITLE' + (this.data.nodes.length === 1 ? '_SINGLE' : '');
        this.message = 'WORKSPACE.DELETE_MESSAGE' + (this.data.nodes.length === 1 ? '_SINGLE' : '');
        if (this.data.nodes.length === 1) {
            const node = this.data.nodes[0];
            const name = RestHelper.getName(node);
            this.messageParams = { name };
            if (node.collection) {
                title = 'WORKSPACE.DELETE_TITLE_COLLECTION';
                this.message = 'WORKSPACE.DELETE_MESSAGE_COLLECTION';
            } else if (this.nodeHelper.isNodePublishedCopy(node)) {
                title = 'WORKSPACE.DELETE_TITLE_PUBLISHED_COPY';
                this.message = 'WORKSPACE.DELETE_MESSAGE_PUBLISHED_COPY';
            } else if (node.mediatype === 'folder-link') {
                title = 'WORKSPACE.DELETE_TITLE_FOLDER_LINK';
                this.message = 'WORKSPACE.DELETE_MESSAGE_FOLDER_LINK';
            } else if (node.isDirectory) {
                // Check for usages and warn user
                waitingForResponse = true;
                this.usageService
                    .getNodeUsages(node.ref.id, node.ref.repo)
                    .subscribe(({ usages }) => {
                        if (usages.length > 0) {
                            this.message = 'WORKSPACE.DELETE_MESSAGE_SINGLE_USAGES';
                            this.messageParams = { name, usages: usages.length.toString() };
                        }
                        this.dialogRef.patchState({ isLoading: false });
                    });
            }
        }
        this.dialogRef.patchConfig({ title });
        if (!waitingForResponse) {
            setTimeout(() => {
                this.dialogRef.patchState({ isLoading: false });
            });
        }
    }

    private getCanBlockImports(): boolean {
        return (
            this.connector.getCurrentLogin()?.isAdmin &&
            this.data.nodes.every(
                (n) => n.properties[RestConstants.CCM_PROP_REPLICATIONSOURCE] != null,
            )
        );
    }

    private onConfirm(): void {
        this.dialogRef.patchState({ isLoading: true });
        void this.removeTemporaryCollections(this.data.nodes);
        forkJoinWithErrors(this.data.nodes.map((node) => this.processNode(node))).subscribe(
            ({ successes: processedNodes, errors }) => {
                if (errors.length === 0) {
                    this.toast.toast('WORKSPACE.TOAST.DELETE_FINISHED');
                }
                if (processedNodes.length > 0) {
                    this.dialogRef.close({
                        nodes: processedNodes,
                        action: this.shouldBlockImport ? 'changed' : 'deleted',
                    });
                } else {
                    this.dialogRef.close(null);
                }
                // If subscribers to the emitted events cause routing actions, the dialog could
                // interfere if still open. So we emit the events after closing the dialog.
                if (this.shouldBlockImport) {
                    this.localEvents.nodesChanged.emit(processedNodes);
                } else {
                    this.localEvents.nodesDeleted.emit(processedNodes);
                }
            },
        );
    }

    /**
     * Processes a single node that was confirmed for deletion.
     */
    private processNode(node: Node): Observable<Node> {
        let action: Observable<Node>;
        if (this.shouldBlockImport) {
            action = this.blockImport(node);
        } else {
            action = this.deleteNode(node);
        }
        return action.pipe(tap(() => this.removeNodeFromClipboard(node)));
    }

    private deleteNode(node: Node): Observable<Node> {
        return this.nodeService.deleteNode(node.ref.id).pipe(map(() => node));
    }

    private blockImport(node: Node): Observable<Node> {
        return this.nodeService
            .editNodeMetadata(
                node.ref.id,
                { [RestConstants.CCM_PROP_IMPORT_BLOCKED]: ['true'] },
                { versionComment: RestConstants.COMMENT_BLOCKED_IMPORT },
            )
            .pipe(
                switchMap((node) => {
                    const permissions: NodePermissions = { inherited: false, permissions: [] };
                    return this.nodeService.setPermissions(node.ref.id, permissions);
                }),
                map(() => node),
            );
    }

    private removeNodeFromClipboard(node: Node) {
        let clip = this.temporaryStorage.get('workspace_clipboard') as ClipboardObject;
        if (clip == null) {
            return;
        }
        for (const clipNode of clip.nodes) {
            if (clipNode.ref.id === node.ref.id) {
                clip.nodes.splice(clip.nodes.indexOf(clipNode), 1);
            }
            if (clip.nodes.length === 0) {
                this.temporaryStorage.remove('workspace_clipboard');
            }
        }
    }

    private async removeTemporaryCollections(nodes: Node[]) {
        const collections: Node[] = await this.sessionStorageService
            .get(SessionStorageService.KEY_ROOT_COLLECTIONS, [], Store.Session)
            .toPromise();
        console.log(collections);
        await this.sessionStorageService.set(
            SessionStorageService.KEY_ROOT_COLLECTIONS,
            collections.filter((c) => !nodes.find((n) => c.ref.id === n.ref.id)),
            Store.Session,
        );
    }
}
