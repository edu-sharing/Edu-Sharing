import { AfterViewInit, ApplicationRef, Component, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { User } from 'ngx-edu-sharing-api';
import {
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
} from 'ngx-edu-sharing-ui';
import {
    DeleteMode,
    DialogButton,
    Group,
    JobDescription,
    RestAdminService,
    RestConstants,
    RestIamService,
    SessionStorageService,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { Toast } from '../../../services/toast';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { AuthoritySearchMode } from '../../../shared/components/authority-search-input/authority-search-input.component';
import { AuthorityNamePipe } from '../../../shared/pipes/authority-name.pipe';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'es-permissions-delete',
    templateUrl: 'delete.component.html',
    styleUrls: ['delete.component.scss'],
})
export class PermissionsDeleteComponent implements OnInit, AfterViewInit {
    readonly DisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly AuthoritySearchMode = AuthoritySearchMode;
    @ViewChild(NodeEntriesWrapperComponent)
    nodeEntriesWrapperComponent: NodeEntriesWrapperComponent<User>;
    deleteModes = [DeleteMode.none, DeleteMode.assign, DeleteMode.delete];
    deleteModesFolder = [DeleteMode.none, DeleteMode.assign];
    canSubmit$ = new BehaviorSubject<boolean>(false);
    options: any;
    receiver: User;
    receiverGroup: Group;
    usersDataSource = new NodeDataSource<User>();
    columns: ListItem[] = [];
    deleteResult: string;
    deleteButtons: DialogButton[];
    jobs: JobDescription[];
    job: JobDescription | 'NONE' = 'NONE';

    constructor(
        private admin: RestAdminService,
        private dialogs: DialogsService,
        private iam: RestIamService,
        private storage: SessionStorageService,
        private toast: Toast,
        private applicationRef: ApplicationRef,
        private translate: TranslateService,
    ) {
        // send list of target users + options for these specific users
        const defaultOptions = {
            // change this value if the config needs to be reset to default
            version: '1.0.0',
            homeFolder: {
                folders: DeleteMode.none,
                privateFiles: DeleteMode.none,
                ccFiles: DeleteMode.none,
                keepFolderStructure: false,
            },
            sharedFolders: {
                folders: DeleteMode.none,
                privateFiles: DeleteMode.none,
                ccFiles: DeleteMode.none,
            },
            collections: {
                privateCollections: DeleteMode.none,
                publicCollections: DeleteMode.none,
            },
            ratings: {
                delete: false,
            },
            comments: {
                delete: false,
            },
            collectionFeedback: {
                delete: false,
            },
            statistics: {
                delete: false,
            },
            stream: {
                delete: false,
            },
            // change owner + (optional) invite a coordinator group
            // comments, ratings, feedback, stream, statistics
            receiver: '',
            receiverGroup: '',
            // shall the user be found & removed inside contributor metadata
            cleanupMetadata: true,
        };
        this.storage.get('delete_users_options', defaultOptions).subscribe((data: any) => {
            if (data.version === defaultOptions.version) {
                this.options = data;
            } else {
                this.options = defaultOptions;
            }
        });
        this.columns.push(new ListItem('USER', RestConstants.AUTHORITY_NAME));
        this.columns.push(new ListItem('USER', RestConstants.AUTHORITY_FIRSTNAME));
        this.columns.push(new ListItem('USER', RestConstants.AUTHORITY_LASTNAME));
        this.deleteButtons = DialogButton.getOk(() => {
            this.deleteResult = null;
        });
        this.refresh();
    }

    async ngOnInit() {
        this.jobs = (await this.admin.getAllJobs().toPromise()).filter((j) =>
            j.tags?.includes('DeletePersonJob'),
        );
    }

    /**
     * returns a code whether all selected modes seem to be data conform and all user-relevant data will be removed and all options match up
     */
    isValid() {
        return !this.anyModeMatches(DeleteMode.none);
    }

    hasAssigning() {
        return this.anyModeMatches(DeleteMode.assign);
    }

