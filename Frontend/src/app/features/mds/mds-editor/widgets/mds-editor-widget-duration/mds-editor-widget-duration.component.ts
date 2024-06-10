import { Component, OnInit } from '@angular/core';
import { Options } from '@angular-slider/ngx-slider';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'es-mds-editor-widget-duration',
    templateUrl: './mds-editor-widget-duration.component.html',
    styleUrls: ['./mds-editor-widget-duration.component.scss'],
})
export class MdsEditorWidgetDurationComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;

    sliderOptions: Options = {
        floor: 0,
        ceil: 0,
        animate: true,
        step: 1,
        draggableRange: true,
        minRange: 1,
        translate: (value: number): string => this.format(value),
    };

    currentValue: number; // Total minutes
    hours: string;
    minutes: string;

    async ngOnInit() {
        await this.initCurrentValue();
        this.sliderOptions.floor = this.widget.definition.min ?? 0;
        this.sliderOptions.ceil = this.widget.definition.max ?? 599;
        this.updateInput();
        this.widget.observeIsDisabled().subscribe((isDisabled) => {
            this.sliderOptions = {
                ...this.sliderOptions,
                disabled: isDisabled,
            };
            if (isDisabled) {
                this.setStatus('DISABLED');
            } else {
                this.setStatus('VALID');
            }
        });
    }

    update(src: 'slider' | 'input') {
        if (src === 'slider') {
            this.updateInput();
        } else {
            this.currentValue = parseInt(this.hours, 10) * 60 + parseInt(this.minutes, 10);
        }
        this.setValue_();
    }

    updateInput() {
        this.hours = Math.floor(this.currentValue / 60) + '';
        this.minutes = (this.currentValue % 60) + '';
    }

    private format(value: number): string {
        return value + ' ' + this.translate.instant('INPUT_MINUTES');
    }

    private async initCurrentValue() {
        const initialValues = (await this.widget.getInitalValuesAsync()).jointValues;
        let value = parseInt(initialValues[0] ?? '0', 10);
        // Internally values are saved as [ms].
        const { valueMin, wasMin } = this.msToMin(value);
        this.currentValue = valueMin;
        // Update legacy values, that were stored as [min].
        if (wasMin) {
            this.setValue_();
        }
    }

    /** Convert milliseconds to minutes. */
    private msToMin(valueMs: number): { valueMin: number; wasMin?: boolean } {
        if (!valueMs) {
            return { valueMin: valueMs };
        }
        // Graceful migration of values falsely saved as [min].
        if (valueMs > 0 && valueMs < 1000) {
            return { valueMin: valueMs, wasMin: true };
        }
        return { valueMin: valueMs / 60000 };
    }

    private setValue_(): void {
        this.setValue([(this.currentValue * 60000).toString()]);
    }
}
