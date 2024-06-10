import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import {
    ConfigurationService,
    Node,
    NodeList,
    RestConnectorService,
    RestConstants,
    SearchRequestCriteria,
    SessionStorageService,
    UIService,
} from '../../../core-module/core.module';
import { TranslateService } from '@ngx-translate/core';
import { Toast } from '../../../services/toast';
import { Helper } from '../../../core-module/rest/helper';
import {
    ActionbarComponent,
    CustomOptions,
    DropSource,
    DropTarget,
    FetchEvent,
    InteractionType,
    ListItem,
    ListSortConfig,
    LocalEventsService,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDataType,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    NodeRoot,
    Scope,
    TemporaryStorageService,
} from 'ngx-edu-sharing-ui';
import { canDropOnNode } from '../workspace-utils';
import { BehaviorSubject, combineLatest, Observable, of, ReplaySubject, Subject } from 'rxjs';
import {
    catchError,
    debounceTime,
    distinctUntilChanged,
    switchMap,
    takeUntil,
    tap,
} from 'rxjs/operators';
import {
    DEFAULT,
    HOME_REPOSITORY,
    NodeService,
    PROPERTY_FILTER_ALL,
    SearchResults,
    SearchService,
} from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-workspace-explorer',
    templateUrl: 'explorer.component.html',
    styleUrls: ['explorer.component.scss'],
})
export class WorkspaceExplorerComponent implements OnDestroy, OnChanges, AfterViewInit {
    public readonly SCOPES = Scope;
    readonly InteractionType = InteractionType;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    public static getColumns(
        connector: RestConnectorService,
        customColumns: ListItem[] = [],
        configColumns: string[] = [],
    ) {
        let defaultColumns: ListItem[] = [];
        defaultColumns.push(new ListItem('NODE', RestConstants.CM_NAME));
        defaultColumns.push(new ListItem('NODE', RestConstants.CM_CREATOR));
        defaultColumns.push(new ListItem('NODE', RestConstants.CM_MODIFIED_DATE));
        if (connector.getCurrentLogin() ? connector.getCurrentLogin().isAdmin : false) {
            defaultColumns.push(new ListItem('NODE', RestConstants.NODE_ID));

            const repsource = new ListItem('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCEID);
            repsource.visible = false;
            defaultColumns.push(repsource);
        }
        const title = new ListItem('NODE', RestConstants.LOM_PROP_TITLE);
        title.visible = false;
        const size = new ListItem('NODE', RestConstants.SIZE);
        size.visible = false;
        const created = new ListItem('NODE', RestConstants.CM_PROP_C_CREATED);
        created.visible = false;
        const mediatype = new ListItem('NODE', RestConstants.MEDIATYPE);
        mediatype.visible = false;
        const keywords = new ListItem('NODE', RestConstants.LOM_PROP_GENERAL_KEYWORD);
        keywords.visible = false;
        const dimensions = new ListItem('NODE', RestConstants.DIMENSIONS);
        dimensions.visible = false;
        const version = new ListItem('NODE', RestConstants.LOM_PROP_LIFECYCLE_VERSION);
        version.visible = false;
        const usage = new ListItem('NODE', RestConstants.VIRTUAL_PROP_USAGECOUNT);
        usage.visible = false;
        const license = new ListItem('NODE', RestConstants.CCM_PROP_LICENSE);
        license.visible = false;
        const wfStatus = new ListItem('NODE', RestConstants.CCM_PROP_WF_STATUS);
        wfStatus.visible = false;
        defaultColumns.push(title);
        defaultColumns.push(size);
        defaultColumns.push(created);
        defaultColumns.push(mediatype);
        defaultColumns.push(keywords);
        defaultColumns.push(dimensions);
        defaultColumns.push(version);
        defaultColumns.push(usage);
        defaultColumns.push(license);
        defaultColumns.push(wfStatus);

        if (Array.isArray(configColumns)) {
            const configList: ListItem[] = [];
            for (const col of defaultColumns) {
                if (configColumns.indexOf(col.name) != -1) {
                    col.visible = true;
                    configList.push(col);
                }
            }
            for (const col of defaultColumns) {
                if (configColumns.indexOf(col.name) == -1) {
                    col.visible = false;
                    configList.push(col);
                }
            }
            // sort as defined inside config
            configList.sort((a, b) => {
                let pos1 = configColumns.indexOf(a.name);
                let pos2 = configColumns.indexOf(b.name);
                if (pos1 === -1) pos1 = configColumns.length;
                if (pos2 === -1) pos2 = configColumns.length;
                return pos1 - pos2;
            });
            defaultColumns = configList;
        }
        if (Array.isArray(customColumns)) {
            for (const column of defaultColumns) {
                let add = true;
                for (const column2 of customColumns) {
                    if (column.name === column2.name) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    customColumns.push(column);
                }
            }
            return customColumns;
        }
        return defaultColumns;
    }

    @ViewChild(NodeEntriesWrapperComponent) nodeEntries: NodeEntriesWrapperComponent<Node>;
    @Input() customOptions: CustomOptions;
    @Input() dataSource: NodeDataSource<Node>;
    @Output() nodesChange = new EventEmitter<Node[]>();
    sort: ListSortConfig = {
        allowed: true,
        active: RestConstants.CM_NAME,
        direction: 'asc',
        columns: [],
    };

    public columns: ListItem[] = [];
    @Input() displayType = NodeEntriesDisplayType.Table;
    @Output() refreshTree = new EventEmitter<void>();
    @Output() displayTypeChange = new EventEmitter<NodeEntriesDisplayType>();
    @Input() reorderDialog = false;
    @Output() reorderDialogChange = new EventEmitter<boolean>();
    @Input() preventKeyevents: boolean;
    @Input() actionbar: ActionbarComponent;

    totalCount: number;

    public searchQuery$ = new BehaviorSubject<string>(null);
    node$ = new BehaviorSubject<Node>(null);
    load$ = new ReplaySubject<{ nodes: Observable<NodeList | SearchResults>; reset: boolean }>();
    private lastRequestSearch: boolean;

    _root: NodeRoot;
    @Input() set root(root: NodeRoot) {
        this._root = root;
        this.storage
            .get(SessionStorageService.KEY_WORKSPACE_SORT + root, null)
            .subscribe((data) => {
                if (data?.active != null) {
                    this.sort.active = data.active;
                    this.sort.direction = data.direction;
                } else {
                    this.sort.active = RestConstants.CM_NAME;
                    this.sort.direction = 'asc';
                }
            });
    }
    @Input() set current(current: Node) {
        this.setNode(current);
    }
    @Input() set searchQuery(query: any) {
        this.setSearchQuery(query);
    }
    @Output() onOpenNode = new EventEmitter<NodeEntriesDataType>();
    @Output() onViewNode = new EventEmitter();
    @Output() onSelectionChanged = new EventEmitter();
    @Output() onSelectNode = new EventEmitter<NodeEntriesDataType>();
    @Output() onSearchGlobal = new EventEmitter();
    @Output() onDrop = new EventEmitter<{ target: DropTarget; source: DropSource<Node> }>();
    @Output() onReset = new EventEmitter();
    private path: Node[];
    private destroyed = new Subject<void>();
    searchGlobal() {
        this.onSearchGlobal.emit(this.searchQuery$.value);
    }
    public load(event: FetchEvent = null) {
        if (this.node$.value == null && !this.searchQuery$.value) return;
        if (this.dataSource.isLoading) {
            // return;
        }
        if (this.searchQuery$.value) {
            this.sort.columns = RestConstants.POSSIBLE_SORT_BY_FIELDS_SOLR;
        } else {
            this.sort.columns = RestConstants.POSSIBLE_SORT_BY_FIELDS;
        }
        if (!this.sort.columns.some((s) => s.name === this.sort.active)) {
            this.sort.active = RestConstants.CM_NAME;
            this.sort.direction = 'asc';
            // set sorting will reinit everything
            this.setSorting(this.sort);
            return;
        }
        if (event?.reset) {
            this.dataSource.reset();
            this.nodeEntries.getSelection().clear();
            this.onReset.emit();
            if (event.offset === 0) {
                this.nodeEntries.resetPagination();
            }
        } else if (this.dataSource.isFullyLoaded()) {
            return;
        }
        this.dataSource.isLoading = true;
        // ignore virtual (new) added/uploaded elements
        const offset = event.offset || this.getRealNodeCount();
        if (this.searchQuery$.value) {
            const query = this.searchQuery$.value;
            this.lastRequestSearch = true;
            /*this.search.searchByProperties([RestConstants.NODE_ID,RestConstants.CM_PROP_TITLE,RestConstants.CM_NAME,RestConstants.LOM_PROP_DESCRIPTION,RestConstants.LOM_PROP_GENERAL_KEYWORD],
              [query,query,query,query,query],[],RestConstants.COMBINE_MODE_OR,RestConstants.CONTENT_TYPE_FILES_AND_FOLDERS, request).subscribe((data : NodeList) => this.addNodes(data,true));*/
            const criteria: SearchRequestCriteria[] = [];
            criteria.push({ property: RestConstants.PRIMARY_SEARCH_CRITERIA, values: [query] });
            if (this.node$.value) {
                criteria.push({
                    property: 'parent',
                    values: [this.node$.value ? this.node$.value.ref.id : ''],
                });
            }
            this.load$.next({
                reset: event?.reset,
                nodes: this.search.search({
                    query: 'workspace',
                    repository: HOME_REPOSITORY,
                    metadataset: DEFAULT,
                    contentType: (this.connector.getCurrentLogin() &&
                    this.connector.getCurrentLogin().isAdmin
                        ? RestConstants.CONTENT_TYPE_ALL
                        : RestConstants.CONTENT_TYPE_FILES_AND_FOLDERS) as any,
                    body: {
                        criteria,
                        resolveCollections: false,
                        resolveUsernames: true,
                    },
                    skipCount: offset,
                    propertyFilter: [PROPERTY_FILTER_ALL],
                    sortProperties: [this.sort.active],
                    sortAscending: [this.sort.direction === 'asc'],
                    maxItems: event?.amount || RestConnectorService.DEFAULT_NUMBER_PER_REQUEST,
                }),
            });
            // this.nodeApi.searchNodes(query,[],request).subscribe((data : NodeList) => this.addNodes(data,true));
        } else {
            this.lastRequestSearch = false;
            this.load$.next({
                reset: event?.reset,
                nodes: this.nodeApi.getChildren(this.node$.value.ref.id, {
                    skipCount: offset,
                    maxItems: event?.amount || RestConnectorService.DEFAULT_NUMBER_PER_REQUEST,
                    propertyFilter: [PROPERTY_FILTER_ALL],
                }),
            });
        }
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
        this.temporaryStorage.set(
            TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE,
            this.dataSource,
        );
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.displayType) {
            await this.initOptions();
        }
    }

