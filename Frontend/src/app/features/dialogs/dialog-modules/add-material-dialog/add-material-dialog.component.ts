import { trigger } from '@angular/animations';
import { Component, ElementRef, Inject, OnInit, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import {
    ClientutilsV1Service,
    Node,
    RestConstants,
    UserQuota,
    UserService,
    WebsiteInformation,
} from 'ngx-edu-sharing-api';
import { ListItem, UIAnimation, notNull } from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { catchError, debounce, filter, finalize, map, switchMap, tap } from 'rxjs/operators';
import {
    ConfigurationService,
    DialogButton,
    ParentList,
    RestNodeService,
    SessionStorageService,
} from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { BreadcrumbsService } from '../../../../shared/components/breadcrumbs/breadcrumbs.service';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { DialogsService } from '../../dialogs.service';
import { AddMaterialDialogData, AddMaterialDialogResult } from './add-material-dialog-data';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'es-add-material-dialog',
    templateUrl: './add-material-dialog.component.html',
    styleUrls: ['./add-material-dialog.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
        trigger('openOverlay', UIAnimation.openOverlay()),
    ],
    providers: [BreadcrumbsService],
})
export class AddMaterialDialogComponent implements OnInit {
    @ViewChild('fileSelect') private file: ElementRef;

    private disabled = true;

