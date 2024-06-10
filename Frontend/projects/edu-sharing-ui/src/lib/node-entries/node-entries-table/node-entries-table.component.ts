import { CdkOverlayOrigin } from '@angular/cdk/overlay';
import {
    AfterViewInit,
    ApplicationRef,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    NgZone,
    OnChanges,
    OnDestroy,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort, MatSortHeader, Sort } from '@angular/material/sort';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import {
    debounceTime,
    delay,
    distinctUntilChanged,
    first,
    map,
    shareReplay,
    startWith,
    takeUntil,
} from 'rxjs/operators';
import {
    ClickSource,
    InteractionType,
    NodeEntriesDataType,
    NodeEntriesDisplayType,
} from '../entries-model';
import { NodeEntriesGlobalService } from '../node-entries-global.service';
import { NodeEntriesService } from '../../services/node-entries.service';
import { UIService } from '../../services/ui.service';
import { BorderBoxObserverDirective } from '../../directives/border-box-observer.directive';
import { ListItem } from '../../types/list-item';
import { CanDrop, DragData } from '../../types/drag-drop';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { Target } from '../../types/option-item';
import { Toast } from '../../services/abstract/toast.service';
import { DropdownComponent } from '../../dropdown/dropdown.component';
import { NodeDataSourceRemote } from '../node-data-source-remote';
import { TranslationsService } from '../../translations/translations.service';

