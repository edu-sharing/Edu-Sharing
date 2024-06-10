import {
    Component,
    ContentChild,
    EventEmitter,
    HostListener,
    Input,
    OnInit,
    Output,
    TemplateRef,
} from '@angular/core';
import { Router } from '@angular/router';
import {
    Node,
    RestCollectionService,
    RestConnectorService,
    RestConstants,
    RestIamService,
    RestNodeService,
} from '../../../core-module/core.module';
import {
    DEFAULT,
    GenericAuthority,
    HOME_REPOSITORY,
    PROPERTY_FILTER_ALL,
    SearchService,
} from 'ngx-edu-sharing-api';
import { Toast } from '../../../services/toast';
import {
    InteractionType,
    ListItem,
    ListSortConfig,
    NodeDataSource,
    NodeEntriesDisplayType,
    OptionItem,
} from 'ngx-edu-sharing-ui';

/**
 * An edu-sharing sidebar dialog for adding data to a collection
 */
@Component({
    selector: 'es-collection-chooser',
    templateUrl: 'collection-chooser.component.html',
    styleUrls: ['collection-chooser.component.scss'],
})
export class CollectionChooserComponent implements OnInit {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly COLLECTION_LATEST_DEFAULT_COUNT = 3;
    @ContentChild('beforeRecent') beforeRecentRef: TemplateRef<any>;
    /**
     * The caption of the dialog, will be translated automatically
     */
    @Input() title: string;

    /**
     * Fired when an element is choosen, a (collection) Node will be send as a result
     */
    @Output() onChoose = new EventEmitter<Node>();
    @Output() onCreateCollection = new EventEmitter<Node>();
    /**
     * Fired when a list of nodes is dropped on a collection item
     */
    @Output() onDrop = new EventEmitter();
    /**
     * Fired when the dialog should be closed
     */
    @Output() onCancel = new EventEmitter();

    searchQueryInput = '';
    searchQuery = '';
    currentRoot: Node;
    breadcrumbs: Node[];
    dataSourceLatest = new NodeDataSource();
    dataSourceTree = new NodeDataSource();
    createCollectionOptionItem = new OptionItem('OPTIONS.NEW_COLLECTION', 'add', () =>
        this.createCollection(),
    );

    private hasMoreToLoad: boolean;
    columns: ListItem[] = ListItem.getCollectionDefaults();
    sortBy: string[];
    sortAscending = false;
    sort: ListSortConfig = {
        columns: [],
        allowed: false,
        active: RestConstants.CM_MODIFIED_DATE,
        direction: 'desc',
    };
    /**
     * shall more than 5 recent collections be shown
     */
    showMore = false;
    canCreate = false;

    constructor(
        private connector: RestConnectorService,
        private router: Router,
        private iam: RestIamService,
        private collectionApi: RestCollectionService,
        private searchService: SearchService,
        private node: RestNodeService,
        private toast: Toast,
    ) {
        // http://plnkr.co/edit/btpW3l0jr5beJVjohy1Q?p=preview
        this.connector
            .hasToolPermission(RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS)
            .subscribe((tp) => (this.canCreate = tp));
    }