    protected readonly linkControl = new FormControl('');
    protected showSaveParent = false;
    protected saveParent = false;
    protected breadcrumbs: {
        homeLabel: string;
        homeIcon: string;
    };
    protected ltiEnabled: boolean;
    protected ltiActivated: boolean;
    protected ltiConsumerKey: string;
    protected ltiSharedSecret: string;
    protected userQuota: UserQuota;
    protected websiteInformation: WebsiteInformation;
    protected hideFileUpload = false;
    protected isFileOver = false;
    protected loadingWebsiteInformation = false;
    protected columns = [
        new ListItem('NODE', RestConstants.LOM_PROP_TITLE),
        new ListItem('NODE', RestConstants.CM_PROP_C_CREATED),
    ];
    protected parent$ = new BehaviorSubject<Node>(null);

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: AddMaterialDialogData,
        private dialogRef: CardDialogRef<AddMaterialDialogData, AddMaterialDialogResult>,
        private breadcrumbsService: BreadcrumbsService,
        private clientUtils: ClientutilsV1Service,
        private configService: ConfigurationService,
        private dialogs: DialogsService,
        private nodeService: RestNodeService,
        private storageService: SessionStorageService,
        private toast: Toast,
        private userService: UserService,
    ) {
        this.registerParentListener();
        this.parent$.next(this.data.parent);
        this.setState('');
        this.userService
            .observeCurrentUser()
            .pipe(takeUntilDestroyed())
            .subscribe((user) => (this.userQuota = user?.person.quota));
        this.configService
            .get('upload.lti.enabled', false)
            .subscribe((ltiEnabled) => (this.ltiEnabled = ltiEnabled));
    }

    private registerParentListener() {
        this.parent$
            .pipe(
                takeUntilDestroyed(),
                filter((p) => !!p),
                tap((p) => console.log(p)),
            )
            .subscribe((parent) => {
                this.breadcrumbs = null;
                this.breadcrumbsService.setNodePath(null);
                this.getBreadcrumbs(parent)
                    .pipe(filter(notNull))
                    .subscribe((breadcrumbs) => {
                        this.breadcrumbs = breadcrumbs;
                        this.breadcrumbsService.setNodePath(breadcrumbs.nodes);
                    });
            });
    }

    ngOnInit(): void {
        this.registerLink();
    }

    private registerLink(): void {
        this.linkControl.valueChanges
            .pipe(
                // Don't let the user submit the link until we fetched website information.
                tap(() => this.setState('')),
                map((url) => getValidHttpUrl(url)),
                debounce((url) => (url ? rxjs.timer(500) : rxjs.timer(0))),
                tap(() => {
                    this.loadingWebsiteInformation = true;
                    this.websiteInformation = null;
                }),
                switchMap((url) =>
                    url ? this.clientUtils.getWebsiteInformation({ url }) : rxjs.of(null),
                ),
                finalize(() => (this.loadingWebsiteInformation = false)),
            )
            .subscribe({
                next: (websiteInformation) => {
                    this.loadingWebsiteInformation = false;
                    this.websiteInformation = websiteInformation;
                    if (websiteInformation) {
                        this.setState(this.linkControl.value);
                    }
                    this.updateHideFileUpload();
                },
                error: () => {
                    this.loadingWebsiteInformation = false;
                },
            });
    }

    private updateHideFileUpload(): void {
        if (this.hideFileUpload && !this.linkControl.value.trim()) {
            this.hideFileUpload = false;
        } else if (!this.hideFileUpload && this.websiteInformation) {
            this.hideFileUpload = true;
        }
    }

    cancel() {
        this.dialogRef.close(null);
    }

    selectFile() {
        this.file.nativeElement.click();
    }

    /**
     * Closes the dialog and returns the given file list to the caller.
     */
    closeWithFiles(fileList: FileList) {
        this.dialogRef.close({
            kind: 'file',
            files: fileList,
            parent: this.parent$.value,
        });
    }

    setLink() {
        if (this.disabled) {
            // To nothing
        } else if (this.ltiActivated && (!this.ltiConsumerKey || !this.ltiSharedSecret)) {
            const params = {
                link: {
                    caption: 'WORKSPACE.TOAST.LTI_FIELDS_REQUIRED_LINK',
                    callback: () => {
                        this.ltiActivated = false;
                        this.setLink();
                    },
                },
            };
            this.toast.error(null, 'WORKSPACE.TOAST.LTI_FIELDS_REQUIRED', null, null, null, params);
        } else {
            this.closeWithLink();
        }
    }

    private closeWithLink(): void {
        this.dialogRef.close({
            kind: 'link',
            link: this.linkControl.value,
            parent: this.parent$.value,
            lti: this.ltiActivated
                ? {
                      consumerKey: this.ltiConsumerKey,
                      sharedSecret: this.ltiSharedSecret,
                  }
                : null,
        });
    }

    setState(link: string) {
        link = link.trim();
        this.disabled = !link;
        this.updateButtons();
        this.dialogRef.patchConfig({ closable: Closable.Standard });
    }

    async chooseParent() {
        const dialogRef = await this.dialogs.openFileChooserDialog({
            pickDirectory: true,
            title: 'WORKSPACE.CHOOSE_LOCATION_TITLE',
            subtitle: 'WORKSPACE.CHOOSE_LOCATION_DESCRIPTION',
        });
        dialogRef.afterClosed().subscribe((nodes) => {
            if (nodes) {
                this.parentSelected(nodes);
            }
        });
    }

    parentSelected(event: Node[]) {
        this.showSaveParent = true;
        this.parent$.next(event[0]);
        this.updateButtons();
        this.dialogRef.patchConfig({ closable: Closable.Standard });
    }

    updateButtons() {
        const [okButton] = DialogButton.getOk(() => this.setLink());
        okButton.disabled = this.disabled || (this.data.chooseParent && !this.parent$.value);
        const buttons = [...DialogButton.getCancel(() => this.cancel()), okButton];
        this.dialogRef.patchConfig({ buttons });
    }

    private getBreadcrumbs(node: Node) {
        if (node && node.ref.id !== RestConstants.USERHOME) {
            return this.nodeService.getNodeParents(node.ref.id).pipe(
                map((parentList) => this.getBreadcrumbsByParentList(parentList)),
                catchError(() =>
                    rxjs.of(
                        this.getBreadcrumbsByParentList({
                            nodes: [node],
                            pagination: null,
                            scope: 'UNKNOWN',
                        }),
                    ),
                ),
            );
        } else {
            return rxjs.of(null);
        }
    }

    private getBreadcrumbsByParentList(parentList: ParentList) {
        const nodes = parentList.nodes.reverse();
        switch (parentList.scope) {
            case 'MY_FILES':
            // api will return null if fullPath was requested (i.e. as admin)
            case null:
                return {
                    nodes,
                    homeLabel: 'WORKSPACE.MY_FILES',
                    homeIcon: 'person',
                };
            case 'SHARED_FILES':
                return {
                    nodes,
                    homeLabel: 'WORKSPACE.SHARED_FILES',
                    homeIcon: 'group',
                };
            case 'UNKNOWN':
                return {
                    nodes,
                    homeLabel: 'WORKSPACE.RESTRICTED_FOLDER',
                    homeIcon: 'folder',
                };
            default:
                console.warn(`Unknown scope "${parentList.scope}"`);
                return {
                    nodes,
                    homeLabel: null,
                    homeIcon: null,
                };
        }
    }

    async setSaveParent(status: boolean) {
        if (status) {
            await this.storageService.set('defaultInboxFolder', this.parent$.value.ref.id);
            this.toast.toast('TOAST.STORAGE_LOCATION_SAVED', { name: this.parent$.value.name });
        } else {
            await this.storageService.delete('defaultInboxFolder');
            this.toast.toast('TOAST.STORAGE_LOCATION_RESET');
        }
    }
}

// Adapted from https://stackoverflow.com/questions/5717093/check-if-a-javascript-string-is-a-url
function getValidHttpUrl(url: string): string {
    url = url?.trim();
    if (!url) {
        return null;
    }
    if (!(url.startsWith('http://') || url.startsWith('https://'))) {
        url = 'http://' + url;
    }
    try {
        const parsedUrl = new URL(url);
        if (parsedUrl.protocol === 'http:' || parsedUrl.protocol === 'https:') {
            return url;
        }
    } catch (e) {
        // Return null
    }
    return null;
}
