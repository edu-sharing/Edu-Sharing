import { Component, EventEmitter, forwardRef, Input, Output } from '@angular/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { NgxMonacoEditorConfig } from 'ngx-monaco-editor-v2';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
    selector: 'es-code-editor',
    templateUrl: 'code-editor.component.html',
    styleUrls: ['code-editor.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CodeEditorComponent),
            multi: true,
        },
    ],
})
export class CodeEditorComponent implements ControlValueAccessor {
    @Input() options: NgxMonacoEditorConfig | any;
    @Input() ngModel: string;
    @Output() ngModelChange = new EventEmitter<string>();
    editorType: 'Textarea' | 'Monaco' | undefined;

    constructor(private configService: ConfigService) {
        this.configService.observeConfig().subscribe((config) => {
            this.editorType = config.admin?.editorType || 'Monaco';
        });
    }

    writeValue(obj: any): void {
        this.ngModel = obj;
    }
    registerOnChange(fn: any): void {
        this.ngModelChange.subscribe((v) => {
            fn(v);
        });
    }
    registerOnTouched(fn: any): void {}
    setDisabledState?(isDisabled: boolean): void {
        if (isDisabled) {
            throw new Error('Method not implemented.');
        }
    }
}
