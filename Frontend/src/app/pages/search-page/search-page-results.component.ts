import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {
    ActionbarComponent,
    NodeEntriesDisplayType,
    Scope,
    TemporaryStorageService,
} from 'ngx-edu-sharing-ui';
import { SearchPageResultsService } from './search-page-results.service';
import { SearchPageService } from './search-page.service';
import { GlobalSearchPageServiceInternal } from './global-search-page.service';
import { Subject } from 'rxjs';
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { TranslateService } from '@ngx-translate/core';
import { switchMap, takeUntil } from 'rxjs/operators';

@Component({
    selector: 'es-search-page-results',
    templateUrl: './search-page-results.component.html',
    styleUrls: ['./search-page-results.component.scss'],
    providers: [SearchPageResultsService],
})
export class SearchPageResultsComponent implements OnInit, OnDestroy {
    readonly Scope = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    private destroyed = new Subject<void>();

    @ViewChild(ActionbarComponent)
    set _actionbar(value: ActionbarComponent) {
        // Avoid changed-after-checked error.
        setTimeout(() => (this.actionbar = value));
    }
    actionbar: ActionbarComponent;

    readonly resultsDataSource = this.results.resultsDataSource;
    readonly collectionsDataSource = this.results.collectionsDataSource;
    readonly resultColumns = this.results.resultColumns;
    readonly collectionColumns = this.results.collectionColumns;
    readonly state = this.results.state;
    readonly addToCollectionMode = this.searchPage.addToCollectionMode;
    readonly customTemplates = this.globalSearchPageInternal.customTemplates;

    constructor(
        private globalSearchPageInternal: GlobalSearchPageServiceInternal,
        private results: SearchPageResultsService,
        private searchPage: SearchPageService,
        private temporaryStorageService: TemporaryStorageService,
        private announcer: LiveAnnouncer,
        private translate: TranslateService,
    ) {
        // announce newly loaded elements to users using screen readers
        results.diffCount
            .pipe(
                takeUntil(this.destroyed), // FIXME: replace with takeUntilDestroyed in Angular 16+
                switchMap((newlyLoadedElements) =>
                    this.translate.get('SEARCH.LOADED_RESULTS', { count: newlyLoadedElements }),
                ),
            )
            .subscribe((elementsLoadedTranslation) => {
                this.announcer.announce(elementsLoadedTranslation);
            });
    }

    ngOnInit(): void {
        setTimeout(() => {
            this.searchPage.results = this.results;
            this.searchPage.showingAllRepositories.next(false);
        });
    }

    toggleFilters(): void {
        const filterBarIsVisible = this.searchPage.filterBarIsVisible;
        filterBarIsVisible.setUserValue(!filterBarIsVisible.getValue());
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
        this.temporaryStorageService.set(
            TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE,
            this.resultsDataSource,
        );
    }

    getCountClass() {
        if (this.searchPage.searchString.getValue()) {
            return 'count-ngsearchword';
        }
        return '';
    }

    setDisplayType(displayType: NodeEntriesDisplayType) {
        this.results.patchState({ displayType });
    }
}
