import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import { LabeledValue, MdsIdentifier, NetworkService, SearchResults } from '../../public-api';
import * as apiModels from '../api/models';
import { SearchV1Service } from '../api/services';
import { onSubscription } from '../utils/rxjs-operators/on-subscription';
import { LabeledValuesDict, MdsLabelService, RawValuesDict } from './mds-label.service';

/** Configuration for `SearchService`. */
export class SearchConfig {
    /** Repository to provide to search API. */
    repository = '-home-';
    /** Metadata set to provide to search API. */
    metadataSet = '-default-';
    /** Query to provide to search API. */
    query = 'ngsearch';
    /** Number of results per page. */
    pageSize = 10;
    /** Properties for which to request facet values. */
    facets: string[] = [];
}

/** A singe entry of a given facet, including its localized display name. */
export interface FacetValue extends LabeledValue {
    count: number;
}

/** The aggregation of all facet values of a given facet. */
export interface FacetAggregation {
    hasMore: boolean;
    values: FacetValue[];
}

/** Values of all facets by property. */
export type FacetsDict = {
    [property: string]: FacetAggregation;
};

export type DidYouMeanSuggestion = Pick<apiModels.Suggest, 'highlighted' | 'text'>;

/** Parameters to be provided to `search`. */
export type SearchRequestParams = Parameters<SearchV1Service['search']>[0];

interface CompletedRequest {
    /** Parameters sent with the API request. */
    requestParams: SearchRequestParams;
    /** API response. */
    results: SearchResults;
    facetUpdates: FacetUpdates;
}

interface CompletedRequestRecord extends CompletedRequest {
    previousRequest: CompletedRequest | null;
}

interface FacetUpdates {
    /** Facets that have been subscribed to at the time of the request. */
    subscribedFacets: string[];
    /** Facets that have been subscribed to and are updated with this request. */
    facetsToUpdate: string[];
}

@Injectable({
    providedIn: 'root',
})
export class SearchService {
    private readonly completedRequestSubject = new BehaviorSubject<CompletedRequestRecord | null>(
        null,
    );
    private readonly facetsSubject = new BehaviorSubject<FacetsDict>({});
    private readonly didYouMeanSuggestionsSubject = new BehaviorSubject<apiModels.Suggest[] | null>(
        null,
    );
    private readonly searchParamsSubject = new BehaviorSubject<SearchRequestParams | null>(null);
    private readonly subscribedFacetsSubject = new BehaviorSubject<string[][]>([]);
    private didYouMeanSuggestionsSubscribers = 0;

    constructor(
        private mdsLabel: MdsLabelService,
        private network: NetworkService,
        private searchV1: SearchV1Service,
    ) {
        this.registerFacetsSubject();
        this.registerDidYouMeanSuggestionSubject();
    }

    /**
     * Sends a search request with new parameters.
     *
     * Facets will be updated and search parameters will be kept for future calls to `getPage` and
     * `getAsYouTypeFacetSuggestions`.
     */
    search(params: SearchRequestParams): Observable<SearchResults> {
        let previousRequest = omitProperty(this.completedRequestSubject.value, 'previousRequest');
        const facetUpdates = this.getFacetUpdates(previousRequest, params);
        this.searchParamsSubject.next(params);
        const requestParams = {
            ...params,
            body: {
                ...params.body,
                facets: this.getFacetsToFetch(params, facetUpdates),
                returnSuggestions:
                    params.body.returnSuggestions || this.didYouMeanSuggestionsSubscribers > 0,
            },
        };
        return this.requestSearch(requestParams).pipe(
            tap((results) =>
                this.completedRequestSubject.next({
                    results,
                    requestParams,
                    facetUpdates,
                    previousRequest,
                }),
            ),
        );
    }

    /**
     * Sends a plain search request without updating any global state.
     */
    requestSearch(params: SearchRequestParams): Observable<SearchResults> {
        return this.searchV1.search(params);
    }

    /**
     * Gets another page of search results with parameters last provided to `search`.
     */
    getPage(pageIndex: number): Observable<SearchResults> {
        const searchParams = this.getSearchParams();
        return this.requestSearch({
            ...searchParams,
            skipCount: (searchParams.maxItems ?? 10) * pageIndex,
            body: {
                ...searchParams.body,
                // Do not fetch facets again. These don't change for equal search criteria.
                facets: [],
            },
        });
    }

