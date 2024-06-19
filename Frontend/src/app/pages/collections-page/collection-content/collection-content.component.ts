import {
    Component,
    ContentChild,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    AuthenticationService,
    ConfigService,
    MdsService,
    Node,
    ProposalNode,
} from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    CanDrop,
    DragData,
    DropSource,
    FetchEvent,
    InteractionType,
    ListEventInterface,
    ListItem,
    ListItemSort,
    ListSortConfig,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodesRightMode,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { Subject, forkJoin as observableForkJoin, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import * as EduData from '../../../core-module/core.module';
import {
    CollectionReference,
    ConfigurationHelper,
    LoginResult,
    NodeWrapper,
    Permission,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { MdsHelperService } from 'ngx-edu-sharing-ui';
import { RequestObject } from '../../../core-module/rest/request-object';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestHelper } from '../../../core-module/rest/rest-helper';
import { RestCollectionService } from '../../../core-module/rest/services/rest-collection.service';
import { RestNodeService } from '../../../core-module/rest/services/rest-node.service';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { NodeHelperService } from '../../../services/node-helper.service';
import { Toast } from '../../../services/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import {
    ManagementEvent,
    ManagementEventType,
} from '../../../features/management-dialogs/management-dialogs.component';
import { LoadingScreenService } from '../../../main/loading-screen/loading-screen.service';
import { MainNavService } from '../../../main/navigation/main-nav.service';
import { BridgeService } from '../../../services/bridge.service';
import { CollectionInfoBarComponent } from '../collection-info-bar/collection-info-bar.component';
import { InfobarService } from '../infobar/infobar.service';

@Component({
    selector: 'es-collection-content',
    templateUrl: 'collection-content.component.html',
    styleUrls: ['collection-content.component.scss'],
})
export class CollectionContentComponent implements OnChanges, OnInit, OnDestroy {
    private static DEFAULT_REQUEST = {
        sortBy: [
            RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
            RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
            RestConstants.CM_MODIFIED_DATE,
        ],
        sortAscending: [false, true, false],
    };
    referencesDisplayType = NodeEntriesDisplayType.Grid;
    private readonly destroyed$ = new Subject<void>();
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;

    @Input() collection: Node;
    /**
     * you can subscribe to the clickItem event in case if you want to use emitter
     */
    @Input() interactionType: InteractionType = InteractionType.DefaultActionLink;
    @Input() scope: string;
    /**
     * reference to the infobar component
     * this is required if you want to interconnect it to have support for editing & managing the collection
     * (the options service is inited in the content component)
     */
    @Input() getInfobar: () => CollectionInfoBarComponent;
    @Input() isRootLevel: boolean;
    @Input() createAllowed: () => boolean;
    @Output() clickItem = new EventEmitter<NodeClickEvent<Node | CollectionReference>>();
    @ContentChild('empty') emptyRef: TemplateRef<unknown>;
    @ViewChild('actionbarReferences') actionbarReferences: ActionbarComponent;
    @ViewChild('listReferences') listReferences: ListEventInterface<CollectionReference>;

