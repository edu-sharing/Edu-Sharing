import { Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { MatChip } from '@angular/material/chips';
import { FacetAggregation, FacetValue } from 'edu-sharing-api';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Widget } from '../../mds-editor-instance.service';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'app-mds-editor-widget-search-suggestions',
    templateUrl: './mds-editor-widget-search-suggestions.component.html',
    styleUrls: ['./mds-editor-widget-search-suggestions.component.scss'],
})
export class MdsEditorWidgetSearchSuggestionsComponent
    extends MdsEditorWidgetBase
    implements OnInit
{
    readonly valueType: ValueType = ValueType.MultiValue;

    @Input() widget: Widget;

    /** Suggestions for this widget, excluding current values. */
    filteredSuggestions$: Observable<FacetValue[]>;
    /** Suggestions for this widget. */
    private readonly widgetSuggestions$ = this.mdsEditorInstance.suggestionsSubject.pipe(
        map((suggestions) => suggestions?.[this.widget.definition.id]),
    );

    @ViewChild(MatChip, { read: ElementRef }) private firstSuggestionChip: ElementRef<HTMLElement>;

    ngOnInit(): void {
        this.registerFilteredSuggestions();
    }

    add(suggestion: FacetValue): void {
        const oldValue = this.widget._new_getValue() ?? [];
        if (!oldValue.includes(suggestion.value)) {
            const newValue = [...oldValue, suggestion.value];
            this.widget._new_setValue(newValue);
        }
    }

    focus(): void {
        this.firstSuggestionChip?.nativeElement.focus();
    }

    private registerFilteredSuggestions(): void {
        this.filteredSuggestions$ = rxjs
            .combineLatest([this.widget._new_observeValue(), this.widgetSuggestions$])
            .pipe(map(([values, suggestions]) => this.getFilteredSuggestions(suggestions, values)));
    }

    private getFilteredSuggestions(suggestions: FacetAggregation, values: string[]): FacetValue[] {
        const result = suggestions?.values.filter(
            (suggestion) => !values?.includes(suggestion.value),
        );
        if (result?.length > 0) {
            return result;
        } else {
            return null;
        }
    }
}
