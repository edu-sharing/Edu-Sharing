import { Component, Inject, OnInit } from '@angular/core';

import { TranslateService } from '@ngx-translate/core';
import {
    ConfigurationHelper,
    DialogButton,
    MdsInfo,
    MdsMetadatasets,
    RestConstants,
    RestMdsService,
} from '../../../../core-module/core.module';
import { ConfigService, Node } from 'ngx-edu-sharing-api';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { AddFolderDialogData, AddFolderDialogResult } from './add-folder-dialog-data';

@Component({
    selector: 'es-add-folder-dialog',
    templateUrl: './add-folder-dialog.component.html',
    styleUrls: ['./add-folder-dialog.component.scss'],
})
export class AddFolderDialogComponent implements OnInit {
    folderName = '';
    mdsSetsIds: MdsInfo[];
    mdsSets: MdsInfo[];
    mdsSet: string;
    _parent: Node;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: AddFolderDialogData,
        private dialogRef: CardDialogRef<AddFolderDialogData, AddFolderDialogResult>,
        private mds: RestMdsService,
        private translate: TranslateService,
        private config: ConfigService,
    ) {}

    ngOnInit(): void {
        this.processDialogData();
        this.updateButtons();
    }

    private processDialogData() {
        this.mds.getSets().subscribe((data: MdsMetadatasets) => {
            this.mdsSets = ConfigurationHelper.filterValidMds(
                RestConstants.HOME_REPOSITORY,
                data.metadatasets,
                this.config,
            );
            if (this.mdsSets) {
                UIHelper.prepareMetadatasets(this.translate, this.mdsSets);
                if (this.mdsSets.length) {
                    this.mdsSet = this.mdsSets[0].id;
                } else {
                    console.error(
                        'Filtering valid mds failed, no mds was available after filtering. Will use default mds',
                    );
                    console.error('Check availableMds in config');
                }
            }
            this._parent = this.data.parent;
            if (
                this._parent &&
                this._parent.metadataset &&
                this._parent.metadataset !== 'default'
            ) {
                this.mdsSet = this._parent.metadataset;
            }
        });
    }

    private cancel() {
        this.dialogRef.close(null);
    }

    addFolder() {
        const name = this.folderName.trim();
        if (!name) {
            return;
        }
        this.dialogRef.close({
            name,
            metadataSet: this.mdsSets ? this.mdsSet : null,
        });
    }

    updateButtons() {
        const buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            new DialogButton('SAVE', { color: 'primary' }, () => this.addFolder()),
        ];
        buttons[1].disabled = !this.folderName.trim();
        this.dialogRef.patchConfig({ buttons });
    }
}
