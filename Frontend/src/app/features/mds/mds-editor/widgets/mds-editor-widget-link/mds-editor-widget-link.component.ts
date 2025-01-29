import { Component, Input, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject } from 'rxjs';
import { LocalEventsService } from 'ngx-edu-sharing-ui';
import { DialogsService } from '../../../../dialogs/dialogs.service';
import { Constraints, NativeWidgetType } from '../../../types/types';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';
import { MdsEditorWidgetAuthorComponent } from '../mds-editor-widget-author/mds-editor-widget-author.component';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'es-mds-editor-widget-link',
    templateUrl: './mds-editor-widget-link.component.html',
    styleUrls: ['./mds-editor-widget-link.component.scss'],
})
export class MdsEditorWidgetLinkComponent
    extends MdsEditorWidgetBase
    implements OnInit, NativeWidgetComponent
{
    static readonly constraints: Constraints = {
        supportsInlineEditing: true,
        requiresNode: true,
        supportsBulk: false,
    };
    readonly valueType: ValueType = ValueType.String;
    @Input() widgetName: NativeWidgetType.Maptemplate | NativeWidgetType.Contributor;

    hasChanges = new BehaviorSubject<boolean>(false);

    // caption: string; // Could use as label.
    linkLabel: string;

    constructor(
        public translate: TranslateService,
        public mdsEditorValues: MdsEditorInstanceService,
        private dialogs: DialogsService,
        private localEvents: LocalEventsService,
    ) {
        super(mdsEditorValues, translate);
    }
    ngOnInit(): void {
        switch (this.widgetName) {
            case 'maptemplate':
                this.linkLabel = 'MDS.TEMPLATE_LINK';
                break;
            case 'contributor':
                this.linkLabel = 'MDS.CONTRIBUTOR_LINK';
                break;
        }
    }

    async onClick() {
        if (this.widgetName === 'maptemplate') {
            const nodes = await this.mdsEditorValues.save();
            if (Array.isArray(nodes)) {
                this.localEvents.nodesChanged.emit(nodes);
            }
            void this.dialogs.openNodeTemplateDialog({
                node: this.mdsEditorValues.nodes$.value[0],
            });
        } else if (this.widgetName === 'contributor') {
            await MdsEditorWidgetAuthorComponent.openContributorDialog(
                this.mdsEditorValues,
                this.dialogs,
            );
        } else {
            throw new Error('not implemented');
        }
    }
}