    private mainNavUpdateTrigger = new Subject<void>();
    sortCollectionColumns: ListItemSort[] = [
        new ListItemSort('NODE', RestConstants.CM_PROP_TITLE),
        new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
        new ListItemSort('NODE', RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION, 'ascending'),
    ];
    createSubCollectionOptionItem = new OptionItem('OPTIONS.NEW_COLLECTION', 'layers', () =>
        this.onCreateCollection(),
    );
    sortReferences: ListSortConfig = {
        active: null,
        direction: 'asc',
        columns: [
            // new ListItemSort('NODE', RestConstants.LOM_PROP_TITLE),
            new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
            new ListItemSort('NODE', RestConstants.CM_PROP_C_CREATED),
            new ListItemSort(
                'NODE',
                RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION,
                'ascending',
            ),
        ],
    };
    sortCollections: ListSortConfig = {
        active: null,
        direction: 'asc',
        columns: [
            new ListItemSort('NODE', RestConstants.CM_PROP_TITLE),
            new ListItemSort('NODE', RestConstants.CM_PROP_C_CREATED),
            new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
            new ListItemSort(
                'NODE',
                RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION,
                'ascending',
            ),
        ],
    };
    addMaterialSearchOptionItem = new OptionItem('OPTIONS.SEARCH_OBJECT', 'redo', () => {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            params.addToCollection = this.collection.ref.id;
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    });
    addMaterialBinaryOptionItem = new OptionItem('OPTIONS.ADD_OBJECT', 'cloud_upload', () => {
        this.mainNavService.getMainNav().topBar.createMenu.openUploadSelect();
    });
    dataSourceCollections = new NodeDataSource<Node>();
    dataSourceReferences = new NodeDataSource<CollectionReference>();
    collectionsColumns: ListItem[] = [];
    referencesColumns: ListItem[] = [];
    private loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed$ });

    private contentNode: Node;
    permissions: Permission[];
    login: LoginResult;

    constructor(
        private authenticationService: AuthenticationService,
        private bridge: BridgeService,
        private collectionService: RestCollectionService,
        private configurationService: ConfigService,
        private dialogs: DialogsService,
        private infobar: InfobarService,
        private loadingScreen: LoadingScreenService,
        private mainNavService: MainNavService,
        private mdsService: MdsService,
        private nodeHelper: NodeHelperService,
        private nodeService: RestNodeService,
        private optionsService: OptionsHelperDataService,
        private route: ActivatedRoute,
        private router: Router,
        private toast: Toast,
        private translation: TranslateService,
        private uiService: UIService,
    ) {
        this.sortCollectionColumns[this.sortCollectionColumns.length - 1].mode = 'ascending';
        // this.collectionSortEmitter.subscribe((sort: SortEvent) => this.setCollectionSort(sort));
        // this.collectionCustomSortEmitter.subscribe((state: boolean) => state ? this.toggleCollectionsOrder() : this.changeCollectionsOrder());
        // this.referenceSortEmitter.subscribe((sort: SortEvent) => this.setReferenceSort(sort));
        // this.referenceCustomSortEmitter.subscribe((state: boolean) => state ? this.toggleReferencesOrder() : this.changeReferencesOrder());
        this.collectionsColumns.push(new ListItem('COLLECTION', 'title'));
        this.collectionsColumns.push(new ListItem('COLLECTION', 'info'));
        this.collectionsColumns.push(new ListItem('COLLECTION', 'scope'));

        this.mainNavService.getDialogs().onEvent.subscribe((event: ManagementEvent) => {
            if (event.event === ManagementEventType.AddCollectionNodes) {
                if (event.data.collection.ref.id === this.collection.ref.id) {
                    this.listReferences.addVirtualNodes(event.data.references);
                }
            }
        });
        this.authenticationService
            .observeLoginInfo()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((login) => {
                this.login = login;
                this.addMaterialBinaryOptionItem.isEnabled = login.toolPermissions?.includes(
                    RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
                );
                this.createSubCollectionOptionItem.isEnabled = login.toolPermissions?.includes(
                    RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
                );
            });
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    async ngOnInit() {
        const mdsSets = await ConfigurationHelper.getAvailableMds(
            RestConstants.HOME_REPOSITORY,
            this.mdsService,
            this.configurationService,
        );
        const set = await this.mdsService
            .getMetadataSet({ metadataSet: mdsSets[0].id })
            .toPromise();
        this.referencesColumns = MdsHelperService.getColumns(
            this.translation,
            set,
            'collectionReferences',
        );

        this.optionsService.clearComponents(this.actionbarReferences);
        this.registerMainNav();
        this.mainNavUpdateTrigger.next();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.collection.currentValue) {
            this.dataSourceCollections.reset();
            this.dataSourceReferences.reset();

            this.createSubCollectionOptionItem.name =
                'OPTIONS.' + (this.isRootLevel ? 'NEW_COLLECTION' : 'NEW_SUB_COLLECTION');
            if (this.isRootLevel) {
                // display root collections with tabs

                // Use hardcoded sorting for root collection.
                this.sortCollections.active = RestConstants.CM_MODIFIED_DATE;
                this.sortCollections.direction = 'desc';
                // To respect sort configuration of the mds, we would need to wait for it here.
                //
                // const sort = metadataSet.sorts.find(sort => sort.id === 'collections');
                // this.sortCollections.active = sort?.default?.sortBy ?? RestConstants.CM_MODIFIED_DATE;
                // this.sortCollections.direction = sort?.default?.sortAscending ? 'asc' : 'desc';
                this.refreshContent();
            } else {
                // load metadata of collection
                this.dataSourceCollections.isLoading = true;
                this.dataSourceReferences.isLoading = true;
                // set the collection and load content data by refresh
                const orderCollections =
                    this.collection.properties[
                        RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE
                    ];
                this.sortCollections.active =
                    orderCollections?.[0] || RestConstants.CM_MODIFIED_DATE;

                this.sortCollections.direction =
                    orderCollections?.[0] === RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION
                        ? 'asc'
                        : orderCollections?.[1] === 'true'
                        ? 'asc'
                        : 'desc';

                const refMode = this.collection.collection.orderMode;
                const refAscending = this.collection.collection.orderAscending;
                // cast old order mode to new parameter
                this.sortReferences.active = ((refMode ===
                RestConstants.COLLECTION_ORDER_MODE_CUSTOM
                    ? RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION
                    : refMode) || RestConstants.CM_MODIFIED_DATE) as any;
                this.sortReferences.direction =
                    this.sortReferences.active === RestConstants.COLLECTION_ORDER_MODE_CUSTOM
                        ? 'asc'
                        : refAscending
                        ? 'asc'
                        : 'desc';
                this.mainNavUpdateTrigger.next();
                this.dataSourceCollections.isLoading = false;
                this.setOptionsCollection();
                this.refreshContent();
                if (
                    this.collection.access.indexOf(RestConstants.ACCESS_CHANGE_PERMISSIONS) !== -1
                ) {
                    this.nodeService
                        .getNodePermissions(this.collection.ref.id)
                        .subscribe((permissions) => {
                            this.permissions =
                                permissions.permissions.localPermissions.permissions.concat(
                                    permissions.permissions.inheritedPermissions,
                                );
                        });
                }
            }
        }
        // update the main nav buttons & availability
        this.mainNavUpdateTrigger.next();
    }

    isUserAllowedToEdit(collection: Node) {
        return RestHelper.isUserAllowedToEdit(collection);
    }

    isAllowedToEditCollection() {
        if (this.isRootLevel) {
            return !this.login?.isGuest; //this.tabSelected === RestConstants.COLLECTIONSCOPE_MY
        }
        return RestHelper.hasAccessPermission(this.collection, RestConstants.PERMISSION_WRITE);
    }
    onCreateCollection() {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            this.router.navigate(
                [
                    UIConstants.ROUTER_PREFIX + 'collections/collection',
                    'new',
                    this.collection.ref.id,
                ],
                { queryParams: params },
            );
        });
    }
    canDropOnCollectionBreadcrumbs = (dropData: DragData<Node | 'HOME'>): CanDrop => {
        const nodes = dropData.draggedNodes;
        const target = dropData.target;
        if (target === 'HOME') {
            const accept =
                dropData.action === 'move' &&
                nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_COLLECTION) !== -1 &&
                this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_WRITE);
            return {
                accept,
                denyExplicit: !accept,
            };
        }
        if (
            nodes[0].ref.id === (target as Node).ref.id ||
            (target as Node).ref.id === this.collection.ref.id ||
            (nodes[0].collection && dropData.action === 'copy')
        ) {
            return {
                accept: false,
            };
        }
        // do not allow to move anything else than editorial collections into editorial collections (if the source is a collection)
        if (nodes[0].collection?.hasOwnProperty('childCollectionsCount')) {
            if (
                (nodes[0].collection.type === RestConstants.COLLECTIONTYPE_EDITORIAL &&
                    (target as Node).collection.type !== RestConstants.COLLECTIONTYPE_EDITORIAL) ||
                (nodes[0].collection.type !== RestConstants.COLLECTIONTYPE_EDITORIAL &&
                    (target as Node).collection.type === RestConstants.COLLECTIONTYPE_EDITORIAL)
            ) {
                return {
                    accept: false,
                };
            }
        }
        if (
            (dropData.action === 'copy' &&
                !this.nodeHelper.getNodesRight(
                    nodes,
                    RestConstants.ACCESS_CC_PUBLISH,
                    NodesRightMode.Effective,
                )) ||
            (dropData.action === 'move' &&
                !this.nodeHelper.getNodesRight(
                    nodes,
                    RestConstants.ACCESS_WRITE,
                    NodesRightMode.Local,
                ))
        ) {
            return {
                accept: false,
            };
        }

        return {
            accept: this.nodeHelper.getNodesRight(
                [target],
                RestConstants.ACCESS_WRITE,
                NodesRightMode.Local,
            ),
        };
    };

    canDropOnCollection = (dropData: DragData<Node>): CanDrop => {
        return this.canDropOnCollectionBreadcrumbs(dropData);
    };

    canDropOnRef = (dragData: DragData<CollectionReference>): CanDrop => {
        // do not allow to drop here
        return {
            accept: false,
        };
    };

    dropOnRef = (target: Node, source: DropSource<CollectionReference>) => {
        return;
    };
    dropOnCollection = (target: Node | 'HOME', source: DropSource<Node>) => {
        if (source.element[0] === target) {
            return;
        }
        this.toast.showProgressSpinner();
        if (source.element[0].mediatype === 'collection') {
            if (source.mode === 'copy') {
                this.toast.error(null, 'INVALID_OPERATION');
                this.toast.closeProgressSpinner();
                return;
            }
            this.nodeService
                .moveNode(
                    (target as Node)?.ref?.id || RestConstants.COLLECTIONHOME,
                    source.element[0].ref.id,
                )
                .subscribe(
                    () => {
                        this.toast.closeProgressSpinner();
                        this.refreshContent();
                    },
                    (error) => {
                        this.handleError(error);
                        this.toast.closeProgressSpinner();
                    },
                );
        } else {
            if (source.mode === 'copy') {
                UIHelper.addToCollection(
                    this.nodeHelper,
                    this.collectionService,
                    this.router,
                    this.bridge,
                    target as Node,
                    source.element,
                    false,
                    (nodes) => {
                        this.toast.closeProgressSpinner();
                        this.refreshContent();
                    },
                );
            } else if (source.mode === 'move') {
                forkJoin(
                    source.element.map((toMove) =>
                        this.nodeService.moveNode(
                            (target as Node)?.ref?.id || RestConstants.COLLECTIONHOME,
                            toMove.ref.id,
                        ),
                    ),
                ).subscribe(
                    () => {
                        this.toast.closeProgressSpinner();
                        this.refreshContent();
                    },
                    (error) => {
                        this.handleError(error);
                        this.toast.closeProgressSpinner();
                    },
                );
            } else {
                this.toast.error(null, 'INVALID_OPERATION');
            }
        }
    };
    private handleError(error: any) {
        if (error.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
            this.toast.error(null, 'COLLECTIONS.ERROR_NODE_EXISTS');
        } else {
            this.toast.error(error);
        }
    }

    async onContentClick(event: NodeClickEvent<CollectionReference | ProposalNode>): Promise<void> {
        this.contentNode = event.element;
        if (event.element.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL) {
            this.clickElementEvent(event);
        } else if ((event.element as CollectionReference).originalId == null) {
            const dialogRef = await this.dialogs.openGenericDialog({
                title: 'COLLECTIONS.ORIGINAL_MISSING',
                message: 'COLLECTIONS.ORIGINAL_MISSING_INFO',
                buttons: [
                    ...(this.isAllowedToDeleteNodes([event.element])
                        ? [{ label: 'OPTIONS.REMOVE_REF', config: { color: 'standard' as const } }]
                        : []),
                    { label: 'COLLECTIONS.OPEN_MISSING', config: { color: 'primary' } },
                ],
            });
            dialogRef.afterClosed().subscribe((response) => {
                if (response === 'OPTIONS.REMOVE_REF') {
                    this.deleteFromCollection();
                } else if (response === 'COLLECTIONS.OPEN_MISSING') {
                    this.clickElementEvent(event);
                }
            });
        } else {
            this.clickElementEvent(event);
        }
    }

    private clickElementEvent(event: NodeClickEvent<CollectionReference | ProposalNode>) {
        if (this.interactionType === InteractionType.DefaultActionLink) {
            this.nodeService
                .getNodeMetadata(event.element.ref.id)
                .subscribe((data: NodeWrapper) => {
                    this.contentNode = data.node;
                    this.router.navigate([
                        UIConstants.ROUTER_PREFIX + 'render',
                        event.element.ref.id,
                    ]);
                });
        } else {
            this.clickItem.emit(event);
        }
    }

    private isAllowedToDeleteNodes(nodes: Node[]) {
        return (
            this.isAllowedToDeleteCollection() ||
            this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_DELETE)
        );
    }
    isAllowedToDeleteCollection(): boolean {
        if (this.isRootLevel) {
            return false;
        }
        return RestHelper.hasAccessPermission(this.collection, RestConstants.PERMISSION_DELETE);
    }

    private deleteFromCollection(callback: Function = null) {
        this.toast.showProgressSpinner();
        this.collectionService
            .removeFromCollection(this.contentNode.ref.id, this.collection.ref.id)
            .subscribe(
                () => {
                    if (!('proposal' in this.collection)) {
                        this.toast.toast('COLLECTIONS.REMOVED_FROM_COLLECTION');
                    }
                    this.toast.closeProgressSpinner();
                    this.refreshContent();
                    if (callback) {
                        callback();
                    }
                },
                (error: any) => {
                    this.toast.closeProgressSpinner();
                    this.toast.error(error);
                },
            );
    }

    private registerMainNav(): void {
        this.mainNavService.setMainNavConfig({
            title: 'COLLECTIONS.TITLE',
            currentScope: 'collections',
            onCreate: (nodes) => this.addNodesToCollection(nodes),
        });
        this.mainNavService.onConnectorCreated
            .pipe(takeUntil(this.destroyed$))
            .subscribe((node) => this.addNodesToCollection([node]));
        this.mainNavUpdateTrigger.pipe(takeUntil(this.destroyed$)).subscribe(async () => {
            this.mainNavService.patchMainNavConfig({
                create: {
                    allowed: this.createAllowed(),
                    allowBinary: !this.isRootLevel && (await this.isAllowedToEditCollection()),
                    parent: this.collection ?? null,
                },
            });
        });
    }

    private refreshContent() {
        this.dataSourceCollections.reset();
        this.dataSourceReferences.reset();
        this.listReferences?.getSelection().clear();
        this.dataSourceCollections.isLoading = true;
        this.dataSourceReferences.isLoading = true;

        // set correct scope
        const request: RequestObject = Helper.deepCopy(CollectionContentComponent.DEFAULT_REQUEST);
        if (this.sortCollections?.active) {
            request.sortBy = [this.sortCollections.active];

            request.sortAscending = [this.sortCollections.direction === 'asc'];
        } else {
            console.warn('Sort for collections is not defined in the mds!');
        }
        // when loading child collections, we load all of them
        if (!this.isRootLevel) {
            request.count = RestConstants.COUNT_UNLIMITED;
        } else {
            // on root level, obey pinned order if collections are pinned
            request.sortBy = [
                RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
                RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
                ...request.sortBy,
            ];
            request.sortAscending = [false, true, ...request.sortAscending];
        }
        this.collectionService
            .getCollectionSubcollections(
                this.collection.ref.id,
                this.scope,
                [],
                request,
                this.collection.ref.repo,
            )
            .subscribe(
                (collection) => {
                    // transfere sub collections and content
                    this.dataSourceCollections.setData(
                        collection.collections,
                        collection.pagination,
                    );
                    this.dataSourceCollections.isLoading = false;
                    if (this.isRootLevel) {
                        this.finishCollectionLoading();
                        return;
                    }
                    const requestRefs = this.getReferencesRequest();
                    requestRefs.count = null;
                    this.collectionService
                        .getCollectionReferences(
                            this.collection.ref.id,
                            [RestConstants.ALL],
                            requestRefs,
                            this.collection.ref.repo,
                        )
                        .subscribe((refs) => {
                            this.dataSourceReferences.setData(refs.references, refs.pagination);
                            this.dataSourceReferences.isLoading = false;
                            this.finishCollectionLoading();
                        });
                },
                (error: any) => {
                    this.toast.error(error);
                },
            );
    }

    isMobile() {
        return this.uiService.isMobile();
    }
    async setReferenceSort(sort: ListSortConfig) {
        const diff = Helper.getKeysWithDifferentValues(this.sortReferences, sort);
        this.sortReferences = sort;
        // auto activate the custom sorting when the users switch to "custom order"
        if (diff.includes('active')) {
            this.sortReferences.customSortingInProgress =
                this.sortReferences.active === RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION;
        }
        this.toggleReferencesOrder();
        if (this.sortReferences.customSortingInProgress) {
            await this.loadMoreReferences({
                reset: true,
                amount: RestConstants.COUNT_UNLIMITED,
                offset: 0,
            });
        }
        if (diff.includes('customSortingInProgress') && sort.customSortingInProgress) {
            return;
        }

        try {
            await this.nodeService
                .editNodeProperty(
                    this.collection.ref.id,
                    RestConstants.CCM_PROP_COLLECTION_ORDER_MODE,
                    [sort.active, (sort.direction === 'asc') + ''],
                )
                .toPromise();
            if (sort.active !== RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION) {
                this.toast.toast('COLLECTIONS.TOAST.SORT_SAVED_TYPE', {
                    type: this.translation.instant('NODE.' + sort.active),
                });
            }
        } catch (e) {
            this.toast.error(e);
        }
        this.refreshContent();
    }

    private getReferencesRequest(): RequestObject {
        return {
            sortBy: [this.sortReferences.active],
            sortAscending: [this.sortReferences.direction === 'asc'],
        };
    }

    async loadMoreReferences(event: FetchEvent) {
        if (!(await this.dataSourceReferences.hasMore()) || this.dataSourceReferences.isLoading) {
            return;
        }
        const request = this.getReferencesRequest();
        request.offset = event.offset ?? (await this.dataSourceReferences.getData()).length;
        if (event.amount != null) {
            request.count = event.amount;
        }
        if (event.reset) {
            this.dataSourceReferences.reset();
        }
        this.dataSourceReferences.isLoading = true;
        this.collectionService
            .getCollectionReferences(
                this.collection.ref.id,
                [RestConstants.ALL],
                request,
                this.collection.ref.repo,
            )
            .subscribe((refs) => {
                this.dataSourceReferences.appendData(refs.references);
                this.dataSourceReferences.isLoading = false;
            });
    }

    async loadMoreCollections() {
        if (!(await this.dataSourceCollections.hasMore()) || this.dataSourceCollections.isLoading) {
            return;
        }
        const request: any = Helper.deepCopy(CollectionContentComponent.DEFAULT_REQUEST);
        request.offset = (await this.dataSourceCollections.getData()).length;
        this.dataSourceCollections.isLoading = true;
        this.collectionService
            .getCollectionSubcollections(
                this.collection.ref.id,
                this.scope,
                [],
                request,
                this.collection.ref.repo,
            )
            .subscribe((refs) => {
                this.dataSourceCollections.appendData(refs.collections);
                this.dataSourceCollections.isLoading = false;
            });
    }

    private finishCollectionLoading(callback?: () => void) {
        this.mainNavService.getMainNav()?.refreshBanner();

        // Cannot trivially reference the add button for the tutorial with
        // current implementation of generic options.
        //
        // TODO: Decide whether to keep the tutorial as it was and implement a
        // way to reference the option button if necessary.

        // if (
        //     this.getCollectionId() == RestConstants.ROOT &&
        //     this.isAllowedToEditCollection()
        // ) {
        //     setTimeout(() => {
        //         this.tutorialElement = this.listCollections.addElementRef;
        //     });
        // }
        if (callback) {
            callback();
        }
        setTimeout(() => {
            this.setOptionsCollection();
            this.listReferences?.initOptionsGenerator({
                actionbar: this.actionbarReferences,
                parent: this.collection,
            });
            if (!this.loadingTask.isDone) {
                this.loadingTask.done();
            }
        });
    }

    deleteReference(content: EduData.CollectionReference | EduData.Node) {
        this.contentNode = content;
        this.deleteFromCollection();
    }

    async setCollectionSort(sort: ListSortConfig) {
        this.sortCollections = sort;
        try {
            await this.nodeService
                .editNodeProperty(
                    this.collection.ref.id,
                    RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE,
                    [this.sortCollections.active, (this.sortCollections.direction === 'asc') + ''],
                )
                .toPromise();
        } catch (e) {
            this.toast.error(e);
        }
        this.refreshContent();
        if (sort.active !== RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION) {
            this.toast.toast('COLLECTIONS.TOAST.SORT_SAVED_TYPE', {
                type: this.translation.instant('NODE.' + sort.active),
            });
        }
        if (this.sortCollections.active === RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION) {
            void this.toggleCollectionsOrder();
        } else {
            this.infobar.close();
        }
    }

    private setOptionsCollection() {
        this.optionsService.setData({
            scope: Scope.CollectionsCollection,
            activeObjects: [this.collection],
        });
        this.optionsService.initComponents(this.getInfobar()?.actionbar, this.listReferences);
        this.optionsService.refreshComponents();
    }

    async toggleCollectionsOrder() {
        if (this.sortCollections.customSortingInProgress) {
            const response = await this.infobar.open({
                title: 'COLLECTIONS.ORDER_COLLECTIONS',
                message: 'COLLECTIONS.ORDER_COLLECTIONS_INFO',
                buttons: [{ label: 'SAVE', config: { color: 'primary' } }],
            });
            console.log('response', response);
            if (response === 'SAVE') {
                return this.changeCollectionsOrder();
            } else {
                this.sortCollections.customSortingInProgress = false;
            }
        } else {
            this.infobar.close();
        }
        this.refreshContent();
    }

    async toggleReferencesOrder() {
        if (this.sortReferences.customSortingInProgress) {
            const response = await this.infobar.open({
                title: 'COLLECTIONS.ORDER_ELEMENTS',
                message: 'COLLECTIONS.ORDER_ELEMENTS_INFO',
                buttons: [{ label: 'SAVE', config: { color: 'primary' } }],
            });
            this.sortReferences.customSortingInProgress = false;
            if (response === 'SAVE') {
                void this.changeReferencesOrder();
                this.listReferences.getSelection().clear();
            } else {
                this.refreshContent();
            }
        } else {
            this.infobar.close();
        }
    }

    addNodesToCollection(nodes: Node[], allowDuplicate: boolean | 'ignore' = false) {
        this.toast.showProgressSpinner();
        UIHelper.addToCollection(
            this.nodeHelper,
            this.collectionService,
            this.router,
            this.bridge,
            this.collection,
            nodes,
            false,
            () => {
                this.refreshContent();
                this.toast.closeProgressSpinner();
            },
            allowDuplicate,
        );
    }

    private async changeReferencesOrder() {
        this.toast.showProgressSpinner();
        this.collectionService
            .setOrder(
                this.collection.ref.id,
                RestHelper.getNodeIds(await this.dataSourceReferences.getData()),
            )
            .subscribe(
                () => {
                    this.toast.toast('COLLECTIONS.TOAST.SORT_SAVED_CUSTOM');
                    this.toast.closeProgressSpinner();
                },
                (error: any) => {
                    this.toast.closeProgressSpinner();
                    this.toast.error(error);
                },
            );
    }

    private async changeCollectionsOrder() {
        this.toast.showProgressSpinner();
        this.collectionService
            .setOrder(
                this.collection.ref.id,
                RestHelper.getNodeIds(await this.dataSourceCollections.getData()),
            )
            .subscribe(
                () => {
                    this.sortCollections.customSortingInProgress = false;
                    this.toast.toast('COLLECTIONS.TOAST.SORT_SAVED_CUSTOM');
                    this.toast.closeProgressSpinner();
                },
                (error: any) => {
                    this.toast.closeProgressSpinner();
                    this.toast.error(error);
                },
            );
    }

    canDelete(node: EduData.CollectionReference) {
        return RestHelper.hasAccessPermission(node, 'Delete');
    }

    isDeleted(node: CollectionReference) {
        return (
            node.aspects.includes(RestConstants.CCM_ASPECT_IO_REFERENCE) &&
            !node.aspects.includes(RestConstants.CCM_ASPECT_REMOTEREPOSITORY) &&
            !node.originalId
        );
    }
}
