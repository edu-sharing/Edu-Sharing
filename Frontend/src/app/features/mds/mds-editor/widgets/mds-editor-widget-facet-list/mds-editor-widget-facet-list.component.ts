import {
    ApplicationRef,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    ViewChild,
} from '@angular/core';
import { UntypedFormArray, UntypedFormControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { FacetAggregation, FacetValue, SearchService } from 'ngx-edu-sharing-api';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { debounceTime, filter, finalize, first, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';

@Component({
    selector: 'es-mds-editor-widget-facet-list',
    templateUrl: './mds-editor-widget-facet-list.component.html',
    styleUrls: ['./mds-editor-widget-facet-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MdsEditorWidgetFacetListComponent
    extends MdsEditorWidgetBase
    implements OnInit, OnDestroy
{
    @ViewChild(MdsEditorWidgetContainerComponent) containerRef: MdsEditorWidgetContainerComponent;
    readonly MAX_FACET_COUNT = 50;
    readonly valueType: ValueType = ValueType.MultiValue;
    /** Available facet values being updated from `mdsEditorInstance.suggestions$`. */
    readonly facetAggregationSubject = new BehaviorSubject<FacetAggregation>(null);
    /** Form array representing checkbox states. */
    formArray: UntypedFormArray;
    /** all available facet values. */
    facetValues: FacetValue[];
    facetValuesFiltered: FacetValue[];

    /** Whether we are currently loading more facets. */
    isLoading = false;
    /** IDs of selected values. Updated through user interaction. */
    private values: string[];
    private readonly destroyed$ = new Subject<void>();
    filter = new UntypedFormControl('');
    isInitState$ = new BehaviorSubject<boolean>(true);

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private search: SearchService,
        private ref: ChangeDetectorRef,
        private changeDetectorRef: ChangeDetectorRef,
    ) {
        super(mdsEditorInstance, translate);

        this.filter.valueChanges
            .pipe(debounceTime(200))
            .subscribe((filter) => this.filterControls(filter));
        this.filter.valueChanges
            .pipe(
                first(),
                switchMap(() =>
                    this.search.loadMoreFacets(
                        this.widget.definition.id,
                        RestConstants.COUNT_UNLIMITED,
                    ),
                ),
            )
            .subscribe(() => {});
    }

    ngOnInit(): void {
        this.values = this.widget.getInitialValues().jointValues;
        this.registerFacetValuesSubject();
        this.registerFormControls();
        this.widget.setValueExternal.subscribe((values) => {
            const valuesMapped = this.facetValuesFiltered.map(
                ({ value }) => !!values?.includes(value),
            );
            this.formArray.setValue(valuesMapped);
            this.widget.setValue(values, true);
        });
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    getFacet(index: number): FacetValue {
        return this.facetValuesFiltered[index];
    }

    onLoadMore(): void {
        this.isLoading = true;
        this.search
            .loadMoreFacets(this.widget.definition.id, 10)
            .pipe(finalize(() => (this.isLoading = false)))
            .subscribe();
    }

    private registerFacetValuesSubject(): void {
        this.isInitState$.next(true);
        this.search
            .observeFacet(this.widget.definition.id, {
                includeActiveFilters: true,
            })
            .pipe(
                takeUntil(this.destroyed$),
                tap((result) => this.isInitState$.next(result === null)),
                // load all facets if filter mode is active
                switchMap((facet) =>
                    this.filter.value && facet.hasMore
                        ? this.search.loadMoreFacets(
                              this.widget.definition.id,
                              RestConstants.COUNT_UNLIMITED,
                          )
                        : of(facet),
                ),
            )
            .subscribe((facetAggregation) =>
                facetAggregation ? this.facetAggregationSubject.next(facetAggregation) : null,
            );
    }

    private registerFormControls(): void {
        // (Re-)create `formArray` on changed facet values.
        this.facetAggregationSubject.subscribe((facetValues) => {
            // console.log(this.widget.definition.id, facetValues, this.filter.value);
            if (facetValues) {
                this.facetValues = facetValues.values;
                if (this.widget.definition.allowempty === false) {
                    this.facetValues = this.facetValues.filter((f) => !!f.value);
                }
                this.formArray = this.generateFormArray(facetValues.values);
                this.updateFilteredValues();
                // expand collapsed field if a value is active/selected
                if (
                    this.containerRef?.expandedState$.value === 'collapsed' &&
                    this.values?.length
                ) {
                    this.containerRef.expandedState$.next('expanded');
                }
            } else {
                this.formArray = null;
                this.facetValues = null;
                this.facetValuesFiltered = null;
            }
        });
    }

    private generateFormArray(facetValues: FacetValue[]): UntypedFormArray {
        const formArray = new UntypedFormArray(
            facetValues.map((value) => new UntypedFormControl(this.values.includes(value.value))),
        );
        // Propagate user interaction to instance service.
        formArray.valueChanges
            .pipe(filter((value) => value !== null))
            .subscribe((checkboxStates: boolean[]) => {
                this.values = this.facetValuesFiltered
                    .filter((_, index) => checkboxStates[index] === true)
                    .map(({ value }) => value);
                this.setValue(this.values);
            });
        return formArray;
    }

    hasFilter() {
        if (
            !this.widget.definition.filterMode ||
            this.widget.definition.filterMode === 'disabled'
        ) {
            return false;
        }
        if (this.widget.definition.filterMode === 'always') {
            return true;
        }
        return (
            this.facetAggregationSubject.value.hasMore ||
            this.facetAggregationSubject.value.values.length > 5
        );
    }

    private filterControls(filter: string) {
        this.updateFilteredValues();
        this.changeDetectorRef.detectChanges();
    }

    private updateFilteredValues() {
        this.facetValuesFiltered = this.facetValues.filter((v) =>
            v.label?.toLowerCase().includes(this.filter.value.toLowerCase()),
        );
        if (this.facetValuesFiltered.length > this.MAX_FACET_COUNT) {
            this.facetValuesFiltered = this.facetValuesFiltered.slice(0, this.MAX_FACET_COUNT);
        }
        this.formArray = this.generateFormArray(this.facetValuesFiltered);
        setTimeout(() => this.ref.detectChanges(), 10000);
    }
}