    /**
     * Gets facets from search requests.
     *
     * The given properties will be registered to be fetched with search requests while the
     * observable is being subscribed to. The observable is updated by new search requests.
     *
     * Subscribing to the observable does *not* trigger a new search request. Therefore, requested
     * properties might not be present in the provided facets until a search request has been
     * triggered.
     *
     * Facets are mapped to include translated labels taken from the respective MDS widget.
     *
     * @param includeActiveFilters - whether to always include active filters in the facet
     * list---even if the facet does not have any results (`count` is 0).
     */
    observeFacets(
        properties: string[],
        { includeActiveFilters = false } = {},
    ): Observable<FacetsDict> {
        return this.facetsSubject.pipe(
            onSubscription({
                onSubscribe: () => this.subscribeFacets(properties),
                onUnsubscribe: () => this.unsubscribeFacets(properties),
            }),
            includeActiveFilters
                ? switchMap((facets) => this.includeActiveFilters(facets))
                : rxjs.identity,
        );
    }

    /**
     * Like `observeFacets`, but gets facets for a single property.
     *
     * Might still return `null` when the requested facet was not part of the last search request
     * (see `observeFacets`).
     */
    observeFacet(
        property: string,
        options?: Parameters<SearchService['observeFacets']>[1],
    ): Observable<FacetAggregation | null> {
        return this.observeFacets([property], options).pipe(
            map((facets) => facets[property] ?? null),
        );
    }

    /**
     * Loads more facets of the given property.
     *
     * Updates the observable returned by `observeFacets`.
     *
     * @param size number of new items to load.
     */
    loadMoreFacets(property: string, size: number): Observable<void> {
        const searchParams = this.getSearchParams();
        const currentFacetSize = this.facetsSubject.value[property].values.length;
        return this.searchV1
            .search({
                ...searchParams,
                maxItems: 0,
                body: {
                    ...searchParams.body,
                    facets: [property],
                    facetLimit: currentFacetSize + size,
                },
            })
            .pipe(
                map((results) => results.facets.find((facet) => facet.property === property)),
                switchMap((facet) =>
                    facet
                        ? this.mapFacet(facet)
                        : rxjs.throwError(`Did not receive requested facet for "${property}"`),
                ),
                tap((facet) =>
                    this.facetsSubject.next({ ...this.facetsSubject.value, [property]: facet }),
                ),
                map(() => {}),
            );
    }

    /**
     * Returns the highest-scored did-you-mean suggestion for the last search request.
     *
     * The observable is updated as new search requests are resolved.
     *
     * Did-you-mean suggestions are only requested, when there is at least one active subscriber
     * when the search request is triggered.
     *
     * @param minimumScore The minimum score assigned my ElasticSearch for a suggestion to be
     * returned. If no suggestion meets the minimum score, `null` is returned.
     */
    observeDidYouMeanSuggestion(minimumScore = 0): Observable<DidYouMeanSuggestion | null> {
        return this.didYouMeanSuggestionsSubject.pipe(
            onSubscription({
                onSubscribe: () => this.didYouMeanSuggestionsSubscribers++,
                onUnsubscribe: () => this.didYouMeanSuggestionsSubscribers--,
            }),
            map((suggestions) => {
                const suggestion = suggestions?.[0];
                if (
                    suggestion &&
                    // If there are no "<em>" tags in `highlighted`, the suggestion equals the
                    // original string.
                    suggestion.text !== suggestion.highlighted &&
                    suggestion.score >= minimumScore
                ) {
                    return suggestion;
                } else {
                    return null;
                }
            }),
        );
    }

    /**
     * Fetches facet suggestions as the user is typing.
     *
     * @param inputString Incomplete search string while the user is typing
     * @param facets Properties for which to fetch suggestions
     * @param size Number of results for each facet to fetch
     * @returns Dictionary of labeled suggestions
     */
    getAsYouTypeFacetSuggestions(
        inputString: string,
        facets: string[],
        size: number,
    ): Observable<FacetsDict> {
        const searchParams = this.getSearchParams();
        const asYouTypeFacetSuggestions = this.searchV1
            .searchFacets({
                repository: searchParams.repository,
                metadataset: searchParams.metadataset,
                query: searchParams.query,
                body: {
                    criteria: this.getFilterCriteria(searchParams.body.criteria),
                    facets: facets,
                    facetLimit: size,
                    facetMinCount: 1,
                    facetSuggest: inputString,
                },
            })
            .pipe(switchMap((response) => this.mapFacets(response.facets)));
        // As-you-type suggestions are only supported on edu-sharing repositories.
        return this.network
            .getRepository(searchParams.repository)
            .pipe(
                switchMap((repository) =>
                    repository?.repositoryType === 'ALFRESCO'
                        ? asYouTypeFacetSuggestions
                        : rxjs.of({}),
                ),
            );
    }

