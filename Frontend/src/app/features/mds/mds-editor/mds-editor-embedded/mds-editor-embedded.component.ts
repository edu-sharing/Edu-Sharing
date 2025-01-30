import { Component } from '@angular/core';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';

@Component({
    selector: 'es-mds-editor-embedded',
    templateUrl: './mds-editor-embedded.component.html',
    styleUrls: ['./mds-editor-embedded.component.scss'],
})
export class MdsEditorEmbeddedComponent {
    constructor(private mdsEditorInstance: MdsEditorInstanceService) {
        this.mdsEditorInstance.isEmbedded = true;
    }
}
