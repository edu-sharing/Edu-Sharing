import { CdkDragEnter, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import {
    ChangeDetectionStrategy,
    Component,
    effect,
    ElementRef,
    HostBinding,
    Input,
    NgZone,
    OnDestroy,
    OnInit,
    QueryList,
    TemplateRef,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import { Sort } from '@angular/material/sort';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Subject } from 'rxjs';
import { distinctUntilChanged, map, switchMap, take, takeUntil } from 'rxjs/operators';
import { GridLayout, NodeEntriesDisplayType } from '../entries-model';
import { ItemsCap } from '../items-cap';
import { NodeEntriesGlobalService } from '../node-entries-global.service';
import { NodeEntriesTemplatesService } from '../node-entries-templates.service';
import { SortSelectPanelComponent } from '../sort-select-panel/sort-select-panel.component';
import { CustomTemplatesDataSource } from '../custom-templates-data-source';
import { Target } from '../../types/option-item';
import { NodeEntriesService } from '../../services/node-entries.service';
import { UIService } from '../../services/ui.service';
import { ListItemSort } from '../../types/list-item';
import { DragData } from '../../types/drag-drop';
import { CdkOverlayOrigin } from '@angular/cdk/overlay';

let displayedWarnings: string[] = [];

@Component({
    selector: 'es-node-entries-card-grid',
    templateUrl: 'node-entries-card-grid.component.html',
    styleUrls: ['node-entries-card-grid.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeEntriesCardGridComponent<T extends Node> implements OnInit, OnDestroy {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly Target = Target;
    /**
     * relative scrolling when a scrolling arrow (left or right) is used
     * a value of 1 would mean to scroll the full width of the entire content
     */
    readonly ScrollingOffsetPercentage = 0.4;

    columnChooserVisible$ = new BehaviorSubject(false);
    @ViewChild('columnChooserTrigger') columnChooserTrigger: CdkOverlayOrigin;
    @ViewChild('gridTop', { static: true }) set gridTop(value: TemplateRef<unknown>) {
        this.registerGridTop(value);
    }
    @ViewChild(SortSelectPanelComponent)
    set sortPanel(value: SortSelectPanelComponent) {
        // if (this.entriesService.dataSource instanceof NodeDataSourceRemote) {
        setTimeout(() => {
            (this.entriesService.dataSource as any).sortPanel = value;
        });
        // }
    }
    @ViewChildren(CdkDropList) dropListsQuery: QueryList<CdkDropList>;
    @ViewChild('grid') gridRef: ElementRef;
    @ViewChildren('item', { read: ElementRef }) itemRefs: QueryList<ElementRef<HTMLElement>>;
    @Input() displayType: NodeEntriesDisplayType;

    isDragging = false; // Drag-and-drop, not rearrange
    dropLists: CdkDropList[];
    /**
     * Whether the number of shown items is limited by `gridConfig.maxRows`.
     *
     * A value of `true` does not mean, that there would be more items available.
     */
    visibleItemsLimited = false;
    layout: GridLayout;
    /**
     * updates via boxObserver
     * and holds the information if scrolling in the direction is currently feasible
     */
    scroll = {
        left: false,
        right: false,
    };

    readonly nodes$;
    private readonly maxRows$;
    private readonly layout$;
    private readonly itemsPerRowSubject = new BehaviorSubject<number | null>(null);
    readonly itemsCap = new ItemsCap<T>();
    private globalCursorStyle: HTMLStyleElement;
    private destroyed = new Subject<void>();
    @HostBinding('style.--scroll-gradient-color') scrollGradientColor: string;

    constructor(
        public entriesService: NodeEntriesService<T>,
        public entriesGlobalService: NodeEntriesGlobalService,
        public templatesService: NodeEntriesTemplatesService,
        public ui: UIService,
        private ngZone: NgZone,
    ) {
        this.nodes$ = this.entriesService.dataSource$.pipe(
            switchMap((dataSource) => dataSource?.connect()),
        );
        this.maxRows$ = this.entriesService.gridConfig$.pipe(
            map((gridConfig) => gridConfig?.maxRows || null),
            distinctUntilChanged(),
        );
        this.layout$ = this.entriesService.gridConfig$.pipe(
            map((gridConfig) => gridConfig?.layout || 'grid'),
            distinctUntilChanged(),
        );
        this.entriesService.dataSource$.pipe(takeUntil(this.destroyed)).subscribe(() => {
            this.updateScrollState();
        });
        effect(() => (this.scrollGradientColor = this.entriesService.scrollGradientColor()));
    }

    ngOnInit(): void {
        this.registerItemsCap();
        this.registerLayout();
        this.registerVisibleItemsLimited();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private registerGridTop(gridTop: TemplateRef<unknown>): void {
        setTimeout(() => {
            this.templatesService.entriesTopMatter = gridTop;
        });
        this.destroyed.subscribe(() => {
            if (this.templatesService.entriesTopMatter === gridTop) {
                setTimeout(() => {
                    this.templatesService.entriesTopMatter = null;
                });
            }
        });
    }

    private registerItemsCap() {
        this.entriesService.dataSource$
            .pipe(takeUntil(this.destroyed))
            .subscribe((dataSource) => (dataSource.itemsCap = this.itemsCap));
        rxjs.combineLatest([this.itemsPerRowSubject.pipe(distinctUntilChanged()), this.maxRows$])
            .pipe(
                map(([itemsPerRow, maxRows]) =>
                    maxRows > 0 && itemsPerRow !== null ? itemsPerRow * maxRows : null,
                ),
            )
            .subscribe((cap) => (this.itemsCap.cap = cap));
    }

    private registerLayout() {
        this.layout$.subscribe((layout) => (this.layout = layout));
    }

    onSortChange(sort: Sort) {
        this.entriesService.sort.active = sort.active;
        this.entriesService.sort.direction = sort.direction;
        this.entriesService.sortChange.emit(this.entriesService.sort);
    }

    loadData(source: 'scroll' | 'button') {
        // @TODO: Maybe this is better handled in a more centraled service
        if (source === 'scroll') {
            // check if there is a footer
            const elements = document.getElementsByTagName('footer');
            if (elements.length && elements.item(0).innerHTML.trim()) {
                return;
            }
        }
        const couldLoadMore = this.entriesService.loadMore(source);
        if (couldLoadMore && source === 'button') {
            this.focusFirstNewItemWhenLoaded();
        }
    }

    onCustomSortingInProgressChange() {
        this.entriesService.sortChange.emit(this.entriesService.sort);
        setTimeout(() => {
            this.refreshDropLists();
        });
    }

    onRearrangeDragEntered($event: CdkDragEnter) {
        moveItemInArray(
            this.entriesService.dataSource.getData(),
            $event.item.data,
            $event.container.data,
        );
        // `CdkDrag` doesn't really want us to rearrange the items while dragging. Its cached
        // element positions get out of sync unless we update them manually.
        this.ngZone.runOutsideAngular(() =>
            setTimeout(() =>
                this.dropLists?.forEach((list) => (list._dropListRef as any)['_cacheItems']()),
            ),
        );
    }

    onRearrangeDragStarted() {
        this.globalCursorStyle = document.createElement('style');
        document.body.appendChild(this.globalCursorStyle);
        this.globalCursorStyle.innerHTML = `* {cursor: grabbing !important; }`;
    }

    onRearrangeDragEnded() {
        document.body.removeChild(this.globalCursorStyle);
        this.globalCursorStyle = null;
    }

    getDragStartDelay(): number {
        if (this.ui.isMobile()) {
            return 500;
        } else {
            return null;
        }
    }

    private registerVisibleItemsLimited() {
        this.maxRows$.subscribe((maxRows) => {
            this.visibleItemsLimited = maxRows > 0;
        });
    }

    private refreshDropLists() {
        this.dropLists = this.dropListsQuery.toArray();
    }

    private focusFirstNewItemWhenLoaded() {
        const oldLength = this.itemRefs.length;
        this.itemRefs.changes
            .pipe(take(1))
            .subscribe((items: NodeEntriesCardGridComponent<T>['itemRefs']) => {
                if (items.length > oldLength) {
                    this.focusOnce(items.get(oldLength).nativeElement);
                }
            });
    }

    private focusOnce(element: HTMLElement): void {
        element.setAttribute('tabindex', '-1');
        element.focus();
        element.addEventListener('blur', () => element.removeAttribute('tabindex'), { once: true });
    }

    onGridSizeChanges(): void {
        const itemsPerRow = this.getItemsPerRow();
        this.itemsPerRowSubject.next(itemsPerRow);
        this.updateScrollState();
    }

    private getItemsPerRow(): number | null {
        if (!this.gridRef?.nativeElement) {
            return null;
        }
        return getComputedStyle(this.gridRef.nativeElement)
            .getPropertyValue('grid-template-columns')
            .split(' ').length;
    }

    getSortColumns() {
        return this.entriesService.sort?.columns?.filter((c) => {
            const result = this.entriesService.columns
                .concat(
                    new ListItemSort('NODE', 'score'),
                    new ListItemSort('NODE', RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION),
                    new ListItemSort('NODE', RestConstants.CM_PROP_TITLE),
                    new ListItemSort('NODE', RestConstants.CM_NAME),
                    new ListItemSort('NODE', RestConstants.CM_PROP_C_CREATED),
                    new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
                    new ListItemSort('NODE', RestConstants.CCM_PROP_REPLICATIONMODIFIED),
                    new ListItemSort('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCETIMESTAMP),
                )
                .some((c2) => c.type === c2.type && c2.name === c.name);
            if (!result && !this.entriesService.configureColumns) {
                return this.entriesService.sort?.columns;
                /*
                    const warning =
                        'Sort field ' +
                        c.name +
                        ' was specified but is not present as a column. ' +
                        'It will be ignored. Please also configure this field in the <lists> section';
                    if (!displayedWarnings.includes(warning)) {
                        console.warn(warning);
                        displayedWarnings.push(warning);
                    }*/
            }
            return result;
        });
    }

    canDropNodes = (dragData: DragData<T>) => this.entriesService.dragDrop.dropAllowed?.(dragData);

    onNodesDropped(dragData: DragData<Node>) {
        this.entriesService.dragDrop.dropped(dragData.target, {
            element: dragData.draggedNodes,
            mode: dragData.action,
        });
    }

    getDragEnabled() {
        return this.ui.isTouchSubject.pipe(
            map((touch) => !touch && this.entriesService.dragDrop?.dragAllowed),
        );
    }

    getDragData(node: T): T[] {
        const selection = this.entriesService.selection;
        if (selection.isSelected(node)) {
            return selection.selected;
        } else {
            return [node];
        }
    }

    onDragStarted(node: T) {
        if (!this.entriesService.selection.isSelected(node)) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(node);
        }
        this.isDragging = true;
    }

    onDragEnded() {
        this.isDragging = false;
    }

    private canScroll(direction: 'left' | 'right') {
        const element = this.gridRef?.nativeElement;
        if (element) {
            if (direction === 'left') {
                return element.scrollLeft > 0;
            } else if (direction === 'right') {
                /*
                 use a small pixel buffer (10px) because scrolling aligns with the start of each card and
                 it can cause slight alignment issues on the end of the container
                 */
                return element.scrollLeft < element.scrollWidth - element.clientWidth - 10;
            }
        }
        return false;
    }

    updateScrollState() {
        if (this.layout === 'scroll') {
            this.scroll.left = this.canScroll('left');
            this.scroll.right = this.canScroll('right');
        }
    }

    doScroll(direction: 'left' | 'right') {
        // 1 is enough because the browser will handle it via css snapping
        const leftScroll = this.gridRef?.nativeElement.scrollLeft;
        const rect = this.gridRef?.nativeElement.getBoundingClientRect();
        // using scroll because it works more reliable than scrollBy
        this.gridRef?.nativeElement.scroll({
            left:
                leftScroll +
                Math.max(250, rect.width * this.ScrollingOffsetPercentage) *
                    (direction === 'right' ? 1 : -1),
            behavior: 'smooth',
        });
    }

    isCustomTemplate() {
        return this.entriesService.dataSource instanceof CustomTemplatesDataSource;
    }

    isBlocked(node: Node) {
        return node.properties?.[RestConstants.CCM_PROP_IMPORT_BLOCKED]?.[0] === 'true';
    }
}