    private handleError(error: any) {
        if (error.status == 404)
            this.toast.error(null, 'WORKSPACE.TOAST.NOT_FOUND', { id: this.node$.value.ref.id });
        else this.toast.error(error);

        this.dataSource.isLoading = false;
    }
    private addNodes(data: NodeList) {
        this.dataSource.isLoading = false;
        if (data && data.nodes) {
            this.dataSource.appendData(data.nodes);
            this.dataSource.setPagination(data.pagination);
        }
    }
    constructor(
        private connector: RestConnectorService,
        private translate: TranslateService,
        private storage: SessionStorageService,
        private temporaryStorage: TemporaryStorageService,
        private config: ConfigurationService,
        private search: SearchService,
        private toast: Toast,
        public ui: UIService,
        private nodeApi: NodeService,
        private localEvents: LocalEventsService,
    ) {
        // super(temporaryStorage,['_node','_nodes','sortBy','sortAscending','columns','totalCount','hasMoreToLoad']);
        this.initColumns();
        this.registerNodesDeleted();
        combineLatest([this.node$, this.searchQuery$])
            .pipe(
                distinctUntilChanged((a, b) => {
                    return Helper.objectEquals(a[0], b[0]) && a[1] === b[1];
                }),
                debounceTime(10),
            )
            .subscribe(async (value) => {
                // FIXME: This goes back to the first page when reloading. `NodeEntriesWrapper`
                // would restore the page via the `page` query parameter otherwise.
                await this.load({
                    offset: 0,
                    reset: true,
                });
            });
        this.load$
            .pipe(
                // map((o) => o.toPromise()),
                // switchMap(result => result.toPromise())
                tap(({ reset }) => {
                    if (reset) {
                        this.dataSource.reset();
                    }
                    this.dataSource.isLoading = true;
                }),
                switchMap(
                    async ({ nodes }) =>
                        await nodes
                            .pipe(
                                catchError((err) => {
                                    this.handleError(err);
                                    return of(null);
                                }),
                            )
                            .toPromise(),
                ),
            )
            .subscribe(async (data) => {
                if (data) {
                    this.addNodes(data);
                }
            });
    }

