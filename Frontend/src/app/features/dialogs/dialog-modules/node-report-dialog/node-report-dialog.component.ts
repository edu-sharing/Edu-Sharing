import { trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Inject,
    OnInit,
    ViewChild,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { filter, first, take } from 'rxjs/operators';
import {
    DialogButton,
    LoginResult,
    Node,
    RestConnectorService,
    RestIamService,
    RestNodeService,
} from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { UIAnimation } from 'ngx-edu-sharing-ui';
import {
    AuthenticationService,
    HOME_REPOSITORY,
    NodeServiceUnwrapped,
    UserService,
} from 'ngx-edu-sharing-api';
import { forkJoin } from 'rxjs';

export interface NodeReportDialogData {
    node: Node;
    mode: 'NODE_REPORT' | 'REVOKE_FEEDBACK';
    showOptions: boolean;
}

@Component({
    selector: 'es-node-report-dialog',
    templateUrl: 'node-report-dialog.component.html',
    styleUrls: ['node-report-dialog.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeReportDialogComponent implements OnInit {
    readonly reasons = ['UNAVAILABLE', 'INAPPROPRIATE_CONTENT', 'INVALID_METADATA', 'OTHER'];
    @ViewChild('formElement') formRef: ElementRef<HTMLFormElement>;

    readonly form = new UntypedFormGroup({
        reason: new UntypedFormControl(''),
        comment: new UntypedFormControl(''),
        email: new UntypedFormControl('', [Validators.email, Validators.required]),
    });

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: NodeReportDialogData,
        private dialogRef: CardDialogRef,
        private authenticationService: AuthenticationService,
        private userService: UserService,
        private translate: TranslateService,
        private toast: Toast,
        private nodeApi: NodeServiceUnwrapped,
        private cdr: ChangeDetectorRef,
    ) {
        this.form.get('comment').clearValidators();
        this.form.get('reason').clearValidators();
        if (!data.showOptions) {
            this.form.get('comment').addValidators(Validators.required);
        } else {
            this.form.get('reason').addValidators(Validators.required);
        }
    }

    ngOnInit(): void {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
                new DialogButton('NODE_REPORT.REPORT', { color: 'primary' }, () => this.report()),
            ],
        });
        // Pre-fill the email field for logged-in users.
        forkJoin(
            this.authenticationService.observeLoginInfo().pipe(first()),
            this.userService.observeCurrentUser().pipe(first()),
        ).subscribe(([login, user]) => {
            if (!login.isGuest && !!user?.person?.profile?.email) {
                this.form.get('email').disable();
                this.form.patchValue({ email: user.person.profile.email });
            }
        });
        // Disable close by backdrop click as soon as the user enters any value .
        this.form.valueChanges
            .pipe(
                filter((values) => Object.values(values).some((value) => !!value)),
                take(1),
            )
            .subscribe(() => this.dialogRef.patchConfig({ closable: Closable.Standard }));
    }

    cancel() {
        this.dialogRef.close();
    }

    shouldShowError(field: string) {
        const fieldControl = this.form.get(field);
        return !fieldControl.disabled && fieldControl.touched && !fieldControl.valid;
    }

    report() {
        if (this.form.valid) {
            // Include value for possibly disabled email field.
            const value = this.form.getRawValue();
            this.setLoading(true);
            this.nodeApi
                .reportNode({
                    repository: HOME_REPOSITORY,
                    node: this.data.node.ref.id,
                    mode: this.data.mode === 'REVOKE_FEEDBACK' ? 'Feedback' : 'ReportProblem',
                    reason: this.getReasonAsString(value.reason),
                    userEmail: value.email,
                    userComment: value.comment,
                })
                .subscribe(
                    () => {
                        this.toast.toast(this.data.mode + '.DONE');
                        this.dialogRef.close();
                    },
                    (error: any) => {
                        this.setLoading(false);
                        this.toast.error(error);
                    },
                );
        } else {
            for (const field of ['reason', 'email']) {
                const control = this.form.get(field);
                if (!control.valid) {
                    control.markAsTouched();
                    this.focusField(field);
                    this.cdr.detectChanges();
                    break;
                }
            }
        }
    }

    setLoading(isLoading: boolean): void {
        this.dialogRef.patchState({ isLoading });
        if (isLoading) {
            this.form.disable();
        } else {
            this.form.enable();
        }
    }

    private getReasonAsString(reason: string) {
        return `${this.translate.instant('NODE_REPORT.REASONS.' + reason)} (${reason})`;
    }

    private focusField(field: string) {
        const form = this.formRef.nativeElement;
        const element = form.elements.namedItem(field);
        if (element instanceof HTMLElement) {
            element.focus();
        } else if (element instanceof RadioNodeList) {
            (element[0] as HTMLElement).focus();
        }
    }
}
