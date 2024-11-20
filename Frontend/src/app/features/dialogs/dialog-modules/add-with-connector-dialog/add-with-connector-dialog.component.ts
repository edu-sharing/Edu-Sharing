import { Component, Inject, ViewChild } from '@angular/core';
import { Connector } from 'ngx-edu-sharing-api';
import { BehaviorSubject } from 'rxjs';
import { first } from 'rxjs/operators';
import { DialogButton } from '../../../../core-module/core.module';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    AddWithConnectorDialogData,
    AddWithConnectorDialogResult,
} from './add-with-connector-dialog-data';
import { MdsEditorWrapperComponent } from '../../../mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { FormatDatePipe } from 'ngx-edu-sharing-ui';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'es-add-with-connector-dialog',
    templateUrl: './add-with-connector-dialog.component.html',
    styleUrls: ['./add-with-connector-dialog.component.scss'],
})
export class AddWithConnectorDialogComponent {
    @ViewChild(MdsEditorWrapperComponent) mdsEditorRef: MdsEditorWrapperComponent;
    readonly connector = this.processConnector(this.data.connector);
    private nameSubject = new BehaviorSubject<string>(this.data.name ?? '');
    get name(): string {
        return this.nameSubject.value;
    }
    set name(value: string) {
        this.nameSubject.next(value);
    }
    type = 0;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: AddWithConnectorDialogData,
        private dialogRef: CardDialogRef<AddWithConnectorDialogData, AddWithConnectorDialogResult>,
        private translate: TranslateService,
    ) {
        this.initDialogConfig();
    }

    private cancel() {
        this.dialogRef.close(null);
    }

    private async create() {
        let data;
        if (this.connector.mdsGroup) {
            if (!this.mdsEditorRef.mdsEditorInstance.getCanSave()) {
                return;
            }

            data = await this.mdsEditorRef?.getValues();
            const primaryWidget = (
                await this.mdsEditorRef.mdsEditorInstance.widgets.pipe(first()).toPromise()
            ).filter((w) => w.definition.isRequired === 'mandatory');
            if (primaryWidget.length !== 1) {
                console.warn(
                    'The mds group ' +
                        this.connector.mdsGroup +
                        ' requires exactly one required widget to be used for the name/title!',
                );
                this.name = new FormatDatePipe(this.translate).transform(new Date(), {
                    relative: false,
                    time: true,
                    date: true,
                });
            } else {
                this.name = data[primaryWidget[0].definition.id].join('');
            }
        } else if (!this.name.trim()) {
            return;
        }
        this.dialogRef.close({ name: this.name, type: this.getType(), data });
    }

    private processConnector(connector: Connector): Connector {
        for (let i = 0; i < connector.filetypes.length; i++) {
            if (!connector.filetypes[i].creatable) {
                connector.filetypes.splice(i, 1);
            }
        }
        return connector;
    }

    private async initDialogConfig(): Promise<void> {
        const cancelButton = new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel());
        const createButton = new DialogButton('CREATE', { color: 'primary' }, () => this.create());
        this.dialogRef.patchConfig({
            title: 'CONNECTOR.' + this.connector.id + '.TITLE',
            subtitle: 'CONNECTOR.' + this.connector.id + '.NAME',
            avatar: { kind: 'icon', icon: this.connector.icon },
            buttons: [cancelButton, createButton],
        });
        this.nameSubject.subscribe((name) => (createButton.disabled = !name.trim()));
        this.nameSubject
            .pipe(first((name) => !!name))
            .subscribe(() => this.dialogRef.patchConfig({ closable: Closable.Standard }));
        setTimeout(() => {
            this.mdsEditorRef?.mdsEditorInstance
                .observeCanSave()
                .subscribe((can) => (createButton.disabled = !can));
        });
    }

    getType() {
        return this.connector.filetypes[this.type];
    }
}
