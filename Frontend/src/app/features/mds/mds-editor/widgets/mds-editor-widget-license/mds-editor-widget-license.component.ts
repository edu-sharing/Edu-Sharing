import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject } from 'rxjs';
import { Node } from '../../../../../core-module/rest/data-object';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { RestConnectorService } from '../../../../../core-module/rest/services/rest-connector.service';
import { NodeHelperService } from '../../../../../services/node-helper.service';
import { MainNavService } from '../../../../../main/navigation/main-nav.service';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { Constraints, MdsWidgetValue, NativeWidgetComponent, Values } from '../../../types/types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { DialogsService } from '../../../../dialogs/dialogs.service';

@Component({
    selector: 'es-mds-editor-widget-license',
    templateUrl: './mds-editor-widget-license.component.html',
    styleUrls: ['./mds-editor-widget-license.component.scss'],
})
export class MdsEditorWidgetLicenseComponent
    extends MdsEditorWidgetBase
    implements OnInit, NativeWidgetComponent
{
    static readonly constraints: Constraints = {
        supportsInlineEditing: true,
        requiresNode: false,
        supportsBulk: true,
    };
    readonly valueType: ValueType = ValueType.MultiValue;
    hasChanges = new BehaviorSubject<boolean>(false);

    isSafe: boolean;
    nodes: Node[];
    licenses: License[];
    /** IDs of checked licenses. */
    checked: string[] = [];

    constructor(
        private connector: RestConnectorService,
        private mainnav: MainNavService,
        private sanitizer: DomSanitizer,
        public translate: TranslateService,
        private nodeHelper: NodeHelperService,
        public mdsEditorValues: MdsEditorInstanceService,
        private dialogs: DialogsService,
    ) {
        super(mdsEditorValues, translate);
        this.isSafe = this.connector.getCurrentLogin()?.currentScope === RestConstants.SAFE_SCOPE;
    }

    ngOnInit(): void {
        this.nodes = this.mdsEditorValues.nodes$.value;
        this.mdsEditorValues.nodes$.subscribe((n) => (this.nodes = n));
        this.licenses = this.widget?.definition?.values.map((v) => {
            const url = this.nodeHelper.getLicenseIconByString(v.id, false);
            const license: License = v;
            if (url) {
                license.imageUrl = this.sanitizer.bypassSecurityTrustUrl(url);
            }
            return license;
        });
        this.checked = this.widget.getInitialValues()?.jointValues ?? [];
    }

    async getValues(values: Values) {
        // nodes mode is read-only, so do not change anything
        if (this.mdsEditorValues.editorMode === 'nodes') {
            return values;
        }
        values[this.widget.definition.id] = this.checked;
        // The property used to be deleted, when no license was selected as commented out below.
        //
        // When aggregating all widgets' values, this leads to the widget object (`this.widget`) to
        // set its old initial value since it is not overridden here anymore.
        //
        // If we need the `delete` for some reason, we need to find a way to prevent this by getting
        // rid of the additional state in the widget object or synchronizing with it.

        // if (!this.checked.length) {
        //     delete values[this.widget.definition.id];
        // }
        return values;
    }

    async openLicense(): Promise<void> {
        const dialogRef = await this.dialogs.openLicenseDialog({
            kind: 'nodes',
            nodes: this.mdsEditorValues.nodes$.value,
        });
        dialogRef.afterClosed().subscribe((updatedNodes) => {
            if (updatedNodes) {
                this.nodes = updatedNodes;
                this.mdsEditorValues.updateNodes(this.nodes);
            }
        });
    }

    updateValue(license: License, status: boolean) {
        if (status) {
            this.checked.push(license.id);
        } else {
            this.checked.splice(this.checked.indexOf(license.id), 1);
        }
        this.hasChanges.next(true);
    }
}

type License = MdsWidgetValue & { imageUrl?: SafeUrl };
