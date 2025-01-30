import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { LTIRegistrationToken, LTIRegistrationTokens } from '../../../core-module/rest/data-object';
import { RestLtiService } from '../../../core-module/rest/services/rest-lti.service';
import { Toast } from '../../../services/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { DELETE_OR_CANCEL } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';

@Component({
    selector: 'es-lti-admin',
    templateUrl: './lti-admin.component.html',
    styleUrls: ['./lti-admin.component.scss'],
})
export class LtiAdminComponent implements OnInit {
    @Output() refreshAppList = new EventEmitter<void>();

    /**
     * dynamic
     */
    tokens: LTIRegistrationTokens;
    displayedColumns: string[] = ['url', 'tsExpiry', 'registeredAppId', 'copy', 'delete'];

    /**
     * advanced
     *
     */
    platformId: string;
    clientId: string;
    deploymentId: string;
    authenticationRequestUrl: string;
    keysetUrl: string;
    keyId: string;
    authTokenUrl: string;

    constructor(
        private dialogs: DialogsService,
        private ltiService: RestLtiService,
        private toast: Toast,
    ) {}

    ngOnInit(): void {
        this.refresh();
    }

    async remove(element: LTIRegistrationToken): Promise<void> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'ADMIN.LTI.REMOVE_TITLE',
            message: 'ADMIN.LTI.REMOVE_MESSAGE',
            messageParameters: element as { [key: string]: any },
            buttons: DELETE_OR_CANCEL,
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'YES_DELETE') {
                this.ltiService.removeToken(element.token).subscribe(() => {
                    this.refresh();
                });
            }
        });
    }

    refresh() {
        this.ltiService.getTokensCall(false).subscribe((t: LTIRegistrationTokens) => {
            this.tokens = t;
        });
    }

    generate() {
        this.ltiService.getTokensCall(true).subscribe((t: LTIRegistrationTokens) => {
            this.tokens = t;
            this.copyUrl(this.tokens.registrationLinks[this.tokens.registrationLinks.length - 1]);
        });
    }

    saveAdvanced() {
        this.ltiService
            .registrationAdvanced(
                this.platformId,
                this.clientId,
                this.deploymentId,
                this.authenticationRequestUrl,
                this.keysetUrl,
                this.keyId,
                this.authTokenUrl,
            )
            .subscribe(
                (t: void) => {
                    this.toast.toast('ADMIN.LTI.DATA.CREATED', null);
                    this.toast.closeProgressSpinner();
                    this.refreshAppList.emit();
                },
                (error: any) => {
                    this.toast.error(error);
                    this.toast.closeProgressSpinner();
                },
            );
    }

    copyUrl(element: LTIRegistrationToken) {
        UIHelper.copyToClipboard(element.url);
        this.toast.toast('ADMIN.APPLICATIONS.COPIED_CLIPBOARD');
    }
}