@Component({
    selector: 'es-node-entries-table',
    templateUrl: './node-entries-table.component.html',
    styleUrls: ['./node-entries-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeEntriesTableComponent<T extends NodeEntriesDataType>
    implements OnChanges, AfterViewInit, OnDestroy
{
    readonly InteractionType = InteractionType;
    readonly ClickSource = ClickSource;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly Target = Target;

    @ViewChild(MatSort) sort: MatSort;
    @ViewChild(MatPaginator) paginator: MatPaginator;
    @ViewChild('columnChooserTrigger') columnChooserTrigger: CdkOverlayOrigin;
    @ViewChild(DropdownComponent) dropdown: DropdownComponent;
    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;

    dropdownLeft: number;
    dropdownTop: number;

    loading: Observable<boolean>;
    isPageSelected = new BehaviorSubject(false);
    isAllSelected = new BehaviorSubject(false);
    columnChooserVisible = false;
    columnChooserTriggerReady = false;
    error: Observable<any>;
    pageSizeOptions = [25, 50, 100];
    isDragging = false;

    private readonly maximumColumnsNumber$ = new BehaviorSubject(1);
    readonly visibleDataColumns$;
    readonly visibleColumnNames$;

    private destroyed = new Subject<void>();

    constructor(
        public entriesService: NodeEntriesService<T>,
        public entriesGlobalService: NodeEntriesGlobalService,
        private applicationRef: ApplicationRef,
        private toast: Toast,
        private translations: TranslationsService,
        private changeDetectorRef: ChangeDetectorRef,
        public ui: UIService,
        private ngZone: NgZone,
        private elementRef: ElementRef<HTMLElement>,
    ) {
        this.visibleDataColumns$ = this.getVisibleDataColumns();
        this.visibleColumnNames$ = this.getVisibleColumnNames();
        this.registerMaximumColumnsNumber();
        this.entriesService.selection.changed
            .pipe(takeUntil(this.destroyed), debounceTime(0))
            .subscribe(() => this.changeDetectorRef.detectChanges());
    }

    ngAfterViewInit(): void {
        void Promise.resolve().then(() => {
            this.registerSortChanges();
            if (this.entriesService.dataSource instanceof NodeDataSourceRemote) {
                (this.entriesService.dataSource as NodeDataSourceRemote).sortPanel = this.sort;
            }
        });
        this.visibleDataColumns$
            .pipe(first(), delay(0))
            .subscribe(() => (this.columnChooserTriggerReady = true));
        rxjs.combineLatest([
            this.entriesService.dataSource$.pipe(startWith(void 0 as void)),
            this.entriesService.options$.pipe(startWith(void 0 as void)),
            this.entriesService.dataSource.isLoadingSubject.pipe(startWith(void 0 as void)),
            this.entriesService.selection.changed.pipe(startWith(void 0 as void)),
            this.translations.waitForInit().pipe(startWith(void 0 as void)),
        ])
            .pipe(takeUntil(this.destroyed))
            .subscribe(() => {
                this.changeDetectorRef.detectChanges();
            });
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.updateSort();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    onRowContextMenu({ event, node }: { event: MouseEvent | Event; node: T }) {
        if (!this.entriesService.selection.selected.includes(node)) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(node);
        }
        event.stopPropagation();
        event.preventDefault();
        if (event instanceof MouseEvent) {
            ({ clientX: this.dropdownLeft, clientY: this.dropdownTop } = event);
        } else {
            ({ x: this.dropdownLeft, y: this.dropdownTop } = (
                event.target as HTMLElement
            ).getBoundingClientRect());
        }
        // Wait for the menu to reflect changed options.
        setTimeout(() => {
            if (this.dropdown.canShowDropdown()) {
                this.menuTrigger.openMenu();
            } else {
                this.toast.toast('NO_AVAILABLE_OPTIONS');
            }
        });
    }

    private updateSort(): void {
        this.sort.sort({
            id: this.entriesService.sort?.active,
            start: this.entriesService.sort?.direction as 'asc' | 'desc',
            disableClear: false,
        });
        // Fix missing sorting indicators. See
        // https://github.com/angular/components/issues/10242#issuecomment-470726829. Seems
        // to be fixed upstream with Angular 11.
        (
            this.sort.sortables.get(this.entriesService.sort?.active) as MatSortHeader
        )._setAnimationTransitionState({
            toState: 'active',
        });
        /*
        this.route.queryParams.pipe(first()).subscribe((queryParams: Params) => {
            const sort: Sort = queryParams.sort ? JSON.parse(queryParams.sort) : null;
            if (sort && sort.direction) {
                this.sort.sort({ id: sort.active, start: sort.direction, disableClear: false });
                // Fix missing sorting indicators. See
                // https://github.com/angular/components/issues/10242#issuecomment-470726829. Seems
                // to be fixed upstream with Angular 11.
                (
                    this.sort.sortables.get(sort.active) as MatSortHeader
                )._setAnimationTransitionState({
                    toState: 'active',
                });
            }
            if (queryParams.pageIndex && queryParams.pageSize) {
                this.paginator.pageIndex = queryParams.pageIndex;
                this.paginator.pageSize = queryParams.pageSize;
            }
        });
         */
    }

    private registerMaximumColumnsNumber() {
        BorderBoxObserverDirective.observeElement(this.elementRef)
            .pipe(
                takeUntil(this.destroyed),
                map((box) => this.getMaximumColumnsNumber(box.width)),
                distinctUntilChanged(),
            )
            .subscribe((maximumColumnsNumber) =>
                this.ngZone.run(() => this.maximumColumnsNumber$.next(maximumColumnsNumber)),
            );
    }

    private getMaximumColumnsNumber(tableWidth: number): number {
        return Math.max(
            1,
            Math.floor(
                // Subtract total width of always visible columns like checkboxes and icons.
                (tableWidth - 187) /
                    // Divide by with of data columns (including margin).
                    126,
            ),
        );
    }

    private getVisibleDataColumns(): Observable<ListItem[]> {
        return rxjs
            .combineLatest([this.maximumColumnsNumber$, this.entriesService.columnsSubject])
            .pipe(
                map(([maximumColumnsNumber, columns]) => {
                    return (columns ?? [])
                        .filter((column) => column.visible)
                        .filter((_, index) => index < maximumColumnsNumber);
                }),
                shareReplay(1),
            );
    }

    private getVisibleColumnNames(): Observable<string[]> {
        return this.visibleDataColumns$.pipe(
            map((visibleDataColumns) => {
                const columns = [];
                if (this.entriesService.checkbox) {
                    columns.push('select');
                }
                columns.push('icon');
                return columns.concat(visibleDataColumns.map((c) => c.name)).concat(['actions']);
            }),
            shareReplay(1),
        );
    }

    isSortable(column: ListItem) {
        return this.entriesService.sort?.columns?.some(
            (c) => c.type === column.type && c.name === column.name,
        );
    }

    toggleAll(checked: boolean) {
        if (checked) {
            this.entriesService.selection.select(...this.entriesService.dataSource.getData());
        } else {
            this.entriesService.selection.clear();
        }
    }

    private registerSortChanges() {
        this.sort.sortChange.subscribe((sort: Sort) => {
            this.entriesService.sort.active = sort.active;
            this.entriesService.sort.direction = sort.direction;
            this.entriesService.sortChange.emit(this.entriesService.sort);
            /*this.router.navigate(['.'], {
                relativeTo: this.route,
                queryParams: { sort: JSON.stringify(sort) },
                queryParamsHandling: 'merge',
                replaceUrl: true,
            });*/
        });
        /*
        this.paginator.page
            .pipe(
                // As a response to changes of other parameters, the pageIndex might be reset to 0 and a
                // page event triggers. This change of other parameters is likely to cause a
                // `router.navigate()` call elsewhere. When this happens just before our call, our
                // updates are ignored. To shield against this, we wait a tick.
                delay(0),
            )
            .subscribe(({ pageIndex, pageSize }: PageEvent) => {
                this.router.navigate(['.'], {
                    relativeTo: this.route,
                    queryParams: { pageIndex, pageSize },
                    queryParamsHandling: 'merge',
                    replaceUrl: true,
                });
            });
         */
    }

    canDrop = (dragData: DragData<T>): CanDrop => {
        return this.entriesService.dragDrop.dropAllowed?.(dragData);
    };

    drop(dragData: DragData<Node>) {
        this.entriesService.dragDrop.dropped(dragData.target, {
            element: dragData.draggedNodes,
            mode: dragData.action,
        });
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

    loadData(source: 'scroll' | 'button') {
        // TODO: focus next item when triggered via button.
        this.entriesService.loadMore(source);
    }

    async openMenu(node: T) {
        this.entriesService.selection.clear();
        this.entriesService.selection.select(node);
        this.entriesService.selection.clickSource = ClickSource.Dropdown;
        await this.applicationRef.tick();
        this.dropdown.menu.focusFirstItem();
    }

    isBlocked(node: Node) {
        return node.properties?.[RestConstants.CCM_PROP_IMPORT_BLOCKED]?.[0] === 'true';
    }
}