    async ngAfterViewInit() {
        await this.initOptions();
    }
    public async setSorting(config: ListSortConfig) {
        this.sort = config;
        await this.storage.set(SessionStorageService.KEY_WORKSPACE_SORT + this._root, {
            active: config.active,
            direction: config.direction,
        });
        await this.load({ reset: true, offset: 0 });
    }
    public onSelection(event: Node[]) {
        this.onSelectionChanged.emit(event);
    }
    /*
    private addParentToPath(node : Node,path : string[]) {

      path.splice(1,0,node.ref.id);
      if (node.parent.id==path[0] || node.parent.id==null) {
        this.onOpenNode.emit(node);
        return;
      }
      this.nodeApi.getNodeMetadata(node.parent.id).subscribe((data: NodeWrapper)=> {
        this.addParentToPath(data.node, path);
      });

    }
     */
    public doubleClick(node: Node) {
        this.onOpenNode.emit(node);
    }

    private setNode(current: Node) {
        this.searchQuery$.next(null);
        if (!current) {
            this.node$.next(null);
            return;
        }

        if (this.dataSource?.isLoading) {
            setTimeout(() => this.setNode(current), 10);
            return;
        }
        if (Helper.objectEquals(this.node$.value, current)) return;
        this.node$.next(current);
        this.initOptions();
    }

