import { Component, Input } from '@angular/core';

@Component({
    selector: 'es-mds-editor-widget-error',
    templateUrl: './mds-editor-widget-error.component.html',
    styleUrls: ['./mds-editor-widget-error.component.scss'],
})
export class MdsEditorWidgetErrorComponent {
    @Input() widgetName: string;
    @Input() reason: string;

    constructor() {}
}
