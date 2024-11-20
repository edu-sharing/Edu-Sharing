import { Component, Inject } from '@angular/core';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { RevocationDialogData, RevocationDialogResult } from './revocation-dialog-data';
import { SharedModule } from '../../../../shared/shared.module';
import { FormControl, FormControlStatus, Validators } from '@angular/forms';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { DialogButton } from '../../../../util/dialog-button';
import { HOME_REPOSITORY, NodeServiceUnwrapped } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../core-module/rest/rest-constants';

@Component({
    standalone: true,
    imports: [SharedModule],
    selector: 'es-revocation-dialog',
    templateUrl: './revocation-dialog.component.html',
    styleUrls: ['./revocation-dialog.component.scss'],
})
export class RevocationDialogComponent {
    reasonControl = new FormControl('', Validators.required);
    edit: boolean;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: RevocationDialogData,
        private dialogRef: CardDialogRef<RevocationDialogData, RevocationDialogResult>,
        private nodeHelperService: NodeHelperService,
        private nodeService: NodeServiceUnwrapped,
    ) {
        if (this.nodeHelperService.isNodeRevoked(data.node)) {
            this.edit = true;
            this.reasonControl.setValue(
                data.node.properties[RestConstants.CCM_PROP_REVOKED_REASON]?.join(''),
            );

            this.dialogRef.patchConfig({
                buttons: DialogButton.getSaveCancel(
                    () => this.dialogRef.close(),
                    () => this.save(),
                ),
            });
        } else {
            this.edit = false;
            const buttons = [
                new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.dialogRef.close()),
                new DialogButton('WORKSPACE.REVOCATION.REVOKE', DialogButton.TYPE_DANGER, () =>
                    this.save(),
                ),
            ];
            buttons[1].disabled = true;
            this.dialogRef.patchConfig({ buttons });
        }
        this.reasonControl.statusChanges.subscribe((status: FormControlStatus) => {
            const buttons = this.dialogRef.config.buttons;
            buttons[1].disabled = status !== 'VALID';
            this.dialogRef.patchConfig({
                buttons,
            });
        });
    }

    private async save() {
        this.dialogRef.patchState({
            isLoading: true,
        });
        const node = await this.nodeService
            .revokeCopy({
                repository: HOME_REPOSITORY,
                node: this.data.node.ref.id,
                body: {
                    reason: this.reasonControl.value,
                },
            })
            .toPromise();
        this.dialogRef.close({
            reason: this.reasonControl.value,
            node,
        });
    }
}
