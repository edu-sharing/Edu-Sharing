import { Component, Inject, OnInit } from '@angular/core';
import {
    DialogButton,
    Group,
    LoginResult,
    RestConnectorService,
    RestConstants,
    RestIamService,
} from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { JoinGroupDialogData, JoinGroupDialogResult } from './join-group-dialog-data';

enum Step {
    selectGroup,
    confirmGroup,
}

@Component({
    selector: 'es-join-group-dialog',
    templateUrl: './join-group-dialog.component.html',
    styleUrls: ['./join-group-dialog.component.scss'],
})
export class JoinGroupDialogComponent implements OnInit {
    readonly STEP = Step;
    dialogStep: Step = Step.selectGroup;
    group: Group;
    groups: Group[];
    password = '';
    groupsLoading = true;
    userGroups: Group[];

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: JoinGroupDialogData,
        private dialogRef: CardDialogRef<JoinGroupDialogData, JoinGroupDialogResult>,
        private connector: RestConnectorService,
        private iam: RestIamService,
        private toast: Toast,
    ) {
        this.updateButtons();
        this.connector.isLoggedIn(false).subscribe((data: LoginResult) => {
            const request = {
                count: RestConstants.COUNT_UNLIMITED,
            };
            this.iam.searchGroups('*', true, '', '*', request).subscribe(
                (groups) => {
                    this.groups = groups.groups;
                    this.groupsLoading = false;
                },
                (error) => {
                    this.toast.error(error);
                    this.groupsLoading = false;
                    this.toast.closeProgressSpinner();
                },
            );
        });
    }

    async ngOnInit() {
        this.userGroups = (await this.iam.getUserGroups().toPromise()).groups;
    }

    private cancel(): void {
        this.dialogRef.close(null);
    }

    updateButtons() {
        let buttons: DialogButton[];
        if (this.dialogStep === Step.selectGroup) {
            buttons = DialogButton.getNextCancel(
                () => this.cancel(),
                () => {
                    this.dialogStep = Step.confirmGroup;
                    this.updateButtons();
                },
            );
            buttons[1].disabled = !this.group || this.isMemberOf(this.group);
        }
        if (this.dialogStep === Step.confirmGroup) {
            const back = new DialogButton('BACK', { color: 'standard' }, () => {
                this.dialogStep = Step.selectGroup;
                this.updateButtons();
            });
            const signup = new DialogButton('SIGNUP_GROUP.SIGNUP', { color: 'primary' }, () =>
                this.signup(),
            );
            signup.disabled = this.group.signupMethod === 'password' && !this.password;
            buttons = [back, signup];
        }
        this.dialogRef.patchConfig({ buttons });
    }

    private signup(): void {
        this.toast.showProgressSpinner();
        this.iam.signupGroup(this.group.authorityName, this.password).subscribe(
            (result) => {
                if (result !== 'Ok') {
                    this.toast.error(null, 'SIGNUP_GROUP.TOAST.' + result);
                } else {
                    if (this.group.signupMethod === 'list') {
                        this.toast.toast('SIGNUP_GROUP.TOAST.ADDED_TO_LIST');
                    } else {
                        this.toast.toast('SIGNUP_GROUP.TOAST.ADDED_TO_GROUP', {
                            group: this.group.profile.displayName,
                        });
                    }
                    this.cancel();
                }
                this.toast.closeProgressSpinner();
            },
            (error) => {
                if (error.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
                    this.toast.error(null, 'SIGNUP_GROUP.TOAST.AlreadyMember');
                } else {
                    this.toast.error(error);
                }
                this.toast.closeProgressSpinner();
            },
        );
    }

    select(group: Group) {
        this.group = group;
        this.updateButtons();
        this.dialogRef.patchConfig({ closable: Closable.Standard });
    }

    isMemberOf(group: Group) {
        return !!this.userGroups?.find((g) => g.authorityName === group.authorityName);
    }
}