    ngOnInit(): void {
        this.loadLatest(true);
        this.loadMy();
    }

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.code == 'Escape') {
            event.preventDefault();
            event.stopPropagation();
            this.cancel();
            return;
        }
    }

    createCollection() {
        this.onCreateCollection.emit(this.currentRoot ? this.currentRoot : null);
    }

    loadLatest(reset = false) {
        this.searchQuery = this.searchQueryInput;
        if (reset) {
            this.dataSourceLatest.reset();
        } else if (!this.hasMoreToLoad) {
            return;
        }
        this.dataSourceLatest.isLoading = true;
        const request = {
            sortBy: this.sortBy,
            offset: this.dataSourceLatest.getData().length,
            sortAscending: false,
            propertyFilter: [RestConstants.ALL],
        };
        let requestCall;
        if (this.searchQuery) {
            const criteria = [
                { property: RestConstants.PRIMARY_SEARCH_CRITERIA, values: [this.searchQuery] },
            ];
            this.searchService
                .search({
                    repository: HOME_REPOSITORY,
                    propertyFilter: [PROPERTY_FILTER_ALL],
                    contentType: 'COLLECTIONS',
                    metadataset: DEFAULT,
                    query: 'collections',
                    skipCount: this.dataSourceLatest.getData().length,
                    maxItems: RestConnectorService.DEFAULT_NUMBER_PER_REQUEST,
                    body: { criteria },
                })
                .subscribe(
                    (data) => {
                        this.dataSourceLatest.isLoading = false;
                        this.hasMoreToLoad = data.nodes.length > 0;
                        this.dataSourceLatest.appendData(data.nodes);
                    },
                    () => {
                        this.dataSourceLatest.isLoading = false;
                        this.hasMoreToLoad = false;
                    },
                );
        } else {
            this.collectionApi
                .getCollectionSubcollections(
                    RestConstants.ROOT,
                    RestConstants.COLLECTIONSCOPE_RECENT,
                    [],
                    request,
                )
                .subscribe(
                    (data) => {
                        this.dataSourceLatest.isLoading = false;
                        this.hasMoreToLoad = data.collections.length > 0;
                        this.dataSourceLatest.appendData(data.collections);
                    },
                    () => {
                        this.dataSourceLatest.isLoading = false;
                        this.hasMoreToLoad = false;
                    },
                );
        }
    }

    cancel() {
        this.onCancel.emit();
    }

    navigateBack() {
        if (this.breadcrumbs.length > 1)
            this.currentRoot = this.breadcrumbs[this.breadcrumbs.length - 2] as any;
        else this.currentRoot = null;
        this.loadMy();
    }

    drop(event: any) {
        if (!this.checkPermissions(event.target)) {
            return;
        }
        this.onDrop.emit(event);
    }

    private checkPermissions(node: Node) {
        if (!this.hasWritePermissions(node)) {
            this.toast.error(null, 'NO_WRITE_PERMISSIONS');
            return false;
        }
        return true;
    }

    hasWritePermissions(node: Node) {
        /*if (this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_COLLECTION_PROPOSAL)){
            return { status: false, message: 'COLLECTIONS.SUGGEST_ONLY', button: {
                    click: () => this.onChoose.emit(node),
                    caption: 'TEST',
                    icon: 'home'
                } };
        }*/
        return !(
            !this.connector.hasToolPermissionInstant(
                RestConstants.TOOLPERMISSION_COLLECTION_PROPOSAL,
            ) && node.access.indexOf(RestConstants.ACCESS_WRITE) === -1
        );
    }

    goIntoCollection(node: Node | GenericAuthority) {
        this.currentRoot = node as Node;
        this.loadMy();
    }

    clickCollection(node: Node | GenericAuthority) {
        if (!this.checkPermissions(node as Node)) {
            return;
        }
        this.onChoose.emit(node as Node);
    }

    private loadMy() {
        this.dataSourceTree.reset();
        this.breadcrumbs = null;
        this.dataSourceTree.isLoading = true;
        this.collectionApi
            .getCollectionSubcollections(
                this.currentRoot ? this.currentRoot.ref.id : RestConstants.ROOT,
                RestConstants.COLLECTIONSCOPE_MY,
                [RestConstants.ALL],
                {
                    sortBy: this.sortBy,
                    sortAscending: false,
                    count: RestConstants.COUNT_UNLIMITED,
                },
            )
            .subscribe((data) => {
                this.dataSourceTree.isLoading = false;
                this.dataSourceTree.setData(data.collections);
            });
        if (this.currentRoot) {
            this.node
                .getNodeParents(this.currentRoot.ref.id, false)
                .subscribe((list) => (this.breadcrumbs = list.nodes.reverse()));
        }
    }
}