    private anyModeMatches(mode: DeleteMode) {
        return (
            this.options.homeFolder.folders === mode ||
            this.options.homeFolder.privateFiles === mode ||
            this.options.homeFolder.ccFiles === mode ||
            this.options.sharedFolders.folders === mode ||
            this.options.sharedFolders.privateFiles === mode ||
            this.options.sharedFolders.ccFiles === mode ||
            this.options.collections.privateCollections === mode ||
            this.options.collections.publicCollections === mode
        );
    }

    refresh() {
        this.usersDataSource.isLoading = true;
        const request = { maxItems: RestConstants.COUNT_UNLIMITED };
        this.iam.searchUsers('*', true, 'todelete', request).subscribe(
            (users) => {
                this.usersDataSource.setData(users.users as unknown as User[], users.pagination);
                this.applicationRef.tick();
                this.nodeEntriesWrapperComponent
                    ?.getSelection()
                    .changed.subscribe(() => this.canSubmit$.next(this.canSubmit()));
                this.usersDataSource.isLoading = false;
            },
            (error) => {
                this.toast.error(error);
                this.usersDataSource.isLoading = false;
            },
        );
    }

    async prepareStart() {
        let message = this.translate.instant('PERMISSIONS.DELETE.CONFIRM.USERS');
        for (const user of this.nodeEntriesWrapperComponent.getSelection().selected) {
            message += '\n' + new AuthorityNamePipe(this.translate).transform(user, null);
        }
        if (this.hasAssigning()) {
            message +=
                '\n\n' +
                this.translate.instant('PERMISSIONS.DELETE.CONFIRM.RECEIVER', {
                    user: new AuthorityNamePipe(this.translate).transform(this.receiver, null),
                });
            message +=
                '\n\n' +
                this.translate.instant('PERMISSIONS.DELETE.CONFIRM.RECEIVER_GROUP', {
                    group: new AuthorityNamePipe(this.translate).transform(
                        this.receiverGroup,
                        null,
                    ),
                });
        }
        message += '\n\n' + this.translate.instant('PERMISSIONS.DELETE.CONFIRM.FINAL');
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'PERMISSIONS.DELETE.CONFIRM.CAPTION',
            message,
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                { label: 'PERMISSIONS.DELETE.START', config: { color: 'primary' } },
            ],
        });
        dialogRef.afterClosed().subscribe((result) => {
            if (result === 'PERMISSIONS.DELETE.START') {
                this.start();
            }
        });
    }

    start() {
        this.toast.showProgressSpinner();
        if (this.job !== 'NONE') {
            this.admin
                .startJobSync(this.job.name, {
                    authorities: this.nodeEntriesWrapperComponent
                        .getSelection()
                        .selected.map((u) => u.authorityName),
                })
                .subscribe(
                    (result) => {
                        this.toast.closeProgressSpinner();
                        this.deleteResult = JSON.stringify(result, null, 2);
                        this.refresh();
                    },
                    (error) => {
                        this.toast.error(error);
                        this.toast.closeProgressSpinner();
                    },
                );
            return;
        }

        if (this.hasAssigning()) {
            this.options.receiver = this.receiver.authorityName;
            this.options.receiverGroup = this.receiverGroup.authorityName;
        }
        this.storage.set('delete_users_options', this.options);
        const submit = Helper.deepCopy(this.options);
        delete submit.version;
        this.admin
            .deletePersons(
                this.nodeEntriesWrapperComponent
                    .getSelection()
                    .selected.map((u) => u.authorityName),
                submit,
            )
            .subscribe(
                (result) => {
                    this.toast.closeProgressSpinner();
                    this.deleteResult = JSON.stringify(result, null, 2);
                    this.refresh();
                },
                (error) => {
                    this.toast.error(error);
                    this.toast.closeProgressSpinner();
                },
            );
    }

    missingAssigning() {
        return this.hasAssigning() && (this.receiver == null || this.receiverGroup == null);
    }

    private canSubmit() {
        return (
            !this.nodeEntriesWrapperComponent?.getSelection().isEmpty() && !this.missingAssigning()
        );
    }

    allAssigning() {
        return (
            this.options.homeFolder.folders === 'assign' &&
            this.options.homeFolder.privateFiles === 'assign' &&
            this.options.homeFolder.ccFiles === 'assign'
        );
    }

    updateForm() {
        if (!this.allAssigning()) {
            this.options.homeFolder.keepFolderStructure = false;
        }
    }

    ngAfterViewInit() {}
}
