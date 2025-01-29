import { Component, Inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { OPEN_URL_MODE } from 'ngx-edu-sharing-ui';
import {
    Connector,
    DialogButton,
    FrameEventsService,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
} from '../../../../core-module/core.module';
import { Node } from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { Toast } from '../../../../services/toast';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { DialogsService } from '../../dialogs.service';
import { BreadcrumbsService } from '../../../../shared/components/breadcrumbs/breadcrumbs.service';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { CreateVariantDialogData, CreateVariantDialogResult } from './create-variant-dialog-data';
import { Observable } from 'rxjs';

@Component({
    selector: 'es-create-variant-dialog',
    templateUrl: './create-variant-dialog.component.html',
    styleUrls: ['./create-variant-dialog.component.scss'],
})
export class CreateVariantDialogComponent {
    variantName: string;
    licenseWarning: string;

    private _openViaConnector: Connector;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: CreateVariantDialogData,
        private dialogRef: CardDialogRef<CreateVariantDialogData, CreateVariantDialogResult>,
        private breadcrumbsService: BreadcrumbsService,
        private connector: RestConnectorService,
        private connectors: RestConnectorsService,
        private dialogs: DialogsService,
        private events: FrameEventsService,
        private iam: RestIamService,
        private nodeApi: RestNodeService,
        private nodeHelper: NodeHelperService,
        private router: Router,
        private toast: Toast,
        private translate: TranslateService,
    ) {
        this._initNode();
        this._updateBreadcrumbs(RestConstants.INBOX);
        this._updateButtons();
    }

    private _initNode() {
        const node = this.data.node;
        this.variantName = this.translate.instant('NODE_VARIANT.DEFAULT_NAME', {
            name: this.data.node.name,
        });
        this._openViaConnector = this.connectors.connectorSupportsEdit(node);
        let license = node.properties[RestConstants.CCM_PROP_LICENSE]
            ? node.properties[RestConstants.CCM_PROP_LICENSE][0]
            : '';
        if (license.startsWith('CC_BY') && license.indexOf('ND') != -1) {
            this.licenseWarning = 'ND';
        } else if (license.startsWith('COPYRIGHT')) {
            this.licenseWarning = 'COPYRIGHT';
        } else if (!license) {
            this.licenseWarning = 'NO_LICENSE';
        }
    }

    private _cancel() {
        this.dialogRef.close(null);
    }

    private _done() {
        this.dialogRef.close(null);
    }

    private _create() {
        if (!this.breadcrumbsService.breadcrumbs$.value?.length) {
            return;
        }
        let win: any = null;
        if (this._openViaConnector) {
            win = UIHelper.getNewWindow(this.connector);
        }
        this.dialogRef.patchState({ isLoading: true });
        this.nodeApi
            .forkNode(
                this.breadcrumbsService.breadcrumbs$.value[
                    this.breadcrumbsService.breadcrumbs$.value.length - 1
                ].ref.id,
                this.data.node.ref.id,
            )
            .subscribe(
                (created) => {
                    this.nodeApi
                        .editNodeMetadata(
                            created.node.ref.id,
                            RestHelper.createNameProperty(this.variantName),
                        )
                        .subscribe(
                            (edited) => {
                                this.dialogRef.patchState({ isLoading: false });
                                if (this._openViaConnector) {
                                    UIHelper.openConnector(
                                        this.connectors,
                                        this.iam,
                                        this.events,
                                        this.toast,
                                        edited.node,
                                        null,
                                        win,
                                    );
                                    UIHelper.goToWorkspaceFolder(
                                        this.nodeApi,
                                        this.router,
                                        this.connector.getCurrentLogin(),
                                        this.breadcrumbsService.breadcrumbs$.value[
                                            this.breadcrumbsService.breadcrumbs$.value.length - 1
                                        ].ref.id,
                                    );
                                } else {
                                    let additional = {
                                        link: {
                                            caption: 'NODE_VARIANT.CREATED_LINK',
                                            callback: () => {
                                                UIHelper.goToWorkspaceFolder(
                                                    this.nodeApi,
                                                    this.router,
                                                    this.connector.getCurrentLogin(),
                                                    this.breadcrumbsService.breadcrumbs$.value[
                                                        this.breadcrumbsService.breadcrumbs$.value
                                                            .length - 1
                                                    ].ref.id,
                                                );
                                            },
                                        },
                                    };
                                    this.toast.toast(
                                        'NODE_VARIANT.CREATED',
                                        {
                                            folder: this.breadcrumbsService.breadcrumbs$.value[
                                                this.breadcrumbsService.breadcrumbs$.value.length -
                                                    1
                                            ].name,
                                        },
                                        null,
                                        null,
                                        additional,
                                    );
                                }
                                this._done();
                            },
                            (error) => {
                                this.dialogRef.patchState({ isLoading: false });
                                this.nodeHelper.handleNodeError(this.variantName, error);
                                if (win) win.close();
                            },
                        );
                },
                (error) => {
                    this.dialogRef.patchState({ isLoading: false });
                    console.log(error);
                    if (error.error?.error?.indexOf('DAORestrictedAccessException') !== -1) {
                        this.toast.error(null, 'RESTRICTED_ACCESS_COPY_ERROR');
                    } else {
                        this.nodeHelper.handleNodeError(this.variantName, error);
                    }
                    if (win) win.close();
                },
            );
    }

    async chooseDirectory() {
        const dialogRef = await this.dialogs.openFileChooserDialog({
            title: 'NODE_VARIANT.FILE_PICKER_TITLE',
            pickDirectory: true,
            writeRequired: true,
        });
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                this._setDirectory(result);
            }
        });
    }

    private _setDirectory(event: Node[]) {
        this._updateBreadcrumbs(event[0].ref.id);
    }

    private _updateBreadcrumbs(id: string) {
        this.nodeApi.getNodeParents(id, false).subscribe((parents) => {
            this.breadcrumbsService.setNodePath(parents.nodes.reverse());
        });
    }

    openLicense(url: string) {
        UIHelper.openUrl(url, this.connector.getBridgeService(), OPEN_URL_MODE.BlankSystemBrowser);
    }

    getLicenseUrl(): Observable<string | null> {
        return this.nodeHelper.getLicenseUrlByString(
            this.data.node.properties[RestConstants.CCM_PROP_LICENSE]?.[0],
            this.data.node.properties[RestConstants.CCM_PROP_LICENSE_CC_VERSION]?.[0],
            this.data.node.properties[RestConstants.CCM_PROP_LICENSE_CC_LOCALE]?.[0],
        );
    }

    private _updateButtons(): void {
        const buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this._cancel()),
            new DialogButton(
                'NODE_VARIANT.CREATE' + (this._openViaConnector ? '_EDIT' : ''),
                { color: 'primary' },
                () => this._create(),
            ),
        ];
        this.dialogRef.patchConfig({ buttons });
    }
}
