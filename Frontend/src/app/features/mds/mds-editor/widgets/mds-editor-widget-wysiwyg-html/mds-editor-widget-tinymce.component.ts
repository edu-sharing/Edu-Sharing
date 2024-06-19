import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { TranslateService } from '@ngx-translate/core';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { PlatformLocation } from '@angular/common';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { CardDialogService } from '../../../../dialogs/card-dialog/card-dialog.service';

@Component({
    selector: 'es-mds-editor-widget-checkbox',
    templateUrl: './mds-editor-widget-tinymce.component.html',
    styleUrls: ['./mds-editor-widget-tinymce.component.scss'],
    providers: [
        {
            provide: TINYMCE_SCRIPT_SRC,
            useFactory: (platformLocation: PlatformLocation) => {
                return platformLocation.getBaseHrefFromDOM() + 'tinymce/tinymce.min.js';
            },
            deps: [PlatformLocation],
        },
    ],
})
export class MdsEditorWidgetTinyMCE extends MdsEditorWidgetBase implements OnInit, AfterViewInit {
    @ViewChild(EditorComponent) editorComponent: EditorComponent;
    @ViewChild(MdsEditorWidgetContainerComponent)
    containerComponent: MdsEditorWidgetContainerComponent;
    readonly valueType: ValueType = ValueType.String;
    private editorConfigDefault = {
        branding: false,
        height: 200,
        apiKey: '',
        menubar: false,
        statusbar: false,
        resize: true,
        plugins: ['link'],
        toolbar:
            'h1 h2 h3 h4 | bold italic underline | link | alignleft aligncenter alignright alignjustify | removeformat | undo redo',
        language: this.translate.getDefaultLang(),
    };
    editorConfig: Record<string, any>;
    _html = '';
    dummyControl = new UntypedFormControl();
    get html() {
        return this._html;
    }
    set html(html: string) {
        this._html = html;
        this.setValue([html]);
    }
    constructor(
        private platformLocation: PlatformLocation,
        public mdsEditorInstance: MdsEditorInstanceService,
        public cardService: CardDialogService,
        protected translate: TranslateService,
    ) {
        super(mdsEditorInstance, translate);
    }
    ngOnInit(): void {}

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

    focus(): void {
        this.editorComponent.editor.focus();
        // this.editor.editorElem.focus();
    }

    blur(): void {
        // @TODO: Check if this works as expected
        this.editorComponent.editor.editorContainer.blur();
        //this.editorComponent.editor.execCommand('blur');
    }

    async ngAfterViewInit() {
        this._html = (await this.widget.getInitalValuesAsync()).jointValues[0];
        if (this.widget.definition.configuration) {
            this.editorConfig = {
                ...this.editorConfigDefault,
                ...JSON.parse(this.widget.definition.configuration),
            };
        } else {
            this.editorConfig = this.editorConfigDefault;
        }
        // dirty workaround for tinyMCE
        setTimeout(() => {
            this.editorComponent.editor.mode.set(
                this.dummyControl.disabled ? 'readonly' : 'design',
            );
            this.dummyControl.registerOnDisabledChange((isDisabled) =>
                this.editorComponent.editor.mode.set(isDisabled ? 'readonly' : 'design'),
            );
            // we need to disable the focus trap cause otherwise any overlay dialogs (i.e. insert link) of tinymce will break
            this.cardService.getFocusTraps().forEach((f) => f._disable());
        });
    }
}
