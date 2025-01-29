import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Toast } from '../../../services/toast';
import { Params, Router } from '@angular/router';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { RegisterService } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-register-reset-password',
    templateUrl: 'register-reset-password.component.html',
    styleUrls: ['register-reset-password.component.scss'],
})
export class RegisterResetPasswordComponent {
    @Output() onStateChanged = new EventEmitter<void>();
    public new_password = '';
    @Input() params: Params;
    public buttonCheck() {
        if (
            UIHelper.getPasswordStrengthString(this.new_password) != 'weak' &&
            this.new_password.trim()
        ) {
            return true;
        } else {
            return false;
        }
    }
    public newPassword() {
        this.toast.showProgressSpinner();
        this.register.resetPassword(this.params.key, this.new_password).subscribe(
            () => {
                this.toast.closeProgressSpinner();
                this.toast.toast('REGISTER.RESET.TOAST');
                void this.router.navigate([UIConstants.ROUTER_PREFIX, 'login']);
            },
            (error) => {
                console.log('error', error);
                this.toast.closeProgressSpinner();
                if (error?.error?.error?.includes('DAOInvalidKeyException')) {
                    this.toast.error(null, 'REGISTER.TOAST_INVALID_RESET_KEY');
                    void this.router.navigate([UIConstants.ROUTER_PREFIX, 'register', 'request']);
                } else {
                    this.toast.error(error);
                }
            },
        );
    }
    constructor(private toast: Toast, private register: RegisterService, private router: Router) {}
}
