import {
    Component,
    ContentChild,
    ElementRef,
    EventEmitter,
    OnDestroy,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {
    ColorHelper,
    LocalEventsService,
    NodeEntriesDisplayType,
    OptionsHelperDataService,
    PreferredColor,
    Scope,
    SortEvent,
    TranslationsService,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { Subject, combineLatest } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import * as EduData from '../../core-module/core.module';
import {
    ConfigurationService,
    DialogButton,
    Mediacenter,
    Node,
    NodeRef,
    RestCollectionService,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestMediacenterService,
    RestNetworkService,
    RestNodeService,
    TemporaryStorageService,
    UIService,
} from '../../core-module/core.module';
import { Helper } from '../../core-module/rest/helper';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { NodeHelperService } from '../../services/node-helper.service';
import { Toast } from '../../services/toast';
import { BreadcrumbsService } from '../../shared/components/breadcrumbs/breadcrumbs.service';
import { CollectionContentComponent } from './collection-content/collection-content.component';
import { CollectionInfoBarComponent } from './collection-info-bar/collection-info-bar.component';
import {
    GlobalCollectionsPageService,
    GlobalCollectionsPageServiceInternal,
} from './global-collections-page.service';

// component class
@Component({
    selector: 'es-collections-page',
    templateUrl: 'collections-page.component.html',
    styleUrls: ['collections-page.component.scss'],
    // provide a new instance so to not get conflicts with other service instances
    providers: [OptionsHelperDataService],
})
export class CollectionsPageComponent implements OnDestroy {
    static INDEX_MAPPING = [
        RestConstants.COLLECTIONSCOPE_MY,
        RestConstants.COLLECTIONSCOPE_ORGA,
        RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
        RestConstants.COLLECTIONSCOPE_TYPE_MEDIA_CENTER,
        RestConstants.COLLECTIONSCOPE_ALL,
    ];
    readonly SCOPES = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    readonly getInfobar = () => this.infobar;

    @ViewChild('infobar') infobar: CollectionInfoBarComponent;
    @ViewChild('collectionContentComponent') collectionContentRef: CollectionContentComponent;
    @ContentChild('collectionContentTemplate') collectionContentTemplateRef: TemplateRef<any>;

    dialogTitle: string;
    dialogCancelable = false;
    dialogMessage: string;
    dialogButtons: DialogButton[];
    tabSelected: string = RestConstants.COLLECTIONSCOPE_MY;
    isLoading = true;
    isReady = false;
    collection: Node;
    collectionSortEmitter = new EventEmitter<SortEvent>();
    collectionCustomSortEmitter = new EventEmitter<boolean>();
    referenceSortEmitter = new EventEmitter<SortEvent>();
    referenceCustomSortEmitter = new EventEmitter<boolean>();
    mainnav = true;
    isGuest = true;
    addToOther: EduData.Node[];
    addPinning: string;
    tutorialElement: ElementRef;

    // FIXME: `collectionShare` is expected to be of type `Node[]` by `workspace-management` but is
    // of type `Node` here.
    private adminMediacenters: Mediacenter[];
    isRootLevel: boolean;
    set collectionShare(collectionShare: Node[]) {
        this._collectionShare = collectionShare as any as Node;
        this.refreshAll();
    }

    get collectionShare() {
        return this._collectionShare as any as Node[];
    }

    set tabSelectedIndex(pos: number) {
        if (this.isGuest) {
            pos += 2; // skip first 2 tabs
        }
        if (!this.hasEditorial && pos > 1) {
            pos++; // skip editorial
        }
        if (!this.hasMediacenter && pos > 2) {
            pos++; // skip mediacenter
        }
        this.selectTab(CollectionsPageComponent.INDEX_MAPPING[pos]);
    }

    get tabSelectedIndex() {
        let pos = CollectionsPageComponent.INDEX_MAPPING.indexOf(this.tabSelected);
        if (this.isGuest) {
            pos -= 2;
        }
        if (!this.hasEditorial && pos > 1) {
            pos--;
        }
        if (!this.hasMediacenter && pos > 2) {
            pos--;
        }
        return pos;
    }
    private contentDetailObject: any = null;
    // real parentCollectionId is only available, if user was browsing
    private parentCollectionId: EduData.Reference = new EduData.Reference(
        RestConstants.HOME_REPOSITORY,
        RestConstants.ROOT,
    );
    private person: EduData.User;
    hasEditorial = false;
    hasMediacenter = false;
    reurl: any;
    private _collectionShare: Node;
    private params: Params;
    private destroyed = new Subject<void>();
    readonly customTemplates = this.globalCollectionsPageServiceInternal.customTemplates;

    // inject services
    constructor(
        private breadcrumbsService: BreadcrumbsService,
        private collectionService: RestCollectionService,
        private config: ConfigurationService,
        private connector: RestConnectorService,
        private iamService: RestIamService,
        private localEvents: LocalEventsService,
        private mediacenterService: RestMediacenterService,
        private nodeHelper: NodeHelperService,
        private nodeService: RestNodeService,
        private route: ActivatedRoute,
        private router: Router,
        private globalCollectionsPageServiceInternal: GlobalCollectionsPageServiceInternal,
        private tempStorage: TemporaryStorageService,
        private optionsService: OptionsHelperDataService,
        private networkService: RestNetworkService,
        private temporaryStorageService: TemporaryStorageService,
        private toast: Toast,
        private translations: TranslationsService,
        private uiService: UIService,
    ) {
        this.translations.waitForInit().subscribe(() => {
            combineLatest([
                this.connector.isLoggedIn(),
                // FIXME: The sub components should be obserable-aware!
                this.networkService.getRepositories(),
            ]).subscribe(
                ([data]) => {
                    if (data.isValidLogin && data.currentScope == null) {
                        this.isGuest = data.isGuest;
                        this.mediacenterService.getMediacenters().subscribe((mediacenters) => {
                            this.adminMediacenters = mediacenters.filter(
                                (m) => m.administrationAccess,
                            );
                        });
                        this.collectionService
                            .getCollectionSubcollections(
                                RestConstants.ROOT,
                                RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
                            )
                            .subscribe((data) => {
                                this.hasEditorial = data.collections.length > 0;
                            });
                        this.collectionService
                            .getCollectionSubcollections(
                                RestConstants.ROOT,
                                RestConstants.COLLECTIONSCOPE_TYPE_MEDIA_CENTER,
                            )
                            .subscribe((data) => {
                                this.hasMediacenter = data.collections.length > 0;
                            });
                        this.initialize();
                    } else {
                        RestHelper.goToLogin(this.router, this.config);
                    }
                },
                () => RestHelper.goToLogin(this.router, this.config),
            );
        });
        // Navigate to parent collection when the current collection is deleted.
        this.localEvents.nodesDeleted
            .pipe(
                takeUntil(this.destroyed),
                filter((nodes) => nodes.some((node) => node.ref.id === this.collection.ref.id)),
            )
            .subscribe(() => this.navigate(this.parentCollectionId.id));
    }

    ngOnDestroy() {
        this.destroyed.next();
        this.destroyed.complete();
        this.tempStorage.set(
            TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE,
            this.collectionContentRef?.dataSourceReferences,
        );
    }

    isMobile() {
        return this.uiService.isMobile();
    }

    isMobileWidth() {
        return window.innerWidth < UIConstants.MOBILE_WIDTH;
    }

    navigate(id = '', addToOther = '', feedback = false) {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            params.scope = this.tabSelected;
            if (id && id !== '-root-') {
                params.id = id;
            }
            if (feedback) {
                params.feedback = feedback;
            }
            if (addToOther) {
                params.addToOther = addToOther;
            }
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'collections'], {
                queryParams: params,
            });
        });
    }

    closeAddToOther() {
        this.navigate(this.collection.ref.id);
    }

    selectTab(tab: string) {
        if (this.tabSelected !== tab || this.getCollectionId() !== RestConstants.ROOT) {
            this.tabSelected = tab;
            this.parentCollectionId = new EduData.Reference(
                RestConstants.HOME_REPOSITORY,
                RestConstants.ROOT,
            );
            this.contentDetailObject = null;
            this.navigate();
        }
    }

    isRootLevelCollection(): boolean {
        return this.isRootLevel;
    }

    isAllowedToEditCollection(): boolean {
        if (this.isRootLevelCollection()) {
            return !this.isGuest; //this.tabSelected === RestConstants.COLLECTIONSCOPE_MY
        }
        return RestHelper.hasAccessPermission(this.collection, RestConstants.PERMISSION_WRITE);
    }

    feedbackAllowed(): boolean {
        return (
            !this.isGuest &&
            RestHelper.hasAccessPermission(this.collection, RestConstants.PERMISSION_FEEDBACK)
        );
    }

    isUserAllowedToEdit(collection: Node): boolean {
        return RestHelper.isUserAllowedToEdit(collection, this.person);
    }

    pinCollection() {
        this.addPinning = this.collection.ref.id;
    }

    getPrivacyScope(collection: EduData.Collection): string {
        return collection.scope;
        //  return RestHelper.getPrivacyScope(collection);
    }

    navigateToSearch() {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    }

    isBrightColor() {
        return (
            ColorHelper.getPreferredColor(this.collection?.collection?.color) ===
            PreferredColor.White
        );
    }

    getScopeInfo() {
        return this.nodeHelper.getCollectionScopeInfo(this.collection);
    }

    collectionEdit(): void {
        if (this.isAllowedToEditCollection()) {
            this.router.navigate(
                [
                    UIConstants.ROUTER_PREFIX + 'collections/collection',
                    'edit',
                    this.collection.ref.id,
                ],
                { queryParams: { mainnav: this.mainnav } },
            );
            return;
        }
    }

    getScope() {
        return this.tabSelected ? this.tabSelected : RestConstants.COLLECTIONSCOPE_ALL;
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

    onCollectionsClick(collection: Node): void {
        // remember actual collection as breadcrumb
        if (!this.isRootLevelCollection()) {
            this.parentCollectionId = this.collection.ref;
        }

        // set thru router so that browser back button can work
        this.navigate(collection.ref.id);
    }

    refreshAll() {
        this.displayCollectionById(this.collection.ref.id);
    }

    async displayCollectionById(id: string) {
        if (!!id) {
            try {
                this.collection = (
                    await this.collectionService.getCollection(id).toPromise()
                ).collection;
            } catch (e) {
                if (e.status === RestConstants.HTTP_FORBIDDEN) {
                    const login = await this.connector.isLoggedIn().toPromise();
                    if (login.statusCode === RestConstants.STATUS_CODE_OK) {
                        this.toast.error(e);
                    } else {
                        RestHelper.goToLogin(this.router, this.config, null);
                    }
                }
            }
        } else {
            this.setCollectionId(RestConstants.ROOT);
        }
        this.isLoading = false;
        this.renderBreadcrumbs();
        this.isRootLevel = !id;
    }

    closeDialog() {
        this.dialogTitle = null;
    }

    showTabs() {
        return this.isRootLevelCollection() && (!this.isGuest || this.hasEditorial);
    }

    hasNonIconPreview(): boolean {
        const preview = this.collection?.preview;
        return preview && !preview.isIcon;
    }

    private renderBreadcrumbs() {
        this.breadcrumbsService.setNodePath([]);
        if (this.collection.ref.id === RestConstants.ROOT) {
            return;
        }
        this.nodeService
            .getNodeParents(this.collection.ref.id, false)
            .subscribe((data: EduData.NodeList) => {
                const path = data.nodes.reverse();
                if (path.length > 1) {
                    this.parentCollectionId = new EduData.Reference(
                        path[path.length - 2].ref.repo,
                        path[path.length - 2].ref.id,
                    );
                }
                this.breadcrumbsService.setNodePath(path);
            });
    }

    private initialize() {
        // load user profile
        this.iamService.getCurrentUserAsync().then(
            (iamUser) => {
                // WIN

                this.person = iamUser.person;

                // set app to ready state
                this.isReady = true;
                // subscribe to parameters of url
                this.route.queryParams.subscribe(async (params) => {
                    const diffs = Helper.getDifferentKeys(this.params, params);
                    if (Object.keys(diffs).length === 1 && diffs.viewType) {
                        this.params = params;
                        return;
                    }
                    this.params = params;
                    if (params.scope) {
                        this.tabSelected = params.scope;
                    } else {
                        this.tabSelected = this.isGuest
                            ? RestConstants.COLLECTIONSCOPE_ALL
                            : RestConstants.COLLECTIONSCOPE_MY;
                    }
                    this.reurl = params.reurl;

                    if (params.mainnav) {
                        this.mainnav = params.mainnav !== 'false';
                    }

                    // get id from route and validate input data
                    const id = params.id;
                    if (params.addToOther) {
                        this.nodeService
                            .getNodeMetadata(params.addToOther)
                            .subscribe((data: EduData.NodeWrapper) => {
                                this.addToOther = [data.node];
                            });
                    }
                    await this.displayCollectionById(id);

                    /*if (params.nodeId) {
                        let node = params.nodeId.split('/');
                        node = node[node.length - 1];
                        this.collectionService
                            .addNodeToCollection(id, node, null)
                            .subscribe(
                                () => this.navigate(id),
                                (error: any) => {
                                    this.handleError(error);
                                    this.navigate(id);
                                    //this.displayCollectionById(id)
                                },
                            );
                    } else {*/
                    // }
                });
            },
            (error) => {
                // FAIL
                this.toast.error(error);
                this.isReady = true;
            },
        );
    }

    private setCollectionId(id: string) {
        this.collection = new Node();
        this.collection.ref = { id } as NodeRef;
        this.collection.aspects = [RestConstants.CCM_ASPECT_COLLECTION];
    }

    private getCollectionId() {
        const c = this.collection;
        return c != null && c.ref != null ? c.ref.id : null;
    }

    createAllowed = () => {
        if (this.isGuest) {
            return false;
        }
        if (this.isRootLevelCollection()) {
            let allowed = this.connector.hasToolPermissionInstant(
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
            );
            if (this.tabSelected === RestConstants.COLLECTIONSCOPE_MY) {
                return allowed;
            }
            if (this.tabSelected === RestConstants.COLLECTIONSCOPE_ORGA) {
                allowed =
                    allowed &&
                    this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE);
            } else {
                // for anything else, the user must be able to invite everyone
                allowed =
                    allowed &&
                    this.connector.hasToolPermissionInstant(
                        RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES,
                    );
                if (this.tabSelected === RestConstants.COLLECTIONSCOPE_TYPE_MEDIA_CENTER) {
                    allowed = allowed && this.adminMediacenters?.length === 1;
                } else if (this.tabSelected === RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL) {
                    allowed =
                        allowed &&
                        this.connector.hasToolPermissionInstant(
                            RestConstants.TOOLPERMISSION_COLLECTION_EDITORIAL,
                        );
                }
            }
            return allowed;
        } else {
            return this.isAllowedToEditCollection();
        }
    };
}

export interface SortInfo {
    name: 'cm:name' | 'cm:modified' | 'ccm:collection_ordered_position';
    ascending: boolean;
    userModifyActive?: boolean;
}
