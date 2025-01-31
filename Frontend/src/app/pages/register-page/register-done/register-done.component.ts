import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { ConfigurationService, RestLocatorService } from '../../../core-module/core.module';
import { Toast } from '../../../services/toast';
import { PlatformLocation } from '@angular/common';
import { Router } from '@angular/router';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { CordovaService } from '../../../services/cordova.service';
import { RegisterService } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-register-done',
    templateUrl: 'register-done.component.html',
    styleUrls: ['register-done.component.scss'],
})
export class RegisterDoneComponent {
    @Output() modify = new EventEmitter<void>();
    @Output() stateChanged = new EventEmitter<void>();
    @Input() inputState: string;
    loading = false;
    email = '';
    keyInput = '';
    private _keyUrl = '';
    private static STATUS_INTERVAL = 5000;
    private activated = false;
    get keyUrl() {
        return this._keyUrl;
    }
    set keyUrl(keyUrl: string) {
        this._keyUrl = keyUrl;
        this.loading = true;
        this.activate(keyUrl);
    }

    public editMail() {
        this.modify.emit();
    }
    public sendMail() {
        this.loading = true;
        this.changes.detectChanges();
        if (this.inputState == 'done') {
            this.register.resendMail(this.email).subscribe(
                () => {
                    this.toast.toast('REGISTER.TOAST');
                    this.loading = false;
                    this.changes.detectChanges();
                },
                (error) => {
                    this.loading = false;
                    this.changes.detectChanges();
                },
            );
        } else {
            this.register.recoverPassword(this.email).subscribe(
                () => {
                    this.loading = false;
                    this.changes.detectChanges();
                },
                (error) => {
                    this.loading = false;
                    this.changes.detectChanges();
                },
            );
        }
    }
    constructor(
        private toast: Toast,
        private register: RegisterService,
        private platformLocation: PlatformLocation,
        private config: ConfigurationService,
        private cordova: CordovaService,
        private changes: ChangeDetectorRef,
        private locator: RestLocatorService,
        private router: Router,
    ) {
        setTimeout(() => this.checkStatus(), RegisterDoneComponent.STATUS_INTERVAL);
    }

    // loop and check if the user has already registered in another tab
    private checkStatus() {
        if (this.activated) {
            return;
        }
        if (this.inputState != 'done') {
            setTimeout(() => this.checkStatus(), RegisterDoneComponent.STATUS_INTERVAL);
            return;
        }
        this.register.mailExists(this.email).subscribe(
            (status) => {
                if (status.exists) {
                    void this.router.navigate([UIConstants.ROUTER_PREFIX, 'login'], {
                        queryParams: { username: this.email },
                    });
                    return;
                }
                setTimeout(() => this.checkStatus(), RegisterDoneComponent.STATUS_INTERVAL);
            },
            (error) => {
                setTimeout(() => this.checkStatus(), RegisterDoneComponent.STATUS_INTERVAL);
            },
        );
    }
    public activate(keyUrl: string) {
        this.loading = true;
        this.changes.detectChanges();
        if (this.inputState == 'done') {
            this.register.activate(keyUrl).subscribe(
                () => {
                    this.activated = true;
                    if (this.cordova.isRunningCordova()) {
                        this.locator.createOAuthFromSession().subscribe(
                            () => {
                                UIHelper.goToDefaultLocation(
                                    this.router,
                                    this.platformLocation,
                                    this.config,
                                );
                            },
                            (error) => {
                                this.toast.error(error);
                            },
                        );
                    } else {
                        UIHelper.goToDefaultLocation(
                            this.router,
                            this.platformLocation,
                            this.config,
                        );
                    }
                },
                (error) => {
                    if (error?.error?.error.includes('DAOInvalidKeyException')) {
                        this.toast.error(null, 'REGISTER.TOAST_INVALID_KEY');
                    } else {
                        this.toast.error(error);
                    }
                    this.loading = false;
                    this.changes.detectChanges();
                },
            );
        } else {
            void this.router.navigate([
                UIConstants.ROUTER_PREFIX,
                'register',
                'reset-password',
                this.keyInput,
            ]);
        }
    }
}
