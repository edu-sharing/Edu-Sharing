import { CdkConnectedOverlay, CdkOverlayOrigin, ConnectedPosition } from '@angular/cdk/overlay';
import { Component, ElementRef, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatChip } from '@angular/material/chips';
import { FacetsDict, LabeledValue, LabeledValuesDict } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';
import { filter, map, takeUntil } from 'rxjs/operators';
import { SearchFieldFacetsComponent } from '../../../features/mds/mds-editor/search-field-facets/search-field-facets.component';
import { Values } from '../../../features/mds/types/types';
import { SearchFieldInternalService } from './search-field-internal.service';
import { SearchFieldConfig } from './search-field.service';

@Component({
    selector: 'es-search-field',
    templateUrl: './search-field.component.html',
    styleUrls: ['./search-field.component.scss'],
})
export class SearchFieldComponent implements OnInit, OnDestroy {
    /** The number of filters visible on the facets overlay. */
    filtersCount: number;
    /** The total number of filters independently of categories of the facets overlay. */
    totalFiltersCount: number;
    config: SearchFieldConfig;

    @ViewChild('input') input: ElementRef;
    @ViewChild(CdkConnectedOverlay) private overlay: CdkConnectedOverlay;
    @ViewChild(SearchFieldFacetsComponent) private searchFieldFacets: SearchFieldFacetsComponent;
    @ViewChild(MatChip) private firstActiveChip: MatChip;

    readonly inputControl = new FormControl('');
    /** The user clicked the filters button inside the search field. */
    readonly filtersButtonClicked = this.internal.filtersButtonClicked;
    readonly filters$ = this.internal.filters$;
    readonly rawFilters$ = this.internal.rawFilters$;
    readonly categories$ = this.internal.categoriesSubject;
    readonly suggestions$ = this.internal.suggestions$;
    showOverlay = false;
    inhibitOverlay = false;
    hasSuggestions = true;
    readonly overlayPositions: ConnectedPosition[] = [
        {
            originX: 'center',
            originY: 'bottom',
            offsetX: 0,
            offsetY: 4,
            overlayX: 'center',
            overlayY: 'top',
        },
    ];
    readonly mdsInfo$ = this.internal.mdsInfo$;
    inputHasFocus = false;

    private readonly destroyed$ = new Subject<void>();

    constructor(private internal: SearchFieldInternalService, private ngZone: NgZone) {}

    ngOnInit(): void {
        this.internal.searchFieldComponent = this;
        this.internal.config
            .pipe(takeUntil(this.destroyed$))
            .subscribe((config) => (this.config = config));
        this.internal.searchString
            .pipe(
                takeUntil(this.destroyed$),
                filter((searchString) => this.inputControl.value !== searchString),
            )
            .subscribe((searchString) => this.inputControl.setValue(searchString));
        this.inputControl.valueChanges.subscribe((inputString) => {
            if (inputString !== this.internal.searchString.value) {
                // The value was updated through user interaction and not by the component input
                // `searchString`.
                this.internal.searchString.next(inputString);
                this.internal.updateSuggestions(inputString);
                this.internal.searchStringChanged.next(inputString);
            }
        });
        rxjs.combineLatest([this.filters$, this.categories$])
            .pipe(
                takeUntil(this.destroyed$),
                map(([filters, categories]) => this.getFiltersCount(filters, categories)),
            )
            .subscribe((filtersCount) => (this.filtersCount = filtersCount));
        this.filters$
            .pipe(
                takeUntil(this.destroyed$),
                map((filters) => this.getTotalFiltersCount(filters)),
            )
            .subscribe((totalFiltersCount) => (this.totalFiltersCount = totalFiltersCount));
        this.suggestions$
            .pipe(
                takeUntil(this.destroyed$),
                map((suggestions) => this.getHasSuggestions(suggestions)),
            )
            .subscribe((hasSuggestions) => {
                this.hasSuggestions = hasSuggestions;
                // We only fetch new suggestions when the user types into the search field. In case
                // the user dismissed the suggestions overlay earlier (`showOverlay = false`), this
                // is the time to show it again.
                if (this.hasSuggestions) {
                    this.showOverlay = true;
                }
            });
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
        if (this.internal.searchFieldComponent === this) {
            this.internal.searchFieldComponent = null;
        }
    }

