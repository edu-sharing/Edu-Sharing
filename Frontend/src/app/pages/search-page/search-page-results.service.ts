import { Injectable, Injector, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
    MdsDefinition,
    MdsQueryCriteria,
    MdsService,
    Node,
    SearchService,
} from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import {
    debounceTime,
    distinctUntilChanged,
    filter,
    first,
    map,
    pairwise,
    share,
    startWith,
    switchMap,
    takeUntil,
    tap,
} from 'rxjs/operators';
import {
    ListItem,
    ListItemSort,
    ListSortConfig,
    MdsHelperService,
    NodeEntriesDisplayType,
    notNull,
    SearchHelperService,
} from 'ngx-edu-sharing-ui';
import {
    fromSearchResults,
    NodeDataSourceRemote,
    NodeRemote,
    NodeRequestParams,
} from './node-data-source-remote';
import { SearchPageRestoreService } from './search-page-restore.service';
import { SearchPageService, SearchRequestParams } from './search-page.service';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { MdsWidgetType } from 'src/app/features/mds/types/types';
import { RestSearchService } from 'src/app/core-module/core.module';

export interface SearchPageResults {
    diffCount?: Observable<number>;
    totalResults?: Observable<number>;
    loadingProgress: Observable<number>;
    addNodes: (nodes: Node[]) => void;
}
export interface SearchPageState {
    displayType: NodeEntriesDisplayType;
    sortConfig: ListSortConfig;
}

@Injectable()
export class SearchPageResultsService implements SearchPageResults, OnDestroy {
    readonly resultsDataSource = new NodeDataSourceRemote(this._injector);
    readonly totalResults = this.resultsDataSource.observeTotal();
    readonly collectionsDataSource = new NodeDataSourceRemote(this._injector);
    readonly resultColumns = new BehaviorSubject<ListItem[]>([]);
    readonly collectionColumns = new BehaviorSubject<ListItem[]>([]);
    readonly loadingParams = new BehaviorSubject<boolean>(true);
    readonly loadingContent = new BehaviorSubject<boolean>(true);
    readonly loadingCollections = new BehaviorSubject<boolean>(true);
    readonly loadingProgress = new BehaviorSubject<number>(0);
    readonly diffCount = new BehaviorSubject<number>(0);
    // stores the state of the primary, configurable node entries component
    readonly state = new BehaviorSubject<SearchPageState>({
        displayType: NodeEntriesDisplayType.Grid,
        sortConfig: null,
    });

    private readonly _destroyed = new Subject<void>();

    constructor(
        private _injector: Injector,
        private _mds: MdsService,
        private _search: SearchService,
        private _searchPage: SearchPageService,
        private _searchPageRestore: SearchPageRestoreService,
        private _translate: TranslateService,
    ) {
        this._registerPageRestore();
        this._registerSearchObservables();
        this._registerColumnsAndSortConfig();
        this._registerLoadingProgress();
        this._registerResultDiffCount();
    }

    ngOnDestroy(): void {
        this._destroyed.next();
        this._destroyed.complete();
    }

    addNodes(nodes: Node[]): void {
        this.resultsDataSource.appendData(nodes, 'before');
    }

    private _registerPageRestore() {
        // restore last state
        if (this._searchPageRestore.getRestoreEntry()) {
            this.state.next(this._searchPageRestore.getRestoreEntry().searchState);
        }
        this._searchPageRestore.registerDataSource('materials', this.resultsDataSource);
        this._searchPageRestore.registerDataSource('collections', this.collectionsDataSource);
        this._searchPageRestore.registerSearchState(this.state);
    }

