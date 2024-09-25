import { Location } from '@angular/common';
import { HttpRequest } from '@angular/common/http';
import { Injectable, Injector } from '@angular/core';
import { type ApiErrorResponse } from 'ngx-edu-sharing-api';
import { RestConstants } from '../core-module/core.module';
import { DialogsService } from '../features/dialogs/dialogs.service';
import { CordovaService } from '../services/cordova.service';
import { Toast } from '../services/toast';

@Injectable({
    providedIn: 'root',
})
export class ErrorHandlerService {
    private toast: Toast;
    private dialogs: DialogsService;

    constructor(private injector: Injector, private location: Location) {
        // Prevent circular dependency.
        Promise.resolve().then(() => {
            this.toast = this.injector.get(Toast);
            this.dialogs = this.injector.get(DialogsService);
        });
    }

    async handleError(error: ApiErrorResponse, req: HttpRequest<unknown>) {
        if (error?.error?.type === 'abort' || (!!(window as any).safari && error.status === 0)) {
            // this might cause by explicit abort and a redirect
            // do nothing to prevent firefox from showing a temporary message i.e. on login sso redirect
            console.warn('Explicitly aborted, do not displaying a message', error);
        } else if (req.method === 'GET' && req.url === this.getApiUrl('/config/v1/values')) {
            this.showConfigurationErrorNotice(error);
        } else if (
            req.method === 'GET' &&
            (req.url === this.getApiUrl('/authentication/v1/hasAccessToScope') ||
                req.url === this.getApiUrl('/authentication/v1/validateSession') ||
                req.url === this.getApiUrl('/iam/v1/people/-home-/-me-'))
        ) {
            this.showReloadNotice();
        } else if (this.injector.get(CordovaService).isRunningCordova()) {
            console.info('corodva api error received', error);
            if (error.status === RestConstants.HTTP_UNAUTHORIZED) {
                await this.injector.get(CordovaService).handleAppReAuthentication(true);
            }
        } else {
            console.warn('handleError has triggered', error);
            this.toast.error(error);
        }
    }

    private getApiUrl(path: string): string {
        return this.location.prepareExternalUrl('/rest' + path);
    }

    private async showReloadNotice() {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'API_ERROR.RELOAD_NOTICE_TITLE',
            message: 'API_ERROR.RELOAD_NOTICE',
            buttons: [{ label: 'API_ERROR.RELOAD_BUTTON_LABEL', config: { color: 'primary' } }],
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'API_ERROR.RELOAD_BUTTON_LABEL') {
                location.reload();
            }
        });
    }

    private showConfigurationErrorNotice(error: ApiErrorResponse) {
        // Use a toast instead of a modal dialog because the configuration is fetched at an early
        // stage of the application and a loading spinner will probably occlude a modal dialog.
        this.toast.error(
            error,
            'Error fetching configuration data. Please contact administrator.\n' +
                'Fehler beim Abrufen der Konfigurationsdaten. Bitte Administrator kontaktieren.',
        );
    }
}
