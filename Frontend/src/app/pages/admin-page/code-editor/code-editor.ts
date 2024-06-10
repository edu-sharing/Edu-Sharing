import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { NgxMonacoEditorConfig } from 'ngx-monaco-editor-v2';

@Component({
    selector: 'es-code-editor',
    templateUrl: 'code-editor.component.html',
    styleUrls: ['code-editor.component.scss'],
})
export class CodeEditorComponent {
    @Input() options: NgxMonacoEditorConfig | any;
    @Input() ngModel: string;
    @Output() ngModelChange = new EventEmitter<string>();
    editorType: 'Textarea' | 'Monaco' | undefined;

    constructor(private configService: ConfigService) {
        this.configService.observeConfig().subscribe((config) => {
            this.editorType = config.admin?.editorType || 'Monaco';
        });
    }
}