    private _registerSearchObservables() {
        const searchRequestParams: Observable<SearchRequestParams> = rxjs
            .combineLatest([
                this._searchPage.activeRepository.observeValue(),
                // .pipe(tap((value) => console.log('activeRepository changed', value))),
                this._searchPage.activeMetadataSet.observeValue(),
                // .pipe(tap((value) => console.log('activeMetadataSet changed', value))),
                this._searchPage.searchFilters.observeValue(),
                // .pipe(tap((value) => console.log('searchFilters changed', value))),
                this._searchPage.searchString.observeValue(),
                // .pipe(tap((value) => console.log('searchString changed', value))),
            ])
            .pipe(
                takeUntil(this._destroyed),
                tap(() => this.loadingParams.next(true)),
                // Search filters and facets to fetch depend on the active repository and metadata
                // set. Their values will be set to `null` while data is being determined after
                // repository or metadata set changed. Give other components a tick to do this, so
                // we don't prematurely send a search request with outdated data.
                debounceTime(0),
                filter(
                    ([repository, metadataSet, searchFilters]) =>
                        notNull(repository) && notNull(metadataSet) && notNull(searchFilters),
                ),
                // Wait until the filter bar's MDS instance has registered its needed facets for
                // suggestions at the search service. We don't explicitly include the facets in the
                // search request here to let the search service decide not to update the facets
                // when not needed (e.g., when loading a new page). See comments on
                // `MdsEditorWrapperComponent.registerLegacySuggestions` for further context.
                switchMap((values) =>
                    this._searchPage.facetsToFetch.pipe(
                        first(notNull),
                        map(() => values),
                    ),
                ),
                tap(() => this.loadingParams.next(false)),
                map(
                    ([repository, metadataSet, searchFilters, searchString]) =>
                        new SearchRequestParams(
                            repository,
                            metadataSet,
                            searchFilters,
                            searchString,
                        ),
                ),
                distinctUntilChanged((x, y) => x.equals(y)),
                share(),
            );
        const collectionRequestParams: Observable<SearchRequestParams> = searchRequestParams.pipe(
            // Omit searchFilters.
            map(
                ({ repository, metadataSet, searchString }) =>
                    new SearchRequestParams(repository, metadataSet, {}, searchString),
            ),
            distinctUntilChanged((x, y) => x.equals(y)),
        );
        searchRequestParams
            .pipe(
                tap(() => this.loadingContent.next(true)),
                map((params) => this._getSearchRemote(params)),
            )
            .subscribe((remote) => this.resultsDataSource.setRemote(remote));

        collectionRequestParams
            .pipe(
                tap(() => this.loadingCollections.next(true)),
                map((params) => this._getCollectionsSearchRemote(params)),
            )
            .subscribe((remote) => this.collectionsDataSource.setRemote(remote));
        this.resultsDataSource.isLoadingSubject.subscribe((isLoading) =>
            this.loadingContent.next(!!isLoading),
        );
        this.collectionsDataSource.isLoadingSubject.subscribe((isLoading) =>
            this.loadingCollections.next(!!isLoading),
        );
    }

    private _registerColumnsAndSortConfig(): void {
        // Get MDS definition.
        const mds: Observable<MdsDefinition> = rxjs
            .combineLatest([
                this._searchPage.activeRepository.observeValue(),
                this._searchPage.activeMetadataSet.observeValue(),
            ])
            .pipe(
                takeUntil(this._destroyed),
                filter(([repository, metadataSet]) => notNull(repository) && notNull(metadataSet)),
                switchMap(([repository, metadataSet]) =>
                    this._mds.getMetadataSet({ repository, metadataSet }),
                ),
            );
        // Register columns.
        mds.pipe(
            map((mds) => MdsHelperService.getColumns(this._translate, mds, 'search')),
        ).subscribe(this.resultColumns);
        mds.pipe(
            map((mds) => MdsHelperService.getColumns(this._translate, mds, 'searchCollections')),
        ).subscribe(this.collectionColumns);
        // Register sort.
        mds.pipe(map((mds) => MdsHelperService.getSortInfo(mds, 'search'))).subscribe(
            (sortInfo) => {
                if (this.state.value.sortConfig === null) {
                    this.patchState({
                        sortConfig: {
                            allowed: true,
                            active: sortInfo.default.sortBy,
                            direction: sortInfo.default.sortAscending ? 'asc' : 'desc',
                            columns: sortInfo.columns?.map(
                                ({ id, mode }) =>
                                    new ListItemSort(
                                        'NODE',
                                        id,
                                        mode as 'ascending' | 'descending',
                                    ),
                            ),
                        },
                    });
                }
            },
        );
    }
    patchState(data: Partial<SearchPageState>) {
        this.state.next({ ...this.state.value, ...data });
    }

    private _getSearchRemote(params: SearchRequestParams): NodeRemote<Node> {
        // console.log('%cgetSearchRemote', 'font-weight: bold', params);
        return (request: NodeRequestParams) => {
            // console.log('search', request);
            return this._search
                .search({
                    body: {
                        criteria: this._getSearchCriteria(params),
                        facetLimit: 5,
                        facetMinCount: 1,
                        resolveCollections: false,
                        resolveUsernames: false,
                        permissions: this._searchPage.reUrl.value
                            ? [RestConstants.ACCESS_CC_PUBLISH]
                            : [],
                    },
                    maxItems: request.range.endIndex - request.range.startIndex,
                    skipCount: request.range.startIndex,
                    sortAscending: request.sort ? [request.sort.direction === 'asc'] : null,
                    sortProperties: request.sort ? [request.sort.active] : null,
                    contentType: 'FILES',
                    repository: params.repository,
                    metadataset: params.metadataSet,
                    query: RestConstants.DEFAULT_QUERY_NAME,
                    propertyFilter: [RestConstants.ALL],
                })
                .pipe(
                    tap((r) => {
                        if (r.pagination.total < r.pagination.count) {
                            console.warn(
                                'Total count of items is smaller than total, results might be truncated, check pagination results of api',
                                r.pagination,
                            );
                        }
                    }),
                    map(fromSearchResults),
                );
        };
    }

