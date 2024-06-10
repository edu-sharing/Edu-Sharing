import { COMMA, ENTER } from '@angular/cdk/keycodes';
import {
    AfterViewInit,
    Component,
    ElementRef,
    OnInit,
    QueryList,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import {
    MatAutocomplete,
    MatAutocompleteSelectedEvent,
    MatAutocompleteTrigger,
} from '@angular/material/autocomplete';
import { MatChipInputEvent, MatChipOption, MatChipRow } from '@angular/material/chips';
import { MatTooltip } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, combineLatest, EMPTY, from, Observable, Subject, timer } from 'rxjs';
import {
    debounce,
    debounceTime,
    delay,
    distinctUntilChanged,
    filter,
    map,
    shareReplay,
    startWith,
    switchMap,
    throttleTime,
} from 'rxjs/operators';
import { Toast, ToastType } from '../../../../../services/toast';
import { UIHelper } from '../../../../../core-ui-module/ui-helper';
import { MdsWidget, MdsWidgetType, MdsWidgetValue } from '../../../types/types';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { DisplayValue } from '../DisplayValues';
import {
    MdsEditorWidgetBase,
    MdsEditorWidgetChipsSuggestionBase,
    ValueType,
} from '../mds-editor-widget-base';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { Helper } from '../../../../../core-module/rest/helper';
import { SuggestionResponseDto, SuggestionStatus } from 'ngx-edu-sharing-api';
import { AuthorityNamePipe } from '../../../../../shared/pipes/authority-name.pipe';

