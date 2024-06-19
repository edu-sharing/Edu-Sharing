import { Injectable, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
    ClientConfig,
    CollectionService,
    ConfigService,
    HOME_REPOSITORY,
    MdsService,
    MetadataSetInfo,
    NetworkService,
    Node,
} from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { distinctUntilChanged, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { Repository, RestConstants, UIConstants } from '../../core-module/core.module';
import {
    CustomOptions,
    DefaultGroups,
    ElementType,
    HideMode,
    NodesRightMode,
    notNull,
    OptionItem,
} from 'ngx-edu-sharing-ui';
import { OptionsHelperService } from '../../services/options-helper.service';
import { MainNavService } from '../../main/navigation/main-nav.service';
import {
    SearchFieldInstance,
    SearchFieldService,
} from '../../main/navigation/search-field/search-field.service';
import { NavigationScheduler } from './navigation-scheduler';
import { SearchPageResults } from './search-page-results.service';
import { UserModifiableValuesService } from './user-modifiable-values';
import { Widget } from '../../features/mds/mds-editor/mds-editor-instance.service';

export class SearchRequestParams {
    constructor(
        public readonly repository: string,
        public readonly metadataSet: string,
        public readonly searchFilters: { [key: string]: string[] },
        public readonly searchString: string,
    ) {}

    equals(other: SearchRequestParams) {
        return JSON.stringify(this) === JSON.stringify(other);
    }
}

interface AddToCollectionMode {
    customOptions: CustomOptions;
    collection: Node;
}

@Injectable()
export class SearchPageService implements OnDestroy {
    readonly availableRepositories = new BehaviorSubject<Repository[]>(null);
    readonly activeRepository = this.userModifiableValues.createString();
    readonly showingAllRepositories = new BehaviorSubject<boolean>(null);
    readonly availableMetadataSets = new BehaviorSubject<MetadataSetInfo[]>(null);
    readonly activeMetadataSet = this.userModifiableValues.createString();
    readonly filterBarIsVisible = this.userModifiableValues.createBoolean(false);
    readonly searchFilters = this.userModifiableValues.createDict();
    readonly searchString = this.userModifiableValues.createString();
    readonly loadingProgress = new BehaviorSubject<number>(null);
    readonly reUrl = new BehaviorSubject<string | false>(null);
    readonly addToCollectionMode = new BehaviorSubject<AddToCollectionMode>(null);
    readonly filtersMdsWidgets: BehaviorSubject<Widget[]> = new BehaviorSubject(null);
    /**
     * A list of properties that should be fetched as facets with a search request.
     *
     * The facets are determined by the mds instance of the filter-bar and are used to show
     * suggestions for filter values to the user.
     *
     * Will be set to `null` while a value is being determined. Set to the empty array in case there
     * are no facets to fetch.
     */
    readonly facetsToFetch = new BehaviorSubject<string[]>(null);
    private _results = new BehaviorSubject<SearchPageResults>(null);
    get results(): SearchPageResults {
        return this._results.value;
    }
    set results(value: SearchPageResults) {
        this._results.next(value);
    }

    private readonly destroyed = new Subject<void>();

    constructor(
        private collection: CollectionService,
        private config: ConfigService,
        private mainNavService: MainNavService,
        private mds: MdsService,
        private navigationScheduler: NavigationScheduler,
        private network: NetworkService,
        private route: ActivatedRoute,
        private router: Router,
        private searchField: SearchFieldService,
        private userModifiableValues: UserModifiableValuesService,
    ) {}

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    /** Initializes the service for use on the search page. */
    init(): void {
        this.initBasicData();
        this.registerSearchField();
        this.initSearchPageUi();
        this.initQueryParams();
        this.registerAllRepositories();
        this.registerAddToCollection();
    }

    /**
     * Utilizes this service to provide facet search via a search field when not on the search page.
     *
     * When a search is triggered by either entering a search string and hitting the search button
     * or by selecting a facet, we jump to the search page.
     */
    initSearchFieldOnly(): void {
        this.initBasicData();
        const searchFieldInstance = this.registerSearchField({ autoFocusSearchField: false });
        this.initQueryParams();
        searchFieldInstance.patchConfig({ showFiltersButton: false });
        // Jump to the search page when a search is triggered.
        rxjs.merge(
            searchFieldInstance.onSearchTriggered().pipe(
                // Don't trigger a search when the search field was cleared with the 'X' button.
                filter(({ cleared }) => !cleared),
            ),
            searchFieldInstance.onFilterValuesChanged(),
        ).subscribe(() => {
            this.navigationScheduler.scheduleNavigation({
                route: [UIConstants.ROUTER_PREFIX, 'search'],
            });
        });
    }

    private initBasicData(): void {
        this.registerRepositories();
        this.registerAvailableMetadataSets();
        this.registerActiveMetadataSet();
    }

    private initSearchPageUi(): void {
        this.registerLoadingProgress();
    }

    private initQueryParams(): void {
        this.activeRepository.registerQueryParameter('repo', this.route);
        this.activeMetadataSet.registerQueryParameter('mds', this.route);
        this.searchFilters.registerQueryParameter('filters', this.route);
        this.searchString.registerQueryParameter('q', this.route);
        this.filterBarIsVisible.registerSessionStorage('search-page-filter-bar');
        this.route.queryParams.pipe(map((params) => params.reurl || false)).subscribe(this.reUrl);
    }

    private registerRepositories(): void {
        rxjs.combineLatest([this.network.getRepositories(), this.config.observeConfig()]).subscribe(
            ([repositories, config]) =>
                this.availableRepositories.next(filterRepositories(repositories, config)),
        );
        this.availableRepositories.pipe(filter(notNull)).subscribe((availableRepositories) => {
            this.activeRepository.setSystemValue(availableRepositories[0].id);
            if (!availableRepositories.some(({ id }) => id === this.activeRepository.getValue())) {
                this.activeRepository.resetUserValue();
            }
        });
    }

    private registerAllRepositories(): void {
        this.showingAllRepositories.subscribe((showingAllRepositories) => {
            if (showingAllRepositories) {
                this.activeRepository.setOverrideValue(null);
                this.filterBarIsVisible.resetUserValue();
            } else {
                this.activeRepository.unsetOverrideValue();
            }
        });
    }

    private registerAvailableMetadataSets(): void {
        this.activeRepository
            .observeValue()
            .pipe(
                filter(notNull),
                tap(() => this.availableMetadataSets.next(null)),
                switchMap((activeRepository) =>
                    this.availableRepositories.pipe(
                        filter(notNull),
                        map((repositories) => repositories.find((r) => r.id === activeRepository)),
                    ),
                ),
                switchMap((repository) => this.getAvailableMetadataSets(repository)),
            )
            .subscribe((availableMetadataSets) =>
                this.availableMetadataSets.next(availableMetadataSets),
            );
    }

    getAvailableMetadataSets(repository: Repository): Observable<MetadataSetInfo[]> {
        return rxjs
            .combineLatest([
                this.mds.getAvailableMetadataSets(repository.id),
                this.config.observeConfig(),
            ])
            .pipe(
                map(([availableMetadataSets, config]) =>
                    filterMetadataSets(availableMetadataSets, config, repository),
                ),
            );
    }

    private registerActiveMetadataSet(): void {
        this.availableMetadataSets.subscribe((availableMetadataSets) => {
            if (!availableMetadataSets) {
                this.activeMetadataSet.setOverrideValue(null);
            } else {
                this.activeMetadataSet.unsetOverrideValue();
            }
            this.activeMetadataSet.setSystemValue(availableMetadataSets?.[0]?.id);
            if (
                availableMetadataSets &&
                !availableMetadataSets.some((mds) => mds.id === this.activeMetadataSet.getValue())
            ) {
                this.activeMetadataSet.resetUserValue();
            }
        });
    }

    private registerSearchField({ autoFocusSearchField = true } = {}): SearchFieldInstance {
        const searchFieldInstance = this.searchField.enable(
            {
                placeholder: 'SEARCH.SEARCH_STUFF',
                autoFocus: autoFocusSearchField,
            },
            this.destroyed,
        );
        this.showingAllRepositories.subscribe((showingAllRepositories) => {
            searchFieldInstance.patchConfig({
                showFiltersButton: !showingAllRepositories,
                enableFiltersAndSuggestions: !showingAllRepositories,
            });
        });
        searchFieldInstance
            .onFiltersButtonClicked()
            .subscribe(() =>
                this.filterBarIsVisible.setUserValue(!this.filterBarIsVisible.getValue()),
            );
        searchFieldInstance
            .onSearchTriggered()
            .subscribe(({ searchString }) => this.searchString.setUserValue(searchString || null));
        this.searchString
            .observeUserValue()
            .subscribe((searchString) => searchFieldInstance.setSearchString(searchString));
        rxjs.combineLatest([
            this.activeRepository.observeValue(),
            this.activeMetadataSet.observeValue(),
        ])
            .pipe(
                takeUntil(this.destroyed),
                filter(([repository, metadataSet]) => notNull(repository) && notNull(metadataSet)),
                map(([repository, metadataSet]) => ({ repository, metadataSet })),
            )
            .subscribe((mdsInfo) => searchFieldInstance.setMdsInfo(mdsInfo));
        searchFieldInstance.onFilterValuesChanged().subscribe((filterValues) => {
            // console.log('onFilterValuesChanged', filterValues);
            this.searchFilters.setUserValue(filterValues);
        });
        this.searchFilters.observeUserValue().subscribe((searchFilters) => {
            // console.log('searchFilters.userValue', searchFilters);
            searchFieldInstance.setFilterValues(searchFilters);
            // Reset the visible search string if the user made unapplied changes.
            searchFieldInstance.setSearchString(this.searchString.getValue());
        });
        return searchFieldInstance;
    }

    private registerLoadingProgress(): void {
        this._results
            .pipe(
                filter(notNull),
                switchMap((results) => results.loadingProgress),
            )
            .subscribe(this.loadingProgress);
    }

    private registerAddToCollection(): void {
        this.route.queryParamMap
            .pipe(
                map((queryParamMap) => queryParamMap.get('addToCollection')),
                distinctUntilChanged(),
                switchMap((collectionId) =>
                    collectionId ? this.collection.getCollection(collectionId) : rxjs.of(null),
                ),
            )
            .subscribe((collection) => {
                if (collection) {
                    this.addToCollectionMode.next({
                        collection,
                        customOptions: this.getAddToCollectionOptions(collection),
                    });
                } else {
                    this.addToCollectionMode.next(null);
                }
            });
    }

    private getAddToCollectionOptions(collection: Node): CustomOptions {
        const addTo = new OptionItem('SEARCH.ADD_INTO_COLLECTION_SHORT', 'layers', (_, nodes) => {
            this.mainNavService.getDialogs().addToCollectionList(collection, nodes, true, () =>
                this.router.navigate([UIConstants.ROUTER_PREFIX + 'collections'], {
                    queryParams: { id: collection.ref.id },
                }),
            );
        });
        addTo.elementType = OptionsHelperService.ElementTypesAddToCollection;
        addTo.permissions = [RestConstants.ACCESS_CC_PUBLISH];
        addTo.permissionsRightMode = NodesRightMode.Effective;
        addTo.permissionsMode = HideMode.Disable;
        addTo.group = DefaultGroups.Reuse;
        addTo.showAlways = true;
        const cancel = new OptionItem('CANCEL', 'close', () =>
            this.router.navigate([UIConstants.ROUTER_PREFIX, 'collections'], {
                queryParams: { id: collection.ref.id },
            }),
        );
        cancel.group = DefaultGroups.Delete;
        cancel.elementType = [ElementType.Unknown];
        return {
            useDefaultOptions: false,
            addOptions: [cancel, addTo],
        };
    }
}

function filterRepositories(repositories: Repository[], config: ClientConfig): Repository[] {
    const enabledRepositories = config.availableRepositories;
    if (enabledRepositories) {
        return repositories.filter(
            (repo) =>
                (repo.isHomeRepo && config.availableRepositories.includes(HOME_REPOSITORY)) ||
                config.availableRepositories.includes(repo.id),
        );
    } else {
        return repositories;
    }
}

function filterMetadataSets(
    metadataSets: MetadataSetInfo[],
    config: ClientConfig,
    repository: Repository,
): MetadataSetInfo[] {
    const enabledMetadataSets = config.availableMds?.find(
        (mdsConfig) =>
            mdsConfig.repository === repository.id ||
            (mdsConfig.repository === HOME_REPOSITORY && repository.isHomeRepo),
    )?.mds;
    if (enabledMetadataSets) {
        const mds = metadataSets.filter((mds) => enabledMetadataSets.includes(mds.id));
        if (mds.length === 0) {
            console.warn(
                `The filtered mds ${enabledMetadataSets} did not exists for the current app ${repository.id}. Check your Application XML config value "metadatasetsV2"`,
            );
        }
        return mds;
    } else {
        return metadataSets;
    }
}
