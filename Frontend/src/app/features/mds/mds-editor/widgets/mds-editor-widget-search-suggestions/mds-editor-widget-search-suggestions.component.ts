import { Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { MatChip } from '@angular/material/chips';
import { FacetAggregation, FacetValue } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Widget } from '../../mds-editor-instance.service';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { MdsWidgetType } from '../../../types/types';
import { Tree } from '../mds-editor-widget-tree/tree';

@Component({
    selector: 'es-mds-editor-widget-search-suggestions',
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
    private tree: Tree;

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

    getTooltip(suggestion: FacetValue) {
        if (
            [
                MdsWidgetType.MultiValueTree.toString(),
                MdsWidgetType.SingleValueTree.toString(),
            ].includes(this.widget.definition.type)
        ) {
            if (!this.tree) {
                // build up tree if not yet present
                this.tree = Tree.generateTree(this.widget.definition.values);
            }
            return this.tree.toDisplayValue(suggestion.value).hint;
        }
        return null;
    }
}