    private setSearchQuery(query: any) {
        setTimeout(() => {
            if (query && query.query) {
                this.searchQuery$.next(query.query);
                this.node$.next(query.node);
            } else {
                this.searchQuery$.next(null);
            }
        });
    }
    canDrop = canDropOnNode;
    drop = (target: DropTarget, source: DropSource<Node>) => {
        this.onDrop.emit({
            target,
            source,
        });
    };

    private getRealNodeCount() {
        return this.dataSource?.getData().filter((n) => !n.virtual).length;
    }

    initColumns() {
        this.config.get('workspaceColumns').subscribe((data: string[]) => {
            this.storage.get('workspaceColumns').subscribe((columns: any[]) => {
                this.columns = WorkspaceExplorerComponent.getColumns(this.connector, columns, data);
            });
        });
    }

    select(event: NodeClickEvent<NodeEntriesDataType>) {
        if (
            !(
                this.nodeEntries.getSelection().selected.length === 1 &&
                this.nodeEntries.getSelection().selected[0] === event.element
            )
        ) {
            this.nodeEntries.getSelection().clear();
        }
        this.nodeEntries.getSelection().toggle(event.element as Node);
    }

    private async initOptions() {
        await this.nodeEntries?.initOptionsGenerator({
            actionbar: this.actionbar,
            customOptions: this.customOptions,
            parent: this.node$.value,
        });
    }

    private registerNodesDeleted(): void {
        this.localEvents.nodesDeleted.pipe(takeUntil(this.destroyed)).subscribe((nodes) => {
            this.dataSource.removeData(nodes);
            this.nodeEntries?.getSelection().clear();
        });
    }

    saveColumns(columns: ListItem[]) {
        this.storage.set('workspaceColumns', columns);
    }

    clickItem(event: NodeClickEvent<NodeEntriesDataType>) {
        if (this.ui.isMobile()) {
            this.onOpenNode.emit(event.element);
        } else {
            this.select(event);
        }
    }

    syncTreeViewOnAdd(nodes: Node[]) {
        if (nodes.filter((n) => n.virtual && n.isDirectory).length) {
            this.refreshTree.emit();
        }
    }
}
