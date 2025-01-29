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
import { CollectionProposalStatus, Node, ProposalNode } from 'ngx-edu-sharing-api';
import {
    LocalEventsService,
    TemporaryStorageService,
    UIAnimation,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { forkJoin as observableForkJoin } from 'rxjs';
import {
    CollectionReference,
    RestCollectionService,
    RestConstants,
    RestHelper,
    RestNodeService,
} from '../../core-module/core.module';
import { Toast } from '../../services/toast';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { OK_OR_CANCEL } from '../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { BridgeService } from '../../services/bridge.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { ErrorProcessingService } from './error.processing';

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
    @Output() onClose = new EventEmitter();
    @Output() onCreate = new EventEmitter();
    @Output() onRefresh = new EventEmitter<Node[] | void>();
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
            if (this.addToCollection != null) {
                this.cancelAddToCollection();
                event.preventDefault();
                event.stopPropagation();
                return;
            }
        }
    }
    public constructor(
        private bridge: BridgeService,
        private collectionService: RestCollectionService,
        private dialogs: DialogsService,
        private errorProcessing: ErrorProcessingService,
        private localEvents: LocalEventsService,
        private nodeHelper: NodeHelperService,
        private nodeService: RestNodeService,
        private router: Router,
        private temporaryStorage: TemporaryStorageService,
        private toast: Toast,
    ) {}

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
        void this.router.navigate([
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

    closeMaterialViewFeedback() {
        this.materialViewFeedback = null;
        this.materialViewFeedbackChange.emit(null);
    }

    declineProposals(nodes: ProposalNode[]) {
        void this.errorProcessing
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
        void this.errorProcessing
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
                void this.errorProcessing
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
