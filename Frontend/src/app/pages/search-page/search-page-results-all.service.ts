import { Injectable, Injector, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
    MdsDefinition,
    MdsService,
    MetadataSetInfo,
    Node,
    SearchService,
    MdsSortDefault,
} from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { distinctUntilChanged, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { Repository, RestConstants } from '../../core-module/core.module';
import { MdsHelperService } from 'ngx-edu-sharing-ui';
import {
    fromSearchResults,
    NodeDataSourceRemote,
    NodeRemote,
    NodeRequestParams,
} from './node-data-source-remote';
import { SearchPageRestoreService } from './search-page-restore.service';
import { SearchPageResults } from './search-page-results.service';
import { SearchPageService } from './search-page.service';
import { ListItem } from 'ngx-edu-sharing-ui';

interface RepoData {
    title: string;
    id: string;
    isHome: boolean;
    dataSource: NodeDataSourceRemote;
    columns: Observable<ListItem[]>;
    loadingParams: Observable<boolean>;
    loadingContent: Observable<boolean>;
}

@Injectable()
export class SearchPageResultsAllService implements SearchPageResults, OnDestroy {
    readonly repoData = new BehaviorSubject<RepoData[]>(null);
    readonly loadingProgress = new BehaviorSubject<number>(0);

    private readonly _destroyed = new Subject<void>();

    constructor(
        private _injector: Injector,
        private _search: SearchService,
        private _searchPage: SearchPageService,
        private _searchPageRestore: SearchPageRestoreService,
        private _mds: MdsService,
        private _translate: TranslateService,
    ) {
        this._initRepoData();
        this._registerPageRestore();
        this._registerLoadingProgress();
    }

    ngOnDestroy(): void {
        this._destroyed.next();
        this._destroyed.complete();
    }

    addNodes(nodes: Node[]): void {
        const homeRepoData = this.repoData.value.find(({ isHome }) => isHome);
        homeRepoData?.dataSource.appendData(nodes, 'before');
    }

    private _initRepoData() {
        this._searchPage.availableRepositories
            .pipe(takeUntil(this._destroyed))
            .subscribe((repositories) => {
                this.repoData.next(repositories.map((repository) => this._getRepoData(repository)));
            });
    }

    private _registerPageRestore() {
        this.repoData.subscribe((repoData) => {
            for (const repo of repoData) {
                this._searchPageRestore.registerDataSource(repo.id, repo.dataSource);
            }
        });
    }

    private _getRepoData(repository: Repository): RepoData {
        const loadingParams = new BehaviorSubject(true);
        const loadingContent = new BehaviorSubject(true);
        const dataSource = new NodeDataSourceRemote(this._injector);
        const metadataSet: Observable<MetadataSetInfo> = this._getMetadataSet(repository);
        const mdsDefinition: Observable<MdsDefinition> = metadataSet.pipe(
            switchMap((metadataSet) => this._getMdsDefinition(repository, metadataSet)),
        );
        const columns: Observable<ListItem[]> = mdsDefinition.pipe(
            switchMap((mdsDefinition) => this._getColumns(mdsDefinition)),
        );
        const sort = mdsDefinition.pipe(
            map((mdsDefinition) => this._getDefaultSort(mdsDefinition)),
        );
        rxjs.combineLatest([metadataSet, sort, this._searchPage.searchString.observeValue()])
            .pipe(
                tap(() => loadingParams.next(false)),
                takeUntil(this._destroyed),
            )
            .subscribe(([metadataSet, sort, searchString]) => {
                dataSource.setRemote(
                    this._getSearchRemote(repository, metadataSet, sort, searchString),
                );
            });
        dataSource.isLoadingSubject.subscribe((isLoading) => loadingContent.next(!!isLoading));
        return {
            title: repository.title,
            id: repository.id,
            isHome: repository.isHomeRepo,
            dataSource,
            columns,
            loadingParams,
            loadingContent,
        };
    }

    private _getMetadataSet(repository: Repository): Observable<MetadataSetInfo> {
        return this._searchPage.getAvailableMetadataSets(repository).pipe(
            takeUntil(this._destroyed),
            map((availableMetadataSets) => availableMetadataSets[0]),
        );
    }

    private _getMdsDefinition(
        repository: Repository,
        metadataSet: MetadataSetInfo,
    ): Observable<MdsDefinition> {
        return this._mds.getMetadataSet({ repository: repository.id, metadataSet: metadataSet.id });
    }

    private _getColumns(mdsDefinition: MdsDefinition): Observable<ListItem[]> {
        return (
            this._translate
                // Make sure translations are initialized when MdsHelper calls `instant`.
                .get('dummy')
                .pipe(
                    map(() =>
                        MdsHelperService.getColumns(this._translate, mdsDefinition, 'search'),
                    ),
                )
        );
    }

    private _getDefaultSort(mdsDefinition: MdsDefinition): MdsSortDefault {
        return MdsHelperService.getSortInfo(mdsDefinition, 'search')?.default;
    }

    private _getSearchRemote(
        repository: Repository,
        metadataSet: MetadataSetInfo,
        sort: MdsSortDefault,
        searchString: string,
    ): NodeRemote<Node> {
        const criteria = searchString ? [{ property: 'ngsearchword', values: [searchString] }] : [];
        return (request: NodeRequestParams) => {
            return this._search
                .search({
                    body: {
                        criteria,
                        // permissions: this.reUrl.value ? [RestConstants.ACCESS_CC_PUBLISH] : [],
                    },
                    maxItems: request.range.endIndex - request.range.startIndex,
                    skipCount: request.range.startIndex,
                    sortAscending: sort ? [sort.sortAscending] : null,
                    sortProperties: sort ? [sort.sortBy] : null,
                    contentType: 'FILES',
                    repository: repository.id,
                    metadataset: metadataSet.id,
                    query: RestConstants.DEFAULT_QUERY_NAME,
                    propertyFilter: [RestConstants.ALL],
                })
                .pipe(map(fromSearchResults));
        };
    }

    private _registerLoadingProgress(): void {
        this.repoData
            .pipe(
                switchMap((repoData) =>
                    rxjs.combineLatest([
                        rxjs.combineLatest(repoData.map((r) => r.loadingParams)),
                        rxjs.combineLatest(repoData.map((r) => r.loadingContent)),
                    ]),
                ),
                map(([loadingParams, loadingContent]) => {
                    const loadingParamsFinished = loadingParams.filter(isFalse).length;
                    const loadingContentFinished = loadingContent.filter(isFalse).length;
                    if (loadingParamsFinished === 0) {
                        return 10;
                    } else if (loadingParamsFinished < loadingParams.length) {
                        return (loadingParamsFinished / loadingParams.length) * 30 + 10;
                    } else {
                        return (loadingContentFinished / loadingContent.length) * 60 + 40;
                    }
                }),
                distinctUntilChanged(),
                // tap((progress) => console.log('progress', progress)),
            )
            .subscribe(this.loadingProgress);
    }
}

function isFalse(value: boolean): boolean {
    return value === false;
}
