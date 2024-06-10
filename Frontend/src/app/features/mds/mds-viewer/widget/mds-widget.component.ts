import { DatePipe } from '@angular/common';
import {
    Component,
    ElementRef,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DateHelper, FormatSizePipe } from 'ngx-edu-sharing-ui';
import { MdsEditorInstanceService, Widget } from '../../mds-editor/mds-editor-instance.service';
import { MdsEditorViewComponent } from '../../mds-editor/mds-editor-view/mds-editor-view.component';
import { ViewInstanceService } from '../../mds-editor/mds-editor-view/view-instance.service';
import { MdsEditorWidgetBase, ValueType } from '../../mds-editor/widgets/mds-editor-widget-base';
import { RestHelper } from '../../../../core-module/rest/rest-helper';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { MdsWidgetType } from '../../types/types';
import { UIService } from '../../../../core-module/rest/services/ui.service';
import { MatRipple } from '@angular/material/core';
import { filter } from 'rxjs/operators';

@Component({
    selector: 'es-mds-widget',
    templateUrl: 'mds-widget.component.html',
    styleUrls: ['mds-widget.component.scss'],
})
export class MdsWidgetComponent extends MdsEditorWidgetBase implements OnInit, OnChanges {
    private static readonly inlineEditing: MdsWidgetType[] = [
        MdsWidgetType.Text,
        MdsWidgetType.Number,
        MdsWidgetType.Date,
        MdsWidgetType.Email,
        MdsWidgetType.Textarea,
        MdsWidgetType.Singleoption,
        MdsWidgetType.SingleValueTree,
        MdsWidgetType.SingleValueSuggestBadges,
        MdsWidgetType.MultiValueBadges,
        MdsWidgetType.MultiValueFixedBadges,
        MdsWidgetType.MultiValueSuggestBadges,
        MdsWidgetType.MultiValueTree,
    ];

    readonly valueType = ValueType.String;

    @Input() widget: Widget;
    @Input() view: MdsEditorViewComponent;

    @ViewChild('editWrapper') editWrapper: ElementRef;
    @ViewChild(MatRipple) matRipple: MatRipple;

    get headingLevel() {
        return this.viewInstance.headingLevel;
    }

    value: string[] = undefined;
    private temporaryValue: string[] = undefined;

    constructor(
        public mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private ui: UIService,
        private viewInstance: ViewInstanceService,
    ) {
        super(mdsEditorInstance, translate);
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.value = this.getNodeValue();
    }

    ngOnInit() {
        this.value = this.getNodeValue();
        this.widget
            .getInitialDisplayValues()
            .pipe(filter((v) => !!v))
            .subscribe(async (value) => {
                this.value = value.values.map((v) => v.displayString);
            });
    }

    getBasicType() {
        switch (this.widget.definition.type) {
            case 'text':
            case 'email':
            case 'month':
            case 'color':
            case 'textarea':
            case 'singleoption':
                return 'text';
            case 'number':
                return 'number';
            case 'date':
                return 'date';
            case 'vcard':
                return 'vcard';
            case 'multivalueFixedBadges':
            case 'multivalueSuggestBadges':
            case 'singlevalueSuggestBadges':
            case 'multivalueBadges':
            case 'singlevalueTree':
            case 'multivalueTree':
                return 'array';
            case 'slider':
                return 'slider';
            case 'duration':
                return 'duration';
            case 'range':
                return 'range';
        }
        return 'unknown';
    }

    supportsInlineEditing() {
        return MdsWidgetComponent.inlineEditing.includes(
            this.widget.definition.type as MdsWidgetType,
        );
    }

    private getNodeValue() {
        if (this.temporaryValue !== undefined) {
            return this.getValue(this.temporaryValue);
        }
        const id = this.widget.definition.id;
        if (this.widget.definition.type === 'range') {
            const values = this.mdsEditorInstance.values$.value;
            if (values) {
                return [values[id + '_from']?.[0], values[id + '_to']?.[0]];
            }
            return null;
        } else if (this.mdsEditorInstance.values$.value?.[id]) {
            // support on the fly changes+updates of the values
            return this.getValue(this.mdsEditorInstance.values$.value[id]);
        } else if (this.widget.getInitialValues()?.jointValues) {
            return this.getValue(this.widget.getInitialValues().jointValues);
        } else {
            return null;
        }
    }

    getValue(data: string[]) {
        let value = data;
        if (!value || value.every((v) => !v)) {
            return null;
        }

        if (this.widget.definition.values) {
            const mapping = this.widget.definition.values
                .filter((v: any) => data.filter((d) => d === v.id).length > 0)
                .map((v) => v.caption);
            if (mapping) {
                return mapping;
            }
        }

        return data;
    }

    click() {
        if (this.widget.definition.link === '_BLANK') {
            window.open(this.formatText()[0]);
        } else if (this.widget.definition.link === '_SELF') {
            window.location.href = this.formatText()[0];
        } else {
            console.warn('Unsupported link type ' + this.widget.definition.link);
        }
    }

    isEmpty() {
        return this.value?.every((v) => !v) || this.value?.length === 0 || !this.value;
    }

    formatDate() {
        return this.value.map((v) => {
            if (this.widget.definition.format) {
                try {
                    return new DatePipe('en').transform(v, this.widget.definition.format);
                } catch (e) {
                    console.warn('Could not format date', e, this.widget.definition);
                    return DateHelper.formatDate(this.translate, v, {
                        showAlwaysTime: true,
                    });
                }
            } else {
                return DateHelper.formatDate(this.translate, v, {
                    showAlwaysTime: true,
                });
            }
        });
    }

    formatNumber() {
        return this.value.map((v) => {
            if (this.widget.definition.format === 'bytes') {
                return new FormatSizePipe(this.translate).transform(v);
            }
            return v;
        });
    }

    formatText() {
        return this.value.map((v) => {
            if (this.widget.definition.format) {
                return this.widget.definition.format.replace('${value}', v);
            }
            return v;
        });
    }

    async finishEdit(instance: MdsEditorWidgetBase) {
        await this.mdsEditorInstance.saveWidgetValue(instance.widget);
        this.temporaryValue = instance.widget.getValue();
        this.value = this.getNodeValue();
        this.editWrapper.nativeElement.children[0].innerHTML = null;
        await this.mdsEditorInstance.fetchDisplayValues(this.widget);
    }

    isEditable() {
        const nodes = this.mdsEditorInstance.nodes$.value;
        return (
            this.mdsEditorInstance.editorMode === 'inline' &&
            this.widget.definition.interactionType === 'Input' &&
            nodes?.length === 1 &&
            RestHelper.hasAccessPermission(nodes[0], RestConstants.ACCESS_WRITE) &&
            this.supportsInlineEditing()
        );
    }

    async focus() {
        if (this.isEditable()) {
            this.matRipple.launch({});
            await this.ui.scrollSmoothElementToChild(this.editWrapper.nativeElement);
            //const result = await this.view.injectEditField(this, this.editWrapper.nativeElement.children[0]);
            //await this.ui.scrollSmoothElementToChild(result.htmlElement);
        }
    }
}
