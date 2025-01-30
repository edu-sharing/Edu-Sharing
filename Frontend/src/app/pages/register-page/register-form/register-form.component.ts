import { Component, EventEmitter, OnDestroy, Output } from '@angular/core';
import { Toast } from '../../../services/toast';
import { Router } from '@angular/router';
import { ConfigurationService, RegisterInformation } from '../../../core-module/core.module';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { PlatformLocation } from '@angular/common';
import {
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators,
} from '@angular/forms';
import { ReplaySubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { VCard } from 'ngx-edu-sharing-ui';
import { RegisterService } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-register-form',
    templateUrl: 'register-form.component.html',
    styleUrls: ['register-form.component.scss'],
})
export class RegisterFormComponent implements OnDestroy {
    @Output() registerDone = new EventEmitter<void>();
    @Output() stateChanged = new EventEmitter<void>();
    password: string;
    registerForm: UntypedFormGroup;
    private destroyed$: ReplaySubject<void> = new ReplaySubject(1);
    info: RegisterInformation & { vcard: string };
    public news = true;
    public agree = false;
    public privacyUrl: string;
    requiredFields: string[] = [];

    public register() {
        this.toast.showProgressSpinner();
        const rawData = this.registerForm.getRawValue();
        this.info = rawData;
        // wrap additional fields (at the moment only "title") into an additional vcard
        const vcard = new VCard();
        vcard.title = rawData.title;
        this.info.vcard = vcard.toVCardString();
        delete (this.info as any).title;

        delete (this.info as any).agree;
        this.info.password = this.password;
        this.registerService.register(this.info).subscribe(
            () => {
                this.toast.closeProgressSpinner();
                this.registerDone.emit();
                // this.toast.toast("REGISTER.TOAST");
            },
            (error) => {
                if (UIHelper.errorContains(error, 'DuplicateAuthorityException')) {
                    this.registerForm.setErrors({ incorrect: true });
                    this.toast.error(null, 'REGISTER.TOAST_DUPLICATE');
                } else {
                    this.toast.error(error);
                }
                this.toast.closeProgressSpinner();
            },
        );
    }

    public openPrivacy() {
        window.open(this.privacyUrl);
    }

    public canRegister() {
        return (
            this.registerForm.valid &&
            this.password &&
            UIHelper.getPasswordStrengthString(this.password) !== 'weak'
        );
    }

    constructor(
        private toast: Toast,
        private platformLocation: PlatformLocation,
        private formBuilder: UntypedFormBuilder,
        private router: Router,
        private registerService: RegisterService,
        private translations: TranslationsService,
        private configService: ConfigurationService,
    ) {
        this.translations.waitForInit().subscribe(() => {
            this.privacyUrl = this.configService.instant('privacyInformationUrl');
            this.requiredFields = this.configService.instant('register.requiredFields', [
                'firstName',
            ]);

            this.registerForm = this.formBuilder.group({
                title: new UntypedFormControl(
                    '',
                    this.requiredFields.includes('title') ? [Validators.required] : null,
                ),
                firstName: new UntypedFormControl(
                    '',
                    this.requiredFields.includes('firstName') ? [Validators.required] : null,
                ),
                lastName: new UntypedFormControl(
                    '',
                    this.requiredFields.includes('lastName') ? [Validators.required] : null,
                ),
                organization: new UntypedFormControl(
                    '',
                    this.requiredFields.includes('organization') ? [Validators.required] : null,
                ),
                email: new UntypedFormControl('', [Validators.required, Validators.email]),
                agree: new UntypedFormControl(false, [Validators.requiredTrue]),
                allowNotifications: new UntypedFormControl(false),
            });
            this.registerForm.statusChanges
                .pipe(takeUntil(this.destroyed$))
                .subscribe(() => this.stateChanged.emit());
        });
    }
    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
