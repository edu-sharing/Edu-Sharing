import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { FacetsDict } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import { MdsView, Values } from '../../types/types';
import { MdsEditorWidgetSearchSuggestionsComponent } from '../widgets/mds-editor-widget-search-suggestions/mds-editor-widget-search-suggestions.component';

/** Information that require a re-initialization once changed. */
interface InitInfo {
    repository: string;
    metadataSet: string;
    group: string;
}

@Component({
    selector: 'es-search-field-facets',
    templateUrl: './search-field-facets.component.html',
    styleUrls: ['./search-field-facets.component.scss'],
    providers: [MdsEditorInstanceService],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchFieldFacetsComponent implements OnInit, OnDestroy {
    @ViewChild('suggestionsPanel', { static: true }) suggestionsPanel: TemplateRef<any>;

    /** The repository to which the metadata set to be used belongs. */
    @Input() set repository(repository: string) {
        this.updateInitInfo({ repository });
    }
    /** The metadata set to be used. */
    @Input() set metadataSet(metadataSet: string) {
        this.updateInitInfo({ metadataSet });
    }
    /** The metadata set's group to be used. */
    @Input() set group(group: string) {
        this.updateInitInfo({ group });
    }
    /** Values to populate or update the editor with. */
    @Input() set values(values: Values) {
        this._values = values;
        this.mdsEditorInstance._new_setValues(values);
    }
    get values() {
        return this._values;
    }
    private _values: Values;
    /** Value changes as defaults are applied or the user uses the editor. */
    @Output() valuesChange = new EventEmitter<Values>();

    @Input() set suggestions(suggestions: FacetsDict) {
        this.mdsEditorInstance.suggestionsSubject.next(suggestions);
    }

    /** Properties for which facet suggestions should be fetched.  */
    @Output() categories = new EventEmitter<string[]>();

    readonly overrideWidget = MdsEditorWidgetSearchSuggestionsComponent;
    views: MdsView[];
    private readonly initInfoSubject = new rxjs.BehaviorSubject<Partial<InitInfo>>({});
    private readonly destroyed$ = new Subject<void>();

    constructor(private mdsEditorInstance: MdsEditorInstanceService) {}

    ngOnInit(): void {
        this.mdsEditorInstance._new_valuesChange.subscribe((values) => {
            this._values = values;
            this.valuesChange.emit(values);
        });
        // Debounce initialization so we don't use partially updated parameters.
        this.initInfoSubject.pipe(debounceTime(0)).subscribe((info) => this.init(info));
        this.mdsEditorInstance
            .getNeededFacets()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((properties) => this.categories.emit(properties));
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    focus(): void {
        this.mdsEditorInstance.focusFirstWidget();
    }

    onRemoveFilter(property: string, value: string): void {
        const index = this.values[property].indexOf(value);
        if (index >= 0) {
            const valueListCopy = [...this.values[property]];
            valueListCopy.splice(index, 1);
            const newValues = { ...this.values, [property]: valueListCopy };
            this.values = newValues;
            this.valuesChange.emit(newValues);
        }
    }

    private updateInitInfo(newInfo: Partial<InitInfo>): void {
        const oldInfo = this.initInfoSubject.value;
        // Only trigger an update if some value actually changed.
        if (
            Object.entries(newInfo).some(([key, value]) => oldInfo[key as keyof InitInfo] !== value)
        ) {
            this.initInfoSubject.next({ ...oldInfo, ...newInfo });
        }
    }

    private init(info: Partial<InitInfo>): void {
        if (info.repository && info.metadataSet && info.group) {
            void this.mdsEditorInstance
                .initWithoutNodes(info.group, info.metadataSet, info.repository)
                .then(() => (this.views = this.mdsEditorInstance.views));
        } else {
            // Consider resetting the mds instance service here.
            return;
        }
    }

    isEmpty() {
        this.mdsEditorInstance.suggestionsSubject.value;
        if (!this.mdsEditorInstance.suggestionsSubject.value) {
            return true;
        }
        return (
            Object.keys(this.mdsEditorInstance.suggestionsSubject.value).filter(
                (key) => this.mdsEditorInstance.suggestionsSubject.value[key].values.length,
            ).length === 0
        );
    }
}
