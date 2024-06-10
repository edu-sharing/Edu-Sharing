import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ComponentRef,
    ContentChild,
    ElementRef,
    EventEmitter,
    Input,
    NgZone,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    signal,
    SimpleChange,
    TemplateRef,
    Type,
    ViewChild,
    ViewContainerRef,
    WritableSignal,
} from '@angular/core';
import { Subject } from 'rxjs';
import { NodeEntriesTemplatesService } from './node-entries-templates.service';
import { NodeEntriesComponent } from './node-entries.component';
import {
    FetchEvent,
    GridConfig,
    InteractionType,
    ListDragGropConfig,
    ListEventInterface,
    ListOptions,
    ListOptionsConfig,
    ListSortConfig,
    NodeClickEvent,
    NodeEntriesDataType,
    NodeEntriesDisplayType,
} from './entries-model';
import { NodeDataSource } from './node-data-source';
import { Helper } from '../util/helper';
import { CustomSelectionModel, NodeEntriesService } from '../services/node-entries.service';
import { OptionItem, Scope } from '../types/option-item';
import { NodeHelperService } from '../services/node-helper.service';
import { ListItem } from '../types/list-item';
import { TemporaryStorageService } from '../services/temporary-storage.service';
import { CollectionReference, Node, User } from 'ngx-edu-sharing-api';
import { VirtualNode } from '../types/api-models';
import { OptionsHelperDataService } from '../services/options-helper-data.service';
import { UIService } from '../services/ui.service';

