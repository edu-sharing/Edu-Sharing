/**
 * Some code from https://github.com/angular/components/blob/15.0.x/src/material/table/table-data-source.ts
 */

import { Injector } from '@angular/core';
import { Sort } from '@angular/material/sort';
import { MatTableDataSourcePageEvent, MatTableDataSourcePaginator } from '@angular/material/table';
import { ActivatedRoute } from '@angular/router';
import { GenericAuthority, Node, SearchResults } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, ReplaySubject, Subject, Subscription } from 'rxjs';
import { debounceTime, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import {
    ItemsCap,
    NodeCache,
    NodeCacheRange,
    NodeCacheSlice,
    NodeDataSourceRemote as NodeDataSourceRemoteBase,
    PaginationConfig,
    SortPanel,
} from 'ngx-edu-sharing-ui';
import { UserModifiableValuesService } from './user-modifiable-values';

type Range = NodeCacheRange;

export interface NodeRequestParams {
    range: Range;
}

export interface NodeResponse<T> {
    data: T[];
    total: number;
}

// TODO: Rename to something like "fetch implementation" or "request handler"
export type NodeRemote<T> = (params: NodeRequestParams) => Observable<NodeResponse<T>>;
export class NodeDataSourceRemoteState {
    _cache: {};
    _pageIndex: number;
    _pageSize: number;
    _length: number;
}

let nextId = 0;

export class NodeDataSourceRemote<
    T extends Node | GenericAuthority = Node,
    P extends MatTableDataSourcePaginator = MatTableDataSourcePaginator,
> extends NodeDataSourceRemoteBase<T, P> {
    get paginator(): P | null {
        return this._paginationHandler.paginator;
    }
    set paginator(value: P | null) {
        this._paginationHandler.paginator = value;
    }
    get sortPanel(): SortPanel | null {
        return this._sortHandler.sortPanel;
    }
    set sortPanel(value: SortPanel | null) {
        this._sortHandler.sortPanel = value;
    }
    private _paginationHandler = new PaginationHandler<P>(this._injector);
    private _sortHandler = new SortHandler(this._injector);

    private _remote: NodeRemote<T>;
    private _renderChangesSubscription: Subscription | null = null;
    private _cache = new NodeCache<T>();
    set itemsCap(value: ItemsCap<T> | null) {
        this._itemsCap = value;
        // Only reconnect if already connected.
        if (this.renderDataSubscription) {
            this._connectRenderData();
        }
    }
    // Even if the data source is not in a loading state until initialized, we expect that the
    // required data is prepared elsewhere and we already show the loading spinner.
    private _isLoading = new BehaviorSubject<boolean>(true);
    private _initDone = false;
    private _resetDone = false;
    private _restoreFunction: () => NodeDataSourceRemoteState;
    id = nextId++;

    constructor(private _injector: Injector) {
        super();
        this._registerLoadingState();
    }

    connect(): Observable<T[]> {
        if (!this.renderDataSubscription) {
            this._connectRenderData();
        }
        return this.renderData;
    }

    disconnect() {}

    init({
        paginationConfig,
        defaultSort,
    }: {
        paginationConfig: PaginationConfig;
        defaultSort: Sort;
    }): void {
        this._paginationHandler.init(paginationConfig);
        this._sortHandler.init(defaultSort);
    }

    setRemote(remote: NodeRemote<T>): void {
        this._remote = remote;
        this._updateRemoteSubscription();
    }

    getData() {
        return this.renderData.value;
    }

    isEmpty(): boolean {
        return this.dataStream.value?.length === 0;
    }

    hasMore() {
        return this._paginationHandler.hasMore();
    }

    loadMore() {
        return this._paginationHandler.loadMore();
    }

    getTotal() {
        return this._paginationHandler.length;
    }

    observeTotal() {
        return this.dataStream.pipe(map(() => this._paginationHandler.length));
    }

    appendData(appendData: T[], location: 'before' | 'after' = 'after') {
        let data = this.dataStream.value;
        if (location === 'after') {
            data = data.concat(appendData);
        } else {
            data = appendData.concat(data);
        }
        this.dataStream.next(data);
        this._paginationHandler.length += appendData.length;
        this._cache.clear();
    }

    removeData(data: T[]): void {
        const currentData = this.dataStream.value;
        const deleteCount = currentData.filter((entry) => data.includes(entry)).length;
        const updatedData = currentData.filter((entry) => !data.includes(entry));
        this.dataStream.next(updatedData);
        this._paginationHandler.length -= deleteCount;
        this._cache.clear();
    }

    registerQueryParameters(route: ActivatedRoute): void {
        this._paginationHandler.registerQueryParameters(route);
        this._sortHandler.registerQueryParameters(route);
    }

    dumpState(): NodeDataSourceRemoteState {
        // With this, we implicitly test for `_initDone` and `_resetDone`.
        console.assert(this.isLoading === false, 'dumping state not supported while loading');
        return {
            _cache: { ...this._cache },
            _pageIndex: this._paginationHandler.pageIndex,
            _pageSize: this._paginationHandler.pageSize,
            _length: this._paginationHandler.length,
        };
    }

    registerRestoreFunction(restoreFunction: () => NodeDataSourceRemoteState): void {
        this._restoreFunction = restoreFunction;
    }

    private _restoreState(state: NodeDataSourceRemoteState) {
        Object.assign(this._cache, state._cache);
        this._paginationHandler.pageIndex = state._pageIndex;
        this._paginationHandler.pageSize = state._pageSize;
        this._paginationHandler.length = state._length;
    }

    private _resetState(): void {
        const restoreState = this._restoreFunction?.();
        if (restoreState) {
            this._restoreState(restoreState);
        } else {
            this._paginationHandler.length = null;
            if (this._initDone) {
                this._paginationHandler.firstPage();
            }
            this._cache.clear();
        }
    }

    private _registerLoadingState(): void {
        this._isLoading.subscribe((isLoading) => {
            if (!isLoading) {
                this.isLoading = false;
            } else if (!this._initDone) {
                this.isLoading = 'initial';
            } else if (!this._resetDone) {
                this.isLoading = 'reset';
            } else {
                this.isLoading = 'page';
            }
        });
    }

    private _connectRenderData(): void {
        this.renderDataSubscription?.unsubscribe();
        if (this.itemsCap) {
            this.renderDataSubscription = this.itemsCap
                .connect(this.dataStream)
                .subscribe((data) => this.renderData.next(data));
        } else {
            this.renderDataSubscription = this.dataStream.subscribe((data) =>
                this.renderData.next(data),
            );
        }
    }

    private _updateRemoteSubscription(): void {
        this._resetDone = false;
        this._resetState();
        const sortChange = rxjs.merge(
            this._sortHandler.sortChange.pipe(
                tap(({ source }) => {
                    this._resetDone = false;
                    this._cache.clear();
                    if (source === 'user') {
                        this._paginationHandler.firstPage();
                    }
                }),
            ),
            this._sortHandler.initialized,
        );
        const pageChange: Observable<MatTableDataSourcePageEvent | void> = rxjs.merge(
            this._paginationHandler.pageChange,
            //   this._internalPageChanges,
            this._paginationHandler.initialized,
        );

        this._renderChangesSubscription?.unsubscribe();
        this._renderChangesSubscription = rxjs
            .combineLatest([sortChange, pageChange])
            .pipe(
                // Don't send multiple requests in case a sort change triggers a page change.
                debounceTime(0),
                map(() => this._cache.getMissingRange(this._getRequestRange())),
                tap((missingRange) => missingRange && this._isLoading.next(true)),
                switchMap((missingRange) => this._downloadAndCache(missingRange)),
                map(() => this._cache.get(this._getDisplayRange())),
                tap(() => {
                    this._initDone = true;
                    this._resetDone = true;
                }),
                tap({
                    next: () => this._isLoading.next(false),
                    error: () => this._isLoading.next(false),
                }),
            )
            .subscribe((data) => this.dataStream.next(data));
    }

    private _getRequestRange(): Range {
        const startIndex = this._paginationHandler.pageIndex * this._paginationHandler.pageSize;
        let endIndex = (this._paginationHandler.pageIndex + 1) * this._paginationHandler.pageSize;
        if (this._paginationHandler.length !== null) {
            endIndex = Math.min(endIndex, this._paginationHandler.length);
        }
        return { startIndex, endIndex };
    }

    private _getDisplayRange(): Range {
        const requestRange = this._getRequestRange();
        return {
            startIndex: requestRange.startIndex,
            endIndex: Math.min(requestRange.endIndex, this._paginationHandler.length),
        };
    }

    private _downloadAndCache(missingRange: Range): Observable<void> {
        if (missingRange) {
            return this._remote({ range: missingRange }).pipe(
                tap((response) => (this._paginationHandler.length = response.total)),
                tap((response) =>
                    this._cache.add(this._getCacheSlice(missingRange, response.data)),
                ),

                map(() => void 0),
            );
        } else {
            return rxjs.of(null);
        }
    }

    private _getCacheSlice(range: Range, data: T[]): NodeCacheSlice<T> {
        return {
            startIndex: range.startIndex,
            endIndex: Math.min(range.endIndex, range.startIndex + data.length),
            data,
        };
    }
}

export function fromSearchResults(searchResults: SearchResults): NodeResponse<Node> {
    return {
        data: searchResults.nodes,
        total: searchResults.pagination.total,
    };
}

/**
 * A layer between the data source and a paginator component or an infinite-scroll paginator.
 *
 * Without this, the data source would need to wait for a paginator component to be added to the DOM
 * to send its first request. Also, the data source would not be able to function without the
 * paginator component or directive constantly available in the DOM.
 */
class PaginationHandler<P extends MatTableDataSourcePaginator = MatTableDataSourcePaginator> {
    private _paginator: P;
    get paginator(): P {
        return this._paginator;
    }
    set paginator(value: P) {
        this._paginator = value;
        this._initPaginator(value);
    }
    private _config: PaginationConfig;
    private _pageIndex = 0;
    get pageIndex() {
        return this._pageIndex;
    }
    set pageIndex(value) {
        this._pageIndex = value;
        if (this.paginator) {
            this.paginator.pageIndex = value;
        }
    }
    private _pageSize: number;
    get pageSize(): number {
        return this._pageSize;
    }
    set pageSize(value: number) {
        this._pageSize = value;
        if (this.paginator) {
            this.paginator.pageSize = value;
        }
    }
    private _length: number = null;
    get length() {
        return this._length;
    }
    set length(value) {
        this._length = value;
        if (this.paginator) {
            this.paginator.length = value;
        }
    }

    private readonly _initialized = new ReplaySubject<void>();
    initialized = this._initialized.asObservable();
    private _isInitialized = false;
    private readonly _pageChange = new Subject<MatTableDataSourcePageEvent>();
    pageChange = this._pageChange.asObservable();
    private readonly _paginatorReset = new Subject<void>();

    constructor(private _injector: Injector) {}

    init(config: PaginationConfig): void {
        this._config = config;
        this.pageSize = config.defaultPageSize;
        this._isInitialized = true;
        this._initialized.next();
        this._initialized.complete();
    }

    firstPage(): void {
        const previousPageIndex = this.pageIndex;
        const previousPageSize = this.pageSize;
        this.pageIndex = 0;
        if (this._config.strategy === 'infinite-scroll') {
            this.pageSize = this._config.defaultPageSize;
        }
        if (previousPageIndex !== this.pageIndex || previousPageSize !== this.pageSize) {
            this._emitPageEvent();
        }
    }

    hasMore(): boolean {
        if (!this._isInitialized) {
            return undefined;
        }
        return this.length > (this.pageIndex + 1) * this.pageSize;
    }

    /**
     * @returns Whether there is more data to load
     */
    loadMore(): boolean {
        if (this._config.strategy !== 'infinite-scroll') {
            console.warn(
                `Called loadMore with pagination strategy ${this._config.strategy}.`,
                `The only supported strategy for loadMore is 'infinite-scroll'.`,
            );
            return false;
        }
        if (this.hasMore()) {
            this.pageSize = Math.min(this.pageSize + this._config.defaultPageSize, this.length);
            this._emitPageEvent();
            return true;
        } else {
            return false;
        }
    }

    registerQueryParameters(route: ActivatedRoute): void {
        if (!this._isInitialized) {
            throw new Error('Tried to register query params before initializing');
        } else if (this._config.strategy === 'infinite-scroll') {
            // Nothing to store in query params when using infinite scrolling.
            return;
        }
        const userModifiableValue = this._injector.get(UserModifiableValuesService);
        const pageIndex = userModifiableValue.createMapped<number>(
            {
                toString: (value) => (value + 1).toString(),
                fromString: (value) => parseInt(value) - 1,
            },
            0,
        );
        const pageSize = userModifiableValue.createMapped<number>(
            {
                toString: (value) => value.toString(),
                fromString: (value) => parseInt(value),
            },
            this._config.defaultPageSize,
        );
        pageIndex.registerQueryParameter('page', route);
        pageSize.registerQueryParameter('pageSize', route);
        rxjs.combineLatest([pageIndex.observeValue(), pageSize.observeValue()]).subscribe(
            ([pageIndex, pageSize]) => {
                this.pageIndex = pageIndex;
                this.pageSize = pageSize;
                this._emitPageEvent();
            },
        );
        this.pageChange.subscribe((event) => {
            pageIndex.setUserValue(event.pageIndex);
            pageSize.setUserValue(event.pageSize);
        });
    }

    private _initPaginator(paginator: MatTableDataSourcePaginator): void {
        this._paginatorReset.next();
        if (paginator) {
            paginator.pageIndex = this.pageIndex;
            paginator.pageSize = this.pageSize;
            paginator.length = this.length;
            paginator.page.pipe(takeUntil(this._paginatorReset)).subscribe((pageEvent) => {
                this.pageIndex = pageEvent.pageIndex;
                this.pageSize = pageEvent.pageSize;
                this.length = pageEvent.length;
                this._emitPageEvent();
            });
        }
    }

    private _emitPageEvent(): void {
        this._pageChange.next({
            pageIndex: this.pageIndex,
            pageSize: this.pageSize,
            length: this.length,
        });
    }
}

class SortHandler {
    private _sortPanel: SortPanel;
    get sortPanel(): SortPanel {
        return this._sortPanel;
    }
    set sortPanel(value: SortPanel) {
        this._sortPanel = value;
        this._initSortPanel(value);
    }
    private readonly _sortChange = new Subject<Sort & { source: 'query-params' | 'user' }>();
    readonly sortChange = this._sortChange.asObservable();
    private readonly _initialized = new ReplaySubject<void>();
    readonly initialized = this._initialized.asObservable();
    private _isInitialized = false;

    private _defaultSort: Readonly<Sort>;
    private _currentSort: Readonly<Sort>;
    get currentSort(): Readonly<Sort> {
        return this._currentSort;
    }
    private set currentSort(value: Readonly<Sort>) {
        this._currentSort = value;
        if (this.sortPanel) {
            this.sortPanel.active = value.active;
            this.sortPanel.direction = value.direction;
        }
    }
    private readonly _sortPanelReset = new Subject<void>();

    constructor(private _injector: Injector) {}

    init(defaultSort: Sort): void {
        defaultSort = { active: defaultSort?.active, direction: defaultSort?.direction };
        this._defaultSort = defaultSort;
        this.currentSort = defaultSort;
        this._isInitialized = true;
        this._initialized.next();
        this._initialized.complete();
    }

    private _changeSort(sort: Sort, source: 'query-params' | 'user'): void {
        if (!sort.direction) {
            sort = this._defaultSort;
        }
        if (
            this.currentSort.active !== sort.active ||
            this.currentSort.direction !== sort.direction
        ) {
            this.currentSort = { ...sort };
            this._sortChange.next({ ...sort, source });
        }
    }

    registerQueryParameters(route: ActivatedRoute): void {
        if (!this._isInitialized) {
            throw new Error('Tried to register query params before initializing');
        }
        // @TODO
        /*
        const userModifiableValue = this._injector.get(UserModifiableValuesService);
        const sortActive = userModifiableValue.createString(this._defaultSort?.active);
        const sortDirection = userModifiableValue.createString<SortDirection>(
            this._defaultSort?.direction,
        );
        sortActive.registerQueryParameter('sortBy', route, { skipScrollToTop: true });
        sortDirection.registerQueryParameter('sortDirection', route, { skipScrollToTop: true });
        rxjs.combineLatest([sortActive.observeValue(), sortDirection.observeValue()]).subscribe(
            ([active, direction]) => this._changeSort({ active, direction }, 'query-params'),
        );
        this.sortChange.subscribe((event) => {
            sortActive.setUserValue(event.active);
            sortDirection.setUserValue(event.direction);
        });
         */
    }

    private _initSortPanel(sortPanel: SortPanel): void {
        this._sortPanelReset.next();
        if (sortPanel) {
            sortPanel.active = this.currentSort?.active;
            sortPanel.direction = this.currentSort?.direction;
            sortPanel.sortChange.pipe(takeUntil(this._sortPanelReset)).subscribe((sortEvent) => {
                this._changeSort(sortEvent, 'user');
            });
        }
    }
}