    private _getCollectionsSearchRemote(params: SearchRequestParams): NodeRemote<Node> {
        const repository = this._searchPage.availableRepositories.value.find(
            ({ id }) => id === params.repository,
        );
        if (
            // We cannot show collections for another repository.
            !repository.isHomeRepo ||
            // We don't show other collections when searching for material to add to a collection.
            this._searchPage.addToCollectionMode.value
        ) {
            this.loadingCollections.next(false);
            return () => rxjs.of({ data: [], total: 0 });
        }
        return (request: NodeRequestParams) => {
            return this._search
                .requestSearch({
                    body: {
                        criteria: this._getSearchCriteria(params),
                        facets: [],
                    },
                    sortProperties: [
                        RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
                        RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
                        RestConstants.CM_MODIFIED_DATE,
                    ],
                    sortAscending: [false, true, false],
                    maxItems: request.range.endIndex - request.range.startIndex,
                    skipCount: request.range.startIndex,
                    // sortAscending: request.sort ? [request.sort.direction === 'asc'] : null,
                    // sortProperties: request.sort ? [request.sort.active] : null,
                    contentType: 'ALL', // This is now handled via mds queries
                    repository: params.repository,
                    metadataset: params.metadataSet,
                    query: RestConstants.QUERY_NAME_COLLECTIONS,
                    propertyFilter: [RestConstants.ALL],
                })
                .pipe(map(fromSearchResults));
        };
    }

    private _getSearchCriteria(params: SearchRequestParams): MdsQueryCriteria[] {
        let criteria: MdsQueryCriteria[] = Object.entries(params.searchFilters ?? {}).map(
            ([property, values]) => ({ property, values }),
        );
        this.convertCriteria(criteria);
        if (params.searchString) {
            criteria.push({ property: 'ngsearchword', values: [params.searchString] });
        }
        return criteria;
    }

    private _registerLoadingProgress(): void {
        rxjs.combineLatest([this.loadingParams, this.loadingContent, this.loadingCollections])
            .pipe(
                map(([loadingParams, loadingContent, loadingCollections]) => {
                    if (loadingParams) {
                        return 10;
                    } else {
                        return 40 + (loadingContent ? 0 : 30) + (loadingCollections ? 0 : 30);
                    }
                }),
                debounceTime(0),
                distinctUntilChanged(),
            )
            .subscribe((progress) => this.loadingProgress.next(progress));
    }

    private _registerResultDiffCount(): void {
        this.resultsDataSource
            .connect()
            .pipe(
                takeUntil(this._destroyed),
                filter(notNull),
                map((nodes) => nodes.length),
                startWith(0),
                pairwise(),
            )
            .subscribe(([previousCount, currentCount]) => {
                this.diffCount.next(currentCount - previousCount);
            });
    }

    // TODO: Port `unfoldTrees` methods from 8.0. See
    // https://scm.edu-sharing.com/edu-sharing/community/repository/edu-sharing-angular-core-module/-/blob/5447ea837a99a3dab04395c10464dd417ddb73a1/rest/services/rest-search.service.ts#L34.
    // Also consider a backend solution.
    private convertCriteria(criteria: MdsQueryCriteria[]): void {
        for (const c of criteria) {
            // We get the widget definition from the MDS editor instance, so overrides with `data-`
            // attributes in the MDS template are reflected.
            const widget = this._searchPage.filtersMdsWidgets.value?.find(
                (widget) => widget.definition.id === c.property,
            )?.definition;
            if (widget?.type === MdsWidgetType.MultiValueTree) {
                // For multi-value-tree widgets, add all child values of selected values to the
                // search criteria.
                let attach = MdsService.unfoldTreeChilds(c.values, widget);
                if (attach) {
                    if (attach.length > SearchHelperService.MAX_QUERY_CONCAT_PARAMS) {
                        console.info(
                            'param ' +
                                c.property +
                                ' has too many unfold childs (' +
                                attach.length +
                                '), falling back to basic prefix-based search',
                        );
                    } else {
                        c.values = c.values.concat(attach);
                    }
                }
            }
        }
    }
}
