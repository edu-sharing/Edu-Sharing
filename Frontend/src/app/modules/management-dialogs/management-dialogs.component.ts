import { trigger } from '@angular/animations';
import {
    Component,
    ContentChild,
    EventEmitter,
    HostListener,
    Input,
    Output,
    TemplateRef,
} from '@angular/core';
import { Router } from '@angular/router';
import { CollectionProposalStatus, ProposalNode } from 'ngx-edu-sharing-api';
import {
    LocalEventsService,
    TemporaryStorageService,
    UIAnimation,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { forkJoin as observableForkJoin } from 'rxjs';
import { ErrorProcessingService } from 'src/app/core-ui-module/error.processing';
import { BridgeService } from '../../core-bridge-module/bridge.service';
import {
    CollectionReference,
    Node,
    NodeVersions,
    RestCollectionService,
    RestConstants,
    RestHelper,
    RestNodeService,
    Version,
} from '../../core-module/core.module';
import { NodeHelperService } from '../../core-ui-module/node-helper.service';
import { Toast } from '../../core-ui-module/toast';
import { UIHelper } from '../../core-ui-module/ui-helper';
import {
    OK_OR_CANCEL,
    YES_OR_NO,
} from '../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { BulkBehavior } from '../../features/mds/types/types';

export enum DialogType {
    SimpleEdit = 'SimpleEdit',
    Mds = 'Mds',
}
export enum ManagementEventType {
    AddCollectionNodes,
}
export interface ManagementEvent {
    event: ManagementEventType;
    data?: any;
}
@Component({
    selector: 'es-workspace-management',
    templateUrl: 'management-dialogs.component.html',
    styleUrls: ['management-dialogs.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('fromLeft', UIAnimation.fromLeft()),
        trigger('fromRight', UIAnimation.fromRight()),
    ],
})
export class WorkspaceManagementDialogsComponent {
    @ContentChild('collectionChooserBeforeRecent')
    collectionChooserBeforeRecentRef: TemplateRef<any>;
    @Input() addToCollection: Node[];
    @Output() addToCollectionChange = new EventEmitter();
    @Output() onEvent = new EventEmitter<ManagementEvent>();
    @Input() addNodesStream: Node[];
    @Output() addNodesStreamChange = new EventEmitter();
    @Input() nodeSimpleEditChange = new EventEmitter<Node[]>();
    @Input() materialViewFeedback: Node;
    @Output() materialViewFeedbackChange = new EventEmitter<Node>();
    @Input() nodeSidebar: Node;
    @Output() nodeSidebarChange = new EventEmitter<Node>();
    @Output() onClose = new EventEmitter();
    @Output() onCreate = new EventEmitter();
    @Output() onRefresh = new EventEmitter<Node[] | void>();
    @Output() onCloseMetadata = new EventEmitter();
    @Output() onCloseAddToCollection = new EventEmitter();
    @Output() onStoredAddToCollection = new EventEmitter<{
        collection: Node;
        references: CollectionReference[];
    }>();
    public editorPending = false;
    /**
     * QR Code object data to print
     * @node: Reference to the node (for header title)
     * @data: The string to display inside the qr code (e.g. an url)
     */
    //   @Input() qr: {node: Node, data: string};

    /**
     * @Deprecated, the components should use toast service directly
     */
    set globalProgress(globalProgress: boolean) {
        if (globalProgress) {
            this.toast.showProgressSpinner();
        } else {
            this.toast.closeProgressSpinner();
        }
    }

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            if (this.nodeSidebar != null) {
                this.closeSidebar();
                event.preventDefault();
                event.stopPropagation();
                return;
            }
            if (this.addToCollection != null) {
                this.cancelAddToCollection();
                event.preventDefault();
                event.stopPropagation();
                return;
            }
        }
    }
    public constructor(
        private nodeService: RestNodeService,
        private temporaryStorage: TemporaryStorageService,
        private collectionService: RestCollectionService,
        private toast: Toast,
        private errorProcessing: ErrorProcessingService,
        private nodeHelper: NodeHelperService,
        private bridge: BridgeService,
        private router: Router,
        private dialogs: DialogsService,
        private localEvents: LocalEventsService,
    ) {}

    async openMdsEditor(nodes: Node[]): Promise<void> {
        const dialogRef = await this.dialogs.openMdsEditorDialogForNodes({
            nodes,
            bulkBehavior: BulkBehavior.Default,
        });
        dialogRef
            .afterClosed()
            .subscribe((updatedNodes) => this.closeMdsEditor(nodes, updatedNodes));
    }

    private closeMdsEditor(originalNodes: Node[], updatedNodes: Node[] = null) {
        let refresh = !!updatedNodes;
        this.onCloseMetadata.emit(updatedNodes);
        if (
            this.nodeSidebar &&
            this.nodeSidebar.ref.id === originalNodes[0]?.ref.id &&
            updatedNodes
        ) {
            this.nodeSidebar = updatedNodes[0];
        }
        if (refresh) {
            this.localEvents.nodesChanged.emit(updatedNodes);
        }
    }

    public closeStream() {
        this.addNodesStream = null;
        this.addNodesStreamChange.emit(null);
    }
    cancelAddToCollection() {
        this.addToCollection = null;
        this.addToCollectionChange.emit(null);
        this.onCloseAddToCollection.emit();
    }
    public addToCollectionCreate(parent: Node = null) {
        this.temporaryStorage.set(TemporaryStorageService.COLLECTION_ADD_NODES, {
            nodes: this.addToCollection,
            callback: this.onStoredAddToCollection,
        });
        this.router.navigate([
            UIConstants.ROUTER_PREFIX,
            'collections',
            'collection',
            'new',
            parent ? parent.ref.id : RestConstants.ROOT,
        ]);
        this.addToCollection = null;
        this.addToCollectionChange.emit(null);
    }
    public async addToCollectionList(
        collection: Node,
        list: Node[] = this.addToCollection,
        close = true,
        callback: () => void = null,
        asProposal = false,
        force = false,
    ) {
        if (!force) {
            if (collection.access.indexOf(RestConstants.ACCESS_WRITE) === -1) {
                const dialogRef = await this.dialogs.openGenericDialog({
                    title: 'DIALOG.COLLECTION_PROPSE',
                    message: 'DIALOG.COLLECTION_PROPSE_INFO',
                    messageParameters: { collection: RestHelper.getTitle(collection) },
                    buttons: OK_OR_CANCEL,
                });
                dialogRef.afterClosed().subscribe((response) => {
                    if (response === 'OK') {
                        void this.addToCollectionList(
                            collection,
                            list,
                            close,
                            callback,
                            true,
                            true,
                        );
                    }
                });
                return;
            } else if (collection.collection.scope !== RestConstants.COLLECTIONSCOPE_MY) {
                const dialogRef = await this.dialogs.openGenericDialog({
                    title: 'DIALOG.COLLECTION_SHARE_PUBLIC',
                    message: 'DIALOG.COLLECTION_SHARE_PUBLIC_INFO',
                    messageParameters: { collection: RestHelper.getTitle(collection) },
                    buttons: OK_OR_CANCEL,
                });
                dialogRef.afterClosed().subscribe((response) => {
                    if (response === 'OK') {
                        void this.addToCollectionList(
                            collection,
                            list,
                            close,
                            callback,
                            asProposal,
                            true,
                        );
                    }
                });
                return;
            }
        }
        if (close) {
            this.cancelAddToCollection();
        } else {
            this.toast.closeProgressSpinner();
        }
        this.toast.showProgressSpinner();
        UIHelper.addToCollection(
            this.nodeHelper,
            this.collectionService,
            this.router,
            this.bridge,
            collection,
            list,
            asProposal,
            (nodes) => {
                this.toast.closeProgressSpinner();
                this.onStoredAddToCollection.emit({ collection, references: nodes });
                if (callback) {
                    callback();
                }
            },
        );
    }

    async restoreVersion(restore: { version: Version; node: Node }) {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'WORKSPACE.METADATA.RESTORE_TITLE',
            message: 'WORKSPACE.METADATA.RESTORE_MESSAGE',
            buttons: YES_OR_NO,
            nodes: [restore.node],
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'YES') {
                this.doRestoreVersion(restore.version);
            }
        });
    }
    private doRestoreVersion(version: Version): void {
        this.toast.showProgressSpinner();
        this.nodeService
            .revertNodeToVersion(
                version.version.node.id,
                version.version.major,
                version.version.minor,
            )
            .subscribe(
                (data: NodeVersions) => {
                    this.toast.closeProgressSpinner();
                    this.closeSidebar();
                    // @TODO type is not compatible
                    this.nodeService
                        .getNodeMetadata(version.version.node.id, [RestConstants.ALL])
                        .subscribe(
                            (node) => {
                                this.localEvents.nodesChanged.emit([node.node]);
                                this.nodeSidebar = node.node;
                                this.nodeSidebarChange.emit(node.node);
                                this.toast.toast('WORKSPACE.REVERTED_VERSION');
                            },
                            (error: any) => this.toast.error(error),
                        );
                },
                (error: any) => this.toast.error(error),
            );
    }

    closeMaterialViewFeedback() {
        this.materialViewFeedback = null;
        this.materialViewFeedbackChange.emit(null);
    }

    closeSidebar() {
        this.nodeSidebar = null;
        this.nodeSidebarChange.emit(null);
    }

    displayNode(node: Node) {
        if (node.version) {
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', node.ref.id, node.version]);
        } else {
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', node.ref.id]);
        }
    }

    declineProposals(nodes: ProposalNode[]) {
        this.errorProcessing
            .handleRestRequest(
                observableForkJoin(
                    nodes.map((n) =>
                        this.nodeService.editNodeProperty(
                            n.proposal?.ref.id ||
                                (n.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL
                                    ? n.ref.id
                                    : null),
                            RestConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS,
                            ['DECLINED' as CollectionProposalStatus],
                        ),
                    ),
                ),
            )
            .then(() => {
                this.toast.toast('COLLECTIONS.PROPOSALS.TOAST.DECLINED');
                this.localEvents.nodesDeleted.emit(nodes);
            });
    }

    addProposalsToCollection(nodes: ProposalNode[]) {
        this.errorProcessing
            .handleRestRequest(
                observableForkJoin(
                    nodes.map((n) =>
                        this.nodeService.editNodeProperty(
                            n.proposal?.ref.id ||
                                (n.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL
                                    ? n.ref.id
                                    : null),
                            RestConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS,
                            ['ACCEPTED' as CollectionProposalStatus],
                        ),
                    ),
                ),
            )
            .then(() => {
                this.errorProcessing
                    .handleRestRequest(
                        observableForkJoin(
                            nodes.map((n) =>
                                this.collectionService.addNodeToCollection(
                                    n.proposalCollection.ref.id,
                                    n.ref.id,
                                    n.ref.repo,
                                ),
                            ),
                        ),
                    )
                    .then((results) => {
                        this.toast.toast('COLLECTIONS.PROPOSALS.TOAST.ACCEPTED');
                        this.localEvents.nodesDeleted.emit(nodes);
                        this.onEvent.emit({
                            event: ManagementEventType.AddCollectionNodes,
                            data: {
                                collection: nodes[0].proposalCollection,
                                references: results.map((r) => r.node),
                            },
                        });
                    });
            });
    }
}
