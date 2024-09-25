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

    async ngOnInit() {
        this.formControl = new UntypedFormControl(
            false,
            this.getStandardValidators({ requiredValidator }),
        );
        this.formControl.valueChanges.subscribe((value: boolean) => {
            this.setValue([value ? value.toString() : 'false'], this.formControl.dirty);
        });
        const initialValue = (await this.widget.getInitalValuesAsync()).jointValues?.[0];
        this.formControl.setValue(initialValue === 'true');
        this.isIndeterminate = !!(await this.widget.getInitalValuesAsync()).individualValues;
        this.setIndeterminateValues(this.isIndeterminate);

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
