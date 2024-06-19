import { PlatformLocation } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    ViewChild,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Params, Router, UrlSerializer } from '@angular/router';
import { TranslationsService, UIConstants } from 'ngx-edu-sharing-ui';
import {
    ConfigurationService,
    DialogButton,
    RestConnectorService,
    RestHelper,
    UIService,
} from '../../core-module/core.module';
import { Toast } from '../../services/toast';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { RegisterDoneComponent } from './register-done/register-done.component';
import { RegisterFormComponent } from './register-form/register-form.component';
import { RegisterRequestComponent } from './register-request/register-request.component';
import { RegisterResetPasswordComponent } from './register-reset-password/register-reset-password.component';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { Subject } from 'rxjs';

@Component({
    selector: 'es-register-page',
    templateUrl: 'register-page.component.html',
    styleUrls: ['register-page.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPageComponent implements OnInit, OnDestroy {
    @ViewChild('registerForm') registerForm: RegisterFormComponent;
    @ViewChild('registerDone') registerDone: RegisterDoneComponent;
    @ViewChild('request') request: RegisterRequestComponent;
    @ViewChild('resetPassword') resetPassword: RegisterResetPasswordComponent;
    state: 'register' | 'request' | 'reset-password' | 'done' | 'done-reset' = 'register';
    buttons: DialogButton[];
    params: Params;
    private destroyed = new Subject<void>();

    public cancel() {
        RestHelper.goToLogin(this.router, this.configService, null, null);
    }

    public requestDone(email: string) {
        this.request.submit();
    }
    public linkRegister() {
        this.router.navigate([UIConstants.ROUTER_PREFIX + 'register']);
    }
    public newPassword() {
        this.resetPassword.newPassword();
    }

    constructor(
        private connector: RestConnectorService,
        private toast: Toast,
        private platformLocation: PlatformLocation,
        private router: Router,
        private translations: TranslationsService,
        private uiService: UIService,
        private configService: ConfigurationService,
        private changes: ChangeDetectorRef,
        private loadingScreen: LoadingScreenService,
        private route: ActivatedRoute,
    ) {}

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    ngOnInit() {
        const loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed });
        this.toast.showProgressSpinner();
        this.updateButtons();
        this.route.params.subscribe((params) => {
            this.params = params;
            if (params.status) {
                if (
                    params.status === 'done' ||
                    params.status === 'done-reset' ||
                    params.status === 'request' ||
                    params.status === 'reset-password'
                ) {
                    this.state = params.status;
                    this.changes.detectChanges();
                } else {
                    this.router.navigate([UIConstants.ROUTER_PREFIX, 'register']);
                }
            }
        });

        this.translations.waitForInit().subscribe(() => {
            loadingTask.done();
            this.toast.closeProgressSpinner();
            console.log('done');
            if (['request', 'reset-password', 'done-reset'].indexOf(this.params.status) !== -1) {
                if (
                    this.configService.instant('register.local', true as boolean) === false &&
                    this.configService.instant('register.recoverPassword', false) === false
                ) {
                    RestHelper.goToLogin(this.router, this.configService, null, null);
                }
            } else if (!this.configService.instant('register.local', true)) {
                RestHelper.goToLogin(this.router, this.configService, null, null);
            }
            setTimeout(() => this.setParams());
            this.connector.isLoggedIn().subscribe((data) => {
                if (data.statusCode === 'OK') {
                    UIHelper.goToDefaultLocation(
                        this.router,
                        this.platformLocation,
                        this.configService,
                    );
                }
            });
        });
    }

    onRegisterDone() {
        const email = this.registerForm.info.email;
        this.state = 'done';
        this.changes.detectChanges();
        this.updateButtons();
        // will lose state when going back to register form
        // this.router.navigate([UIConstants.ROUTER_PREFIX,"register","done","-",email]);
        this.uiService.waitForComponent(this, 'registerDone').subscribe(() => {
            this.registerDone.email = email;
            this.changes.detectChanges();
            this.updateButtons();
        });
        this.toast.toast('REGISTER.TOAST');
    }

    private setParams() {
        this.route.params.subscribe((params) => {
            this.params = params;
            if (params.email) {
                this.registerDone.email = params.email;
            }
            if (params.key) {
                if (this.registerDone) {
                    this.registerDone.keyUrl = params.key;
                }
            }
        });
    }

    modifyData() {
        if (this.state === 'done') {
            this.state = 'register';
        } else {
            this.state = 'request';
        }
        this.updateButtons();
    }

    onPasswordRequested() {
        const email = this.request.emailFormControl.value;
        this.state = 'done-reset';
        setTimeout(() => (this.registerDone.email = email));
    }

    updateButtons() {
        const primaryButton = this.getPrimaryButton();
        const cancelButton = new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel());
        this.buttons = [cancelButton];
        if (primaryButton) {
            this.buttons.push(primaryButton);
        }
        return this.buttons;
    }

    private getPrimaryButton(): DialogButton {
        let btn: DialogButton;
        if (this.state === 'register') {
            btn = new DialogButton('REGISTER.BUTTON', { color: 'primary' }, () =>
                this.registerForm.register(),
            );
            btn.disabled = !this.registerForm || !this.registerForm.canRegister();
        }
        if (this.state === 'request') {
            btn = new DialogButton('REGISTER.REQUEST.BUTTON', { color: 'primary' }, () => {
                this.requestDone(this.request.emailFormControl.value);
            });
            btn.disabled = !this.request || !this.request.emailFormControl.valid;
        }
        if (this.state === 'reset-password') {
            btn = new DialogButton('REGISTER.RESET.BUTTON', { color: 'primary' }, () =>
                this.newPassword(),
            );
            btn.disabled = !this.resetPassword || !this.resetPassword.buttonCheck();
        }
        if ((this.state === 'done' || this.state === 'done-reset') && this.registerDone) {
            btn = new DialogButton(
                this.state === 'done' ? 'REGISTER.DONE.ACTIVATE' : 'NEXT',
                { color: 'primary' },
                () => this.registerDone.activate(this.registerDone.keyInput),
            );
            btn.disabled = !this.registerDone || !this.registerDone.keyInput.trim();
        }
        return btn;
    }

    canRegister() {
        return this.configService.instant('register.local', true);
    }
}
