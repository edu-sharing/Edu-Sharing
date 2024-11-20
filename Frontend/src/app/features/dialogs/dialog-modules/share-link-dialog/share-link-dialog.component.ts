import { Component, Inject, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
    DialogButton,
    Node,
    NodeShare,
    RestConstants,
    RestNodeService,
} from '../../../../core-module/core.module';
import { DateHelper } from 'ngx-edu-sharing-ui';
import { Toast } from '../../../../services/toast';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { ShareLinkDialogData, ShareLinkDialogResult } from './share-link-dialog-data';
import { BehaviorSubject } from 'rxjs';
import { filter, first } from 'rxjs/operators';

@Component({
    selector: 'es-share-link-dialog',
    templateUrl: './share-link-dialog.component.html',
    styleUrls: ['./share-link-dialog.component.scss'],
})
export class ShareLinkDialogComponent implements OnInit {
    private loading$ = new BehaviorSubject<boolean>(false);
    constructor(
        @Inject(CARD_DIALOG_DATA) public data: ShareLinkDialogData,
        private dialogRef: CardDialogRef<ShareLinkDialogData, ShareLinkDialogResult>,
        private nodeService: RestNodeService,
        private translate: TranslateService,
        private toast: Toast,
    ) {
        // Set `isLoading` before `ngOnInit`, so it won't cause a changed-after-checked error.
        this.dialogRef.patchState({ isLoading: true });
    }

    ngOnInit(): void {
        this.setNode(this.data.node);
    }

    _node: Node;
    enabled = false;
    expiry = false;
    passwordEnabled = false;
    passwordString: string;
    _expiryDate: Date;
    passwordAlreadySet: boolean;
    buttons: DialogButton[];
    today = new Date();
    set expiryDate(date: Date) {
        this._expiryDate = date;
        this.setExpiry(true);
    }
    get expiryDate() {
        return this._expiryDate;
    }
    currentShare: NodeShare = {} as NodeShare;
    private currentDate: number;

    async copyClipboard() {
        if (!this.enabled) return;
        await this.loading$
            .pipe(
                filter((v) => !v),
                first(),
            )
            .toPromise();
        try {
            UIHelper.copyToClipboard(this.currentShare.url);
            this.toast.toast('WORKSPACE.SHARE_LINK.COPIED_CLIPBOARD');
        } catch (e) {
            this.toast.error(null, 'WORKSPACE.SHARE_LINK.COPIED_CLIPBOARD_ERROR');
        }
    }

    cancel() {
        this.dialogRef.close();
    }

    setEnabled(value: boolean) {
        if (value) {
            this.createShare();
            //this.updateShare(RestConstants.SHARE_EXPIRY_UNLIMITED);
        } else {
            this.deleteShare();
            this.expiry = false;
            this.passwordEnabled = false;
        }
    }

    private setNode(node: Node) {
        this._node = node;
        this.dialogRef.patchState({ isLoading: true });
        this.nodeService.getNodeShares(node.ref.id, RestConstants.SHARE_LINK).subscribe(
            (data: NodeShare[]) => {
                this._expiryDate = new Date(new Date().getTime() + 3600 * 24 * 1000);
                if (data.length) {
                    this.currentShare = data[0];
                    this.expiry = data[0].expiryDate > 0;
                    this.passwordEnabled = data[0].password;
                    if (this.passwordEnabled) {
                        this.passwordAlreadySet = true;
                    }
                    this.currentDate = data[0].expiryDate;
                    if (this.expiry) {
                        this.expiryDate = new Date(data[0].expiryDate);
                    }
                    if (data[0].expiryDate == 0) {
                        this.enabled = false;
                        this.dialogRef.patchState({ isLoading: false });
                        this.currentShare.url = this.translate.instant(
                            'WORKSPACE.SHARE_LINK.DISABLED',
                        );
                    } else {
                        this.enabled = true;
                        this.dialogRef.patchState({ isLoading: false });
                    }
                } else {
                    this.createShare();
                }
            },
            (error: any) => this.toast.error(error),
        );
    }

    private updateShare(date = this.currentDate) {
        this.currentShare.url = this.translate.instant('LOADING');
        this.loading$.next(true);
        this.nodeService
            .updateNodeShare(
                this._node.ref.id,
                this.currentShare.shareId,
                date,
                this.getPasswordParameter(),
            )
            .subscribe((data: NodeShare) => {
                this.currentShare = data;
                if (date == 0)
                    this.currentShare.url = this.translate.instant('WORKSPACE.SHARE_LINK.DISABLED');
                this.loading$.next(false);
            });
    }

    private getPasswordParameter(): string | null {
        if (this.passwordEnabled && this.passwordString) {
            // Set or update the password
            return this.passwordString;
        } else if (this.passwordEnabled && this.passwordAlreadySet) {
            // Keep the password that was already set
            return null;
        } else {
            // Remove the password
            return '';
        }
    }

    setExpiry(value: boolean) {
        if (!this.enabled) return;
        this.currentDate = value
            ? DateHelper.getDateFromDatepicker(this.expiryDate).getTime()
            : RestConstants.SHARE_EXPIRY_UNLIMITED;
        this.updateShare();
    }

    setPassword() {
        if (!this.passwordEnabled) {
            this.passwordString = null;
            this.passwordAlreadySet = false;
        }
        this.updateShare();
    }

    private createShare() {
        this.dialogRef.patchState({ isLoading: true });
        this.nodeService.createNodeShare(this._node.ref.id).subscribe(
            (data: NodeShare) => {
                this.passwordAlreadySet = false;
                this.currentShare = data;
                this.dialogRef.patchState({ isLoading: false });
                this.enabled = true;
            },
            (error: any) => this.toast.error(error),
        );
    }

    private deleteShare() {
        this.dialogRef.patchState({ isLoading: true });
        this.nodeService
            .deleteNodeShare(this._node.ref.id, this.currentShare.shareId)
            .subscribe(() => {
                (this.currentShare as any) = {};
                this.dialogRef.patchState({ isLoading: false });
            });
    }
}
