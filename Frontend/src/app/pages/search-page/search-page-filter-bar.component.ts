import { Component, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { AuthenticationService, SavedSearch, SearchService } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';
import { debounceTime, delay, filter, first, map, skip, takeUntil, tap } from 'rxjs/operators';
import { RestConstants } from '../../core-module/core.module';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { MdsEditorWrapperComponent } from '../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { Values } from '../../features/mds/types/types';
import { notNull } from 'ngx-edu-sharing-ui';
import { GlobalSearchPageServiceInternal } from './global-search-page.service';
import { SearchPageService } from './search-page.service';

@Component({
    selector: 'es-search-page-filter-bar',
    templateUrl: './search-page-filter-bar.component.html',
    styleUrls: ['./search-page-filter-bar.component.scss'],
})
export class SearchPageFilterBarComponent implements OnInit, OnDestroy {
    @ViewChild(MdsEditorWrapperComponent) mdsEditor: MdsEditorWrapperComponent;

    readonly activeRepository = this.searchPage.activeRepository;
    readonly availableMetadataSets = this.searchPage.availableMetadataSets;
    readonly activeMetadataSet = this.searchPage.activeMetadataSet;
    readonly activeMdsForm = new UntypedFormControl(this.activeMetadataSet.getValue());
    readonly searchFilters = this.searchPage.searchFilters;
    readonly reUrl = this.searchPage.reUrl;
    readonly customTemplates = this.globalSearchPageInternal.customTemplates;
    /** Deep copy of `searchFilters.userValue` for immutability. */
    searchFilterValues: Values;
    mdsParams: { repository: string; setId: string } = null;
    savedSearchesButtonIsVisible = false;
    mdsExternalFilters: Values;

    private mdsInitialized = false;
    private defaultValues: Values;
    private causedValueChange = false;
    private destroyed = new Subject<void>();

    constructor(
        private authentication: AuthenticationService,
        private dialogs: DialogsService,
        private globalSearchPageInternal: GlobalSearchPageServiceInternal,
        private ngZone: NgZone,
        private searchPage: SearchPageService,
        private searchService: SearchService,
        private translate: TranslateService,
    ) {}

    ngOnInit(): void {
        this.registerSearchFilterOverride();
        this.activeMetadataSet.registerFormControl(this.activeMdsForm);
        this.registerMdsEditor();
        this.searchFilters
            .observeUserValue()
            .pipe(takeUntil(this.destroyed))
            .subscribe((values) => {
                this.searchFilterValues = JSON.parse(JSON.stringify(values ?? {}));
            });
        this.registerSavedSearches();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    async openSavedSearchesDialog(): Promise<void> {
        const dialogRef = await this.dialogs.openSavedSearchesDialog({
            saveSearchData: {
                name: await this.getSavedSearchInitialName(),
                searchString: this.searchPage.searchString.getValue(),
            },
            reUrl: this.reUrl.value || null,
        });
        dialogRef.afterClosed().subscribe((selectedSavedSearch) => {
            if (selectedSavedSearch) {
                this.applySavedSearch(selectedSavedSearch);
            }
        });
    }

    private applySavedSearch(savedSearch: SavedSearch): void {
        this.searchPage.activeRepository.setUserValue(savedSearch.repository);
        this.searchPage.activeMetadataSet.setUserValue(savedSearch.metadataSet);
        this.searchPage.searchString.setUserValue(savedSearch.searchString);
        this.searchPage.searchFilters.setUserValue(savedSearch.filters);
    }

    private async getSavedSearchInitialName(): Promise<string> {
        let components: string[] = [];
        if (this.activeRepository.getUserValue()) {
            const repo = this.searchPage.availableRepositories.value.find(
                ({ id }) => id === this.activeRepository.getUserValue(),
            );
            if (repo) {
                components.push(repo.title);
            }
        }
        if (this.searchPage.searchString.getValue()) {
            components.push(`"${this.searchPage.searchString.getValue()}"`);
        }
        const filters = await this.searchService.getFilters().toPromise();
        const filterLabels = Object.values(filters)
            .flat()
            .map(({ label }) => label);
        components = [...components, ...filterLabels];
        if (components.length > 0) {
            return components.join(' - ');
        } else {
            return this.translate.get('SEARCH.SAVE_SEARCH.ALL_ITEMS').toPromise();
        }
    }

    /**
     * Overrides the search-filter value with null when the active repository or metadata set
     * changes.
     *
     * We do this to inhibit search requests until the mds editor is ready. We unset the override
     * when it is. There are two reasons for this:
     * 1. The mds editor might have default values we need to include in the search request.
     * 2. The mds editor might request facets that will be fetched with the search request and need
     *    to be registered before the search request is fired to be considered.
     */
    private registerSearchFilterOverride(): void {
        rxjs.merge(this.activeRepository.observeValue(), this.activeMetadataSet.observeValue())
            .pipe(takeUntil(this.destroyed))
            .subscribe(() => {
                this.searchFilters.setOverrideValue(null);
            });
    }

    private registerMdsEditor(): void {
        rxjs.forkJoin([
            this.activeRepository.observeValue().pipe(first(notNull)),
            this.activeMetadataSet.observeValue().pipe(first(notNull)),
        ])
            .pipe(
                tap(([repository, setId]) => (this.mdsParams = { repository, setId })),
                delay(0),
                takeUntil(this.destroyed),
            )
            .subscribe(() => this.initMdsEditor());
        this.searchPage.searchString
            .observeValue()
            .pipe(takeUntil(this.destroyed))
            .subscribe((searchString) => {
                if (searchString) {
                    this.mdsExternalFilters = {
                        [RestConstants.PRIMARY_SEARCH_CRITERIA]: [searchString],
                    };
                } else {
                    this.mdsExternalFilters = null;
                }
            });
    }

    private registerSavedSearches(): void {
        this.authentication
            .observeLoginInfo()
            .pipe(
                takeUntil(this.destroyed),
                map((loginInfo) => loginInfo && !loginInfo.isGuest),
            )
            .subscribe((value) => (this.savedSearchesButtonIsVisible = value));
    }

    private initMdsEditor(): void {
        this.mdsEditor.mdsEditorInstance.values
            .pipe(takeUntil(this.destroyed))
            .subscribe((values) => this.onMdsValuesChange(values));
        this.mdsEditor
            .getInstanceService()
            .widgets.pipe(takeUntil(this.destroyed))
            .subscribe((mdsWidgets) => this.searchPage.filtersMdsWidgets.next(mdsWidgets));
        rxjs.merge(
            this.activeMetadataSet.observeValue().pipe(
                filter(notNull),
                skip(1),
                // tap((mds) => console.log('active mds changed', mds)),
            ),
            this.activeRepository.observeValue().pipe(
                filter(notNull),
                skip(1),
                // tap((repo) => console.log('active repo changed', repo)),
            ),
            this.searchFilters.observeValue().pipe(
                filter(notNull),
                skip(1),
                filter(() => !this.causedValueChange),
                // tap((filter) => console.log('search filters changed', filter)),
            ),
        )
            .pipe(
                takeUntil(this.destroyed),
                debounceTime(0),
                filter(() => notNull(this.activeMetadataSet.getValue())),
            )
            .subscribe(() => this.resetMds());
        this.mdsEditor.mdsEditorInstance
            .getNeededFacets()
            .pipe(takeUntil(this.destroyed))
            .subscribe((neededFacets) => this.searchPage.facetsToFetch.next(neededFacets));
    }

    // TODO: Provide this functionality in mds editor.
    private onMdsValuesChange(values: Values): void {
        values = JSON.parse(JSON.stringify(values));
        this.causedValueChange = true;
        if (this.mdsInitialized) {
            const userValues = getUserValues(values, this.defaultValues);
            // console.log('onMdsValuesChange', { values, userValues });
            this.searchFilters.setUserValue(userValues);
        } else {
            this.mdsInitialized = true;
            const userValues = stripValues(this.searchFilters.getUserValue() ?? {}, values);
            const defaultValues = this.getDefaultValues(values, userValues);
            // console.log('onMdsInitialValues', { values, defaultValues, userValues });
            this.defaultValues = defaultValues;
            this.searchFilters.setSystemValue(defaultValues);
            this.searchFilters.setUserValue(userValues);
            this.searchFilters.unsetOverrideValue();
        }
        this.ngZone.runOutsideAngular(() => setTimeout(() => (this.causedValueChange = false)));
    }

    private resetMds(): void {
        this.mdsParams = {
            repository: this.activeRepository.getValue(),
            setId: this.activeMetadataSet.getValue(),
        };
        // console.log('resetMds', this.mdsParams);
        this.mdsInitialized = false;
        // Wait for search-filter values to propagate via data binding to the mds editor.
        setTimeout(() => {
            // TODO: This should work automatically when updating the mds editor's setId.
            void this.mdsEditor.reInit();
        });
    }
    getDefaultValues(mergedValues: Values, userValues: Values): Values {
        const defaultValues = {} as Values;
        for (const [key, value] of Object.entries(mergedValues)) {
            if (value.length > 0 && !userValues?.[key]) {
                const definition =
                    this.mdsEditor.mdsEditorInstance.getPrimaryWidget(key)?.definition;
                if (!definition || definition.countDefaultvalueAsFilter !== true) {
                    defaultValues[key] = value;
                }
            }
        }
        return defaultValues;
    }
}

function getUserValues(mergedValues: Values, defaultValues: Values): Values {
    const userValues = {} as Values;
    for (const [key, value] of Object.entries(mergedValues)) {
        if (value.length > 0 && JSON.stringify(defaultValues[key]) !== JSON.stringify(value)) {
            userValues[key] = value;
        }
    }
    if (Object.keys(userValues).length > 0) {
        return userValues;
    } else {
        return null;
    }
}

function stripValues(values: Values, availableValues: Values): Values {
    const strippedValues = {} as Values;
    for (const [key, value] of Object.entries(values)) {
        if (key in availableValues) {
            strippedValues[key] = value;
        }
    }
    if (Object.keys(strippedValues).length > 0) {
        return strippedValues;
    } else {
        return null;
    }
}

function objectDifference(a: { [key: string]: any }, b: { [key: string]: any }) {
    a = { ...a };
    for (const key in Object.keys(b)) {
        delete a[key];
    }
    return a;
}
