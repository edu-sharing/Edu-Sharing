import { Component, OnInit } from '@angular/core';
import { AbstractControl, UntypedFormControl, ValidatorFn } from '@angular/forms';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'es-mds-editor-widget-checkbox',
    templateUrl: './mds-editor-widget-checkbox.component.html',
    styleUrls: ['./mds-editor-widget-checkbox.component.scss'],
})
export class MdsEditorWidgetCheckboxComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;
    formControl: UntypedFormControl;
    isIndeterminate: boolean;

    ngOnInit(): void {
        const initialValue = this.widget.getInitialValues()?.jointValues?.[0];
        this.isIndeterminate = !!this.widget.getInitialValues()?.individualValues;
        this.setIndeterminateValues(this.isIndeterminate);
        this.formControl = new UntypedFormControl(
            initialValue === 'true',
            this.getStandardValidators({ requiredValidator }),
        );
        this.formControl.valueChanges.subscribe((value: boolean) => {
            this.setValue([value ? value.toString() : 'false'], this.formControl.dirty);
        });
        this.registerValueChanges(this.formControl);
    }

    onIndeterminateChange(isIndeterminate: boolean): void {
        this.setIndeterminateValues(isIndeterminate);
    }

    private setIndeterminateValues(isIndeterminate: boolean): void {
        if (isIndeterminate) {
            this.widget.setIndeterminateValues(['false', 'true']);
        } else {
            this.widget.setIndeterminateValues(null);
        }
    }
}

const requiredValidator: ValidatorFn = (
    control: AbstractControl,
): { [key: string]: any } | null => {
    const valid = control.value === true;
    return valid ? null : { required: true };
};