    /**
     * Returns the filters provided as search params.
     *
     * Filters are mapped to include translated labels taken from the respective MDS widget.
     */
    getFilters(): Observable<LabeledValuesDict> {
        const filterCriteria = this.getFilterCriteria();
        if (filterCriteria.length === 0) {
            // If we don't have any filters yet, search params might not be available and
            // `getMdsIdentifier` would fail.
            return rxjs.of({});
        }
        const filterRawValues = this.criteriaToRawValues(filterCriteria);
        return this.mdsLabel.labelValuesDict(this.getMdsIdentifier(), filterRawValues);
    }

    /** Registers the internal `facetsSubject` to be updated with search responses. */
    private registerFacetsSubject(): void {
        this.completedRequestSubject
            .pipe(
                filter((request): request is CompletedRequestRecord => request !== null),
                switchMap((request) => this.updateFacetsDict(this.facetsSubject.value, request)),
            )
            .subscribe((facets) => this.facetsSubject.next(facets));
    }

    private registerDidYouMeanSuggestionSubject(): void {
        this.completedRequestSubject
            .pipe(map((request) => request?.results.suggests ?? null))
            .subscribe((didYouMeanSuggestion) =>
                this.didYouMeanSuggestionsSubject.next(didYouMeanSuggestion),
            );
    }

    /** Adds given properties to the list of facets to be fetched with search requests. */
    private subscribeFacets(properties: string[]) {
        this.subscribedFacetsSubject.next([...this.subscribedFacetsSubject.value, properties]);
    }

    /**
     * Removes the given array from the subscribed-facets list.
     *
     * The given array has to be the exact object that has been passed to `subscribeFacets`.
     */
    private unsubscribeFacets(properties: string[]) {
        const subscribedFacets = [...this.subscribedFacetsSubject.value];
        const index = subscribedFacets.indexOf(properties);
        if (index === -1) {
            throw new Error('Could not remove properties from subscribedFacets: ' + properties);
        }
        subscribedFacets.splice(index, 1);
        this.subscribedFacetsSubject.next(subscribedFacets);
    }

    /**
     * Gets a list of all facets that should be fetched with search request right now.
     *
     * @param additionalFacets - facets that should be fetched in addition to the ones on the
     * subscribed-facets list.
     */
    private getSubscribedFacets(additionalFacets: string[] = []): string[] {
        return additionalFacets
            .concat(...this.subscribedFacetsSubject.value) // Flatten array
            .filter((value, index, array) => array.indexOf(value) === index); // Remove duplicates
    }

    /** Gets parameters of the last call to `search`. */
    private getSearchParams(): SearchRequestParams {
        if (this.searchParamsSubject.value) {
            return this.searchParamsSubject.value;
        } else {
            throw new Error('Missing search parameters');
        }
    }

    /** Gets the mds identifier taken from the last call to `search`. */
    private getMdsIdentifier(): MdsIdentifier {
        const searchParams = this.getSearchParams();
        return {
            repository: searchParams.repository,
            metadataSet: searchParams.metadataset,
        };
    }

    /**
     * Extracts property filters from the search parameters "criterias" property.
     *
     * This includes everything but the free-text search string.
     */
    private getFilterCriteria(
        criteria: apiModels.SearchParameters['criteria'] = this.searchParamsSubject.value?.body
            .criteria ?? [],
    ): apiModels.SearchParameters['criteria'] {
        return criteria.filter((criterion) => criterion.property !== 'ngsearchword');
    }

    /**
     * Adds filters as facets to the given facets dictionary.
     *
     * Filters are taken from the last call to `search` and updated with new requests.
     *
     * If a filter is not already included as facet in the facets dictionary, it is appended with a
     * `count` of 0 to the respective list.
     */
    private includeActiveFilters(facets: FacetsDict): Observable<FacetsDict> {
        return this.getFilters().pipe(
            map((activeFilters) => {
                for (const [property, filters] of Object.entries(activeFilters)) {
                    for (const filter of filters) {
                        if (
                            facets[property]?.values.every((facet) => facet.value !== filter.value)
                        ) {
                            facets = {
                                ...facets,
                                [property]: {
                                    ...facets[property],
                                    values: [...facets[property].values, { ...filter, count: 0 }],
                                },
                            };
                        }
                    }
                }
                return facets;
            }),
        );
    }