    onSubmit(): void {
        this.showOverlay = false;
        this.internal.triggerSearch({ searchString: this.inputControl.value, cleared: false });
    }

    onClear(): void {
        this.inputControl.setValue('');
        this.internal.triggerSearch({ searchString: null, cleared: true });
    }

    onValuesChange(values: Values): void {
        // A `valuesChange` event from the mds editor means, a suggestion card has been added as
        // filter.
        this.inputControl.setValue('');
        this.internal.setFilterValues(values, { emitValuesChange: true });
    }

    onRemoveFilter(property: string, filter: LabeledValue): void {
        this.internal.removeFilter(property, filter);
    }

    onOutsideClick(event: MouseEvent): void {
        const clickTarget = event.target as HTMLElement;
        const origin = this.overlay.origin as CdkOverlayOrigin;
        const element = origin.elementRef.nativeElement as HTMLElement;
        if (!element.contains(clickTarget)) {
            this.showOverlay = false;
        }
    }

    focusOverlayIfOpen(event: Event): void {
        if (this.firstActiveChip) {
            this.firstActiveChip._elementRef.nativeElement.focus();
            event.stopPropagation();
            event.preventDefault();
        } else if (this.showOverlay && this.hasSuggestions) {
            this.searchFieldFacets.focus();
            event.stopPropagation();
            event.preventDefault();
        }
    }

    onDetach(): void {
        const focusWasOnOverlay = this.overlay.overlayRef.overlayElement.contains(
            document.activeElement,
        );
        if (focusWasOnOverlay) {
            this.input.nativeElement.focus();
        }
        // Update `showOverlay` if the user closed the overlay by hitting Esc, but leave it if it
        // was detached because we have no suggestions right now. In the latter case, we want to
        // show the overlay again as soon as suggestions become available.
        if (this.hasSuggestions) {
            this.showOverlay = false;
        }
    }

    onInputFocus(): void {
        Promise.resolve().then(() => (this.inputHasFocus = true));
        if (!this.inhibitOverlay) {
            this.showOverlay = true;
        }
    }

    onInputBlur(event: FocusEvent): void {
        this.inputHasFocus = false;
        if (
            !this.overlay?.overlayRef?.overlayElement.contains(event.relatedTarget as HTMLElement)
        ) {
            this.showOverlay = false;
        }
    }

    onFiltersButtonClicked(): void {
        this.inhibitOverlay = true;
        this.filtersButtonClicked.next();
        this.ngZone.runOutsideAngular(() => setTimeout(() => (this.inhibitOverlay = false)));
    }

    onCategories(properties: string[]): void {
        this.internal.categoriesSubject.next(properties);
    }

    private getHasSuggestions(suggestions: FacetsDict): boolean {
        return (
            suggestions &&
            Object.values(suggestions).some((suggestion) => suggestion.values.length > 0)
        );
    }

    getFiltersCount(filters: LabeledValuesDict | null, categories: string[]): number {
        if (!filters) {
            return 0;
        }
        const mapped = Object.keys(filters)
            .filter((f) => categories?.includes(f))
            .map((k) => filters[k].length);
        if (!mapped.length) {
            return 0;
        } else {
            return mapped.reduce((a, b) => a + b);
        }
    }

    getTotalFiltersCount(filters: LabeledValuesDict | null): number {
        if (!filters) {
            return 0;
        }
        const mapped = Object.keys(filters).map((k) => filters[k].length);
        if (!mapped.length) {
            return 0;
        } else {
            return mapped.reduce((a, b) => a + b);
        }
    }
}
