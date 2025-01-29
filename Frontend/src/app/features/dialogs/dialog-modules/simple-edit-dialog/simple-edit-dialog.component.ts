import { Component, Inject, ViewChild } from '@angular/core';
import { LocalEventsService } from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import {
    DialogButton,
    RestConnectorService,
    RestConstants,
    RestNodeService,
} from '../../../../core-module/core.module';
import { Node } from 'ngx-edu-sharing-api';
import { Toast } from '../../../../services/toast';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { DialogsService } from '../../dialogs.service';
import { SAVE_OR_CANCEL } from '../generic-dialog/generic-dialog-data';
import { SimpleEditDialogData, SimpleEditDialogResult } from './simple-edit-dialog-data';
import { SimpleEditInviteComponent } from './simple-edit-invite/simple-edit-invite.component';
import { SimpleEditLicenseComponent } from './simple-edit-license/simple-edit-license.component';
import { SimpleEditMetadataComponent } from './simple-edit-metadata/simple-edit-metadata.component';

@Component({
    selector: 'es-simple-edit-dialog',
    templateUrl: './simple-edit-dialog.component.html',
    styleUrls: ['./simple-edit-dialog.component.scss'],
})
export class SimpleEditDialogComponent {
    @ViewChild('metadata') metadata: SimpleEditMetadataComponent;
    @ViewChild('invite') invite: SimpleEditInviteComponent;
    @ViewChild('license') license: SimpleEditLicenseComponent;

    protected _nodes: Node[] = this.data.nodes;
    private initState = { license: false, invite: false };
    protected tpInvite: boolean;
    protected tpLicense: boolean;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: SimpleEditDialogData,
        private dialogRef: CardDialogRef<SimpleEditDialogData, SimpleEditDialogResult>,
        private connector: RestConnectorService,
        private dialogs: DialogsService,
        private localEvents: LocalEventsService,
        private nodeApi: RestNodeService,
        private toast: Toast,
    ) {
        this.dialogRef.patchState({ isLoading: true });
        this.connector
            .hasToolPermission(RestConstants.TOOLPERMISSION_INVITE)
            .subscribe((tp) => (this.tpInvite = tp));
        this.connector
            .hasToolPermission(RestConstants.TOOLPERMISSION_LICENSE)
            .subscribe((tp) => (this.tpLicense = tp));
        this._initButtons();
    }

    private _initButtons(): void {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this._cancel()),
                new DialogButton('SAVE', { color: 'primary' }, () => this._saveAndClose()),
            ],
        });
    }

    private _saveAndClose(): void {
        this._save().subscribe((nodes) => {
            if (nodes && nodes.length > 0) {
                this.localEvents.nodesChanged.next(nodes);
                this.dialogRef.close(nodes);
            }
        });
    }

    /**
     * Closes the dialog without saving data.
     */
    private _cancel(): void {
        this.dialogRef.close(null);
    }

    /**
     * Opens the full MDS dialog on top of the current dialog.
     */
    protected async _openMetadata(): Promise<void> {
        const upToDate = await this._ensureUpToDate();
        if (!upToDate) {
            return;
        }
        const dialogRef = await this.dialogs.openMdsEditorDialogForNodes({
            nodes: this._nodes,
        });
        const updatedNodes = await dialogRef.afterClosed().toPromise();
        if (updatedNodes) {
            this._nodes = updatedNodes;
        }
    }

    /**
     * Opens the share dialog on top of the current dialog.
     */
    protected async _openShare(): Promise<void> {
        const upToDate = await this._ensureUpToDate();
        if (!upToDate) {
            return;
        }
        const dialogRef = await this.dialogs.openShareDialog({
            nodes: this._nodes,
        });
        await dialogRef.afterClosed().toPromise();
        this.dialogRef.patchState({ isLoading: true });
        rxjs.forkJoin(
            this._nodes.map((n) => this.nodeApi.getNodeMetadata(n.ref.id, [RestConstants.ALL])),
        ).subscribe((nodes) => {
            this._nodes = nodes.map((n) => n.node);
            this.dialogRef.patchState({ isLoading: false });
        });
    }

    /**
     * Opens the license dialog on top of the current dialog.
     */
    protected async _openLicense(): Promise<void> {
        const upToDate = await this._ensureUpToDate();
        if (!upToDate) {
            return;
        }
        const dialogRef = await this.dialogs.openLicenseDialog({
            kind: 'nodes',
            nodes: this._nodes,
        });
        const updatedNodes = await dialogRef.afterClosed().toPromise();
        if (updatedNodes) {
            this._nodes = updatedNodes;
        }
    }

    /**
     * Saves changed data.
     *
     * @returns updated nodes if successful, `null` otherwise
     */
    private _save(): Observable<Node[] | null> {
        if (!this.metadata.validate()) {
            return rxjs.of(null);
        }
        this.dialogRef.patchState({ isLoading: true });
        return this.metadata.save().pipe(
            switchMap(() => this.invite.save()),
            switchMap(() => this.license.save()),
            switchMap(() =>
                rxjs.forkJoin(
                    this._nodes.map((n) =>
                        this.nodeApi.getNodeMetadata(n.ref.id, [RestConstants.ALL]),
                    ),
                ),
            ),
            map((nodes) => {
                this.toast.toast('SIMPLE_EDIT.SAVED' + (nodes.length === 1 ? '' : '_MULTIPLE'), {
                    name: nodes[0].node.name,
                    count: nodes.length,
                });
                this.dialogRef.patchState({ isLoading: false });
                return nodes.map((n) => n.node);
            }),
            catchError((error) => {
                this.toast.error(error);
                this.dialogRef.patchState({ isLoading: false });
                return rxjs.of(null);
            }),
        );
    }

    /**
     * Checks if there are any unsaved changes and prompts the user to save any changes if so.
     *
     * When the user accepts, saves any data.
     *
     * @returns whether data is up-to-date
     */
    private async _ensureUpToDate(): Promise<boolean> {
        if (!this._isDirty()) {
            return true;
        }
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'SIMPLE_EDIT.DIRTY.TITLE',
            message: 'SIMPLE_EDIT.DIRTY.MESSAGE',
            buttons: SAVE_OR_CANCEL,
        });
        const result = await dialogRef.afterClosed().toPromise();
        if (result === 'SAVE') {
            const updatedNodes = await this._save().toPromise();
            if (updatedNodes) {
                this._nodes = updatedNodes;
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if there are unsaved changes.
     */
    private _isDirty(): boolean {
        return this.metadata.isDirty() || this.invite.isDirty() || this.license.isDirty();
    }

    /**
     * Sets the state for the given component to initialized.
     *
     * When all components are initialized, turns off the loading spinner.
     */
    protected _updateInitState(component: keyof SimpleEditDialogComponent['initState']) {
        this.initState[component] = true;
        if (this.initState.invite && this.initState.license) {
            this.dialogRef.patchState({ isLoading: false });
        }
    }

    /**
     * Handles an error in a sub component.
     */
    protected _handleError(error: any): void {
        if (error) {
            this.toast.error(error);
        }

        // Before migration to standalone modules, this used to close the dialog on error. We copied
        // the behavior, but keeping the dialog and just turning off the loading spinner might be
        // the more sensible choice.

        // this.dialogRef.patchState({ isLoading: false });
        this.dialogRef.close(null);
    }
}