@Component({
    selector: 'es-node-entries-wrapper',
    template: `<es-node-entries
        #nodeEntriesComponent
        *ngIf="!customNodeListComponent"
    ></es-node-entries>`,
    providers: [NodeEntriesService, OptionsHelperDataService, NodeEntriesTemplatesService],
})
export class NodeEntriesWrapperComponent<T extends NodeEntriesDataType>
    implements AfterViewInit, OnInit, OnChanges, OnDestroy, ListEventInterface<T>
{
    /**
     * title (above) the table/grid
     */
    @ContentChild('title') titleRef: TemplateRef<any>;
    /**
     * data shown when data source is empty
     */
    @ContentChild('empty') emptyRef: TemplateRef<any>;
    /**
     * custom area for actions only for NodeEntriesDisplayType.SmallGrid (per card at the bottom)
     */
    @ContentChild('actionArea') actionAreaRef: TemplateRef<any>;
    /**
     * custom area for an overlay "above" each card (i.e. to show disabled infos), only for NodeEntriesDisplayType.SmallGrid & odeEntriesDisplayType.Grid
     */
    @ContentChild('overlay') overlayRef: TemplateRef<any>;
    @ViewChild('nodeEntriesComponent') nodeEntriesComponentRef: NodeEntriesComponent<T>;
    @Input() dataSource: NodeDataSource<T>;
    @Input() scope: Scope;
    @Input() columns: ListItem[];
    @Input() configureColumns: boolean;
    @Input() checkbox = true;
    @Output() columnsChange = new EventEmitter<ListItem[]>();
    @Input() globalOptions: OptionItem[];
    @Input() displayType = NodeEntriesDisplayType.Grid;
    @Output() displayTypeChange = new EventEmitter<NodeEntriesDisplayType>();
    @Input() elementInteractionType = InteractionType.DefaultActionLink;
    @Input() sort: ListSortConfig;
    @Input() dragDrop: ListDragGropConfig<T>;
    @Input() gridConfig: GridConfig;

    /**
     * This color defines the base color of gradients visually limiting a grid in scroll direction.
     * Defaults to white.
     */
    @Input() scrollGradientColor: string = '#fff';

    /**
     * this can be set instead of calling initOptionsGenerator()
     */
    @Input() initConfig: ListOptionsConfig;
    /**
     * Whether this node-entries instance represents the page's main content.
     *
     * Only set to true for one instance per page.
     *
     * If true, this instance will
     * - handle page-wide keyboard shortcuts
     * - take control of the `page` and `pageSize` query parameters for pagination
     */
    @Input() primaryInstance: boolean;
    /**
     * UI hints for whether a single click will cause a dynamic action.
     *
     * This does not configure the actual behavior but only UI hints to the user. Hints include
     * hover effects and a changed cursor.
     *
     * - When choosing 'static', the `clickItem` event should trigger some stationary action like
     *   selecting the element or displaying information in a complementary page area. The
     *   `dblClickItem` event can be used for a more disruptive action.
     * - When choosing 'dynamic', the `clickItem` event should trigger a major action like
     *   navigating to a new page or closing a dialog.
     */
    // TODO: Consider controlling the ui hints and the actual behavior with a single option.
    @Input() singleClickHint: 'dynamic' | 'static' = 'dynamic';
    /**
     * Do not load more data on scroll.
     */
    @Input() disableInfiniteScroll = false;

    @Output() fetchData = new EventEmitter<FetchEvent>();
    @Output() clickItem = new EventEmitter<NodeClickEvent<T>>();
    @Output() dblClickItem = new EventEmitter<NodeClickEvent<T>>();
    @Output() sortChange = new EventEmitter<ListSortConfig>();
    @Output() virtualNodesAdded;
    @Output() displayTypeChanged;

    customNodeListComponent: Type<NodeEntriesComponent<T>>;
    private componentRef: ComponentRef<NodeEntriesComponent<T>>;
    private options: ListOptions;
    private destroyed = new Subject<void>();

    constructor(
        private viewContainerRef: ViewContainerRef,
        private temporaryStorageService: TemporaryStorageService,
        private ngZone: NgZone,
        private entriesService: NodeEntriesService<T>,
        public optionsHelper: OptionsHelperDataService,
        private nodeHelperService: NodeHelperService,
        private uiService: UIService,
        // @TODO
        // private mainNav: MainNavService,
        private templatesService: NodeEntriesTemplatesService,
        private changeDetectorRef: ChangeDetectorRef,
        private elementRef: ElementRef,
    ) {
        // regulary re-bind template since it might have updated without ngChanges trigger
        /*
        ngZone.runOutsideAngular(() =>
            setInterval(() => this.componentRef.instance.emptyRef = this.emptyRef)
        );
        */
        this.virtualNodesAdded = this.optionsHelper.virtualNodesAdded;
        this.displayTypeChanged = this.optionsHelper.displayTypeChanged;
        this.entriesService.selection.changed.subscribe(() => {
            if (this.optionsHelper.getData()) {
                this.optionsHelper.getData().selectedObjects =
                    this.entriesService.selection.selected;
                this.optionsHelper.getData().activeObjects = this.entriesService.selection.selected;
            } else {
                console.warn('optionsHelper is not initalized correctly; data is empty');
            }
            this.optionsHelper.refreshComponents();
        });
    }

    ngOnInit(): void {
        if (this.primaryInstance) {
            this.optionsHelper.registerGlobalKeyboardShortcuts();
        }
    }

    ngOnChanges(changes: { [key: string]: SimpleChange } = {}) {
        if (!this.componentRef) {
            this.init();
        }
        this.entriesService.list = this;
        this.entriesService.dataSource = this.dataSource;
        this.entriesService.scope = this.scope;
        this.entriesService.columns = this.columns;
        this.entriesService.configureColumns = this.configureColumns;
        this.entriesService.checkbox = this.checkbox;
        this.entriesService.displayType = this.displayType;
        this.entriesService.elementInteractionType = this.elementInteractionType;
        this.entriesService.gridConfig = this.gridConfig;
        this.entriesService.options = this.options;
        this.entriesService.globalOptions = this.globalOptions;
        this.entriesService.sort = this.sort;
        this.entriesService.sortChange = this.sortChange;
        this.entriesService.dragDrop = this.dragDrop;
        this.entriesService.clickItem = this.clickItem;
        this.entriesService.dblClickItem = this.dblClickItem;
        this.entriesService.fetchData = this.fetchData;
        this.entriesService.primaryInstance = this.primaryInstance;
        this.entriesService.singleClickHint = this.singleClickHint;
        this.entriesService.disableInfiniteScroll = this.disableInfiniteScroll;
        this.entriesService.scrollGradientColor.set(this.scrollGradientColor);

        if (changes['initConfig']) {
            this.initOptionsGenerator(this.initConfig);
        }
        if (this.componentRef) {
            this.componentRef.instance.changeDetectorRef?.detectChanges();
        }
        // This might need wrapping with `setTimeout`.
        this.updateTemplates();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    /**
     * Replaces this wrapper with the configured custom-node-list component.
     */
    private init(): void {
        this.customNodeListComponent = this.temporaryStorageService.get(
            TemporaryStorageService.CUSTOM_NODE_ENTRIES_COMPONENT,
            null,
        );
        if (this.customNodeListComponent == null) {
            return;
        }
        this.componentRef = this.uiService.injectAngularComponent(
            this.viewContainerRef,
            this.customNodeListComponent,
            this.elementRef.nativeElement,
            // Input bindings are initialized in `ngOnChanges`.
            this.getOutputBindings(),
        );
    }
    /**
     * Creates a simple map of the output bindings defined in this component.
     */
    private getOutputBindings(): { [key: string]: EventEmitter<any> } {
        const outputBindings: { [key: string]: any } = {};
        for (const key of Object.keys(this)) {
            const value = (this as any)[key];
            if (value instanceof EventEmitter) {
                outputBindings[key] = value;
            }
        }
        return outputBindings;
    }

    getDisplayType(): NodeEntriesDisplayType {
        return this.displayType;
    }

    setDisplayType(displayType: NodeEntriesDisplayType): void {
        this.displayType = displayType;
        this.entriesService.displayType = displayType;
        this.ngOnChanges();
        this.displayTypeChange.emit(displayType);
    }

    updateNodes(nodes: void | T[]) {
        if (!nodes) {
            return;
        }
        this.dataSource.getData().forEach((d) => {
            let hits = (nodes as T[]).filter((n) => (n as Node).ref.id === (d as Node).ref.id);
            if (hits.length === 0) {
                // handle if the original has changed (for collection refs)
                hits = (nodes as T[]).filter(
                    (n) => (n as Node).ref.id === (d as unknown as CollectionReference).originalId,
                );
            }
            if (hits.length === 1) {
                this.nodeHelperService.copyDataToNode(d as Node, hits[0] as Node);
            }
        });
        // trigger rebuild
        if (this.dataSource instanceof NodeDataSource) {
            (this.dataSource as NodeDataSource<T>).refresh();
        }
        const oldSelection = this.entriesService.selection.selected;
        this.entriesService.selection.clear();
        this.entriesService.selection.select(
            ...oldSelection.map(
                (o) => this.dataSource.getData().filter((d) => Helper.objectEquals(o, d))?.[0],
            ),
        );
        this.changeDetectorRef.detectChanges();
    }

    showReorderColumnsDialog(): void {}

    addVirtualNodes(virtual: T[]): void {
        virtual = virtual.map((o) => {
            (o as VirtualNode).virtual = true;
            return o;
        });
        virtual.forEach((v) => {
            const contains = this.dataSource
                .getData()
                .some((d) =>
                    (d as Node).ref
                        ? (d as Node).ref?.id === (v as Node).ref?.id
                        : (d as User).authorityName === (v as User).authorityName,
                );
            if (contains) {
                this.updateNodes([v]);
            } else {
                this.dataSource.appendData([v], 'before');
            }
        });
        this.entriesService.selection.clear();
        this.entriesService.selection.select(...virtual);
        this.virtualNodesAdded.emit(virtual as Node[]);
        this.changeDetectorRef.detectChanges();
    }

    setOptions(options: ListOptions): void {
        this.options = options;
        this.ngOnChanges();
    }

    getSelection(): CustomSelectionModel<T> {
        return this.entriesService.selection;
    }

    async initOptionsGenerator(config: ListOptionsConfig) {
        await this.optionsHelper.initComponents(config.actionbar, this);
        this.optionsHelper.setData({
            scope: this.entriesService.scope,
            activeObjects: this.entriesService.selection.selected,
            selectedObjects: this.entriesService.selection.selected,
            allObjects: this.dataSource.getData(),
            parent: config.parent,
            customOptions: config.customOptions,
        });
        this.optionsHelper.refreshComponents();
    }

    ngAfterViewInit(): void {
        // Prevent changed-after-checked error
        Promise.resolve().then(() => this.updateTemplates());
    }

    private updateTemplates(): void {
        this.templatesService.title = this.titleRef;
        this.templatesService.empty = this.emptyRef;
        this.templatesService.actionArea = this.actionAreaRef;
        this.templatesService.overlay = this.overlayRef;
    }

    /**
     * reset the pagination to the first page
     * hint: this will do nothing in case the paginationStrategy !== Pagination
     */
    resetPagination() {
        this.nodeEntriesComponentRef?.paginator?.firstPage();
    }

    deleteNodes(objects: T[]): void {
        this.dataSource.removeData(objects);
        this.getSelection().clear();
    }
}