@Component({
    templateUrl: './mds-editor-widget-chips.component.html',
    styleUrls: ['./mds-editor-widget-chips.component.scss'],
})
export class MdsEditorWidgetChipsComponent
    extends MdsEditorWidgetChipsSuggestionBase
    implements OnInit, AfterViewInit
{
    @ViewChild('input') input: ElementRef<HTMLInputElement>;
    @ViewChild('container') container: MdsEditorWidgetContainerComponent;
    @ViewChild(MatAutocompleteTrigger, { read: MatAutocompleteTrigger })
    trigger: MatAutocompleteTrigger;
    @ViewChild('auto') matAutocomplete: MatAutocomplete;
    @ViewChildren('chip') chips: QueryList<MatChipRow>;

    readonly valueType: ValueType = ValueType.MultiValue;
    readonly separatorKeysCodes: number[] = [ENTER, COMMA];
    inputControl = new UntypedFormControl();
    autocompleteValues: Observable<DisplayValue[]>;
    shouldShowNoMatchingValuesNotice: Observable<boolean>;
    indeterminateValues$: BehaviorSubject<string[]>;
    showDropdownArrow: boolean;

    private autocompleteIsInhibited = new BehaviorSubject(false);
    private autoCompleteToggleTrigger = new Subject<'open' | 'close' | 'opened' | 'closed'>();

    readonly showTooltip = (() => {
        let previousTooltip: MatTooltip;
        return (tooltip?: MatTooltip) => {
            previousTooltip?.hide();
            tooltip?.show();
            previousTooltip = tooltip;
        };
    })();

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private toast: Toast,
    ) {
        super(mdsEditorInstance, translate);
    }

    async ngOnInit() {
        this.chipsControl = new UntypedFormControl(null, this.getStandardValidators());
        this.chipsControl = new UntypedFormControl(
            [
                ...((await this.widget.getInitalValuesAsync()).jointValues ?? []),
                ...((await this.widget.getInitalValuesAsync()).individualValues ?? []),
            ].map((value) => this.toDisplayValue(value)),
            this.getStandardValidators(),
        );
        this.widget
            .getInitialDisplayValues()
            .pipe(filter((v) => !!v))
            .subscribe(async (value) => {
                this.chipsControl.setValue(await this.getInitialValues());
            });

        this.indeterminateValues$ = new BehaviorSubject(
            (await this.widget.getInitalValuesAsync()).individualValues,
        );
        super.initSuggestions();
        if (
            this.widget.definition.type === MdsWidgetType.MultiValueBadges ||
            this.widget.definition.type === MdsWidgetType.MultiValueSuggestBadges
        ) {
            if (!this.widget.definition.bottomCaption) {
                this.translate.get('WORKSPACE.EDITOR.HINT_ENTER').subscribe((bottomCaption) => {
                    this.widget.definition.bottomCaption = bottomCaption;
                });
            }
        }
        this.chipsControl.valueChanges
            .pipe(distinctUntilChanged())
            .subscribe((values: DisplayValue[]) => this.setValue(values.map((value) => value.key)));
        if (
            this.widget.definition.type === MdsWidgetType.MultiValueSuggestBadges ||
            this.widget.definition.type === MdsWidgetType.SingleValueSuggestBadges ||
            this.widget.definition.type === MdsWidgetType.MultiValueFixedBadges
        ) {
            const filteredValues = this.subscribeForSuggestionUpdates();
            this.autocompleteValues = combineLatest([
                filteredValues,
                this.autocompleteIsInhibited,
            ]).pipe(
                distinctUntilChanged((a, b) => Helper.objectEquals(a, b)),
                map(([values, inhibit]) => (inhibit ? null : values)),
            );
            this.shouldShowNoMatchingValuesNotice = combineLatest([
                this.autocompleteValues,
                this.inputControl.valueChanges,
            ]).pipe(
                map(
                    ([autocompleteValues, inputValue]) =>
                        this.widget.definition.type === MdsWidgetType.MultiValueFixedBadges &&
                        autocompleteValues?.length === 0 &&
                        inputValue,
                ),
            );
        }
        this.showDropdownArrow =
            this.widget.definition.type === MdsWidgetType.MultiValueFixedBadges &&
            !!this.widget.definition.values;

        this.indeterminateValues$.subscribe((indeterminateValues) =>
            this.widget.setIndeterminateValues(indeterminateValues),
        );
        this.widget.addValue.subscribe((value: MdsWidgetValue) => {
            this.add(this.toDisplayValue(value));
        });
        this.registerValueChanges(this.chipsControl);
    }

    private async getInitialValues() {
        return [
            ...((await this.widget.getInitalValuesAsync()).jointValues ?? []),
            ...((await this.widget.getInitalValuesAsync()).individualValues ?? []),
        ].map((value) => this.toDisplayValue(value));
    }

    ngAfterViewInit(): void {
        this.registerAutoCompleteToggleTrigger();
        // We mark all chips as selected for better screen-reader output. However, since selection
        // doesn't do anything, we disable toggling the selection.
        this.chips.changes
            .pipe(startWith(this.chips))
            .subscribe((chips: QueryList<MatChipOption>) =>
                chips.forEach((chip) => (chip.toggleSelected = () => true)),
            );
    }

    onInputTokenEnd(event: MatChipInputEvent): void {
        if (this.widget.definition.type === MdsWidgetType.MultiValueFixedBadges) {
            if (event.value) {
                // If the input field still has a value, the use has not selected on option from the
                // autocomplete list. In this case, we notify them, that they cannot add arbitrary
                // values.
                this.toast.show({
                    message: 'MDS.NO_ARBITRARY_VALUES_NOTICE',
                    type: 'info',
                    subtype: ToastType.InfoSimple,
                });
            }
            return;
        }
        const value = (event.value || '').trim();
        if (value) {
            this.add(this.toDisplayValue(value));
        }
        this.inputControl.setValue(null);
    }

    onBlurInput(event: FocusEvent): void {
        const target = event.relatedTarget as HTMLElement;
        // ignore mat option focus to prevent resetting before selection is done
        if (
            target == null ||
            target?.tagName === 'MAT-OPTION' ||
            target === this.input.nativeElement
        ) {
            return;
        }
        this.inputControl.setValue(null);
        // `matAutocomplete` doesn't seem to close the autocomplete panel in some situations,
        // including when focus goes to the component's own chips (breaks keyboard navigation) and
        // some external elements (produces multiple open overlays).
        //
        // We would also get unintended behavior when the input is blurred because the auto-complete
        // toggle button was pressed, but the `autoCompleteToggleTrigger` mechanism takes care of
        // that.
        if (this.trigger.panelOpen) {
            this.autoCompleteToggleTrigger.next('close');
        }
        if (
            !UIHelper.isParentElementOfElement(
                event.relatedTarget as HTMLElement,
                this.container.nativeElement.nativeElement,
            )
        ) {
            this.onBlur.emit();
        }
    }

    toggleAutoCompletePanel(): void {
        // There are different strategies for doing this (see
        // https://stackoverflow.com/questions/50491195/open-matautocomplete-with-open-openpanel-method).
        // We rely on the `autoCompleteToggleTrigger` mechanism.
        if (this.chipsControl.disabled) {
            return;
        }
        if (this.trigger.panelOpen) {
            this.autoCompleteToggleTrigger.next('close');
        } else {
            this.autoCompleteToggleTrigger.next('open');
        }
    }

    remove(toBeRemoved: DisplayValue): void {
        const values: DisplayValue[] = this.chipsControl.value;
        if (values.includes(toBeRemoved)) {
            this.chipsControl.setValue(values.filter((value) => value !== toBeRemoved));
        }
        this.removeFromIndeterminateValues(toBeRemoved.key);
        this.inhibitAutocomplete();
    }

    selected(event: MatAutocompleteSelectedEvent) {
        this.add(event.option.value);
        this.input.nativeElement.value = '';
        this.inputControl.setValue(null);
    }

    focus() {
        this.input?.nativeElement?.focus();
    }
    add(value: DisplayValue): void {
        if (this.widget.definition.type === MdsWidgetType.SingleValueSuggestBadges) {
            this.chipsControl.setValue([]);
        }
        if (!this.chipsControl.value.some((v: DisplayValue) => v.key === value.key)) {
            this.chipsControl.setValue([...this.chipsControl.value, value]);
        }
        this.removeFromIndeterminateValues(value.key);
    }

    getTooltip(value: DisplayValue): string | null {
        const shouldShowIndeterminateNotice =
            this.widget.getStatus() !== 'DISABLED' &&
            this.widget.getIndeterminateValues()?.includes(value.key);
        if (shouldShowIndeterminateNotice) {
            return (
                this.translate.instant('MDS.INDETERMINATE_NOTICE', { value: value.label }) +
                ` (${this.translate.instant('MDS.DELETE_KEY_NOTICE')})`
            );
        } else {
            return value.label + ` (${this.translate.instant('MDS.DELETE_KEY_NOTICE')})`;
        }
    }
    /**
     * Prevent the auto-complete panel from quickly opening and closing.
     *
     * Use `this.autoCompleteToggleTrigger.next('open' | 'close')` to trigger.
     *
     * Not using a mechanism like this results in all kinds of flaky behavior when focus moves
     * around the input. This happens for example when the user clicks on the form field but not on
     * the <input> element, the mouse button is released above an auto-complete option, or even when
     * just clicking the toggle button. The behavior differs on browsers, especially Safari.
     */
    private registerAutoCompleteToggleTrigger(): void {
        this.matAutocomplete.opened.subscribe(() => this.autoCompleteToggleTrigger.next('opened'));
        this.matAutocomplete.closed.subscribe(() => this.autoCompleteToggleTrigger.next('closed'));
        rxjs.combineLatest([
            // The panel might close on the `mousedown` event on the button and then open again on
            // `mouseup`. So we give the user 200ms to release the mouse button, before we assume,
            // the events are unrelated.
            this.autoCompleteToggleTrigger.pipe(throttleTime(200)),
            this.autoCompleteToggleTrigger,
        ])
            .pipe(
                // Each time, the panel tries to open or close, we enforce our throttled action,
                // regardless of what is wants to do at the given point. E.g., if it opened and
                // closed again within 200ms, we just open it again.
                map(([throttledAction]) => {
                    if (throttledAction === 'open' || throttledAction === 'opened') {
                        return 'open';
                    } else {
                        // throttledAction === 'close' || throttledAction === 'closed'
                        return 'close';
                    }
                }),
                // Delay the open event, so the panel doesn't decide it needs to be closed because
                // of the toggle button press before it even opened.
                delay(0),
            )
            .subscribe((action) => {
                switch (action) {
                    case 'open':
                        this.trigger.openPanel();
                        this.input.nativeElement.focus();
                        break;
                    case 'close':
                        this.trigger.closePanel();
                        break;
                }
            });
    }

    private removeFromIndeterminateValues(key: string): void {
        const indeterminateValues = this.indeterminateValues$.value;
        if (key && indeterminateValues?.includes(key)) {
            indeterminateValues.splice(indeterminateValues.indexOf(key), 1);
            this.indeterminateValues$.next(indeterminateValues);
        }
    }

    private subscribeForSuggestionUpdates(): Observable<DisplayValue[]> {
        // Combine observables to update suggestions when either the input field or currently
        // selected values change.
        return combineLatest([
            this.inputControl.valueChanges.pipe(
                startWith(null as string),
                filter(
                    (value: string | null | DisplayValue) =>
                        typeof value === 'string' || value === null,
                ),
                // Debounce user input, but pass on a cleared input field immediately.
                debounce((value) => (value !== null ? timer(200) : EMPTY)),
                distinctUntilChanged(),
            ) as Observable<string | null>,
            this.chipsControl.valueChanges.pipe(
                startWith(this.chipsControl.value),
                distinctUntilChanged(),
            ),
        ]).pipe(
            // When accepting a value, the chips' value and the input's value both change. Debounce
            // to only trigger once in that case.
            debounceTime(0),
            switchMap(([filterString, selectedValues]) =>
                this.filter(filterString, selectedValues),
            ),
            // Don't send multiple requests for multiple subscribers.
            shareReplay(1),
        );
    }

    toDisplayValue(value: MdsWidgetValue | string): DisplayValue {
        if (typeof value === 'string') {
            const knownValue = this.widget.definition.values?.find((v) => v.id === value);
            if (!knownValue && this.widget.getInitialDisplayValues().value) {
                const ds = this.widget
                    .getInitialDisplayValues()
                    .value.values?.find((v) => v.key === value)?.displayString;
                return {
                    key: value,
                    label: ds || value,
                };
            }
            if (knownValue) {
                value = knownValue;
            } else {
                return {
                    key: value,
                    label: value,
                };
            }
        }
        return {
            key: value.id,
            label: value.caption,
        };
    }

    private filter(
        filterString: string | null,
        selectedValues: DisplayValue[],
    ): Observable<DisplayValue[]> {
        return from(this.widget.getSuggestedValues(filterString)).pipe(
            map((suggestedValues) =>
                suggestedValues
                    .map((suggestedValue) => this.toDisplayValue(suggestedValue))
                    .filter((suggestedValue) =>
                        selectedValues.every(
                            (selectedValue) => suggestedValue.key !== selectedValue.key,
                        ),
                    ),
            ),
        );
    }

    private inhibitAutocomplete() {
        this.autocompleteIsInhibited.next(true);
        this.autoCompleteToggleTrigger.next('close');
        setTimeout(() => {
            this.autocompleteIsInhibited.next(false);
        });
    }
}
@Component({
    templateUrl: './mds-editor-widget-chips.component.html',
    styleUrls: ['./mds-editor-widget-chips.component.scss'],
})
export class MdsEditorWidgetChipsRangedValueComponent extends MdsEditorWidgetChipsComponent {}
