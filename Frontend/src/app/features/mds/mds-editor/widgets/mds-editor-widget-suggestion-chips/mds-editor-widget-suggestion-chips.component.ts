import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FacetValue, SearchService } from 'ngx-edu-sharing-api';
import { combineLatest, Observable, Subject } from 'rxjs';
import { map, shareReplay, startWith, takeUntil } from 'rxjs/operators';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';

@Component({
    selector: 'es-mds-editor-widget-suggestion-chips',
    templateUrl: './mds-editor-widget-suggestion-chips.component.html',
    styleUrls: ['./mds-editor-widget-suggestion-chips.component.scss'],
})
export class MdsEditorWidgetSuggestionChipsComponent implements OnInit, OnDestroy {
    @Input() widget: Widget;

    filteredSuggestions$: Observable<FacetValue[]>;

    /** The widget controlling the property that this widget is displaying suggestions for. */
    private primaryWidget: Widget;
    private destroyed$ = new Subject<void>();

    constructor(
        private mdsEditorInstance: MdsEditorInstanceService,
        private search: SearchService,
    ) {}

    ngOnInit(): void {
        this.primaryWidget = this.mdsEditorInstance.getPrimaryWidget(this.widget.definition.id);
        if (this.primaryWidget) {
            this.filteredSuggestions$ = this.getFilteredSuggestions(this.primaryWidget);
        } else {
            console.error(
                'Could not find corresponding primary widget for suggestion widget.\n',
                `widget: ${this.widget.definition.id}\n`,
                `group: ${this.mdsEditorInstance.groupId}\n`,
                `mds: ${this.mdsEditorInstance.mdsId}`,
            );
        }
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    add(suggestion: FacetValue): void {
        this.primaryWidget.addValue.emit({ id: suggestion.value, caption: suggestion.label });
    }

    /**
     * Returns an observable of suggestions, excluding the ones already included in the primary
     * widget's value.
     */
    private getFilteredSuggestions(primaryWidget: Widget): Observable<FacetValue[]> {
        const widgetSuggestions$ = this.search.observeFacet(this.widget.definition.id).pipe(
            takeUntil(this.destroyed$),
            map((suggestions) =>
                suggestions?.values.filter((v) => v?.label?.trim() || v?.value?.trim()).slice(0, 5),
            ),
        );
        // Filter `widgetSuggestions$` by primary widget's value.
        return combineLatest([
            widgetSuggestions$,
            primaryWidget.observeValue().pipe(startWith(null as string[])),
        ]).pipe(
            map(([suggestions, values]) =>
                suggestions?.filter(
                    (suggestion) => !values?.some((value) => value === suggestion.value),
                ),
            ),
            shareReplay(1),
        );
    }
}