    /**
     * Returns the properties which have to be fetched with the upcoming search request.
     */
    private getFacetsToFetch(
        searchParams: SearchRequestParams,
        facetUpdates: FacetUpdates,
    ): string[] {
        return (
            [...(searchParams.body.facets ?? []), ...facetUpdates.facetsToUpdate]
                // Remove duplicates
                .filter((value, index, array) => array.indexOf(value) === index)
        );
    }

    /**
     * Produces an object containing information on what facets are currently being subscribed to
     * and which of these need to be updated due to changed search parameters.
     *
     * We want to avoid updating existing facets when possible. This makes it easier for the UI
     * implementation because Angular won't recreate the facets table when not changed which would
     * mess up keyboard focus. Also, we want to keep longer facet lists, that have been loaded with
     * `loadMoreFacets`.
     */
    private getFacetUpdates(
        previousRequest: CompletedRequest | null,
        searchParams: SearchRequestParams,
    ): FacetUpdates {
        const subscribedFacets = this.getSubscribedFacets();
        if (previousRequest === null) {
            return { subscribedFacets, facetsToUpdate: subscribedFacets };
        } else {
            const facetsToUpdate = subscribedFacets.filter(
                (facet) => !this.canKeepFacet(previousRequest, searchParams, facet),
            );
            return { subscribedFacets, facetsToUpdate };
        }
    }

    /**
     * Generates an updated facets dictionary from a previous facets dictionary and a completed
     * search request.
     *
     * The completed search request contains information on what facets to update. The remaining
     * facets are reused from the previous facets dictionary.
     */
    private updateFacetsDict(
        facetsDict: FacetsDict | null,
        completedRequest: CompletedRequest,
    ): Observable<FacetsDict> {
        const facetsToUpdate = completedRequest.facetUpdates.facetsToUpdate;
        const facetsToKeep = completedRequest.facetUpdates.subscribedFacets.filter(
            (facet) => !facetsToUpdate.includes(facet),
        );
        const updatedFacets =
            completedRequest.results.facets?.filter((facet) =>
                facetsToUpdate.includes(facet.property),
            ) ?? [];
        return this.mapFacets(updatedFacets).pipe(
            map((mappedUpdatedFacets) => ({
                ...mappedUpdatedFacets,
                ...pickProperties(facetsDict ?? {}, facetsToKeep),
            })),
        );
    }

    /**
     * Returns `true` if the facet for the given `facetProperty` has been fetched with the last
     * search request and does not need to be updated.
     */
    private canKeepFacet(
        previousRequest: CompletedRequest,
        searchParams: SearchRequestParams,
        facetProperty: string,
    ): boolean {
        const previousSearchParams = previousRequest.requestParams;
        // We compare the previous request params that have actually been sent to the request
        // params, which will be modified for the next request, so some non-critical parameters are
        // going to be different even if they were provided equally to `search()`.
        const changedParams = this.getChangedProperties(previousSearchParams, searchParams);
        // Non-critical parameters. Any change of these doesn't affect facets.
        const nonCriticalParams = [
            'maxItems',
            'skipCount',
            'sortProperties',
            'sortAscending',
            'propertyFilter',
        ];
        if (!changedParams.every((param) => [...nonCriticalParams, 'body'].includes(param))) {
            // Some critical parameter was changed and results facets might be different.
            return false;
        } else if (!changedParams.includes('body')) {
            // Only non-critical have changed.
            return true;
        }
        // 'body' (and maybe non-critical parameters) have changed.
        const changedBodyProperties = this.getChangedProperties(
            previousSearchParams.body,
            searchParams.body,
        );
        const nonCriticalBodyParams = [
            'criteria',
            'facetSuggest',
            'facets',
            'resolveCollections',
            'resolveUsernames',
            'returnSuggestions',
        ];
        if (!changedBodyProperties.every((param) => [...nonCriticalBodyParams].includes(param))) {
            // Critical body params have changed.
            return false;
        }
        const changedCriteria = this.getChangedCriteria(
            previousSearchParams.body.criteria,
            searchParams.body.criteria,
        );
        // ~~If the only criterion that has changed is the facet in question, we can keep the facet.~~
        // return (
        //     changedCriteria.length === 0 ||
        //     (changedCriteria.length === 1 && changedCriteria[0] === facetProperty)
        // );

        // The above works only when multiple values of the criterion are OR-combined. Otherwise,
        // additional filter values for the criterion will reduce the available facets.
        //
        // TODO: Clarify if either OR or AND combination is preferred / always used.
        return changedCriteria.length === 0;
    }

