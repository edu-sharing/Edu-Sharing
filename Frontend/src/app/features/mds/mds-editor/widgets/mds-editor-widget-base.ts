import { FormControl, UntypedFormControl, ValidatorFn, Validators } from '@angular/forms';
import { InputStatus, MdsWidget, RequiredMode } from '../../types/types';
import { Directive, EventEmitter, OnInit } from '@angular/core';
import { MdsEditorWidgetCore } from '../mds-editor-widget-core.directive';
import { SuggestionResponseDto, SuggestionStatus } from 'ngx-edu-sharing-api';
import { DisplayValue } from './DisplayValues';
import { AuthorityNamePipe } from '../../../../shared/pipes/authority-name.pipe';

export enum ValueType {
    String,
    MultiValue,
    Range,
}

@Directive()
export abstract class MdsEditorWidgetBase extends MdsEditorWidgetCore {
    abstract readonly valueType: ValueType;

    /**
     * triggered when the input focus is lost
     */
    onBlur = new EventEmitter<void>();

    /**
     * this method should set focus on the primary input of the widget
     */
    focus(): void {
        // default implementation will do nothing
    }

    protected setValue(value: string[], dirty?: boolean): void {
        this.widget.setValue(value, dirty);
    }

    protected setStatus(value: InputStatus): void {
        this.widget.setStatus(value);
    }

    /**
     * register the form control that should be updated when external value changes received
     */
    protected registerValueChanges(formControl: FormControl) {
        this.widget.setValueExternal.subscribe((value) => {
            formControl.setValue(value);
            this.setValue(value, true);
        });
    }

    protected getStandardValidators(
        overrides: { requiredValidator?: ValidatorFn } = {},
    ): ValidatorFn[] {
        const validators: ValidatorFn[] = [];
        const widgetDefinition = this.widget.definition;
        // Marking both `Mandatory` and `MandatoryForPublish` fields as required is needed for the
        // hint texts to be shown when revealing unfilled fields or leaving the field without
        // entering a value.
        if (
            this.mdsEditorInstance.editorMode !== 'search' &&
            (widgetDefinition.isRequired === RequiredMode.Mandatory ||
                widgetDefinition.isRequired === RequiredMode.MandatoryForPublish)
        ) {
            validators.push(overrides.requiredValidator ?? Validators.required);
        }
        return validators;
    }
    protected static attachGraphqlSelection(definition: MdsWidget, fields: string[]) {
        const id = MdsEditorWidgetBase.mapGraphqlId(definition);
        if (id) {
            const originalId = id[0];
            return fields.map((f) => originalId + '.' + f);
        }
        return null;
    }
    public static mapGraphqlId(definition: MdsWidget) {
        // @TODO: make types!
        const id = (definition as any).ids?.graphql;
        if (id) {
            return [(definition as any).ids?.graphql];
        }
        return null;
    }
    public static mapGraphqlSuggestionId(definition: MdsWidget): string[] {
        return [];
    }
}

@Directive()
/**
 * used by chips or tree based widget that might show suggestion chips
 */
export abstract class MdsEditorWidgetChipsSuggestionBase extends MdsEditorWidgetBase {
    // holds suggestions from users or automatic generated data
    chipsSuggestions: SuggestionResponseDto[];
    chipsControl: UntypedFormControl;

    abstract add(value: DisplayValue): void;
    abstract toDisplayValue(value: string): DisplayValue;
    initSuggestions(): void {
        this.chipsSuggestions =
            this.widget.getSuggestions()?.filter((s) => s.status === 'PENDING') ?? [];
    }
    removeSuggestion(toBeRemoved: SuggestionResponseDto): void {
        this.updateSuggestionState(toBeRemoved, 'DECLINED');
    }
    addSuggestion(suggestion: SuggestionResponseDto) {
        this.add(this.toDisplayValue(suggestion.value as string));
        this.updateSuggestionState(suggestion, 'ACCEPTED');
    }
    getSuggestionTooltip(suggestion: SuggestionResponseDto): string | null {
        return `${this.translate.instant('MDS.SUGGESTION_TOOLTIP', {
            value: this.toDisplayValue(suggestion.value as string).label,
            // @TODO
            creator: new AuthorityNamePipe(this.translate).transform(suggestion.createdBy),
        })}`;
    }
    updateSuggestionState(suggestion: SuggestionResponseDto, status: SuggestionStatus) {
        suggestion.status = status;
        this.mdsEditorInstance.updateSuggestionState(this.widget.definition.id, suggestion);
        this.chipsSuggestions.splice(this.chipsSuggestions.indexOf(suggestion), 1);
        this.widget.markSuggestionChanged();
    }

    getSuggestions() {
        // console.log(this.chipsSuggestions, this.chipsControl);
        return this.chipsSuggestions?.filter(
            (s) => !this.chipsControl.value.some((s1: DisplayValue) => s1.key === s.value),
        );
    }
}
