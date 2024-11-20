import { Component, Inject } from '@angular/core';
import { FormControl } from '@angular/forms';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { CheckboxDialogData, CheckboxDialogResult } from './checkbox-dialog-data';
import { SharedModule } from '../../../../shared/shared.module';

/**
 * A generic dialog with an input form field and configurable title, message, label and icon.
 */
@Component({
    standalone: true,
    imports: [SharedModule],
    templateUrl: './checkbox-dialog.component.html',
    styleUrls: ['./checkbox-dialog.component.scss'],
})
export class CheckboxDialogComponent {
    control = new FormControl(false);

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: CheckboxDialogData,
        private dialogRef: CardDialogRef<CheckboxDialogData, CheckboxDialogResult>,
    ) {
        this.control.valueChanges.subscribe((value) => (this.data.state = value));
    }
}