    /** Compares two objects and return the keys of properties that are not deep-equal in a string
     * array. */
    private getChangedProperties<T extends {}>(lhs: T, rhs: T): (keyof T)[] {
        const keys = [...new Set([...Object.keys(lhs), ...Object.keys([rhs])])] as (keyof T)[];
        return keys.filter(
            (key) => JSON.stringify((lhs as any)[key]) !== JSON.stringify((rhs as any)[key]),
        );
    }

    /** Compares the criteria arrays of two search-request parameters and returns the properties for
     * which the entries differ as a string array. */
    private getChangedCriteria(
        lhs: apiModels.MdsQueryCriteria[],
        rhs: apiModels.MdsQueryCriteria[],
    ): string[] {
        const properties = [
            ...new Set([
                ...lhs.map((criterion) => criterion.property),
                ...rhs.map((criterion) => criterion.property),
            ]),
        ];
        return properties.filter(
            (property) =>
                JSON.stringify(lhs.find((c) => c.property === property)?.values) !==
                JSON.stringify(rhs.find((c) => c.property === property)?.values),
        );
    }

    /**
     * Maps the `facets` part of a search response to a facets dictionary with labeled facet values.
     */
    private mapFacets(results: SearchResults['facets']): Observable<FacetsDict> {
        if (results.length === 0) {
            return rxjs.of({});
        }
        return rxjs.forkJoin(
            results.reduce((acc, facet) => {
                acc[facet.property] = this.mapFacet(facet);
                return acc;
            }, {} as { [property: string]: Observable<FacetAggregation> }),
        );
    }

    /**
     * Maps a facets list of a single property from a search response to a facet aggregation with
     * labeled facet values.
     */
    private mapFacet(facet: apiModels.Facet): Observable<FacetAggregation> {
        if (facet.values.length === 0) {
            return rxjs.of({ values: [], hasMore: false });
        }
        return rxjs
            .forkJoin(facet.values.map((value) => this.mapFacetValue(facet.property, value)))
            .pipe(
                map((values) => ({
                    values,
                    hasMore: !!facet.sumOtherDocCount && facet.sumOtherDocCount > 0,
                })),
            );
    }

    /**
     * Maps a single facet value from a search response to a labeled facet value.
     */
    private mapFacetValue(
        property: string,
        { count, value }: apiModels.Value,
    ): Observable<FacetValue> {
        return this.mdsLabel
            .getLabel(this.getMdsIdentifier(), property, value)
            .pipe(map((label) => ({ count, value, label })));
    }

    /**
     * Maps the `criteria` array of a search request to a simple dictionary of values indexed by
     * properties.
     */
    private criteriaToRawValues(criteria: apiModels.SearchParameters['criteria']): RawValuesDict {
        return criteria.reduce((acc, criterion) => {
            acc[criterion.property] = criterion.values;
            return acc;
        }, {} as RawValuesDict);
    }
}

function omitProperty<T, K extends keyof T>(obj: T, property: K): Omit<T, K>;
function omitProperty<T, K extends keyof T>(obj: T | null, property: K): Omit<T, K> | null;
function omitProperty<T, K extends keyof T>(obj: T | null, property: K): Omit<T, K> | null {
    if (obj === null) {
        return null;
    } else {
        const { [property]: _, ...result } = obj;
        return result;
    }
}

function pickProperties<T extends {}, K extends keyof T>(obj: T, properties: K[]): Pick<T, K>;
function pickProperties<T extends {}, K extends keyof T>(
    obj: T | null,
    properties: K[],
): Pick<T, K> | null;
function pickProperties<T extends {}, K extends keyof T>(
    obj: T | null,
    properties: K[],
): Pick<T, K> | null {
    if (obj === null) {
        return null;
    } else {
        return properties.reduce((acc, prop) => {
            acc[prop] = obj[prop];
            return acc;
        }, {} as Pick<T, K>);
    }
}
